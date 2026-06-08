import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useEaterySearchCombined, useCreateEatery } from '@/shared/api/queries';
import { apiClient } from '@/shared/api/client';
import { useDebounce } from '@/shared/hooks/useDebounce';

interface StepEateryProps {
  onSelect: (id: string, name: string) => void;
}

interface EateryType {
  id: string;
  label: string;
}

export default function StepEatery({ onSelect }: StepEateryProps) {
  const [input, setInput] = useState('');
  const debounced = useDebounce(input, 300);
  const { data: results, isLoading } = useEaterySearchCombined(debounced);
  const createEatery = useCreateEatery();
  const [creating, setCreating] = useState<string | null>(null);

  const { data: eateryTypes } = useQuery({
    queryKey: ['eatery-types'],
    queryFn: async () => {
      const { data } = await apiClient.get<EateryType[]>('/eatery-types');
      return data;
    },
    staleTime: 300_000,
  });

  const defaultTypeId = eateryTypes?.[0]?.id;

  const handleSelect = (id: string, name: string) => {
    onSelect(id, name);
    setInput('');
  };

  const handleCreateFromOneMap = async (item: { name: string; address: string }) => {
    if (!defaultTypeId) return;
    setCreating(item.name);
    try {
      const eatery = await createEatery.mutateAsync({
        name: item.name,
        address: item.address,
        typeId: defaultTypeId,
      });
      handleSelect(eatery.eateryId, eatery.name);
    } catch {
      setCreating(null);
    }
  };

  const hasLocal = results?.local && results.local.length > 0;
  const hasOneMap = results?.onemap && results.onemap.length > 0;

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

        {!isLoading && hasLocal && (
          <div>
            {hasOneMap && (
              <p className="px-3 py-1 text-xs font-semibold uppercase tracking-wider text-secondary-400">Existing</p>
            )}
            {results!.local.map((r) => (
              <button
                key={r.eateryId}
                type="button"
                onClick={() => handleSelect(r.eateryId, r.name)}
                className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left hover:bg-primary-50"
              >
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-secondary-100 text-sm font-bold text-secondary-500">
                  {r.name.charAt(0)}
                </div>
                <div>
                  <p className="text-sm font-medium text-secondary-900">{r.name}</p>
                  {r.address && <p className="text-xs text-secondary-400">{r.address}</p>}
                </div>
              </button>
            ))}
          </div>
        )}

        {!isLoading && hasOneMap && (
          <div>
            {hasLocal && (
              <p className="border-t border-secondary-100 px-3 py-1 pt-3 text-xs font-semibold uppercase tracking-wider text-secondary-400">
                OneMap Results
              </p>
            )}
            {results!.onemap.map((r, i) => (
              <button
                key={`onemap-${i}`}
                type="button"
                onClick={() => handleCreateFromOneMap(r)}
                disabled={creating === r.name || !defaultTypeId}
                className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left hover:bg-primary-50 disabled:opacity-50"
              >
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-amber-100 text-sm font-bold text-amber-600">
                  {r.name.charAt(0)}
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-secondary-900">{r.name}</p>
                  <p className="text-xs text-secondary-400">{r.address}</p>
                </div>
                <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700">New</span>
              </button>
            ))}
          </div>
        )}

        {!isLoading && input && !hasLocal && !hasOneMap && (
          <p className="py-4 text-center text-sm text-secondary-400">No eateries found. Try a different search.</p>
        )}
      </div>
    </div>
  );
}
