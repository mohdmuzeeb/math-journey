package app.mathjourney.content;

import java.util.List;
import java.util.stream.Collectors;

/** Thrown at startup when any content file fails validation; lists every problem found (AD-5). */
public class ContentValidationException extends RuntimeException {

	private final List<ContentError> errors;

	public ContentValidationException(List<ContentError> errors) {
		super(errors.size() + " content error(s); the app refuses to start:\n"
				+ errors.stream().map(ContentError::toString).collect(Collectors.joining("\n")));
		this.errors = List.copyOf(errors);
	}

	public List<ContentError> getErrors() {
		return this.errors;
	}

}
