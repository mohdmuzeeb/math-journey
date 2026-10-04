---
title: 'Learner profile and data that survives upgrades'
type: 'feature'
ticket: '3'
created: '2026-10-03'
status: 'built'
baseline_revision: 'ff3fa1b152dbe1d5ddb5cb242a2035b981fee10d'
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

**Problem:** The app has no database. Epics 2–4 need a learner profile (onboarding stage, sound, companion name), and that data has to survive the parent replacing the jar (CAP-12, AD-17).

**Approach:** Add an H2 file database in `${app.data-dir}` (default `${user.home}/.mathjourney`), with its schema owned by Flyway (`V1`, `ddl-auto=validate`). Add a `learner` module that holds the single `learner_profile` row, and expose it read-only as `GET /api/learner` → `{ onboardingStage: "journey", settings: { sound: true, companionName: "Lumi" } }`.

## Boundaries & Constraints

**Always:** Boot-managed versions of `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway` and `h2` (runtime). The schema changes only through Flyway, migration SQL is portable to PostgreSQL, and `spring.jpa.hibernate.ddl-auto=validate` (AD-11). Config lives under `app.*` in `application.yml`. The `learner` module depends on nothing else. `web` reaches it only through its public service, never a repository. `onboardingStage` is one of `welcome | prereq-check | journey` in the contract, and the interim default is `journey`. The OpenAPI export picks up the new DTO, and the regenerated `frontend/openapi.json` is committed.

**Never:** Tests never read or write `~/.mathjourney`. No write endpoint (epics 3–4 add one). No frontend screen or `fetchLearner` yet. No session, attempt or progress tables (epic 2). No `flyway-database-postgresql`. No H2 console.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| First start | empty `app.data-dir` | Flyway creates `mathjourney.mv.db` and the default row; `GET /api/learner` → 200 with the defaults | — |
| Data dir missing | `app.data-dir` points at a folder that doesn't exist | H2 creates the folder; the start succeeds | — |
| Restart / new jar | DB holds a changed `companion_name` | after a restart, `GET /api/learner` returns the changed value; Flyway applies no migration | — |
| Schema drift | an entity field with no matching column | startup fails in Hibernate validate | the start fails loudly; no silent DDL |
| Profile row missing | someone deleted the row by hand | 500 ProblemDetail with the generic detail | logged with a `[learner]` prefix |

</frozen-after-approval>

## Code Map

Stories 1.1 and 1.2 built (`ff3fa1b`). Reuse them and keep their tests green:

- `backend/pom.xml` -- add the three dependencies. Leave the frontend-maven-plugin phases (the export comes in `test`, the frontend build in `prepare-package`) alone. Surefire is not configured yet.
- `backend/src/main/resources/application.yml` -- has `spring.mvc.problemdetails` and `server.address/port`. Add `app.data-dir`, the datasource, JPA and Flyway settings.
- `backend/src/main/java/app/mathjourney/web/ConceptController.java`, `ConceptResponse.java` -- the pattern to copy: a package-private controller and records, with `@Schema(requiredMode = REQUIRED)` on every component.
- `backend/src/main/java/app/mathjourney/web/ApiExceptionHandler.java` -- already turns any uncaught exception into a 500 ProblemDetail. Don't touch it.
- `backend/src/test/java/app/mathjourney/web/OpenApiExportTest.java` -- writes `../frontend/openapi.json`. Extend its assertion to `LearnerResponse` too.
- Every existing `@SpringBootTest` (`ConceptControllerTest`, `ConceptControllerWithoutDemoTest`, `ErrorAndSpaRoutingTest`, `OpenApiExportTest`) will now start a datasource. That's why tests need the data-dir override (below).
- `start.sh` -- no change. The data dir comes from config.

## Tasks & Acceptance

**Execution:**
- [x] `backend/pom.xml` -- add `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway` and `com.h2database:h2` (runtime). Configure `maven-surefire-plugin` with `<systemPropertyVariables><app.data-dir>${project.build.directory}/test-data</app.data-dir>` so tests never touch the home folder.
- [x] `backend/src/main/resources/application.yml` -- `app.data-dir: ${user.home}/.mathjourney`; `spring.datasource.url: jdbc:h2:file:${app.data-dir}/mathjourney`; `spring.jpa.hibernate.ddl-auto: validate`; `spring.jpa.open-in-view: false`.
- [x] `backend/src/main/resources/db/migration/V1__learner_profile.sql` -- create `learner_profile (id UUID PRIMARY KEY, onboarding_stage VARCHAR(20) NOT NULL CHECK (...three values...), sound BOOLEAN NOT NULL, companion_name VARCHAR(40) NOT NULL)`, then insert the one default row with a fixed UUID literal.
- [x] `backend/src/main/java/app/mathjourney/learner/` -- `OnboardingStage` enum, mapped to and from the kebab-case strings by an `AttributeConverter`; a package-private `LearnerProfile` entity and a Spring Data repository; and a public `LearnerService` with `LearnerProfile`-free output (a public record `LearnerView(OnboardingStage, boolean sound, String companionName)`) whose read is `@Transactional(readOnly = true)`. It throws `IllegalStateException` (and logs `[learner]`) when there isn't exactly one row.
- [x] `backend/src/main/java/app/mathjourney/web/LearnerController.java` + `LearnerResponse.java` -- `GET /api/learner` maps `LearnerView` → `LearnerResponse(onboardingStage, settings: LearnerSettingsResponse(sound, companionName))`. `onboardingStage` is a `String` with `@Schema(allowableValues = {"welcome","prereq-check","journey"})`, so the generated TS is a union. All components are REQUIRED.
- [x] `backend/src/test/java/app/mathjourney/web/LearnerControllerTest.java` -- `GET /api/learner` returns exactly the default JSON.
- [x] `backend/src/test/java/app/mathjourney/learner/LearnerPersistenceTest.java` -- uses a `@TempDir`. It starts the app (`SpringApplicationBuilder`, web type NONE, `--app.data-dir=<tmp>`), checks the `.mv.db` file exists, updates `companion_name` with `JdbcTemplate`, and closes. Then it starts again on the same dir and checks that `LearnerService` returns the changed name and Flyway reports one applied migration. A second case: a non-existent nested dir gets created.
- [x] `backend/src/test/java/app/mathjourney/web/OpenApiExportTest.java` -- also assert `/components/schemas/LearnerResponse`. Commit the regenerated `frontend/openapi.json`.

**Acceptance Criteria:**
- Given a clean checkout, when `./mvnw verify` runs, then the build succeeds, the existing tests still pass, and `~/.mathjourney` is neither created nor modified by the build.
- Given the generated `frontend/src/api/schema.d.ts`, when it is inspected, then `LearnerResponse.onboardingStage` is `"welcome" | "prereq-check" | "journey"` and no field is optional.
- Given a jar run with `--app.data-dir=<tmp>`, when its companion name is changed with SQL, the jar is rebuilt and the app restarted on the same dir, then `GET /api/learner` returns the changed name.

## Implementation Notes

- `LearnerPersistenceTest` has a third case, `missingProfileRowFailsLoudly`: it deletes the row and expects `IllegalStateException`, with `[learner]` logged. The existing `ApiExceptionHandler` turns that into the generic 500.
- The datasource username is empty, because Boot defaults to `sa` only for embedded URLs. The H2 Shell needs `-user "" -password ""` for the jar check.
- Flyway 12.4 warns that H2 2.4.240 is newer than the version it was verified with (2.3.232). Migration and validation work.
- Matrix audit (orchestrator): added `LearnerPersistenceTest.schemaDriftStopsStartup` (drops `sound`, restart fails Hibernate validate) and `web/LearnerMissingRowTest` (row deleted in a rolled-back transaction → 500 `application/problem+json` with the generic detail). `./mvnw clean verify` BUILD SUCCESS: backend 18, Vitest 9; `~/.mathjourney` still absent.
- Patches (review pass 1): test-only `config/application.yml` (`app.data-dir: ${user.dir}/target/test-data`), drift assertion on `missing column [sound]`, `;DB_CLOSE_ON_EXIT=FALSE` on the URL. Re-verified in a fresh worktree: `./mvnw clean verify` BUILD SUCCESS (backend 18, Vitest 9). Jar check: defaults → H2 Shell set `Pip` → `package -DskipTests` → restart on the same dir returns `Pip`, Flyway up to date, no errors on SIGTERM. In the main checkout, `clean verify` fails at Vitest with a different jsdom dependency missing each run (`parse5`, `decimal.js`). The baseline passes in a fresh worktree, and so does this diff, so the cause is environmental and outside this change.

## Plan Change Log

## Review Triage Log

### Pass 1 (quick lens): high 0, medium 2, low 2, false 0, maybe-false 0

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | `app.data-dir` is overridden only by Surefire, so IDE test runs hit `~/.mathjourney` | medium | patch | Confirmed: `src/test/resources` holds only `static/`, and `.vscode/` exists. Breaks the Never rule outside Maven. Fix: test-only `config/application.yml` setting `app.data-dir: ${user.dir}/target/test-data`. |
| 2 | The `[learner]` log line is never asserted | low | reject | Real, but cosmetic. The fix adds output-capture plumbing to tests. |
| 3 | The schema-drift test asserts only that the stack trace contains "sound" | low | patch | Direct correction: assert `missing column [sound]` (Hibernate's message, seen in the verify log). |
| 4 | H2 file URL without `DB_CLOSE_ON_EXIT=FALSE` races Spring shutdown | medium | patch | Boot adds this flag only to the embedded URLs it generates. A user-set file URL keeps H2's own shutdown hook, which runs concurrently with Spring's on SIGTERM from `start.sh`. Fix: append it to the URL. |

## Design Notes

**Seed in V1, not on first read.** The migration inserts the single row, so `GET` never writes, and "exactly one row" holds from the first start. Epic 3 changes the default for new installs (`welcome`) with its own migration. Database IDs are UUIDs per the conventions. A fixed literal (`'00000000-0000-0000-0000-000000000001'`) is portable, where `RANDOM_UUID()`/`gen_random_uuid()` are not.

**Test data dir.** Pointing `app.data-dir` at `target/test-data` through Surefire keeps the real file-mode URL under test, and `mvn clean` resets it. The persistence test uses its own `@TempDir`, so the shared DB only ever holds defaults.

**Module boundary.** `web` depends on `learner` through `LearnerService` and `LearnerView`. The entity and repository stay package-private in `learner`.

## Verification

**Commands:**
- `./mvnw clean verify` -- expected: BUILD SUCCESS. Backend tests, including `LearnerControllerTest` and `LearnerPersistenceTest`, and Vitest pass.
- `ls ~/.mathjourney` before and after the build -- expected: unchanged (or still absent).
- `git status --short frontend/openapi.json` after a second `verify` -- expected: no further change.
- Jar check: `java -jar backend/target/math-journey.jar --app.data-dir=$TMP &`, then `curl -s 127.0.0.1:8080/api/learner` -- expected: the defaults. Stop it, change `companion_name` via H2 `org.h2.tools.Shell` (the h2 jar from `~/.m2`), run `./mvnw -q package -DskipTests`, restart on the same `$TMP`, and curl -- expected: the changed name.
