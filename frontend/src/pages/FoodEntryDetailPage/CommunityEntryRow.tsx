import { useNavigate } from 'react-router-dom';
import { centsToSgd } from '@/shared/utils';
import type { FoodPreview } from '@/shared/types/api';
import VoteButtonGroup from '@/components/VoteButtonGroup';

interface CommunityEntryRowProps {
  entry: FoodPreview;
  isSelected: boolean;
}

export default function CommunityEntryRow({ entry, isSelected }: CommunityEntryRowProps) {
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
      className={`flex cursor-pointer items-center gap-3 px-4 py-3 transition-colors ${
        isSelected
          ? 'bg-primary-50/50 border-l-2 border-l-primary-700'
          : 'hover:bg-secondary-50 border-l-2 border-l-transparent'
      }`}
    >
      <div className="flex h-10 w-10 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-secondary-100">
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
          <span className="text-sm font-bold text-secondary-400">{entry.name.charAt(0)}</span>
        )}
      </div>

      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-secondary-900">{isSelected ? 'Entry selected' : entry.name}</p>
        <p className="text-xs text-secondary-400">
          Added by {entry.submitterUsername} &middot;{' '}
          {new Date(entry.createdAt).toLocaleDateString('en-SG', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </p>
      </div>

      <p className="text-sm font-bold text-secondary-900">{centsToSgd(entry.sgCents)}</p>

      <VoteButtonGroup entry={entry} compact />
    </div>
  );
}
