import { create } from 'zustand';

interface AuthPromptStore {
  isOpen: boolean;
  open: () => void;
  close: () => void;
}

export const useAuthPromptStore = create<AuthPromptStore>((set) => ({
  isOpen: false,
  open: () => set({ isOpen: true }),
  close: () => set({ isOpen: false }),
}));
