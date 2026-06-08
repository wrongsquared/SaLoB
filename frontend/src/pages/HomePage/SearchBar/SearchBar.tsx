import { useEffect, useRef, useState } from 'react';
import { useEaterySearchInput } from '@/shared/api/queries';
import { useMapStore } from '@/stores/mapStore';
import SearchResultsDropdown from './SearchResultsDropdown';

export default function SearchBar() {
  const [open, setOpen] = useState(false);
  const { input, setInput, items, isLoading, creatingName, selectItem } = useEaterySearchInput();
  const ref = useRef<HTMLDivElement>(null);

  const selectEatery = useMapStore((s) => s.selectEatery);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleSelectItem = async (item: (typeof items)[number]) => {
    const success = await selectItem(item, (eateryId) => {
      selectEatery(eateryId);
    });
    if (success) {
      setInput('');
      setOpen(false);
    }
  };

  return (
    <div ref={ref} className="relative w-full max-w-md">
      <input
        type="text"
        value={input}
        onChange={(e) => {
          setInput(e.target.value);
          if (!open) setOpen(true);
        }}
        onFocus={() => {
          if (input && items.length > 0) setOpen(true);
        }}
        placeholder="Search eateries..."
        className="w-full rounded-lg border border-white/30 bg-white/70 px-4 py-2.5 pl-11 text-base text-secondary-900 backdrop-blur-sm placeholder-secondary-400 outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
      />
      <svg
        className="absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-secondary-400"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        strokeWidth={2}
      >
        <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35M11 19a8 8 0 100-16 8 8 0 000 16z" />
      </svg>

      <SearchResultsDropdown
        open={open}
        isLoading={isLoading}
        items={items}
        isItemDisabled={(item) => creatingName === item.name}
        onSelectItem={handleSelectItem}
      />
    </div>
  );
}
