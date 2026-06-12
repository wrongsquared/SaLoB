import { useAuthStore } from '@/stores/authStore';
import { useVote } from '@/shared/api/queries';
import { ThumbsUp, ThumbsDown } from 'lucide-react';
import type { FoodPreview } from '@/shared/types/api';
import type { ComponentProps } from 'react';

function VoteBtn({ isUpvote, ...btnProps }: { isUpvote: boolean } & ComponentProps<'button'>) {
  const Icon = isUpvote ? ThumbsUp : ThumbsDown;
  return (
    <button type="button" {...btnProps}>
      <Icon size={14} />
    </button>
  );
}

interface VoteButtonGroupProps {
  entry: FoodPreview;
  compact?: boolean;
}

export default function VoteButtonGroup({ entry, compact }: VoteButtonGroupProps) {
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

  const upBtn = (
    <VoteBtn
      isUpvote
      onClick={(e) => handleVote(e, true)}
      disabled={isOwnEntry}
      title={
        isOwnEntry ? 'You cannot vote on your own entry' : entry.currentUserVote === true ? 'Remove upvote' : 'Upvote'
      }
      className={`cursor-pointer rounded p-1 transition-all duration-100 disabled:cursor-not-allowed ${
        entry.currentUserVote === true
          ? 'bg-green-100 text-green-700 hover:bg-green-200'
          : 'text-secondary-400 hover:scale-110 hover:bg-green-50 hover:text-green-600'
      } active:scale-90`}
    />
  );

  const downBtn = (
    <VoteBtn
      isUpvote={false}
      onClick={(e) => handleVote(e, false)}
      disabled={isOwnEntry}
      title={
        isOwnEntry
          ? 'You cannot vote on your own entry'
          : entry.currentUserVote === false
            ? 'Remove downvote'
            : 'Downvote'
      }
      className={`cursor-pointer rounded p-1 transition-all duration-100 disabled:cursor-not-allowed ${
        entry.currentUserVote === false
          ? 'bg-red-100 text-red-700 hover:bg-red-200'
          : 'text-secondary-400 hover:scale-110 hover:bg-red-50 hover:text-red-500'
      } active:scale-90`}
    />
  );

  if (compact) {
    return (
      <div className="flex items-center gap-1.5 rounded-full border border-secondary-200 bg-secondary-50 px-2.5 py-1">
        {upBtn}
        <span className={`text-xs font-semibold ${net >= 0 ? 'text-green-600' : 'text-red-500'}`}>
          {net >= 0 ? '+' : ''}
          {net}
        </span>
        {downBtn}
      </div>
    );
  }

  return (
    <div className="flex flex-col items-end gap-0.5">
      <div className="flex items-center gap-1.5 text-xs">
        {upBtn}
        <span className="text-green-600">{entry.upvotes}</span>
        {downBtn}
        <span className="text-red-500">{entry.downvotes}</span>
      </div>
      <span className={`text-xs font-semibold ${net >= 0 ? 'text-green-600' : 'text-red-500'}`}>
        NET {net >= 0 ? '+' : ''}
        {net}
      </span>
    </div>
  );
}
