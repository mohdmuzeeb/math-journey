package app.mathjourney.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import app.mathjourney.content.ConceptCatalog;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

@RestController
@RequestMapping("/api/concepts")
class ConceptController {

	private final ConceptCatalog catalog;

	/** The concept envelope has more fields (land, roles, hints, ...) than this contract exposes. */
	private final ObjectReader responseReader;

	ConceptController(ConceptCatalog catalog, ObjectMapper objectMapper) {
		this.catalog = catalog;
		this.responseReader = objectMapper.readerFor(ConceptResponse.class)
			.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

	@GetMapping("/{id}")
	ConceptResponse concept(@PathVariable String id) {
		return this.catalog.findById(id)
			.map((concept) -> this.responseReader.<ConceptResponse>readValue(concept.json()))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No concept " + id));
	}

}
