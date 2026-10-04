// Real-browser smoke test for the number line (headless Chromium, vitest.browser.config.ts): real
// layout from app.css and tokens.css, real mouse and keyboard input through Playwright, at a
// phone-width 360px card. jsdom has no layout, so these are the checks it cannot make.
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { commands, userEvent } from 'vitest/browser'
import { Activity } from '../Activity.tsx'
import type { ConceptItem } from '../api.ts'
import '../styles/tokens.css'
import '../styles/app.css'

type Point = { x: number; y: number }

declare module 'vitest/browser' {
  interface BrowserCommands {
    mouseDrag: (from: Point, path: Point[]) => Promise<void>
    pressKey: (key: string) => Promise<void>
    emulateReducedMotion: (reduce: boolean) => Promise<void>
  }
}

const CARD_WIDTH = 360

afterEach(async () => {
  cleanup()
  await commands.emulateReducedMotion(false)
})

function renderCard(item: ConceptItem) {
  render(
    // a phone-width card, as App lays it out
    <section className="card" style={{ width: `${CARD_WIDTH}px` }}>
      <Activity item={item} />
    </section>,
  )
  return screen.getByTestId('number-line-point')
}

const centre = (el: Element): Point => {
  const r = el.getBoundingClientRect()
  return { x: r.left + r.width / 2, y: r.top + r.height / 2 }
}

/** A tick's x position: zones tile the line, so the end zones are half bands with the tick on their outer edge. */
function tickX(index: number, last: number): number {
  const r = screen.getByTestId(`number-line-tick-${index}`).getBoundingClientRect()
  if (index === 0) return r.left
  if (index === last) return r.right
  return r.left + r.width / 2
}

const checkButton = () => screen.getByRole('button', { name: 'Check' }) as HTMLButtonElement
const strip = () => document.querySelector('.feedback')

/** Waits for the browser to lay out and paint the latest update. */
const nextFrame = () => new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))

const quarters: ConceptItem = {
  id: 'smoke#three-quarters',
  kind: 'number-line',
  payload: { prompt: 'Drag the point to 3/4', min: 0, max: 2, step: 0.25, target: '3/4' },
  answer: 0.75,
}

describe('number line in a real browser', () => {
  it('starts unplaced beside the line, inside the card, and answers 3/4 with the keyboard alone', async () => {
    const point = renderCard(quarters)
    await nextFrame()
    const card = document.querySelector('.card')!.getBoundingClientRect()
    const parked = point.getBoundingClientRect()
    expect(point.getAttribute('aria-label')).toBe('Number line point, not placed yet')
    expect(parked.width).toBeGreaterThanOrEqual(48)
    expect(parked.left).toBeGreaterThanOrEqual(card.left)
    expect(parked.right).toBeLessThan(screen.getByTestId('number-line-tick-0').getBoundingClientRect().left)
    expect(Math.abs(centre(point).y - centre(document.querySelector('.number-line__axis')!).y)).toBeLessThan(1)
    expect(checkButton().disabled).toBe(true)
    // the glide the reduced-motion test turns off
    expect(getComputedStyle(point).transitionDuration).not.toBe('0s')

    point.focus()
    await commands.pressKey('Space')
    for (let i = 0; i < 3; i++) await commands.pressKey('ArrowRight')
    await commands.pressKey('Enter')
    await expect.poll(() => point.getAttribute('aria-label')).toBe('Number line point at 0.75')

    await userEvent.click(checkButton())
    await expect.poll(() => strip()?.textContent).toBe('You did it!')
  })

  it("drops a mouse drag on the tick under the point's centre, not under the pointer", async () => {
    // 0 to 2 in tenths: 20 steps across a 360px card, so ticks are far closer than the 48px point
    const point = renderCard({
      id: 'smoke#half',
      kind: 'number-line',
      payload: { prompt: 'Drag the point to 1/2', min: 0, max: 2, step: 0.1, target: '1/2' },
      answer: 0.5,
    })
    await nextFrame()
    const last = 20
    const spacing = tickX(1, last) - tickX(0, last)
    expect(spacing).toBeLessThan(24)

    // Grab the point 18px right of its centre and release with its centre on tick 5 (0.5): the
    // pointer is then over a zone further right.
    const grabOffset = 18
    const start = centre(point)
    const lineY = start.y
    const target = { x: tickX(5, last) + grabOffset, y: lineY }
    const underPointer = document
      .elementsFromPoint(target.x, target.y)
      .find((el) => el.getAttribute('data-testid')?.startsWith('number-line-tick-'))
    expect(underPointer?.getAttribute('data-testid')).not.toBe('number-line-tick-5')

    await commands.mouseDrag({ x: start.x + grabOffset, y: lineY }, [target])
    await expect.poll(() => point.getAttribute('aria-label')).toBe('Number line point at 0.5')
    await expect.poll(() => checkButton().disabled).toBe(false)
    await userEvent.click(checkButton())
    await expect.poll(() => strip()?.textContent).toBe('You did it!')
  })

  it('returns the point instantly on a cancel under prefers-reduced-motion: reduce', async () => {
    await commands.emulateReducedMotion(true)
    const point = renderCard(quarters)
    await nextFrame()
    expect(getComputedStyle(point).transitionDuration).toBe('0s')

    // Place it on 0.25, then pick it up, move it two ticks and cancel.
    point.focus()
    await commands.pressKey('Space')
    await commands.pressKey('ArrowRight')
    await commands.pressKey('Enter')
    await expect.poll(() => point.getAttribute('aria-label')).toBe('Number line point at 0.25')
    await nextFrame()
    const home = tickX(1, 8)
    expect(Math.abs(centre(point).x - home)).toBeLessThan(1)

    await commands.pressKey('Space')
    await commands.pressKey('ArrowRight')
    await commands.pressKey('ArrowRight')
    await expect.poll(() => Math.abs(centre(point).x - tickX(3, 8))).toBeLessThan(1)

    await commands.pressKey('Escape')
    // One frame after the cancel it is already home: no glide.
    await expect.poll(() => point.style.transform).toMatch(/^translate3d\(0px, 0px, 0(px)?\)$/)
    await nextFrame()
    expect(Math.abs(centre(point).x - home)).toBeLessThan(1)
    expect(point.getAttribute('aria-label')).toBe('Number line point at 0.25')
    expect(checkButton().disabled).toBe(false)
  })

  it('parks an unplaced point again instantly on a cancel under prefers-reduced-motion: reduce', async () => {
    await commands.emulateReducedMotion(true)
    const point = renderCard(quarters)
    await nextFrame()
    const parked = centre(point)
    expect(parked.x).toBeLessThan(tickX(0, 8))

    point.focus()
    await commands.pressKey('Space')
    await commands.pressKey('ArrowRight')
    await commands.pressKey('ArrowRight')
    await expect.poll(() => Math.abs(centre(point).x - tickX(2, 8))).toBeLessThan(1)

    await commands.pressKey('Escape')
    // One frame after the cancel it is already back at the parking spot: no glide.
    await expect.poll(() => point.style.transform).toMatch(/^translate3d\(0px, 0px, 0(px)?\)$/)
    await nextFrame()
    expect(Math.abs(centre(point).x - parked.x)).toBeLessThan(1)
    expect(point.getAttribute('aria-label')).toBe('Number line point, not placed yet')
    expect(checkButton().disabled).toBe(true)
  })
})
