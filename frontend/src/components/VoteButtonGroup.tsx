import { useAuthStore } from '@/stores/authStore';
import { useAuthGuard } from '@/shared/hooks/useAuthGuard';
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
  const guard = useAuthGuard();

  const handleVote = (isUpvote: boolean) => (e: React.MouseEvent) => {
    e.stopPropagation();
    guard(() => {
      if (isOwnEntry) return;
      const newVote = entry.currentUserVote === isUpvote ? null : isUpvote;
      voteMutation.mutate({ foodEntryId: entry.foodEntryId, isUpvote: newVote });
    });
  };

  const voteTitle = (isUpvote: boolean, isActive: boolean): string => {
    const action = isUpvote ? 'Upvote' : 'Downvote';
    if (isOwnEntry) return 'You cannot vote on your own entry';
    if (isActive) return `Remove ${action.toLowerCase()}`;
    return action;
  };

  const voteClass = (isUpvote: boolean, isActive: boolean) => {
    const upClasses = isActive
      ? 'bg-green-100 text-green-700 hover:bg-green-200'
      : 'text-secondary-400 hover:scale-110 hover:bg-green-50 hover:text-green-600';
    const downClasses = isActive
      ? 'bg-red-100 text-red-700 hover:bg-red-200'
      : 'text-secondary-400 hover:scale-110 hover:bg-red-50 hover:text-red-500';
    return `cursor-pointer rounded p-1 transition-all duration-100 disabled:cursor-not-allowed ${isUpvote ? upClasses : downClasses} active:scale-90`;
  };

  const upBtn = (
    <VoteBtn
      isUpvote
      onClick={handleVote(true)}
      disabled={isOwnEntry}
      title={voteTitle(true, entry.currentUserVote === true)}
      className={voteClass(true, entry.currentUserVote === true)}
    />
  );

  const downBtn = (
    <VoteBtn
      isUpvote={false}
      onClick={handleVote(false)}
      disabled={isOwnEntry}
      title={voteTitle(false, entry.currentUserVote === false)}
      className={voteClass(false, entry.currentUserVote === false)}
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
