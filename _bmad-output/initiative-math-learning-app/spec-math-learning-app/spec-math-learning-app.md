---
id: SPEC-math-learning-app
companions:
  - ../ux-math-learning-app/DESIGN.md
  - ../ux-math-learning-app/EXPERIENCE.md
  - ../architecture-math-learning-app/architecture-math-learning-app.md
  - ../brief-math-learning-app/addendum.md
sources:
  - ../brief-math-learning-app/brief-math-learning-app.md
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test and validate. Source documents listed in frontmatter are for traceability. Consult them only if you need narrative rationale or prose color this contract intentionally omits.

# Math Journey

## Why

This is a **pain to solve** and a **vision to realize**. Maaheem, an 8th grader, finds math hard. Her only practice is static homework with no immediate feedback, even though she learns best by interacting. Her parent wants her to learn Common Core Grade 8 math through an hour a day of drag-and-manipulate activities, framed as a light fantasy journey with Lumi, a butterfly companion. The aim is to make math feel achievable and to raise her school grades. It is a personal project for one learner now; friends' families may run their own copies later.

## Capabilities

- **CAP-1**
  - **intent:** Maaheem completes a daily one-hour session in three parts: a warm-up reviewing about 8 earlier concepts, 1–2 new concepts, and a wrap-up, with a break after each of the first two parts.
  - **success:** Starting today's session shows warm-up, break, new idea, break and wrap-up in that order. The item list for a day never changes once created, and a second start on the same day returns the same session.
- **CAP-2**
  - **intent:** She learns by dragging and manipulating math objects and gets instant feedback on each answer.
  - **success:** Every activity can be completed with a mouse, with touch and with only the keyboard. Feedback appears without a network wait: a green "correct" or an amber nudge, never red.
- **CAP-3**
  - **intent:** When she is stuck, help escalates the way a parent would explain: a nudge, then a hint, then a worked walkthrough by Lumi followed by a similar problem. She can ask for help at any time.
  - **success:**
    - The 1st wrong try shows a nudge, the 2nd shows a hint, and the 3rd opens the walkthrough and then the similar problem.
    - "I'm stuck" before any wrong try starts at the hint.
    - Closing and reopening mid-item preserves the try count.
- **CAP-4**
  - **intent:** Concepts she has learned come back for review on a schedule, so they stick. Each concept shows how well she knows it.
  - **success:**
    - A concept moves at most one mastery box per day.
    - Needing a walkthrough sends it back to box 1.
    - Labels read *not started*, *new*, *practicing* or *easy*, and a concept she learned yesterday appears in today's warm-up.
- **CAP-5**
  - **intent:** Her progress is a journey along a map through five lands (Numbers, Equations, Functions, Shapes, Data). Lands unlock in order, and she can see the concepts in each land and practice finished concepts again.
  - **success:**
    - Introducing all of a land's concepts unlocks the next land.
    - Land detail lists each concept with its mastery label.
    - Replaying a concept never changes its mastery or the streak.
- **CAP-6**
  - **intent:** On first run she meets Lumi and takes a short prerequisite check, so grade 6–7 gaps are reviewed early.
  - **success:** A fresh install routes through the welcome, then the prerequisite check, then the map. Every prerequisite concept she misses appears in her first warm-up.
- **CAP-7**
  - **intent:** She can stop mid-session and later continue exactly where she left off.
  - **success:** After she closes the app on any item or break and reopens it the same day, "Continue today's journey" lands on that same item or break.
- **CAP-8**
  - **intent:** She sees visible progress: a streak, a day count and a celebration when the session is complete.
  - **success:** Completing a session shows the celebration and increases the streak by 1. A missed day resets the streak to 0 with no message about it.
- **CAP-9**
  - **intent:** Lumi sends click-to-open messages during a session (hints, encouragement) and stays quiet on the map.
  - **success:** A message shows an unread dot until it is clicked, never blocks the activity, and never appears on the map screen.
- **CAP-10**
  - **intent:** She can turn sound off and rename her companion.
  - **success:** Sound is on by default, and the toggle takes effect immediately. A new companion name appears in every later message and persists across restarts.
- **CAP-11**
  - **intent:** A library of Grade 8 Common Core lessons, written by Claude, that can grow without code changes and never shows her a wrong answer key.
  - **success:**
    - Adding a valid concept file adds a lesson with no code change.
    - The app refuses to start if any lesson fails its schema or if a computed answer doesn't match the stored one, including walkthrough problems.
    - A lesson reaches her only after the parent marks it reviewed.
- **CAP-12**
  - **intent:** The parent runs the app on her laptop with one command, and her progress survives restarts and upgrades.
  - **success:** `start.sh` opens the app at a local address. After the jar is replaced with a newer version and restarted, every earlier session, attempt and mastery label is still present.

## Constraints

- One learner per installation: no sign-in and no accounts. Each family runs its own copy.
- A laptop with mouse or trackpad is the primary device. Touch must work, and every drag has a keyboard alternative.
- A new lesson must never require a code change. A new activity kind is a code change (architecture AD-5, AD-6).
- The learning flow never uses red error states, time limits, lives, or loss of progress.
- The app is a React SPA plus Spring Boot, shipped as a single jar with an H2 file database and run locally in v1. It must stay movable to free hosting through configuration (architecture AD-1, AD-11, AD-17).
- All learning logic and dates live in the backend. "Today" is the date in America/New_York (architecture AD-2, AD-9).
- Lessons are written in journey order: Numbers first, then Equations, Functions, Shapes and Data.
- Claude writes the code and the content, and the parent reviews. Contracts are generated and checked by the build rather than kept by convention (architecture AD-3, AD-5, AD-7).

## Non-goals

- A parent dashboard or reports.
- Accounts, multiple learners, sharing, or social and competitive features.
- Syncing with the school's pacing or homework.
- Grades other than 8th, apart from the prerequisite check.
- An AI tutor or chat.
- Backing up or syncing progress; losing it is acceptable.
- Dark mode.
- Hosted deployment in v1.

## Success signal

- Maaheem's school math grade rises above a baseline recorded before her first session, within the next one to two terms, and she completes most of her daily sessions. Lessons cover all five Grade 8 domains by the end of the school year.
- **Demonstration:** on a fresh install, go through onboarding and complete a full session. Start the next session, close the app midway, and reopen it to resume at the same item. The following day's warm-up includes the concept introduced the day before.

## Assumptions

- "Tablet" means the laptop's own touch screen. The app is reachable only on the laptop itself.
- The parent installs JDK 25 on her laptop, and `start.sh` checks for it.
- The parent records her current math grade as the baseline before the first session.
