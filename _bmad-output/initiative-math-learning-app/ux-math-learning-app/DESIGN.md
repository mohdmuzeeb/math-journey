---
name: Math Journey
description: A storybook journey through Grade 8 math for one learner, guided by Lumi the butterfly. Bright, playful, and rounded; light mode only.
status: final
created: 2026-10-03
updated: 2026-10-03
sources:
  - ../brief-math-learning-app/brief-math-learning-app.md
colors:
  background: '#FFFFFF'
  surface: '#FDF4FF'
  surface-raised: '#FFFFFF'
  primary: '#A21CAF'
  primary-strong: '#86198F'
  on-primary: '#FFFFFF'
  accent: '#22D3EE'
  on-accent: '#1F1235'
  ink: '#1F1235'
  ink-muted: '#7A6A8A'
  border: '#F5D0FE'
  path-done: '#A21CAF'
  path-ahead: '#F5D0FE'
  success: '#16A34A'
  success-surface: '#DCFCE7'
  success-ink: '#166534'
  nudge-surface: '#FEF3C7'
  nudge-ink: '#92400E'
typography:
  display:
    fontFamily: 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif'
    fontSize: 28px
    fontWeight: 800
    lineHeight: 1.2
  title:
    fontFamily: 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif'
    fontSize: 20px
    fontWeight: 800
    lineHeight: 1.3
  body:
    fontFamily: 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif'
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif'
    fontSize: 13px
    fontWeight: 700
    lineHeight: 1.3
  math:
    fontFamily: 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif'
    fontSize: 22px
    fontWeight: 700
    lineHeight: 1.2
rounded:
  sm: 8px
  md: 14px
  lg: 24px
  full: 9999px
spacing:
  '1': 4px
  '2': 8px
  '3': 12px
  '4': 16px
  '5': 24px
  '6': 32px
  '7': 48px
  page-gutter: 28px
components:
  button-primary:
    background: '{colors.primary}'
    text: '{colors.on-primary}'
    rounded: '{rounded.full}'
    typography: '{typography.label}'
    minHeight: 44px
  button-secondary:
    background: '{colors.surface}'
    text: '{colors.primary}'
    rounded: '{rounded.full}'
    minHeight: 44px
  card:
    background: '{colors.surface-raised}'
    rounded: '{rounded.lg}'
    shadow: '0 12px 32px rgba(162,28,175,.18)'
    padding: '{spacing.5}'
  speech-bubble:
    background: '{colors.surface-raised}'
    rounded: '24px 24px 24px 6px'
    shadow: '0 4px 14px rgba(162,28,175,.14)'
    text: '{colors.ink}'
  unread-dot:
    background: '{colors.accent}'
    size: 12px
  map-stop-done:
    background: '{colors.path-done}'
    text: '{colors.on-primary}'
    size: 52px
  map-stop-current:
    background: '{colors.accent}'
    border: '5px solid {colors.background}'
    size: 68px
  map-stop-locked:
    background: '{colors.background}'
    border: '4px solid {colors.path-ahead}'
    size: 44px
  drag-tile:
    background: '{colors.surface-raised}'
    border: '2px solid {colors.border}'
    rounded: '{rounded.sm}'
    typography: '{typography.math}'
    minSize: 48px
  drag-tile-active:
    background: '{colors.primary}'
    text: '{colors.on-primary}'
    shadow: '0 8px 20px rgba(162,28,175,.30)'
  drop-zone:
    background: '{colors.surface}'
    border: '2px dashed {colors.primary}'
    rounded: '{rounded.md}'
  feedback-correct:
    background: '{colors.success-surface}'
    text: '{colors.success-ink}'
    rounded: '{rounded.md}'
  feedback-nudge:
    background: '{colors.nudge-surface}'
    text: '{colors.nudge-ink}'
    rounded: '{rounded.md}'
  progress-bar:
    track: '{colors.path-ahead}'
    fill: '{colors.accent}'
    height: 8px
    rounded: '{rounded.full}'
---

## Brand & Style

Math Journey is a storybook for one 8th grader, Maaheem, who finds math a little hard. Its job is to make math feel like an adventure she is on, not homework she owes. The home screen is a winding illustrated path through five lands, one per Grade 8 math area, with Lumi the butterfly travelling beside her.

The feel is **bright, playful, and rounded**, with an orchid pink-purple and a splash of aqua. It is light and airy like the websites she enjoys, never cluttered. The fantasy comes from the map, Lumi, and the soft storybook shapes, not from dense decoration. It should feel grown up enough for a 13-year-old and never babyish.

Maaheem picked both the palette (Orchid Pop) and the style (Storybook Map) from rendered options.

## Colors

- **Orchid (`{colors.primary}`)** is the journey color: the travelled path, finished stops, primary buttons, and an active drag tile. It is never used for error or "wrong" states.
- **Aqua (`{colors.accent}`)** marks *where she is now*: the current map stop, Lumi's unread dot, and the session progress fill. Aqua is never used for body text on white, because its contrast is too low. Text on aqua uses `{colors.on-accent}`.
- **Soft orchid surface (`{colors.surface}`)** is the map background and the fill of secondary panels.
- **Ink (`{colors.ink}`)** is body text; **muted ink (`{colors.ink-muted}`)** is secondary text and meets AA on white and on the surface color.
- **Success green** is shown only when an answer is correct.
- **Nudge amber** is used for "not quite yet" feedback. It is warm and never red, so a wrong answer reads as a nudge, not a failure.

Avoid: red anywhere in the learning flow, dark mode (not in scope for v1), and more than one accent color on a screen.

## Typography

The system font stack keeps the app fast and familiar. A rounded display font for headings could add storybook character later.

- `display`: greeting and celebration headlines ("Good evening, Maaheem!").
- `title`: card and activity titles.
- `body`: Lumi's messages, instructions, hints, and explanations. Kept at 16px or larger, because she reads every word on a hard subject.
- `label`: buttons, map stop names, chips, and the streak counter.
- `math`: numbers and expressions on drag tiles and in equations. Bold and large enough to grab.

## Layout & Spacing

The layout is built for a laptop at a study table, around 1280–1440px wide, with tablet as a secondary surface. Page gutter is `{spacing.page-gutter}`.

- **Map (home):** the illustrated path fills the screen. Today's journey card floats top right, and Lumi rests bottom left.
- **Session screens:** one activity at a time, centered, with a maximum content width of about 880px. Lumi's bubble is anchored bottom left, and the session progress bar runs along the top.
- Spacing uses the 4px-based scale. Activity areas get generous space (`{spacing.6}`–`{spacing.7}`) so drag targets never crowd each other.

## Elevation & Depth

The map is the ground layer. Floating cards (today's journey, the break screen, the celebration screen) sit above it with the soft orchid shadow from `{components.card.shadow}`. A tile being dragged lifts higher with a stronger shadow, so it clearly floats above the drop zones. Nothing else casts a shadow.

## Shapes

Everything is rounded, like a storybook. Cards use `{rounded.lg}`, buttons and pills use `{rounded.full}`, tiles use `{rounded.sm}`, and drop zones use `{rounded.md}`. Lumi's speech bubble has a small tail at the bottom left, pointing back to Lumi. Map stops are circles. Sharp corners appear nowhere.

## Components

- **Map stop:** three states: done (orchid, check mark), current (larger aqua stop with a white ring, Lumi perched nearby), and locked (white with a lock icon on the soft-orchid path).
- **Today's journey card:** day number, a one-line summary of today's new idea, the three parts with their times, and one primary "Let's go" button.
- **Lumi bubble:** Lumi's portrait plus a speech bubble. An aqua `{components.unread-dot}` pulses gently until she clicks it.
- **Drag tile and drop zone:** a tile at rest has a white fill and orchid border. When grabbed, it turns orchid with a lifted shadow. A drop zone shows a dashed orchid outline when a tile hovers over it, and the tile snaps into place on release.
- **Feedback strip:** green for correct and amber for a nudge, shown under the activity, never over it.
- **"I'm stuck" button:** a secondary button that is always visible on activity screens.
- **Session progress bar:** sits at the top, with an aqua fill and three segment marks for warm-up, new idea, and wrap-up.

**Illustration:** the lands and Lumi are soft, flat illustrations drawn in the Orchid Pop palette. The mocks use emoji placeholders.

→ Visual references: `mockups/color-themes.html` (theme 5, Orchid Pop), `mockups/directions-storybook-map.html` (style B), and `mockups/key-activity.html` (activity components). This spine wins on conflict with any mock.

## Do's and Don'ts

| Do | Don't |
|---|---|
| Use aqua only for "you are here" and "new" | Use red or ✗ marks for wrong answers |
| Keep one activity on screen at a time | Fill session screens with stats or menus |
| Make every drag target at least 48px | Place drop zones closer than `{spacing.4}` apart |
| Let the map illustration breathe | Add decorative clutter that competes with the path |
| Keep Lumi's messages short, bubble-sized | Use babyish cartoon styling |
