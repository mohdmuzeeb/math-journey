---
type: initiative
title: "Maaheem learns Grade 8 math an hour a day"
parent: none
covers: [CAP-1, CAP-2, CAP-3, CAP-4, CAP-5, CAP-6, CAP-7, CAP-8, CAP-9, CAP-10, CAP-11, CAP-12]
after: []
assignee: ""
risk: medium
---

# Maaheem learns Grade 8 math an hour a day

## Description

Math Journey: a daily one-hour storybook journey through Common Core Grade 8 math, with drag-and-manipulate activities and Lumi the butterfly, running on Maaheem's laptop. The spec owns the capabilities, constraints, and non-goals; this initiative delivers all of them.

## Outcome

Maaheem's school math grade rises above her recorded baseline within one to two terms while she completes most daily sessions — the spec's success signal.

## Done when

1. Maaheem completes daily sessions on her laptop via `start.sh` across all five lands.
2. Every CAP-1–CAP-12 success check passes on the running jar.
3. The spec's demonstration passes: fresh install → onboarding → session → close mid-way → resume → next-day warm-up includes yesterday's concept.
4. Lessons cover all five Grade 8 domains, each verified and marked reviewed by the parent.

## Boundaries

One repo, one deployable (AD-1); production is the jar run via `start.sh` on her laptop (AD-17). Non-goals: see the spec. No external units, so no touch points. Tracer path: `start.sh` → page → drag one number onto a number line → "correct".

## References

- spec — _bmad-output/initiative-math-learning-app/spec-math-learning-app/spec-math-learning-app.md, section Capabilities
- constraint — same spec, section Constraints
- architecture — _bmad-output/initiative-math-learning-app/architecture-math-learning-app/architecture-math-learning-app.md (AD-1–AD-17: every shared contract between epics)
- design — _bmad-output/initiative-math-learning-app/ux-math-learning-app/DESIGN.md, EXPERIENCE.md, mockups/
- curriculum map — _bmad-output/initiative-math-learning-app/brief-math-learning-app/addendum.md

## Notes

- Decision (2026-10-03): Maaheem starts using the app after epic 5 (Numbers).
- Decision (2026-10-03): epics 2, 3, 4 stay separate; one epic per land.
- Decision (2026-10-03): lessons are authored in journey order, Numbers first.
- Decision (2026-10-03): shared setup owners — learner_profile row with settings defaults (sound on), interim `onboardingStage=journey`, design tokens, `curriculum.json` skeleton with five empty lands → epic 1; Lumi illustration → epic 3; sort-match kind and `curriculum.prerequisites` → epic 4; each land epic appends its own concept IDs.
- Decision (2026-10-03): prerequisite check uses only number-line and sort-match kinds (no early balance-scale).
- Decision (2026-10-03): epic 2 builds a minimal home screen (journey card, streak, celebration) that epic 4's map later hosts.
- Decision (2026-10-03): epic 2 owns test-only lessons for session tests; they never ship to Maaheem.
- Decision (2026-10-03): CI deferred; local `./mvnw verify` is the gate (architecture, Deferred).
