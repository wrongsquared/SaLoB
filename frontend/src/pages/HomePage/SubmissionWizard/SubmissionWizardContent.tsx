import { DialogContent, DialogFooter, DialogClose, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { useSubmitFoodEntry, useFoodSearch } from '@/shared/api/queries';
import { ShieldCheck, Building2, UtensilsCrossed, CircleCheck } from 'lucide-react';
import { useState, useRef, useEffect, useCallback } from 'react';
import { useDebounce } from '@/shared/hooks/useDebounce';
import { useMapStore } from '@/stores/mapStore';
import { Button } from '@/components/ui/button';
import SearchBar from '../SearchBar/SearchBar';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export default function SubmissionWizardContent() {
  const { wizardOpen, setWizardOpen } = useMapStore();
  const [eateryId, setEateryId] = useState<string | null>(null);
  const [foodInput, setFoodInput] = useState('');
  const [foodId, setFoodId] = useState<string | null>(null);
  const [priceCents, setPriceCents] = useState('');
  const [foodDropdownOpen, setFoodDropdownOpen] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);

  const debouncedFood = useDebounce(foodInput, 300);
  const { data: foodResults, isLoading: foodLoading } = useFoodSearch(debouncedFood);
  const submitMutation = useSubmitFoodEntry();

  const foodRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (foodRef.current && !foodRef.current.contains(e.target as Node)) {
        setFoodDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleClose = useCallback(() => {
    setWizardOpen(false);
    setEateryId(null);
    setFoodInput('');
    setFoodId(null);
    setPriceCents('');
    setFoodDropdownOpen(false);
    setShowSuccess(false);
    submitMutation.reset();
  }, [setWizardOpen, submitMutation]);

  if (!wizardOpen) return null;

  const submitting = submitMutation.isPending;

  const handleFoodSelect = (id: string, name: string) => {
    setFoodId(id);
    setFoodInput(name);
    setFoodDropdownOpen(false);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!eateryId || !foodId || !priceCents) return;

    const price = Math.round(parseFloat(priceCents) * 100);
    if (isNaN(price) || price <= 0) return;

    await submitMutation.mutateAsync({
      eateryId,
      foodId,
      priceSgCents: price,
    });

    setShowSuccess(true);
    setTimeout(() => {
      handleClose();
    }, 1800);
  };

  const foodSuggestions = foodResults?.filter((r) => r.foodName.toLowerCase().includes(foodInput.toLowerCase())) ?? [];

  return (
    <DialogContent className="sm:max-w-md bg-secondary-50" showCloseButton={false}>
      {/* --- SUCCESS OVERLAY --- */}
      {showSuccess && (
        <div className="absolute inset-0 z-50 flex flex-col items-center justify-center rounded-xl bg-white/95">
          <CircleCheck className="h-16 w-16 text-primary-500 animate-in zoom-in-50 fade-in duration-300" />
          <p className="mt-4 text-base font-semibold text-primary-700 animate-in fade-in slide-in-from-bottom-2 duration-500 delay-150">
            Price Submitted!
          </p>
        </div>
      )}

      {/* --- HEADER --- */}
      <DialogHeader className="flex flex-row items-center gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-100">
          <ShieldCheck className="h-5 w-5 text-primary-600" />
        </div>
        <DialogTitle className="flex-1 text-lg">Submit Price Intelligence</DialogTitle>
      </DialogHeader>

      <form onSubmit={handleSubmit} className="flex flex-col gap-5">
        {/* --- EATERY SEARCH --- */}
        <div className="flex flex-col gap-2">
          <Label className="text-xs font-semibold tracking-wide text-primary-500 uppercase">Find Store</Label>
          <div className="relative">
            <SearchBar
              placeholder="Search for hawker centre or market..."
              inputClassName="w-full rounded-xl border border-secondary-200 bg-white px-4 py-3 pl-11 text-sm text-secondary-900 placeholder-secondary-400 outline-none focus:border-primary-400 focus:ring-1 focus:ring-primary-400"
              icon={<Building2 className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-secondary-400" />}
              onSelect={(id) => {
                setEateryId(id);
              }}
            />
          </div>
          {eateryId && <p className="text-xs text-primary-600">Selected eatery ✓</p>}
        </div>

        {/* --- FOOD + PRICE --- */}
        <div className="grid grid-cols-5 gap-3">
          {/* Food search */}
          <div ref={foodRef} className="relative col-span-3">
            <Label className="mb-1.5 block text-xs font-semibold tracking-wide text-secondary-500 uppercase">
              Food
            </Label>
            <div className="relative">
              <UtensilsCrossed className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-secondary-400" />
              <Input
                value={foodInput}
                onChange={(e) => {
                  setFoodInput(e.target.value);
                  setFoodId(null);
                  if (e.target.value) setFoodDropdownOpen(true);
                }}
                onFocus={() => {
                  if (foodInput && foodSuggestions.length > 0) setFoodDropdownOpen(true);
                }}
                placeholder="e.g. Chicken Rice"
                className="h-10 border-secondary-200 bg-white pl-9 text-sm"
              />
              {foodLoading && foodDropdownOpen && (
                <div className="absolute right-3 top-1/2 -translate-y-1/2 text-secondary-400">...</div>
              )}
            </div>
            {foodDropdownOpen && foodInput && !foodId && (
              <div className="absolute z-20 mt-1 max-h-48 w-full overflow-auto rounded-lg border border-secondary-200 bg-white shadow-lg">
                {foodLoading && <p className="py-3 text-center text-xs text-secondary-400">Searching...</p>}
                {!foodLoading && foodSuggestions.length === 0 && (
                  <p className="py-3 text-center text-xs text-secondary-400">No foods found</p>
                )}
                {!foodLoading &&
                  foodSuggestions.map((f) => (
                    <button
                      key={f.foodId}
                      type="button"
                      onClick={() => handleFoodSelect(f.foodId, f.foodName)}
                      className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-primary-50"
                    >
                      <span className="font-medium text-secondary-900">{f.foodName}</span>
                    </button>
                  ))}
              </div>
            )}
          </div>

          {/* Price */}
          <div className="col-span-2">
            <Label className="mb-1.5 block text-xs font-semibold tracking-wide text-secondary-500 uppercase">
              Price
            </Label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-secondary-400">$</span>
              <Input
                type="number"
                step="0.10"
                min="0"
                value={priceCents}
                onChange={(e) => setPriceCents(e.target.value)}
                placeholder="0.00"
                className="h-10 border-secondary-200 bg-white pl-7 text-sm"
              />
            </div>
          </div>
        </div>

        {/* --- FOOTER --- */}
        <DialogFooter className="mt-2">
          <DialogClose asChild>
            <Button type="button" variant="ghost" onClick={handleClose}>
              Cancel
            </Button>
          </DialogClose>
          <Button
            type="submit"
            disabled={!eateryId || !foodId || !priceCents || submitting}
            className="bg-primary-600 px-6 text-white hover:bg-primary-700 disabled:opacity-50"
          >
            {submitting ? 'Submitting...' : 'Submit Entry'}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  );
}
