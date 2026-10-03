---
type: epic
title: "A full daily session that remembers"
parent: initiative-math-learning-app
covers: [CAP-1, CAP-4, CAP-7, CAP-8, CAP-11, CAP-2]
after: []
assignee: ""
risk: high
---

# A full daily session that remembers

## Description

The learning engine and its screens: a frozen daily session (warm-up, break, new idea, break, wrap-up), Leitner review with mastery labels, exact resume, streak and day number, the session-complete celebration, and a minimal home screen carrying today's journey card.

## Outcome

Maaheem can complete a whole daily session that remembers where she stopped and brings yesterday's concept back today (spec CAP-1, CAP-4, CAP-7, CAP-8).

## Done when

1. `POST /api/sessions/today` is idempotent and freezes the item list; parts run warm-up, break, new idea, break, wrap-up (CAP-1, AD-14).
2. Leitner moves at most one box per concept per day; labels read not started/new/practicing/easy; a concept introduced yesterday appears in today's warm-up (CAP-4, AD-8).
3. Only `reviewed: true` concepts are scheduled as new; when the next concept is not reviewed, the session has no new-idea part (CAP-11 part, AD-13).
4. Closing and reopening on any item or break the same day resumes there via the home journey card's "Continue today's journey" (CAP-7).
5. Completing a session shows the celebration and streak +1; a missed day resets the streak silently; a replay-session API exists whose attempts never move boxes or streak (CAP-8, AD-16).
6. Run from `start.sh`, the new migrations keep every session, attempt and progress row across a jar replacement (CAP-12).

## Boundaries

The `learning` module, session screens, a minimal home screen (journey card, streak) and the Session complete screen; the success sound and its playback, reading `settings.sound` live (CAP-2 part). Owns test-only lessons for session tests, which never ship. Not the help ladder (epic 3), not the map, the Land detail replay entry, or the sound toggle (epic 4).

## References

- parent — _bmad-output/initiative-math-learning-app/initiative-math-learning-app.md
- spec — _bmad-output/initiative-math-learning-app/spec-math-learning-app/spec-math-learning-app.md, section Capabilities
- constraint — _bmad-output/initiative-math-learning-app/spec-math-learning-app/spec-math-learning-app.md, section Constraints
- architecture — _bmad-output/initiative-math-learning-app/architecture-math-learning-app/architecture-math-learning-app.md
- design — _bmad-output/initiative-math-learning-app/ux-math-learning-app/DESIGN.md, _bmad-output/initiative-math-learning-app/ux-math-learning-app/EXPERIENCE.md

## Notes

- Waits on epic-foundation because: needs the scaffold, content pipeline, number-line kind and learner_profile.
- Handoff: epic 4's map hosts this epic's journey card and calls its replay-session API.
