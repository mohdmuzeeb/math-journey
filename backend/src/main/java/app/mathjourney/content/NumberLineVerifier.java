package app.mathjourney.content;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

/**
 * The {@code number-line} kind's {@link AnswerVerifier} (AD-3).
 * <p>
 * There are two separate meanings of "correct" here, kept in separate code paths:
 * <ul>
 * <li><strong>Content verification</strong> ({@link #verify}) is exact: {@code target} is parsed as
 * an integer, a decimal or {@code p/q}, and {@code min}, {@code max}, {@code step} and {@code answer}
 * as decimals,
 * with no floating point anywhere. It passes only when {@code target} lies in [min, max], sits
 * exactly on a tick ({@code (target - min) / step} is a whole number) and {@code answer} equals
 * it exactly. A number-line item has exactly one correct tick, so {@code acceptedAnswers} must be
 * empty.</li>
 * <li><strong>The browser's comparison</strong> ({@link #canonicalTick}, {@link #verdict}) mirrors
 * {@code frontend/src/numberLine.ts} exactly, double arithmetic and JS {@code Math.round}
 * included, so the shared fixtures under {@code content/fixtures/number-line/} hold in Java;
 * entry 7 runs the same files in TypeScript.</li>
 * </ul>
 */
@Component
class NumberLineVerifier implements AnswerVerifier {

	static final String KIND = "number-line";

	@Override
	public String kind() {
		return KIND;
	}

	@Override
	public List<String> verify(JsonNode payload, JsonNode answer, JsonNode acceptedAnswers) {
		List<String> problems = new ArrayList<>();
		check(payload, answer, problems);
		if (acceptedAnswers != null && !acceptedAnswers.isEmpty()) {
			problems.add("a number-line item has exactly one correct tick, so acceptedAnswers must be empty (found "
					+ acceptedAnswers + ")");
		}
		return problems;
	}

	private static void check(JsonNode payload, JsonNode answer, List<String> problems) {
		String targetText = payload.get("target").asString();
		BigDecimal min = decimal(payload.get("min"));
		BigDecimal max = decimal(payload.get("max"));
		BigDecimal step = decimal(payload.get("step"));
		BigDecimal stored = decimal(answer);

		// target = numerator / denominator, with denominator > 0; compare by cross-multiplying
		String[] parts = targetText.split("/", 2);
		BigDecimal numerator = new BigDecimal(parts[0]);
		BigDecimal denominator = (parts.length == 2) ? new BigDecimal(new BigInteger(parts[1])) : BigDecimal.ONE;
		if (denominator.signum() == 0) {
			problems.add("target " + targetText + " has a zero denominator");
			return;
		}

		if (numerator.compareTo(min.multiply(denominator)) < 0 || numerator.compareTo(max.multiply(denominator)) > 0) {
			problems.add("target " + targetText + " is outside the line's range [" + plain(min) + ", " + plain(max)
					+ "]");
			return;
		}

		// (target - min) / step = (numerator - min * denominator) / (step * denominator)
		BigDecimal offset = numerator.subtract(min.multiply(denominator));
		BigDecimal tickWidth = step.multiply(denominator);
		if (offset.remainder(tickWidth).signum() != 0) {
			problems.add("target " + targetText + " is not on a tick: (target - min) / step is not a whole number"
					+ " (min " + plain(min) + ", step " + plain(step) + ")");
			return;
		}
		BigDecimal tick = offset.divideToIntegralValue(tickWidth);
		BigDecimal computed = min.add(tick.multiply(step));
		if (stored.compareTo(computed) != 0) {
			problems.add("stored answer " + plain(stored) + " does not equal the computed answer " + plain(computed)
					+ " (target " + targetText + " is tick " + tick.toBigInteger() + " from min " + plain(min)
					+ " in steps of " + plain(step) + ")");
		}
	}

	/**
	 * The decimal the JSON number's text wrote. The content loader reads decimals as
	 * {@link BigDecimal} nodes ({@code USE_BIG_DECIMAL_FOR_FLOATS}), so integers and decimals are
	 * both exact. A double node (only from a caller that parsed without that feature) falls back to
	 * {@link BigDecimal#valueOf(double)}.
	 */
	private static BigDecimal decimal(JsonNode number) {
		if (number.isIntegralNumber()) {
			return new BigDecimal(number.bigIntegerValue());
		}
		if (number.isBigDecimal()) {
			return number.decimalValue();
		}
		return BigDecimal.valueOf(number.doubleValue());
	}

	private static String plain(BigDecimal value) {
		return (value.signum() == 0) ? "0" : value.stripTrailingZeros().toPlainString();
	}

	// --- the browser's comparison (numberLine.ts), for the shared fixtures ---

	/**
	 * The canonical form of a response: the index of the tick it snaps to, clamped to the line, i.e.
	 * {@code clamp(round((response - min) / step), 0, last)} in double arithmetic, as
	 * {@code numberLine.ts}'s {@code snap} computes it.
	 */
	static long canonicalTick(JsonNode payload, double response) {
		double min = payload.get("min").asDouble();
		double max = payload.get("max").asDouble();
		double step = payload.get("step").asDouble();
		long last = jsRound((max - min) / step);
		long index = jsRound((response - min) / step);
		return Math.min(Math.max(index, 0), last);
	}

	/** {@code correct} when the response snaps to the answer's tick, else {@code wrong} ({@code isCorrect}). */
	static String verdict(JsonNode payload, double answer, double response) {
		return (canonicalTick(payload, response) == canonicalTick(payload, answer)) ? "correct" : "wrong";
	}

	/**
	 * JS {@code Math.round}: the nearest integer, ties toward positive infinity. Java's
	 * {@link Math#round(double)} has the same specification.
	 */
	private static long jsRound(double value) {
		return Math.round(value);
	}

}
