import { useState, useRef, useEffect } from 'react';
import { useDebounce } from '@/shared/hooks/useDebounce';
import { useFoodSearch } from '@/shared/api/queries';
import { Input } from '@/components/ui/input';
import { UtensilsCrossed } from 'lucide-react';

interface FoodSearchInputProps {
  onSelect: (foodId: string, foodName: string) => void;
  placeholder?: string;
}

export default function FoodSearchInput({ onSelect, placeholder = 'e.g. Chicken Rice' }: FoodSearchInputProps) {
  const [input, setInput] = useState('');
  const [open, setOpen] = useState(false);
  const debounced = useDebounce(input, 300);
  const { data, isLoading } = useFoodSearch(debounced);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const suggestions = data?.filter((r) => r.foodName.toLowerCase().includes(input.toLowerCase())) ?? [];

  return (
    <div ref={ref} className="relative">
      <UtensilsCrossed className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-secondary-400" />
      <Input
        value={input}
        onChange={(e) => {
          setInput(e.target.value);
          if (e.target.value) setOpen(true);
        }}
        onFocus={() => {
          if (input && suggestions.length > 0) setOpen(true);
        }}
        placeholder={placeholder}
        className="h-10 border-secondary-200 bg-white pl-9 text-sm"
      />
      {isLoading && open && <div className="absolute right-3 top-1/2 -translate-y-1/2 text-secondary-400">...</div>}
      {open && input && (
        <div className="absolute z-20 mt-1 max-h-48 w-full overflow-auto rounded-lg border border-secondary-200 bg-white shadow-lg">
          {isLoading && <p className="py-3 text-center text-xs text-secondary-400">Searching...</p>}
          {!isLoading && suggestions.length === 0 && (
            <p className="py-3 text-center text-xs text-secondary-400">No foods found</p>
          )}
          {!isLoading &&
            suggestions.map((f) => (
              <button
                key={f.foodId}
                type="button"
                onClick={() => {
                  onSelect(f.foodId, f.foodName);
                  setInput(f.foodName);
                  setOpen(false);
                }}
                className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-primary-50"
              >
                <span className="font-medium text-secondary-900">{f.foodName}</span>
              </button>
            ))}
        </div>
      )}
    </div>
  );
}
