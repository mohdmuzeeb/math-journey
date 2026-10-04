package app.mathjourney.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import app.mathjourney.content.ConceptCatalog;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/concepts")
class ConceptController {

	private final ConceptCatalog catalog;

	private final ObjectMapper objectMapper;

	ConceptController(ConceptCatalog catalog, ObjectMapper objectMapper) {
		this.catalog = catalog;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/{id}")
	ConceptResponse concept(@PathVariable String id) {
		return this.catalog.findById(id)
			.map((node) -> this.objectMapper.treeToValue(node, ConceptResponse.class))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No concept " + id));
	}

}
