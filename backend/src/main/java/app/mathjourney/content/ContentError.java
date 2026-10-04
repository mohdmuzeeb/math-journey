package app.mathjourney.content;

/**
 * One content problem found at startup.
 *
 * @param file the file, relative to the content root (e.g. {@code concepts/x.json})
 * @param jsonPath the JSON path inside that file (e.g. {@code $.items[2].hint})
 * @param message what is wrong
 */
public record ContentError(String file, String jsonPath, String message) {

	@Override
	public String toString() {
		return "[content] " + this.file + " " + this.jsonPath + ": " + this.message;
	}

}
