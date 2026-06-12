import { useAuthStore } from '@/stores/authStore';
import { useAuthPromptStore } from '@/stores/authPromptStore';

export function useAuthGuard() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const openAuthPrompt = useAuthPromptStore((s) => s.open);

  return (action: () => void) => {
    if (!isAuthenticated) {
      openAuthPrompt();
      return;
    }
    action();
  };
}
