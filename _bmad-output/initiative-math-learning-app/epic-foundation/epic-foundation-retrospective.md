---
epic: epic-foundation
date: 2026-10-03
verdict: accepted-with-open-items
criteria: declared
headless: false
---

# Retrospective: epic-foundation — App runs and the lesson pipeline is trustworthy

## Epic summary

- **Epic:** `epic-foundation` (id 1). It has 8 tickets, all at `status: built` and `state: review`, and none is marked `done` yet. Closing them is `bmad-ticket`'s job. `pending_tickets` is empty, and every ticket is finished.
- **Merged:** all 8 commits reached `main`, via PR #1 (1.1–1.3), PR #2 (1.4) and merge `24ace0a` (1.5–1.8).
- **User focus (going in):** build reliability, the answer-checking chain (1.4 → 1.5 → 1.6/1.7), and process overhead.
- **Ranges.** Each ticket has one commit, and every range starts at a recorded plan baseline. Sizes come from `git_evidence.py`.

| Ticket | Plan baseline → end | Commit | Files | +/− |
|---|---|---|---|---|
| 1.1 Tracer | `1e815bd` → `d6def44` | d6def44 | 36 | +3946 / −0 |
| 1.2 API contract & errors | `d6def44` → `ff3fa1b` | ff3fa1b | 18 | +874 / −82 |
| 1.3 Learner profile | `ff3fa1b` → `9865e37` | 9865e37 | 18 | +549 / −0 |
| 1.4 Schemas at startup | `9865e37` → `65f8993` | 65f8993 | 67 | +7810 / −106 |
| 1.5 Answer verification | `65f8993` → `772bb50` | 772bb50 | 21 | +2099 / −19 |
| 1.6 Tokens, registry, strip | `772bb50` → `0919b7b` | 0919b7b | 20 | +961 / −232 |
| 1.7 Number-line input | `0919b7b` → `fe9c71c` | fe9c71c | 12 | +1034 / −152 |
| 1.8 Refactor sweep | `fe9c71c` → `a021ba8` (inferred end; merge `24ace0a` follows) | a021ba8 | 17 | +698 / −46 |

- **Evidence inventory:**
  - **Available:** the epic file, the initiative file, all 8 plans with their Implementation Notes and Review Triage Logs, all 8 commits, and the merged `main`.
  - **Missing:** there are no story files, because no ticket was pulled.
  - **Narrowed:** session transcripts are not in the tree. The process lessons therefore rest on the plans' notes and triage logs. Session observations by this retro's author, who ran 1.3–1.8, are marked *(session)*.
  - **No previous retrospective:** this is the first epic.
- **Diff-scope review:** `bmad-review` ran three lenses (adversarial, edge-case hunter, verification gap) over `1e815bd..a021ba8`. Lockfiles, the generated `openapi.json` and test content-cases were excluded. The lenses were weighted on the boundaries between tickets. Every finding below was re-checked against the source before routing.

## Findings

Each finding lists: severity · *what to do about this instance* · *what would prevent the next one*.

### A. The answer-checking chain (1.4 schema → 1.5 verifier → 1.6/1.7 browser): user focus

**Checked and clean (verification-gap lens):** for every answer Java accepts on a well-formed grid, the browser judges it correct.
- `NumberLineFixturesTest.everyFixtureIsValidContent` runs the Java verifier on the same fixture files that `fixtures.test.ts` runs through the real TypeScript `snap`/`isCorrect`.
- The canonical form is the tick index on both sides.

The weak points are the inputs the chain assumes but never checks.

- **A1. The line's geometry is never validated, so content that passes startup can render wrongly.**
  - **Severity:** high · **fix now** · **lesson:** a cross-ticket contract needs explicit "renderable" rules.
  - **Source:** `content/schemas/number-line.schema.json` `$defs.payload` checks only types and `step > 0`. `NumberLineVerifier.check` (`NumberLineVerifier.java:53-90`) checks the target's range, its grid position and the answer. Nothing checks that:
    - `max > min`,
    - `(max − min)` is a whole number of steps, or
    - the tick count is bounded.
  - **Why it matters:** the browser computes `lastTickIndex = Math.round((max−min)/step)` (`frontend/src/activities/number-line/numberLine.ts:11-13`). Verified: min 0, max 1, step 0.4 gives 3. So it draws a droppable tick labelled 1.2 beyond a line whose aria-label says "from 0 to 1". A tiny step on a wide range creates an unbounded number of drop zones (`NumberLine.tsx` tick and zone generation). `min == max` passes. Extreme values (above 2^53, scale overflow) can make the exact Java check and the browser's double arithmetic disagree, or hang the BigDecimal remainder.
  - **Raised by:** adversarial #3, edge-case #1–#5, verification-gap #1.
  - **Fix:** in the verifier, require `max > min`, `(max−min) % step == 0`, a tick count ≤ N (e.g. 40), and finite double values. Also check that `canonicalTick(answer)` equals the exact tick. Add a loader case for each.
- **A2. Pointer drops are decided by where the pointer is, not where the point is.**
  - **Severity:** medium · **fix now** · **lesson:** the same rule as A6 (a real browser is needed to see it).
  - **Source:** `NumberLine.tsx:91` sends pointer drags through `pointerWithin`. The point is at least 48px wide, so the pointer can be up to about 24px from its centre. When ticks are under 48px apart (a phone-width card, or 10+ ticks), the tick reported can differ from the tick the point visibly sits over.
  - **Raised by:** adversarial #8.
  - **Fix:** use the point's centre (`nearestTick(collisionRect centre)`, as the keyboard path does) for pointer drops too.
- **A3. An answer at `min` is hard to give.**
  - **Severity:** medium · **fix now** · **lesson:** the starting state is part of the activity's contract.
  - **Source:** the point starts on `min` with no response (`NumberLine.tsx:75-77`), Check is disabled, and a tap under the 4px activation distance does nothing (`:113`). Java accepts `target == min` (`NumberLineVerifier.java:69`). To answer `min`, she must drag away and come back.
  - **Raised by:** adversarial #9.
  - **Fix:** start the point unplaced, or count a pick-up and drop on the current tick as a response.
- **A4. Nothing checks the prompt against the target.**
  - **Severity:** medium · **defer to the first real-content epic (epic 5)** · **lesson:** this is an authoring rule.
  - **Source:** "Drag the point to 3/4" is free text that is never compared with `target` (`content/demo/concepts/demo-number-line.json`, `NumberLineVerifier`). An item asked about 3/4 but keyed to 1/2 passes every check.
  - **Raised by:** adversarial #4.
  - **Fix:** render the prompt's number from `target`, or add a loader check, before Claude authors real lessons.
- **A5. The fixture validity test reads numbers differently from production.**
  - **Severity:** low · **fix now**.
  - **Source:** `NumberLineFixturesTest.java:25` uses `JsonMapper.builder().build()`, without `USE_BIG_DECIMAL_FOR_FLOATS`, so `verify` falls back to `BigDecimal.valueOf(double)` instead of the loader's exact path. The fixtures are also never validated against the kind schema.
  - **Raised by:** adversarial #5, verification-gap #5.
- **A6. No real-browser test exists. Layout, touch and keyboard are proven only on a mocked layout.**
  - **Severity:** medium · **fix now** · **lesson:** UI stories with drag or layout need one real-browser smoke test as part of "done".
  - **Source:** `vite.config.ts` uses `environment: 'jsdom'`, and `testLayout.ts:31-38` hard-codes centred rects. No test asserts the point's centring CSS. Touch events carry no `pointerType: 'touch'`. Reduced motion is checked only through a class name.
  - **Track record:** 1.7's high-severity defect (a 24px collision offset) was found only by reading dnd-kit's source (`story-number-line-for-mouse-touch-and-keyboard-plan.md`, Review Triage Log #1). The manual browser checks in the 1.6 and 1.7 plans were handed to the user and are not yet confirmed.
  - **Raised by:** verification-gap #3, #4.
- **A7. The API payload is not asserted on the server side.**
  - **Severity:** low · **fix now**.
  - **Source:** `ConceptControllerTest.java:23-29` asserts only `id`, `items[0].id` and `answer`. The frontend tests stub `fetch` with literal payloads, and `payload` is opaque in OpenAPI, so drift between the payload and the renderer would ship unnoticed.
  - **Raised by:** verification-gap #2.
- **A8. Latent seams for the second activity kind.**
  - **Severity:** low · **defer to the story that adds the second kind**.
  - **Source:**
    - `acceptedAnswers` are verified in Java but never reach the browser. `ConceptItemResponse` drops them, and `isCorrect` has no parameter for them (`ConceptResponse.java:22-28`, `types.ts:16`).
    - `x-equivalence` is declared but read by nothing (`number-line.schema.json:6`).
    - AD-6's fourth leg, shared fixtures per kind, isn't enforced: `checkKindRegistry` checks the enum, the schema and the verifier only (`ContentLoader.java`), and `fixtures.test.ts` hard-codes `number-line`.
  - **Raised by:** adversarial #2, #13; aggregate view.
  - **Fix:** when a second kind is added, carry `acceptedAnswers` through, and add a test that every kind has a non-empty `content/fixtures/<kind>/`.

### B. Build reliability: user focus

- **B1. The intermittent Vitest failure (`Cannot find module …`).**
  - **Severity:** medium · **watch; root cause unconfirmed** · **lesson:** don't let an editor share a build output.
  - **Source:** recorded in the Implementation Notes of the 1.4, 1.5, 1.7 and 1.8 plans, and in 1.3 *(session)*. 1.3–1.7 each had to be re-verified in a fresh git worktree. 1.7's implementer saw jsdom dependencies vanish after `npm ci`.
  - **Suspected cause:** VS Code's Java tooling working on the same folder while `npm ci` deletes `node_modules`. *(Session)*: Eclipse-compiled classes appeared in `backend/target/classes` during 1.4. `.vscode/settings.json` sets `java.configuration.updateBuildConfiguration: interactive`.
  - **Mitigation:** 1.8 (`a021ba8`, `backend/pom.xml` execution `npm-install`) switched to `npm install`. Since then, two `clean verify` runs passed in the main checkout (1.8 plan Implementation Notes). That is not proof of the root cause.
- **B2. `npm install` can rewrite the lockfile during a build.**
  - **Severity:** low · **accept with a check**.
  - **Source:** `backend/pom.xml` `npm-install`. Verified unchanged after two builds (1.8 notes).
  - **Raised by:** adversarial #12.
  - **Guard:** if lockfile drift shows up, add `--package-lock=false`, or check `git diff --exit-code frontend/package-lock.json` in the build.
- **B3. `start.sh` runs a stale jar after the code or content changes.**
  - **Severity:** low · **defer**.
  - **Source:** `start.sh:62-69` builds only when the jar is missing. Content is bundled into the jar, so after a `git pull` the parent runs old content and old validation.
  - **Raised by:** adversarial #12, edge-case #13.
  - **Fix:** rebuild, or warn, when any file under `backend/src`, `frontend/src` or `content/` is newer than the jar.

### C. Startup ordering and errors (1.2 / 1.3 / 1.4 / 1.8 boundaries)

**Checked and clean:**
- Content is validated before Flyway. The verification-gap lens removed `ContentBeforeFlywayConfiguration` in a throwaway worktree, and `ContentStartupOrderTest` failed.
- The 1.8 `@ResponseStatus` branch is tested for 409 and 410, and the 1.2 cases still pass.

- **C1. 5xx `@ResponseStatus` exceptions are not logged.**
  - **Severity:** low · **fix now** (one line) · **lesson:** keep the logging rule next to the status rule.
  - **Source:** `ApiExceptionHandler.java:33-38` returns before `log.error`, including for a reason-only annotation, whose code defaults to 500.
  - **Raised by:** all three lenses.
- **C2. A wrapped `@ResponseStatus` exception still becomes a 500.**
  - **Severity:** low · **accept** (already rejected in the 1.8 triage, #3; nothing throws one wrapped).
- **C3. The "content before database" guarantee hangs on one bean name.**
  - **Severity:** low · **defer to epic 2**, which adds more database consumers.
  - **Source:** `ContentBeforeFlywayConfiguration.java:20-24` orders only `FlywayMigrationInitializer`. A later bean that opens a connection early would bypass it.
  - **Raised by:** adversarial #6, edge-case #10.
  - **Fix:** make the `DataSource` depend on `ContentCatalog` as well.
- **C4. Content errors are logged twice, and the failure is buried in a stack trace.**
  - **Severity:** low · **defer**.
  - **Source:** `ContentCatalog.java:31-37` logs each error, then `ContentValidationException` repeats the list.
  - **Raised by:** adversarial #14.
  - **Fix:** a `FailureAnalyzer` that gives the parent a clean message.
- **C5. The API serves retired concepts and items.**
  - **Severity:** low · **defer to epic 2**, which builds the session API.
  - **Source:** `ConceptController.java:30-35`.
  - **Raised by:** adversarial #11.

### D. Accessibility details (1.6 / 1.7)

- **D1. Focus moves to the feedback strip after Check.**
  - **accept:** EXPERIENCE.md's Accessibility Floor requires it ("After feedback, focus moves to the feedback strip").
  - **Raised by:** adversarial #10.
- **D2. The drop zones are labelled `role="group"`, which adds screen-reader noise.**
  - **Severity:** low · **defer**.
  - **Source:** `NumberLine.tsx`, the `Zone` component.
  - **Raised by:** adversarial #10.
- **D3. An "off the line" announcement may fire right after pick-up.**
  - **Severity:** low · **defer**.
  - **Source:** `NumberLine.tsx:138-143`.
  - **Raised by:** edge-case #12.

### E. Aggregate views

- **Architecture delta:** clean.
  - The only cross-package imports are `web → content` (1) and `web → learner` (2).
  - `content` and `learner` import nothing from each other, and `content` has no database access.
  - Measured with grep over `backend/src/main/java`.
- **Size growth:**
  - `ContentLoader.java` is 482 lines, grown by 1.4, 1.5 and 1.8. It holds schema loading, the kind-registry check, per-concept checks, verification and the curriculum cross-check.
  - It is coherent today, so **defer** the split until the second kind lands; split it at that point.
  - Every other main source file is ≤ 235 lines.
- **Duplication:** the tick snapping exists in TypeScript (`numberLine.ts` `snap`) and in Java (`NumberLineVerifier.canonicalTick`). **Accept:** it is an intentional mirror, guarded by the shared fixtures (AD-3).
- **Pattern divergence:** none found. Logging prefixes (`[content]`, `[web]`, `[learner]`) and error handling are consistent.
- **Spec reconciliation:** see the Acceptance verdict. No requirement was silently dropped.
  - Added behaviour beyond the spec, all recorded in the plans: duplicate-key detection, a ban on `acceptedAnswers` for number-line, and the `@ResponseStatus` mapping.

### F. Process: user focus

- **F1. Plan size is well above the guideline in every story.**
  - **Lesson:** raise the guideline for this project, or trim plan sections.
  - **Source:** the plans are 11,210–13,631 characters (`wc -c`), roughly 2,800–3,400 tokens, against a 1,600-token guideline. The user chose "keep full plan" every time it was asked, so the gate added a question to every story without changing any outcome.
- **F2. The quick review found real defects in 7 of 8 stories.**
  - **Lesson:** keep at least quick review, and use thorough review for high-risk stories.
  - **Source:** the Review Triage Logs.
    - 1.2: SPA dotted-route medium.
    - 1.3: two mediums (tests touching `~/.mathjourney`, the H2 shutdown hook).
    - 1.4: Flyway-before-content medium.
    - 1.5: double-precision medium.
    - 1.6: cancelled-drag medium.
    - 1.7: collision-offset **high**.
    - 1.8: lows only.
  - The epic-wide review then found A1–A3, cross-ticket defects that no per-story review could see.
- **F3. Re-verification overhead.** 1.3–1.7 each needed an extra fresh-worktree build because of B1 (plan Implementation Notes). It is resolved if B1 stays fixed.

## Behavior verification

Exercised end to end on the final jar (`a021ba8`) on 2026-10-03:
- **Demo running on 127.0.0.1:8080:** `GET /` and `GET /map` → 200 (SPA). `GET /api/nope` → 404 `application/problem+json`. `GET /api/learner` → the defaults.
- **Bad content** (demo `items[5].answer` set to 1.0) → exit 1, with `[content] concepts/demo-number-line.json $.items[5].answer: item "demo-number-line#three-halves": stored answer 1 does not equal the computed answer 1.5…`, and **no data directory was created**.
- **Data survival:** a fresh data dir returns the defaults. After the companion name was changed to "Pip" through the H2 Shell and the app restarted, `/api/learner` returned `"Pip"`.
- **Not exercised by this retro:** real-browser mouse, touch and keyboard interaction. It was handed to the user after 1.6 and 1.7, and the user has not confirmed it (see A6).

## Previous-retro follow-through

There was nothing to follow through: no previous retrospective file exists. This is the initiative's first epic.

## Action items

All of these are *proposed*. None was applied by this retrospective.

| # | Action | Covers | Owner |
|---|---|---|---|
| 1 | **Number-line hardening story (before epic 5 content).** Verifier rules for `max > min`, whole steps, a tick cap, finite doubles, and `canonicalTick(answer)` = the exact tick. Pointer drops resolved by the point's centre. A way to answer `min`. The fixture test uses the BigDecimal mapper. A server-side payload assertion. | A1, A2, A3, A5, A7 | Dev, via `bmad-build` (new story, created with `bmad-ticket`) |
| 2 | Add one real-browser smoke test (Vitest browser mode or Playwright) to `./mvnw verify`: keyboard answer, pointer drop near a tick, ~360px width, reduced motion. | A6 | Dev; can join #1 |
| 3 | Log 5xx `@ResponseStatus` exceptions in `ApiExceptionHandler`. | C1 | Dev; one line, can join #1 |
| 4 | Confirm the real-browser checks of 1.6 and 1.7 (mouse, touch emulation, keyboard, reduced motion) on the running demo. | A6, verdict | **User** |
| 5 | Watch B1 through epic 2. If the "Cannot find module" failure returns, stop VS Code's Java tooling from building this folder, or build only from a separate worktree. | B1 | User + Dev |
| 6 | **Spec reconciliation (proposal):** add the number-line grid rules from #1 to AD-12/AD-3 in the architecture, and an authoring rule tying the prompt to `target` (A4), before Claude writes real lessons. | A1, A4 | User (architecture owner) |
| 7 | **Process:** raise the plan-size guideline for this project (plans run about 3k tokens), or trim the template, so the size question stops interrupting every story. Keep the quick review, and use thorough review for `risk: high`. | F1, F2 | User |
| 8 | Deferred to epic 2: `DataSource` depends on `ContentCatalog` (C3); the session API filters retired content (C5). | C3, C5 | Dev (epic 2 planning) |
| 9 | Deferred: `start.sh` rebuilds or warns on a stale jar (B3); a clean `FailureAnalyzer` for content errors (C4); screen-reader polish (D2, D3); split `ContentLoader` when a second kind arrives; carry `acceptedAnswers` through and enforce per-kind fixtures (A8). | B3, C4, D2, D3, E, A8 | Dev (refactor sweep of the next epic) |

## Acceptance verdict

**accepted-with-open-items.** This is the machine verdict and awaits the user's decision. The criteria are declared in the epic file's "Done when".

1. **`start.sh` checks for Java 25 and runs the single jar at 127.0.0.1:8080; Flyway creates the database in `~/.mathjourney`; data survives replacing the jar.** Met.
   - The 1.1 plan's matrix audit covers the Java 21 stub and the missing-java case. 1.8 adds the `JAVA_HOME` path.
   - The 1.3 jar check shows "Pip" surviving a rebuild, and this retro repeated it.
2. **The app refuses to start on schema failures, curriculum mismatches and computed-answer mismatches, similar problems included.** Met.
   - `ContentLoaderTest` and `NumberLineVerifierTest`, plus the 1.5 jar check, cover these.
   - This retro reproduced the wrong-answer case. Open item A1: content with malformed geometry still passes.
3. **The demo number line accepts an answer by mouse, touch and keyboard alone, with instant green or amber feedback.** Met on automated evidence: `NumberLine.test.tsx`, `Activity.test.tsx` and `App.test.tsx` all pass, 67 Vitest tests in total.
   - Open: the real-browser confirmation (#4), and the A2/A3 interaction defects.
4. **`./mvnw verify` builds both sides, generates the API and content types, and runs the shared fixtures in Java and TypeScript.** Met. The 1.8 `clean verify` passed with 74 backend and 67 Vitest tests, including `NumberLineFixturesTest` and `fixtures.test.ts`.
5. **Unknown `/api` paths get a 404 ProblemDetail, and other paths get the SPA.** Met. `ErrorAndSpaRoutingTest` covers it, and this retro's curl checks confirmed it.

No ticket is unfinished. The open items are A1–A3, A6 and the user's browser confirmation. None of them blocks epic 2's session logic. A1 should land before epic 5 authors real number-line content.

## Open questions

- Has the user confirmed mouse, touch and keyboard in a real browser (action #4)? A failure there would downgrade criterion 3.
- Should the number-line hardening (#1–#3) be a story 1.9 in this epic, or the first story of epic 5? It changes which epic's verdict carries the open items.
- What is B1's root cause? It stayed unconfirmed: the problem has not recurred since 1.8, but two clean runs are thin evidence.
