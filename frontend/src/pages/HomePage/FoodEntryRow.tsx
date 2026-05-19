import type { FoodPreview } from '@/shared/types/api'
import { centsToSgd } from '@/shared/utils/format'

interface FoodEntryRowProps {
  entry: FoodPreview
  onClick?: () => void
}

export default function FoodEntryRow({ entry, onClick }: FoodEntryRowProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left transition-colors hover:bg-primary-50"
    >
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary-100 text-sm font-bold text-secondary-500">
        {entry.name.charAt(0)}
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-secondary-900">
          {entry.name}
        </p>
        <p className="text-xs text-secondary-400">
          {entry.submitterUsername} &middot; {new Date(entry.createdAt).toLocaleDateString()}
        </p>
      </div>
      <div className="text-right">
        <p className="text-sm font-semibold text-primary-700">
          {centsToSgd(entry.sgCents)}
        </p>
        <p className="text-xs text-secondary-400">
          +{entry.upvotes}/-{entry.downvotes}
        </p>
      </div>
    </button>
  )
}
