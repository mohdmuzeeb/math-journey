package app.mathjourney.content;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
 * <li>each item's answers, and its walkthrough's {@code similar} answer, by the kind's
 * {@link AnswerVerifier} (AD-3), once their schema checks pass;</li>
 * <li>{@code curriculum.json} against {@code curriculum.schema.json}, then cross-checked against
 * the concept files.</li>
 * </ol>
 * It also checks that the kind registry is closed (AD-6): the {@code kind} enum in
 * {@code concept.schema.json}, the {@code <kind>.schema.json} files and the verifiers name the same
 * kinds.
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

	/** Schema files that are not activity kinds. */
	static final Set<String> NON_KIND_SCHEMAS = Set.of("concept.schema.json", "curriculum.schema.json");

	static final String KIND_SCHEMA_SUFFIX = ".schema.json";

	private final String contentRoot;

	private final Map<String, AnswerVerifier> verifiers;

	private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	private final JsonMapper jsonMapper = JsonMapper.builder()
		.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
		// AD-3: decimals keep their exact JSON text value, so answer verification is exact
		.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
		.build();

	/**
	 * @param contentRoot a Spring resource location such as {@code classpath:content} or
	 * {@code file:/path/to/content}
	 * @param verifiers the answer verifiers by kind (see {@link #byKind})
	 */
	ContentLoader(String contentRoot, Map<String, AnswerVerifier> verifiers) {
		this.contentRoot = contentRoot.endsWith("/") ? contentRoot.substring(0, contentRoot.length() - 1)
				: contentRoot;
		this.verifiers = Map.copyOf(verifiers);
	}

	/** Indexes verifiers by their kind; a kind with two verifiers is a programming error. */
	static Map<String, AnswerVerifier> byKind(Collection<AnswerVerifier> verifiers) {
		Map<String, AnswerVerifier> byKind = new HashMap<>();
		for (AnswerVerifier verifier : verifiers) {
			AnswerVerifier previous = byKind.putIfAbsent(verifier.kind(), verifier);
			if (previous != null) {
				throw new IllegalStateException("two AnswerVerifiers for kind \"" + verifier.kind() + "\": "
						+ previous.getClass().getName() + " and " + verifier.getClass().getName());
			}
		}
		return byKind;
	}

	/** The loaded concepts by id (only when there are no errors at all) and every error found. */
	record Result(Map<String, CatalogConcept> concepts, List<ContentError> errors) {
	}

	Result load() {
		List<ContentError> errors = new ArrayList<>();
		Map<String, String> schemaFiles = schemaFiles(errors);
		if (!errors.isEmpty()) {
			return new Result(Map.of(), List.copyOf(errors));
		}
		checkKindRegistry(schemaFiles, errors);
		SchemaRegistry registry = schemaRegistry(schemaFiles);
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

	/** Every schema file's text by file name; adds an error when one cannot be read or is missing. */
	private Map<String, String> schemaFiles(List<ContentError> errors) {
		Map<String, String> schemas = new TreeMap<>();
		try {
			for (Resource resource : this.resolver.getResources(SCHEMA_LOCATION)) {
				try (InputStream in = resource.getInputStream()) {
					schemas.put(resource.getFilename(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
				}
			}
		}
		catch (IOException ex) {
			errors.add(new ContentError("schemas/", "$", "cannot read the content schemas: " + ex.getMessage()));
			return Map.of();
		}
		for (String required : NON_KIND_SCHEMAS.stream().sorted().toList()) {
			if (!schemas.containsKey(required)) {
				errors.add(new ContentError("schemas/" + required, "$", "schema not found on the classpath"));
			}
		}
		return schemas;
	}

	/**
	 * AD-6: the kinds in {@code concept.schema.json}'s {@code kind} enum, the {@code <kind>.schema.json}
	 * files and the verifiers must be the same set. Each kind missing from any of them is an error.
	 */
	private void checkKindRegistry(Map<String, String> schemaFiles, List<ContentError> errors) {
		String file = "schemas/concept.schema.json";
		String enumPath = "$.$defs.item.properties.kind.enum";
		JsonNode concept;
		try {
			concept = this.jsonMapper.readTree(schemaFiles.get("concept.schema.json"));
		}
		catch (JacksonException ex) {
			errors.add(new ContentError(file, "$", "malformed JSON: " + ex.getOriginalMessage()));
			return;
		}
		JsonNode kindEnum = concept.at("/$defs/item/properties/kind/enum");
		if (!kindEnum.isArray()) {
			errors.add(new ContentError(file, enumPath, "the item kind must be an enum of activity kinds (AD-6)"));
			return;
		}
		Set<String> inEnum = new TreeSet<>();
		kindEnum.forEach((kind) -> inEnum.add(kind.asString()));
		Set<String> withSchema = new TreeSet<>();
		schemaFiles.keySet()
			.stream()
			.filter((name) -> !NON_KIND_SCHEMAS.contains(name))
			.forEach((name) -> withSchema.add(name.substring(0, name.length() - KIND_SCHEMA_SUFFIX.length())));
		Set<String> withVerifier = new TreeSet<>(this.verifiers.keySet());

		Set<String> all = new TreeSet<>(inEnum);
		all.addAll(withSchema);
		all.addAll(withVerifier);
		for (String kind : all) {
			List<String> missing = new ArrayList<>();
			if (!inEnum.contains(kind)) {
				missing.add("an entry in concept.schema.json's kind enum");
			}
			if (!withSchema.contains(kind)) {
				missing.add("a schemas/" + kind + KIND_SCHEMA_SUFFIX + " file");
			}
			if (!withVerifier.contains(kind)) {
				missing.add("an AnswerVerifier bean");
			}
			if (!missing.isEmpty()) {
				errors.add(new ContentError(file, enumPath, "activity kind \"" + kind
						+ "\" is not registered everywhere (AD-6); it has no " + String.join(", no ", missing)));
			}
		}
	}

	/**
	 * The registry the loader validates with, holding {@code schemaFiles} (file name to text) under
	 * {@link #SCHEMA_BASE}. Package-private so tests validate exactly as the loader does.
	 */
	static SchemaRegistry schemaRegistry(Map<String, String> schemaFiles) {
		Map<String, String> schemas = new HashMap<>();
		schemaFiles.forEach((name, text) -> schemas.put(SCHEMA_BASE + name, text));
		SchemaRegistryConfig config = SchemaRegistryConfig.builder().pathType(PathType.JSON_PATH).build();
		// Draft 2020-12, plus two known annotation keywords: the kind schemas' "x-equivalence" (AD-3),
		// and "tsType", which only steers the generated TypeScript types (gen-content-types.mjs)
		Dialect dialect = Dialect.builder(Dialects.getDraft202012())
			.keyword(new AnnotationKeyword("x-equivalence"))
			.keyword(new AnnotationKeyword("tsType"))
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

	/**
	 * Validates each item's payload and answers, and its similar's, against the kind's {@code $defs};
	 * then, where those passed, verifies the answers with the kind's {@link AnswerVerifier} (AD-3).
	 */
	private void checkKinds(SchemaRegistry registry, JsonNode concept, String file,
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
			String itemId = item.get("id").asString();
			AnswerVerifier verifier = this.verifiers.get(kind); // null: reported by checkKindRegistry

			int before = errors.size();
			schemaErrors(payload, item.get("payload"), file, itemPath + ".payload", errors);
			schemaErrors(answer, item.get("answer"), file, itemPath + ".answer", errors);
			JsonNode accepted = item.get("acceptedAnswers");
			if (accepted != null) {
				for (int a = 0; a < accepted.size(); a++) {
					schemaErrors(answer, accepted.get(a), file, itemPath + ".acceptedAnswers[" + a + "]", errors);
				}
			}
			if (verifier != null && errors.size() == before) {
				verifyAnswers(verifier, item.get("payload"), item.get("answer"), accepted, itemId, file, itemPath,
						errors);
			}

			JsonNode similar = item.get("walkthrough").get("similar");
			String similarPath = itemPath + ".walkthrough.similar";
			before = errors.size();
			schemaErrors(payload, similar.get("payload"), file, similarPath + ".payload", errors);
			schemaErrors(answer, similar.get("answer"), file, similarPath + ".answer", errors);
			if (verifier != null && errors.size() == before) {
				verifyAnswers(verifier, similar.get("payload"), similar.get("answer"), null, itemId, file,
						similarPath, errors);
			}
		}
	}

	/**
	 * Runs one verification. Each problem is recorded at {@code <basePath>.payload},
	 * {@code <basePath>.answer} or {@code <basePath>.acceptedAnswers}, by its field, with the message
	 * prefixed by the item id.
	 */
	private static void verifyAnswers(AnswerVerifier verifier, JsonNode payload, JsonNode answer, JsonNode accepted,
			String itemId, String file, String basePath, List<ContentError> errors) {
		String prefix = "item \"" + itemId + "\": ";
		for (AnswerProblem problem : verifier.verify(payload, answer, accepted)) {
			String path = basePath + switch (problem.field()) {
				case PAYLOAD -> ".payload";
				case ANSWER -> ".answer";
				case ACCEPTED_ANSWERS -> ".acceptedAnswers";
			};
			errors.add(new ContentError(file, path, prefix + problem.message()));
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
