package app.mathjourney.content;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.keyword.AnnotationKeyword;
import com.networknt.schema.path.PathType;

import tools.jackson.core.JacksonException;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loads every content file under a content root and validates it (AD-5, AD-12, AD-13):
 * <ol>
 * <li>each concept file against {@code concept.schema.json}, and each item's payload and answers
 * (and its walkthrough's {@code similar}) against its kind's schema ({@code $defs.payload},
 * {@code $defs.answer});</li>
 * <li>per-file checks JSON Schema cannot express: id equals the file name, item ids are
 * {@code <conceptId>#...} and unique, AD-12 role minimums;</li>
 * <li>{@code curriculum.json} against {@code curriculum.schema.json}, then cross-checked against
 * the concept files.</li>
 * </ol>
 * It never throws for bad content: every problem is collected into {@link Result#errors()}.
 * Schemas always come from {@code classpath:content/schemas/}, whatever the content root.
 */
final class ContentLoader {

	static final String SCHEMA_LOCATION = "classpath:content/schemas/*.schema.json";

	/** Base IRI the schemas are registered under; matches their {@code $id}s. */
	static final String SCHEMA_BASE = "https://mathjourney.app/content/schemas/";

	static final String CURRICULUM_FILE = "curriculum.json";

	static final String CONCEPTS_DIR = "concepts/";

	/** AD-12: minimum non-retired items per role in a non-retired concept. */
	static final Map<String, Integer> ROLE_MINIMUMS = rolesInOrder();

	private final String contentRoot;

	private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	private final JsonMapper jsonMapper = JsonMapper.builder()
		.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
		.build();

	/**
	 * @param contentRoot a Spring resource location such as {@code classpath:content} or
	 * {@code file:/path/to/content}
	 */
	ContentLoader(String contentRoot) {
		this.contentRoot = contentRoot.endsWith("/") ? contentRoot.substring(0, contentRoot.length() - 1)
				: contentRoot;
	}

	/** The loaded concepts by id (only when there are no errors at all) and every error found. */
	record Result(Map<String, CatalogConcept> concepts, List<ContentError> errors) {
	}

	Result load() {
		List<ContentError> errors = new ArrayList<>();
		SchemaRegistry registry = schemaRegistry(errors);
		if (!errors.isEmpty()) {
			return new Result(Map.of(), List.copyOf(errors));
		}
		Schema conceptSchema = registry.getSchema(SchemaLocation.of(SCHEMA_BASE + "concept.schema.json"));
		Schema curriculumSchema = registry.getSchema(SchemaLocation.of(SCHEMA_BASE + "curriculum.schema.json"));

		Set<String> conceptFileIds = new HashSet<>();
		Map<String, CatalogConcept> valid = new LinkedHashMap<>();
		for (Resource resource : conceptResources(errors)) {
			String fileName = resource.getFilename();
			String fileId = fileName.substring(0, fileName.length() - ".json".length());
			conceptFileIds.add(fileId);
			String file = CONCEPTS_DIR + fileName;
			int before = errors.size();
			JsonNode json = read(resource, file, errors);
			if (json == null) {
				continue;
			}
			schemaErrors(conceptSchema, json, file, "$", errors);
			if (errors.size() > before) {
				continue;
			}
			checkKinds(registry, json, file, errors);
			checkConcept(json, fileId, file, errors);
			if (errors.size() == before) {
				valid.put(fileId, new CatalogConcept(fileId, json.get("land").asString(),
						json.get("reviewed").asBoolean(), json.get("retired").asBoolean(), json));
			}
		}

		checkCurriculum(curriculumSchema, conceptFileIds, valid, errors);
		return errors.isEmpty() ? new Result(Map.copyOf(valid), List.of())
				: new Result(Map.of(), List.copyOf(errors));
	}

	// --- schemas ---

	private SchemaRegistry schemaRegistry(List<ContentError> errors) {
		Map<String, String> schemas = new HashMap<>();
		try {
			for (Resource resource : this.resolver.getResources(SCHEMA_LOCATION)) {
				try (InputStream in = resource.getInputStream()) {
					schemas.put(SCHEMA_BASE + resource.getFilename(),
							new String(in.readAllBytes(), StandardCharsets.UTF_8));
				}
			}
		}
		catch (IOException ex) {
			errors.add(new ContentError("schemas/", "$", "cannot read the content schemas: " + ex.getMessage()));
			return null;
		}
		for (String required : List.of("concept.schema.json", "curriculum.schema.json")) {
			if (!schemas.containsKey(SCHEMA_BASE + required)) {
				errors.add(new ContentError("schemas/" + required, "$", "schema not found on the classpath"));
			}
		}
		SchemaRegistryConfig config = SchemaRegistryConfig.builder().pathType(PathType.JSON_PATH).build();
		// Draft 2020-12, plus the kind schemas' "x-equivalence" annotation (AD-3) as a known keyword
		Dialect dialect = Dialect.builder(Dialects.getDraft202012())
			.keyword(new AnnotationKeyword("x-equivalence"))
			.build();
		return SchemaRegistry.withDefaultDialect(dialect,
				(builder) -> builder.schemas(schemas).schemaRegistryConfig(config));
	}

	/**
	 * Validates {@code node} (found at {@code basePath} in {@code file}) and records each error with
	 * its full path in the file.
	 */
	private static void schemaErrors(Schema schema, JsonNode node, String file, String basePath,
			List<ContentError> errors) {
		for (com.networknt.schema.Error error : schema.validate(node)) {
			String relative = error.getInstanceLocation().toString();
			errors.add(new ContentError(file, basePath + relative.substring(1), error.getMessage()));
		}
	}

	// --- concept files ---

	private List<Resource> conceptResources(List<ContentError> errors) {
		Resource[] resources;
		try {
			resources = this.resolver.getResources(this.contentRoot + "/" + CONCEPTS_DIR + "*.json");
		}
		catch (FileNotFoundException ex) {
			return List.of(); // no concepts folder: zero concepts
		}
		catch (IOException ex) {
			errors.add(new ContentError(CONCEPTS_DIR, "$", "cannot list concept files: " + ex.getMessage()));
			return List.of();
		}
		return Arrays.stream(resources)
			.filter(Resource::exists)
			.sorted(Comparator.comparing(Resource::getFilename))
			.toList();
	}

	private JsonNode read(Resource resource, String file, List<ContentError> errors) {
		try (InputStream in = resource.getInputStream()) {
			return this.jsonMapper.readTree(in);
		}
		catch (JacksonException ex) {
			TokenStreamLocation location = ex.getLocation();
			String where = (location != null && location.getLineNr() > 0)
					? " at line " + location.getLineNr() + ", column " + location.getColumnNr() : "";
			errors.add(new ContentError(file, "$", "malformed JSON" + where + ": " + ex.getOriginalMessage()));
		}
		catch (IOException ex) {
			errors.add(new ContentError(file, "$", "cannot read file: " + ex.getMessage()));
		}
		return null;
	}

	/** Validates each item's payload and answers, and its similar's, against the kind's {@code $defs}. */
	private static void checkKinds(SchemaRegistry registry, JsonNode concept, String file,
			List<ContentError> errors) {
		JsonNode items = concept.get("items");
		for (int i = 0; i < items.size(); i++) {
			JsonNode item = items.get(i);
			String itemPath = "$.items[" + i + "]";
			String kind = item.get("kind").asString();
			Schema payload;
			Schema answer;
			try {
				payload = registry.getSchema(SchemaLocation.of(SCHEMA_BASE + kind + ".schema.json#/$defs/payload"));
				answer = registry.getSchema(SchemaLocation.of(SCHEMA_BASE + kind + ".schema.json#/$defs/answer"));
			}
			catch (RuntimeException ex) {
				errors.add(new ContentError(file, itemPath + ".kind",
						"no usable schema for kind \"" + kind + "\": " + ex.getMessage()));
				continue;
			}
			schemaErrors(payload, item.get("payload"), file, itemPath + ".payload", errors);
			schemaErrors(answer, item.get("answer"), file, itemPath + ".answer", errors);
			JsonNode accepted = item.get("acceptedAnswers");
			if (accepted != null) {
				for (int a = 0; a < accepted.size(); a++) {
					schemaErrors(answer, accepted.get(a), file, itemPath + ".acceptedAnswers[" + a + "]", errors);
				}
			}
			JsonNode similar = item.get("walkthrough").get("similar");
			String similarPath = itemPath + ".walkthrough.similar";
			schemaErrors(payload, similar.get("payload"), file, similarPath + ".payload", errors);
			schemaErrors(answer, similar.get("answer"), file, similarPath + ".answer", errors);
		}
	}

	/** Per-file checks JSON Schema cannot express (AD-10, AD-12). */
	private static void checkConcept(JsonNode concept, String fileId, String file, List<ContentError> errors) {
		String id = concept.get("id").asString();
		if (!id.equals(fileId)) {
			errors.add(new ContentError(file, "$.id",
					"id \"" + id + "\" must equal the file name \"" + fileId + "\""));
		}
		String prefix = id + "#";
		Set<String> itemIds = new HashSet<>();
		Map<String, Integer> roleCounts = new HashMap<>();
		JsonNode items = concept.get("items");
		for (int i = 0; i < items.size(); i++) {
			JsonNode item = items.get(i);
			String itemId = item.get("id").asString();
			String path = "$.items[" + i + "].id";
			if (!itemId.startsWith(prefix) || itemId.length() == prefix.length()) {
				errors.add(new ContentError(file, path,
						"item id \"" + itemId + "\" must be \"" + prefix + "<slug>\""));
			}
			if (!itemIds.add(itemId)) {
				errors.add(new ContentError(file, path, "duplicate item id \"" + itemId + "\""));
			}
			JsonNode retired = item.get("retired");
			if (retired == null || !retired.asBoolean()) {
				roleCounts.merge(item.get("role").asString(), 1, Integer::sum);
			}
		}
		if (!concept.get("retired").asBoolean()) {
			ROLE_MINIMUMS.forEach((role, minimum) -> {
				int count = roleCounts.getOrDefault(role, 0);
				if (count < minimum) {
					errors.add(new ContentError(file, "$.items", "needs at least " + minimum + " non-retired " + role
							+ " item(s), found " + count));
				}
			});
		}
	}

	// --- curriculum ---

	private void checkCurriculum(Schema curriculumSchema, Set<String> conceptFileIds,
			Map<String, CatalogConcept> valid, List<ContentError> errors) {
		Resource resource = this.resolver.getResource(this.contentRoot + "/" + CURRICULUM_FILE);
		if (!resource.exists()) {
			errors.add(new ContentError(CURRICULUM_FILE, "$", "file not found under " + this.contentRoot));
			return;
		}
		int before = errors.size();
		JsonNode curriculum = read(resource, CURRICULUM_FILE, errors);
		if (curriculum == null) {
			return;
		}
		schemaErrors(curriculumSchema, curriculum, CURRICULUM_FILE, "$", errors);
		if (errors.size() > before) {
			return;
		}

		Map<String, Integer> listings = new HashMap<>();
		JsonNode lands = curriculum.get("lands");
		for (int i = 0; i < lands.size(); i++) {
			JsonNode land = lands.get(i);
			String landId = land.get("id").asString();
			JsonNode concepts = land.get("concepts");
			for (int j = 0; j < concepts.size(); j++) {
				String id = concepts.get(j).asString();
				String path = "$.lands[" + i + "].concepts[" + j + "]";
				if (!listed(id, path, conceptFileIds, listings, errors)) {
					continue;
				}
				CatalogConcept concept = valid.get(id);
				if (concept == null) {
					continue; // the concept file has its own errors
				}
				if (concept.retired()) {
					errors.add(new ContentError(CURRICULUM_FILE, path,
							"concept \"" + id + "\" is retired and must not be listed in a land"));
				}
				if (!concept.land().equals(landId)) {
					errors.add(new ContentError(CONCEPTS_DIR + id + ".json", "$.land", "land \"" + concept.land()
							+ "\" does not match the land listing it in " + CURRICULUM_FILE + " (" + landId + ", "
							+ path + ")"));
				}
			}
		}
		JsonNode prerequisites = curriculum.get("prerequisites");
		for (int k = 0; k < prerequisites.size(); k++) {
			listed(prerequisites.get(k).asString(), "$.prerequisites[" + k + "]", conceptFileIds, listings, errors);
		}

		valid.values()
			.stream()
			.filter((concept) -> !concept.retired() && !listings.containsKey(concept.id()))
			.forEach((concept) -> errors.add(new ContentError(CONCEPTS_DIR + concept.id() + ".json", "$",
					"concept \"" + concept.id() + "\" is not listed in " + CURRICULUM_FILE
							+ " (lands or prerequisites)")));
	}

	/** Records one curriculum listing; false (with an error) when the id has no concept file or is a repeat. */
	private static boolean listed(String id, String path, Set<String> conceptFileIds, Map<String, Integer> listings,
			List<ContentError> errors) {
		if (!conceptFileIds.contains(id)) {
			errors.add(new ContentError(CURRICULUM_FILE, path,
					"concept \"" + id + "\" has no file " + CONCEPTS_DIR + id + ".json"));
			return false;
		}
		if (listings.merge(id, 1, Integer::sum) > 1) {
			errors.add(new ContentError(CURRICULUM_FILE, path,
					"concept \"" + id + "\" is listed more than once (lands and prerequisites together)"));
			return false;
		}
		return true;
	}

	private static Map<String, Integer> rolesInOrder() {
		Map<String, Integer> minimums = new LinkedHashMap<>();
		minimums.put("discover", 1);
		minimums.put("practice", 4);
		minimums.put("review", 3);
		minimums.put("challenge", 1);
		return java.util.Collections.unmodifiableMap(minimums);
	}

}
