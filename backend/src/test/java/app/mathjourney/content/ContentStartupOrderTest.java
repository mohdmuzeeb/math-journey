package app.mathjourney.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.core.io.ClassPathResource;

import app.mathjourney.MathJourneyApplication;

/** Bad content refuses to start before Flyway touches the learner's database (AD-5, AD-17). */
class ContentStartupOrderTest {

	@Test
	void badContentFailsStartupWithoutCreatingTheDatabase(@TempDir Path dataDir) throws Exception {
		Path badRoot = new ClassPathResource("content-cases/missing-hint").getFile().toPath();
		assertThatThrownBy(() -> SpringApplication.run(MathJourneyApplication.class,
				"--app.content-root=" + badRoot.toUri(), "--app.data-dir=" + dataDir, "--server.port=0"))
			.hasRootCauseInstanceOf(ContentValidationException.class);
		assertThat(Files.exists(dataDir.resolve("mathjourney.mv.db"))).isFalse();
	}

}
