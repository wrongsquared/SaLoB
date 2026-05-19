import { useNavigate } from 'react-router-dom'
import { useMapStore } from '@/stores/mapStore'
import { useEateryDetail, useReportEateryClosed } from '@/shared/api/queries'
import { X, Flag } from 'lucide-react'
import FoodEntryRow from './FoodEntryRow'

export default function EateryPanel() {
  const navigate = useNavigate()
  const { selectedEateryId, sidebarOpen, selectEatery, setSidebarOpen, setWizardOpen, reportedEateryIds } =
    useMapStore()
  const { data: eatery, isLoading, isError } = useEateryDetail(selectedEateryId)
  const reportMutation = useReportEateryClosed()

  const isReported = selectedEateryId ? reportedEateryIds.has(selectedEateryId) : false

  if (!sidebarOpen) return null

  return (
    <aside
      className="absolute left-0 top-0 z-20 flex h-full w-96 flex-col border-r border-secondary-200 bg-white shadow-lg"
      role="dialog"
      aria-label="Eatery details"
    >
      <div className="flex items-center justify-between border-b border-secondary-100 px-4 py-3">
        <div>
          {isLoading ? (
            <div className="h-5 w-32 animate-pulse rounded bg-secondary-100" />
          ) : isError ? (
            <p className="text-sm text-red-500">
              Failed to load eatery details.
            </p>
          ) : (
            <>
              <h2 className="text-base font-bold text-secondary-900">
                {eatery?.name ?? 'Unknown'}
              </h2>
              <p className="text-xs text-secondary-400">
                {eatery?.typeLabel}
              </p>
            </>
          )}
        </div>
        <div className="flex items-center gap-2">
          {eatery && (
            <button
              type="button"
              onClick={() => setWizardOpen(true, eatery.eateryId)}
              className="rounded-lg bg-primary-700 px-3 py-1.5 text-xs font-medium text-primary-50 hover:bg-primary-600"
            >
              + Price
            </button>
          )}
          {eatery && !isReported && (
            <button
              type="button"
              onClick={() => {
                reportMutation.mutate(eatery.eateryId)
                useMapStore.getState().markEateryReported(eatery.eateryId)
              }}
              disabled={reportMutation.isPending}
              className="flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs text-secondary-400 hover:text-red-500 disabled:opacity-40"
              aria-label="Report as closed"
            >
              <Flag size={12} />
              Report
            </button>
          )}
          {isReported && (
            <span className="rounded-lg bg-red-50 px-2 py-1.5 text-xs text-red-500">
              Reported
            </span>
          )}
          <button
            type="button"
            onClick={() => {
              selectEatery(null)
              setSidebarOpen(false)
            }}
            className="rounded-full p-1 text-secondary-400 hover:bg-secondary-100 hover:text-secondary-700"
            aria-label="Close sidebar"
          >
            <X size={18} />
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto">
        {isLoading && (
          <div className="space-y-2 p-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <div
                key={i}
                className="h-14 animate-pulse rounded-lg bg-secondary-50"
              />
            ))}
          </div>
        )}

        {isError && (
          <div className="flex h-full items-center justify-center p-8 text-center text-sm text-red-400">
            Could not load eatery data. Please try again.
          </div>
        )}

        {!isLoading && !isError && eatery && eatery.foodPreviews.length === 0 && (
          <div className="flex h-full items-center justify-center p-8 text-center text-sm text-secondary-400">
            No food entries yet for this eatery.
          </div>
        )}

        {!isLoading && !isError && eatery && eatery.foodPreviews.length > 0 && (
          <div className="space-y-1 p-2">
            {eatery.foodPreviews.map((entry) => (
              <FoodEntryRow
                key={entry.foodEntryId}
                entry={entry}
                onClick={() => navigate(`/food-entry/${entry.foodEntryId}`)}
              />
            ))}
          </div>
        )}
      </div>
    </aside>
  )
}
