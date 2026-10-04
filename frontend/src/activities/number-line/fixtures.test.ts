// @vitest-environment node
/// <reference types="node" />
// The shared number-line fixtures (content/fixtures/number-line/) that the Java NumberLineFixturesTest
// also runs: the TypeScript comparison must agree with the server case by case.
import { readdirSync, readFileSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { isCorrect, snap, type NumberLineAnswer, type NumberLinePayload } from './numberLine.ts'

interface Fixture {
  description: string
  payload: NumberLinePayload
  answer: NumberLineAnswer
  cases: { response: number; canonical: number; verdict: 'correct' | 'wrong' }[]
}

// Vitest runs from frontend/ (npm test, and Maven's frontend plugin), so the content folder is one level up.
const FIXTURE_DIR = resolve(process.cwd(), '../content/fixtures/number-line')
const files = readdirSync(FIXTURE_DIR)
  .filter((name) => name.endsWith('.json'))
  .sort()

describe('number-line fixtures', () => {
  it('finds the fixture files', () => {
    expect(files.length).toBeGreaterThan(0)
  })

  describe.each(files)('%s', (file) => {
    const fixture = JSON.parse(readFileSync(join(FIXTURE_DIR, file), 'utf8')) as Fixture
    const { payload, answer } = fixture
    const { min, max, step } = payload

    it('has cases', () => {
      expect(fixture.cases.length).toBeGreaterThan(0)
    })

    it.each(fixture.cases.map((c, index) => ({ ...c, index })))(
      `${file} case $index: response $response snaps to tick $canonical and is $verdict`,
      ({ response, canonical, verdict, index }) => {
        expect(snap(response, min, max, step), `${file} case ${index}: canonical tick`).toBe(canonical)
        expect(isCorrect(response, payload, answer) ? 'correct' : 'wrong', `${file} case ${index}: verdict`).toBe(
          verdict,
        )
      },
    )
  })
})
