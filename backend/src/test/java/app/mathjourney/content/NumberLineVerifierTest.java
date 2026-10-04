package app.mathjourney.content;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

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

	private List<String> verify(JsonNode payload, String answer) {
		return this.verifier.verify(payload, json(answer), null);
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
			.asString()
			.contains("exactly one correct tick", "acceptedAnswers");
		assertThat(this.verifier.verify(payload, json("0.5"), json("[]"))).isEmpty();
	}

	@Test
	void answerAndAcceptedAnswersProblemsAreBothReported() {
		JsonNode payload = payload("0", "2", "0.25", "1/2");
		assertThat(this.verifier.verify(payload, json("0.75"), json("[0.75]"))).hasSize(2);
	}

}
