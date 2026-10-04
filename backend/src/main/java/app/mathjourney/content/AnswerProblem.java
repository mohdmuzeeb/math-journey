package app.mathjourney.content;

/**
 * One problem an {@link AnswerVerifier} found with a node's stored answers (AD-3).
 * @param field which stored field the problem belongs to; the content loader records it at
 * {@code <node>.answer} or {@code <node>.acceptedAnswers}
 * @param message the human-readable description of the problem
 */
public record AnswerProblem(Field field, String message) {

	/** The stored field a problem is attributed to. */
	public enum Field {

		/** The canonical {@code answer}. */
		ANSWER,

		/** The {@code acceptedAnswers} alternatives. */
		ACCEPTED_ANSWERS

	}

	static AnswerProblem answer(String message) {
		return new AnswerProblem(Field.ANSWER, message);
	}

	static AnswerProblem acceptedAnswers(String message) {
		return new AnswerProblem(Field.ACCEPTED_ANSWERS, message);
	}

}
