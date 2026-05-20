import { centsToSgd } from "@/shared/utils/format";
import type { FoodEntryDetail, FoodHistoricalData } from "@/shared/types/api";

interface SubmitterPanelProps {
  detail: FoodEntryDetail;
  history: FoodHistoricalData;
}

export default function SubmitterPanel({ detail, history }: SubmitterPanelProps) {
  const submittedAt = new Date(detail.submittedAt);
  const dateStr = submittedAt.toLocaleDateString("en-SG", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
  const timeStr = submittedAt.toLocaleTimeString("en-SG", {
    hour: "2-digit",
    minute: "2-digit",
  });

  return (
    <div className="rounded-xl border border-secondary-200 bg-white">
      {/* Submitter header */}
      <div className="border-b border-secondary-100 p-5 text-center">
        <div className="mx-auto mb-3 flex h-20 w-20 items-center justify-center overflow-hidden rounded-full bg-secondary-100">
          {detail.submitterProfilePhotoPresignedUrl ? (
            <img
              src={detail.submitterProfilePhotoPresignedUrl}
              alt={detail.submitterUsername}
              className="h-full w-full object-cover"
              onError={(e) => {
                (e.target as HTMLImageElement).style.display = "none";
                (e.target as HTMLImageElement).parentElement!.innerHTML =
                  `<span class="text-2xl font-bold text-secondary-500">${detail.submitterUsername.charAt(0).toUpperCase()}</span>`;
              }}
            />
          ) : (
            <span className="text-2xl font-bold text-secondary-500">
              {detail.submitterUsername.charAt(0).toUpperCase()}
            </span>
          )}
        </div>
        <h3 className="text-lg font-semibold text-secondary-900">
          {history.submitterUsername || detail.submitterUsername}
        </h3>
        <p className="text-xs font-medium uppercase tracking-wider text-primary-700">
          Data Contributor
        </p>

        <div className="mt-4 grid grid-cols-3 gap-2">
          <div className="rounded-lg bg-secondary-50 p-2">
            <p className="text-[10px] uppercase text-secondary-400">Trust Score</p>
            <p className="text-sm font-semibold text-secondary-900">
              {detail.submitterWtfScore?.toFixed(1) ?? "—"}
            </p>
          </div>
          <div className="rounded-lg bg-secondary-50 p-2">
            <p className="text-[10px] uppercase text-secondary-400">Tenure</p>
            <p className="text-sm font-semibold text-secondary-900">
              {detail.submitterTenureDays}d
            </p>
          </div>
          <div className="rounded-lg bg-secondary-50 p-2">
            <p className="text-[10px] uppercase text-secondary-400">Entries</p>
            <p className="text-sm font-semibold text-secondary-900">
              {detail.submitterEntriesSubmitted.toLocaleString()}
            </p>
          </div>
        </div>
      </div>

      {/* Entry details */}
      <div className="p-5">
        <h4 className="mb-3 text-xs font-semibold uppercase tracking-wider text-secondary-400">
          Entry Details
        </h4>

        {detail.foodPhotoPresignedUrl && (
          <div className="mb-4 overflow-hidden rounded-lg bg-secondary-100">
            <img
              src={detail.foodPhotoPresignedUrl}
              alt={history.foodName}
              className="h-40 w-full object-cover"
              onError={(e) => {
                (e.target as HTMLImageElement).style.display = "none";
              }}
            />
          </div>
        )}

        <div className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-secondary-400">Timestamp</span>
            <span className="text-secondary-900">
              {dateStr} &middot; {timeStr}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-secondary-400">Price</span>
            <span className="font-semibold text-primary-700">
              {centsToSgd(history.sgCentsConsensusPrice)}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
