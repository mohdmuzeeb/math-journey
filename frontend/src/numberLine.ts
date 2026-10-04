// Pure number-line helpers. The browser does no math evaluation (AD-3): a drop is snapped
// to a grid tick and compared with the stored answer by tick index, never by float equality.

export interface NumberLinePayload {
  prompt: string
  min: number
  max: number
  step: number
  /** The problem's exact value, e.g. "3/4" */
  target: string
}

/** Index of the last tick on the line. */
export function lastTickIndex(min: number, max: number, step: number): number {
  return Math.round((max - min) / step)
}

/** Snaps a value to the nearest tick and returns that tick's index, clamped to the line's ends. */
export function snap(value: number, min: number, max: number, step: number): number {
  const index = Math.round((value - min) / step)
  return Math.min(Math.max(index, 0), lastTickIndex(min, max, step))
}

/** The value at a tick index. */
export function tickValue(index: number, min: number, step: number): number {
  return min + index * step
}

/** True when the drop lands on the same tick as the expected answer. */
export function isCorrect(dropValue: number, payload: NumberLinePayload, answer: number): boolean {
  const { min, max, step } = payload
  return snap(dropValue, min, max, step) === snap(answer, min, max, step)
}
