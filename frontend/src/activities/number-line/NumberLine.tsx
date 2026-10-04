import { useRef, useState } from 'react'
import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  pointerWithin,
  useDraggable,
  useDroppable,
  useSensor,
  useSensors,
  type Announcements,
  type ClientRect,
  type CollisionDetection,
  type DragEndEvent,
  type KeyboardCoordinateGetter,
  type UniqueIdentifier,
} from '@dnd-kit/core'
import type { RendererProps } from '../types.ts'
import { useReducedMotion } from '../../useReducedMotion.ts'
import { lastTickIndex, tickValue, type NumberLinePayload } from './numberLine.ts'

const POINT_ID = 'number-line-point'

/** Pixels the pointer must travel before a press becomes a drag (so a tap is not a drag). */
const POINTER_ACTIVATION_DISTANCE = 4

/** Tick label without float noise (0.30000000000000004 → 0.3). Display only; snapping is unaffected. */
function formatTick(value: number): string {
  return String(Number(value.toFixed(6)))
}

const zoneId = (index: number) => `number-line-tick-${index}`

/** The zone's tick index, from its droppable data. */
function zoneIndex(data: { current?: Record<string, unknown> } | undefined): number | undefined {
  const index = data?.current?.index
  return typeof index === 'number' ? index : undefined
}

/** Where tick `index` sits along the line, in % of the track width. */
function tickPercent(index: number, last: number): number {
  return last === 0 ? 50 : (index / last) * 100
}

/**
 * The horizontal pixel position of a tick, from its zone's rect. Zones tile the line, one band per
 * tick, so the end bands are half bands with the tick on their outer edge.
 */
function tickX(rect: ClientRect, index: number, last: number): number {
  if (last > 0 && index === 0) return rect.left
  if (last > 0 && index === last) return rect.right
  return rect.left + rect.width / 2
}

/** The tick nearest a horizontal pixel position, among the measured zones. */
function nearestTick(x: number, rects: Map<UniqueIdentifier, ClientRect>, last: number): number | undefined {
  let best: { index: number; distance: number } | undefined
  for (let index = 0; index <= last; index++) {
    const rect = rects.get(zoneId(index))
    if (!rect) continue
    const distance = Math.abs(tickX(rect, index, last) - x)
    if (!best || distance < best.distance) best = { index, distance }
  }
  return best?.index
}

/**
 * The number-line activity: the prompt plus a line with one draggable point. The point moves by
 * mouse, touch (pointer events) or keyboard, and drops onto one zone per tick. A drop on a zone
 * reports that tick's value; a drop off the line or a cancel returns the point to where it was.
 */
export function NumberLine({ payload, onResponse, onInteract }: RendererProps<NumberLinePayload, number>) {
  const { prompt, min, max, step } = payload
  const last = lastTickIndex(min, max, step)
  const [index, setIndex] = useState(0)
  // Whether the point's resting tick is a response she gave (it starts on `min` without one).
  const answered = useRef(false)
  // Set on pick-up: dnd-kit reports the starting zone as "over" straight away, which would replace the
  // pick-up announcement before it is read, so that first report stays silent.
  const justPickedUp = useRef(false)
  const reducedMotion = useReducedMotion()

  const label = (i: number) => formatTick(tickValue(i, min, step))
  const overLabel = (over: { data: { current?: Record<string, unknown> } } | null) => {
    const i = over ? zoneIndex(over.data) : undefined
    return i === undefined ? undefined : label(i)
  }

  /** Pointer: the zone under the pointer, or none off the line. Keyboard: the nearest tick. */
  const collisionDetection: CollisionDetection = (args) => {
    if (args.pointerCoordinates) return pointerWithin(args)
    const nearest = nearestTick(args.collisionRect.left + args.collisionRect.width / 2, args.droppableRects, last)
    if (nearest === undefined) return []
    const container = args.droppableContainers.find((c) => c.id === zoneId(nearest))
    return container ? [{ id: container.id, data: { droppableContainer: container, value: 0 } }] : []
  }

  /** One arrow press moves one tick: the point's centre goes to the neighbouring tick, clamped at the ends. */
  const coordinateGetter: KeyboardCoordinateGetter = (event, { context, currentCoordinates }) => {
    const direction = { ArrowRight: 1, ArrowUp: 1, ArrowLeft: -1, ArrowDown: -1 }[event.code]
    const { collisionRect, droppableRects } = context
    if (direction === undefined || !collisionRect) return undefined
    event.preventDefault()
    const from = nearestTick(collisionRect.left + collisionRect.width / 2, droppableRects, last)
    if (from === undefined) return undefined
    const to = Math.min(Math.max(from + direction, 0), last)
    const rect = droppableRects.get(zoneId(to))
    if (!rect) return undefined
    return { x: tickX(rect, to, last) - collisionRect.width / 2, y: currentCoordinates.y }
  }

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: POINTER_ACTIVATION_DISTANCE } }),
    useSensor(KeyboardSensor, { coordinateGetter }),
  )

  /** Back to the tick she grabbed it from; if that tick was her answer, it is her answer again. */
  const restore = () => {
    if (answered.current) onResponse(tickValue(index, min, step))
  }

  const handleDragEnd = ({ over }: DragEndEvent) => {
    const dropped = over ? zoneIndex(over.data) : undefined
    if (dropped === undefined) {
      restore()
      return
    }
    setIndex(dropped)
    answered.current = true
    onResponse(tickValue(dropped, min, step))
  }

  const announcements: Announcements = {
    onDragStart: () => {
      justPickedUp.current = true
      return `You picked up the point. It's on ${label(index)}.`
    },
    onDragOver: ({ over }) => {
      const value = overLabel(over)
      const first = justPickedUp.current
      justPickedUp.current = false
      if (first && value === label(index)) return undefined
      return value === undefined ? 'The point is off the line.' : `The point is over ${value}.`
    },
    onDragEnd: ({ over }) => {
      const value = overLabel(over)
      return value === undefined
        ? `That's off the line, so the point went back to ${label(index)}.`
        : `You dropped the point on ${value}.`
    },
    onDragCancel: () => `Okay, the point went back to ${label(index)}.`,
  }

  const ticks = Array.from({ length: last + 1 }, (_, i) => i)

  return (
    <>
      <p className="activity-prompt">{prompt}</p>
      <DndContext
        sensors={sensors}
        collisionDetection={collisionDetection}
        onDragStart={onInteract}
        onDragEnd={handleDragEnd}
        onDragCancel={restore}
        accessibility={{
          announcements,
          screenReaderInstructions: {
            draggable:
              'To pick up the point, press Space or Enter. Use the arrow keys to move it one tick at a time. ' +
              'Press Space or Enter to drop it, or Escape to put it back.',
          },
        }}
      >
        <div
          className={reducedMotion ? 'number-line number-line--still' : 'number-line'}
          role="group"
          aria-label={`Number line from ${formatTick(min)} to ${formatTick(max)}`}
        >
          <div className="number-line__track">
            <div className="number-line__axis" />
            {ticks.map((i) => (
              <Zone key={i} index={i} last={last} label={label(i)} />
            ))}
            {ticks.map((i) => (
              <div key={i} className="number-line__tick" style={{ left: `${tickPercent(i, last)}%` }} aria-hidden="true">
                <span className="number-line__label">{label(i)}</span>
              </div>
            ))}
            <Point percent={tickPercent(index, last)} label={label(index)} />
          </div>
        </div>
      </DndContext>
    </>
  )
}

/** One drop zone: a band of the line around its tick, highlighted while the point is over it. */
function Zone({ index, last, label }: { index: number; last: number; label: string }) {
  const { setNodeRef, isOver } = useDroppable({ id: zoneId(index), data: { index } })
  const half = last === 0 ? 50 : 50 / last
  const left = Math.max(tickPercent(index, last) - half, 0)
  const right = Math.min(tickPercent(index, last) + half, 100)
  return (
    <div
      ref={setNodeRef}
      className={isOver ? 'number-line__zone number-line__zone--over' : 'number-line__zone'}
      style={{ left: `${left}%`, width: `${right - left}%` }}
      role="group"
      aria-label={`Drop zone ${label}`}
      data-testid={zoneId(index)}
    />
  )
}

/** The draggable point: a button of at least 48px, resting on its tick. */
function Point({ percent, label }: { percent: number; label: string }) {
  const { setNodeRef, attributes, listeners, transform, isDragging } = useDraggable({
    id: POINT_ID,
    attributes: { roleDescription: 'draggable point' },
  })
  const x = transform?.x ?? 0
  const y = transform?.y ?? 0
  return (
    <button
      ref={setNodeRef}
      type="button"
      className={isDragging ? 'number-line__point number-line__point--dragging' : 'number-line__point'}
      style={{ left: `${percent}%`, transform: `translate3d(${x}px, ${y}px, 0)` }}
      data-testid={POINT_ID}
      {...attributes}
      {...listeners}
      aria-label={`Number line point at ${label}`}
    />
  )
}
