package app.mathjourney.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import java.util.List;

import org.junit.jupiter.api.Test;

import app.mathjourney.content.AnswerProblem.Field;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Exact content verification for the number-line kind (AD-3). */
class NumberLineVerifierTest {

	/** Reads numbers as the content loader does: decimals as exact BigDecimal nodes. */
	private static final JsonMapper JSON = JsonMapper.builder()
		.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
		.build();

	private final NumberLineVerifier verifier = new NumberLineVerifier();

	private static JsonNode json(String text) {
		return JSON.readTree(text);
	}

	private static JsonNode payload(String min, String max, String step, String target) {
		return json("{\"prompt\":\"p\",\"min\":" + min + ",\"max\":" + max + ",\"step\":" + step + ",\"target\":\""
				+ target + "\"}");
	}

	/** The messages of the problems with {@code answer}; fails if any problem is not about {@code answer}. */
	private List<String> verify(JsonNode payload, String answer) {
		List<AnswerProblem> problems = this.verifier.verify(payload, json(answer), null);
		assertThat(problems).allSatisfy((problem) -> assertThat(problem.field()).isEqualTo(Field.ANSWER));
		return problems.stream().map(AnswerProblem::message).toList();
	}

	@Test
	void kindIsNumberLine() {
		assertThat(this.verifier.kind()).isEqualTo("number-line");
	}

	@Test
	void fractionTarget() {
		assertThat(verify(payload("0", "2", "0.25", "3/4"), "0.75")).isEmpty();
	}

	@Test
	void decimalTarget() {
		assertThat(verify(payload("0", "2", "0.25", "1.25"), "1.25")).isEmpty();
	}

	@Test
	void integerTarget() {
		assertThat(verify(payload("0", "5", "1", "3"), "3")).isEmpty();
		assertThat(verify(payload("0", "2", "0.5", "2"), "2.0")).isEmpty();
	}

	@Test
	void negativeTarget() {
		assertThat(verify(payload("-1", "1", "0.25", "-1/2"), "-0.5")).isEmpty();
		assertThat(verify(payload("-1", "1", "0.25", "-3/4"), "-0.75")).isEmpty();
	}

	@Test
	void targetsAtBothEndsOfTheLine() {
		assertThat(verify(payload("-1", "1", "0.25", "-1"), "-1")).isEmpty();
		assertThat(verify(payload("-1", "1", "0.25", "4/4"), "1")).isEmpty();
	}

	@Test
	void floatNoiseGridIsExact() {
		// 0.1 * 3 is 0.30000000000000004 in doubles; the exact check still passes
		assertThat(verify(payload("0", "1", "0.1", "3/10"), "0.3")).isEmpty();
		assertThat(verify(payload("0", "1", "0.1", "7/10"), "0.7")).isEmpty();
		assertThat(verify(payload("0", "1", "0.2", "3/5"), "0.6")).isEmpty();
	}

	@Test
	void offGridTarget() {
		assertThat(verify(payload("0", "2", "0.25", "1/3"), "0.25")).singleElement()
			.asString()
			.contains("1/3", "not on a tick", "step 0.25");
	}

	@Test
	void offGridBecauseMinIsOffset() {
		// on a grid of quarters starting at 0.1, 3/4 is not a tick
		assertThat(verify(payload("0.1", "2.1", "0.25", "3/4"), "0.75")).singleElement()
			.asString()
			.contains("not on a tick");
	}

	@Test
	void targetOutOfRange() {
		assertThat(verify(payload("0", "2", "0.25", "5/2"), "2.5")).singleElement()
			.asString()
			.contains("5/2", "outside", "[0, 2]");
		assertThat(verify(payload("-1", "1", "0.25", "-5/4"), "-1.25")).singleElement()
			.asString()
			.contains("[-1, 1]");
	}

	@Test
	void answerMismatch() {
		assertThat(verify(payload("0", "2", "0.25", "1/2"), "0.75")).singleElement()
			.asString()
			.contains("stored answer 0.75", "computed answer 0.5");
	}

	@Test
	void answerOnANeighbouringTenth() {
		assertThat(verify(payload("0", "1", "0.1", "3/10"), "0.30000000000000004")).singleElement()
			.asString()
			.contains("computed answer 0.3");
	}

	@Test
	void answerBeyondDoublePrecisionIsRejected() {
		// 0.50000000000000001 reads as the double 0.5; the exact decimal text must not
		assertThat(verify(payload("0", "2", "0.25", "1/2"), "0.50000000000000001")).singleElement()
			.asString()
			.contains("stored answer 0.50000000000000001", "computed answer 0.5");
	}

	@Test
	void zeroDenominator() {
		assertThat(verify(payload("0", "2", "0.25", "1/0"), "0")).singleElement().asString().contains("zero");
	}

	@Test
	void acceptedAnswersAreNotAllowed() {
		JsonNode payload = payload("0", "2", "0.25", "1/2");
		assertThat(this.verifier.verify(payload, json("0.5"), json("[0.5]"))).singleElement()
			.satisfies((problem) -> {
				assertThat(problem.field()).isEqualTo(Field.ACCEPTED_ANSWERS);
				assertThat(problem.message()).contains("exactly one correct tick", "acceptedAnswers");
			});
		assertThat(this.verifier.verify(payload, json("0.5"), json("[]"))).isEmpty();
	}

	@Test
	void answerAndAcceptedAnswersProblemsAreBothReported() {
		JsonNode payload = payload("0", "2", "0.25", "1/2");
		assertThat(this.verifier.verify(payload, json("0.75"), json("[0.75]"))).extracting(AnswerProblem::field)
			.containsExactly(Field.ANSWER, Field.ACCEPTED_ANSWERS);
	}

	// --- geometry (story 1.9) ---

	/** The single problem, which must be about the payload. */
	private String payloadProblem(JsonNode payload, String answer) {
		List<AnswerProblem> problems = this.verifier.verify(payload, json(answer), null);
		assertThat(problems).singleElement()
			.satisfies((problem) -> assertThat(problem.field()).isEqualTo(Field.PAYLOAD));
		return problems.get(0).message();
	}

	@Test
	void rangeThatIsNotWholeSteps() {
		// 0..1 in steps of 0.4 would draw a droppable 1.2 past the end
		assertThat(payloadProblem(payload("0", "1", "0.4", "2/5"), "0.4")).contains("whole number of steps", "0.4",
				"2.5");
	}

	@Test
	void tooManySteps() {
		assertThat(payloadProblem(payload("0", "41", "1", "3"), "3")).contains("at most 40 steps");
		assertThat(payloadProblem(payload("0", "1", "0.001", "1/2"), "0.5")).contains("at most 40 steps");
	}

	@Test
	void fortyStepsPass() {
		assertThat(verify(payload("-10", "10", "0.5", "-10"), "-10")).isEmpty();
		assertThat(verify(payload("-10", "10", "0.5", "10"), "10")).isEmpty();
		assertThat(verify(payload("-10", "10", "0.5", "3/2"), "1.5")).isEmpty();
	}

	@Test
	void emptyRange() {
		assertThat(payloadProblem(payload("1", "1", "0.25", "1"), "1")).contains("max 1 must be greater than min 1");
	}

	@Test
	void reversedRange() {
		assertThat(payloadProblem(payload("2", "0", "0.25", "1"), "1")).contains("max 0 must be greater than min 2");
	}

	@Test
	void stepThatIsZeroAsADouble() {
		assertThat(payloadProblem(payload("0", "1", "1e-400", "0"), "0")).contains("step", "0 as a double");
	}

	@Test
	void valuesThatAreNotFiniteAsDoubles() {
		assertThat(payloadProblem(payload("0", "1e400", "1", "0"), "0")).contains("max", "not a finite double");
		assertThat(payloadProblem(payload("-1e400", "0", "1", "0"), "0")).contains("min", "not a finite double");
		assertThat(payloadProblem(payload("0", "1", "1e400", "0"), "0")).contains("step", "not a finite double");
	}

	@Test
	void answerThatIsNotFiniteAsADouble() {
		assertThat(verify(payload("0", "1", "0.25", "1/4"), "1e400")).singleElement()
			.asString()
			.contains("stored answer", "not a finite double");
	}

	@Test
	void geometryStopsTheOtherAnswerChecks() {
		// the target is also off the line, but only the first geometry problem is reported
		assertThat(payloadProblem(payload("0", "1", "0.4", "5"), "7")).contains("whole number of steps");
	}

	@Test
	void browserSnapThatDisagreesWithTheExactTick() {
		// min 2^53: in doubles 2^53 + 1 rounds to 2^53, so the browser snaps the answer to tick 0,
		// while its exact tick is 1
		String min = "9007199254740992";
		String answer = "9007199254740993";
		assertThat(this.verifier.verify(payload(min, "9007199254741000", "1", answer), json(answer), null))
			.singleElement()
			.satisfies((problem) -> {
				assertThat(problem.field()).isEqualTo(Field.ANSWER);
				assertThat(problem.message()).contains("tick 0", "exact tick is 1");
			});
	}

	@Test
	void browserStepCountThatDisagreesWithTheExactCount() {
		// exactly 9 steps, but max - min is 8 (or 10) in doubles above 2^53
		assertThat(payloadProblem(payload("9007199254740992", "9007199254741001", "1", "9007199254740992"),
				"9007199254740992"))
			.contains("has 9 steps", "in double arithmetic");
		// exactly 20 steps, but max - min overflows to Infinity in doubles
		assertThat(payloadProblem(payload("-1e308", "1e308", "1e307", "0"), "0")).contains("has 20 steps",
				"Infinity");
	}

	@Test
	void valuesWithAHugeExponentAreRefusedQuickly() {
		// 1e-20000000 reads as the double 0, so it is finite; exact arithmetic on it would take minutes
		assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
			assertThat(payloadProblem(payload("1e-20000000", "1", "0.25", "1/2"), "0.5")).contains("min",
					"exponent");
			assertThat(this.verifier.verify(payload("0", "1", "0.25", "1/2"), json("1e-20000000"), null))
				.singleElement()
				.satisfies((problem) -> {
					assertThat(problem.field()).isEqualTo(Field.ANSWER);
					assertThat(problem.message()).contains("stored answer", "exponent");
				});
		});
	}

}
