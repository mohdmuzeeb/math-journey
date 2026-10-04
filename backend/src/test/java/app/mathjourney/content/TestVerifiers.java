package app.mathjourney.content;

import java.util.List;
import java.util.Map;

/** The real answer verifiers, as Spring wires them, for tests that build the loader or catalog by hand. */
final class TestVerifiers {

	private TestVerifiers() {
	}

	static List<AnswerVerifier> list() {
		return List.of(new NumberLineVerifier());
	}

	static Map<String, AnswerVerifier> byKind() {
		return ContentLoader.byKind(list());
	}

}
