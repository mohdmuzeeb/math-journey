---
type: epic
title: "Functions land with the coordinate graph"
parent: initiative-math-learning-app
covers: [CAP-11, CAP-2]
after: []
assignee: ""
risk: medium
---

# Functions land with the coordinate graph

## Description

Every 8.F standard written as reviewed, verified lessons using the coordinate-graph kind (new) plus existing kinds, with each concept appended to its land in `curriculum.json`.

## Outcome

Maaheem can learn every 8.F concept in daily sessions (spec CAP-11, CAP-2).

## Done when

1. Every 8.F standard in the curriculum map has at least one concept file, each meeting AD-12's minimum items and marked `reviewed: true` by the parent (CAP-11).
2. The app starts with all 8.F concepts verified; every new kind here has schema, verifier, renderer with walkthrough mode, and shared fixtures (AD-6).
3. Maaheem completes a daily session whose new idea is a 8.F concept, on her laptop via `start.sh` (CAP-1, CAP-2).

## Boundaries

8.F content and the kinds it needs (the coordinate-graph kind (new) plus existing kinds). Not engine or screen changes; a needed engine change is a story in the owning epic.

## References

- parent — _bmad-output/initiative-math-learning-app/initiative-math-learning-app.md
- spec — _bmad-output/initiative-math-learning-app/spec-math-learning-app/spec-math-learning-app.md, section Capabilities
- constraint — _bmad-output/initiative-math-learning-app/spec-math-learning-app/spec-math-learning-app.md, section Constraints
- architecture — _bmad-output/initiative-math-learning-app/architecture-math-learning-app/architecture-math-learning-app.md
- design — _bmad-output/initiative-math-learning-app/ux-math-learning-app/DESIGN.md, _bmad-output/initiative-math-learning-app/ux-math-learning-app/EXPERIENCE.md
- curriculum map — _bmad-output/initiative-math-learning-app/brief-math-learning-app/addendum.md

## Notes

- Waits on epic-help-and-lumi because: walkthrough mode.
- Waits on epic-journey-map because: the map and unlock.
- Waits on epic-land-equations because: Functions unlocks only after Equations.
- Decision (2026-10-03): concepts appended in curriculum order; concept IDs follow AD-10.
