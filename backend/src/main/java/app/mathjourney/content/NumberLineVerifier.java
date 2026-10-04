package app.mathjourney.content;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

/**
 * The {@code number-line} kind's {@link AnswerVerifier} (AD-3).
 * <p>
 * There are two separate meanings of "correct" here, kept in separate code paths:
 * <ul>
 * <li><strong>Geometry</strong> ({@link #verify}, first): the line must render as the browser draws
 * it. {@code min}, {@code max}, {@code step} and {@code answer} must be finite doubles with a
 * non-zero {@code step}, {@code max > min}, and {@code (max - min) / step} must be a whole number of
 * at most {@value #MAX_STEPS} steps. The first geometry problem is reported on the payload and stops
 * the node's other answer checks.</li>
 * <li><strong>Content verification</strong> ({@link #verify}) is exact: {@code target} is parsed as
 * an integer, a decimal or {@code p/q}, and {@code min}, {@code max}, {@code step} and {@code answer}
 * as decimals,
 * with no floating point anywhere. It passes only when {@code target} lies in [min, max], sits
 * exactly on a tick ({@code (target - min) / step} is a whole number) and {@code answer} equals
 * it exactly. The browser's double snap of the stored answer ({@link #canonicalTick}) must then be
 * that exact tick. A number-line item has exactly one correct tick, so {@code acceptedAnswers} must
 * be empty.</li>
 * <li><strong>The browser's comparison</strong> ({@link #canonicalTick}, {@link #verdict}) mirrors
 * {@code frontend/src/numberLine.ts} exactly, double arithmetic and JS {@code Math.round}
 * included, so the shared fixtures under {@code content/fixtures/number-line/} hold in Java;
 * entry 7 runs the same files in TypeScript.</li>
 * </ul>
 */
@Component
class NumberLineVerifier implements AnswerVerifier {

	static final String KIND = "number-line";

	/** The most steps a line may have: 40 steps, 41 ticks, so drop zones stay usable. */
	static final int MAX_STEPS = 40;

	/**
	 * The largest decimal exponent and scale a value may have. Every finite double lies within
	 * 1e-324..1e308, so this refuses nothing the browser can tell apart from 0 or Infinity, and it
	 * keeps exact arithmetic on, e.g., {@code 1e-20000000} from running for minutes.
	 */
	static final int MAX_EXPONENT = 400;

	@Override
	public String kind() {
		return KIND;
	}

	@Override
	public List<AnswerProblem> verify(JsonNode payload, JsonNode answer, JsonNode acceptedAnswers) {
		List<AnswerProblem> problems = new ArrayList<>();
		AnswerProblem geometry = geometry(payload, answer);
		if (geometry != null) {
			problems.add(geometry);
		}
		else {
			List<String> answerProblems = new ArrayList<>();
			check(payload, answer, answerProblems);
			answerProblems.forEach((message) -> problems.add(AnswerProblem.answer(message)));
		}
		if (acceptedAnswers != null && !acceptedAnswers.isEmpty()) {
			problems.add(AnswerProblem.acceptedAnswers("a number-line item has exactly one correct tick, so acceptedAnswers must be empty (found "
					+ acceptedAnswers + ")"));
		}
		return problems;
	}

	/**
	 * The first rule that keeps the line from rendering as the content means it, or null. Values are
	 * checked as doubles first (what the browser sees), so the exact checks below never meet an
	 * infinite, zero or unbounded quantity.
	 */
	private static AnswerProblem geometry(JsonNode payload, JsonNode answer) {
		for (String field : new String[] { "min", "max", "step" }) {
			double value = browserDouble(payload.get(field));
			if (!Double.isFinite(value)) {
				return AnswerProblem.payload(field + " " + payload.get(field) + " is not a finite double (it reads as "
						+ value + " in the browser)");
			}
		}
		if (browserDouble(payload.get("step")) == 0) {
			return AnswerProblem.payload("step " + payload.get("step") + " is 0 as a double, so the browser cannot"
					+ " place ticks");
		}
		if (!Double.isFinite(browserDouble(answer))) {
			return AnswerProblem.answer("stored answer " + answer + " is not a finite double (it reads as "
					+ browserDouble(answer) + " in the browser)");
		}
		for (String field : new String[] { "min", "max", "step" }) {
			if (outOfScale(decimal(payload.get(field)))) {
				return AnswerProblem.payload(field + " " + payload.get(field) + " has a decimal exponent or scale beyond ±"
						+ MAX_EXPONENT);
			}
		}
		if (outOfScale(decimal(answer))) {
			return AnswerProblem.answer("stored answer " + answer + " has a decimal exponent or scale beyond ±"
					+ MAX_EXPONENT);
		}
		BigDecimal min = decimal(payload.get("min"));
		BigDecimal max = decimal(payload.get("max"));
		BigDecimal step = decimal(payload.get("step"));
		if (max.compareTo(min) <= 0) {
			return AnswerProblem.payload("max " + plain(max) + " must be greater than min " + plain(min));
		}
		BigDecimal range = max.subtract(min);
		if (range.compareTo(step.multiply(BigDecimal.valueOf(MAX_STEPS))) > 0) {
			return AnswerProblem.payload("the line from " + plain(min) + " to " + plain(max) + " in steps of "
					+ plain(step) + " has more than " + MAX_STEPS + " steps; a line has at most " + MAX_STEPS
					+ " steps (" + (MAX_STEPS + 1) + " ticks)");
		}
		if (range.remainder(step).signum() != 0) {
			BigDecimal steps = range.divide(step, MathContext.DECIMAL64);
			return AnswerProblem.payload("the line from " + plain(min) + " to " + plain(max) + " is not a whole number"
					+ " of steps of " + plain(step) + ": (max - min) / step is " + plain(steps)
					+ ", so the browser would not end the line on max");
		}
		// the browser draws Math.round((max - min) / step) steps in doubles (lastTickIndex)
		BigDecimal exactSteps = range.divide(step);
		double browserSteps = (browserDouble(payload.get("max")) - browserDouble(payload.get("min")))
				/ browserDouble(payload.get("step"));
		if (!Double.isFinite(browserSteps) || BigDecimal.valueOf(jsRound(browserSteps)).compareTo(exactSteps) != 0) {
			return AnswerProblem.payload("the line from " + plain(min) + " to " + plain(max) + " has " + plain(exactSteps)
					+ " steps of " + plain(step) + ", but the browser computes (max - min) / step as " + browserSteps
					+ " in double arithmetic");
		}
		return null;
	}

	/** Whether a value's decimal exponent or scale is beyond {@link #MAX_EXPONENT}. */
	private static boolean outOfScale(BigDecimal value) {
		long exponent = (long) value.precision() - value.scale() - 1;
		return Math.abs(exponent) > MAX_EXPONENT || Math.abs((long) value.scale()) > MAX_EXPONENT;
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
			return;
		}
		long browserTick = canonicalTick(payload, answer.asDouble());
		if (BigInteger.valueOf(browserTick).compareTo(tick.toBigInteger()) != 0) {
			problems.add("the browser snaps stored answer " + plain(stored) + " to tick " + browserTick
					+ " in double arithmetic, but its exact tick is " + tick.toBigInteger() + " (min " + plain(min)
					+ ", step " + plain(step) + ")");
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

	/**
	 * The double the browser's {@code JSON.parse} reads: the nearest double, {@code Infinity} beyond
	 * the double range and 0 below it ({@link JsonNode#asDouble()} throws for those instead).
	 */
	private static double browserDouble(JsonNode number) {
		if (number.isDouble() || number.isFloat()) {
			return number.doubleValue(); // already a double, possibly infinite
		}
		return decimal(number).doubleValue();
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
