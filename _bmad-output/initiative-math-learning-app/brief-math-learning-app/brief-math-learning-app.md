---
title: "Product Brief: Math Learning App"
status: final
created: 2026-10-03
updated: 2026-10-03
---

# Product Brief: Math Learning App

## Executive Summary

This is a web app built by a parent for their 8th-grade daughter. She finds math hard, and right now her only practice is school homework. The app teaches the Common Core Grade 8 curriculum through hands-on, drag-and-manipulate activities in a one-hour daily session that she and her parent have agreed on.

The idea is simple: she learns by doing. Instead of reading a worked example and copying it, she drags points onto a graph to see slope change, balances both sides of an equation by moving tiles, and rearranges squares to see why the Pythagorean theorem works. Each day introduces a small amount of new material and spends most of the time reinforcing what she has already seen, so the year adds up without becoming overwhelming.

It is a personal project first. If it works for her, she may share it with friends later. Success means better math grades at school.

## The Problem

- **Math feels hard to her.** For a student who already finds a subject difficult, each confusing homework session strengthens the belief that she is "not a math person."
- **Homework is the only practice, and it isn't interactive.** It is static, comes with no immediate feedback, and doesn't explain *why* a step works. If she gets something wrong, she may not find out until the work is graded. Her strongest learning mode, interaction, isn't being used at all.
- **Earlier gaps may get in the way.** Grade 8 math depends on negative numbers, fractions, ratios, and basic equations from grades 6–7. A gap there can look like trouble with 8th-grade topics. Whether she has such gaps is unknown; a prerequisite check will find out.

## The Solution

A daily one-hour session in the browser, built around visual manipulatives she can drag and move herself.

**A day's session**
- **Warm-up review (~15 min):** quick drag-and-drop items on about 8 concepts she learned earlier. This is spaced repetition, so earlier topics stick.
- **New idea (~30 min):** 1–2 new concepts, each introduced through a manipulative she controls, then guided practice with instant feedback and hints.
- **Wrap-up (~15 min):** mixed practice that combines today's idea with earlier ones, and ends on a problem she can solve.

That adds up to about 10 concepts each day: 1–2 new ones and 8 reviewed. This is how the "at least 10 concepts a day" requirement is met without overloading her.

**Core experiences**
- **Hands-on manipulatives** for each major topic: algebra tiles for equations, draggable lines and points for slope and functions, transformation tools for rotating, reflecting, and translating shapes, and fill-the-shape demonstrations for volume.
- **Instant feedback.** Every action responds immediately, and wrong answers get a hint rather than just an "incorrect."
- **A guided path through Grade 8,** ordered by prerequisites rather than by her school's calendar, with a short prerequisite check at the start to find grade 6–7 gaps.
- **Visible progress for her:** streaks, topics completed, and seeing earlier concepts become "easy." This helps sustain an hour a day over months.

## What Makes This Different

Products like Khan Academy, IXL, and DeltaMath already offer free, standards-aligned practice. This app has no technical advantage over them, and it doesn't need one. What it offers:

- **Built for one learner.** Pacing, tone, and difficulty fit her, not an average student.
- **Manipulation first, not quiz first.** Most practice tools are mainly multiple-choice and fill-in-the-blank. This one leads with dragging and building, which is how she learns.
- **A parent who can adjust it.** When something doesn't work for her, the app can change the following week.

Building rather than adopting an existing tool is a deliberate choice. She hasn't tried any online tool yet, so which interactions engage her most will only become clear once she is using the app, and the first weeks should be treated as learning.

## Who This Serves

**Primary: the daughter (8th grade).** She finds math hard and learns best through hands-on interaction. She has agreed to an hour a day. To her, success means math stops feeling confusing and her grades go up.

**Secondary: the parent and builder.** They check on progress by talking with her, not through a dashboard. They are also the app's developer, so it has to be buildable and maintainable by one person.

**Later: her friends.** They are not in scope now, but the app shouldn't make sharing it impossible.

## Success Criteria

- **Primary:** her school math grades improve over the next term or two. Her current grade is recorded as a baseline before she starts, so the comparison is real.
- **Leading signals** (from talking with her):
  - She completes most of her daily sessions without arguing.
  - She can explain a concept back in her own words.
  - Homework on topics she has covered in the app gets easier.
- **Coverage:** all five Grade 8 domains are covered by the end of the school year.

## Scope

**In for v1**
- Web app for one learner, usable on both laptop (mouse) and tablet (touch). Every drag interaction must work well with either input.
- Common Core Grade 8 content, starting with the three critical areas: linear equations and systems, functions, and geometry (Pythagorean theorem and volume). The full map is in the addendum.
- Daily one-hour session structure with review, new material, and wrap-up.
- Drag-and-manipulate activities, instant feedback, and hints.
- Basic progress tracking that she can see.

**Out for v1**
- Parent dashboard and reports.
- Accounts for multiple students, sharing, and social or competitive features.
- Syncing with her school's pacing or homework.
- Grades outside 8th, except a light prerequisite check.
- AI tutor or chat. Deferred to keep the build simple; worth revisiting.

## Content

Claude (AI) writes the learning content: explanations, manipulative activity designs, worked examples, problems, hints, and review items for every Grade 8 standard. The parent builds the app.

AI-written math content can contain errors: a wrong answer key, a misleading hint, or a problem that tests the wrong skill. For a student who already finds math hard, one confidently wrong answer does real damage. So content needs a correctness check before she sees it: worked solutions verified independently (for example, computed rather than only written), and a parent skim of each new unit.

## Open Questions

- **Content format:** how content is structured so the app can render it and new units can be added without code changes. This is a question for the PRD and architecture.
- **Motivation over months:** whether streaks and visible progress are enough once the novelty wears off. Watch this in the first month.

## Vision

If it works for her, it becomes a small, friendly Grade 8 practice space that she can share with friends. The same daily manipulatives-first structure could then be opened to a few more learners, and possibly extended to grade 9 algebra as she moves up.
