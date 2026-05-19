import { useParams } from 'react-router-dom'
import { useFoodEntryDetail, useFoodHistoricalData } from '@/shared/api/queries'
import { centsToSgd } from '@/shared/utils/format'

export default function FoodEntryDetailPage() {
  const { foodEntryId } = useParams<{ foodEntryId: string }>()
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

  if (detailError || historyError) {
    return (
      <div className="flex h-64 items-center justify-center text-red-400">
        Failed to load food entry details. Please try again.
      </div>
    )
  }

  if (detailLoading || historyLoading) {
    return (
      <div className="mx-auto max-w-2xl space-y-4 p-6">
        <div className="h-6 w-48 animate-pulse rounded bg-secondary-100" />
        <div className="h-4 w-32 animate-pulse rounded bg-secondary-100" />
        <div className="h-32 animate-pulse rounded bg-secondary-100" />
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
    <div className="mx-auto max-w-2xl space-y-6 p-6">
      <div>
        <h1 className="text-xl font-bold text-secondary-900">
          {history.foodName}
        </h1>
        <p className="text-sm text-secondary-400">
          at {history.eateryAddress}
        </p>
      </div>

      <div className="rounded-lg border border-secondary-200 bg-secondary-50 p-4">
        <p className="text-xs text-secondary-400">Consensus price</p>
        <p className="text-2xl font-bold text-primary-700">
          {centsToSgd(history.sgCentsConsensusPrice)}
        </p>
      </div>

      <div className="space-y-3">
        <h2 className="text-sm font-semibold text-secondary-700">
          Submitted by
        </h2>
        <div className="flex items-center gap-3 rounded-lg border border-secondary-200 p-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-secondary-200 text-sm font-bold text-secondary-500">
            {detail.submitterUsername.charAt(0).toUpperCase()}
          </div>
          <div>
            <p className="text-sm font-medium text-secondary-900">
              {detail.submitterUsername}
            </p>
            <p className="text-xs text-secondary-400">
              WTF: {detail.submitterWtfScore.toFixed(1)} &middot;{' '}
              {detail.submitterTenureDays} days &middot;{' '}
              {detail.submitterEntriesSubmitted} entries
            </p>
          </div>
        </div>
      </div>

      <div className="space-y-3">
        <h2 className="text-sm font-semibold text-secondary-700">
          Recent entries
        </h2>
        <div className="divide-y divide-secondary-100 rounded-lg border border-secondary-200">
          {history.benchmarkDateEntries.map((entry) => (
            <div
              key={entry.foodEntryId}
              className="flex items-center justify-between px-4 py-2.5"
            >
              <div>
                <p className="text-sm text-secondary-900">
                  {centsToSgd(entry.sgCents)}
                </p>
                <p className="text-xs text-secondary-400">
                  {entry.submitterUsername} &middot;{' '}
                  {new Date(entry.createdAt).toLocaleDateString()}
                </p>
              </div>
              <div className="text-xs text-secondary-400">
                +{entry.upvotes}/-{entry.downvotes}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
