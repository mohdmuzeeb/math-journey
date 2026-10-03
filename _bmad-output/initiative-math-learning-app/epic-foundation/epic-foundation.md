---
type: epic
title: "App runs and the lesson pipeline is trustworthy"
parent: initiative-math-learning-app
covers: [CAP-12, CAP-11, CAP-2]
after: []
assignee: ""
risk: medium
---

# App runs and the lesson pipeline is trustworthy

## Description

The platform baseline: one Spring Boot jar serving React, started by `start.sh`, with the content pipeline that refuses invalid or unverified lessons, and the first activity kind (number-line) working end to end. Everything later builds on this.

## Outcome

The parent starts the app with one command, and no lesson Claude writes can reach Maaheem unless it is valid and its answers are computed-verified (spec CAP-11, CAP-12).

## Requirements

- E1 (CAP-12): `start.sh` checks Java 25 then runs the single jar bound to 127.0.0.1; data in `~/.mathjourney` survives replacing the jar.
- E2 (CAP-11): startup refuses a concept file failing `concept.schema.json` (including AD-12 minimum items per role) or its kind schema, or a `curriculum.json` / concept-file mismatch; `reviewed` is loaded.
- E3 (CAP-11): every answer-carrying node, walkthrough `similar` included, is computed-verified at startup; shared fixtures run in Java and TypeScript.
- E4 (CAP-2): a number-line activity accepts an answer by mouse, touch and keyboard alone, with instant green/amber feedback, in Orchid Pop styling, per EXPERIENCE.md Component Patterns and Accessibility Floor.
- E5 (spec Constraints; AD-1, AD-7): single jar serving SPA and `/api`; API types generated from OpenAPI, content types from schemas; ProblemDetail errors; unknown `/api` → 404, other paths → SPA.

## Done when

1. `start.sh` checks for Java 25, then runs the single jar at 127.0.0.1:8080; the database is created by Flyway in `~/.mathjourney`, and data survives replacing the jar (CAP-12).
2. The app refuses to start when a concept file fails `concept.schema.json` or its kind schema, when `curriculum.json` and concept files disagree, or when a computed answer does not match a stored one, walkthrough similar problems included (CAP-11, AD-3, AD-5, AD-12, AD-13).
3. A demo number-line concept renders and accepts a drag answer by mouse, touch and keyboard alone, with instant green or amber feedback (CAP-2 part: feedback strip, drag/keyboard primitive, number-line kind).
4. `./mvnw verify` builds backend and frontend, generates API types from OpenAPI and content types from schemas, and runs the shared number-line fixtures in Java and TypeScript (AD-3, AD-7).
5. Unknown `/api` paths return a 404 ProblemDetail; every other path serves the SPA (AD-1).

## Boundaries

Platform baseline, the `content` module, the first activity kind, and Orchid Pop design tokens. Owns: `curriculum.json` skeleton with five empty lands; `learner_profile` single row with settings defaults (sound on, companion name Lumi) and an interim `onboardingStage=journey`; loading the `reviewed` flag. Not session logic (epic 2), not the review-gate scheduling rule (epic 2).

## References

- parent — _bmad-output/initiative-math-learning-app/initiative-math-learning-app.md
- spec — _bmad-output/initiative-math-learning-app/spec-math-learning-app/spec-math-learning-app.md, section Capabilities
- constraint — _bmad-output/initiative-math-learning-app/spec-math-learning-app/spec-math-learning-app.md, section Constraints
- architecture — _bmad-output/initiative-math-learning-app/architecture-math-learning-app/architecture-math-learning-app.md
- design — _bmad-output/initiative-math-learning-app/ux-math-learning-app/DESIGN.md, _bmad-output/initiative-math-learning-app/ux-math-learning-app/EXPERIENCE.md

## Notes

- Decision (2026-10-03): CI deferred; local `./mvnw verify` is the gate.
- Decision (2026-10-03): the demo number-line concept is a fixture, not a real 8.NS lesson; real Numbers lessons are epic 5.
- Tracer bullet: entry 1 — `start.sh --demo` → page → drag a point on a number line → "correct".
- Decision (2026-10-03): the demo concept and its own demo curriculum live in `content/demo/`, loaded only with the `demo` profile (`start.sh --demo`) and in tests; the real `curriculum.json` lands stay empty. Demo content is validated and verified like real content and meets AD-12 minimums.
- Decision (2026-10-03): entries 2→3→4→5→6 are chained because they share `pom.xml` and `application.yml` (collision rule), not because each needs the previous feature.
- Decision (2026-10-03): learner API shape `{ onboardingStage, settings: { sound: boolean, companionName: string } }`; epic 2 reads `settings.sound`, epics 3–4 read and change `companionName`.
- Assumption: the JSON-Schema validator (Java) and JSON-Schema→TypeScript generator are chosen current at build; the architecture Stack names neither.
