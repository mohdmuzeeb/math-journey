import type { ComponentType } from 'react'

/** What every activity Renderer receives: its kind's typed payload and two callbacks. */
export interface RendererProps<Payload, Response> {
  payload: Payload
  /** She has given (or changed) her response, e.g. dropped the point. Recorded, not judged. */
  onResponse: (response: Response) => void
  /** She has started her next action, e.g. grabbed the point again. */
  onInteract: () => void
}

/** One activity kind, typed by its generated content types. */
export interface TypedActivity<Payload, Answer, Response> {
  Renderer: ComponentType<RendererProps<Payload, Response>>
  /** The kind's pure comparison helper (AD-3: no math evaluation). */
  isCorrect: (response: Response, payload: Payload, answer: Answer) => boolean
}

/** A registry entry with the kind's types erased, so the shared Activity shell stays kind-agnostic. */
export type ActivityEntry = TypedActivity<unknown, unknown, unknown>
