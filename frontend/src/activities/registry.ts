// Activity registry (AD-6): the React side of each activity kind, looked up by `kind`.
import type { Item } from '../content-types/concept.d.ts'
import { NumberLine } from './number-line/NumberLine.tsx'
import { isCorrect as numberLineIsCorrect, type NumberLineAnswer, type NumberLinePayload } from './number-line/numberLine.ts'
import type { ActivityEntry, TypedActivity } from './types.ts'

/** Every kind the content schema allows. A kind without a registry entry fails `tsc`. */
export type ActivityKind = Item['kind']

/**
 * The one place the opaque API payload and answer (AD-7) meet their kind's generated types: the
 * content is validated against the kind's schema at startup, so the API value has that shape.
 */
function entry<Payload, Answer, Response>(activity: TypedActivity<Payload, Answer, Response>): ActivityEntry {
  return activity as unknown as ActivityEntry
}

export const registry: Record<ActivityKind, ActivityEntry> = {
  'number-line': entry<NumberLinePayload, NumberLineAnswer, number>({
    Renderer: NumberLine,
    isCorrect: numberLineIsCorrect,
  }),
}

/** The entry for a kind, or undefined for a kind this build does not know (never throws). */
export function lookupActivity(kind: string): ActivityEntry | undefined {
  return Object.hasOwn(registry, kind) ? registry[kind as ActivityKind] : undefined
}
