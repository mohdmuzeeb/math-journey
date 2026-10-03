---
review-of: ../architecture-math-learning-app.md
reviewer: independent rubric reviewer
date: 2026-10-03
inputs: brief-math-learning-app.md, addendum.md, EXPERIENCE.md, .memlog.md
---

# Rubric Review: Math Journey Architecture Spine

## Verdict

The spine is solid on the plumbing: one deployable, generated contracts, schema-held content, Flyway, and server-owned "today". But it leaves the **learning domain** under-specified, and the learning domain is where the units built one level down will actually diverge. Specifically, nothing pins down:

- how Leitner moves aggregate per concept;
- what a session is made of;
- where curriculum order and land membership live;
- how the prerequisite check and first run feed progress;
- what streak and replay mean.

AD-8 as written has a correctness bug: a concept can reach "easy" on its first day. There are also operational gaps. Tablet use conflicts with the localhost-only binding, and the location of the data and content directories depends on the working directory. Not ready to bind stories until findings F1–F6 are fixed.

Severity counts: 1 critical, 6 high, 9 medium, 5 low.

---

## Part 1: Rubric walk

### Checklist summary

| Check | Result |
| --- | --- |
| Fixes the real divergence points one level down, missing none | **Partial.** The plumbing divergences are covered. The learning-domain divergences are not: curriculum order, session shape, Leitner aggregation, prerequisite seeding, streak and replay semantics, the concept file envelope, and item ID stability. |
| Every AD Rule is enforceable and prevents its divergence | **Partial.** AD-8 doesn't prevent inconsistent mastery and creates a bug (F1). AD-5 and AD-7 together create two type sources for one payload (F7). AD-7 and AD-11 have no enforcement mechanism (F14, F18). AD-3's "canonical compare" is undefined for multi-answer kinds (F12). |
| Nothing under Deferred lets two units diverge | **Mostly.** "Frontend state library" can let screens diverge on staleness after an attempt (F17). "Exact kind set beyond the seven seed kinds" refers to seven kinds the spine never names (F19). |
| Every owned dimension is decided, deferred, or an open question | **Partial.** The following are silent: deployment and operations (how the app starts, where data lives, the upgrade procedure, the Java runtime on her laptop), tablet network reachability, the content update and review workflow, and module dependency direction for streak. |

### Findings

#### F1 (critical): AD-8 lets a concept jump from box 1 to box 5 in one session, and leaves several transitions undefined

- **Location:** AD-8; AD-4 ("concept progress … one transaction per attempt").
- **Problem:** Leitner boxes are per concept, but the moves are triggered per attempt. A new-idea phase has many guided-practice items for the same concept. Five first-try correct answers would carry the concept 1→5 ("easy") on day one, and the due date would land 30 days out.

  The rule is also silent on several cases:
  - correct on try 2 (the memlog assumes "keeps box", but the spine dropped that);
  - correct after pressing "I'm stuck" or seeing a hint;
  - the "similar problem" solved after a walkthrough;
  - when a brand-new concept first gets a progress row, and with which box and due date;
  - the label for a concept with no progress ("not started" or "locked") in Land detail.

  Units implementing attempts, warm-up selection and Land detail will each guess differently.
- **Fix:** Replace the AD-8 rule with:
  > **Rule:** Each concept has at most one Leitner move per day key. The move is applied when the session part containing the concept's last item for that day completes (or at day rollover for an abandoned session), from that day's attempts on the concept:
  > - (a) any item reached the walkthrough (try 3 wrong, or "Show me") → box 1;
  > - (b) else every item correct on try 1 with no "I'm stuck" → up one box (max 5);
  > - (c) otherwise → box unchanged.
  >
  > A concept's progress row is created when it is first introduced in a new-idea part, at box 1, due on the next day key. Due date = move day + interval(box). Mastery labels are derived by the backend and returned by the API: no row = *not started*; boxes 1–2 = *new*; 3–4 = *practicing*; 5 = *easy*. Replay and prerequisite-check attempts never move boxes (see F5 and F4).

#### F2 (high): No curriculum manifest; concept order, land membership, prerequisites and "land complete" are undefined

- **Location:** AD-5, AD-10, and the Structural Seed (`content/concepts/<conceptId>.json` only).
- **Problem:** The map, the "current stop", the next new concept, land unlock, "Land detail lists the concepts in that land", and the brief's "path ordered by prerequisites" all need an ordering and grouping that the spine never says where to store. Several things could diverge:
  - Content authors could put order in each concept file, as `order: 7`.
  - The learning module could sort by CCSS code.
  - The frontend could hard-code the five lands.

  "Concept finished" (which drives the stop moving forward and land completion) is also undefined.
- **Fix:** Add a new AD:
  > **AD-12: One curriculum manifest owns order.** `content/curriculum.json` (with its own schema) lists the five lands in order. Each land holds an ordered list of concept IDs, and optionally `prerequisites` per concept. It is the only source of path order and land membership; concept files do not carry order. A concept is *introduced* when its new-idea part is completed. The current stop is the first non-retired concept in manifest order that is not yet introduced. A land is complete when all its non-retired concepts are introduced. Startup validation fails if the manifest references a missing concept file or a concept file is absent from the manifest (except grade 6–7 prerequisite concepts, listed under a separate `prerequisites` land that is not shown on the map). The map and Land detail render only what `/api/journey` returns.

#### F3 (high): Session shape and assembly rules are not architecture; SESSION_ITEM has no part or break

- **Location:** AD-2 (it names "session assembly" but doesn't define it), the ER diagram, and AD-4.
- **Problem:** The brief and EXPERIENCE fix a three-part session:
  - warm-up of about 8 items;
  - break;
  - new idea with 1–2 concepts, a discovery activity, then guided practice;
  - break;
  - wrap-up of mixed practice that ends on a problem she can solve.

  The spine doesn't say:
  - whether the session is assembled once and frozen, or recomputed on resume;
  - how 1 vs 2 new concepts is decided;
  - what happens when fewer than 8 concepts are due (early days);
  - how "ends on a problem she can solve" is chosen;
  - how breaks are represented in session position.

  The three-segment progress bar and the break screens need the part boundaries from the API. If the session isn't frozen, resume can land on a different item than she left.
- **Fix:** Add a new AD:
  > **AD-13: A daily session is assembled once and frozen.** `DAILY_SESSION` is unique per (learner, day key, type=DAILY). It is created idempotently by `POST /api/sessions/today` (get or create). At creation it materializes ordered `SESSION_ITEM` rows, each with `part ∈ {WARMUP, NEW_IDEA, WRAP_UP}` and a `seq`; nothing is recomputed afterward.
  > - **Warm-up:** up to 8 due concepts, most overdue first (AD-8). If fewer are due, fill from prerequisite gaps, then from the most recent introduced concepts.
  > - **New idea:** the next one concept from the manifest, or two if the content file sets `pairWith`.
  > - **Wrap-up:** N items mixing today's concept(s) and introduced concepts. The last item is from a concept at box ≥3, or a discovery-level item of today's concept.
  >
  > Session position = (current seq, `atBreak` boolean). A break is the position after the last item of WARMUP or NEW_IDEA, until "Continue" is posted. The API returns parts and counts for the progress bar. Item counts per part live in `application.yml` under `app.session.*`.

#### F4 (high): Prerequisite check and first-run flow are absent from the spine

- **Location:** The module table, AD-2, and the ER diagram. Even the memlog constraint list omits it.
- **Problem:** The brief and EXPERIENCE require a first-run Welcome, then a prerequisite check of about 10 items on grade 6–7 skills, whose gaps are "added to early warm-ups". The spine doesn't say:
  - where first-run state lives;
  - who decides whether the app opens on Welcome or the Map;
  - whether grade 6–7 content exists as concepts (AD-10's `<ccss-code>` would allow `7.NS.1-…`);
  - how a gap becomes a warm-up entry;
  - whether these attempts touch Leitner.

  The frontend and backend will each invent this differently. A route guard in React that reads localStorage, for example, would violate AD-2.
- **Fix:** Add a new AD:
  > **AD-14: First run and prerequisite check are server state.** `LEARNER_PROFILE` has `onboardingStage ∈ {WELCOME, PREREQ_CHECK, DONE}`, and `GET /api/learner` returns it. React routes on it and stores nothing. The check is a session of `type=PREREQ` built from the manifest's `prerequisites` land, one or two items per concept. On completion, each prerequisite concept with any wrong final answer gets a `CONCEPT_PROGRESS` row at box 1, due today, so it enters warm-up via AD-8. Correct ones get no row. Prerequisite concepts are never "current stop". The profile row is created at first `GET /api/learner` (or by the V1 migration) with stage WELCOME.

#### F5 (high): Streak, day number, "done for today", replay and rollover semantics are undefined, and streak ownership creates a module cycle

- **Location:** AD-9 (it covers clock only), the module table ("learner owns streak"), and the dependency arrows (LEARN → LRN only).
- **Problem:**
  - It's undefined what earns a streak day (session completed? started?).
  - "Day 12" is undefined (calendar days since start, or count of completed sessions?).
  - It's unclear whether the streak is stored or derived.
  - The UX allows replay after "Done for today" and from Land detail; whether replay attempts move Leitner or streak is unspecified.
  - An unfinished session at day rollover (started at 11:50pm, or abandoned yesterday) is unspecified: does she resume it tomorrow, or does it close?

  Also, `learner` owns the streak, but session completion lives in `learning`. The learner module would have to read `DAILY_SESSION`, which is a reverse dependency.
- **Fix:** Add a new AD:
  > **AD-15: Day semantics.**
  > - A *completed day* is a day key whose DAILY session reached the end of WRAP_UP.
  > - The streak is derived (never stored): the count of consecutive completed day keys ending today, or yesterday if today isn't completed yet.
  > - Day number = count of completed day keys + 1 (if today isn't completed).
  > - An unfinished DAILY session from an earlier day key is closed as `ABANDONED` on the first request of a new day. Its recorded attempts stand and feed AD-8, and a fresh session is assembled.
  > - Replay creates a `type=REPLAY` session whose attempts are logged but never move Leitner, the streak or the current stop.
  >
  > Move streak and day number to `learning` (computed from DAILY_SESSION), or have `learning` push `recordDayCompleted(dayKey)` into `learner`. Either way, `learner` never reads learning tables.

#### F6 (high): Tablet use is a requirement, but the spine binds to localhost only

- **Location:** Conventions ("Auth: Spring binds to localhost only"), AD-1, and the deployment diagram. The brief requires laptop and tablet; EXPERIENCE says tablet touch is the secondary input.
- **Problem:** A tablet can't reach `localhost:8080` on the laptop. Either the tablet requirement silently becomes "touchscreen laptop only", or someone later flips the bind address and opens an unauthenticated app to the home Wi-Fi. That's an undecided deployment dimension with architectural consequences: bind address, the host and port the frontend calls, and a LAN trust assumption.
- **Fix:** Pick one and record it.
  - **Option A:** "v1 tablet support means touch input on the same device; the server binds to localhost." Add a Deferred row for LAN access.
  - **Option B:** Add `app.server.bind-address` (default `127.0.0.1`; set `0.0.0.0` for tablet-on-LAN), with a documented assumption that the home LAN is trusted and there's still no auth. The frontend always uses relative `/api` URLs (this already follows from AD-1; state it).

  Either way, add an Open Question if undecided.

#### F7 (high): Two type sources for the activity payload (AD-5 vs AD-7)

- **Location:** AD-5(c) (TypeScript types generated from content schemas) and AD-7 (all API types generated from OpenAPI, built from Java classes).
- **Problem:** Activity items reach React through the API. If Java maps each kind to a DTO class, springdoc emits a second TypeScript type for the same payload, and the renderer could use either. The Java DTO and the JSON Schema can drift, which is the exact divergence both ADs claim to prevent.
- **Fix:** Add this to AD-5:
  > The activity payload crosses the API as opaque JSON: `JsonNode` in Java, published in OpenAPI as `{kind: string, data: object}`. Renderers type `data` only with the schema-generated types, narrowed by `kind`. No Java DTO mirrors a kind schema. Java verifiers read the JSON via the same schema, or with generated POJOs (jsonschema2pojo) that are never exposed through controllers.

### Medium findings

#### F8 (medium): No concept file envelope schema; hints, walkthrough and similar problem are unspecified

- **Location:** AD-5 and AD-6 (schemas exist per *kind* only).
- **Problem:** A concept file also contains:
  - the discovery activity;
  - guided practice;
  - review items (warm-up items);
  - two hints per item (the try-2 hint, and the "I'm stuck" first rung);
  - a worked walkthrough with steps;
  - a "similar problem";
  - `retired`;
  - CCSS code and land.

  Without an envelope schema, each authoring pass and each loader will structure these differently. The walkthrough and similar problem are also needed for *every* practice item, or the try-3 path breaks at runtime.
- **Fix:** Add `content/schemas/concept.schema.json` as the single envelope. It has `id`, `ccss`, `title`, `retired`, `reviewed` (see F11), and `phases: {discover[], practice[], review[]}`. Each item has `id`, `kind`, `data`, `answer`, `hints[1..2]`, and `walkthrough{steps[], similar{data, answer}}`. Each item's `data` and `answer` are validated against `<kind>.schema.json`. Startup fails if any practice or review item lacks a hint or walkthrough.

#### F9 (medium): Item IDs `<conceptId>#<n>` are only "stable within a file"; content edits break frozen sessions and history

- **Location:** Consistency Conventions (IDs).
- **Problem:** The brief expects weekly adjustment ("the app can change the following week"). If an item is inserted or reordered, `#n` shifts. Then:
  - in-progress `SESSION_ITEM` rows and attempts point to different items;
  - resume lands on the wrong problem;
  - the attempt history silently changes meaning.
- **Fix:** Item IDs are permanent, like concept IDs:
  - Assign `#n` append-only and never renumber or reuse; removed items keep their number retired.
  - Startup validation fails on duplicate item IDs.
  - On resume, any `SESSION_ITEM` whose item ID no longer exists is skipped and logged.

#### F10 (medium): Attempt reporting isn't idempotent, and the try number comes from the client

- **Location:** AD-4.
- **Problem:** Several things can go wrong:
  - A network retry or double-click on Check double-posts the attempt, which double-counts it.
  - The client sends `try number`, so after resume mid-item the client and server can disagree: the client resets to try 1, while the server has 2 attempts logged.
  - It's unspecified whether React advances before the POST is acknowledged. If it does and the POST fails, the server position lags, and resume replays items she already did.
- **Fix:** Amend AD-4 so that:
  - Each attempt carries a client-generated `attemptId` (UUID), and the server ignores duplicates.
  - The server derives the try number from the attempt log for that session item. The client's value is advisory or removed.
  - Feedback shows instantly (AD-3), but React moves to the next item only after the attempt POST returns 2xx. On failure it retries the same `attemptId` and shows a gentle Lumi message.
  - Break "Continue" is also a POST, because position is server-owned.

#### F11 (medium): The brief's content correctness check is only half covered

- **Location:** AD-3 and AD-5. The brief's Content section requires worked solutions verified independently *and* a parent skim of each new unit.
- **Problem:** AD-3 verifies the stored answer. It doesn't verify:
  - walkthrough steps (a wrong worked example is the most damaging failure the brief names);
  - "similar problem" answers;
  - prerequisite-check items.

  There is also no gate for the parent skim, so an unskimmed unit reaches her as soon as the file exists.
- **Fix:**
  - Extend AD-3: verifiers run on every answer-bearing node, including `walkthrough.similar` and each walkthrough step's intermediate state where the kind supports it (for example, balance-scale step equations stay equivalent).
  - Add to AD-5: concepts carry `reviewed: true|false`. Session assembly never introduces a concept whose `reviewed` is false, and the startup log lists unreviewed concepts.

#### F12 (medium): AD-3's "compare canonical values" is undefined for kinds with many correct answers or continuous input

- **Location:** AD-3.
- **Problem:** Several kinds don't have a single canonical answer:
  - `coordinate-graph`: any two points on the line;
  - `scatter-plot`: an "informal line of best fit";
  - `shape-transform`: equivalent transformations;
  - `sort-match`: order-insensitive groups.

  Each renderer will invent its own tolerance or equivalence, which is a hidden evaluator. That's the divergence AD-3 claims to prevent.
- **Fix:** Amend AD-3:
  - Each kind schema defines `answer` as either one canonical value or an `accepted[]` set.
  - Each renderer snaps input to the kind's grid or step (from the schema), so canonicalization is exact.
  - Numbers are exact rationals encoded as `"p/q"` strings, with no float compares.
  - Tolerance bands, where unavoidable (line of best fit), are data in the item, not code.
  - Canonicalization from UI state to canonical value lives in the kind's renderer folder and is unit-tested against the Java verifier's fixtures.

#### F13 (medium): Data directory and content location depend on the working directory, and run, upgrade and runtime are unaddressed

- **Location:** The deployment diagram (`./data/mathjourney.mv.db`), Config (`app.content-dir`), and the Structural Seed (`content/` at the repo root).
- **Problem:**
  - A relative `./data` means launching the jar from another folder silently creates a fresh, empty DB. Progress loss is acceptable as a risk, but not as a routine accident.
  - `app.content-dir` reading the filesystem means the jar and the content can be mismatched (or missing) outside the repo.
  - It's unstated how the app gets started each evening (she has to start a Java 25 server before 6pm).
  - The upgrade procedure is unstated (Flyway runs migrations on startup: is that the intended path?).
  - There's no dev vs prod profile (H2 console, logging).
- **Fix:** Add a new AD:
  > **AD-16: Runtime and operations.**
  > - Content is packaged into the jar under `classpath:content/` (the Maven build copies `content/`). `app.content-dir` exists only as a dev override.
  > - The DB lives at `app.data-dir`, default `${user.home}/.mathjourney/`, which is absolute.
  > - The app is started by a `run.sh`/`run.command` script in the repo, with an optional macOS LaunchAgent (on her login).
  > - An upgrade is: stop, replace the jar, start. Flyway migrates on start.
  > - The H2 console is enabled only under the `dev` profile.
  > - Logs go to `${app.data-dir}/logs`.
  >
  > Add an Open Question: is the JDK installed on her laptop, or is a jlink'd runtime shipped?

#### F14 (medium): AD-7 has no stated generation mechanism, and there's a build-order cycle

- **Location:** AD-7 and the Structural Seed paragraph.
- **Problem:** The frontend build runs inside the Maven build (AD-1), but the OpenAPI document needs the compiled, running backend. Without a stated mechanism, each story will improvise, for example by hand-copying `openapi.json` or skipping regeneration. Then "a Java change surfaces as a frontend type error" doesn't happen.
- **Fix:** Add to AD-7:
  > The OpenAPI document is committed as `frontend/openapi.json`. A backend test (`OpenApiSnapshotTest`) boots the context, fetches `/v3/api-docs`, and fails `./mvnw verify` if the result differs from the committed file, printing a regeneration command. The frontend build generates types from the committed file.

  This breaks the cycle and makes drift fail the build.

#### F15 (medium): The "stuck→review queue" is named but has no definition separate from Leitner

- **Location:** The module table, AD-2, and EXPERIENCE ("the concept is added to future warm-ups").
- **Problem:** It's unclear whether this is a separate queue or table, or just "reset to box 1". Two implementers may build both, so a concept is double-scheduled. Or a new concept that hits the walkthrough on day one may get no progress row (F1) and never appear.
- **Fix:** State in AD-8: "The stuck→review queue *is* the Leitner box-1 reset. No separate structure exists." Combine with F1 so new concepts always have a row.

#### F16 (medium): The "I'm stuck" ladder isn't modeled in the attempt contract

- **Location:** AD-4 (`used-stuck flag`).
- **Problem:** EXPERIENCE's ladder has these rungs:
  - "I'm stuck" (any time, even before a try) goes to the hint;
  - then "Show me";
  - then the walkthrough;
  - then the similar problem.

  A single boolean can't tell "used hint" apart from "used walkthrough", and AD-8 needs that distinction (F1(a) vs (c)). The similar problem's outcome is also not reported.
- **Fix:** Replace the flag with `assist ∈ {NONE, HINT, WALKTHROUGH}` on each attempt. Report the similar-problem attempt as an attempt on the same session item with `phase=SIMILAR`. Ladder state is server-derived on resume, the same way as the try number (F10).

### Low findings

#### F17 (low): The deferred "frontend state library" can cause stale-data divergence

- **Location:** Deferred.
- **Problem:** One screen may cache `/api/learner` while another refetches. After an attempt or completion, the map can then show the old streak or stop, and the units disagree on invalidation.
- **Fix:** Either decide now ("TanStack Query; every mutation invalidates the `session`, `learner` and `journey` keys"), or add the rule "no client cache: screens fetch on mount".

#### F18 (low): AD-11's "portable SQL" is unenforced

- **Fix:** Run the H2 URL with `MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`, and optionally run a Testcontainers Postgres Flyway-migrate test in `verify` (it can be skipped when Docker is absent).

#### F19 (low): The "seven seed kinds" are named only in the memlog

- **Fix:** List them in AD-6: `balance-scale, number-line, coordinate-graph, shape-transform, volume-fill, sort-match, scatter-plot`.

#### F20 (low): The Tests convention says "in CI", but CI is deferred

- **Fix:** Say "`./mvnw verify` locally is the gate; CI is deferred".

#### F21 (low): The learner's name has no home

- **Location:** The learner module and Settings.
- **Problem:** Lumi addresses "Maaheem" by name, but only the companion name is stored. A hard-coded name blocks the "friends run their own copy" path.
- **Fix:** Add `learnerName` to `LEARNER_PROFILE`, set in Welcome or config `app.learner-name`.

---

## Part 2: Reconciling the inputs

| Input requirement | Source | Spine status | Finding |
| --- | --- | --- | --- |
| Resume on the exact item, across closing the laptop | EXP Flow 4, State Patterns | Covered by AD-4, but fragile: not frozen, not idempotent, item IDs unstable, midnight rollover undefined | F3, F9, F10, F5 |
| Prerequisite check finds grade 6–7 gaps, which feed early warm-ups | Brief Solution; EXP IA, Flow 1 | **Silently dropped** | F4 |
| First-run Welcome, then check, then map reveal | EXP State Patterns | **Silently dropped** | F4 |
| Session 15/30/15 with two breaks, about 8 review and 1–2 new, wrap-up ends solvable | Brief; EXP IA, Break screen | Only "up to 8 due" is covered; the structure is dropped | F3 |
| Three-segment progress bar that never moves backward | EXP Components | Needs the part boundaries from the API | F3 |
| Stuck ladder (try 1 nudge, try 2 hint, try 3 walkthrough, similar problem) adds the concept to warm-ups | EXP State Patterns, Flow 3 | Partial: AD-8 resets to box 1; ladder states not modeled; new-concept row creation unspecified | F1, F15, F16 |
| "I'm stuck" any time, even before a wrong try | EXP Components | Reduced to a boolean flag; its effect on mastery is undefined | F16, F1 |
| Land detail shows new, practicing or easy per concept; earlier concepts become easy | EXP IA; Brief | Labels derived, but a day-one jump to easy is possible; no "not started"; land membership unsourced | F1, F2 |
| Linear map: land unlocks when the current one is finished; current stop; locked stops | EXP Story Layer | **Silently dropped** (no order or land source) | F2 |
| Streak grows on completion and resets quietly on a missed day; "Day 12" | EXP Flow 2, State Patterns | Clock covered (AD-9); semantics undefined; module cycle | F5 |
| "Done for today" and replay, including replaying a finished concept from Land detail | EXP Components, State Patterns | **Silently dropped** (replay's effect on progress undefined) | F5 |
| Settings (sound, rename Lumi) | EXP Settings | Covered (learner module); learner name missing | F21 |
| Sound | EXP | Settings persistence covered; assets deferred correctly | none |
| Content correctness: computed verification and a parent skim per unit | Brief Content | Half covered: answers only, no skim gate, walkthroughs unverified | F11 |
| New units added without code changes | Brief Open Questions | Covered by AD-5 and AD-6, but needs the envelope and manifest | F8, F2 |
| Laptop *and tablet*, every drag works with either | Brief Scope; EXP Foundation | **Contradicted** by the localhost-only bind | F6 |
| Hints on wrong answers (no plain "incorrect") | Brief | Content-structure consequence, not in the schema | F8 |
| Keyboard alternative to drag | EXP Accessibility | dnd-kit core's KeyboardSensor chosen; fine | none |
| Friends may run it later ("don't make sharing impossible") | Brief | Covered (each family runs its own copy; Postgres path); learner name hard-coding risk | F21 |
| Coverage of all five domains by year end (pacing) | Brief Success | 1 vs 2 new concepts per day is undecided, which affects pacing | F3 |
| No time limits, never lose progress | EXP Interaction Primitives | The working-directory-relative DB makes routine progress loss likely | F13 |

### Pure-UI items ignored as having no architectural consequence

Lumi bubble timing, the "nice streak" every third correct (a client-side, per-screen counter, cosmetic, not learning logic), the palette, reduced motion, focus order, map animations, and snap or glide behavior.

---

## Suggested order of fixes

1. F1, F15 and F16: rewrite AD-8 and the attempt contract together.
2. F2, F3, F4 and F5: add AD-12 through AD-15 (manifest, frozen session, onboarding, day semantics).
3. F6 and F13: deployment and operations (AD-16, plus a tablet decision).
4. F7, F8, F9, F11, F12 and F14: tighten the content and type contracts.
5. Low findings: one editing pass.
