import type { SearchFlattenedItem } from '@/shared/api/queries';
import SearchResultItem from './SearchResultItem';

interface SearchResultsDropdownProps {
  open: boolean;
  isLoading: boolean;
  items: SearchFlattenedItem[];
  emptyMessage?: string;
  isItemDisabled?: (item: SearchFlattenedItem) => boolean;
  onSelectItem: (item: SearchFlattenedItem) => void;
}

export default function SearchResultsDropdown({
  open,
  isLoading,
  items,
  emptyMessage = 'No eateries found.',
  isItemDisabled,
  onSelectItem,
}: SearchResultsDropdownProps) {
  if (!open) return null;

  return (
    <div className="absolute left-0 right-0 top-full z-30 mt-1 max-h-72 overflow-y-auto rounded-xl border border-secondary-200 bg-white shadow-lg">
      {isLoading && <p className="py-4 text-center text-sm text-secondary-400">Searching...</p>}

      {!isLoading && items.length > 0 && (
        <div>
          {items.map((item, i) => (
            <SearchResultItem
              key={`${item.kind}-${item.name}-${i}`}
              item={item}
              disabled={isItemDisabled?.(item)}
              onSelect={() => onSelectItem(item)}
            />
          ))}
        </div>
      )}

      {!isLoading && items.length === 0 && (
        <p className="py-4 text-center text-sm text-secondary-400">{emptyMessage}</p>
      )}
    </div>
  );
}
