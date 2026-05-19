import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'

interface StepPriceProps {
  onConfirm: (cents: number) => void
  onBack: () => void
}

export default function StepPrice({ onConfirm, onBack }: StepPriceProps) {
  const [input, setInput] = useState('')

  const raw = input.replace(/[^0-9.]/g, '')
  const decimal = parseFloat(raw) || 0
  const cents = Math.round(decimal * 100)
  const display = `$${(cents / 100).toFixed(2)}`

  const handleConfirm = () => {
    if (cents > 0) onConfirm(cents)
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
        Enter the price
      </h2>
      <div className="relative">
        <input
          type="text"
          inputMode="decimal"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="$0.00"
          className="w-full rounded-lg border border-secondary-200 px-4 py-3 text-center text-2xl font-bold text-secondary-900 outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
          autoFocus
        />
      </div>
      <div className="flex justify-end">
        <button
          type="button"
          onClick={handleConfirm}
          disabled={cents === 0}
          className="rounded-lg bg-primary-700 px-6 py-2 text-sm font-medium text-primary-50 hover:bg-primary-600 disabled:cursor-not-allowed disabled:opacity-50"
        >
          Continue &middot; {display}
        </button>
      </div>
    </div>
  )
}
