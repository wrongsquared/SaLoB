import { useState } from 'react'
import { useMapStore } from '@/stores/mapStore'
import { useSubmitFoodEntry } from '@/shared/api/queries'
import { X } from 'lucide-react'
import StepEatery from './StepEatery'
import StepFood from './StepFood'
import StepPrice from './StepPrice'
import StepConfirm from './StepConfirm'

const TOTAL_STEPS = 4

export default function SubmissionWizard() {
  const { wizardOpen, setWizardOpen } =
    useMapStore()
  const [step, setStep] = useState(1)
  const [eateryId, setEateryId] = useState<string | null>(null)
  const [eateryName, setEateryName] = useState('')
  const [foodId, setFoodId] = useState('')
  const [foodName, setFoodName] = useState('')
  const [priceCents, setPriceCents] = useState(0)

  const submitMutation = useSubmitFoodEntry()

  const handleClose = () => {
    setWizardOpen(false)
    setStep(1)
    setEateryId(null)
    setEateryName('')
    setFoodId('')
    setFoodName('')
    setPriceCents(0)
    submitMutation.reset()
  }

  if (!wizardOpen) return null

  const submitting = submitMutation.isPending
  const submitError = submitMutation.error
  const submitSuccess = submitMutation.isSuccess

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="relative w-full max-w-lg rounded-xl bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-secondary-100 px-6 py-4">
          <span className="text-sm text-secondary-400">
            Step {step} of {TOTAL_STEPS}
          </span>
          <button
            type="button"
            onClick={handleClose}
            disabled={submitting}
            className="rounded-full p-1 text-secondary-400 hover:bg-secondary-100 hover:text-secondary-700 disabled:opacity-40"
            aria-label="Close wizard"
          >
            <X size={18} />
          </button>
        </div>

        <div className="px-6 py-5">
          {submitSuccess ? (
            <div className="space-y-4 py-8 text-center">
              <div className="text-4xl"></div>
              <p className="text-lg font-semibold text-secondary-900">
                Price submitted!
              </p>
              <p className="text-sm text-secondary-400">
                {foodName} at {eateryName} &middot; $
                {(priceCents / 100).toFixed(2)}
              </p>
              <button
                type="button"
                onClick={handleClose}
                className="rounded-lg bg-primary-700 px-6 py-2 text-sm font-medium text-primary-50 hover:bg-primary-600"
              >
                Done
              </button>
            </div>
          ) : (
            <>
              {step === 1 && (
                <StepEatery
                  onSelect={(id, name) => {
                    setEateryId(id)
                    setEateryName(name)
                    setStep(2)
                  }}
                />
              )}
              {step === 2 && (
                <StepFood
                  onSelect={(id, name) => {
                    setFoodId(id)
                    setFoodName(name)
                    setStep(3)
                  }}
                  onBack={() => setStep(1)}
                />
              )}
              {step === 3 && (
                <StepPrice
                  onConfirm={(cents) => {
                    setPriceCents(cents)
                    setStep(4)
                  }}
                  onBack={() => setStep(2)}
                />
              )}
              {step === 4 && (
                <StepConfirm
                  eateryName={eateryName}
                  foodName={foodName}
                  priceCents={priceCents}
                  isSubmitting={submitting}
                  error={submitError}
                  onSubmit={() => {
                    if (!eateryId || !foodId) return
                    submitMutation.mutate({
                      eateryId,
                      foodId,
                      priceSgCents: priceCents,
                    })
                  }}
                  onBack={() => setStep(3)}
                />
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
