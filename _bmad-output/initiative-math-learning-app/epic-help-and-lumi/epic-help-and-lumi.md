---
type: epic
title: "Help when stuck, Lumi beside her"
parent: initiative-math-learning-app
covers: [CAP-3, CAP-9]
after: []
assignee: ""
risk: medium
---

# Help when stuck, Lumi beside her

## Description

The stuck ladder (nudge, hint, walkthrough, similar problem), the always-available "I'm stuck", and Lumi's click-to-open messages during sessions, plus the Lumi illustration.

## Outcome

When Maaheem is stuck she gets the parent-style escalation from Lumi instead of giving up (spec CAP-3, CAP-9).

## Done when

1. 1st wrong try shows a nudge, 2nd a hint, 3rd opens the walkthrough then the similar problem; "I'm stuck" before any wrong try starts at the hint (CAP-3, AD-4).
2. Try count and assist stage survive closing and reopening mid-item (CAP-3).
3. Lumi messages show an unread dot until clicked, never block the activity, never appear on the home or map screen, and use the companion name from the learner profile (CAP-9).
4. The walkthrough view renders steps through the activity kind's renderer in a walkthrough mode; number-line supports it, and the contract for kinds added later is written down (AD-6).
5. Works when run from `start.sh`.

## Boundaries

Assist stage API and UI, walkthrough view, Lumi messaging and the Lumi illustration asset. Not the map (epic 4).

## References

- parent — _bmad-output/initiative-math-learning-app/initiative-math-learning-app.md
- spec — _bmad-output/initiative-math-learning-app/spec-math-learning-app/spec-math-learning-app.md, section Capabilities
- constraint — _bmad-output/initiative-math-learning-app/spec-math-learning-app/spec-math-learning-app.md, section Constraints
- architecture — _bmad-output/initiative-math-learning-app/architecture-math-learning-app/architecture-math-learning-app.md
- design — _bmad-output/initiative-math-learning-app/ux-math-learning-app/DESIGN.md, _bmad-output/initiative-math-learning-app/ux-math-learning-app/EXPERIENCE.md

## Notes

- Waits on epic-daily-session because: needs item state, the attempts API and session screens.
- Handoff: every later kind (epics 4, 6–9) implements this epic's walkthrough mode.
