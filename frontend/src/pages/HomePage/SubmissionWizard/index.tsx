import { useMapStore } from '@/stores/mapStore';
import { useAuthGuard } from '@/shared/hooks/useAuthGuard';
import { Plus } from 'lucide-react';
import SubmissionWizardContent from './SubmissionWizardContent';
import { Dialog } from '@/components/ui/dialog';

export default function SubmissionWizard() {
  const wizardOpen = useMapStore((s) => s.wizardOpen);
  const setWizardOpen = useMapStore((s) => s.setWizardOpen);
  const guard = useAuthGuard();

  return (
    <Dialog open={wizardOpen} onOpenChange={(open) => setWizardOpen(open, null)}>
      {!wizardOpen && (
        <button
          type="button"
          onClick={() => guard(() => setWizardOpen(true))}
          className="absolute bottom-6 right-6 z-20 flex h-14 w-14 items-center justify-center rounded-full bg-primary-700 text-white shadow-lg transition-transform hover:scale-105 hover:bg-primary-600"
          aria-label="Submit price"
        >
          <Plus size={24} />
        </button>
      )}
      {wizardOpen && <SubmissionWizardContent />}
    </Dialog>
  );
}
