import { useEffect, useRef, useState, type ReactNode } from 'react';
import SearchResultsDropdown from './SearchResultsDropdown';
import { useEaterySearchInput } from '@/shared/api/queries';
import { useMapStore } from '@/stores/mapStore';

interface SearchBarProps {
  // For whatever reason, if you want to display some text in the search bar
  customDisplayValue?: string;

  placeholder?: string;
  inputClassName?: string;
  icon?: ReactNode;
  onSelect?: (eateryId: string) => void;
}

export default function SearchBar({
  placeholder = 'Search eateries...',
  inputClassName,
  icon,
  onSelect,
}: SearchBarProps) {
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
      if (onSelect) {
        onSelect(eateryId);
      } else {
        selectEatery(eateryId);
      }
    });
    if (success) {
      setInput(item.name);
      setOpen(false);
    }
  };

  const defaultIcon = (
    <svg
      className="absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-secondary-400"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      strokeWidth={2}
    >
      <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35M11 19a8 8 0 100-16 8 8 0 000 16z" />
    </svg>
  );

  return (
    <div ref={ref} className="relative w-full">
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
        placeholder={placeholder}
        className={
          inputClassName ??
          `w-full rounded-lg border border-white/30 bg-white/70 px-4 py-2.5 pl-11 text-base text-secondary-900
          backdrop-blur-sm placeholder-secondary-400 outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500`
        }
      />
      {icon ?? defaultIcon}

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
