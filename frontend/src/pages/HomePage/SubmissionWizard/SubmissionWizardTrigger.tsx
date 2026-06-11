import { DialogTrigger } from '@/components/ui/dialog';
import { useMapStore } from '@/stores/mapStore';
import { Plus } from 'lucide-react';

const SubmissionWizardTrigger = () => {
  const { setWizardOpen } = useMapStore();

  return (
    <DialogTrigger>
      <button
        type="button"
        onClick={() => setWizardOpen(true)}
        className="absolute bottom-6 right-6 z-20 flex h-14 w-14 items-center justify-center rounded-full bg-primary-700 text-white shadow-lg transition-transform hover:scale-105 hover:bg-primary-600"
        aria-label="Submit price"
      >
        <Plus size={24} />
      </button>
    </DialogTrigger>
  );
};

export default SubmissionWizardTrigger;
