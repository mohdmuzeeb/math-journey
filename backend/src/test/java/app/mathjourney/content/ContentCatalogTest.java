package app.mathjourney.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ContentCatalogTest {

	@Test
	void logsEveryErrorThenRefusesToStart(CapturedOutput output) {
		assertThatExceptionOfType(ContentValidationException.class)
			.isThrownBy(() -> new ContentCatalog("classpath:content-cases/two-bad-files", TestVerifiers.list()))
			.satisfies((ex) -> assertThat(ex.getErrors()).hasSize(2));
		assertThat(output).contains("[content] concepts/8.NS.1-test-concept.json $.items[2]: ")
			.contains("[content] concepts/8.NS.2-second-concept.json $.items[1].payload.step: ");
	}

	@Test
	void findsDemoConcept() {
		ContentCatalog catalog = new ContentCatalog("classpath:content/demo", TestVerifiers.list());
		assertThat(catalog.findById("demo-number-line")).hasValueSatisfying((c) -> assertThat(c.reviewed()).isTrue());
		assertThat(catalog.findById("nope")).isEmpty();
	}

}
