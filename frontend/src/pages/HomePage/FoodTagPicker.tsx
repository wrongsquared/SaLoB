import { useState } from 'react'
import { useMapStore } from '@/stores/mapStore'
import { useFoodSearch } from '@/shared/api/queries'
import { useDebounce } from '@/shared/hooks/useDebounce'
import { Loader2 } from 'lucide-react'

export default function FoodTagPicker() {
  const { selectedFoods, addFood, removeFood } = useMapStore()
  const [input, setInput] = useState('')
  const debouncedInput = useDebounce(input, 300)
  const { data: results, isLoading } = useFoodSearch(debouncedInput)

  const suggestions =
    results?.filter(
      (r) => !selectedFoods.includes(r.foodName),
    ) ?? []

  const handleSelect = (foodName: string) => {
    addFood(foodName)
    setInput('')
  }

  return (
    <div className="relative">
      <div className="relative">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Search foods to show on map... (max 5)"
          disabled={selectedFoods.length >= 5}
          className="w-full rounded-lg border border-white/30 bg-white/70 px-4 py-2.5 pr-10 text-base text-secondary-900 backdrop-blur-sm placeholder-secondary-400 outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500 disabled:cursor-not-allowed disabled:opacity-50"
        />
        {isLoading && (
          <Loader2
            size={20}
            className="absolute right-3.5 top-1/2 -translate-y-1/2 animate-spin text-secondary-400"
          />
        )}
      </div>
      {input && suggestions.length > 0 && (
        <div className="absolute z-10 mt-1 max-h-48 w-full overflow-auto rounded-lg border border-secondary-200 bg-white shadow-lg">
          {suggestions.map((s) => (
            <button
              key={s.foodId}
              type="button"
              onClick={() => handleSelect(s.foodName)}
              className="flex w-full items-center gap-2 px-4 py-2 text-left text-sm hover:bg-primary-50"
            >
              <span className="font-medium text-secondary-900">
                {s.foodName}
              </span>
            </button>
          ))}
        </div>
      )}
      {selectedFoods.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-1.5">
          {selectedFoods.map((food) => (
            <span
              key={food}
              className="inline-flex items-center gap-1 rounded-full bg-primary-100 px-2.5 py-0.5 text-xs font-medium text-primary-700"
            >
              {food}
              <button
                type="button"
                onClick={() => removeFood(food)}
                className="ml-0.5 inline-flex h-3.5 w-3.5 items-center justify-center rounded-full hover:bg-primary-200"
                aria-label={`Remove ${food}`}
              >
                &times;
              </button>
            </span>
          ))}
        </div>
      )}
    </div>
  )
}
