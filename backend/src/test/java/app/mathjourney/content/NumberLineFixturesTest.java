package app.mathjourney.content;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the shared number-line fixtures (AD-3) against the Java mirror of {@code numberLine.ts}.
 * Entry 7 runs the same files in TypeScript.
 */
class NumberLineFixturesTest {

	/** Relative to {@code backend/}, where Maven runs the tests. */
	private static final Path FIXTURES = Path.of("../content/fixtures/number-line");

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Test
	void everyFixtureCaseMatches() throws IOException {
		List<Path> files;
		try (Stream<Path> listing = Files.list(FIXTURES)) {
			files = listing.filter((path) -> path.toString().endsWith(".json")).sorted().toList();
		}
		assertThat(files).as("fixture files in %s", FIXTURES.toAbsolutePath()).isNotEmpty();
		int checked = 0;
		for (Path file : files) {
			JsonNode fixture = JSON.readTree(file.toFile());
			JsonNode payload = fixture.get("payload");
			double answer = fixture.get("answer").asDouble();
			JsonNode cases = fixture.get("cases");
			assertThat(cases.size()).as("cases in %s", file.getFileName()).isPositive();
			for (int i = 0; i < cases.size(); i++) {
				JsonNode testCase = cases.get(i);
				double response = testCase.get("response").asDouble();
				String where = file.getFileName() + " cases[" + i + "] (response " + response + ")";
				assertThat(NumberLineVerifier.canonicalTick(payload, response)).as("canonical of %s", where)
					.isEqualTo(testCase.get("canonical").asLong());
				assertThat(NumberLineVerifier.verdict(payload, answer, response)).as("verdict of %s", where)
					.isEqualTo(testCase.get("verdict").asString());
				checked++;
			}
		}
		assertThat(checked).isPositive();
	}

	@Test
	void everyFixtureIsValidContent() throws IOException {
		// a fixture's payload and answer are real content, so they must verify too
		try (Stream<Path> listing = Files.list(FIXTURES)) {
			for (Path file : listing.filter((path) -> path.toString().endsWith(".json")).toList()) {
				JsonNode fixture = JSON.readTree(file.toFile());
				assertThat(new NumberLineVerifier().verify(fixture.get("payload"), fixture.get("answer"), null))
					.as("verification of %s", file.getFileName())
					.isEmpty();
			}
		}
	}

}
