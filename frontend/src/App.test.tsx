import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App.tsx'
import { NumberLine } from './NumberLine.tsx'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('App', () => {
  it('shows a friendly message when there is no lesson (404)', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(null, { status: 404 })))
    render(<App />)
    expect(await screen.findByText('No lesson yet')).toBeTruthy()
  })

  it('shows the prompt and judges drops on the number line', async () => {
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
    vi.stubGlobal('fetch', vi.fn(async () => Response.json(concept)))
    render(<App />)
    expect(await screen.findByText('Drag the point to 3/4')).toBeTruthy()
    expect(screen.getByText('0.75')).toBeTruthy()

    // jsdom has no layout: give the SVG the viewBox size so clientX maps 1:1 to SVG x.
    const svg = screen.getByRole('img')
    svg.getBoundingClientRect = () => ({ left: 0, top: 0, width: 600, height: 100 }) as DOMRect
    const point = screen.getByTestId('number-line-point')
    point.setPointerCapture = vi.fn()
    // Line runs from x=40 (value 0) to x=560 (value 2), so 3/4 is at x=235.
    const drag = (clientX: number) => {
      fireEvent.pointerDown(point, { pointerId: 1, clientX: 40 })
      fireEvent.pointerMove(point, { pointerId: 1, clientX })
      fireEvent.pointerUp(point, { pointerId: 1, clientX })
    }

    drag(300) // value 1.0
    expect(screen.getByText('Not quite — try again')).toBeTruthy()

    // A new drag clears the old verdict, so a second wrong drop is judged (and announced) again.
    fireEvent.pointerDown(point, { pointerId: 1, clientX: 300 })
    expect(screen.queryByText('Not quite — try again')).toBeNull()
    fireEvent.pointerMove(point, { pointerId: 1, clientX: 105 })
    fireEvent.pointerUp(point, { pointerId: 1, clientX: 105 }) // value 0.25
    expect(screen.getByText('Not quite — try again')).toBeTruthy()

    drag(240) // value ~0.77, snaps to 0.75
    expect(screen.getByText('Correct!')).toBeTruthy()
  })
})

describe('NumberLine labels', () => {
  it('formats tick labels without floating-point noise', () => {
    render(<NumberLine min={0} max={1} step={0.1} onAnswer={() => {}} />)
    expect(screen.getByText('0.3')).toBeTruthy()
    expect(screen.queryByText('0.30000000000000004')).toBeNull()
  })
})
