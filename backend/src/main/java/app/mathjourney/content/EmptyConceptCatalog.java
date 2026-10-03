package app.mathjourney.content;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

/** Default catalog for non-demo runs: no lessons yet. */
@Service
@Profile("!demo")
class EmptyConceptCatalog implements ConceptCatalog {

	@Override
	public Optional<JsonNode> findById(String id) {
		return Optional.empty();
	}

}
