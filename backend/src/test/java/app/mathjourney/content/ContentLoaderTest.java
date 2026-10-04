package app.mathjourney.content;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * One minimal content root per error case under {@code src/test/resources/content-cases/}. Every
 * error must name the file and the JSON path inside it (AD-5).
 */
class ContentLoaderTest {

	private static final String CONCEPT_FILE = "concepts/8.NS.1-test-concept.json";

	private static ContentLoader.Result load(String caseName) {
		return new ContentLoader("classpath:content-cases/" + caseName).load();
	}

	private static List<ContentError> errors(String caseName) {
		ContentLoader.Result result = load(caseName);
		assertThat(result.errors()).as("errors of case %s", caseName).isNotEmpty();
		assertThat(result.concepts()).isEmpty();
		return result.errors();
	}

	@Test
	void realContentLoadsWithNoConcepts() {
		ContentLoader.Result result = new ContentLoader("classpath:content").load();
		assertThat(result.errors()).isEmpty();
		assertThat(result.concepts()).isEmpty();
	}

	@Test
	void demoContentLoadsAndIsReviewed() {
		ContentLoader.Result result = new ContentLoader("classpath:content/demo").load();
		assertThat(result.errors()).isEmpty();
		assertThat(result.concepts()).containsOnlyKeys("demo-number-line");
		CatalogConcept concept = result.concepts().get("demo-number-line");
		assertThat(concept.reviewed()).isTrue();
		assertThat(concept.retired()).isFalse();
		assertThat(concept.land()).isEqualTo("numbers");
		assertThat(concept.json().at("/items/0/payload/prompt").asString()).isEqualTo("Drag the point to 3/4");
		assertThat(concept.json().at("/items/0/answer").asDouble()).isEqualTo(0.75);
	}

	@Test
	void validCaseLoads() {
		ContentLoader.Result result = load("valid");
		assertThat(result.errors()).isEmpty();
		assertThat(result.concepts()).containsOnlyKeys("8.NS.1-test-concept");
		assertThat(result.concepts().get("8.NS.1-test-concept").reviewed()).isFalse();
	}

	@Test
	void missingConceptsFolderMeansZeroConcepts() {
		ContentLoader.Result result = load("no-concepts-folder");
		assertThat(result.errors()).isEmpty();
		assertThat(result.concepts()).isEmpty();
	}

	@Test
	void missingRequiredField() {
		assertThat(errors("missing-hint")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items[2]");
			assertThat(error.message()).contains("hint");
		});
	}

	@Test
	void tooFewItemsOfARole() {
		assertThat(errors("too-few-practice")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items");
			assertThat(error.message()).contains("at least 4", "practice", "found 3");
		});
	}

	@Test
	void badKindPayload() {
		assertThat(errors("bad-payload-step")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items[1].payload.step");
		});
	}

	@Test
	void badSimilarPayload() {
		assertThat(errors("bad-similar-payload")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items[3].walkthrough.similar.payload");
			assertThat(error.message()).contains("target");
		});
	}

	@Test
	void unknownKind() {
		assertThat(errors("unknown-kind")).anySatisfy((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items[0].kind");
		});
	}

	@Test
	void unknownCurriculumId() {
		assertThat(errors("unknown-curriculum-id")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo("curriculum.json");
			assertThat(error.jsonPath()).isEqualTo("$.lands[1].concepts[0]");
			assertThat(error.message()).contains("8.EE.7-missing-concept");
		});
	}

	@Test
	void orphanConcept() {
		assertThat(errors("orphan-concept")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo("concepts/8.NS.2-orphan-concept.json");
			assertThat(error.jsonPath()).isEqualTo("$");
			assertThat(error.message()).contains("not listed");
		});
	}

	@Test
	void idDiffersFromFileName() {
		assertThat(errors("id-mismatch")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo("concepts/8.NS.1-other-name.json");
			assertThat(error.jsonPath()).isEqualTo("$.id");
		});
	}

	@Test
	void itemIdWithoutConceptPrefix() {
		assertThat(errors("item-id-prefix")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items[5].id");
		});
	}

	@Test
	void duplicateItemId() {
		assertThat(errors("duplicate-item-id")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items[6].id");
			assertThat(error.message()).contains("duplicate");
		});
	}

	@Test
	void landDiffersFromTheListingLand() {
		assertThat(errors("land-mismatch")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.land");
			assertThat(error.message()).contains("$.lands[0].concepts[0]");
		});
	}

	@Test
	void retiredConceptInALand() {
		assertThat(errors("retired-in-land")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo("curriculum.json");
			assertThat(error.jsonPath()).isEqualTo("$.lands[0].concepts[0]");
			assertThat(error.message()).contains("retired");
		});
	}

	@Test
	void malformedJson() {
		assertThat(errors("malformed-json")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.message()).contains("malformed JSON");
		});
	}

	@Test
	void everyBrokenFileIsReported() {
		assertThat(errors("two-bad-files")).hasSize(2)
			.anySatisfy((error) -> {
				assertThat(error.file()).isEqualTo(CONCEPT_FILE);
				assertThat(error.jsonPath()).isEqualTo("$.items[2]");
			})
			.anySatisfy((error) -> {
				assertThat(error.file()).isEqualTo("concepts/8.NS.2-second-concept.json");
				assertThat(error.jsonPath()).isEqualTo("$.items[1].payload.step");
			});
	}

	@Test
	void unknownPrerequisiteId() {
		assertThat(errors("unknown-prerequisite-id")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo("curriculum.json");
			assertThat(error.jsonPath()).isEqualTo("$.prerequisites[0]");
			assertThat(error.message()).contains("7.NS.1-missing-prereq");
		});
	}

	@Test
	void conceptInALandAndInPrerequisites() {
		assertThat(errors("land-and-prerequisite")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo("curriculum.json");
			assertThat(error.jsonPath()).isEqualTo("$.prerequisites[0]");
			assertThat(error.message()).contains("more than once");
		});
	}

	@Test
	void duplicateJsonKey() {
		assertThat(errors("duplicate-json-key")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$");
			assertThat(error.message()).contains("malformed JSON", "title");
		});
	}

	@Test
	void badAcceptedAnswer() {
		assertThat(errors("bad-accepted-answer")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items[2].acceptedAnswers[0]");
		});
	}

	@Test
	void errorLineNamesFileAndPath() {
		ContentError error = new ContentError("concepts/x.json", "$.items[0]", "boom");
		assertThat(error).hasToString("[content] concepts/x.json $.items[0]: boom");
		assertThat(new ContentValidationException(List.of(error))).hasMessageContaining(error.toString());
	}

}
