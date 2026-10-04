package app.mathjourney.content;

import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * Verifies the stored answers of one activity kind (AD-3, AD-6).
 * <p>
 * The browser only <em>compares</em> a learner's canonical response with the stored answer; it does
 * no math. So a wrong stored answer would mark a correct learner wrong. A verifier closes that gap:
 * it <strong>computes</strong> the answer from the problem in the payload, using exact arithmetic,
 * and reports every way the stored answer (and any accepted alternatives) disagrees with it.
 * <p>
 * The content loader runs the verifier of the item's kind on every item and on every walkthrough's
 * {@code similar} problem, after their schema checks pass, so the arguments always have the shapes
 * the kind's schema defines. Any problem stops startup (AD-5).
 * <p>
 * The kind registry is closed (AD-6): exactly one verifier bean exists per kind in
 * {@code concept.schema.json}'s {@code kind} enum, and per {@code <kind>.schema.json} file.
 */
public interface AnswerVerifier {

	/** The kebab-case activity kind this verifier checks, e.g. {@code number-line}. */
	String kind();

	/**
	 * Computes the answer from {@code payload} and checks the stored answers against it.
	 * <p>
	 * The loader calls it once per item and once per walkthrough {@code similar}. Each problem
	 * names the field it belongs to, which decides the path it is recorded at.
	 * @param payload the problem, valid against the kind's {@code $defs.payload}
	 * @param answer the stored canonical answer, valid against the kind's {@code $defs.answer}
	 * @param acceptedAnswers the stored alternatives (an array), or null when there are none, as for
	 * a walkthrough's {@code similar}
	 * @return one problem per disagreement, each tagged with its field; empty when the stored
	 * answers are correct
	 */
	List<AnswerProblem> verify(JsonNode payload, JsonNode answer, JsonNode acceptedAnswers);

}
