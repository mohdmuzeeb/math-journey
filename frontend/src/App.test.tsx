import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App.tsx'
import { NumberLine } from './activities/number-line/NumberLine.tsx'

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

/** jsdom has no layout: give the SVG the viewBox size so clientX maps 1:1 to SVG x. */
function numberLinePoint() {
  const svg = screen.getByRole('img')
  svg.getBoundingClientRect = () => ({ left: 0, top: 0, width: 600, height: 100 }) as DOMRect
  const point = screen.getByTestId('number-line-point')
  point.setPointerCapture = vi.fn()
  return point
}

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
    expect(screen.queryByRole('status')).toBeNull()

    // Line runs from x=40 (value 0) to x=560 (value 2), so 3/4 is at x=235 and 1.0 at x=300.
    const point = numberLinePoint()
    fireEvent.pointerDown(point, { pointerId: 1, clientX: 40 })
    fireEvent.pointerMove(point, { pointerId: 1, clientX: 300 })
    fireEvent.pointerUp(point, { pointerId: 1, clientX: 300 }) // value 1.0
    expect(check.disabled).toBe(false)
    expect(screen.queryByRole('status')).toBeNull() // a drop is not judged

    fireEvent.click(check)
    const nudge = screen.getByRole('status')
    expect(nudge.textContent).toBe('Not quite — have another look.')
    expect(document.activeElement).toBe(nudge)

    // Grabbing the point again is her next action: the strip goes away.
    fireEvent.pointerDown(point, { pointerId: 1, clientX: 300 })
    expect(screen.queryByRole('status')).toBeNull()
    fireEvent.pointerMove(point, { pointerId: 1, clientX: 240 })
    fireEvent.pointerUp(point, { pointerId: 1, clientX: 240 }) // ~0.77, snaps to 0.75
    expect(screen.queryByRole('status')).toBeNull()

    fireEvent.click(check)
    const correct = screen.getByRole('status')
    expect(correct.textContent).toBe('You did it!')
    expect(document.activeElement).toBe(correct)
  })
})

describe('NumberLine labels', () => {
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
})
