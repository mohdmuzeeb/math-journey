---
title: 'Tracer: one number-line answer through every layer'
type: 'feature'
ticket: '1'
created: '2026-10-03'
status: 'built'
baseline_revision: '1e815bd2376ba1f942bfa63b5e4506fad8c68e1e'
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

**Problem:** The repo has no code. Every later story needs a running skeleton: one Spring Boot jar serving React and `/api`, started by `start.sh`, built and tested by `./mvnw verify`.

**Approach:** Scaffold Spring Boot 4.1.1 (Maven, Java 25) and Vite React-TS with Vitest, bundled into one jar by frontend-maven-plugin (Node 22). The backend serves one demo concept from `content/demo/` at `/api/concepts/{id}`, only under the `demo` profile. React draws a minimal number line where a mouse drag, snapped to the grid, is compared to the expected answer.

## Boundaries & Constraints

**Always:** Bind to `127.0.0.1:8080`. Package `app.mathjourney.<module>`. Content is packaged into the jar classpath under `content/` (AD-17). Demo content is loaded only with the `demo` profile. Vite dev server proxies `/api` to 8080; no CORS configuration (AD-1). Stack versions per architecture: Boot 4.1.1, frontend-maven-plugin 2.0.2 with Node v22.23.3, TypeScript 6.0.x pinned, React 19.x, Vite 8.3.x.

**Never:** No JPA, Flyway, H2 or database (entry 3). No springdoc/openapi-typescript, ProblemDetail advice or SPA fallback for deep paths (entry 2). No JSON schemas, startup validation or AnswerVerifier (entries 4–5). No design tokens, Check button or feedback strip styling (entry 6). No dnd-kit, touch or keyboard work (entry 7). The browser does no math evaluation — it compares a snapped grid value with the stored answer (AD-3).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Demo concept | `GET /api/concepts/demo-number-line` with `demo` profile | 200, concept JSON | — |
| Unknown concept | `GET /api/concepts/nope` | 404 | Spring default error body |
| No demo profile | `GET /api/concepts/demo-number-line` | 404; page shows a friendly "No lesson yet" message | no crash |
| Right drop | point released nearest the answer tick | "Correct!" shown | — |
| Wrong drop | released on another tick | "Not quite — try again", point stays | can drag again |
| Off the line | released beyond min/max | clamps to nearest end tick | — |
| No Java 25 | `start.sh` with Java < 25 or no `java` | prints install instructions, exit 1 | — |

</frozen-after-approval>

## Code Map

Greenfield: only `_bmad/` and `_bmad-output/` exist. Local env: JDK 25 is the default `java`; local Node is 26 (the build downloads its own Node 22); no global `mvn` — use the wrapper.

- `pom.xml` (root) -- aggregator, packaging `pom`, module `backend`; `./mvnw verify` runs from the root.
- `mvnw`, `.mvn/wrapper/` -- take from a Spring Initializr download (`bootVersion=4.1.1`, `javaVersion=25`, `type=maven-project`, dependency `web`), so the Boot 4 starter names are correct (`spring-boot-starter-webmvc` and its test starter).
- `backend/pom.xml` -- Boot parent 4.1.1, `finalName` `math-journey`.
- `frontend/` -- `npm create vite@latest frontend -- --template react-ts`, then pin TypeScript 6.0.3 and add Vitest, jsdom and @testing-library/react.
- `content/demo/concepts/demo-number-line.json` -- the demo fixture.

## Tasks & Acceptance

**Execution:**
- [x] `.gitignore` -- ignore `target/`, `node_modules/`, `frontend/dist/`, `frontend/node/`, `.idea/`, `.DS_Store`.
- [x] `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/` -- root aggregator and Maven wrapper.
- [x] `backend/pom.xml` -- webmvc and test starters. frontend-maven-plugin (workingDirectory `../frontend`): `install-node-and-npm` v22.23.3 and `npm ci` + `npm run build` in `generate-resources`, `npm test` (`vitest run`) in `test`, honouring `-DskipTests`. Resources: `src/main/resources`, `../content` → `content/`, `../frontend/dist` → `static/`.
- [x] `backend/src/main/resources/application.yml` -- `server.address: 127.0.0.1`, `server.port: 8080`, `spring.application.name: math-journey`.
- [x] `backend/src/main/java/app/mathjourney/MathJourneyApplication.java` -- `@SpringBootApplication` entry point.
- [x] `backend/src/main/java/app/mathjourney/content/DemoConceptCatalog.java` -- `@Profile("demo")` service; on startup reads `classpath:content/demo/concepts/*.json` into a map keyed by `id`, and refuses to start on unreadable JSON. Keep it behind a `ConceptCatalog` interface with an empty default implementation for non-demo runs.
- [x] `backend/src/main/java/app/mathjourney/web/ConceptController.java` -- `GET /api/concepts/{id}` returns the concept JSON as an opaque tree; missing → `ResponseStatusException(NOT_FOUND)`.
- [x] `backend/src/test/java/app/mathjourney/web/ConceptControllerTest.java` -- `@SpringBootTest` + MockMvc with `demo` active: 200 with the item id; unknown id → 404.
- [x] `content/demo/concepts/demo-number-line.json` -- see Design Notes.
- [x] `frontend/vite.config.ts` -- React plugin, `/api` proxy to `http://127.0.0.1:8080`, Vitest `environment: 'jsdom'`.
- [x] `frontend/src/numberLine.ts` -- pure `snap(value, min, max, step)` returning the tick index, and `isCorrect(dropValue, payload, answer)` comparing tick indices.
- [x] `frontend/src/numberLine.test.ts` -- covers the I/O matrix rows: right drop, wrong drop, clamping beyond the ends.
- [x] `frontend/src/api.ts` -- hand-written `Concept` type (entry 2 replaces it) and `fetchConcept(id)`.
- [x] `frontend/src/NumberLine.tsx` -- SVG line with labelled ticks and one draggable point driven by pointer events. On release it snaps to a tick and calls `onAnswer(value)`.
- [x] `frontend/src/App.tsx` -- loads `demo-number-line`, shows the prompt and the number line, then a "Correct!" or "Not quite — try again" line in an `aria-live` region; 404 → "No lesson yet". Remove the Vite template's demo assets and CSS.
- [x] `start.sh` (executable) -- checks `java` exists and its major version is ≥ 25, otherwise prints JDK 25 install instructions (macOS and Linux) and exits 1. Builds the jar with `./mvnw -q package -DskipTests` if it is missing. `--demo` adds `--spring.profiles.active=demo`. Starts the jar, waits for port 8080, then opens the browser (`open`, or `xdg-open`, best effort).
- [x] `README.md` -- prerequisites (JDK 25), `./start.sh [--demo]`, `./mvnw verify`, dev mode (`./mvnw -pl backend spring-boot:run` plus `npm run dev`).

**Acceptance Criteria:**
- Given a clean clone with JDK 25, when `./mvnw verify` runs, then the frontend build, the Vitest tests and the backend tests all run and pass, and `backend/target/math-journey.jar` contains `static/index.html` and `content/demo/concepts/demo-number-line.json`.
- Given the jar is built, when `./start.sh --demo` runs, then a browser opens `http://127.0.0.1:8080`, and dragging the point to the answer tick with a mouse shows "Correct!".
- Given the app is running, when it is reached on the machine's LAN IP, then the connection is refused.

## Implementation Notes

- Maven wrapper (3.3.4, Maven 3.9.16) and starter names come from a Spring Initializr download (Boot 4.1.1, Java 25, `web`): `spring-boot-starter-webmvc` + `spring-boot-starter-webmvc-test`. The root `pom.xml` is a plain aggregator; `backend/pom.xml` inherits the Boot parent.
- Frontend scaffolded with create-vite 9.2.1 (`react-ts`). Resolved: React 19.3.0, Vite 8.3.2, TypeScript 6.0.3 (exact pin), Vitest 5.0.3, jsdom 30.1.1, @testing-library/react 16.3.3 (+ @testing-library/dom peer). Template assets, CSS, `public/icons.svg` and the template README removed; `favicon.svg` kept.
- `numberLine.ts` and `NumberLine.tsx` differ only in case, which TypeScript rejects on case-insensitive filesystems for extensionless imports. All relative imports therefore use explicit extensions (`./numberLine.ts`, `./NumberLine.tsx`); `allowImportingTsExtensions` is already on in the template.
- `ConceptCatalog` interface with `DemoConceptCatalog` (`@Profile("demo")`) and `EmptyConceptCatalog` (`@Profile("!demo")`). The demo catalog also refuses to start on a missing/non-string `id` or a duplicate id.
- frontend-maven-plugin's `npm` goal natively skips test-phase executions under `-DskipTests` (verified: "Skipping execution.").
- Extra tests beyond the plan: `ConceptControllerWithoutDemoTest` (no-demo → 404) and `App.test.tsx` (404 → "No lesson yet"; simulated pointer drag → "Not quite — try again" then "Correct!").
- `start.sh`: on macOS `/usr/bin/java` is a system shim, so `PATH=/usr/bin:/bin` still finds a JDK when one is installed; the no-java check was verified with a stub PATH instead. The shim exits non-zero when no JDK is installed, so the version probe ends in `|| true` to avoid a silent `set -e` exit.
- Matrix audit (orchestrator): `./mvnw verify` re-run, BUILD SUCCESS (Vitest 2 files / 8 tests, backend 3 tests). "No Java 25" row checked with stubbed PATHs: fake Java 21 → "Found Java 21.0.2, but … needs Java 25", exit 1; no java → "Java was not found.", exit 1. The plan's `PATH=/usr/bin:/bin` command can't test this on macOS (the `/usr/bin/java` shim). Manual drag demo run by the parent via `./start.sh --demo`.

## Plan Change Log

## Review Triage Log

### Pass 1 (quick lens) — high 0, medium 2, low 3, false 2, maybe-false 0; 1 low rejected

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | `start.sh` readiness probe passes on an existing listener on 8080 | medium | patch | Probe only tests that the port connects; a second `./start.sh` opens the browser on the old process, then the new jar dies with "port in use". Fix: refuse to launch when 8080 is already taken. |
| 2 | `start.sh` startup timeout is silent | low | patch | After 60 failed probes the loop falls through to `wait` with no message. One-line message added. |
| 3 | `start.sh` checks `java` on PATH but `mvnw` uses `$JAVA_HOME` | low | reject | Real, but `JAVA_HOME` is unset here and in the documented setup; it only bites the first-run build, which fails loudly. The fix adds a branch. |
| 4 | `start.sh` rejects `25-ea` versions | low | patch | `java_major="25-ea"` fails the numeric test. Fix: strip any `-suffix` before comparing. |
| 5 | `App.tsx` repeated wrong drop leaves feedback text unchanged | medium | patch | Same string set again: no visible change and no aria-live re-announcement on the second wrong try. Fix: clear feedback when a drag starts. |
| 6 | `NumberLine.tsx` tick labels show float noise for steps like 0.1 | low | patch | `min + i*step` is rendered raw; harmless for the demo's 0.25 but any 0.1 step would show `0.30000000000000004`. Fix: format the label. |
| 7 | `numberLine.ts` silently snaps an off-grid stored answer | false | reject | By design (Design Notes: compare by tick index). Off-grid content is for the entry-5 AnswerVerifier to refuse at startup (AD-3), not the browser. |
| 8 | `mvnw`, `mvnw.cmd`, `package-lock.json` missing from the diff | false | reject | They exist, are not git-ignored (`git check-ignore` finds none), and were left out of the review diff only as generated files. They are committed with the change. |

## Design Notes

Demo concept (the full AD-12 envelope arrives in entry 4; IDs follow AD-10's `<conceptId>#<slug>`):

```json
{ "id": "demo-number-line", "title": "Demo: fractions on a number line",
  "items": [{ "id": "demo-number-line#three-quarters", "kind": "number-line",
    "payload": { "prompt": "Drag the point to 3/4", "min": 0, "max": 2, "step": 0.25 },
    "answer": 0.75 }] }
```

The answer is compared by tick index, `Math.round((v - min) / step)`, so there is no float equality. The Java side treats payloads as opaque JSON. With Boot 4 that is Jackson 3 (`tools.jackson.databind.JsonNode`), so check the package names when compiling.

Java check: accept ≥ 25 rather than exactly 25, since a newer LTS would run the jar too.

## Verification

**Commands:**
- `./mvnw verify` -- expected: BUILD SUCCESS; the output shows Vitest passing and the backend tests passing.
- `unzip -l backend/target/math-journey.jar | grep -E 'static/index.html|demo-number-line.json'` -- expected: both listed.
- `java -jar backend/target/math-journey.jar --spring.profiles.active=demo & sleep 8; curl -s 127.0.0.1:8080/api/concepts/demo-number-line; curl -s -o /dev/null -w '%{http_code}' 127.0.0.1:8080/api/concepts/nope` -- expected: the concept JSON, then 404.
- `PATH=/usr/bin:/bin JAVA_HOME= ./start.sh` (no java on PATH) -- expected: install instructions, exit 1.

**Manual checks:**
- `./start.sh --demo`: drag the point to 0.75 to see "Correct!", and drop it on 1.0 to see "Not quite — try again" (HITL: parent).
