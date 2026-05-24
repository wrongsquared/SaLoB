import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@/shared/api/client'
import { useEaterySearchCombined, useCreateEatery } from '@/shared/api/queries'
import { useDebounce } from '@/shared/hooks/useDebounce'
import { useMapStore } from '@/stores/mapStore'

interface EateryType {
  id: string
  label: string
}

export default function SearchBar() {
  const [input, setInput] = useState('')
  const [open, setOpen] = useState(false)
  const debounced = useDebounce(input, 300)
  const { data: results, isLoading } = useEaterySearchCombined(debounced)
  const createEatery = useCreateEatery()
  const [creating, setCreating] = useState<string | null>(null)
  const ref = useRef<HTMLDivElement>(null)

  const selectEatery = useMapStore((s) => s.selectEatery)

  const { data: eateryTypes } = useQuery({
    queryKey: ["eatery-types"],
    queryFn: async () => {
      const { data } = await apiClient.get<EateryType[]>("/eatery-types")
      return data
    },
    staleTime: 300_000,
  })

  const defaultTypeId = eateryTypes?.[0]?.id

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  useEffect(() => {
    if (input && (results?.local.length || results?.onemap.length)) {
      setOpen(true)
    } else {
      setOpen(false)
    }
  }, [results, input])

  const handleSelectLocal = (id: string) => {
    selectEatery(id)
    setInput('')
    setOpen(false)
  }

  const handleCreateFromOneMap = async (item: { name: string; address: string }) => {
    if (!defaultTypeId) return
    setCreating(item.name)
    try {
      const eatery = await createEatery.mutateAsync({
        name: item.name,
        address: item.address,
        typeId: defaultTypeId,
      })
      selectEatery(eatery.eateryId)
      setInput('')
      setOpen(false)
    } catch {
      setCreating(null)
    }
  }

  const hasLocal = results?.local && results.local.length > 0
  const hasOneMap = results?.onemap && results.onemap.length > 0

  return (
    <div ref={ref} className="relative w-full max-w-md">
      <input
        type="text"
        value={input}
        onChange={(e) => {
          setInput(e.target.value)
          if (!open) setOpen(true)
        }}
        onFocus={() => { if (input && (hasLocal || hasOneMap)) setOpen(true) }}
        placeholder="Search eateries..."
        className="w-full rounded-lg border border-white/30 bg-white/70 px-4 py-2.5 pl-11 text-base text-secondary-900 backdrop-blur-sm placeholder-secondary-400 outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
      />
      <svg
        className="absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-secondary-400"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        strokeWidth={2}
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M21 21l-4.35-4.35M11 19a8 8 0 100-16 8 8 0 000 16z"
        />
      </svg>

      {open && (
        <div className="absolute left-0 right-0 top-full z-30 mt-1 max-h-72 overflow-y-auto rounded-xl border border-secondary-200 bg-white shadow-lg">
          {isLoading && (
            <p className="py-4 text-center text-sm text-secondary-400">
              Searching...
            </p>
          )}

          {!isLoading && hasLocal && (
            <div>
              {hasOneMap && (
                <p className="px-3 py-1.5 text-xs font-semibold uppercase tracking-wider text-secondary-400">
                  Existing
                </p>
              )}
              {results!.local.map((r) => (
                <button
                  key={r.eateryId}
                  type="button"
                  onClick={() => handleSelectLocal(r.eateryId)}
                  className="flex w-full items-center gap-3 px-3 py-2.5 text-left hover:bg-primary-50"
                >
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-secondary-100 text-sm font-bold text-secondary-500">
                    {r.name.charAt(0)}
                  </div>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-secondary-900">
                      {r.name}
                    </p>
                    {r.address && (
                      <p className="truncate text-xs text-secondary-400">{r.address}</p>
                    )}
                  </div>
                </button>
              ))}
            </div>
          )}

          {!isLoading && hasOneMap && (
            <div>
              {hasLocal && (
                <p className="border-t border-secondary-100 px-3 py-1.5 pt-2 text-xs font-semibold uppercase tracking-wider text-secondary-400">
                  OneMap Results
                </p>
              )}
              {results!.onemap.map((r, i) => (
                <button
                  key={`onemap-${i}`}
                  type="button"
                  onClick={() => handleCreateFromOneMap(r)}
                  disabled={creating === r.name || !defaultTypeId}
                  className="flex w-full items-center gap-3 px-3 py-2.5 text-left hover:bg-primary-50 disabled:opacity-50"
                >
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-amber-100 text-sm font-bold text-amber-600">
                    {r.name.charAt(0)}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-secondary-900">
                      {r.name}
                    </p>
                    <p className="truncate text-xs text-secondary-400">{r.address}</p>
                  </div>
                  <span className="shrink-0 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700">
                    New
                  </span>
                </button>
              ))}
            </div>
          )}

          {!isLoading && input && !hasLocal && !hasOneMap && (
            <p className="py-4 text-center text-sm text-secondary-400">
              No eateries found.
            </p>
          )}
        </div>
      )}
    </div>
  )
}
