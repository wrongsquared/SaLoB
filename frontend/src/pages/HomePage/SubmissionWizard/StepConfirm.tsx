import { ArrowLeft, Loader2 } from 'lucide-react'

interface StepConfirmProps {
  eateryName: string
  foodName: string
  priceCents: number
  isSubmitting: boolean
  error: Error | null
  onSubmit: () => void
  onBack: () => void
}

export default function StepConfirm({
  eateryName,
  foodName,
  priceCents,
  isSubmitting,
  error,
  onSubmit,
  onBack,
}: StepConfirmProps) {
  return (
    <div className="space-y-4">
      <button
        type="button"
        onClick={onBack}
        disabled={isSubmitting}
        className="flex items-center gap-1 text-xs text-secondary-400 hover:text-secondary-700 disabled:opacity-40"
      >
        <ArrowLeft size={14} /> Back
      </button>
      <h2 className="text-lg font-semibold text-secondary-900">
        Confirm submission
      </h2>
      <div className="space-y-3 rounded-lg border border-secondary-200 bg-secondary-50 p-4">
        <div className="flex justify-between">
          <span className="text-sm text-secondary-400">Eatery</span>
          <span className="text-sm font-medium text-secondary-900">
            {eateryName}
          </span>
        </div>
        <div className="flex justify-between">
          <span className="text-sm text-secondary-400">Food</span>
          <span className="text-sm font-medium text-secondary-900">
            {foodName}
          </span>
        </div>
        <div className="flex justify-between">
          <span className="text-sm text-secondary-400">Price</span>
          <span className="text-sm font-semibold text-primary-700">
            ${(priceCents / 100).toFixed(2)}
          </span>
        </div>
      </div>
      {error && (
        <p className="text-sm text-red-500">
          Failed to submit. Please try again.
        </p>
      )}
      <div className="flex justify-end">
        <button
          type="button"
          onClick={onSubmit}
          disabled={isSubmitting}
          className="flex items-center gap-2 rounded-lg bg-primary-700 px-6 py-2 text-sm font-medium text-primary-50 hover:bg-primary-600 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isSubmitting ? (
            <>
              <Loader2 size={16} className="animate-spin" />
              Submitting...
            </>
          ) : (
            'Submit'
          )}
        </button>
      </div>
    </div>
  )
}
