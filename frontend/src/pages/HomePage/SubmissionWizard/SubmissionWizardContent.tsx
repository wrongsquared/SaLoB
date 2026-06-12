import { DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogClose } from '@/components/ui/dialog';
import { useSubmitFoodEntry } from '@/shared/api/queries';
import { ShieldCheck, Building2, CircleCheck } from 'lucide-react';
import { useState, useCallback } from 'react';
import { useMapStore } from '@/stores/mapStore';
import SearchBar from '../SearchBar/SearchBar';
import FoodSearchInput from './FoodSearchInput';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export default function SubmissionWizardContent() {
  const { setWizardOpen, wizardPreselectedEateryId } = useMapStore();
  const [eateryId, setEateryId] = useState<string | null>(wizardPreselectedEateryId);
  const [foodId, setFoodId] = useState<string | null>(null);
  const [selectedFood, setSelectedFood] = useState('');
  const [priceCents, setPriceCents] = useState('');
  const [showSuccess, setShowSuccess] = useState(false);

  const submitMutation = useSubmitFoodEntry();

  const handleClose = useCallback(() => {
    setWizardOpen(false, null);
  }, [setWizardOpen]);

  const submitting = submitMutation.isPending;
  const canSubmit = eateryId && foodId && priceCents && !submitting;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;

    const price = Math.round(parseFloat(priceCents) * 100);
    if (isNaN(price) || price <= 0) return;

    await submitMutation.mutateAsync({
      eateryId,
      foodId,
      priceSgCents: price,
    });

    setShowSuccess(true);
    setTimeout(() => handleClose(), 1800);
  };

  return (
    <DialogContent className="sm:max-w-md bg-secondary-50" showCloseButton={false}>
      {showSuccess && (
        <div className="absolute inset-0 z-50 flex flex-col items-center justify-center rounded-xl bg-white/95">
          <CircleCheck className="h-16 w-16 text-primary-500 animate-in zoom-in-50 fade-in duration-300" />
          <p className="mt-4 text-base font-semibold text-primary-700 animate-in fade-in slide-in-from-bottom-2 duration-500 delay-150">
            Price Submitted!
          </p>
        </div>
      )}

      <DialogHeader className="flex flex-row items-center gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-100">
          <ShieldCheck className="h-5 w-5 text-primary-600" />
        </div>
        <DialogTitle className="flex-1 text-lg">Submit Price Intelligence</DialogTitle>
        <DialogClose asChild>
          <Button variant="ghost" size="icon-sm" className="text-secondary-400 hover:text-secondary-600">
            <span className="sr-only">Close</span>
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </Button>
        </DialogClose>
      </DialogHeader>

      <form onSubmit={handleSubmit} className="flex flex-col gap-5">
        <div className="flex flex-col gap-2">
          <Label className="text-xs font-semibold tracking-wide text-primary-500 uppercase">Find Store</Label>
          <SearchBar
            placeholder="Search for hawker centre or market..."
            inputClassName="w-full rounded-xl border border-secondary-200 bg-white px-4 py-3 pl-11 text-sm text-secondary-900 placeholder-secondary-400 outline-none focus:border-primary-400 focus:ring-1 focus:ring-primary-400"
            icon={<Building2 className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-secondary-400" />}
            onSelect={(id) => setEateryId(id)}
          />
          {eateryId && <p className="text-xs text-primary-600">Selected eatery</p>}
        </div>

        <div className="grid grid-cols-5 gap-3">
          <div className="col-span-3">
            <Label className="mb-1.5 block text-xs font-semibold tracking-wide text-secondary-500 uppercase">
              Food
            </Label>
            <FoodSearchInput
              onSelect={(id, name) => {
                setFoodId(id);
                setSelectedFood(name);
              }}
            />
            {selectedFood && <p className="mt-1 text-xs text-primary-600">{selectedFood}</p>}
          </div>

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

        <DialogFooter className="mt-2">
          <Button type="button" variant="ghost" onClick={handleClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            disabled={!canSubmit}
            className="bg-primary-600 px-6 text-white hover:bg-primary-700 disabled:opacity-50"
          >
            {submitting ? 'Submitting...' : 'Submit Entry'}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  );
}
