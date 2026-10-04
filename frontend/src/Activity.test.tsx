import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Activity } from './Activity.tsx'
import type { ConceptItem } from './api.ts'

afterEach(cleanup)

const item: ConceptItem = {
  id: 'demo-number-line#three-quarters',
  kind: 'number-line',
  payload: { prompt: 'Drag the point to 3/4', min: 0, max: 2, step: 0.25, target: '3/4' },
  answer: 0.75,
}

/** Drags the point and releases it at clientX (the SVG is 600px wide; 0 is at x=40, 2 at x=560). */
function dropAt(clientX: number) {
  const svg = screen.getByRole('img')
  svg.getBoundingClientRect = () => ({ left: 0, top: 0, width: 600, height: 100 }) as DOMRect
  const point = screen.getByTestId('number-line-point')
  point.setPointerCapture = vi.fn()
  fireEvent.pointerDown(point, { pointerId: 1, clientX: 40 })
  fireEvent.pointerMove(point, { pointerId: 1, clientX })
  fireEvent.pointerUp(point, { pointerId: 1, clientX })
}

const checkButton = () => screen.getByRole('button', { name: 'Check' }) as HTMLButtonElement

describe('Activity', () => {
  it('starts with the prompt, a disabled Check and no strip', () => {
    render(<Activity item={item} />)
    expect(screen.getByText('Drag the point to 3/4')).toBeTruthy()
    expect(checkButton().disabled).toBe(true)
    expect(screen.queryByRole('status')).toBeNull()
  })

  it('enables Check once there is an answer, without judging it yet', () => {
    render(<Activity item={item} />)
    dropAt(300)
    expect(checkButton().disabled).toBe(false)
    expect(screen.queryByRole('status')).toBeNull()
  })

  it('shows the green strip and focuses it for a right answer', () => {
    render(<Activity item={item} />)
    dropAt(235) // 0.75
    fireEvent.click(checkButton())
    const strip = screen.getByRole('status')
    expect(strip.textContent).toBe('You did it!')
    expect(strip.className).toContain('feedback--correct')
    expect(document.activeElement).toBe(strip)
  })

  it('shows the amber strip and focuses it for a wrong answer', () => {
    render(<Activity item={item} />)
    dropAt(300) // 1.0
    fireEvent.click(checkButton())
    const strip = screen.getByRole('status')
    expect(strip.textContent).toBe('Not quite — have another look.')
    expect(strip.className).toContain('feedback--nudge')
    expect(document.activeElement).toBe(strip)
  })

  it('clears the strip on her next grab, then judges the new drop', () => {
    render(<Activity item={item} />)
    dropAt(300)
    fireEvent.click(checkButton())
    expect(screen.getByRole('status')).toBeTruthy()

    fireEvent.pointerDown(screen.getByTestId('number-line-point'), { pointerId: 1, clientX: 300 })
    expect(screen.queryByRole('status')).toBeNull()
    fireEvent.pointerUp(screen.getByTestId('number-line-point'), { pointerId: 1, clientX: 235 })
    fireEvent.click(checkButton())
    expect(screen.getByRole('status').textContent).toBe('You did it!')
  })

  it('disables Check again when a grab is cancelled before a new drop', () => {
    render(<Activity item={item} />)
    dropAt(300)
    expect(checkButton().disabled).toBe(false)
    const point = screen.getByTestId('number-line-point')
    fireEvent.pointerDown(point, { pointerId: 1, clientX: 300 })
    fireEvent.pointerCancel(point, { pointerId: 1 })
    expect(checkButton().disabled).toBe(true)
  })

  it('shows a friendly message for an unknown kind instead of crashing', () => {
    render(<Activity item={{ ...item, kind: 'abacus' }} />)
    expect(screen.getByText(/This activity isn't ready yet/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Check' })).toBeNull()
  })
})
