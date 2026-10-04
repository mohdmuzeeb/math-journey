package app.mathjourney.learner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import app.mathjourney.MathJourneyApplication;

/**
 * The learner's data lives in {@code app.data-dir} and survives a restart (CAP-12, AD-17).
 * Each case starts the real app on its own temporary directory.
 */
class LearnerPersistenceTest {

	@TempDir
	Path tmp;

	@Test
	void profileSurvivesRestart() {
		try (ConfigurableApplicationContext context = start(this.tmp)) {
			assertThat(this.tmp.resolve("mathjourney.mv.db")).exists();
			assertThat(context.getBean(LearnerService.class).profile())
				.isEqualTo(new LearnerView(OnboardingStage.JOURNEY, true, "Lumi"));
			context.getBean(JdbcTemplate.class).update("UPDATE learner_profile SET companion_name = ?", "Pip");
		}
		try (ConfigurableApplicationContext context = start(this.tmp)) {
			assertThat(context.getBean(LearnerService.class).profile().companionName()).isEqualTo("Pip");
			assertThat(context.getBean(Flyway.class).info().applied()).hasSize(1);
		}
	}

	@Test
	void missingDataDirIsCreated() {
		Path dataDir = this.tmp.resolve("not").resolve("there");
		assertThat(dataDir).doesNotExist();
		try (ConfigurableApplicationContext context = start(dataDir)) {
			assertThat(Files.isDirectory(dataDir)).isTrue();
			assertThat(dataDir.resolve("mathjourney.mv.db")).exists();
		}
	}

	@Test
	void missingProfileRowFailsLoudly() {
		try (ConfigurableApplicationContext context = start(this.tmp)) {
			context.getBean(JdbcTemplate.class).update("DELETE FROM learner_profile");
			assertThatIllegalStateException().isThrownBy(() -> context.getBean(LearnerService.class).profile())
				.withMessageContaining("found 0");
		}
	}

	@Test
	void schemaDriftStopsStartup() {
		try (ConfigurableApplicationContext context = start(this.tmp)) {
			context.getBean(JdbcTemplate.class).execute("ALTER TABLE learner_profile DROP COLUMN sound");
		}
		assertThatThrownBy(() -> start(this.tmp).close()).hasStackTraceContaining("missing column [sound]");
	}

	private static ConfigurableApplicationContext start(Path dataDir) {
		return new SpringApplicationBuilder(MathJourneyApplication.class).web(WebApplicationType.NONE)
			.run("--app.data-dir=" + dataDir.toAbsolutePath());
	}

}
