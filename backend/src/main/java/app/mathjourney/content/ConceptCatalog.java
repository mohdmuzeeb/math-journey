package app.mathjourney.content;

import java.util.Optional;

import tools.jackson.databind.JsonNode;

/**
 * Read-only lookup of concept files by id. Concepts are opaque JSON trees here; their shape
 * is defined by the content schemas (AD-5, AD-12), not by Java types.
 */
public interface ConceptCatalog {

	Optional<JsonNode> findById(String id);

}
