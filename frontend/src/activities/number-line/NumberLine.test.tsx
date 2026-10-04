/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { NumberLine } from './NumberLine.tsx'
import type { NumberLinePayload } from './numberLine.ts'
import {
  announcement,
  LINE_Y,
  layoutNumberLine,
  PARK_X,
  pointerDrag,
  pointerGrab,
  pointerRelease,
  pressKey,
  tickClientX,
} from './testLayout.ts'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

// 0 to 2 in quarters: nine ticks, indices 0..8.
const payload: NumberLinePayload = { prompt: 'Drag the point to 3/4', min: 0, max: 2, step: 0.25, target: '3/4' }
const LAST = 8

function setup() {
  const onResponse = vi.fn()
  const onInteract = vi.fn()
  render(<NumberLine payload={payload} onResponse={onResponse} onInteract={onInteract} />)
  const point = layoutNumberLine()
  return { point, onResponse, onInteract }
}

const zone = (label: string) => screen.getByRole('group', { name: `Drop zone ${label}` })

const UNPLACED = 'Number line point, not placed yet'

/** The point is parked beside the line: no tick position, the parking class and the unplaced name. */
function expectUnplaced(point: HTMLElement) {
  expect(screen.getByRole('button', { name: UNPLACED })).toBe(point)
  expect(point.style.left).toBe('')
  expect(point.className).toContain('number-line__point--unplaced')
}

describe('NumberLine', () => {
  it('renders a labelled, focusable point button and one labelled zone per tick', () => {
    const { point } = setup()
    expect(screen.getByRole('button', { name: UNPLACED })).toBe(point)
    expect(point.tagName).toBe('BUTTON')
    expect(point.getAttribute('type')).toBe('button')
    expect(point.tabIndex).toBe(0)
    expect(screen.getByRole('group', { name: 'Number line from 0 to 2' })).toBeTruthy()
    for (const label of ['0', '0.25', '0.5', '0.75', '1', '1.25', '1.5', '1.75', '2']) {
      expect(zone(label)).toBeTruthy()
    }
  })

  it('formats tick labels without floating-point noise', () => {
    render(
      <NumberLine
        payload={{ prompt: 'Drag the point to 3/10', min: 0, max: 1, step: 0.1, target: '3/10' }}
        onResponse={() => {}}
        onInteract={() => {}}
      />,
    )
    expect(screen.getByText('0.3')).toBeTruthy()
    expect(screen.queryByText('0.30000000000000004')).toBeNull()
  })

  describe('keyboard', () => {
    it('picks up, moves one tick per arrow and drops, announcing each step', async () => {
      const { point, onResponse, onInteract } = setup()
      point.focus()

      await pressKey('Space')
      expect(onInteract).toHaveBeenCalledTimes(1)
      expect(announcement()).toBe("You picked up the point. It's not placed yet; it starts over 0.")

      await pressKey('ArrowRight')
      expect(announcement()).toBe('The point is over 0.25.')
      await pressKey('ArrowRight')
      await pressKey('ArrowRight')
      expect(announcement()).toBe('The point is over 0.75.')
      expect(zone('0.75').className).toContain('number-line__zone--over')

      await pressKey('Enter')
      expect(onResponse).toHaveBeenCalledExactlyOnceWith(0.75)
      expect(announcement()).toBe('You dropped the point on 0.75.')
      expect(screen.getByRole('button', { name: 'Number line point at 0.75' })).toBe(point)
    })

    it('moves with Up and Down too', async () => {
      const { point, onResponse } = setup()
      point.focus()
      await pressKey('Enter')
      await pressKey('ArrowUp')
      await pressKey('ArrowUp')
      await pressKey('ArrowDown')
      await pressKey('Space')
      expect(onResponse).toHaveBeenCalledExactlyOnceWith(0.25)
    })

    it('stays on the last tick when moved past it', async () => {
      const { point, onResponse } = setup()
      point.focus()
      await pressKey('Space')
      for (let i = 0; i < LAST + 3; i++) await pressKey('ArrowRight')
      expect(announcement()).toBe('The point is over 2.')
      await pressKey('Space')
      expect(onResponse).toHaveBeenCalledExactlyOnceWith(2)
    })

    it('stays on the first tick when moved before it', async () => {
      const { point, onResponse } = setup()
      point.focus()
      await pressKey('Space')
      await pressKey('ArrowLeft')
      await pressKey('Space')
      expect(onResponse).toHaveBeenCalledExactlyOnceWith(0)
    })

    it('cancels with Escape: back to the previous tick, re-reporting the previous answer', async () => {
      const { point, onResponse, onInteract } = setup()
      point.focus()
      await pressKey('Space')
      await pressKey('ArrowRight')
      await pressKey('ArrowRight')
      await pressKey('Space')
      expect(onResponse).toHaveBeenLastCalledWith(0.5)

      await pressKey('Space')
      expect(onInteract).toHaveBeenCalledTimes(2)
      await pressKey('ArrowRight')
      await pressKey('Escape')
      expect(announcement()).toBe('Okay, the point went back to 0.5.')
      expect(onResponse).toHaveBeenCalledTimes(2)
      expect(onResponse).toHaveBeenLastCalledWith(0.5)
      expect(screen.getByRole('button', { name: 'Number line point at 0.5' })).toBe(point)
    })

    it('reports nothing on a cancel before any answer, and parks the point again', async () => {
      const { point, onResponse } = setup()
      point.focus()
      await pressKey('Space')
      await pressKey('ArrowRight')
      await pressKey('Escape')
      expect(onResponse).not.toHaveBeenCalled()
      expect(announcement()).toBe('Okay, the point went back to its start, beside the line.')
      expectUnplaced(point)
    })

    it('answers min with Space then Enter from the unplaced start', async () => {
      const { point, onResponse } = setup()
      point.focus()
      await pressKey('Space')
      await pressKey('Enter')
      expect(onResponse).toHaveBeenCalledExactlyOnceWith(0)
      expect(announcement()).toBe('You dropped the point on 0.')
      expect(screen.getByRole('button', { name: 'Number line point at 0' })).toBe(point)
      expect(point.style.left).toBe('0%')
    })

    it('picks up a placed point announcing its tick', async () => {
      const { point } = setup()
      point.focus()
      await pressKey('Space')
      await pressKey('ArrowRight')
      await pressKey('Enter')
      await pressKey('Space')
      expect(announcement()).toBe("You picked up the point. It's on 0.25.")
    })

    it('drops on the starting tick after pick-up and Enter when ticks are closer than the point', async () => {
      // 0 to 2 in tenths: 20 gaps over 800px, so ticks are 40px apart, under the 48px point.
      const onResponse = vi.fn()
      render(
        <NumberLine
          payload={{ prompt: 'Drag the point to 3/10', min: 0, max: 2, step: 0.1, target: '3/10' }}
          onResponse={onResponse}
          onInteract={() => {}}
        />,
      )
      const point = layoutNumberLine()
      expect(point.style.transform).not.toContain('%')
      point.focus()
      await pressKey('Space')
      await pressKey('Enter')
      expect(onResponse).toHaveBeenCalledExactlyOnceWith(0)
      expect(announcement()).toBe('You dropped the point on 0.')
    })

    it('describes how to use the keyboard', () => {
      const { point } = setup()
      const describedBy = point.getAttribute('aria-describedby')
      expect(describedBy).toBeTruthy()
      expect(document.getElementById(describedBy!)?.textContent).toContain('arrow keys')
    })
  })

  describe('pointer', () => {
    it('drops onto the zone under the pointer and snaps to its tick', async () => {
      const { point, onResponse, onInteract } = setup()
      // Just right of 0.75 (x=300): still inside the 0.75 band.
      await pointerDrag(point, tickClientX(3, LAST) + 30)
      expect(onInteract).toHaveBeenCalledTimes(1)
      expect(onResponse).toHaveBeenCalledExactlyOnceWith(0.75)
      expect(point.style.left).toBe(`${(3 / LAST) * 100}%`)
      expect(point.getAttribute('aria-label')).toBe('Number line point at 0.75')
    })

    it('highlights only the zone under the held point', async () => {
      const { point } = setup()
      pointerGrab(point, tickClientX(3, LAST))
      expect(point.className).toContain('number-line__point--dragging')
      const over = document.querySelectorAll('.number-line__zone--over')
      expect(over).toHaveLength(1)
      expect(over[0]).toBe(zone('0.75'))
      await pointerRelease(point, tickClientX(3, LAST))
      expect(document.querySelectorAll('.number-line__zone--over')).toHaveLength(0)
    })

    it('returns the point to its previous tick when released off the line', async () => {
      const { point, onResponse } = setup()
      await pointerDrag(point, tickClientX(4, LAST))
      expect(onResponse).toHaveBeenLastCalledWith(1)

      await pointerDrag(point, tickClientX(6, LAST), -200) // far above the line
      expect(point.style.left).toBe('50%')
      expect(point.getAttribute('aria-label')).toBe('Number line point at 1')
      // The earlier answer is reported again, so Check is enabled again.
      expect(onResponse).toHaveBeenCalledTimes(2)
      expect(onResponse).toHaveBeenLastCalledWith(1)
      expect(announcement()).toBe("That's off the line, so the point went back to 1.")
    })

    it('reports nothing for a drop off the line before any answer, and parks the point again', async () => {
      const { point, onResponse } = setup()
      await pointerDrag(point, tickClientX(6, LAST), -200)
      expect(onResponse).not.toHaveBeenCalled()
      expect(announcement()).toBe("That's off the line, so the point went back to its start, beside the line.")
      expectUnplaced(point)
    })

    it('starts parked outside every zone, so a pick-up highlights nothing', async () => {
      const { point, onResponse } = setup()
      const rect = point.getBoundingClientRect()
      expect(rect.left + rect.width / 2).toBe(PARK_X)
      expect(rect.right).toBeLessThan(0) // the track, and its first zone, start at x = 0
      pointerGrab(point, PARK_X + 10) // held, just past the activation distance
      expect(document.querySelectorAll('.number-line__zone--over')).toHaveLength(0)
      expect(announcement()).toBe("You picked up the point. It's not placed yet.")
      await pointerRelease(point, PARK_X + 10)
      expect(onResponse).not.toHaveBeenCalled()
      expectUnplaced(point)
    })

    it('answers min by dropping the unplaced point on the first tick', async () => {
      const { point, onResponse } = setup()
      await pointerDrag(point, tickClientX(0, LAST) + 5)
      expect(onResponse).toHaveBeenCalledExactlyOnceWith(0)
      expect(point.getAttribute('aria-label')).toBe('Number line point at 0')
    })

    it("lands on the tick under the point's centre, not under the pointer", async () => {
      // 0 to 2 in tenths: ticks 40px apart, under the 48px point, so each zone band is 40px wide.
      const onResponse = vi.fn()
      render(
        <NumberLine
          payload={{ prompt: 'Drag the point to 3/10', min: 0, max: 2, step: 0.1, target: '3/10' }}
          onResponse={onResponse}
          onInteract={() => {}}
        />,
      )
      const point = layoutNumberLine()
      // Grabbed 22px right of its centre: released with the pointer at x=142, inside tick 4's band
      // (140..180), while the point's centre is at x=120, exactly on tick 3.
      const grabOffset = 22
      const pointerX = tickClientX(3, 20) + grabOffset
      pointerGrab(point, pointerX, LINE_Y, grabOffset)
      expect(document.querySelector('.number-line__zone--over')).toBe(zone('0.3'))
      await pointerRelease(point, pointerX)
      expect(onResponse).toHaveBeenCalledExactlyOnceWith(0.30000000000000004) // tickValue(3): snaps to tick 3
      expect(point.getAttribute('aria-label')).toBe('Number line point at 0.3')
    })

    it("misses the line when the point's centre is outside every zone, even with the pointer over one", async () => {
      const { point, onResponse } = setup()
      // Grabbed 22px right of its centre and released with the pointer at x=10, inside the first
      // zone (0..50): the point's centre is at x=-12, left of the line.
      await pointerDrag(point, 10, LINE_Y, 22)
      expect(onResponse).not.toHaveBeenCalled()
      expectUnplaced(point)
    })

    it('stops touch from scrolling the page and keeps the point at least 48px', () => {
      // jsdom does not apply stylesheets, so check the point's rule in app.css directly.
      const css = readFileSync(resolve(process.cwd(), 'src/styles/app.css'), 'utf8')
      const rule = /\.number-line__point \{([^}]*)\}/.exec(css)?.[1] ?? ''
      expect(rule).toMatch(/touch-action:\s*none/)
      expect(rule).toMatch(/min-width:\s*var\(--size-drag-min\)/)
      expect(rule).toMatch(/min-height:\s*var\(--size-drag-min\)/)
      const tokens = readFileSync(resolve(process.cwd(), 'src/styles/tokens.css'), 'utf8')
      expect(tokens).toMatch(/--size-drag-min:\s*48px/)
      expect(screen.queryByTestId('number-line-point')).toBeNull()
      setup()
      expect(screen.getByTestId('number-line-point').className).toBe('number-line__point number-line__point--unplaced')
    })

    it('parks the unplaced point beside the line with tokens, leaving room for it', () => {
      const css = readFileSync(resolve(process.cwd(), 'src/styles/app.css'), 'utf8')
      const parked = /\.number-line__point--unplaced \{([^}]*)\}/.exec(css)?.[1] ?? ''
      expect(parked).toMatch(/left:\s*var\(--offset-number-line-park\)/)
      const line = /\.number-line \{([^}]*)\}/.exec(css)?.[1] ?? ''
      expect(line).toMatch(/padding:[^;]*var\(--space-number-line-park\)/)
      const tokens = readFileSync(resolve(process.cwd(), 'src/styles/tokens.css'), 'utf8')
      // PARK_X in testLayout.ts mirrors this token: half the tile plus --space-2
      expect(tokens).toMatch(/--offset-number-line-park:\s*calc\(-1 \* \(var\(--size-drag-centre\) \+ var\(--space-2\)\)\)/)
      expect(tokens).toMatch(/--space-2:\s*8px/)
    })
  })

  describe('reduced motion', () => {
    it('marks the line still when the learner prefers reduced motion', () => {
      vi.stubGlobal(
        'matchMedia',
        vi.fn((query: string) => ({
          matches: query === '(prefers-reduced-motion: reduce)',
          media: query,
          addEventListener: vi.fn(),
          removeEventListener: vi.fn(),
        })),
      )
      setup()
      expect(screen.getByRole('group', { name: 'Number line from 0 to 2' }).className).toContain('number-line--still')
    })

    it('animates by default', () => {
      setup()
      expect(screen.getByRole('group', { name: 'Number line from 0 to 2' }).className).not.toContain(
        'number-line--still',
      )
    })
  })
})
