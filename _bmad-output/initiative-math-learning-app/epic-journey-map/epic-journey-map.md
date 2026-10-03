---
type: epic
title: "The storybook map, first run and settings"
parent: initiative-math-learning-app
covers: [CAP-5, CAP-6, CAP-10, CAP-2]
after: []
assignee: ""
risk: medium
---

# The storybook map, first run and settings

## Description

The storybook map around epic 2's journey card, land unlocking and its animation, Land detail with replay, the first-run welcome and prerequisite check, and settings.

## Outcome

Maaheem sees her journey as a map she moves along, starts with her gaps found, and can make the app hers (spec CAP-5, CAP-6, CAP-10).

## Done when

1. The map shows five lands from `curriculum.json` as done, current or locked; introducing all of a land's concepts unlocks the next, with the stop-moves and unlock animation (CAP-5, AD-13).
2. Land detail lists each concept with its mastery label, and its replay entry starts epic 2's replay session (CAP-5).
3. A fresh install routes welcome → prerequisite check → map; every missed prerequisite concept appears in the first warm-up (CAP-6, AD-15).
4. The sound toggle (default on) takes effect immediately on epic 2's playback; renaming Lumi persists and appears in later messages (CAP-10).
5. Works when run from `start.sh`.

## Boundaries

Map, Land detail, onboarding and settings screens; switching fresh installs to `onboardingStage=welcome` and routing by stage; `curriculum.prerequisites` and the prerequisite concepts; the sort-match kind with walkthrough mode (CAP-2 part). Not session logic (epic 2).

## References

- parent — _bmad-output/initiative-math-learning-app/initiative-math-learning-app.md
- spec — _bmad-output/initiative-math-learning-app/spec-math-learning-app/spec-math-learning-app.md, section Capabilities
- constraint — _bmad-output/initiative-math-learning-app/spec-math-learning-app/spec-math-learning-app.md, section Constraints
- architecture — _bmad-output/initiative-math-learning-app/architecture-math-learning-app/architecture-math-learning-app.md
- design — _bmad-output/initiative-math-learning-app/ux-math-learning-app/DESIGN.md, _bmad-output/initiative-math-learning-app/ux-math-learning-app/EXPERIENCE.md
- curriculum map — _bmad-output/initiative-math-learning-app/brief-math-learning-app/addendum.md

## Notes

- Waits on epic-daily-session because: needs sessions, progress, streak, the home journey card, the replay-session API and sound playback.
- Waits on epic-help-and-lumi because: needs the Lumi illustration and companion-name use.
- Decision (2026-10-03): prerequisite items use only number-line and sort-match (e.g. match each equation to its solution).
