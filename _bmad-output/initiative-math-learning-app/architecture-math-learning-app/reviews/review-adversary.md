# Adversarial Review: Math Journey Architecture Spine

- **Reviewed:** `../architecture-math-learning-app.md` (draft, 2026-10-03) with `.memlog.md`, brief and `EXPERIENCE.md`
- **Lens:** Find two units, one level down, that each follow every AD to the letter and still build incompatibly.
- **Verdict:** The spine settles the *plumbing* well: one deployable, backend-owned logic, generated contracts and a closed kind registry. It leaves the *learning domain semantics* almost entirely unstated. Leitner granularity, session identity and freezing, item and try lifecycle, the shape of the concept file, curriculum order and land unlock are all open. A content author, the session assembler, the progress service and the React session player can each follow AD-1..AD-11 exactly and still produce a product that does not work. Most of these findings are critical or high because each one decides a database row shape or an API field that is costly to change once stories are built.

Severity key: **critical** means the units cannot work together, or progress data gets corrupted. **high** means visible wrong behavior or a forced rework of a shared shape. **medium** means drift that is likely but recoverable. **low** means tidy-up.

---

## F1. Concept files have no envelope schema, so author and loader invent different shapes. **CRITICAL**

**Units:** Content author (Claude writing `content/concepts/*.json`) vs `content` module loader and `learning` session assembler.

**Divergence:** AD-5 gives one JSON Schema to each *activity kind* and none to the *concept file* that contains the activities. Nothing defines where these live: concept title, land, order, CCSS code, the `retired` flag (AD-10), hints (try-2 hint text), nudge text, the worked example, the walkthrough's similar problem, or which activities are "discovery", "guided practice", "review" and "wrap-up". Author A writes `{ "activities": [...], "hints": [...] }` with hints at concept level. The loader story models `Concept { List<Activity> items; }` with hints per item. Both pass AD-5 because every activity validates against its kind schema. The assembler then has no way to choose "warm-up items" over "discovery items".

**Proposed AD (new AD-12: Concept file envelope):**
> `content/schemas/concept.schema.json` is the single schema for the envelope of every concept file and is validated at startup like the kind schemas (AD-5). It defines at least: `id` (= file name, AD-10), `title`, `ccss`, `land`, `retired`, `prerequisites[]`, and `items[]`. Each item has: `itemId`, `role` ∈ {`discover`, `practice`, `review`, `wrapup`}, `kind`, a `payload` validated by the kind schema, `answer` (canonical, AD-3), `nudge` (try-1 text), `hint` (try-2 and "I'm stuck" text) and `walkthrough` (see AD-17). The schema validates the `kind` payload by `$ref` to `<kind>.schema.json`. Startup fails if a non-retired concept lacks at least N `review` items and one `wrapup` item. The concept file holds no other top-level fields.

---

## F2. Leitner granularity: per attempt or per concept per day. **CRITICAL**

**Units:** `learning` progress service (AD-4: "concept progress written per attempt, one transaction") vs `learning` session assembler and the Land-detail mastery screen.

**Divergence:** AD-4 updates concept progress *on each attempt*. AD-8 says "a correct first-try answer moves the concept up one box". A new concept has around 6 practice items plus wrap-up items, all on the same concept. Implementer A applies AD-8 per attempt, so the concept goes from box 1 to box 5 ("easy", due in 30 days) on the day it was introduced. Implementer B reads AD-8 as one move per review and applies it once per session. Both follow the text. Under A, spaced repetition is broken, and Land detail shows "easy" for a concept learned an hour earlier. Related: the memlog says "correct on try 2 keeps box", but AD-8 does not include it, so a third implementer may treat try 2 as "not first-try, therefore wrong" and reset the box.

**Proposed AD (tighten AD-8):**
> A concept's box moves **at most once per day**, and only on the concept's **first scored item in that day's session**. All other attempts are logged but do not move boxes. Outcome mapping for that scored item: correct on try 1 with no stuck use → box +1 (max 5); correct on try 2 or 3, or after a hint-only stuck → box unchanged; wrong on try 3, or "Show me" walkthrough → box 1. After any scored item, `due = today + interval(box)`, even when the box did not change. Introducing a new concept creates its progress row at box 1 with `due = today + 1`. Replay attempts (F11) never move boxes.

---

## F3. Session identity and when it is frozen: one per day, created when, recomputed or snapshotted. **CRITICAL**

**Units:** `learning` session assembler (`GET /api/sessions/today`?) vs React session player and the resume story.

**Divergence:** The ERD has `DAILY_SESSION ||--o{ SESSION_ITEM`. No AD says (a) what creates a session (map load, "Let's go", or the first attempt), (b) whether there is exactly one per day key, (c) whether `SESSION_ITEM` rows are materialised at creation or computed on read, or (d) how React learns the `{id}` in `POST /api/sessions/{id}/attempts`. Builder A computes the item list on every `GET`. After warm-up attempts change `due` dates, the "due" set shrinks, the list reshuffles, and the stored resume position (an index) points at a different item, which breaks Flow 4. Builder B snapshots the list but creates the session on map load, so opening the map at 11:58pm creates day D's session and pressing "Let's go" at 12:01am uses it. A third builder's "Let's go" calls `POST /api/sessions`, which creates a second session on the same day.

**Proposed AD (new AD-13: Daily session lifecycle):**
> There is at most one `DAILY_SESSION` per day key (unique constraint on `day`). `POST /api/sessions/today` is idempotent: it returns the existing session for today (AD-9) or assembles a new one. The map calls only `GET /api/sessions/today`, which returns `404` or `{status:none}` and never creates. Assembly **materialises** every `SESSION_ITEM` (ordinal, part ∈ {warmup, new, wrapup}, conceptId, itemId) in one transaction, and the list never changes afterwards. Resume position is the `SESSION_ITEM.id` of the first item that is not complete, never an index derived on read. A session started on day D stays day D's session until it completes, even after midnight. Its completion credits day D's streak, and a session from an earlier day that was never finished is abandoned (status `abandoned`) when a session for a later day is created.

---

## F4. Item IDs are positional (`<conceptId>#<n>`), but sessions and attempts persist them. **HIGH**

**Units:** Content author editing a live concept (inserting a better problem as item 3) vs `SESSION_ITEM`/`ATTEMPT` rows that reference item ids.

**Divergence:** The conventions say item ids are "stable within a file", and AD-10 makes only *concept* ids permanent. The brief has the parent tune content "the following week". An author inserts an item, and `#3..#7` all shift. Yesterday's unfinished session, the attempt log and any "which items has she seen" logic now point at different problems with different answers. Both units obeyed the rules.

**Proposed AD (extend AD-10):**
> Item ids are permanent like concept ids. `itemId` is an explicit field in the file (`<conceptId>#<slug-or-number>`), never derived from array position, and is never reused. Removing an item means marking it `retired: true`. Startup fails if an item id that the database references is missing from content. When a materialised `SESSION_ITEM` references a retired item or concept, the server skips it on read and marks it `skipped`. It is never re-resolved to another item.

---

## F5. Try counter and item completion have two owners (client sends the try number). **HIGH**

**Units:** React activity player vs `learning` progress service.

**Divergence:** AD-4 has React send `try number` and `outcome`, so the client owns the try count while AD-2 says the backend owns learning state. On resume (Flow 4) after two wrong tries, does the client restart at try 1? The server has the attempts, but no AD exposes the item's try state, so the client starts again at try 1. That bypasses the try-3 walkthrough and lets a later correct answer count as "first try" (F2), which moves the box up. Nothing defines when a `SESSION_ITEM` is *complete*: correct at any try? After the walkthrough's similar problem? What if the similar problem is also wrong? One player advances after the walkthrough and another loops forever. With no idempotency key, a network retry double-logs an append-only attempt and could move the box twice.

**Proposed AD (tighten AD-4):**
> The server owns item state. Each `SESSION_ITEM` exposes `state` ∈ {`pending`, `in-progress`, `walkthrough`, `complete`, `skipped`}, `triesUsed` and `stuckStage` ∈ {`none`, `hint`, `walkthrough`}, and React renders those values and never counts on its own. The attempt request is `{attemptId (client UUID, idempotency key), sessionItemId, answerCorrect, stuckAction?}`. The server derives the try number, rejects attempts on a `complete` item and treats a repeated `attemptId` as a no-op. An item becomes `complete` on a correct answer at try ≤ 3, or once the walkthrough's similar problem has been attempted (correct or not: the walkthrough always ends on a shown solution, never a loop). Each response returns the updated item and the next resume pointer.

---

## F6. What "correct" means: who canonicalises the learner's drag state. **HIGH**

**Units:** Renderer for `number-line` or `sort-match`, written in one story, vs a renderer for `coordinate-graph` or `balance-scale`, written in another, vs the Java `AnswerVerifier`.

**Divergence:** AD-3 says React compares canonical values and does "no math evaluation", but it never says how a drag state *becomes* a canonical value. The `balance-scale` renderer must decide whether a scale with `x` alone on the left and four unit tiles on the right equals canonical `{"x":4}`. That requires counting tiles, which is arithmetic. The `scatter-plot` renderer must decide whether a point dropped at (2.03, 3.97) counts as (2, 4). `sort-match` must decide whether order matters. Renderer A reduces `4/2` to `2` and renderer B does not. One accepts any order and another does not. The Java verifier computes `x=4` in yet another representation. Each renderer follows AD-3, and "correct" still means something different per kind and per author.

**Proposed AD (tighten AD-3 and AD-6):**
> Each kind's schema defines (1) the **canonical answer shape** and (2) the **response shape**: the learner's raw interaction state, snapped to the schema's declared grid or step, so no tolerance comparison is ever needed. Each kind provides a pure `toCanonical(response)` function in the renderer, with no arithmetic beyond counting and grid snapping. Its behaviour is pinned by shared fixture files `content/schemas/<kind>.fixtures.json` (response → canonical) that both a TypeScript test and the Java verifier test run. Comparison is deep equality after `toCanonical`. Order-insensitive or multiple-answer kinds declare `answers: [...]` or set semantics in the schema, never in renderer code. A kind is not "registered" (AD-6) until its fixtures pass in both languages.

---

## F7. The walkthrough's worked example and "similar problem" have no source. **HIGH**

**Units:** Stuck walkthrough UI story vs content author vs `learning`.

**Divergence:** Flow 3 needs a worked example (2x − 4 = 6), then a *similar* problem (4x − 2 = 10). Nothing in the spine says where these come from. Frontend builder A generates the similar problem by perturbing coefficients in TypeScript, which breaks AD-3 because no verifier ever runs on it. Builder B pulls "the next practice item" in the same concept, which consumes and spoils an item from later in the session. Content author C adds a `similar` field that nothing reads. Nothing says whether the similar problem is a `SESSION_ITEM` or whether attempts on it are logged.

**Proposed AD (new AD-17: Walkthrough content):**
> Every scored item's `walkthrough` block in the concept file is authored content: `{steps[] (each step a full kind payload state plus Lumi text), similar: {kind, payload, answer}}`. Both the final step and `similar.answer` pass the kind's `AnswerVerifier` at startup. Nothing generates problems at runtime in either tier. Attempts on `similar` are logged against the *same* `SESSION_ITEM` with `phase: walkthrough` and never move the Leitner box beyond the reset already applied (AD-8).

---

## F8. Fewer than 8 due concepts, and more than 8 after missed days. **HIGH**

**Units:** Session assembler vs React three-segment progress bar and the break screen, vs the prerequisite-check story.

**Divergence:** AD-8 says "up to 8 due". On days 1–3 nothing has been learned, so zero concepts are due. Assembler A returns an empty warm-up. The player renders an empty segment and then a "Warm-up done ✓" break for a warm-up that never happened. Assembler B fills the slots with not-yet-due concepts (overdue-first ordering is undefined for items that are not due). Assembler C pulls gap concepts from the prerequisite check, but no AD says gaps become progress rows. After a missed week with 20 due, does the session grow, or are 12 deferred? The brief's "~10 concepts/day" requirement is not encoded anywhere.

**Proposed AD (tighten AD-8):**
> Warm-up selection: concepts with `due ≤ today` and not retired, ordered by `(due asc, box asc, conceptId)`, take 8. If fewer than 8 are due, fill with introduced concepts not yet due, ordered by `(due asc)`, but never a concept introduced today. If the warm-up is still empty, the session has **no warm-up part** and the API omits that part. React draws segments and breaks only for the parts returned. Each warm-up concept contributes exactly **one** `review` item, chosen as the least recently attempted `review` item of that concept. Concepts left over beyond 8 stay due and come first tomorrow, and the session never grows. Gap concepts from the prerequisite check (F10) enter as box 1 with `due = today`.

---

## F9. New-concept selection, curriculum order, the "current stop" and land unlock have no source of truth. **HIGH**

**Units:** Content author (land and ordering), session assembler (which 1–2 new concepts), Map screen (the current stop, locked stops, "Day 12" label), Session-complete screen (land unlocked animation).

**Divergence:** Lands and stop order are not in any AD. Author A puts `land` and `order` in each file. The map builder sorts by CCSS code, but `8.EE.7` and `8.F.1` do not follow the prerequisite order the brief requires. The assembler picks "1–2" new concepts with no rule, so one builder always picks 2 and another picks 2 only if yesterday's warm-up went well. If she quits during the new idea, has the concept been "introduced"? Builder A marks it introduced at assembly, which advances the stop. Builder B marks it at session complete. The map and the warm-up then disagree. "Land complete" could mean every concept introduced, or every concept at box ≥ 3. Under the second reading she could be stuck in a land for weeks, and EXPERIENCE does not say. "Day 12" could count completed sessions or calendar days since first run.

**Proposed AD (new AD-14: Curriculum and progression):**
> `content/curriculum.json` (schema-validated) is the **only** source of land order and concept order within a land. Concept files do not carry order. The `content` module exposes `nextUnintroduced()` in curriculum order, skipping retired concepts. A day's new part takes **1** concept by default, and a second one only when that concept is marked `pairWithNext: true` in the curriculum, a choice the author makes. A concept is *introduced* when its first `new`-part item is completed. Its progress row is created at that moment, and nothing happens at assembly. An abandoned new part is re-offered tomorrow. The *current stop* is the first unintroduced concept. A land is *complete* when every non-retired concept in it is introduced, and the next land unlocks then. Mastery is not required. The unlock is reported once, as `landUnlocked` in the response to the session-complete call. "Day N" = the number of completed sessions + 1. React gets every one of these values from the API (AD-2).

---

## F10. The prerequisite check sits outside every AD. **MEDIUM**

**Units:** First-run prerequisite check story vs session and attempt model vs content.

**Divergence:** The prerequisite check has about 10 scored drag items, but AD-4's only write path is `/api/sessions/{id}/attempts`. Builder A creates a fake `DAILY_SESSION` for it, which then counts as day 1 and starts the streak and "Done for today". Builder B writes a new `/api/placement` endpoint that writes concept progress directly from the controller. Grade 6–7 concepts are not in the curriculum. Do they get concept files? Do they appear in Land detail (F12)?

**Proposed AD (new AD-15: Placement):**
> The prerequisite check is a `PLACEMENT` record (one per installation) with its own items and attempts, posted to `POST /api/placement/attempts` and written only by the `learning` progress service. It never creates a `DAILY_SESSION` and never touches the streak. Its items come from concept files whose ids use grade 6–7 CCSS codes (`7.EE.4-...`), listed under `curriculum.json → prerequisites`. These are not shown on the map. A wrong answer on a placement item creates or resets that prerequisite concept's progress at box 1, `due = today`, and the concept then flows through warm-up selection (AD-8) like any other. Finishing placement is the only thing that clears the "first run" state.

---

## F11. Replay from Land detail has no write path. **MEDIUM**

**Units:** Land-detail replay story vs Leitner, streak and session position.

**Divergence:** EXPERIENCE allows replaying finished concepts after "Done for today". The only attempt endpoint is session-scoped. Builder A posts replay attempts to today's completed session, which reopens it or moves the resume pointer and the box. Builder B makes replay client-only and posts nothing. That is arguably fine, but then the "used-stuck → future warm-ups" rule silently does not apply.

**Proposed AD (extend AD-4):**
> Replay is a read-only practice mode. React fetches items with `GET /api/concepts/{id}/items` and checks answers locally (AD-3), but posts **no** attempts and changes no progress, streak or session state. Any later decision to make replay count must go through a new AD.

---

## F12. Mastery label for concepts not yet introduced, and the "stuck→review queue" as a separate structure. **MEDIUM**

**Units:** Land-detail screen vs `learning` mastery API; progress service vs assembler.

**Divergence:** (a) Box 1–2 is labelled *new*, but a concept never introduced has no progress row. One builder shows it as *new*, another hides it, and the API may return `null`, which TypeScript then handles in its own way. (b) AD-2 names a "stuck→review queue" as its own computed thing, while AD-8's box-1 reset already re-schedules the concept. Builder A adds a `REVIEW_QUEUE` table that the assembler reads. Builder B relies on the box reset alone. The concept then appears twice in warm-up, or the queue is never drained.

**Proposed AD (tighten AD-8 and AD-2):**
> The mastery API returns `state` ∈ {`locked`, `new`, `practicing`, `easy`} for every curriculum concept. `locked` means there is no progress row, and nothing else in the app derives labels. There is no separate stuck queue: "added to future warm-ups" *is* the box-1 reset with `due = today + 1` (AD-8), and no table or list named queue exists.

---

## F13. Activity payload types come from two generators. **MEDIUM**

**Units:** Frontend content types generated from `content/schemas` (AD-5c) vs frontend API types generated from OpenAPI (AD-7). Java content classes are a third shape.

**Divergence:** The activity payload reaches React *through* a Spring controller. springdoc describes it from whatever Java class the loader binds to (AD-5b does not say whether that class is generated or hand-written). React therefore has two generated types for the same JSON, and they drift whenever a Java field is renamed or a schema `oneOf` has no counterpart in Java. Both generators follow AD-5 and AD-7.

**Proposed AD (tighten AD-5 and AD-7):**
> Java never re-models activity payloads. The content loader keeps each item payload as a `JsonNode` validated by its schema, and the API exposes it in OpenAPI as an opaque object discriminated by `kind`. React narrows `payload` and `answer` **only** with the schema-generated TypeScript types, keyed by `kind`. Verifiers read `JsonNode` or schema-generated Java records, never hand-written classes. The OpenAPI-generated types cover only envelopes such as session, item state and progress.

---

## F14. Streak rules are left to whoever builds them. **LOW**

**Units:** `learner` streak vs `learning` session completion.

**Divergence:** Two modules could own the streak. AD-2 says it is computed in the backend, and the module table gives it to `learner`. `learning` knows when a session completes. Builder A increments the streak inside `learning`'s transaction, which writes `learner` tables directly. Builder B computes it in `learner` by reading sessions. The reset rule (a missed day, read lazily) also has no owner. One builder resets it with a scheduled job and another recomputes on read.

**Proposed AD (extend AD-9):**
> The streak is derived on read by `learner` from completed `DAILY_SESSION.day` keys (count of consecutive days ending today or yesterday) and is never stored. Completion is marked only by `learning` (`POST /api/sessions/today/complete`), and `learner` never writes session data. There are no scheduled jobs.

---

## Summary table

| # | Severity | Gap | Closing AD |
|---|---|---|---|
| F1 | critical | No concept-file envelope schema (roles, hints, walkthrough) | New AD-12 |
| F2 | critical | Leitner moves per attempt let a concept go from box 1 to 5 in a day; try-2 rule missing | Tighten AD-8 |
| F3 | critical | Session creation, uniqueness and snapshot vs recompute; crossing midnight | New AD-13 |
| F4 | high | Positional item ids break persisted sessions and attempts after edits | Extend AD-10 |
| F5 | high | Client owns try count; item completion undefined; no idempotency | Tighten AD-4 |
| F6 | high | Drag state → canonical answer is unspecified per kind | Tighten AD-3/AD-6 |
| F7 | high | Walkthrough example and similar problem have no source | New AD-17 |
| F8 | high | Fewer than 8 or more than 8 due; empty warm-up part | Tighten AD-8 |
| F9 | high | Curriculum order, new-concept count, introduced and land complete | New AD-14 |
| F10 | medium | Prerequisite check has no model | New AD-15 |
| F11 | medium | Replay write path | Extend AD-4 |
| F12 | medium | Locked label; ghost "stuck queue" | Tighten AD-8/AD-2 |
| F13 | medium | Two generated type sources for activity payloads | Tighten AD-5/AD-7 |
| F14 | low | Streak owner, and stored vs derived | Extend AD-9 |
