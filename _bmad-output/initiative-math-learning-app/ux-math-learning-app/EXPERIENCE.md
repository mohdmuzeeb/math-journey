---
name: Math Journey
status: final
created: 2026-10-03
updated: 2026-10-03
sources:
  - ../brief-math-learning-app/brief-math-learning-app.md
---

# Math Journey: Experience Spine

## Foundation

A web app used mainly on a **laptop** (mouse or trackpad) at Maaheem's study table, with **tablet touch** as a secondary input that every interaction must also support. There is no UI system; `DESIGN.md` is the visual identity reference. The app is light mode only, has one learner and no sign-in: it opens straight to her map. Sessions happen at about 6pm EST, first in her 6–9pm study block, so she arrives fresh.

Product content (curriculum, session structure, and success criteria) lives in the brief and is not repeated here.

## Information Architecture

| Surface | Reached from | Purpose |
|---|---|---|
| **Welcome** (first run only) | First app open | Meet Lumi; Lumi explains the journey in three short bubbles |
| **Prerequisite check** (first run only) | Welcome | A short drag-and-drop check of grade 6–7 skills. Gaps found here are added to early warm-ups. |
| **Map** (home) | App open; end of session | The winding path through five lands: Numbers, Equations, Functions, Shapes, and Data. Shows today's journey card, her streak, and her current stop. |
| **Land detail** | Clicking a finished or current land on the map | The concepts in that land, each marked *new*, *practicing* or *easy*. This is where she sees earlier concepts become easy. |
| **Warm-up** | "Let's go" on today's journey card | About 8 quick review items on earlier concepts, about 15 minutes |
| **Break** | End of the warm-up; end of the new idea | A short pause screen; she continues when ready |
| **New idea** | After the first break | 1–2 new concepts: a hands-on discovery activity, then guided practice. About 30 minutes. |
| **Wrap-up** | After the second break | Mixed practice combining today's idea with earlier ones, ending on a problem she can solve. About 15 minutes. |
| **Stuck walkthrough** | 3rd wrong try, or "I'm stuck" then "Show me" | Lumi walks through a worked example step by step, then gives a similar problem |
| **Session complete** | End of the wrap-up | Celebration: what she learned today, the stop moving forward on the map, her streak growing |
| **Settings** | Gear icon on the map | Sound on or off, rename Lumi |

Map navigation is flat. During a session there is only forward progress and a "Back to map" exit, which saves her place. Overlays (Lumi bubble, stuck walkthrough) never stack more than one deep.

→ Composition references: `mockups/directions-storybook-map.html` (map, style B), `mockups/key-activity.html` (activity screen and stuck walkthrough) and `mockups/color-themes.html` (palette, theme 5). Other surfaces are built from this spine alone. The spine wins on conflict with any mock.

## Voice and Tone

Lumi is a **friendly coach**: warm, encouraging, and brief. Lumi sometimes speaks to Maaheem by name. Brand posture lives in `DESIGN.md`.

| Do | Don't |
|---|---|
| "Not quite. Check the right side again." | "Wrong!" / "Incorrect." |
| "Here's a hint: what's on both sides?" | "Hint 1 of 3" |
| "Let's walk through one together." | "You failed 3 times. Showing solution." |
| "You did it! x = 4. Both sides balance." | "Correct. +10 points." |
| "Nice, three in a row! 🦋" | Constant praise after every single tap |
| "Welcome back, Maaheem. Let's pick up where you left off." | "Session restored." |
| Short sentences that fit in one bubble | Paragraphs, jargon before the idea is shown |

Math words such as *slope* and *function* are introduced **after** she has seen the idea with her hands, never before.

## Component Patterns

Behavioral. Visual specs live in `DESIGN.md.Components`.

| Component | Use | Behavioral rules |
|---|---|---|
| Today's journey card | Map | One per day. It shows the three parts and has one button, "Let's go." After the session it changes to "Done for today ✓," and she can still replay. |
| Map stop | Map | Clicking a done or current land opens Land detail. Clicking a locked stop: Lumi says "We'll get there soon!" and nothing opens. |
| Lumi bubble | All session screens | Lumi appears with an aqua unread dot and a gentle pulse; she clicks to open the message. Unopened bubbles collapse after about 20 seconds, leaving the dot, and never block the activity. Lumi is **quiet on the map**: present, but with no messages. |
| Drag tile | Activities | Grab, move, and drop. On a valid drop zone the tile snaps in; on release elsewhere it glides back. It never disappears. |
| Check button | Activities | Disabled until the activity has an answer. Feedback appears instantly, with no loading. |
| "I'm stuck" button | Activities | Always visible. Tapping it starts the stuck ladder at the hint step, even with no wrong tries yet. |
| Feedback strip | Activities | Correct: green, plus a sound if sound is on. Wrong: amber nudge. It stays until her next action. |
| Session progress bar | Session screens | Three segments. The fill advances per item and never moves backward. |
| Break screen | Between parts | Shows how far she has come ("Warm-up done ✓"), with a "Continue" button. There is no timer and no pressure. |

## State Patterns

| State | Surface | Treatment |
|---|---|---|
| First run | Welcome | Lumi introduces themself, then the prerequisite check. The map is revealed afterwards, with the first stop glowing. |
| Daily open, session not started | Map | Today's journey card is waiting, the current stop is highlighted, and Lumi rests quietly. |
| Resume mid-session | Map → session | The card reads "Continue today's journey," and she resumes on the exact item where she left off. |
| Session done today | Map | The card shows "Done for today ✓ See you tomorrow." She can replay any finished concept from Land detail. |
| Missed day(s) | Map | The streak resets quietly. Lumi says nothing about the missed day; the warm-up simply covers older concepts. |
| Wrong answer, tries 1 and 2 | Activity | Try 1: amber nudge. Try 2: a Lumi bubble with a hint. The activity stays as she left it. |
| Wrong answer, try 3 | Activity → stuck walkthrough | Lumi: "Let's walk through one together." Then the worked example, then a similar problem, and the concept is added to future warm-ups. |
| Correct answer | Activity | Green strip, success sound, and Lumi flutters. Every third correct answer in a row gets a Lumi "nice streak" bubble. |
| Land complete | Session complete → map | The path fills to the next land, which unlocks with a short animation. |
| Sound off | All | The visual feedback is identical; only the sound is skipped. |

## Interaction Primitives

- **Drag and drop is the core action.** It works with a mouse (press, drag, release), touch (touch, drag, lift), and keyboard (see below). Drop zones highlight while a tile hovers over them.
- **Click to open** Lumi's messages. She enjoys this, so it is a reward in itself, not friction.
- **Click a map stop** to open Land detail.
- **Snap and glide back:** a misplaced tile returns to where it came from. Nothing is lost or undone without her action.
- **Not allowed:** time limits, countdowns, lives or hearts, losing progress, red error states, and pop-ups that interrupt a drag.

## Accessibility Floor

Behavioral. Visual contrast lives in `DESIGN.md`. No specific needs were reported, so this is the standard floor.

- **Keyboard alternative to drag:** Tab to a tile, press Enter or Space to pick it up, use the arrow keys or Tab to choose a drop zone, and press Enter to drop. Esc cancels.
- Every drag tile, drop zone, and map stop has a label for screen readers, such as "Tile 2x" or "Drop zone: left side of the scale."
- Click and touch targets are at least 44px; drag tiles are at least 48px.
- Reduced motion: Lumi's flutter, the map path animation, and the bubble pulse become instant state changes.
- Focus order follows reading order. After feedback, focus moves to the feedback strip.

## Story Layer

The story is a **light layer**: the map and Lumi, not a plot.

- Five lands appear in order on one winding path. Each land is one Grade 8 area, and each stop is a concept.
- Lumi is a butterfly companion who travels with her. Maaheem can rename Lumi in Settings.
- Story shows up in three places only: the map's progress, Lumi's tone, and the moment a new land unlocks.
- The journey is strictly linear: each land unlocks only after the current one is finished, with no side paths in v1.

## Key Flows

### Flow 1: First evening (Maaheem, 6pm, study table, laptop)

1. Maaheem opens the app for the first time.
2. Lumi flutters in: "Hi Maaheem! I'm Lumi. We're going on a journey through 8th-grade math together."
3. She clicks through three short bubbles about the map, the daily hour, and the "I'm stuck" button.
4. She takes the prerequisite check: about 10 drag-and-drop items on fractions, negative numbers, and simple equations.
5. Lumi: "Thanks! Now I know where to start."
6. **Climax:** the map is revealed with the first stop in Numbers glowing aqua, and Lumi lands beside it. Her journey has a starting point.

### Flow 2: A normal day (Maaheem, Tuesday, 6pm)

1. She opens the app to the map. Her current stop is in Equations, the streak shows 🔥 4, and Lumi rests quietly.
2. Today's journey card: "Day 12: Two-step equations. Warm-up, new idea, wrap-up." She clicks "Let's go."
3. **Warm-up (about 15 minutes):** 8 quick drag items from earlier stops. On the third correct answer in a row, Lumi's bubble pops up with "Nice, three in a row! 🦋"
4. **Break:** "Warm-up done ✓". She stretches, then clicks "Continue."
5. **New idea (about 30 minutes):** she drags tiles off both sides of a balance scale until x is alone, and sees the rule before it is named. Then she does guided practice.
6. **Break**, then **wrap-up (about 15 minutes):** mixed problems, ending on one she can solve.
7. **Climax:** the Session complete screen. "You learned two-step equations today!" Her stop moves forward on the map, and the streak becomes 🔥 5.
8. She returns to the map, which shows "Done for today ✓." Then on to homework.

### Flow 3: Stuck (Maaheem, Wednesday, new idea part)

→ Illustrated in `mockups/key-activity.html`.

1. She drags tiles for 3x − 5 = 10 and clicks Check. Amber nudge: "Not quite. What did you do to the left side?"
2. Second try, still wrong. Lumi's bubble pulses, and she clicks it: "Hint: get rid of the −5 first. What undoes subtracting 5?"
3. Third try, wrong. Lumi: "Let's walk through one together."
4. In the stuck walkthrough, Lumi solves 2x − 4 = 6 step by step, and she clicks "Next" through each move, watching the tiles animate.
5. Lumi: "Your turn, a similar one." She solves 4x − 2 = 10.
6. **Climax:** green strip. "You did it!" The concept is quietly added to her future warm-ups.

Alternative: on her first look at a problem she clicks **"I'm stuck"**. Lumi goes straight to the hint (step 2), then offers "Show me an example" (step 4).

### Flow 4: Interrupted (Maaheem, Thursday, dinner is called mid-session)

1. She closes the laptop in the middle of the new idea.
2. Later she reopens the app. The map card reads "Continue today's journey."
3. **Climax:** she clicks it and lands on the exact problem she left, with Lumi saying "Welcome back, Maaheem!" Nothing was lost.
