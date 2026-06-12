import { useState } from 'react';
import { isAxiosError } from 'axios';
import { useNavigate } from 'react-router-dom';
import { useMapStore } from '@/stores/mapStore';
import { useAuthGuard } from '@/shared/hooks/useAuthGuard';
import { useEateryDetail, useReportEateryClosed } from '@/shared/api/queries';
import { X, Flag, Clock, Star } from 'lucide-react';
import { centsToSgd } from '@/shared/utils';
import type { FoodPreview } from '@/shared/types/api';
import VoteButtonGroup from '@/components/VoteButtonGroup';
import ConfirmDialog from '@/components/ConfirmDialog';

function computeRating(foodPreviews: FoodPreview[]) {
  if (foodPreviews.length === 0) return { rating: 0, reviews: 0 };
  const totalUp = foodPreviews.reduce((sum, f) => sum + f.upvotes, 0);
  const totalDown = foodPreviews.reduce((sum, f) => sum + f.downvotes, 0);
  const total = totalUp + totalDown;
  const rating = total > 0 ? (totalUp / total) * 5 : 0;
  return { rating: Math.round(rating * 10) / 10, reviews: total };
}

function FoodEntryCard({ entry }: { entry: FoodPreview }) {
  const navigate = useNavigate();

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={() => navigate(`/food-entry/${entry.foodEntryId}`)}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          navigate(`/food-entry/${entry.foodEntryId}`);
        }
      }}
      className="flex w-full cursor-pointer items-center gap-3 rounded-xl border border-secondary-100 bg-white p-3 text-left transition-colors hover:border-primary-200 hover:bg-primary-50/50"
    >
      <div className="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-secondary-100">
        {entry.photoPresignedUrl ? (
          <img
            src={entry.photoPresignedUrl}
            alt={entry.name}
            className="h-full w-full object-cover"
            onError={(e) => {
              (e.target as HTMLImageElement).style.display = 'none';
            }}
          />
        ) : (
          <span className="text-lg">{entry.name.charAt(0)}</span>
        )}
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-semibold text-secondary-900">{entry.name}</p>
        <p className="text-sm font-bold text-accent-600">{centsToSgd(entry.sgCents)}</p>
      </div>
      <VoteButtonGroup entry={entry} />
    </div>
  );
}

export default function EateryPanel() {
  const selectedEateryId = useMapStore((s) => s.selectedEateryId);
  const sidebarOpen = useMapStore((s) => s.sidebarOpen);
  const selectEatery = useMapStore((s) => s.selectEatery);
  const setSidebarOpen = useMapStore((s) => s.setSidebarOpen);
  const setWizardOpen = useMapStore((s) => s.setWizardOpen);
  const reportedEateryIds = useMapStore((s) => s.reportedEateryIds);

  const { data: eatery, isLoading, isError } = useEateryDetail(selectedEateryId);
  const reportMutation = useReportEateryClosed();
  const guard = useAuthGuard();
  const [confirmReportOpen, setConfirmReportOpen] = useState(false);
  const [reportTarget, setReportTarget] = useState<{ id: string; name: string } | null>(null);

  const isReported = selectedEateryId
    ? reportedEateryIds.has(selectedEateryId) || (eatery?.hasUserReported ?? false)
    : false;

  const handleConfirmReport = () => {
    if (!reportTarget) return;
    void (async () => {
      try {
        await reportMutation.mutateAsync(reportTarget.id);
      } catch (err) {
        if (!isAxiosError(err) || err.response?.status !== 409) return;
      }
      useMapStore.getState().markEateryReported(reportTarget.id);
    })();
  };
  const { rating, reviews } = eatery ? computeRating(eatery.foodPreviews) : { rating: 0, reviews: 0 };

  if (!sidebarOpen) return null;

  return (
    <>
      <aside
        className="absolute left-0 top-0 z-20 flex h-full w-full flex-col border-r border-secondary-200 bg-white shadow-xl md:w-96"
        role="dialog"
        aria-label="Eatery details"
      >
        {/* Hero Image */}
        <div className="relative h-48 shrink-0 overflow-hidden bg-secondary-100">
          {eatery?.photoUrl ? (
            <img
              src={eatery.photoUrl}
              alt={eatery.name}
              className="h-full w-full object-cover"
              onError={(e) => {
                (e.target as HTMLImageElement).style.display = 'none';
              }}
            />
          ) : (
            <div className="flex h-full items-center justify-center text-secondary-300">
              <span className="text-4xl">🏪</span>
            </div>
          )}
          {rating >= 4.0 && (
            <span className="absolute left-3 top-3 rounded-md bg-accent-600 px-2 py-0.5 text-xs font-bold text-white shadow-sm">
              TOP RATED
            </span>
          )}
          <button
            type="button"
            onClick={() => {
              selectEatery(null);
              setSidebarOpen(false);
            }}
            className="absolute right-2 top-2 rounded-full bg-black/40 p-1.5 text-white backdrop-blur-sm transition-colors hover:bg-black/60"
            aria-label="Close sidebar"
          >
            <X size={16} />
          </button>
        </div>

        {/* Header */}
        <div className="border-b border-secondary-100 px-4 py-4">
          {isLoading ? (
            <div className="space-y-2">
              <div className="h-6 w-48 animate-pulse rounded bg-secondary-100" />
              <div className="h-4 w-32 animate-pulse rounded bg-secondary-100" />
            </div>
          ) : isError ? (
            <p className="text-sm text-red-500">Failed to load eatery details.</p>
          ) : eatery ? (
            <>
              <h2 className="text-xl font-bold text-secondary-900">{eatery.name}</h2>
              <p className="mt-1 text-sm text-secondary-500">{eatery.address}</p>
              <div className="mt-2 flex items-center gap-2">
                <div className="flex items-center gap-1">
                  <Star size={14} className="fill-yellow-400 text-yellow-400" />
                  <span className="text-sm font-semibold text-secondary-900">{rating.toFixed(1)}</span>
                </div>
                <span className="text-xs text-secondary-400">({reviews.toLocaleString()} reviews)</span>
              </div>
            </>
          ) : null}
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2 border-b border-secondary-100 px-4 py-2">
          {eatery && (
            <button
              type="button"
              onClick={() => guard(() => setWizardOpen(true, eatery.eateryId))}
              className="rounded-lg bg-primary-700 px-4 py-2 text-sm font-medium text-primary-50 transition-colors hover:bg-primary-600"
            >
              + Submit Price
            </button>
          )}
          {eatery && !isReported && (
            <button
              type="button"
              onClick={() =>
                guard(() => {
                  setReportTarget({ id: eatery.eateryId, name: eatery.name });
                  setConfirmReportOpen(true);
                })
              }
              disabled={reportMutation.isPending}
              className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm text-secondary-500 transition-colors hover:text-red-500 disabled:opacity-40"
              aria-label="Report as closed"
            >
              <Clock size={14} />
              Report as Closed
            </button>
          )}
          {isReported && (
            <span className="flex items-center gap-1.5 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-500">
              <Flag size={14} />
              Reported
            </span>
          )}
        </div>

        {/* Scrollable content */}
        <div className="flex-1 overflow-y-auto">
          {isLoading && (
            <div className="space-y-3 p-4">
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="h-16 animate-pulse rounded-xl bg-secondary-50" />
              ))}
            </div>
          )}

          {isError && (
            <div className="flex h-full items-center justify-center p-8 text-center text-sm text-red-400">
              Could not load eatery data. Please try again.
            </div>
          )}

          {!isLoading && !isError && eatery && (
            <>
              <div className="px-4 pt-4 pb-2">
                <h3 className="text-xs font-semibold uppercase tracking-wider text-secondary-400">
                  Proprietary Pricing Index
                </h3>
              </div>

              {eatery.foodPreviews.length > 0 ? (
                <div className="space-y-2 px-4 pb-4">
                  {eatery.foodPreviews.map((entry) => (
                    <FoodEntryCard key={entry.foodEntryId} entry={entry} />
                  ))}
                </div>
              ) : (
                <div className="flex h-32 items-center justify-center p-8 text-center text-sm text-secondary-400">
                  No food entries yet for this eatery.
                </div>
              )}
            </>
          )}
        </div>
      </aside>
      {reportTarget && (
        <ConfirmDialog
          open={confirmReportOpen}
          onOpenChange={setConfirmReportOpen}
          title="Report as Closed"
          message={`Are you sure you want to report ${reportTarget.name} as permanently closed?`}
          confirmLabel="Report"
          onConfirm={handleConfirmReport}
          variant="destructive"
        />
      )}
    </>
  );
}
