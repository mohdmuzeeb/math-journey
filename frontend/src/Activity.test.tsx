import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { Activity } from './Activity.tsx'
import type { ConceptItem } from './api.ts'
import {
  layoutNumberLine,
  pointerDrag,
  pointerGrab,
  pointerRelease,
  pressKey,
  tickClientX,
} from './activities/number-line/testLayout.ts'

afterEach(cleanup)

const item: ConceptItem = {
  id: 'demo-number-line#three-quarters',
  kind: 'number-line',
  payload: { prompt: 'Drag the point to 3/4', min: 0, max: 2, step: 0.25, target: '3/4' },
  answer: 0.75,
}

// The line runs 0 to 2 in quarters: tick 3 is 0.75 and tick 4 is 1.0.
const LAST = 8

/** Drags the point onto a tick's drop zone with the mouse and releases it there. */
async function dropOnTick(tick: number) {
  await pointerDrag(layoutNumberLine(), tickClientX(tick, LAST))
}

const checkButton = () => screen.getByRole('button', { name: 'Check' }) as HTMLButtonElement

/** The feedback strip (dnd-kit's own live region is also a status, so pick the strip by its class). */
const strip = () => screen.queryAllByRole('status').find((el) => el.classList.contains('feedback')) ?? null

describe('Activity', () => {
  it('starts with the prompt, a disabled Check and no strip', () => {
    render(<Activity item={item} />)
    expect(screen.getByText('Drag the point to 3/4')).toBeTruthy()
    expect(checkButton().disabled).toBe(true)
    expect(strip()).toBeNull()
  })

  it('enables Check once there is an answer, without judging it yet', async () => {
    render(<Activity item={item} />)
    await dropOnTick(4)
    expect(checkButton().disabled).toBe(false)
    expect(strip()).toBeNull()
  })

  it('shows the green strip and focuses it for a right answer', async () => {
    render(<Activity item={item} />)
    await dropOnTick(3) // 0.75
    fireEvent.click(checkButton())
    const shown = strip()!
    expect(shown.textContent).toBe('You did it!')
    expect(shown.className).toContain('feedback--correct')
    expect(document.activeElement).toBe(shown)
  })

  it('shows the amber strip and focuses it for a wrong answer', async () => {
    render(<Activity item={item} />)
    await dropOnTick(4) // 1.0
    fireEvent.click(checkButton())
    const shown = strip()!
    expect(shown.textContent).toBe('Not quite — have another look.')
    expect(shown.className).toContain('feedback--nudge')
    expect(document.activeElement).toBe(shown)
  })

  it('clears the strip on her next grab, then judges the new drop', async () => {
    render(<Activity item={item} />)
    await dropOnTick(4)
    fireEvent.click(checkButton())
    expect(strip()).toBeTruthy()

    const point = layoutNumberLine()
    pointerGrab(point, tickClientX(3, LAST))
    expect(strip()).toBeNull()
    expect(checkButton().disabled).toBe(true)
    await pointerRelease(point, tickClientX(3, LAST))
    fireEvent.click(checkButton())
    expect(strip()!.textContent).toBe('You did it!')
  })

  it('starts with the point unplaced beside the line, not on min', () => {
    render(<Activity item={item} />)
    const point = layoutNumberLine()
    expect(point.getAttribute('aria-label')).toBe('Number line point, not placed yet')
    expect(point.style.left).toBe('')
    expect(checkButton().disabled).toBe(true)
  })

  it('keeps Check disabled and parks the point again when a grab is cancelled before any answer', async () => {
    render(<Activity item={item} />)
    const point = layoutNumberLine()
    point.focus()
    await pressKey('Space')
    await pressKey('ArrowRight')
    await pressKey('Escape')
    expect(checkButton().disabled).toBe(true)
    expect(point.getAttribute('aria-label')).toBe('Number line point, not placed yet')
    expect(point.className).toContain('number-line__point--unplaced')
  })

  it('judges an answer at min right after a drop on the first tick', async () => {
    const atMin: ConceptItem = { ...item, payload: { ...item.payload, prompt: 'Drag the point to 0', target: '0' }, answer: 0 }
    render(<Activity item={atMin} />)
    await pointerDrag(layoutNumberLine(), tickClientX(0, LAST) + 5)
    expect(checkButton().disabled).toBe(false)
    fireEvent.click(checkButton())
    expect(strip()!.textContent).toBe('You did it!')
  })

  it('re-enables Check when a grab after an answer is cancelled', async () => {
    render(<Activity item={item} />)
    await dropOnTick(3)
    fireEvent.click(checkButton())
    expect(strip()!.textContent).toBe('You did it!')

    const point = layoutNumberLine()
    point.focus()
    await pressKey('Space')
    expect(strip()).toBeNull()
    expect(checkButton().disabled).toBe(true)
    await pressKey('ArrowRight')
    await pressKey('Escape')
    expect(checkButton().disabled).toBe(false)
    fireEvent.click(checkButton())
    expect(strip()!.textContent).toBe('You did it!')
  })

  it('re-enables Check when a later drop misses the line', async () => {
    render(<Activity item={item} />)
    await dropOnTick(4)
    const point = layoutNumberLine()
    pointerGrab(point, tickClientX(6, LAST), -200)
    expect(checkButton().disabled).toBe(true)
    await pointerRelease(point, tickClientX(6, LAST), -200)
    expect(checkButton().disabled).toBe(false)
    fireEvent.click(checkButton())
    expect(strip()!.textContent).toBe('Not quite — have another look.')
  })

  it('shows a friendly message for an unknown kind instead of crashing', () => {
    render(<Activity item={{ ...item, kind: 'abacus' }} />)
    expect(screen.getByText(/This activity isn't ready yet/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Check' })).toBeNull()
  })
})
