---
title: 'Answer verification and shared fixtures'
type: 'feature'
ticket: '5'
created: '2026-10-03'
status: 'built'
baseline_revision: '65f8993de876517d0bcd33d565a202523f5f540f'
route: 'full'
route_source: 'auto'
risk: 'high'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/initiative-math-learning-app/architecture-math-learning-app/architecture-math-learning-app.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Stored answers are trusted as written. A wrong `answer` in a Claude-authored lesson, or in a walkthrough's `similar` problem, would mark the learner wrong when she is right (CAP-11, AD-3).

**Approach:** Add a Java `AnswerVerifier` contract and a closed kind registry. Add a number-line verifier that computes each item's answer exactly from `payload.target` on the payload's grid. The content loader runs it for every item and every `walkthrough.similar`, and any mismatch stops startup, naming the item. Also add `content/fixtures/number-line/`, with response → canonical tick → verdict cases, run by Java tests now and by TypeScript in entry 7.

## Boundaries & Constraints

**Always:**
- **Exact verification.** It uses exact arithmetic (`BigDecimal` or a rational), never float equality. Parse `target` as an integer, a decimal or `p/q`, and parse `min`, `max`, `step` and `answer` from their JSON decimal text.
- **What makes a pass.** Verification passes only when `target` lies within [min, max], (target − min)/step is an exact integer, and `answer` equals `target` exactly.
- **Number-line items have exactly one correct tick.** A non-empty `acceptedAnswers` is a verification error.
- **Errors.** They use the existing `ContentError` path: `$.items[n].answer` or `$.items[n].walkthrough.similar.answer`. The message names the item id, the stored value and the computed one.
- **Closed registry.** Startup fails unless the kinds in `concept.schema.json`'s `kind` enum, the `<kind>.schema.json` files and the verifier beans are the same set (AD-6, minus the React renderer, which is entry 6).
- **Fixture semantics** match `frontend/src/numberLine.ts` exactly. The canonical value is the **tick index** `clamp(round((response − min) / step), 0, last)`, using double arithmetic and JS `Math.round` rounding. The verdict is `correct` when that index equals the answer's tick index.

**Never:**
- No TypeScript fixture runner (entry 7).
- No change to the API contract or to frontend behaviour.
- No new activity kinds.
- No support for non-decimal steps such as 1/3; that is a later schema change.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Correct content | demo concept, all 9 items + similars | starts | — |
| Wrong item answer | demo item 2 `answer` 0.5 → 0.75 | startup fails | `concepts/demo-number-line.json $.items[1].answer` names the item id, 0.75 and 0.5 |
| Wrong similar answer | a `similar.answer` changed | startup fails | the `...walkthrough.similar.answer` path |
| Off-grid target | `target` "1/3", step 0.25 | startup fails | names the target and the step |
| Target out of range | `target` "5/2", max 2 | startup fails | names the range |
| Accepted answers present | `acceptedAnswers: [0.75]` | startup fails | the `$.items[n].acceptedAnswers` path |
| Float-noise grid | step 0.1, target "3/10", answer 0.3 | passes, because the arithmetic is exact | — |
| Kind without verifier | loader built with no verifiers | startup fails | names the kind |
| Fixture cases | each `content/fixtures/number-line/*.json` case | Java canonical index and verdict equal the fixture's | — |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/app/mathjourney/content/ContentLoader.java`
  - `ContentLoader(String contentRoot)` is pure, and `load()` collects `ContentError`s.
  - `checkKinds(...)` already walks each item's payload, answer, `acceptedAnswers` and `walkthrough.similar` after schema validation. Add verification there, and only when that item's schema checks passed.
  - Add a constructor parameter `Map<String, AnswerVerifier>`.
- `ContentCatalog.java` -- constructs the loader from `app.content-root`. Inject `List<AnswerVerifier>`, and keep `ContentBeforeFlywayConfiguration` working: the catalog must still validate before Flyway.
- `ContentLoaderTest.java:18,30,37` -- construct `new ContentLoader(root)`. Pass the real verifiers via a test helper.
- **The existing `content-cases/*` must stay correct apart from their intended error.** Several have number-line items, so check that their answers verify. Fix any wrong answer in a case that is not about answers.
- `content/demo/concepts/demo-number-line.json` -- all 18 answer nodes are already correct (e.g. challenge −1/2 on [−1, 1] step 0.25). Leave it unchanged.
- `frontend/src/numberLine.ts` (`snap`, `isCorrect`) -- the reference semantics the fixtures encode. Read it, and don't change it.

## Tasks & Acceptance

**Execution:**
- [x] `backend/src/main/java/app/mathjourney/content/AnswerVerifier.java`
  - A public interface: `String kind()`, and `List<String> verify(JsonNode payload, JsonNode answer, JsonNode acceptedAnswers)`, returning problem messages (empty means OK).
  - Javadoc for the AD-3 contract.
- [x] `backend/src/main/java/app/mathjourney/content/NumberLineVerifier.java` -- a `@Component` implementing the exact checks in Always. It also exposes the package-visible `canonicalTick(payload, response)` and `verdict(payload, answer, response)`, which mirror `numberLine.ts` for the fixtures.
- [x] `ContentLoader.java` / `ContentCatalog.java`
  - Wire the verifiers in.
  - Verify every item and its `similar`. The `similar` has no `acceptedAnswers`; pass null.
  - Map each message to a `ContentError` at the answer's path, prefixed with the item id.
  - Add the registry check from Always, comparing the `kind` enum, the schema files and the verifier kinds.
- [x] `content/fixtures/number-line/*.json` -- 3–4 files. Each is `{ description, payload, answer, cases: [{ response, canonical, verdict }] }`. Together they cover:
  - an exact hit, near-tick snapping either way, and a half-way rounding case;
  - clamping below `min` and above `max`;
  - a negative range, and a step-0.1 grid (target 0.3);
  - a wrong tick.
- [x] `backend/src/test/java/app/mathjourney/content/NumberLineVerifierTest.java` -- unit tests for each verification row in the matrix: fraction, decimal and integer targets, a negative target, off-grid, out of range, an answer mismatch, accepted answers, and the step-0.1 float-noise case.
- [x] `backend/src/test/java/app/mathjourney/content/NumberLineFixturesTest.java` -- loads every fixture file from `../content/fixtures/number-line/` (relative to `backend/`), runs `canonicalTick` and `verdict` per case, and fails with the file and case index.
- [x] `ContentLoaderTest.java` + `content-cases/`
  - New cases `wrong-answer`, `wrong-similar-answer` and `accepted-answers`, each asserting the file and path, with the item id in the message.
  - A registry-mismatch test, using a loader built with no verifiers.
  - Demo and real roots still load.

**Acceptance Criteria:**
- Given `./mvnw clean verify`, when it runs, then all the earlier tests plus the verifier and fixture tests pass.
- Given the jar started with `--app.content-root=file:<copy of content/demo with one similar answer changed>`, when it starts, then it exits non-zero, and a `[content]` line names that item's `walkthrough.similar.answer` path.

## Implementation Notes

- `AnswerVerifier.verify` returns plain messages, so the loader attributes them to paths by calling it twice: with `acceptedAnswers` null (problems go to `.answer`), then with them (the extra problems go to `.acceptedAnswers`). The interface Javadoc requires verifiers to be deterministic for this reason.
- Verification runs per node (item, then `similar`) only when that node's schema checks passed, so existing single-error cases still produce exactly one error.
- The loader's mapper enables `USE_BIG_DECIMAL_FOR_FLOATS`, so `min`, `max`, `step` and `answer` are verified from their exact JSON decimal text (an answer of 0.50000000000000001 for target 1/2 is rejected). The API still serializes them as plain JSON numbers.
- Registry errors are reported at `schemas/concept.schema.json $.$defs.item.properties.kind.enum`, one per kind, naming what is missing. Two verifier beans for one kind throw `IllegalStateException` at startup (`ContentLoader.byKind`).
- Fixture canonical values were generated with Node using the same expressions as `numberLine.ts`, then checked by `NumberLineFixturesTest`. Verdict strings are `correct` / `wrong`.
- `./mvnw clean verify` hit the known Vitest `Cannot find module 'decimal.js'` in this checkout; it passed (67 backend tests, 9 frontend tests) in a fresh worktree.
- Matrix audit (orchestrator): every row is covered by `NumberLineVerifierTest` (15), `NumberLineFixturesTest` (2) and the new `ContentLoaderTest` cases. Fresh-worktree `./mvnw clean verify` BUILD SUCCESS: backend 67, Vitest 9.
- Re-verified after review patches (orchestrator): fresh-worktree `./mvnw clean verify` BUILD SUCCESS (backend 69, Vitest 9). Jar on demo with `items[2].walkthrough.similar.answer` = 1.5: exit 1, `[content] ... $.items[2].walkthrough.similar.answer: item "demo-number-line#five-quarters": stored answer 1.5 does not equal the computed answer 1.75`, no DB created.

## Plan Change Log

## Review Triage Log

### Pass 1 (quick lens): high 0, medium 1, low 1, false 0, maybe-false 0

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | Decimals arrive as `DoubleNode`, so verification is not exact beyond about 15 significant digits | medium | patch | Reviewer showed `0.50000000000000001` → `DoubleNode` → `BigDecimal.valueOf` = 0.5, so a wrong answer passes as 1/2. This breaks the frozen exact-arithmetic rule. Fix: `USE_BIG_DECIMAL_FOR_FLOATS` on the loader mapper, plus a test. |
| 2 | Javadoc says TypeScript already runs the fixtures | low | patch | Untrue until entry 7. A direct wording fix. |

## Design Notes

**Why the canonical value is the tick index.** Tick *values* pick up float noise (0.1 × 3 = 0.30000000000000004), so Java and TypeScript would have to agree on an epsilon. Tick *indices* are integers, which makes the fixtures compare exactly in both languages. The stored `answer` stays a value, and the verifier proves that it is exactly on the grid.

**Two meanings of "correct".** The *verifier* is exact and decides whether content is valid. The *fixtures* encode what the browser does with a drag, which is double arithmetic. Keep the two code paths separate inside `NumberLineVerifier`.

```java
// target "3/4" on [0, 2] step 0.25 → index (3/4 − 0) / 0.25 = 3, exact, so the answer must equal 0.75
```

## Verification

**Commands:**
- `./mvnw clean verify` -- expected: BUILD SUCCESS. If Vitest fails with `Cannot find module` in this checkout, rerun in a fresh worktree (the known VS Code interference).
- Copy `content/demo` to `$TMP/bad`, change `items[2].walkthrough.similar.answer`, then run `java -jar backend/target/math-journey.jar --app.content-root=file:$TMP/bad --app.data-dir=$TMP/data` -- expected: a non-zero exit and `[content] concepts/demo-number-line.json $.items[2].walkthrough.similar.answer: ...`.
