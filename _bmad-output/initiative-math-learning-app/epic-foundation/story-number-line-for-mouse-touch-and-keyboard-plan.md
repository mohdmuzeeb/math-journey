---
title: 'Number line for mouse, touch and keyboard'
type: 'feature'
ticket: '7'
created: '2026-10-03'
status: 'built'
baseline_revision: '0919b7b8faa0cdc599b3a42f41e9fd3c963f230b'
route: 'full'
route_source: 'auto'
risk: 'medium'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/initiative-math-learning-app/ux-math-learning-app/DESIGN.md'
  - '{project-root}/_bmad-output/initiative-math-learning-app/ux-math-learning-app/EXPERIENCE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The number line works only with a mouse. Its point is smaller than the 48px drag target, and nothing shows where a drop will land. The TypeScript comparison has never been checked against the shared fixtures from entry 5 (CAP-2, AD-3, the EXPERIENCE.md Accessibility Floor).

**Approach:** Rebuild the number-line renderer on `@dnd-kit/core` 6.3.1:
- **The point:** a draggable, focusable element of at least 48px.
- **Drop zones:** one per tick, each highlighted while the point hovers over it. Dropping on a zone snaps the point to that tick and reports the response.
- **Input:** a pointer sensor for mouse and touch, and a keyboard sensor that moves tick by tick.
- **Accessibility:** screen-reader labels and announcements, and reduced-motion behaviour.

Add a Vitest runner for `content/fixtures/number-line/` that must agree with the Java fixture tests.

## Boundaries & Constraints

**Always:**
- `@dnd-kit/core` 6.3.1, pinned exactly.
- **Keyboard (EXPERIENCE.md):** Tab focuses the point. Enter or Space picks it up. Left/Right (and Up/Down) move one tick, clamped at the ends. Enter or Space drops it, and Esc cancels.
- **Pointer input:** works for mouse and touch through pointer events, with `touch-action: none` on the point.
- **Drop zones** tile the line, one band per tick. A drop outside every band, or a cancel, glides the point back to where it was before the grab. It never disappears.
- **Responses:**
  - A successful drop reports `onResponse(tickValue)`.
  - Grabbing reports `onInteract`, as in entry 6.
  - A cancel or a missed drop returns the point to its previous tick. If that tick was a response, it is reported again, so Check is enabled again.
- **Sizes:** the point is ≥ 48×48 CSS px at every width. Styling uses the drag-tile tokens: white with an orchid border at rest, orchid with a lifted shadow while held. The zone under the point shows the drop-zone dashed orchid outline. No literal style values.
- **Screen readers:** the point is labelled "Number line point at <value>"; each zone is labelled with its value. Custom dnd-kit announcements cover pick up, over, drop and cancel, in Lumi's plain voice.
- **Reduced motion:** with `prefers-reduced-motion: reduce`, there's no glide-back or drop animation, just instant moves.
- **Comparison:** `snap`, `tickValue` and `isCorrect` stay the pure comparison (AD-3). The fixture runner asserts `snap(response, …) === canonical` and `isCorrect(...) ? "correct" : "wrong" === verdict` for every case in every fixture file.

**Never:**
- No change to `Activity.tsx`'s Check/strip contract beyond what the renderer needs.
- No backend change.
- No other activity kinds.
- No `@dnd-kit/sortable` (no list ordering here).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Mouse drop | drag the point onto the 0.75 zone | point snaps to 0.75; `onResponse(0.75)` | — |
| Touch drop | touch, drag, lift on a zone | same as mouse | — |
| Keyboard answer | Tab, Space, → ×3, Enter (0 to 2, step 0.25) | point at 0.75; `onResponse(0.75)`; announcements spoken | — |
| Keyboard clamp | → past the last tick | stays on the last tick | — |
| Keyboard cancel | pick up, move, Esc | back at its previous tick; the previous response, if any, is reported again | — |
| Drop off the line | release far above the line | glides back to its previous tick | — |
| Hover highlight | point held over a zone | that zone shows the dashed orchid outline; no others do | — |
| Reduced motion | the media query matches | no drop or glide animation | — |
| Fixtures | every case in `content/fixtures/number-line/*.json` | TS canonical index and verdict equal the fixture | test names the file and case |

</frozen-after-approval>

## Code Map

- `frontend/src/activities/number-line/NumberLine.tsx` -- today an SVG with a pointer-captured `<circle>` (radius 18 in a 600-unit viewBox, so it scales below 48px). Rewrite it as HTML positioned in `%` along the line, because dnd-kit needs DOM nodes and fixed CSS sizes.
  - Keep: the prompt, `formatTick`, tick labels, and the `RendererProps<NumberLinePayload, number>` contract (`onResponse`, `onInteract`).
- `frontend/src/activities/number-line/numberLine.ts` -- `snap`, `tickValue`, `lastTickIndex`, `isCorrect`. Reuse them; their behaviour must not change.
- `frontend/src/Activity.tsx` -- `onInteract` clears the response and the verdict; `onResponse` records a response and enables Check. A re-report after cancel therefore re-enables Check.
- `frontend/src/App.test.tsx`, `Activity.test.tsx` -- drive the old SVG with pointer events on `data-testid="number-line-point"` and a mocked `getBoundingClientRect`. Update them to the new DOM, keeping their assertions about Check and the strip.
- `content/fixtures/number-line/{quarters,negative-range,tenths}.json` -- `{ description, payload, answer, cases:[{response, canonical, verdict: "correct"|"wrong"}] }`. Reachable from `frontend/` as `../content/fixtures/number-line/`.
- `frontend/src/styles/tokens.css` / `app.css` -- the drag-tile, drag-tile-active, drop-zone and shadow tokens exist. Add any missing ones from DESIGN.md first.

## Tasks & Acceptance

**Execution:**
- [x] `frontend/package.json` -- add `@dnd-kit/core` at exactly `6.3.1`.
- [x] `frontend/src/activities/number-line/NumberLine.tsx`
  - `DndContext` with a `PointerSensor` (small activation distance) and a `KeyboardSensor` whose `coordinateGetter` moves to the neighbouring tick zone's centre.
  - One `useDroppable` per tick, and a `useDraggable` point (a `<button type="button">`).
  - `onDragStart` calls `onInteract`. `onDragEnd` either reports the zone's value or restores the previous tick and re-reports it. `onDragCancel` restores it too.
  - Custom `accessibility.announcements` and `screenReaderInstructions`.
  - Use `useReducedMotion` (a small hook on `matchMedia`) to drop the animations.
- [x] `frontend/src/styles/app.css` -- the point (≥ 48px, `touch-action: none`, rest/active token styles), the zone bands and their `--over` dashed outline. Animations sit behind `@media (prefers-reduced-motion: no-preference)`.
- [x] `frontend/src/activities/number-line/fixtures.test.ts` -- read every `../content/fixtures/number-line/*.json` with Node `fs` (the path relative to `frontend/`), and assert canonical and verdict per case, naming the file and case index on failure.
- [x] `frontend/src/activities/number-line/NumberLine.test.tsx`
  - Keyboard pick up / move / clamp / drop / cancel with announcements.
  - A pointer drop onto a zone (mock the zone rects), the hover-zone highlight class, a drop off the line returning the point, and the point's accessible name.
- [x] `frontend/src/App.test.tsx`, `frontend/src/Activity.test.tsx` -- port them to the new DOM, keeping their behaviour assertions. Add: cancel after an earlier answer re-enables Check.

**Acceptance Criteria:**
- Given `./mvnw clean verify`, when it runs, then it succeeds, the Vitest fixture tests pass, and the Java `NumberLineFixturesTest` passes on the same files.
- Given `./start.sh --demo` in Chrome, when the 3/4 item is answered by mouse, by touch emulation (DevTools device mode) and by keyboard alone, then Check shows "You did it!" each time.
- Given DevTools emulating `prefers-reduced-motion: reduce`, when a drop is cancelled, then the point jumps back instantly.

## Implementation Notes

- Keyboard collision uses the tick nearest the point's centre (not `rectIntersection`): the end bands are half bands, so an intersection test could pick the neighbour at the ends. Pointer collision is `pointerWithin`.
- dnd-kit reports the starting zone as "over" right after pick-up; that first report is silenced so the pick-up announcement is not overwritten.
- The point is centred on its tick with negative margins (`--offset-drag-centre`), not `translate(-50%, -50%)`: dnd-kit measures draggables ignoring their transform, so a centring transform would offset its collision rect by half the tile from the visible point.
- Axis and ticks sit above the zone bands, and the point above both, through `--layer-number-line-*` tokens; the track height and label offset are tokens too.
- Added tokens `--size-drag-min: 48px` (drag-tile.minSize) and `--motion-glide: 180ms ease-out` (DESIGN.md names no motion timing; only used under `prefers-reduced-motion: no-preference`).
- dnd-kit's live region is also `role="status"`, so the Activity/App tests pick the feedback strip by its `feedback` class. dnd-kit swallows clicks for 50ms after a pointer drag; the test helper waits that out before clicking Check.
- `./mvnw clean verify` failed in this checkout with `Cannot find module` (jsdom deps after `npm ci`) and passed in a fresh worktree, as the Verification note anticipates.
- Matrix audit (orchestrator): rows covered by `NumberLine.test.tsx`, `fixtures.test.ts`, `Activity.test.tsx` and `App.test.tsx`. Touch shares the pointer-sensor path and needs a manual check. Fresh-worktree `./mvnw clean verify` BUILD SUCCESS: backend 69 (incl. Java `NumberLineFixturesTest`), Vitest 64.
- Re-verified after review patches (orchestrator): fresh-worktree `./mvnw clean verify` BUILD SUCCESS (backend 69, Vitest 65). The manual browser checks (mouse, touch emulation, keyboard, reduced motion) are handed to the user on the restarted demo.

## Plan Change Log

## Review Triage Log

### Pass 1 (quick lens): high 1, medium 0, low 3, false 0, maybe-false 0

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | Point centred by `translate(-50%,-50%)`; dnd-kit measures it transform-agnostically, so keyboard moves sit 24px off the announced tick, and Enter can drop one tick off when ticks are < 48px apart | high | patch | Cited dnd-kit 6.3.1 internals (`getTransformAgnosticClientRect`, `KeyboardSensor` using `collisionRect.left`). jsdom skips the inverse transform, so the tests miss it. Fix: centre with margins, no transform; make the test layout real; add a narrow-spacing pick-up + Enter test. |
| 2 | The hovered zone band hides the axis | low | patch | Zones follow the axis in the DOM with no stacking order, and `--over` fills opaque. Direct fix: stacking via a token. |
| 3 | `* 2.5` track height and `z-index: 1` literals | low | patch | Break the tokens-only rule. Add named tokens. |
| 4 | `testLayout.ts` mocks the ideal geometry, so the tests can't see #1 | low | patch (grouped with #1) | Same root cause as #1; fixed together. |

## Design Notes

**Why bands, not a nearest-tick hit test.** One droppable band per tick, tiling the line, gives dnd-kit real drop zones to highlight. `rectIntersection`/`pointerWithin` collision then makes "released off the line" a real no-target drop that glides back, as the EXPERIENCE.md drag rule requires. The response is the zone's tick value, so `snap` stays the shared comparison and the fixture contract is unchanged.

**Keyboard coordinates.** dnd-kit's default keyboard step is pixels. A custom `coordinateGetter` returns the centre of the next or previous zone's rect, so one arrow press is one tick at any line width.

## Verification

**Commands:**
- `./mvnw clean verify` -- expected: BUILD SUCCESS, with the Vitest fixture and NumberLine tests included. If Vitest fails with `Cannot find module` in this checkout, rerun in a fresh worktree.
- `cd frontend && npx vitest run src/activities/number-line` -- expected: all pass.

**Manual checks:**
- `./start.sh --demo`: answer 3/4 by mouse, by touch emulation and by keyboard alone. Watch the hover outline, the glide-back off the line, and Esc cancel.
