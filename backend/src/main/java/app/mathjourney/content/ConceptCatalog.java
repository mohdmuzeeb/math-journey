package app.mathjourney.content;

import java.util.Optional;

/**
 * Read-only lookup of the concept files, all of which passed startup validation (AD-5).
 */
public interface ConceptCatalog {

	Optional<CatalogConcept> findById(String id);

}
