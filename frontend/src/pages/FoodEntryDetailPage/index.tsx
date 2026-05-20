import { useParams, useNavigate } from 'react-router-dom'
import { useFoodEntryDetail, useFoodHistoricalData } from '@/shared/api/queries'
import { ArrowLeft } from 'lucide-react'
import PriceChart from './PriceChart'
import CommunityEntryRow from './CommunityEntryRow'
import SubmitterPanel from './SubmitterPanel'

export default function FoodEntryDetailPage() {
  const { foodEntryId } = useParams<{ foodEntryId: string }>()
  const navigate = useNavigate()

  const {
    data: detail,
    isLoading: detailLoading,
    isError: detailError,
  } = useFoodEntryDetail(foodEntryId ?? null)

  const {
    data: history,
    isLoading: historyLoading,
    isError: historyError,
  } = useFoodHistoricalData(foodEntryId ?? null)

  const isLoading = detailLoading || historyLoading
  const isError = detailError || historyError

  if (isError) {
    return (
      <div className="flex h-64 items-center justify-center text-red-400">
        Failed to load food entry details. Please try again.
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="mx-auto max-w-6xl space-y-6 p-6">
        <div className="h-8 w-64 animate-pulse rounded bg-secondary-100" />
        <div className="h-4 w-48 animate-pulse rounded bg-secondary-100" />
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div className="col-span-2 h-80 animate-pulse rounded-xl bg-secondary-50" />
          <div className="h-80 animate-pulse rounded-xl bg-secondary-50" />
        </div>
      </div>
    )
  }

  if (!detail || !history) {
    return (
      <div className="flex h-64 items-center justify-center text-secondary-400">
        Food entry not found.
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-6 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="mb-6">
        <button
          type="button"
          onClick={() => navigate('/')}
          className="mb-4 flex items-center gap-1 text-sm text-secondary-400 hover:text-primary-700"
        >
          <ArrowLeft size={16} />
          Back to results
        </button>
        <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h1 className="text-3xl font-bold text-secondary-900">
              {history.foodName}
            </h1>
            <p className="mt-1 text-sm text-secondary-400">
              {history.eateryAddress}
            </p>
          </div>
          <div className="mt-3 flex gap-2 sm:mt-0">
            <button
              type="button"
              className="rounded-lg border border-secondary-200 px-4 py-2 text-sm font-medium text-secondary-500 hover:bg-secondary-50"
              title="TODO: implement outlier reporting"
            >
              Report Outlier
            </button>
            <button
              type="button"
              className="rounded-lg bg-primary-700 px-4 py-2 text-sm font-medium text-primary-50 hover:bg-primary-600"
              title="TODO: implement submission wizard"
            >
              Submit Entry
            </button>
          </div>
        </div>
      </div>

      {/* Main content grid */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Left column: chart + community entries */}
        <div className="col-span-1 space-y-6 lg:col-span-2">
          <PriceChart history={history} />

          <div>
            <h2 className="mb-3 text-lg font-semibold text-secondary-900">
              Community Entries
            </h2>
            <div className="divide-y divide-secondary-100 rounded-xl border border-secondary-200 bg-white">
              {history.benchmarkDateEntries.map((entry) => (
                <CommunityEntryRow
                  key={entry.foodEntryId}
                  entry={entry}
                  isSelected={entry.foodEntryId === foodEntryId}
                />
              ))}
            </div>
          </div>
        </div>

        {/* Right column: submitter panel */}
        <div className="lg:col-span-1">
          <SubmitterPanel detail={detail} history={history} />
        </div>
      </div>
    </div>
  )
}
