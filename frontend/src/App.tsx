import { useEffect, useState, type ReactNode } from 'react'
import { fetchConcept, type Concept } from './api.ts'
import { Activity } from './Activity.tsx'

const CONCEPT_ID = 'demo-number-line'

type LoadState =
  | { status: 'loading' }
  | { status: 'missing' }
  | { status: 'error' }
  | { status: 'ready'; concept: Concept }

function App() {
  const [state, setState] = useState<LoadState>({ status: 'loading' })

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

  if (state.status === 'loading') return <Page><p>Loading…</p></Page>
  if (state.status === 'missing') return <Page><p>No lesson yet</p></Page>
  if (state.status === 'error') return <Page><p>Something went wrong loading the lesson. Please try again.</p></Page>

  const item = state.concept.items[0]
  if (!item) return <Page><p>No lesson yet</p></Page>

  return (
    <Page>
      <section className="card">
        <h1 className="card-title">{state.concept.title}</h1>
        <Activity key={item.id} item={item} />
      </section>
    </Page>
  )
}

function Page({ children }: { children: ReactNode }) {
  return <main className="page">{children}</main>
}

export default App
