export interface EateryMapItem {
  eateryId: string;
  name: string;
  latitude: number;
  longitude: number;
  typeLabel: string;
}

export interface FoodEntryMapItem {
  foodEntryId: string;
  foodName: string;
  sgCents: number;
  eateryId: string;
  eateryName: string;
  latitude: number;
  longitude: number;
}

export interface FoodPreview {
  foodEntryId: string;
  name: string;
  sgCents: number;
  upvotes: number;
  downvotes: number;
  photoPresignedUrl: string | null;
  submitterId: string;
  submitterUsername: string;
  createdAt: string;
  currentUserVote: boolean | null;
}

export interface EateryDetail {
  eateryId: string;
  name: string;
  address: string;
  typeLabel: string;
  photoUrl: string;
  foodPreviews: FoodPreview[];
}

export interface EaterySearchResult {
  eateryId: string;
  name: string;
  address: string;
}

export interface FoodSearchResult {
  foodId: string;
  foodName: string;
  photoUrl: string;
}

export interface FoodEntryDetail {
  foodEntryId: string;
  foodPhotoPresignedUrl: string;
  submittedAt: string;
  submitterId: string;
  submitterUsername: string;
  submitterProfilePhotoPresignedUrl: string;
  submitterWtfScore: number;
  submitterTenureDays: number;
  submitterEntriesSubmitted: number;
}

export interface DatePrice {
  date: string;
  sgCents: number;
  confidence: number;
  entryCount: number;
}

export interface FoodHistoricalData {
  foodName: string;
  sgCentsConsensusPrice: number;
  eateryId: string;
  eateryAddress: string;
  submitterUsername: string;
  datePrices: DatePrice[];
  communityEntries: FoodPreview[];
  consensusEntry: FoodEntryDetail;
}

export interface Bounds {
  minLat: number;
  maxLat: number;
  minLon: number;
  maxLon: number;
}

export interface FoodCreationRequest {
  foodName: string;
}

export interface FoodEntrySubmissionRequest {
  eateryId: string;
  foodId: string;
  priceSgCents: number;
}

// ─── Auth ───────────────────────────────────────────────────
export interface User {
  id: string;
  email: string;
  username: string;
  roles: string[];
  avatarUrl: string | null;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface LoginResponse {
  jwt: string;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
}

export interface GoogleLoginRequest {
  idToken: string;
}

// ─── OneMap ─────────────────────────────────────────────────
export interface OneMapEateryItem {
  name: string;
  address: string;
  latitude: number;
  longitude: number;
}

export interface EaterySearchCombinedResult {
  local: EaterySearchResult[];
  onemap: OneMapEateryItem[];
}
