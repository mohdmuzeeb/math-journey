// Type-level checks on the generated content types (scripts/gen-content-types.mjs). `tsc -b` (the
// build) type-checks this file; a regression here is a compile error, not just a failed assertion.
import { describe, expect, expectTypeOf, it } from 'vitest'
import type { Item } from './content-types/concept.d.ts'
import type { Curriculum, Land } from './content-types/curriculum.d.ts'

describe('generated content types', () => {
  it('types curriculum lands as land objects', () => {
    const curriculum: Curriculum = {
      lands: [
        { id: 'numbers', title: 'Numbers', concepts: ['8.NS.1-rational-numbers'] },
        { id: 'equations', title: 'Equations', concepts: [] },
        { id: 'functions', title: 'Functions', concepts: [] },
        { id: 'shapes', title: 'Shapes', concepts: [] },
        { id: 'data', title: 'Data', concepts: [] },
      ],
      prerequisites: [],
    }
    expectTypeOf(curriculum.lands[0]).toEqualTypeOf<Land>()
    expectTypeOf<Land>().toEqualTypeOf<{ id: string; title: string; concepts: string[] }>()
    expect(curriculum.lands.map((land) => land.id)).toEqual(['numbers', 'equations', 'functions', 'shapes', 'data'])
  })

  it('types answers as unknown, so a number is accepted', () => {
    const answer: Item['answer'] = 0.75
    const similarAnswer: Item['walkthrough']['similar']['answer'] = 0.5
    expectTypeOf<Item['answer']>().toBeUnknown()
    expectTypeOf<Item['walkthrough']['similar']['answer']>().toBeUnknown()
    expect([answer, similarAnswer]).toEqual([0.75, 0.5])
  })
})
