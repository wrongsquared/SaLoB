import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/authStore";
import { useVote } from "@/shared/api/queries";
import { ThumbsUp, ThumbsDown } from "lucide-react";
import { centsToSgd } from "@/shared/utils/format";
import type { FoodPreview } from "@/shared/types/api";

interface CommunityEntryRowProps {
  entry: FoodPreview;
  isSelected: boolean;
}

export default function CommunityEntryRow({ entry, isSelected }: CommunityEntryRowProps) {
  const navigate = useNavigate();
  const voteMutation = useVote();
  const currentUserId = useAuthStore((s) => s.user?.id);
  const isOwnEntry = currentUserId === entry.submitterId;

  const net = entry.upvotes - entry.downvotes;

  const handleVote = (e: React.MouseEvent, isUpvote: boolean) => {
    e.stopPropagation();
    if (isOwnEntry) return;
    const newVote = entry.currentUserVote === isUpvote ? null : isUpvote;
    voteMutation.mutate({ foodEntryId: entry.foodEntryId, isUpvote: newVote });
  };

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
        <button
          type="button"
          onClick={(e) => handleVote(e, true)}
          disabled={isOwnEntry}
          title={isOwnEntry ? "You cannot vote on your own entry" : entry.currentUserVote === true ? "Remove upvote" : "Upvote"}
          className={`cursor-pointer rounded p-1 transition-all duration-100 disabled:cursor-not-allowed ${
            entry.currentUserVote === true
              ? "bg-green-100 text-green-700 hover:bg-green-200"
              : "text-secondary-400 hover:scale-110 hover:bg-green-50 hover:text-green-600"
          } active:scale-90`}
        >
          <ThumbsUp size={14} />
        </button>
        <span className={`text-xs font-semibold ${net >= 0 ? "text-green-600" : "text-red-500"}`}>
          {net >= 0 ? "+" : ""}
          {net}
        </span>
        <button
          type="button"
          onClick={(e) => handleVote(e, false)}
          disabled={isOwnEntry}
          title={isOwnEntry ? "You cannot vote on your own entry" : entry.currentUserVote === false ? "Remove downvote" : "Downvote"}
          className={`cursor-pointer rounded p-1 transition-all duration-100 disabled:cursor-not-allowed ${
            entry.currentUserVote === false
              ? "bg-red-100 text-red-700 hover:bg-red-200"
              : "text-secondary-400 hover:scale-110 hover:bg-red-50 hover:text-red-500"
          } active:scale-90`}
        >
          <ThumbsDown size={14} />
        </button>
      </div>
    </div>
  );
}
