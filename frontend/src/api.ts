// Hand-written for the tracer only; entry 2 replaces these with generated API types (AD-7).

export interface ConceptItem {
  id: string
  kind: string
  payload: unknown
  answer: unknown
}

export interface Concept {
  id: string
  title: string
  items: ConceptItem[]
}

/** Fetches a concept by id. Resolves to null when the server has no such concept (404). */
export async function fetchConcept(id: string): Promise<Concept | null> {
  const response = await fetch(`/api/concepts/${encodeURIComponent(id)}`)
  if (response.status === 404) {
    return null
  }
  if (!response.ok) {
    throw new Error(`Loading concept ${id} failed with HTTP ${response.status}`)
  }
  return (await response.json()) as Concept
}
