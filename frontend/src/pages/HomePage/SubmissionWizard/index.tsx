import SubmissionWizardTrigger from './SubmissionWizardTrigger';
import SubmissionWizardContent from './SubmissionWizardContent';
import { Dialog } from '@/components/ui/dialog';

const SubmissionWizard = () => {
  return (
    <Dialog>
      <SubmissionWizardTrigger />
      <SubmissionWizardContent />
    </Dialog>
  );
};

export default SubmissionWizard;
