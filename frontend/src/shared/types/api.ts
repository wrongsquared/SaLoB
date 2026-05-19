export interface EateryMapItem {
  eateryId: string
  name: string
  latitude: number
  longitude: number
  typeLabel: string
}

export interface FoodEntryMapItem {
  foodEntryId: string
  foodName: string
  sgCents: number
  eateryId: string
  eateryName: string
  latitude: number
  longitude: number
}

export interface FoodPreview {
  foodEntryId: string
  name: string
  sgCents: number
  upvotes: number
  downvotes: number
  photoPresignedUrl: string
  submitterId: string
  submitterUsername: string
  createdAt: string
}

export interface EateryDetail {
  eateryId: string
  name: string
  address: string
  typeLabel: string
  photoUrl: string
  foodPreviews: FoodPreview[]
}

export interface EaterySearchResult {
  eateryId: string
  name: string
  address: string
}

export interface FoodSearchResult {
  foodId: string
  foodName: string
  photoUrl: string
}

export interface FoodEntryDetail {
  foodEntryId: string
  foodPhotoPresignedUrl: string
  submittedAt: string
  submitterId: string
  submitterUsername: string
  submitterProfilePhotoPresignedUrl: string
  submitterWtfScore: number
  submitterTenureDays: number
  submitterEntriesSubmitted: number
}

export interface FoodHistoricalData {
  foodName: string
  sgCentsConsensusPrice: number
  eateryId: string
  eateryAddress: string
  availableDates: string[]
  benchmarkDateEntries: FoodPreview[]
  consensusEntry: FoodEntryDetail
}

export interface Bounds {
  minLat: number
  maxLat: number
  minLon: number
  maxLon: number
}

export interface FoodCreationRequest {
  foodName: string
}

export interface FoodEntrySubmissionRequest {
  eateryId: string
  foodId: string
  priceSgCents: number
}
