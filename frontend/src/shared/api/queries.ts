import { useCallback, useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import { useAuthStore } from '@/stores/authStore';
import { useDebounce } from '@/shared/hooks/useDebounce';
import type {
  EateryMapItem,
  EaterySearchCombinedResult,
  FoodEntryMapItem,
  Bounds,
  EateryDetail,
  EaterySearchResult,
  FoodSearchResult,
  FoodEntryDetail,
  FoodHistoricalData,
  FoodCreationRequest,
  FoodEntrySubmissionRequest,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  User,
} from '@/shared/types/api';

// ── Query keys ─────────────────────────────────────────────
// Use `QK.*` (uppercase) for invalidation (prefix match).
// Use `qk.*` (lowercase) for query key construction (full key with params).

function roundBounds(b: Bounds | null): Bounds | null {
  if (!b) return null;
  return {
    minLat: Math.round(b.minLat * 100) / 100,
    maxLat: Math.round(b.maxLat * 100) / 100,
    minLon: Math.round(b.minLon * 100) / 100,
    maxLon: Math.round(b.maxLon * 100) / 100,
  };
}

export const QK = {
  EATERIES_DETAIL: ['eateries', 'detail'],
  EATERIES_WITHIN_BOUNDS: ['eateries', 'within-bounds'],
  EATERIES_SEARCH: ['eateries', 'search'],
  EATERIES_SEARCH_COMBINED: ['eateries', 'search', 'combined'],
  FOODS_SEARCH: ['foods', 'search'],
  FOOD_ENTRIES_DETAIL: ['food-entries', 'detail'],
  FOOD_ENTRIES_WITHIN_BOUNDS: ['food-entries', 'within-bounds'],
  FOOD_ENTRIES_HISTORICAL: ['food-entries', 'historical'],
  AUTH_ME: ['auth', 'me'],
} as const;

function userSuffix(): string | null {
  return useAuthStore.getState().user?.id ?? null;
}

export const qk = {
  eateryWithinBounds: (b: Bounds | null) => [...QK.EATERIES_WITHIN_BOUNDS, roundBounds(b)] as const,
  eateryDetail: (id: string | null) => [...QK.EATERIES_DETAIL, id, userSuffix()] as const,
  eaterySearch: (q: string) => [...QK.EATERIES_SEARCH, q] as const,
  eaterySearchCombined: (q: string) => [...QK.EATERIES_SEARCH_COMBINED, q] as const,
  eateryAllDetails: (b: Bounds | null) => ['eateries', 'all-details', roundBounds(b)] as const,
  foodSearch: (q: string) => [...QK.FOODS_SEARCH, q] as const,
  foodEntryWithinBounds: (b: Bounds | null) => [...QK.FOOD_ENTRIES_WITHIN_BOUNDS, roundBounds(b)] as const,
  foodEntryDetail: (id: string | null) => [...QK.FOOD_ENTRIES_DETAIL, id] as const,
  foodEntryHistorical: (id: string | null) => [...QK.FOOD_ENTRIES_HISTORICAL, id, userSuffix()] as const,
};

// ── Eateries ────────────────────────────────────────────────
export function useEateriesWithinBounds(bounds: Bounds | null) {
  return useQuery({
    queryKey: qk.eateryWithinBounds(bounds),
    queryFn: async () => {
      if (!bounds) return [];
      const { data } = await apiClient.get<EateryMapItem[]>('/eateries/within-bounds', {
        params: bounds,
      });
      return Array.isArray(data) ? data : [];
    },
    enabled: !!bounds,
    staleTime: 30_000,
    placeholderData: keepPreviousData,
  });
}

export function useEateryDetail(eateryId: string | null) {
  return useQuery({
    queryKey: qk.eateryDetail(eateryId),
    queryFn: async () => {
      const { data } = await apiClient.get<EateryDetail>(`/eateries/${eateryId}`);
      return data;
    },
    enabled: !!eateryId,
    staleTime: 60_000,
  });
}

export function useEaterySearch(searchQuery: string) {
  return useQuery({
    queryKey: qk.eaterySearch(searchQuery),
    queryFn: async () => {
      if (!searchQuery.trim()) return [];
      const { data } = await apiClient.get<EaterySearchResult[]>('/eateries/search', {
        params: { search: searchQuery },
      });
      return data;
    },
    enabled: searchQuery.trim().length > 0,
    staleTime: 30_000,
  });
}

// ── Foods ───────────────────────────────────────────────────
export function useFoodSearch(searchQuery: string) {
  return useQuery({
    queryKey: qk.foodSearch(searchQuery),
    queryFn: async () => {
      if (!searchQuery.trim()) return [];
      const { data } = await apiClient.get<FoodSearchResult[]>('/foods/search', {
        params: { search: searchQuery },
      });
      return data;
    },
    enabled: searchQuery.trim().length > 0,
    staleTime: 30_000,
  });
}

// ── Food Entries ────────────────────────────────────────────
export function useFoodEntryDetail(foodEntryId: string | null) {
  return useQuery({
    queryKey: qk.foodEntryDetail(foodEntryId),
    queryFn: async () => {
      const { data } = await apiClient.get<FoodEntryDetail>(`/food-entries/${foodEntryId}/details`);
      return data;
    },
    enabled: !!foodEntryId,
    staleTime: 60_000,
  });
}

export function useFoodEntriesWithinBounds(bounds: Bounds | null) {
  return useQuery({
    queryKey: qk.foodEntryWithinBounds(bounds),
    queryFn: async () => {
      if (!bounds) return [];
      const { data } = await apiClient.get<FoodEntryMapItem[]>('/food-entries/within-bounds', {
        params: bounds,
      });
      return Array.isArray(data) ? data : [];
    },
    enabled: !!bounds,
    staleTime: 30_000,
    placeholderData: keepPreviousData,
  });
}

export function useFoodHistoricalData(foodEntryId: string | null) {
  return useQuery({
    queryKey: qk.foodEntryHistorical(foodEntryId),
    queryFn: async () => {
      const startDate = new Date(Date.now() - 90 * 24 * 60 * 60 * 1000).toISOString();
      const { data } = await apiClient.get<FoodHistoricalData>(`/food-entries/historical-data/${foodEntryId}`, {
        params: { startDate },
      });
      return data;
    },
    enabled: !!foodEntryId,
    staleTime: 120_000,
  });
}

// ── Mutations ───────────────────────────────────────────────
// Convention: mutationFn = pure API calls (may throw), onSuccess = side effects only.
// Components handle error display via mutation.error or try/catch on mutateAsync.

export function useSubmitFoodEntry() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: FoodEntrySubmissionRequest) => {
      await apiClient.post('/food-entries/submit', data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QK.EATERIES_DETAIL });
      queryClient.invalidateQueries({ queryKey: QK.EATERIES_WITHIN_BOUNDS });
      queryClient.invalidateQueries({ queryKey: ['food-entries'] });
    },
  });
}

export function useCreateFood() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: FoodCreationRequest) => {
      const { data: result } = await apiClient.post<FoodSearchResult>('/foods', data);
      return result;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QK.FOODS_SEARCH });
    },
  });
}

// ── Auth ────────────────────────────────────────────────────
// Each mutation chains sequential API calls in mutationFn.
// The token is saved immediately so the subsequent /users/me call
// includes the Bearer header (via the axios request interceptor).
// If any step throws, no partial state leaks — onSuccess is skipped.

export function useLoginMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (req: LoginRequest) => {
      const { data } = await apiClient.post<LoginResponse>('/auth/login', req);
      useAuthStore.getState().login(data.jwt);
      const { data: user } = await apiClient.get<User>('/users/me');
      useAuthStore.getState().setUser(user);
      return user;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QK.AUTH_ME });
    },
  });
}

export function useGoogleLoginMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (idToken: string) => {
      const { data } = await apiClient.post<LoginResponse>('/auth/google', { idToken });
      useAuthStore.getState().login(data.jwt);
      const { data: user } = await apiClient.get<User>('/users/me');
      useAuthStore.getState().setUser(user);
      return user;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QK.AUTH_ME });
    },
  });
}

export function useRegisterMutation() {
  return useMutation({
    mutationFn: async (req: RegisterRequest) => {
      await apiClient.post('/auth/register', req);
      const { data } = await apiClient.post<LoginResponse>('/auth/login', {
        usernameOrEmail: req.email,
        password: req.password,
      });
      useAuthStore.getState().login(data.jwt);
      const { data: user } = await apiClient.get<User>('/users/me');
      useAuthStore.getState().setUser(user);
      return user;
    },
  });
}

export function useEaterySearchCombined(searchQuery: string) {
  return useQuery({
    queryKey: qk.eaterySearchCombined(searchQuery),
    queryFn: async () => {
      const { data } = await apiClient.get<EaterySearchCombinedResult>('/eateries/search/combined', {
        params: { search: searchQuery },
      });
      return data;
    },
    enabled: searchQuery.trim().length > 0,
    staleTime: 30_000,
  });
}

export type SearchFlattenedItem =
  | { kind: 'local'; eateryId: string; name: string; address: string }
  | { kind: 'onemap'; name: string; address: string; latitude: number; longitude: number };

export function flattenCombinedResults(results: EaterySearchCombinedResult | undefined): SearchFlattenedItem[] {
  if (!results) return [];
  return [
    ...results.local.map((r) => ({ kind: 'local' as const, ...r })),
    ...results.onemap.map((r) => ({ kind: 'onemap' as const, ...r })),
  ];
}

interface EateryTypeResponse {
  id: string;
  label: string;
}

export function useEateryTypes() {
  return useQuery({
    queryKey: ['eatery-types'],
    queryFn: async () => {
      const { data } = await apiClient.get<EateryTypeResponse[]>('/eatery-types');
      return data;
    },
    staleTime: 300_000,
  });
}

export function useDefaultEateryTypeId() {
  const { data: eateryTypes } = useEateryTypes();
  return eateryTypes?.[0]?.id ?? null;
}

export function useEaterySelection() {
  const createEatery = useCreateEatery();
  const defaultTypeId = useDefaultEateryTypeId();
  const [creatingName, setCreatingName] = useState<string | null>(null);

  const selectItem = useCallback(
    async (item: SearchFlattenedItem, onSelected: (eateryId: string) => void) => {
      if (item.kind === 'local') {
        onSelected(item.eateryId);
        return true;
      }

      if (!defaultTypeId) return false;
      setCreatingName(item.name);
      try {
        const eatery = await createEatery.mutateAsync({
          name: item.name,
          address: item.address,
          typeId: defaultTypeId,
        });
        onSelected(eatery.eateryId);
        return true;
      } catch {
        setCreatingName(null);
        return false;
      }
    },
    [createEatery, defaultTypeId],
  );

  return { creatingName, selectItem };
}

export function useEaterySearchInput() {
  const [input, setInput] = useState('');
  const debounced = useDebounce(input, 300);
  const { data: results, isLoading } = useEaterySearchCombined(debounced);
  const { creatingName, selectItem } = useEaterySelection();
  const items = flattenCombinedResults(results);
  return { input, setInput, items, isLoading, creatingName, selectItem };
}

export function useCreateEatery() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { name: string; address: string; typeId: string }) => {
      const { data: result } = await apiClient.post<EaterySearchResult>('/eateries', data);
      return result;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QK.EATERIES_SEARCH });
      queryClient.invalidateQueries({ queryKey: QK.EATERIES_SEARCH_COMBINED });
    },
  });
}

export function useVote() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ foodEntryId, isUpvote }: { foodEntryId: string; isUpvote: boolean | null }) => {
      await apiClient.post(`/food-entries/${foodEntryId}/vote`, { isUpvote });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QK.EATERIES_DETAIL });
      queryClient.invalidateQueries({ queryKey: QK.FOOD_ENTRIES_HISTORICAL });
    },
  });
}

export function useReportEateryClosed() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (eateryId: string) => {
      await apiClient.post(`/eateries/${eateryId}/report-closed`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QK.EATERIES_DETAIL });
    },
  });
}
