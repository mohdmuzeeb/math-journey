package app.mathjourney.content;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Loads and validates all content under {@code app.content-root} at startup. Any error stops the
 * application from starting (AD-5): each one is logged with its file and JSON path first.
 */
@Service
class ContentCatalog implements ConceptCatalog {

	private static final Logger log = LoggerFactory.getLogger(ContentCatalog.class);

	private final Map<String, CatalogConcept> conceptsById;

	ContentCatalog(@Value("${app.content-root}") String contentRoot) {
		ContentLoader.Result result = new ContentLoader(contentRoot).load();
		if (!result.errors().isEmpty()) {
			result.errors().forEach((error) -> log.error("{}", error));
			throw new ContentValidationException(result.errors());
		}
		this.conceptsById = result.concepts();
		log.info("[content] loaded {} concept(s) from {}: {}", this.conceptsById.size(), contentRoot,
				this.conceptsById.keySet());
	}

	@Override
	public Optional<CatalogConcept> findById(String id) {
		return Optional.ofNullable(this.conceptsById.get(id));
	}

}
