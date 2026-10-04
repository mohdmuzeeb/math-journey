// API types are generated from the backend's OpenAPI document (AD-7): never hand-write them here.
import type { components } from './api/schema.d.ts'

export type Concept = components['schemas']['ConceptResponse']
export type ConceptItem = components['schemas']['ConceptItemResponse']

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
