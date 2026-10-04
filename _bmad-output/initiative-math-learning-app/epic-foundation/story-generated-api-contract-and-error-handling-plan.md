---
title: 'Generated API contract and error handling'
type: 'feature'
ticket: '2'
created: '2026-10-03'
status: 'built'
baseline_revision: 'd6def4494b89a1b810b208d61555af51f8fb2f48'
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

**Problem:** The frontend's API type is hand-written, so it can drift from Java (AD-7). Errors are Spring's default bodies, not ProblemDetail. Unknown paths aren't split into an `/api` 404 and an SPA fallback (AD-1).

**Approach:** Publish the OpenAPI document with springdoc, export it to `frontend/openapi.json` during `./mvnw verify`, and generate the frontend API types from it with openapi-typescript. Then enable ProblemDetail with one global `@RestControllerAdvice`, and route unknown `/api/**` to a 404 ProblemDetail and every other unknown path to `index.html`.

## Boundaries & Constraints

**Always:** springdoc-openapi-starter-webmvc-ui 3.1.1; openapi-typescript 7.13.0, with TypeScript held at 6.0.3 through npm `overrides` (the peer asks for `^5.x`). Concept payload and answer stay opaque JSON in the contract (AD-7). One `./mvnw verify` pass regenerates the contract, then the types, then builds the frontend. A DTO change must reach `tsc` in that same run.

**Never:** No hand-written API types left in the frontend. No CORS configuration. No content schemas or generated content types (entry 6). No database (entry 3). Never show a raw error to the learner; the existing friendly messages stay.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Unknown API path | `GET /api/nope` | 404, `application/problem+json`, `status: 404` | — |
| Unknown concept | `GET /api/concepts/nope` | 404 ProblemDetail whose `detail` names the id | — |
| SPA deep link | `GET /map`, `GET /land/numbers` | 200 `index.html` | — |
| Real asset | `GET /` and a built `assets/*.js` | the file itself | — |
| Missing asset | `GET /assets/missing.js` (path has a dot) | 404, not `index.html` | — |
| Unexpected failure | a controller throws a `RuntimeException` | 500 ProblemDetail with a generic `detail`, no message or stack trace | logged with the module name |
| DTO drift | a field renamed in `ConceptResponse` | `./mvnw verify` fails in the frontend `tsc` step | — |

</frozen-after-approval>

## Code Map

Story 1.1 built (`d6def44`). Reuse it, and keep its test and `start.sh` behaviour:

- `backend/pom.xml` -- frontend-maven-plugin today runs `npm ci` and `npm run build` in `generate-resources` and `npm test` in `test`, and `../frontend/dist` is copied as a `<resource>` to `static/`. These phases have to move (see Design Notes).
- `backend/src/main/java/app/mathjourney/web/ConceptController.java` -- returns a raw `JsonNode`. Switch it to a DTO; keep the `ResponseStatusException(NOT_FOUND)`.
- `backend/src/main/java/app/mathjourney/content/ConceptCatalog.java` (`Optional<JsonNode> findById`) -- unchanged. Mapping to the DTO happens in `web`.
- `frontend/src/api.ts` -- the hand-written `Concept`/`ConceptItem` and `fetchConcept`. Keep `fetchConcept` and its 404 → `null` behaviour; swap the types for generated ones.
- `frontend/src/App.tsx`, `App.test.tsx` -- consume `Concept`. Their behaviour is unchanged.
- Jackson 3 (`tools.jackson.*`) is on the classpath; Boot 4 namespaces apply.

## Tasks & Acceptance

**Execution:**
- [ ] `backend/pom.xml` -- add springdoc 3.1.1. Re-phase the frontend: `install-node-and-npm` + `npm ci` in `prepare-package`, then `npm run build -- --outDir ${project.build.outputDirectory}/static --emptyOutDir` in `prepare-package`, then `npm test` in `integration-test` (so `-DskipTests` still skips Vitest). Drop the `frontend/dist` resource.
- [ ] `backend/src/main/resources/application.yml` -- `spring.mvc.problemdetails.enabled: true`.
- [ ] `backend/src/main/java/app/mathjourney/web/ConceptResponse.java` -- records `ConceptResponse(id, title, items)` and `ConceptItemResponse(id, kind, payload, answer)`, with `payload` as an opaque object and `answer` as any JSON. Annotate them so OpenAPI shows required fields and free-form JSON (see Design Notes).
- [ ] `backend/src/main/java/app/mathjourney/web/ConceptController.java` -- map the catalog node to `ConceptResponse` with the injected mapper's `treeToValue`, and return the DTO.
- [ ] `backend/src/main/java/app/mathjourney/web/ApiExceptionHandler.java` -- one `@RestControllerAdvice` extending `ResponseEntityExceptionHandler`. It adds an `Exception` handler that returns a 500 ProblemDetail with a generic detail and logs the cause with a `[web]` prefix.
- [ ] `backend/src/main/java/app/mathjourney/web/SpaResourceConfig.java` -- `WebMvcConfigurer` with a `/**` resource handler on `classpath:/static/`. Its resolver returns the file if it exists; returns `null` for `api/**` and for any path whose last segment contains a dot; otherwise returns `index.html`.
- [ ] `backend/src/test/java/app/mathjourney/web/OpenApiExportTest.java` -- `@SpringBootTest` + MockMvc: GET `/v3/api-docs` and write it pretty-printed to `../frontend/openapi.json` (working directory is `backend/`). Assert that `ConceptResponse` is present.
- [ ] `backend/src/test/java/app/mathjourney/web/ErrorAndSpaRoutingTest.java` -- covers every matrix row except DTO drift. It uses a test-only controller that throws, plus a test `static/index.html` and `static/assets/app.js` under `src/test/resources`.
- [ ] `frontend/openapi.json` -- committed, so `./mvnw package -DskipTests` (start.sh's first run) and `npm run dev` work without running tests.
- [ ] `frontend/package.json` -- add `openapi-typescript` 7.13.0 and `overrides: { "typescript": "$typescript" }`; add a `gen:api` script (`openapi-typescript openapi.json -o src/api/schema.d.ts`) run by `prebuild`, `predev` and `pretest`. Refresh `package-lock.json`.
- [ ] `.gitignore` -- add `frontend/src/api/schema.d.ts` (always generated).
- [ ] `frontend/src/api.ts` -- `export type Concept = components['schemas']['ConceptResponse']` (and the item type) from `./api/schema.d.ts`. No hand-written interfaces.
- [ ] `frontend/src/App.tsx` -- adjust casts only if the generated types need it. Behaviour is unchanged.
- [ ] `README.md` -- one line: `frontend/openapi.json` is regenerated by `./mvnw verify`, so commit it when it changes.

**Acceptance Criteria:**
- Given a clean tree, when `./mvnw verify` runs, then `frontend/openapi.json` is rewritten from the running app, `src/api/schema.d.ts` is regenerated, the frontend builds into the jar's `static/`, and Vitest and the backend tests pass.
- Given `ConceptResponse.title` is renamed to `name`, when `./mvnw verify` runs, then the build fails in the frontend `tsc` step. Reverting the rename makes it pass again.
- Given `./start.sh --demo`, when the page loads, then the demo still answers "Correct!" for 3/4.
- Given `frontend/src`, when it is searched for `interface Concept`, then nothing is found.

## Implementation Notes

- Implemented by the step-03 subagent as planned. Deviations: `App.tsx` casts `item.payload as unknown as NumberLinePayload` (tsc rejects a direct cast from the generated map type); the throwing test controller is nested in `ErrorAndSpaRoutingTest` and pulled in with `@Import`, so it stays out of the OpenAPI export. springdoc also enables Swagger UI at `/swagger-ui.html` (local-only).
- Matrix audit (orchestrator): `./mvnw clean verify` BUILD SUCCESS: backend 11 tests (`ErrorAndSpaRoutingTest` 7 covers the first six matrix rows), Vitest 9. DTO drift row: renaming `title` → `name` failed the build with `src/App.tsx(44,26): error TS2339: Property 'title' does not exist`; reverted, and a rebuild restored `openapi.json`.
- Patch (review #3): `SpaFallbackResolver` 404s only known static-file extensions; `/concept/8.EE.7-two-step-equations` → index.html (test added). Re-verified: `./mvnw verify` BUILD SUCCESS (backend 12, Vitest 9); curl against a fresh jar: `/map` and the dotted route serve index.html, `/assets/missing.js` 404, `/api/nope` 404 `application/problem+json`. (A first curl pass hit a stale jar still on 8080 and showed a false 404.)

## Plan Change Log

## Review Triage Log

### Pass 1 (quick lens) — high 0, medium 1, low 2, false 2, maybe-false 0

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | `frontend/openapi.json` untracked / missing from the diff | false | reject | The file exists and is not git-ignored. It was left out of the review diff only as a generated file, and is committed with the change. |
| 2 | `frontend/package-lock.json` changes missing from the diff | false | reject | Same: filtered from the review diff on purpose; the modified lockfile is committed with the change. |
| 3 | `SpaFallbackResolver` 404s any last segment containing a dot, so deep links ending in an AD-10 concept id (`8.EE.7-two-step-equations`) get a ProblemDetail instead of `index.html` | medium | patch | AD-10 ids always contain dots, so this breaks AD-1 for the first concept-id route. The frozen row only needs `/assets/missing.js` to 404. Fix: return null only when the last segment ends in a known static-file extension; add a deep-link test with a concept id. |
| 4 | `@ExceptionHandler(Exception.class)` turns `@ResponseStatus`-annotated exceptions into 500s | low | reject | Real but latent: nothing uses `@ResponseStatus`, and project convention is ProblemDetail via the advice. The fix adds branching. |
| 5 | Real-asset and `/` matrix row tested only against test fixtures, not the built Vite output | low | reject | Covered by the plan's Verification curl against the jar (implementer ran it: `/`, `/map`, a real `assets/*.js` all correct). Automating it needs an integration test beyond a direct fix. |

## Design Notes

**Build order.** The contract comes from compiled Java, so the frontend must build after the `test` phase. The order is: `compile` → `test`, where `OpenApiExportTest` writes `openapi.json` → `prepare-package`, which regenerates types, runs `tsc` and `vite build` into `target/classes/static` → `package`, which builds the jar → `integration-test`, which runs Vitest. With `-DskipTests` the committed `openapi.json` is used.

**Opaque JSON in the contract.** Type `payload` as `Map<String, Object>` and `answer` as `Object` with `@Schema(nullable = false)`, or use `JsonNode` with `@Schema(type = "object")` if springdoc renders a JsonNode schema. Either way, the generated TypeScript must be `Record<string, unknown>` and `unknown`, never a `JsonNode` shape. Mark record components `@Schema(requiredMode = REQUIRED)` so generated fields aren't optional.

**SPA fallback.** Doing it in a resource resolver rather than a regex `@GetMapping` keeps `/api/**` misses on the `NoResourceFoundException` path. `ResponseEntityExceptionHandler` already turns that into a 404 ProblemDetail.

## Verification

**Commands:**
- `./mvnw verify` -- expected: BUILD SUCCESS; backend tests, including the export and routing tests, and Vitest all pass.
- `git status --short frontend/openapi.json` -- expected: no change on a second run, so the export is deterministic.
- Temporarily rename `title` → `name` in `ConceptResponse` and run `./mvnw verify` -- expected: it fails at frontend `tsc`. Then revert.
- `java -jar backend/target/math-journey.jar --spring.profiles.active=demo &`, then `curl -si 127.0.0.1:8080/api/nope` -- expected: `404`, `Content-Type: application/problem+json`. `curl -s 127.0.0.1:8080/map | grep '<div id="root">'` -- expected: a match.
