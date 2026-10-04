import { useRef, useState, type PointerEvent } from 'react'
import { lastTickIndex, snap, tickValue } from './numberLine.ts'

interface NumberLineProps {
  min: number
  max: number
  step: number
  onAnswer: (value: number) => void
  onDragStart?: () => void
}

const WIDTH = 600
const HEIGHT = 100
const PAD = 40
const LINE_Y = 50

/** Minimal SVG number line with one draggable point. Mouse-driven via pointer events. */
/** Tick label without float noise (0.30000000000000004 → 0.3). Display only; snapping is unaffected. */
function formatTick(value: number): string {
  return String(Number(value.toFixed(6)))
}

export function NumberLine({ min, max, step, onAnswer, onDragStart }: NumberLineProps) {
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
    onDragStart?.()
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
    onAnswer(snapped)
  }

  const ticks = Array.from({ length: lastIndex + 1 }, (_, i) => tickValue(i, min, step))

  return (
    <svg
      ref={svgRef}
      viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
      width="100%"
      style={{ maxWidth: WIDTH, touchAction: 'none', userSelect: 'none' }}
      role="img"
      aria-label={`Number line from ${min} to ${max}`}
    >
      <line x1={PAD} y1={LINE_Y} x2={WIDTH - PAD} y2={LINE_Y} stroke="currentColor" strokeWidth={2} />
      {ticks.map((t) => (
        <g key={formatTick(t)}>
          <line x1={toX(t)} y1={LINE_Y - 8} x2={toX(t)} y2={LINE_Y + 8} stroke="currentColor" strokeWidth={2} />
          <text x={toX(t)} y={LINE_Y + 28} textAnchor="middle" fontSize={14} fill="currentColor">
            {formatTick(t)}
          </text>
        </g>
      ))}
      <circle
        data-testid="number-line-point"
        cx={toX(value)}
        cy={LINE_Y}
        r={12}
        fill="steelblue"
        style={{ cursor: dragging ? 'grabbing' : 'grab' }}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={() => setDragging(false)}
      />
    </svg>
  )
}
