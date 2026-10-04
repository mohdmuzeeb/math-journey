---
title: 'Concept and curriculum schemas, enforced at startup'
type: 'feature'
ticket: '4'
created: '2026-10-03'
status: 'built'
baseline_revision: '9865e37a6737070468e02c4fa92e7fa82d8551a4'
route: 'full'
route_source: 'auto'
risk: 'medium'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/initiative-math-learning-app/architecture-math-learning-app/architecture-math-learning-app.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Concept files are loaded as unchecked JSON. A lesson with a missing field, too few items, or a curriculum entry with no file would reach the learner. AD-5, AD-12 and AD-13 require the app to refuse to start instead (CAP-11).

**Approach:** Add JSON Schemas under `content/schemas/` for the concept envelope, the number-line kind and the curriculum. Add `content/curriculum.json` with five empty lands and a demo curriculum, and replace the demo and empty catalogs with one loader. At startup the loader validates every file, enforces the AD-12 minimums and cross-checks the curriculum. Any failure stops startup, with a log line naming the file and the JSON path. The demo concept moves to the full envelope, and the catalog exposes `reviewed`.

## Boundaries & Constraints

**Always:**
- **Validator:** `com.networknt:json-schema-validator` 3.0.8 (Jackson 3), draft 2020-12.
- **Errors:** collect every error across all files, log each one as `[content] <file> <jsonPath>: <message>`, then fail startup with one exception that lists them all.
- **Content root:** set by `app.content-root`. The default is `classpath:content`, and the `demo` profile uses `classpath:content/demo`. Schemas always load from `classpath:content/schemas/`.
- **Demo concept:** meets the AD-12 minimums, and its first item stays "Drag the point to 3/4" (answer 0.75, min 0, max 2, step 0.25). Every stored answer in it is mathematically correct.
- **API:** the contract (`ConceptResponse`) is unchanged.

**Never:**
- No answer computation or `AnswerVerifier`, and no fixtures (entry 5).
- No generated content TypeScript types (entry 6). The hand-written `NumberLinePayload` is just updated to the new payload shape.
- No session or review-gate logic (epic 2).
- No real lesson content. The real `curriculum.json` stays empty.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Valid demo | `demo` profile | starts; `/api/concepts/demo-number-line` → 200, first item 3/4; catalog reports `reviewed` | — |
| Valid real, empty | default profile | starts with 0 concepts and 5 empty lands | — |
| Missing required field | an item without `hint` | startup fails | names the file and `$.items[n]` |
| Too few items | 3 `practice` items | startup fails | names the file and the role count |
| Bad kind payload | `payload.step` is a string, or the `similar` payload is invalid | startup fails | names the file and the payload path |
| Unknown kind | `kind: "abacus"` | startup fails | names the file and `$.items[n].kind` |
| Unknown curriculum id | a land lists an id with no file | startup fails | names the curriculum file and `$.lands[i].concepts[j]` |
| Orphan concept | a non-retired concept file is not in the curriculum | startup fails | names the concept file |
| Id mismatches | id ≠ file name; an item id not prefixed `<conceptId>#`; a duplicate item id; `land` ≠ the land listing it | startup fails | names the file and the path |
| Malformed JSON | a truncated file | startup fails | names the file |
| Several broken files | 2 bad files | both errors logged before the failure | — |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/app/mathjourney/content/ConceptCatalog.java` -- the public interface: `Optional<JsonNode> findById`. Change it to return the public record `CatalogConcept(String id, String land, boolean reviewed, boolean retired, JsonNode json)`.
- `DemoConceptCatalog.java`, `EmptyConceptCatalog.java` (same package) -- profile-split catalogs. Delete both. One `ContentCatalog` (package-private, `@Service`) replaces them, and the loading code is reused from `DemoConceptCatalog`.
- `backend/src/main/java/app/mathjourney/web/ConceptController.java` -- maps `catalog.findById(id)` with `treeToValue` into `ConceptResponse`. Map `CatalogConcept.json()` instead. The envelope adds fields `ConceptResponse` doesn't have, and the existing `ConceptControllerTest` catches it if unknown properties fail.
- `content/demo/concepts/demo-number-line.json` -- today `{id, title, items:[{id, kind, payload{prompt,min,max,step}, answer}]}`.
- `backend/pom.xml` -- `content/` is already packaged at `classpath:content/` (AD-17). Add only the validator dependency.
- `frontend/src/numberLine.ts` (`NumberLinePayload`), `App.test.tsx:26` (mock payload) -- add `target`. `App.tsx` keeps rendering `items[0]`.
- Tests: `ConceptControllerTest` (demo), `ConceptControllerWithoutDemoTest` (404 without demo). Keep both green.

## Tasks & Acceptance

**Execution:**
- [x] `content/schemas/concept.schema.json`
  - Concept: `id` (pattern `^[A-Za-z0-9.]+(-[a-z0-9]+)+$`), `land` (enum of the 5 land ids), `ccss`, `title`, `reviewed`, `retired` and `items[]` are all required; `additionalProperties: false`.
  - Each item: `id`, `role` (enum), `kind` (enum `["number-line"]`), `payload` (object), `answer`, optional `acceptedAnswers[]`, optional `retired`, and the strings `nudge` and `hint` (`minLength` 1). It also has `walkthrough { steps: string[] (minItems 1), similar { payload, answer } }`.
- [x] `content/schemas/number-line.schema.json`
  - `$defs.payload`: `prompt`, `min`, `max`, `step > 0`, and `target` (an exact-value string matching `^-?\d+(\.\d+)?(/\d+)?$`, e.g. `"3/4"`).
  - `$defs.answer`: a number.
  - The top-level annotation `"x-equivalence": "ordered"`.
- [x] `content/schemas/curriculum.schema.json`
  - `lands` holds exactly the 5 lands in order, by `prefixItems` with `const` ids: `numbers`, `equations`, `functions`, `shapes`, `data`. Each has `id`, `title` and `concepts: string[]` with unique items.
  - `prerequisites: string[]` with unique items.
- [x] `content/curriculum.json` -- the 5 empty lands and empty `prerequisites`. `content/demo/curriculum.json` -- the same, with `numbers.concepts: ["demo-number-line"]`.
- [x] `content/demo/concepts/demo-number-line.json`
  - The full envelope, with `land: numbers`, `ccss: "demo"` and `reviewed: true`.
  - At least 1 discover, 4 practice, 3 review and 1 challenge item. Each item has a correct `target`/`answer` pair on its grid, a nudge, a hint, steps and a correct `similar`.
- [x] `backend/pom.xml` -- add `com.networknt:json-schema-validator:3.0.8`. `application.yml`: `app.content-root: classpath:content`. New `application-demo.yml`: `app.content-root: classpath:content/demo`.
- [x] `backend/src/main/java/app/mathjourney/content/`
  - `CatalogConcept` record; `ConceptCatalog` updated.
  - `ContentLoader`: pure, constructed with a content-root location, returning the loaded concepts or the full error list.
  - `ContentCatalog`: runs the loader at construction, logs the errors and throws `ContentValidationException`. Delete `DemoConceptCatalog` and `EmptyConceptCatalog`.
  - Cross-checks:
    - Concept `id` equals the file name; item ids start with `<conceptId>#` and are unique in the file.
    - AD-12 role minimums, counting non-retired items, for non-retired concepts.
    - Each item's payload and answers, and its `similar`'s, are validated against its kind's `$defs`.
    - Every curriculum id (in lands and prerequisites) has a file. A land-listed concept's `land` matches. Each non-retired file is listed once. A retired concept is not in a land.
- [x] `backend/src/main/java/app/mathjourney/web/ConceptController.java` -- map from `CatalogConcept.json()`.
- [x] `backend/src/test/resources/content-cases/<case>/` and `backend/src/test/java/app/mathjourney/content/ContentLoaderTest.java`
  - One minimal content root per matrix error row. Each case asserts that the error names the file and the JSON path.
  - A test that the real and demo roots load cleanly, and that the demo concept reports `reviewed() == true`.
- [x] `frontend/src/numberLine.ts`, `frontend/src/App.test.tsx` -- add `target: string` to `NumberLinePayload` and to the mock.

**Acceptance Criteria:**
- Given `./mvnw clean verify`, when it runs, then it succeeds with all the earlier backend and Vitest tests still passing.
- Given the jar started with `--app.content-root=file:<copy of content/demo with one hint removed>`, when it starts, then it exits non-zero, and the log has a `[content]` line naming that file and the item's path.
- Given `./start.sh --demo`, when the 3/4 point is dragged, then the page says "Correct!".

## Implementation Notes

- Schemas are registered by `$id` (`https://mathjourney.app/content/schemas/<file>`) from `classpath:content/schemas/*.schema.json`; the curriculum reuses `concept.schema.json#/$defs/conceptId`. The draft 2020-12 dialect is extended with `x-equivalence` as an annotation keyword, so the validator does not warn about it.
- Error paths use the validator's `JSON_PATH` format; kind-schema errors are re-rooted under the item path (e.g. `$.items[1].payload.step`). Files are named relative to the content root (`concepts/<id>.json`, `curriculum.json`).
- Order: parse → envelope schema → (only if the envelope is valid) kind `$defs` + id/role checks → curriculum schema → curriculum cross-checks. "Has a file" counts every concept file, even an invalid one, so a broken file is not also reported as missing from the curriculum. Orphan and land checks use only valid concepts.
- Also checked (beyond the matrix): a concept listed more than once across lands and prerequisites, duplicate JSON keys (`FAIL_ON_READING_DUP_TREE_KEY`), and `acceptedAnswers[]` against the kind's `$defs.answer`.
- `ConceptController` reads `ConceptResponse` with `FAIL_ON_UNKNOWN_PROPERTIES` disabled, since the envelope has fields the contract does not; `openapi.json` is unchanged.
- Review fix: `ContentBeforeFlywayConfiguration` makes Boot's `FlywayMigrationInitializer` depend on `ContentCatalog`, so bad content refuses to start before any migration or DB file is created (`ContentStartupOrderTest`). A repeat curriculum listing no longer re-runs the land/retired checks. Added cases: unknown prerequisite id, land + prerequisite duplicate, duplicate JSON key, bad `acceptedAnswers[0]`.
- `frontend/src/numberLine.test.ts` also got `target` in its payload literal (needed for type-checking).
- Matrix audit (orchestrator): every matrix row is covered by `ContentLoaderTest` (19) / `ContentCatalogTest` (2), each asserting file + JSON path. `./mvnw clean verify` BUILD SUCCESS: backend 39, Vitest 9.
- Re-verified after review patches (orchestrator): fresh worktree `./mvnw clean verify` BUILD SUCCESS (backend 44, Vitest 9). The main checkout hit the known Vitest `Cannot find module` flake again. Demo jar: 9 items, first is 3/4 → 0.75 with `target: "3/4"`.

## Plan Change Log

## Review Triage Log

### Pass 1 (quick lens): high 0, medium 1, low 2, false 0, maybe-false 0

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | Flyway migrates the learner DB before content is validated | medium | patch | Reviewer's bad-content run logged Hikari and `DbMigrate` before the `[content]` error and created `mathjourney.mv.db`. A new jar with a migration plus bad content migrates the learner's data, then refuses to start. Fix: the Flyway initializer depends on the content catalog; test that a bad root creates no DB file. |
| 2 | A repeat curriculum listing re-runs the retired and land checks, so errors repeat | low | patch | `listed()` returns true after the duplicate error. Direct correction: return false for a repeat listing. |
| 3 | Unknown prerequisite id, land + prerequisite duplicate, duplicate JSON key and bad `acceptedAnswers` have no tests | low | patch | Confirmed: no content-cases for them. Add one case each. |

## Design Notes

**Answer placement.** `answer` stays on the item (the API contract from entry 2), and `walkthrough.similar` has the same `{payload, answer}` shape. A kind schema then describes one "answerable" through two `$defs`, and the loader validates the item and its `similar` the same way. `target` is the problem's exact value, so entry 5's verifier can *compute* the grid position from it without parsing the prompt text.

**Cross-file checks.** JSON Schema can't express these, so they stay in Java, after schema validation passes. They use the same `{file, jsonPath, message}` error record.

**Resource listing.** `PathMatchingResourcePatternResolver` handles both `classpath:` (inside the jar) and `file:` roots. A missing `concepts/` folder means zero concepts, not an error.

## Verification

**Commands:**
- `./mvnw clean verify` -- expected: BUILD SUCCESS, including `ContentLoaderTest` and the existing tests. If Vitest fails with `Cannot find module` in this checkout, rerun in a fresh worktree (a known environmental flake, seen in entry 3).
- `java -jar backend/target/math-journey.jar --app.content-root=file:$TMP/bad --app.data-dir=$TMP/data` -- expected: a non-zero exit and a `[content] ... $.items[...]` line.
- `./start.sh --demo`, then `curl -s 127.0.0.1:8080/api/concepts/demo-number-line` -- expected: 200, with the first item's prompt "Drag the point to 3/4".
