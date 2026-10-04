import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App.tsx'
import {
  layoutNumberLine,
  pointerDrag,
  pointerGrab,
  pointerRelease,
  pressKey,
  tickClientX,
} from './activities/number-line/testLayout.ts'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

const concept = {
  id: 'demo-number-line',
  title: 'Demo: fractions on a number line',
  items: [
    {
      id: 'demo-number-line#three-quarters',
      kind: 'number-line',
      payload: { prompt: 'Drag the point to 3/4', min: 0, max: 2, step: 0.25, target: '3/4' },
      answer: 0.75,
    },
  ],
}

// The line runs 0 to 2 in quarters: tick 3 is 0.75 and tick 4 is 1.0.
const LAST = 8

/** The feedback strip (dnd-kit's own live region is also a status, so pick the strip by its class). */
const strip = () => screen.queryAllByRole('status').find((el) => el.classList.contains('feedback')) ?? null

describe('App', () => {
  it('shows a friendly message when there is no lesson (404)', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(null, { status: 404 })))
    render(<App />)
    expect(await screen.findByText('No lesson yet')).toBeTruthy()
  })

  it('records a drop, judges it on Check and clears the verdict on the next grab', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Response.json(concept)))
    render(<App />)
    expect(await screen.findByText('Drag the point to 3/4')).toBeTruthy()
    expect(screen.getByRole('heading', { name: 'Demo: fractions on a number line' })).toBeTruthy()

    const check = screen.getByRole('button', { name: 'Check' }) as HTMLButtonElement
    expect(check.disabled).toBe(true)
    expect(strip()).toBeNull()

    const point = layoutNumberLine()
    // The point waits beside the line, not on 0, until her first drop.
    expect(point.getAttribute('aria-label')).toBe('Number line point, not placed yet')
    await pointerDrag(point, tickClientX(4, LAST)) // value 1.0
    expect(check.disabled).toBe(false)
    expect(strip()).toBeNull() // a drop is not judged

    fireEvent.click(check)
    const nudge = strip()!
    expect(nudge.textContent).toBe('Not quite — have another look.')
    expect(document.activeElement).toBe(nudge)

    // Grabbing the point again is her next action: the strip goes away.
    pointerGrab(point, tickClientX(3, LAST) + 20)
    expect(strip()).toBeNull()
    await pointerRelease(point, tickClientX(3, LAST) + 20) // inside the 0.75 zone, snaps to 0.75
    expect(strip()).toBeNull()

    fireEvent.click(check)
    const correct = strip()!
    expect(correct.textContent).toBe('You did it!')
    expect(document.activeElement).toBe(correct)
  })

  it('answers 3/4 with the keyboard alone', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Response.json(concept)))
    render(<App />)
    await screen.findByText('Drag the point to 3/4')
    const check = screen.getByRole('button', { name: 'Check' }) as HTMLButtonElement

    layoutNumberLine().focus()
    await pressKey('Space')
    await pressKey('ArrowRight')
    await pressKey('ArrowRight')
    await pressKey('ArrowRight')
    await pressKey('Enter')
    expect(screen.getByRole('button', { name: 'Number line point at 0.75' })).toBeTruthy()

    fireEvent.click(check)
    expect(strip()!.textContent).toBe('You did it!')
  })
})
