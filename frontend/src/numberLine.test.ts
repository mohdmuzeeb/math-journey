import { describe, expect, it } from 'vitest'
import { isCorrect, snap, tickValue } from './numberLine.ts'

const payload = { prompt: 'Drag the point to 3/4', min: 0, max: 2, step: 0.25 }
const answer = 0.75

describe('snap', () => {
  it('rounds to the nearest tick index', () => {
    expect(snap(0.74, 0, 2, 0.25)).toBe(3)
    expect(snap(0.8, 0, 2, 0.25)).toBe(3)
    expect(snap(0.9, 0, 2, 0.25)).toBe(4)
  })

  it('clamps values beyond the ends to the end ticks', () => {
    expect(snap(-3, 0, 2, 0.25)).toBe(0)
    expect(snap(5, 0, 2, 0.25)).toBe(8)
  })

  it('maps back to tick values', () => {
    expect(tickValue(3, 0, 0.25)).toBe(0.75)
  })
})

describe('isCorrect', () => {
  it('accepts a drop released nearest the answer tick', () => {
    expect(isCorrect(0.75, payload, answer)).toBe(true)
    expect(isCorrect(0.7, payload, answer)).toBe(true)
    // 0.1 + 0.2 + 0.45 is not exactly 0.75 in floating point, but snaps to the same tick.
    expect(isCorrect(0.1 + 0.2 + 0.45, payload, answer)).toBe(true)
  })

  it('rejects a drop on another tick', () => {
    expect(isCorrect(1.0, payload, answer)).toBe(false)
    expect(isCorrect(0.5, payload, answer)).toBe(false)
  })

  it('clamps drops beyond the ends before comparing', () => {
    expect(isCorrect(-1, payload, answer)).toBe(false)
    expect(isCorrect(9, payload, answer)).toBe(false)
    expect(isCorrect(9, payload, 2)).toBe(true)
    expect(isCorrect(-1, payload, 0)).toBe(true)
  })
})
