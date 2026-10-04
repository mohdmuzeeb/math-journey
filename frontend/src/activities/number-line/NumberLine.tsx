import { useRef, useState, type PointerEvent } from 'react'
import type { RendererProps } from '../types.ts'
import { lastTickIndex, snap, tickValue, type NumberLinePayload } from './numberLine.ts'

const WIDTH = 600
const HEIGHT = 100
const PAD = 40
const LINE_Y = 50
const POINT_RADIUS = 18

/** Tick label without float noise (0.30000000000000004 → 0.3). Display only; snapping is unaffected. */
function formatTick(value: number): string {
  return String(Number(value.toFixed(6)))
}

/**
 * The number-line activity: the prompt plus an SVG line with one draggable point (mouse, via pointer
 * events). Grabbing the point reports `onInteract`; releasing it reports the snapped value as the response.
 */
export function NumberLine({ payload, onResponse, onInteract }: RendererProps<NumberLinePayload, number>) {
  const { prompt, min, max, step } = payload
  const svgRef = useRef<SVGSVGElement>(null)
  const [value, setValue] = useState(min)
  const [dragging, setDragging] = useState(false)

  const lastIndex = lastTickIndex(min, max, step)
  const toX = (v: number) => PAD + ((v - min) / (max - min)) * (WIDTH - 2 * PAD)

  const valueAt = (clientX: number): number => {
    const svg = svgRef.current
    if (!svg) return value
    const rect = svg.getBoundingClientRect()
    if (rect.width === 0) return value
    const x = ((clientX - rect.left) / rect.width) * WIDTH
    return min + ((x - PAD) / (WIDTH - 2 * PAD)) * (max - min)
  }

  const handlePointerDown = (event: PointerEvent<SVGCircleElement>) => {
    event.currentTarget.setPointerCapture(event.pointerId)
    setDragging(true)
    onInteract()
  }

  const handlePointerMove = (event: PointerEvent<SVGCircleElement>) => {
    if (!dragging) return
    // While dragging, follow the pointer but stay on the line.
    setValue(Math.min(Math.max(valueAt(event.clientX), min), max))
  }

  const handlePointerUp = (event: PointerEvent<SVGCircleElement>) => {
    if (!dragging) return
    setDragging(false)
    const snapped = tickValue(snap(valueAt(event.clientX), min, max, step), min, step)
    setValue(snapped)
    onResponse(snapped)
  }

  const ticks = Array.from({ length: lastIndex + 1 }, (_, i) => tickValue(i, min, step))

  return (
    <>
      <p className="activity-prompt">{prompt}</p>
      <svg
        ref={svgRef}
        className="number-line"
        viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
        role="img"
        aria-label={`Number line from ${min} to ${max}`}
      >
        <line className="number-line__axis" x1={PAD} y1={LINE_Y} x2={WIDTH - PAD} y2={LINE_Y} />
        {ticks.map((t) => (
          <g key={formatTick(t)}>
            <line className="number-line__tick" x1={toX(t)} y1={LINE_Y - 8} x2={toX(t)} y2={LINE_Y + 8} />
            <text className="number-line__label" x={toX(t)} y={LINE_Y + 34} textAnchor="middle">
              {formatTick(t)}
            </text>
          </g>
        ))}
        <circle
          data-testid="number-line-point"
          className={dragging ? 'number-line__point number-line__point--dragging' : 'number-line__point'}
          cx={toX(value)}
          cy={LINE_Y}
          r={POINT_RADIUS}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={() => setDragging(false)}
        />
      </svg>
    </>
  )
}
