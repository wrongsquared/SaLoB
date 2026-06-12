import { useNavigate } from 'react-router-dom';
import { useAuthPromptStore } from '@/stores/authPromptStore';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { LogIn } from 'lucide-react';

export default function AuthPrompt() {
  const { isOpen, close } = useAuthPromptStore();
  const navigate = useNavigate();

  return (
    <Dialog open={isOpen} onOpenChange={close}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Login Required</DialogTitle>
          <DialogDescription>
            You need to be logged in to perform this action. Please log in or create an account to continue.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="ghost" onClick={close}>
            Cancel
          </Button>
          <Button
            onClick={() => {
              close();
              navigate('/login');
            }}
          >
            <LogIn className="mr-2 h-4 w-4" />
            Login
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
