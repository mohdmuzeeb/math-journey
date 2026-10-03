package app.mathjourney.content;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Loads the demo concepts from {@code classpath:content/demo/concepts/*.json} at startup.
 * Unreadable JSON stops the application from starting.
 */
@Service
@Profile("demo")
class DemoConceptCatalog implements ConceptCatalog {

	static final String LOCATION = "classpath:content/demo/concepts/*.json";

	private static final Logger log = LoggerFactory.getLogger(DemoConceptCatalog.class);

	private final Map<String, JsonNode> conceptsById;

	DemoConceptCatalog(ObjectMapper objectMapper) {
		this.conceptsById = Map.copyOf(load(objectMapper, new PathMatchingResourcePatternResolver()));
		log.info("[content] loaded {} demo concept(s): {}", this.conceptsById.size(), this.conceptsById.keySet());
	}

	@Override
	public Optional<JsonNode> findById(String id) {
		return Optional.ofNullable(this.conceptsById.get(id));
	}

	private static Map<String, JsonNode> load(ObjectMapper objectMapper, ResourcePatternResolver resolver) {
		Resource[] resources;
		try {
			resources = resolver.getResources(LOCATION);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot list demo concepts at " + LOCATION, ex);
		}
		Map<String, JsonNode> concepts = new HashMap<>();
		for (Resource resource : resources) {
			String name = resource.getFilename();
			JsonNode concept;
			try (InputStream in = resource.getInputStream()) {
				concept = objectMapper.readTree(in);
			}
			catch (IOException | JacksonException ex) {
				throw new IllegalStateException("Unreadable demo concept file " + name + ": " + ex.getMessage(), ex);
			}
			JsonNode id = concept.get("id");
			if (id == null || !id.isString() || id.asString().isBlank()) {
				throw new IllegalStateException("Demo concept file " + name + " has no string \"id\"");
			}
			if (concepts.put(id.asString(), concept) != null) {
				throw new IllegalStateException("Duplicate demo concept id \"" + id.asString() + "\" in " + name);
			}
		}
		return concepts;
	}

}
