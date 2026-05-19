import { useQuery } from '@tanstack/react-query'
import { apiClient } from './client'
import type {
  EateryMapItem,
  Bounds,
  EateryDetail,
  EaterySearchResult,
  FoodSearchResult,
  FoodEntryDetail,
  FoodHistoricalData,
} from '@/shared/types/api'

export function useEateriesWithinBounds(bounds: Bounds | null) {
  return useQuery({
    queryKey: ['eateries', 'within-bounds', bounds],
    queryFn: async () => {
      if (!bounds) return []
      const { data } = await apiClient.get<EateryMapItem[]>('/eateries/within-bounds', {
        params: bounds,
      })
      return data
    },
    enabled: !!bounds,
    staleTime: 30_000,
  })
}

export function useEateryDetail(eateryId: string | null) {
  return useQuery({
    queryKey: ['eateries', 'detail', eateryId],
    queryFn: async () => {
      const { data } = await apiClient.get<EateryDetail>(`/eateries/${eateryId}`)
      return data
    },
    enabled: !!eateryId,
    staleTime: 60_000,
  })
}

export function useEaterySearch(searchQuery: string) {
  return useQuery({
    queryKey: ['eateries', 'search', searchQuery],
    queryFn: async () => {
      if (!searchQuery.trim()) return []
      const { data } = await apiClient.get<EaterySearchResult[]>('/eateries/search', {
        params: { search: searchQuery },
      })
      return data
    },
    enabled: searchQuery.trim().length > 0,
    staleTime: 30_000,
  })
}

export function useFoodSearch(searchQuery: string) {
  return useQuery({
    queryKey: ['foods', 'search', searchQuery],
    queryFn: async () => {
      if (!searchQuery.trim()) return []
      const { data } = await apiClient.get<FoodSearchResult[]>('/foods/search', {
        params: { search: searchQuery },
      })
      return data
    },
    enabled: searchQuery.trim().length > 0,
    staleTime: 30_000,
  })
}

export function useAllEateryDetails(bounds: Bounds | null) {
  const { data: eateries } = useEateriesWithinBounds(bounds)

  return useQuery({
    queryKey: ['eateries', 'all-details', bounds],
    queryFn: async () => {
      if (!eateries || eateries.length === 0) return []
      const results = await Promise.all(
        eateries.map((e) =>
          apiClient
            .get<EateryDetail>(`/eateries/${e.eateryId}`)
            .then((res) => res.data),
        ),
      )
      return results
    },
    enabled: !!bounds && !!eateries && eateries.length > 0,
    staleTime: 30_000,
  })
}

export function useFoodEntryDetail(foodEntryId: string | null) {
  return useQuery({
    queryKey: ['food-entries', 'detail', foodEntryId],
    queryFn: async () => {
      const { data } = await apiClient.get<FoodEntryDetail>(
        `/food-entries/${foodEntryId}/details`,
      )
      return data
    },
    enabled: !!foodEntryId,
    staleTime: 60_000,
  })
}

export function useFoodHistoricalData(foodEntryId: string | null) {
  return useQuery({
    queryKey: ['food-entries', 'historical', foodEntryId],
    queryFn: async () => {
      const startDate = new Date(
        Date.now() - 90 * 24 * 60 * 60 * 1000,
      ).toISOString()
      const { data } = await apiClient.get<FoodHistoricalData>(
        `/food-entries/${foodEntryId}/historical-data`,
        { params: { startDate } },
      )
      return data
    },
    enabled: !!foodEntryId,
    staleTime: 120_000,
  })
}
