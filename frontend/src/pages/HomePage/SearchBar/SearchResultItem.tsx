import type { SearchFlattenedItem } from '@/shared/api/queries';

interface SearchResultItemProps {
  item: SearchFlattenedItem;
  disabled?: boolean;
  onSelect: () => void;
}

export default function SearchResultItem({ item, disabled, onSelect }: SearchResultItemProps) {
  return (
    <button
      type="button"
      onClick={onSelect}
      disabled={disabled}
      className="flex w-full items-center gap-3 px-3 py-2.5 text-left hover:bg-primary-50 disabled:opacity-50"
    >
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-secondary-100 text-sm font-bold text-secondary-500">
        {item.name.charAt(0)}
      </div>
      <div className="min-w-0">
        <p className="truncate text-sm font-medium text-secondary-900">{item.name}</p>
        {item.address && <p className="truncate text-xs text-secondary-400">{item.address}</p>}
      </div>
    </button>
  );
}
