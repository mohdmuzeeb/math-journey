---
title: 'Number-line hardening'
type: 'feature'
ticket: '9'
created: '2026-10-03'
status: 'built'
baseline_revision: '008088075078995aa4c110e0129d5a023027f428'
route: 'full'
route_source: 'auto'
risk: 'medium'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/initiative-math-learning-app/epic-foundation/epic-foundation-retrospective.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The epic retrospective found gaps between what startup accepts and what the browser does:
- **Geometry (A1):** a number line whose geometry is wrong passes validation. For example, 0–1 in steps of 0.4 draws a droppable 1.2 past the end.
- **Pointer drops (A2):** they are judged by the pointer, not the point.
- **Answers at `min` (A3):** they can't be given with a tap or a short drag, and the point resting on `min` hints at the answer.
- **Test gaps (A5, A6, A7):** a fixture test reads numbers differently from production, the API payload has no server-side test, and nothing runs in a real browser.
- **Logging (C1):** 5xx status exceptions are not logged.

**Approach:**
- **Verifier:** extend the number-line verifier with geometry rules.
- **Pointer drops:** resolve them by the zone under the point's centre.
- **Starting position:** start the point *unplaced*, off the line, so every tick, `min` included, is answered by a drop.
- **Tests and logging:** fix the two Java test gaps and the 5xx logging.
- **Browser smoke test:** add one real-browser smoke test (Vitest browser mode, Playwright Chromium) to `./mvnw verify`.

## Boundaries & Constraints

**Always:**
- **Geometry rules:** startup refuses a number-line payload, in an item or a `similar`, when any of these holds:
  - `max ≤ min`;
  - `(max − min) / step` is not a whole number;
  - `(max − min) / step > 40` (at most 40 steps, 41 ticks);
  - `min`, `max`, `step` or `answer` is not finite as a double, or `step` is 0 as a double;
  - the browser's double snap of the stored answer (`canonicalTick`) is not the exact tick.
- **Where geometry errors go:** a new `AnswerProblem.Field.PAYLOAD`, mapped to `<node>.payload`. The message names the item id and the rule. All existing errors keep their paths and messages.
- **Pointer drops:**
  - Mouse and touch drops land on the zone whose rect contains the point's centre (the dragged rect's centre).
  - If no zone contains the centre, the drop is off the line and the point glides back.
  - Keyboard behaviour is unchanged.
- **The unplaced point:**
  - It starts in a visible spot just left of the line's start, outside every zone, labelled "Number line point, not placed yet".
  - Check stays disabled until the first drop.
  - A cancel, or a drop off the line, before the first placement returns it to that spot.
  - Once placed, a cancel or miss behaves as today and re-reports the previous answer.
- **Fixture test:** `NumberLineFixturesTest` reads numbers with `USE_BIG_DECIMAL_FOR_FLOATS`, like the loader, and validates each fixture's payload and answer against the kind schema's `$defs`.
- **API test:** `ConceptControllerTest` asserts `items[0].payload` `prompt`, `min`, `max`, `step` and `target`, with numeric types for the numbers.
- **Logging:** `ApiExceptionHandler` logs `@ResponseStatus` exceptions whose status is 5xx with a `[web]` prefix, then still returns their ProblemDetail.
- **Browser smoke test:**
  - `@vitest/browser-playwright` 5.0.3, `@vitest/browser` 5.0.3 and `playwright` 1.63.0, pinned exactly, running headless Chromium.
  - A separate Vitest project or config, so the existing jsdom suite is unchanged.
  - Run by Maven in `integration-test` after the jsdom tests, after a `playwright install chromium` step. `-DskipTests` skips both.
- **Styling:** tokens only (no literal style values).

**Never:**
- No change to the API shape or `openapi.json`.
- No new activity kinds.
- No change to the AD-3 comparison semantics (`snap`, `isCorrect`, the fixtures' meaning).
- No relaxing of any existing content check.
- The demo concept's content stays the same; it already satisfies the new rules.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Range not whole steps | min 0, max 1, step 0.4 | startup fails | `$.items[n].payload`, names the item and "whole number of steps" |
| Too many ticks | min 0, max 41, step 1 | startup fails | names "at most 40 steps" |
| 40 steps | min −10, max 10, step 0.5 | passes | — |
| Empty or reversed range | max = min, or max < min | startup fails | names the item |
| Non-finite as double | step `1e-400` or max `1e400` | startup fails | names the item |
| Browser snap disagrees | an answer whose double snap ≠ the exact tick | startup fails | `.answer` path, names both ticks |
| Similar problem bad | the `similar` payload breaks a rule | startup fails | `...walkthrough.similar.payload` |
| Pointer off-centre | ticks < 48px apart; drag released with the pointer over the neighbouring zone but the point's centre over tick k | response = tick k | — |
| Off the line | release with the point's centre outside every zone | glides back; nothing reported unless previously placed | — |
| First load | page loads | point unplaced, beside the line, not on `min`; Check disabled | — |
| Answer at min | drop the unplaced point on tick 0, then Check | "You did it!" when the target is `min` | — |
| 5xx status exception | `@ResponseStatus(SERVICE_UNAVAILABLE)` thrown | 503 ProblemDetail, and a `[web]` error is logged | — |
| Browser smoke | real Chromium, 360px-wide card | keyboard-only answer, off-centre pointer drop, reduced-motion instant return all pass | — |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/app/mathjourney/content/NumberLineVerifier.java`
  - `verify` → `check(...)` holds the exact target/grid/answer checks.
  - `canonicalTick(payload, double)` mirrors the browser's snap. Reuse it for the snap-agreement rule.
  - Run the geometry rules before the target checks, and stop at the first geometry problem for that node.
- `backend/src/main/java/app/mathjourney/content/AnswerProblem.java` -- `enum Field { ANSWER, ACCEPTED_ANSWERS }`. Add `PAYLOAD`.
- `ContentLoader.java:350-356` -- the switch from `Field` to a path suffix. Add `PAYLOAD -> ".payload"`. Verification already runs per node, item and `similar`.
- `backend/src/test/java/app/mathjourney/content/NumberLineFixturesTest.java:25` -- `JsonMapper.builder().build()` (no BigDecimal).
- `backend/src/test/java/app/mathjourney/web/ConceptControllerTest.java` -- asserts only `id`, `items[0].id` and `answer`.
- `backend/src/main/java/app/mathjourney/web/ApiExceptionHandler.java:32-38` -- the `@ResponseStatus` branch returns without logging. `ErrorAndSpaRoutingTest` has nested throwing controllers (409, 410) to copy.
- `frontend/src/activities/number-line/NumberLine.tsx`
  - `useState(0)` for `index` and the `answered` ref (lines 75-77): the point starts on tick 0.
  - `collisionDetection` (line 90) uses `pointerWithin` for pointer drags.
  - `nearestTick` and `tickX` are keyboard helpers.
  - `restore`, `handleDragEnd` and the announcements all reference `index`; they need an "unplaced" state.
- `frontend/src/activities/number-line/testLayout.ts`, `NumberLine.test.tsx`, `Activity.test.tsx`, `App.test.tsx` -- assume the point starts on tick 0. Update them to start unplaced.
- `frontend/vite.config.ts` -- `test.environment: 'jsdom'`. `backend/pom.xml` -- the frontend-maven-plugin executions (`npm-install` in `prepare-package`, `npm-test` in `integration-test`).
- Content that must keep passing: `content/demo/concepts/demo-number-line.json` (all lines ≤ 12 steps), `content/fixtures/number-line/*.json` and `backend/src/test/resources/content-cases/*`. Check them against the new rules.

## Tasks & Acceptance

**Execution:**
- [x] `AnswerProblem.java`, `NumberLineVerifier.java`, `ContentLoader.java` -- the geometry rules and the `PAYLOAD` field.
- [x] `NumberLineVerifierTest.java` -- one test per geometry row in the matrix, plus the 40-step pass.
- [x] `ContentLoaderTest.java` + `content-cases/` -- loader cases for "not whole steps" on an item and "bad similar payload geometry", asserting file and path.
- [x] `NumberLineFixturesTest.java` -- the BigDecimal mapper, plus fixture payload and answer validated against `number-line.schema.json` `$defs`.
- [x] `ConceptControllerTest.java` -- payload field assertions.
- [x] `ApiExceptionHandler.java` + `ErrorAndSpaRoutingTest.java` -- 5xx logging, plus a 503 test that asserts the log line (`OutputCaptureExtension`).
- [x] `frontend/src/activities/number-line/NumberLine.tsx` + `app.css`/`tokens.css`
  - The unplaced start spot, its label, and its glide-back.
  - Pointer collision by the point's centre.
  - Announcements for "not placed yet".
- [x] Update `testLayout.ts`, `NumberLine.test.tsx`, `Activity.test.tsx` and `App.test.tsx` for the unplaced start. Add jsdom tests:
  - an off-centre pointer drop answers the centre's tick;
  - an answer at `min`;
  - a cancel before the first placement returns the point to the start spot with Check disabled.
- [x] `frontend/package.json`, `vitest.browser.config.ts` (or a `projects` entry), `frontend/src/browser/numberLine.browser.test.tsx`
  - Run in real Chromium at a 360px-wide container:
    - a keyboard-only 3/4 answer with Check giving "You did it!";
    - a pointer drag released off-centre landing on the point's tick;
    - an emulated `prefers-reduced-motion: reduce` cancel returning instantly.
  - Add the `test:browser` script.
- [x] `backend/pom.xml` -- a `playwright install chromium` execution and an `npm run test:browser` execution in `integration-test`, after `npm-test`.

**Acceptance Criteria:**
- Given `./mvnw clean verify`, when it runs, then BUILD SUCCESS. All earlier tests pass (adapted only for the unplaced start), the browser smoke test runs in Chromium, and `package-lock.json` holds the new pins.
- Given a copy of `content/demo` with one item's payload set to min 0, max 1, step 0.4, when the jar starts on it, then it exits non-zero, and a `[content]` line names the item and `$.items[n].payload`.
- Given `./start.sh --demo` in a browser, when the page loads, then the point waits beside the line rather than on 0. A mouse drop with the pointer slightly off the point lands where the point is.

## Implementation Notes

- **Verifier:** `NumberLineVerifier.geometry` runs first and returns the first problem only; when it reports one, the target/answer checks are skipped for that node (the `acceptedAnswers` ban still runs). Order: finite doubles (`min`, `max`, `step`), `step` 0 as a double, finite `answer`, `max > min`, at most 40 steps (compared as `range > 40 * step`, so no division on huge quotients), then whole steps. Doubles are read as `BigDecimal.doubleValue()` (`browserDouble`), because Jackson 3's `asDouble()` throws for `1e400` instead of returning Infinity. The snap-agreement rule runs after the exact answer check passes and is reported at `.answer`, naming both ticks.
- **Unplaced point:** `index` is `number | null`; the `answered` ref is gone (a non-null `index` is always her response). Parked via `.number-line__point--unplaced { left: var(--offset-number-line-park) }` (centre 32px left of tick 0); `.number-line` left padding grew to `--space-number-line-park` (56px) to hold it. Keyboard pick-up from the parking spot is over tick 0 (nearest tick), so Space then Enter answers `min`; the pick-up announcement says so ("It's not placed yet; it starts over 0."). The drag's input (keyboard vs pointer) is read from `activatorEvent` in `onDragStart`, which dnd-kit calls before the announcement monitor.
- **Pointer collision:** `zoneContaining(centre of collisionRect)`; containment, so a centre outside every zone is a miss. Mutation-checked: switching back to the pointer's coordinates fails two jsdom tests and the browser off-centre test.
- **Browser smoke:** `vitest.browser.config.ts` (own `include: src/browser/**`), with three Playwright-backed commands (real mouse drag offset by the test iframe's box, key press, `emulateMedia` reduced motion). `vite.config.ts` excludes `src/browser/**` from the jsdom run. Reduced-motion test mutation-checked: without the emulation the point is ~7px from home one frame after Escape.
- **Verification run:** `./mvnw clean verify` BUILD SUCCESS (88 JUnit, 76 jsdom Vitest, 3 Chromium); a second `./mvnw verify` also passed and left `package-lock.json` byte-identical. `-DskipTests` skips `npm-test`, `playwright-install` and `npm-test-browser`. Bad-content jar check: exit 1 with `[content] concepts/demo-number-line.json $.items[0].payload: item "demo-number-line#three-quarters": the line from 0 to 1 is not a whole number of steps of 0.4 ...`, and no data dir created.
- **Not done here:** the manual `./start.sh --demo` look in a desktop browser (the Chromium smoke test covers the same behaviour headless).
- Matrix audit (orchestrator): geometry rows covered by `NumberLineVerifierTest` + 2 loader cases; pointer/unplaced rows by jsdom tests + `numberLine.browser.test.tsx` (Chromium); 503 by `ErrorAndSpaRoutingTest`. `./mvnw clean verify` BUILD SUCCESS in the main checkout: backend 88, jsdom Vitest 76, browser 3.
- Review patches (pass 1): rule that the browser's double step count is finite and equals the exact count; decimal-exponent bound ±400 before exact arithmetic (1e-20000000 refused within 1 s); Chromium reduced-motion park-return test; `Field.ANSWER` assertion; `$defs/answer` negative test. Re-verified: `./mvnw clean verify` BUILD SUCCESS in the main checkout (backend 91, jsdom 76, browser 4).

## Plan Change Log

- A non-finite stored `answer` is reported at `<node>.answer` (Field.ANSWER), not `.payload`: it is a fault of the answer, and the matrix row only names the item. Payload values keep `.payload`.
- `ContentLoader.schemaRegistry` became package-private so `NumberLineFixturesTest` validates against `$defs` with the loader's exact dialect.
- Added a `pretest:browser` script (same generators as `pretest`) so `npm run test:browser` works on a fresh checkout.

## Review Triage Log

### Pass 1 (quick lens): high 0, medium 2, low 3, false 0, maybe-false 0, other 1

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | Exact step count is never compared with the browser's double `Math.round((max-min)/step)` | medium | patch | Reviewer checked with java: 2^53 min, 9 steps → browser 8; ±1e308 with step 1e307 → `Infinity` → RangeError. This is the A1 gap the intent closes. Add a rule that the double count is finite and equal. |
| 2 | Exact arithmetic can hang on a tiny non-zero value (`1e-20000000` reads as 0.0, so it passes the finite gate) | medium | patch | Measured: subtract 3.5 s, remainder > 60 s. The retro listed it under A1. Bound the decimal exponent before arithmetic. |
| 3 | AC 3 (`./start.sh --demo` in a desktop browser) not demonstrated | — | no code change | Manual check; the orchestrator restarts the demo for the user after commit. |
| 4 | Chromium reduced-motion test doesn't cover the return to the parking spot | low | patch | Add the case. |
| 5 | Snap-disagreement test doesn't assert `Field.ANSWER` | low | patch | One assertion. |
| 6 | No negative test that `#/$defs/answer` resolves | low | patch | Add one. |

## Design Notes

**Unplaced, not on `min`.** Starting the point on tick 0 does two wrong things at once: it hints at the answer when the target is `min`, and it makes answering `min` need a drag away and back. A visible parking spot just left of the line's first tick makes every answer a deliberate drop. Keyboard pick-up from the parking spot starts at tick 0, so Space then Enter answers `min`.

**Centre-based pointer collision.** The point is at least 48px wide and ticks can be closer than that, so the zone under the pointer is not the tick she sees. Use the zone whose rect contains the dragged rect's centre. Containment, not nearest, keeps "released off the line" a real miss.

**Why Field.PAYLOAD.** Geometry problems belong to the payload, not the answer. A separate field keeps the error path honest (`$.items[3].payload`) without changing any existing path.

## Verification

**Commands:**
- `./mvnw clean verify` -- expected: BUILD SUCCESS, with `numberLine.browser.test.tsx` reported by the browser run.
- Copy the demo, set an item's payload to max 1 and step 0.4, then run `java -jar backend/target/math-journey.jar --app.content-root=file:$TMP/bad --app.data-dir=$TMP/d --server.port=0` -- expected: exit 1, with `[content] concepts/demo-number-line.json $.items[n].payload: ...`.
- `git status --short frontend/package-lock.json` after a second `./mvnw verify` -- expected: no change.
