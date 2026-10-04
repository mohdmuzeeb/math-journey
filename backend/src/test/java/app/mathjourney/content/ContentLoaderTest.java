package app.mathjourney.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * One minimal content root per error case under {@code src/test/resources/content-cases/}. Every
 * error must name the file and the JSON path inside it (AD-5).
 */
class ContentLoaderTest {

	private static final String CONCEPT_FILE = "concepts/8.NS.1-test-concept.json";

	/** A loader with the real answer verifiers, as Spring wires it. */
	private static ContentLoader loader(String root) {
		return new ContentLoader(root, TestVerifiers.byKind());
	}

	private static ContentLoader.Result load(String caseName) {
		return loader("classpath:content-cases/" + caseName).load();
	}

	private static List<ContentError> errors(String caseName) {
		ContentLoader.Result result = load(caseName);
		assertThat(result.errors()).as("errors of case %s", caseName).isNotEmpty();
		assertThat(result.concepts()).isEmpty();
		return result.errors();
	}

	@Test
	void realContentLoadsWithNoConcepts() {
		ContentLoader.Result result = loader("classpath:content").load();
		assertThat(result.errors()).isEmpty();
		assertThat(result.concepts()).isEmpty();
	}

	@Test
	void demoContentLoadsAndIsReviewed() {
		ContentLoader.Result result = loader("classpath:content/demo").load();
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

	/**
	 * The curriculum schema pins exactly five lands, in order. Each variant is loaded from a temporary
	 * content root with no concepts, so the curriculum is its only possible error.
	 */
	@Test
	void curriculumLandsArePinned(@TempDir Path root) throws IOException {
		String numbers = land("numbers");
		String rest = land("equations") + "," + land("functions") + "," + land("shapes") + "," + land("data");
		assertThat(curriculumErrors(root, numbers + "," + rest)).isEmpty();
		assertThat(curriculumErrors(root, rest)).as("four lands").isNotEmpty();
		assertThat(curriculumErrors(root, numbers + "," + rest + "," + land("extra"))).as("six lands").isNotEmpty();
		assertThat(curriculumErrors(root, rest + "," + numbers)).as("wrong order").isNotEmpty();
		assertThat(curriculumErrors(root,
				"{\"id\":\"numbers\",\"concepts\":[]}," + rest)).as("land without title").isNotEmpty();
		assertThat(curriculumErrors(root,
				"{\"id\":\"numbers\",\"title\":\"N\",\"concepts\":[],\"x\":1}," + rest)).as("extra land property")
			.isNotEmpty();
	}

	private static String land(String id) {
		return "{\"id\":\"" + id + "\",\"title\":\"T\",\"concepts\":[]}";
	}

	private static List<ContentError> curriculumErrors(Path root, String lands) throws IOException {
		Files.writeString(root.resolve("curriculum.json"), "{\"lands\":[" + lands + "],\"prerequisites\":[]}");
		return loader(root.toUri().toString()).load().errors();
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
	void wrongItemAnswer() {
		assertThat(errors("wrong-answer")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items[1].answer");
			assertThat(error.message()).contains("8.NS.1-test-concept#item-1", "0.75", "0.5");
		});
	}

	@Test
	void answerIsReadFromItsExactDecimalText() {
		// 0.50000000000000001 would read as the double 0.5; the loader keeps the exact decimal
		assertThat(errors("answer-beyond-double")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items[1].answer");
			assertThat(error.message()).contains("8.NS.1-test-concept#item-1", "0.50000000000000001");
		});
	}

	@Test
	void wrongSimilarAnswer() {
		assertThat(errors("wrong-similar-answer")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items[2].walkthrough.similar.answer");
			assertThat(error.message()).contains("8.NS.1-test-concept#item-2", "0.5", "0.25");
		});
	}

	@Test
	void acceptedAnswersOnANumberLineItem() {
		assertThat(errors("accepted-answers")).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items[1].acceptedAnswers");
			assertThat(error.message()).contains("8.NS.1-test-concept#item-1", "acceptedAnswers");
		});
	}

	@Test
	void wrongAnswerAndAcceptedAnswersAreBothReported() {
		assertThat(errors("wrong-answer-and-accepted-answers")).satisfiesExactly((error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items[1].answer");
			assertThat(error.message()).contains("8.NS.1-test-concept#item-1", "stored answer 0.75",
					"computed answer 0.5");
		}, (error) -> {
			assertThat(error.file()).isEqualTo(CONCEPT_FILE);
			assertThat(error.jsonPath()).isEqualTo("$.items[1].acceptedAnswers");
			assertThat(error.message()).contains("8.NS.1-test-concept#item-1", "exactly one correct tick");
		});
	}

	@Test
	void verifierIsCalledOncePerNode() throws IOException {
		AnswerVerifier real = new NumberLineVerifier();
		AtomicInteger calls = new AtomicInteger();
		AnswerVerifier counting = new AnswerVerifier() {
			@Override
			public String kind() {
				return real.kind();
			}

			@Override
			public List<AnswerProblem> verify(JsonNode payload, JsonNode answer, JsonNode acceptedAnswers) {
				calls.incrementAndGet();
				return real.verify(payload, answer, acceptedAnswers);
			}
		};
		String caseRoot = "content-cases/wrong-answer-and-accepted-answers/";
		List<ContentError> errors = new ContentLoader("classpath:" + caseRoot, Map.of(counting.kind(), counting))
			.load()
			.errors();
		assertThat(errors).hasSize(2);
		// one call per item and one per walkthrough similar, including the item with acceptedAnswers
		int items;
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(caseRoot + CONCEPT_FILE)) {
			items = JsonMapper.builder().build().readTree(in).get("items").size();
		}
		assertThat(calls).hasValue(2 * items);
	}

	@Test
	void kindWithoutAVerifier() {
		ContentLoader.Result result = new ContentLoader("classpath:content/demo", Map.of()).load();
		assertThat(result.concepts()).isEmpty();
		assertThat(result.errors()).singleElement().satisfies((error) -> {
			assertThat(error.file()).isEqualTo("schemas/concept.schema.json");
			assertThat(error.jsonPath()).isEqualTo("$.$defs.item.properties.kind.enum");
			assertThat(error.message()).contains("\"number-line\"", "AnswerVerifier");
		});
	}

	@Test
	void verifierForAnUnknownKind() {
		AnswerVerifier extra = new AnswerVerifier() {
			@Override
			public String kind() {
				return "balance-scale";
			}

			@Override
			public List<AnswerProblem> verify(JsonNode payload, JsonNode answer, JsonNode acceptedAnswers) {
				return List.of();
			}
		};
		Map<String, AnswerVerifier> verifiers = new HashMap<>(TestVerifiers.byKind());
		verifiers.put(extra.kind(), extra);
		assertThat(new ContentLoader("classpath:content/demo", verifiers).load().errors()).singleElement()
			.satisfies((error) -> assertThat(error.message()).contains("\"balance-scale\"", "kind enum",
					"balance-scale.schema.json"));
	}

	@Test
	void twoVerifiersForOneKindIsAProgrammingError() {
		assertThatIllegalStateException()
			.isThrownBy(() -> ContentLoader.byKind(List.of(new NumberLineVerifier(), new NumberLineVerifier())))
			.withMessageContaining("number-line");
	}

	@Test
	void errorLineNamesFileAndPath() {
		ContentError error = new ContentError("concepts/x.json", "$.items[0]", "boom");
		assertThat(error).hasToString("[content] concepts/x.json $.items[0]: boom");
		assertThat(new ContentValidationException(List.of(error))).hasMessageContaining(error.toString());
	}

}
