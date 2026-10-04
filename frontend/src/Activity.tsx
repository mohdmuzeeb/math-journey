import { useEffect, useRef, useState } from 'react'
import type { ConceptItem } from './api.ts'
import { lookupActivity } from './activities/registry.ts'
import type { ActivityEntry } from './activities/types.ts'

type Verdict = 'correct' | 'nudge'

const MESSAGES: Record<Verdict, string> = {
  correct: 'You did it!',
  nudge: 'Not quite — have another look.',
}

/**
 * One item, whatever its kind: the kind's Renderer, then the shared feedback strip and Check button.
 * A response is only recorded; Check judges it, and the verdict stays until her next action.
 */
export function Activity({ item }: { item: ConceptItem }) {
  const entry = lookupActivity(item.kind)
  if (!entry) {
    return <p className="activity-prompt">This activity isn't ready yet. Let's try another one soon!</p>
  }
  return <KnownActivity key={item.id} entry={entry} item={item} />
}

function KnownActivity({ entry, item }: { entry: ActivityEntry; item: ConceptItem }) {
  const { Renderer } = entry
  const [response, setResponse] = useState<{ value: unknown } | null>(null)
  const [verdict, setVerdict] = useState<Verdict | null>(null)
  const [checks, setChecks] = useState(0)
  const stripRef = useRef<HTMLDivElement>(null)

  // After every Check, move focus to the strip so the verdict is read out and seen.
  useEffect(() => {
    if (checks > 0) stripRef.current?.focus()
  }, [checks])

  const check = () => {
    if (!response) return
    setVerdict(entry.isCorrect(response.value, item.payload, item.answer) ? 'correct' : 'nudge')
    setChecks((n) => n + 1)
  }

  return (
    <div className="activity">
      <Renderer
        payload={item.payload}
        onResponse={(value) => setResponse({ value })}
        onInteract={() => {
          // Her next action: the old verdict and the old response no longer apply.
          setVerdict(null)
          setResponse(null)
        }}
      />
      {verdict && (
        <div ref={stripRef} className={`feedback feedback--${verdict}`} role="status" tabIndex={-1}>
          {MESSAGES[verdict]}
        </div>
      )}
      <div className="activity-actions">
        <button type="button" className="button-primary" disabled={!response} onClick={check}>
          Check
        </button>
      </div>
    </div>
  )
}
