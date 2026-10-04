---
title: 'Refactor sweep'
type: 'refactor'
ticket: '8'
created: '2026-10-03'
status: 'built'
baseline_revision: 'fe9c71c0508b74ed401c5c6730558fb27908e1a2'
route: 'full'
route_source: 'auto'
risk: 'low'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Epic 1 left small debts, recorded in its plans and review triage logs:
- the verifier contract forces the loader to call `verify` twice;
- the shipped jar carries test fixtures;
- the test data dir is configured in two places;
- two generated content types are wrong (`answer` is typed as an object, curriculum `lands` as `never[]`);
- every build wipes and reinstalls `node_modules`;
- the catch-all error handler turns `@ResponseStatus` exceptions into 500s;
- `start.sh` checks the `java` on `PATH` while `mvnw` uses `$JAVA_HOME`.

**Approach:** Clean each one up with the smallest change. Behaviour seen by the learner, the API contract and the content rules stay the same, and every existing test stays green (tests that only change shape are adapted).

## Boundaries & Constraints

**Always:**
- **Verifier result:** `AnswerVerifier.verify` returns a typed result, a public record `AnswerProblem(Field field, String message)` with `Field` = `ANSWER | ACCEPTED_ANSWERS`. `ContentLoader` calls it once per node and maps `field` to `.answer` / `.acceptedAnswers`. The paths and messages stay exactly as today.
- **Packaging:** `content/fixtures/**` is excluded from the jar's `content/` resources. The Java fixture tests keep reading them from `../content/fixtures/`.
- **Test data dir:** the only source is `backend/src/test/resources/config/application.yml`. Remove the Surefire `systemPropertyVariables` block.
- **Generated types:** item `answer` and `walkthrough.similar.answer` are `unknown`, and curriculum `lands` is an array of `{ id; title; concepts: string[] }`. Fix this in the schemas or the generator options. Java validation results must be identical; any new schema keyword is registered with the validator, as `x-equivalence` is.
- **Build:** the `npm-ci` execution becomes `npm install --no-audit --no-fund`, and `package-lock.json` must be unchanged after a build.
- **Error handler:** an exception class annotated `@ResponseStatus` is answered with a ProblemDetail carrying that status, and the annotation's `reason` as `detail` when it has one. Everything else is still the generic 500.
- **`start.sh`:** when `JAVA_HOME` is set, it checks and runs `"$JAVA_HOME/bin/java"`. Otherwise it uses `java` on `PATH`, as today. The messages are unchanged.

**Never:**
- No new features.
- No change to the API shape or `openapi.json`.
- No change to content files, `numberLine.ts` semantics or UI behaviour.
- No dependency upgrades.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Wrong answer + accepted answers | an item with a wrong `answer` and a non-empty `acceptedAnswers` | the same two errors as today, at `.answer` and `.acceptedAnswers`; `verify` called once | — |
| Jar contents | `unzip -l math-journey.jar` | no `BOOT-INF/classes/content/fixtures/` entries; `content/schemas` and `content/demo` present | — |
| `@ResponseStatus` exception | a controller throws `@ResponseStatus(CONFLICT, reason="taken")` | 409 `application/problem+json` with `detail: "taken"` | — |
| Plain exception | a `RuntimeException` | unchanged: 500 with the generic detail | logged `[web]` |
| JAVA_HOME older | `JAVA_HOME` → a fake Java 21, `PATH` → Java 25 | `start.sh` refuses with "Found Java 21…" | exit 1 |
| JAVA_HOME unset | `PATH` java 25 | starts as today | — |
| Second build | `./mvnw verify` twice | `node_modules` reused; `git status` shows no `package-lock.json` change | — |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/app/mathjourney/content/AnswerVerifier.java` -- `List<String> verify(payload, answer, acceptedAnswers)`, whose Javadoc requires determinism for the double call. Update the contract and the Javadoc.
- `NumberLineVerifier.java` -- `verify` → `check(...)` plus the accepted-answers message. Tag each problem with its field.
- `ContentLoader.java` (`verifyAnswers`) -- calls `verify` twice and diffs the lists. Replace this with one call and a mapping by field.
- Tests: `NumberLineVerifierTest`, `NumberLineFixturesTest` (also verifies each fixture's own answer), `ContentLoaderTest` (`TestVerifiers` helper) -- adapt them to the result type.
- `backend/pom.xml`
  - The `<resource>` for `${project.basedir}/../content` with `targetPath content`: add `<excludes><exclude>fixtures/**</exclude></excludes>`.
  - The `maven-surefire-plugin` block with `app.data-dir`: delete it.
  - The `npm-ci` execution: change its arguments.
- `backend/src/test/resources/config/application.yml` -- already sets `app.data-dir: ${user.dir}/target/test-data`.
- `content/schemas/concept.schema.json` -- `answer` (line 36) and `similar.answer` (line 60) have no `type`. `curriculum.schema.json` `lands` uses `prefixItems` + `items: false` (lines 10–20), which the generator turns into `never[]`.
- `frontend/scripts/gen-content-types.mjs` -- the generator options.
- `backend/src/main/java/app/mathjourney/web/ApiExceptionHandler.java` -- the `@ExceptionHandler(Exception.class)` returns the generic 500. Test it via the nested throwing controller pattern in `ErrorAndSpaRoutingTest`.
- `start.sh` -- the Java check uses `command -v java` / `java -version`, and the run uses `java -jar`.

## Tasks & Acceptance

**Execution:**
- [x] `AnswerVerifier.java`, `AnswerProblem.java`, `NumberLineVerifier.java`, `ContentLoader.java` + tests -- introduce the typed result and the single call. Add a loader test asserting `verify` is called once per node, using a counting verifier.
- [x] `backend/pom.xml` -- exclude the fixtures, remove the Surefire block, switch `npm ci` → `npm install --no-audit --no-fund`.
- [x] `content/schemas/*.schema.json` and/or `frontend/scripts/gen-content-types.mjs` -- correct the generated `answer` (`unknown`) and `lands` types. Add `frontend/src/content-types.test.ts`: a type-level check that a `Curriculum` literal with lands type-checks and that `answer` accepts a number.
- [x] `ApiExceptionHandler.java` + `ErrorAndSpaRoutingTest.java` -- `@ResponseStatus` handling plus a test for the 409 row.
- [x] `start.sh` -- select `$JAVA_HOME/bin/java` when set, for both the check and the run.

**Acceptance Criteria:**
- Given `./mvnw clean verify`, when it runs, then BUILD SUCCESS, with every earlier test passing (adapted only for the new verifier type) plus the new ones.
- Given the built jar, when listed, then it has no `content/fixtures/` entries.
- Given `./start.sh --demo` after the build, when the page loads, then the 3/4 demo answers "You did it!" as before.

## Implementation Notes

- `AnswerProblem` lives in its own file, with `Field` nested inside it. `NumberLineVerifier` tags each `check` message `ANSWER` and the non-empty-accepted message `ACCEPTED_ANSWERS`. `ContentLoader.verifyAnswers` makes one call and maps the field to the path with a `switch`.
- New loader tests: `wrongAnswerAndAcceptedAnswersAreBothReported` (new case `content-cases/wrong-answer-and-accepted-answers`) and `verifierIsCalledOncePerNode` (counting verifier: calls == 2 × items).
- `answer` typing: json-schema-to-typescript treats `{ "description": ... }` (no type) as an object. Fixed with `"tsType": "unknown"` on both answer schemas; `tsType` is registered as an `AnnotationKeyword` next to `x-equivalence`.
- `lands`: `items: false` → `items: { "$ref": "#/$defs/land" }` plus `maxItems: 5`. The generator now emits `[Land, Land, Land, Land, Land]`. Validity is pinned by the new `curriculumLandsArePinned` test (5 ok; 4, 6, wrong order, missing title and extra property rejected), which passed before and after the schema change. The only Java difference: a 6th land is now reported as `maxItems` at `$.lands` rather than as `items: false` at `$.lands[5]`.
- The Maven execution id was renamed `npm-ci` → `npm-install`.
- `ApiExceptionHandler` uses `AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class)`; with no `reason`, `detail` is left out. These responses are not logged as `[web]` errors.
- Matrix audit (orchestrator): rows covered by `ContentLoaderTest` (single call, wrong + accepted), `ErrorAndSpaRoutingTest` (409/410, generic 500), and the jar listing (0 fixture entries). JAVA_HOME and second-build rows were checked manually by the implementer. `./mvnw clean verify` BUILD SUCCESS in the main checkout (backend 74, Vitest 67), with no `Cannot find module` flake.
- Re-verified after the review patch (orchestrator): `./mvnw clean verify` BUILD SUCCESS in the main checkout (backend 74, Vitest 67); `package-lock.json` and `openapi.json` unchanged.

## Plan Change Log

## Review Triage Log

### Pass 1 (quick lens): high 0, medium 0, low 3, false 0, maybe-false 0

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | A 6th land is now reported as `maxItems` at `$.lands`, not `items:false` at `$.lands[5]` | low | reject | The same curricula are accepted and rejected (`curriculumLandsArePinned` before and after); only the error path and keyword differ, and the new message is clearer. Keeping `items:false` with correct generated types needs a fragile `tsType` tuple. Only a hand-written sixth land meets it. |
| 2 | `verifierIsCalledOncePerNode` takes its item count from a different case | low | patch | Passes by the coincidence that both cases have 9 items. Direct fix: count the loaded case. |
| 3 | A wrapped `@ResponseStatus` exception still becomes a 500 | low | reject | Nothing throws one wrapped today; the plan scoped this to "an exception class annotated". The fix adds cause-walking branches. |

## Design Notes

**`lands` typing.** The curriculum schema pins the five lands with `prefixItems` and `const` ids, and that must keep validating the same way in Java. If the generator can't render `prefixItems`, keep it for validation and add `"items": { "$ref": "#/$defs/land" }`, plus `minItems`/`maxItems: 5`. The `const`s in `prefixItems` still pin the order.

**npm install vs ci.** `npm ci` deletes `node_modules` on every build. That is slow, and it is the likely trigger of the "Cannot find module" flake when something else (VS Code) touches the folder mid-build. `npm install` with a committed lockfile installs the same tree and reuses what's there.

## Verification

**Commands:**
- `./mvnw clean verify` -- expected: BUILD SUCCESS. If Vitest fails with `Cannot find module` in this checkout, rerun in a fresh worktree.
- `unzip -l backend/target/math-journey.jar | grep -c 'content/fixtures'` -- expected: `0`.
- `./mvnw verify && git status --short frontend/package-lock.json` -- expected: no change.
- `JAVA_HOME=<dir with a bin/java stub printing version "21.0.2"> ./start.sh` -- expected: "Found Java 21.0.2…" and exit 1.
