import { useState } from 'react'
import { useFoodSearch, useCreateFood } from '@/shared/api/queries'
import { useDebounce } from '@/shared/hooks/useDebounce'
import { ArrowLeft, Loader2 } from 'lucide-react'

interface StepFoodProps {
  onSelect: (foodId: string, foodName: string) => void
  onBack: () => void
}

export default function StepFood({ onSelect, onBack }: StepFoodProps) {
  const [input, setInput] = useState('')
  const [useFreeText, setUseFreeText] = useState(false)
  const debounced = useDebounce(input, 300)
  const { data: results, isLoading } = useFoodSearch(debounced)
  const createMutation = useCreateFood()

  const handleSelectExisting = (foodId: string, foodName: string) => {
    onSelect(foodId, foodName)
  }

  const handleCreateNew = async () => {
    if (!input.trim()) return
    const result = await createMutation.mutateAsync({ foodName: input.trim() })
    onSelect(result.foodId, result.foodName)
  }

  if (useFreeText) {
    return (
      <div className="space-y-4">
        <button
          type="button"
          onClick={() => setUseFreeText(false)}
          className="flex items-center gap-1 text-xs text-secondary-400 hover:text-secondary-700"
        >
          <ArrowLeft size={14} /> Back to search
        </button>
        <h2 className="text-lg font-semibold text-secondary-900">
          Enter food name
        </h2>
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="e.g. Chicken Rice"
          className="w-full rounded-lg border border-secondary-200 px-4 py-2.5 text-sm text-secondary-900 outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
          autoFocus
        />
        <div className="flex justify-end">
          <button
            type="button"
            onClick={handleCreateNew}
            disabled={!input.trim() || createMutation.isPending}
            className="flex items-center gap-2 rounded-lg bg-primary-700 px-6 py-2 text-sm font-medium text-primary-50 hover:bg-primary-600 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {createMutation.isPending && <Loader2 size={14} className="animate-spin" />}
            Continue
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <button
        type="button"
        onClick={onBack}
        className="flex items-center gap-1 text-xs text-secondary-400 hover:text-secondary-700"
      >
        <ArrowLeft size={14} /> Back
      </button>
      <h2 className="text-lg font-semibold text-secondary-900">
        Select a food
      </h2>
      <input
        type="text"
        value={input}
        onChange={(e) => setInput(e.target.value)}
        placeholder="Search foods..."
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
              key={r.foodId}
              type="button"
              onClick={() => handleSelectExisting(r.foodId, r.foodName)}
              className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left hover:bg-primary-50"
            >
              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-secondary-100 text-sm font-bold text-secondary-500">
                {r.foodName.charAt(0)}
              </div>
              <p className="text-sm font-medium text-secondary-900">
                {r.foodName}
              </p>
            </button>
          ))}
        {!isLoading && input && results?.length === 0 && (
          <div className="py-4 text-center">
            <p className="text-sm text-secondary-400">
              No foods found in the database.
            </p>
            <button
              type="button"
              onClick={() => setUseFreeText(true)}
              className="mt-2 text-sm font-medium text-primary-700 hover:underline"
            >
              Enter it manually &rarr;
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
