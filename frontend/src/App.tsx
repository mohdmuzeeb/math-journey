import { useEffect, useState } from 'react'
import { fetchConcept, type Concept } from './api.ts'
import { NumberLine } from './NumberLine.tsx'
import { isCorrect, type NumberLinePayload } from './numberLine.ts'

const CONCEPT_ID = 'demo-number-line'

type LoadState =
  | { status: 'loading' }
  | { status: 'missing' }
  | { status: 'error' }
  | { status: 'ready'; concept: Concept }

function App() {
  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const [feedback, setFeedback] = useState('')

  useEffect(() => {
    let cancelled = false
    fetchConcept(CONCEPT_ID)
      .then((concept) => {
        if (!cancelled) setState(concept ? { status: 'ready', concept } : { status: 'missing' })
      })
      .catch(() => {
        if (!cancelled) setState({ status: 'error' })
      })
    return () => {
      cancelled = true
    }
  }, [])

  if (state.status === 'loading') return <main><p>Loading…</p></main>
  if (state.status === 'missing') return <main><p>No lesson yet</p></main>
  if (state.status === 'error') return <main><p>Something went wrong loading the lesson. Please try again.</p></main>

  const item = state.concept.items[0]
  if (!item) return <main><p>No lesson yet</p></main>
  const payload = item.payload as NumberLinePayload
  const answer = item.answer as number

  return (
    <main>
      <h1>{state.concept.title}</h1>
      <p>{payload.prompt}</p>
      <NumberLine
        min={payload.min}
        max={payload.max}
        step={payload.step}
        onDragStart={() => setFeedback('')}
        onAnswer={(value) => setFeedback(isCorrect(value, payload, answer) ? 'Correct!' : 'Not quite — try again')}
      />
      <p aria-live="polite">{feedback}</p>
    </main>
  )
}

export default App
