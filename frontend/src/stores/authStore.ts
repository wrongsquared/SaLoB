import { create } from 'zustand'
import type { User } from '@/shared/types/api'

interface AuthStore {
  token: string | null
  user: User | null
  isAuthenticated: boolean
  login: (token: string) => void
  logout: () => void
  setUser: (user: User | null) => void
}

export const useAuthStore = create<AuthStore>((set) => ({
  token: localStorage.getItem('auth_token'),
  user: null,
  isAuthenticated: !!localStorage.getItem('auth_token'),
  login: (token: string) => {
    localStorage.setItem('auth_token', token)
    set({ token, isAuthenticated: true })
  },
  logout: () => {
    localStorage.removeItem('auth_token')
    set({ token: null, user: null, isAuthenticated: false })
  },
  setUser: (user) => set({ user }),
}))
