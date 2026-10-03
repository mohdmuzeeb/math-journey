package app.mathjourney.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import app.mathjourney.content.ConceptCatalog;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/concepts")
class ConceptController {

	private final ConceptCatalog catalog;

	ConceptController(ConceptCatalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/{id}")
	JsonNode concept(@PathVariable String id) {
		return this.catalog.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No concept " + id));
	}

}
