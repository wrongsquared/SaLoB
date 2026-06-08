import { useEaterySearchInput } from '@/shared/api/queries';
import SearchResultItem from '../SearchBar/SearchResultItem';

interface StepEateryProps {
  onSelect: (id: string, name: string) => void;
}

export default function StepEatery({ onSelect }: StepEateryProps) {
  const { input, setInput, items, isLoading, creatingName, selectItem } = useEaterySearchInput();
  const hasResults = items.length > 0;

  const handleSelectItem = async (item: (typeof items)[number]) => {
    const success = await selectItem(item, (eateryId) => {
      onSelect(eateryId, item.name);
    });
    if (success) {
      setInput('');
    }
  };

  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold text-secondary-900">Select an eatery</h2>
      <input
        type="text"
        value={input}
        onChange={(e) => setInput(e.target.value)}
        placeholder="Search eateries..."
        className="w-full rounded-lg border border-secondary-200 px-4 py-2.5 text-sm text-secondary-900 outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
        autoFocus
      />
      <div className="max-h-60 space-y-1 overflow-y-auto">
        {isLoading && <p className="py-4 text-center text-sm text-secondary-400">Searching...</p>}

        {!isLoading && hasResults && (
          <div>
            {items.map((item, i) => (
              <SearchResultItem
                key={`${item.kind}-${item.name}-${i}`}
                item={item}
                disabled={creatingName === item.name}
                onSelect={() => handleSelectItem(item)}
              />
            ))}
          </div>
        )}

        {!isLoading && input && !hasResults && (
          <p className="py-4 text-center text-sm text-secondary-400">No eateries found. Try a different search.</p>
        )}
      </div>
    </div>
  );
}
