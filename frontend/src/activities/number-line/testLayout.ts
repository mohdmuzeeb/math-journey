// Test-only layout for the number line. jsdom has no layout, so this gives the drop zones and the
// point real rects: the track is TRACK_WIDTH px wide at the page's left edge, and the axis runs at LINE_Y.
import { act, fireEvent, screen } from '@testing-library/react'

export const TRACK_WIDTH = 800
export const TRACK_HEIGHT = 120
export const LINE_Y = TRACK_HEIGHT / 2
const POINT_SIZE = 48
/**
 * The unplaced point's centre, left of the track's start: tokens.css --offset-number-line-park,
 * half the 48px tile plus --space-2 (8px), so the whole tile sits outside every zone.
 */
export const PARK_X = -(POINT_SIZE / 2 + 8)

function rect(left: number, top: number, width: number, height: number): DOMRect {
  return {
    left,
    top,
    width,
    height,
    right: left + width,
    bottom: top + height,
    x: left,
    y: top,
    toJSON: () => ({}),
  } as DOMRect
}

const percentOf = (style: string) => (Number.parseFloat(style) / 100) * TRACK_WIDTH

/** Lays out the rendered number line's zones and point; call after render and before a drag. */
export function layoutNumberLine(): HTMLElement {
  for (const zone of screen.getAllByTestId(/^number-line-tick-/)) {
    zone.getBoundingClientRect = () =>
      rect(percentOf(zone.style.left), 0, percentOf(zone.style.width), TRACK_HEIGHT)
  }
  const point = screen.getByTestId('number-line-point')
  // dnd-kit measures the point ignoring its CSS transform, so this is the untransformed box: its
  // left edge at the tick (`left: <tick>%`), pulled back by app.css's negative margins
  // (--offset-drag-centre, half the 48px tile) so it is centred on the tick and the axis. Unplaced,
  // it has no inline `left` and app.css parks its centre at PARK_X.
  point.getBoundingClientRect = () => {
    // Centring with a percentage transform would put the visible point somewhere dnd-kit does not see.
    if (point.style.transform.includes('%')) throw new Error(`point is centred with a transform: ${point.style.transform}`)
    const unplaced = point.classList.contains('number-line__point--unplaced')
    if (unplaced === (point.style.left !== '')) throw new Error(`unplaced point with inline left: ${point.style.left}`)
    const centreX = unplaced ? PARK_X : percentOf(point.style.left)
    return rect(centreX - POINT_SIZE / 2, LINE_Y - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE)
  }
  return point
}

/** The x position of a tick on the laid-out line. */
export const tickClientX = (index: number, lastIndex: number) => (index / lastIndex) * TRACK_WIDTH

/**
 * Presses on the point and drags the pointer to (clientX, clientY), leaving it held. The first move
 * only passes the activation distance (dnd-kit starts the drag on it); the second moves the point.
 * The press is `grabOffset` px right of the point's centre, so the point's centre ends up at
 * clientX - grabOffset.
 */
export function pointerGrab(point: HTMLElement, clientX: number, clientY = LINE_Y, grabOffset = 0) {
  const from = point.getBoundingClientRect()
  const startX = from.left + from.width / 2 + grabOffset
  fireEvent.pointerDown(point, { pointerId: 1, isPrimary: true, button: 0, clientX: startX, clientY: LINE_Y })
  fireEvent.pointerMove(point, { pointerId: 1, isPrimary: true, clientX: startX + 10, clientY: LINE_Y })
  fireEvent.pointerMove(point, { pointerId: 1, isPrimary: true, clientX, clientY })
}

/** Lets pending timers run, e.g. dnd-kit attaching keyboard listeners or lifting its post-drag click guard. */
async function flushTimers(ms = 0) {
  await act(async () => {
    await new Promise((resolve) => setTimeout(resolve, ms))
  })
}

/** dnd-kit removes its post-drag click guard 50ms after a pointer drag ends. */
const CLICK_GUARD_MS = 60

/**
 * Releases a held point at (clientX, clientY). dnd-kit swallows clicks for a moment after a pointer
 * drag ends, so this waits that out; a click on Check straight after then reaches the button.
 */
export async function pointerRelease(point: HTMLElement, clientX: number, clientY = LINE_Y) {
  fireEvent.pointerUp(point, { pointerId: 1, isPrimary: true, clientX, clientY })
  await flushTimers(CLICK_GUARD_MS)
}

/** Presses on the point (`grabOffset` px right of its centre), drags it to (clientX, clientY) and releases it there. */
export async function pointerDrag(point: HTMLElement, clientX: number, clientY = LINE_Y, grabOffset = 0) {
  pointerGrab(point, clientX, clientY, grabOffset)
  await pointerRelease(point, clientX, clientY)
}

/** Presses a key on the focused point, then lets dnd-kit attach its keyboard listeners. */
export async function pressKey(code: string) {
  const target = document.activeElement ?? document.body
  fireEvent.keyDown(target, { code, key: code })
  await flushTimers()
}

/** The text dnd-kit's live region last announced. */
export function announcement(): string {
  return document.querySelector('[id^="DndLiveRegion"]')?.textContent ?? ''
}
