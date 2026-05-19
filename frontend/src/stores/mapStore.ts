import { create } from 'zustand'
import type { Bounds } from '@/shared/types/api'

interface MapStore {
  mode: 'eatery' | 'food'
  selectedEateryId: string | null
  sidebarOpen: boolean
  selectedFoods: string[]
  searchQuery: string
  mapCenter: [number, number]
  mapZoom: number
  mapBounds: Bounds | null
  wizardOpen: boolean
  wizardPreselectedEateryId: string | null
  reportedEateryIds: Set<string>

  setMode: (mode: 'eatery' | 'food') => void
  selectEatery: (id: string | null) => void
  setSidebarOpen: (open: boolean) => void
  addFood: (foodName: string) => void
  removeFood: (foodName: string) => void
  clearFoods: () => void
  setSearchQuery: (query: string) => void
  setMapCenter: (center: [number, number]) => void
  setMapZoom: (zoom: number) => void
  setMapBounds: (bounds: Bounds | null) => void
  setWizardOpen: (open: boolean, preselectedEateryId?: string | null) => void
  markEateryReported: (eateryId: string) => void
}

export const useMapStore = create<MapStore>((set) => ({
  mode: 'eatery',
  selectedEateryId: null,
  sidebarOpen: false,
  selectedFoods: [],
  searchQuery: '',
  mapCenter: [1.3521, 103.8198],
  mapZoom: 13,
  mapBounds: null,
  wizardOpen: false,
  wizardPreselectedEateryId: null,
  reportedEateryIds: new Set(),

  setMode: (mode) =>
    set({ mode, selectedEateryId: null, sidebarOpen: false }),
  selectEatery: (id) => set({ selectedEateryId: id, sidebarOpen: id !== null }),
  setSidebarOpen: (open) => set({ sidebarOpen: open }),
  addFood: (foodName) =>
    set((state) => ({
      selectedFoods:
        state.selectedFoods.length < 5 && !state.selectedFoods.includes(foodName)
          ? [...state.selectedFoods, foodName]
          : state.selectedFoods,
    })),
  removeFood: (foodName) =>
    set((state) => ({
      selectedFoods: state.selectedFoods.filter((f) => f !== foodName),
    })),
  clearFoods: () => set({ selectedFoods: [] }),
  setSearchQuery: (query) => set({ searchQuery: query }),
  setMapCenter: (center) => set({ mapCenter: center }),
  setMapZoom: (zoom) => set({ mapZoom: zoom }),
  setMapBounds: (bounds) => set({ mapBounds: bounds }),
  setWizardOpen: (open: boolean, preselectedEateryId?: string | null) =>
    set({ wizardOpen: open, wizardPreselectedEateryId: preselectedEateryId ?? null }),
  markEateryReported: (eateryId) =>
    set((state) => {
      const newSet = new Set(state.reportedEateryIds)
      newSet.add(eateryId)
      return { reportedEateryIds: newSet }
    }),
}))
