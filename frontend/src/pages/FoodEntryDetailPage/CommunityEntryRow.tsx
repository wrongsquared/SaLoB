import { useNavigate } from "react-router-dom";
import { ThumbsUp, ThumbsDown } from "lucide-react";
import { centsToSgd } from "@/shared/utils/format";
import type { FoodPreview } from "@/shared/types/api";

interface CommunityEntryRowProps {
  entry: FoodPreview;
  isSelected: boolean;
}

export default function CommunityEntryRow({ entry, isSelected }: CommunityEntryRowProps) {
  const navigate = useNavigate();
  const net = entry.upvotes - entry.downvotes;

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={() => navigate(`/food-entry/${entry.foodEntryId}`)}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          navigate(`/food-entry/${entry.foodEntryId}`);
        }
      }}
      className={`flex cursor-pointer items-center gap-3 px-4 py-3 transition-colors ${
        isSelected
          ? "bg-primary-50/50 border-l-2 border-l-primary-700"
          : "hover:bg-secondary-50 border-l-2 border-l-transparent"
      }`}
    >
      <div className="flex h-10 w-10 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-secondary-100">
        {entry.photoPresignedUrl ? (
          <img
            src={entry.photoPresignedUrl}
            alt={entry.name}
            className="h-full w-full object-cover"
            onError={(e) => {
              (e.target as HTMLImageElement).style.display = "none";
            }}
          />
        ) : (
          <span className="text-sm font-bold text-secondary-400">{entry.name.charAt(0)}</span>
        )}
      </div>

      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-secondary-900">{isSelected ? "Entry selected" : entry.name}</p>
        <p className="text-xs text-secondary-400">
          Added by {entry.submitterUsername} &middot;{" "}
          {new Date(entry.createdAt).toLocaleDateString("en-SG", {
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
          })}
        </p>
      </div>

      <p className="text-sm font-bold text-secondary-900">{centsToSgd(entry.sgCents)}</p>

      <div className="flex items-center gap-1.5 rounded-full border border-secondary-200 bg-secondary-50 px-2.5 py-1">
        <ThumbsUp size={12} className="text-secondary-400" />
        <span className={`text-xs font-semibold ${net >= 0 ? "text-green-600" : "text-red-500"}`}>
          {net >= 0 ? "+" : ""}
          {net}
        </span>
        <ThumbsDown size={12} className="text-secondary-400" />
      </div>
    </div>
  );
}
