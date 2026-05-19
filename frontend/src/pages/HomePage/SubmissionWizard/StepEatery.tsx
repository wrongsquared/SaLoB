import { useState } from 'react'
import { useEaterySearch } from '@/shared/api/queries'
import { useDebounce } from '@/shared/hooks/useDebounce'

interface StepEateryProps {
  onSelect: (id: string, name: string) => void
}

export default function StepEatery({
  onSelect,
}: StepEateryProps) {
  const [input, setInput] = useState('')
  const debounced = useDebounce(input, 300)
  const { data: results, isLoading } = useEaterySearch(debounced)

  const handleSelect = (id: string, name: string) => {
    onSelect(id, name)
    setInput('')
  }

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold text-secondary-900">
        Select an eatery
      </h2>
      <input
        type="text"
        value={input}
        onChange={(e) => setInput(e.target.value)}
        placeholder="Search eateries..."
        className="w-full rounded-lg border border-secondary-200 px-4 py-2.5 text-sm text-secondary-900 outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
        autoFocus
      />
      <div className="max-h-60 space-y-1 overflow-y-auto">
        {isLoading && (
          <p className="py-4 text-center text-sm text-secondary-400">
            Searching...
          </p>
        )}
        {!isLoading &&
          results?.map((r) => (
            <button
              key={r.eateryId}
              type="button"
              onClick={() => handleSelect(r.eateryId, r.name)}
              className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left hover:bg-primary-50"
            >
              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-secondary-100 text-sm font-bold text-secondary-500">
                {r.name.charAt(0)}
              </div>
              <div>
                <p className="text-sm font-medium text-secondary-900">
                  {r.name}
                </p>
                {r.address && (
                  <p className="text-xs text-secondary-400">{r.address}</p>
                )}
              </div>
            </button>
          ))}
        {!isLoading && input && results?.length === 0 && (
          <p className="py-4 text-center text-sm text-secondary-400">
            No eateries found. Try a different search.
          </p>
        )}
      </div>
    </div>
  )
}
