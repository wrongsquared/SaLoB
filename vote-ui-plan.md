# Vote UI — Implementation Plan

## Design Decisions

| Decision | Choice |
|----------|--------|
| Vote UX | **Optimistic** — update UI instantly, revert on error |
| Logout | **Full reload** — `window.location.href = '/'` |
| Self-vote | **Disabled with tooltip** — "You cannot vote on your own entry" |
| Eatery detail caching | **No caching** — endpoint is now user-specific |

---

## Backend Changes

### 1. `FoodEntryVoteRepository.java` — add vote lookup + delete

```java
@Query("SELECT v FROM FoodEntryVote v WHERE v.foodEntry.id IN :foodEntryIds AND v.voterId = :voterId")
List<FoodEntryVote> findByVoterIdAndFoodEntryIdIn(@Param("voterId") UUID voterId, @Param("foodEntryIds") List<UUID> foodEntryIds);

Optional<FoodEntryVote> findByVoterIdAndFoodEntryId(UUID voterId, UUID foodEntryId);

void deleteByVoterIdAndFoodEntryId(UUID voterId, UUID foodEntryId);
```

### 2. `FoodEntryPreviewDTO.java` — add `currentUserVote`

Append `Boolean currentUserVote` (nullable) as the last field. This is a positional record — update all callers.

### 3. `VoteRequest.java` — make `isUpvote` nullable

Change `@NotNull Boolean isUpvote` to allow null (for retraction).

### 4. `FoodEntryService.java` — vote upsert/retract + populate currentUserVote

**`castVote()`** — change to upsert logic:
- If vote exists + `isUpvote = null` → delete vote (retract)
- If vote exists + `isUpvote != null` → delete old, insert new (toggle)
- If no vote + `isUpvote != null` → insert new
- Self-vote check stays

**`getFoodEntryHistoricalData()`** — accept `UUID userId`, batch-lookup user's votes for community entries, populate `currentUserVote`.

### 5. `EateryService.java` — accept userId, populate currentUserVote

**`getEateryDetailed()`** — accept `UUID userId`, batch-lookup votes for all food previews, populate `currentUserVote`.

### 6. Controllers — accept `X-User-Id`

- `EateryController.getEateryDetailed()` — add `@RequestHeader(value = "X-User-Id", required = false) UUID userId`
- `FoodEntryController.getFoodEntryHistoricalData()` — add same
- Pass to services

### 7. `api-spec.yaml`

- Add `currentUserVote` (nullable boolean) to `FoodPreview`
- Make `VoteRequest.isUpvote` nullable (nullable boolean, not required)
- Fix `FoodHistoricalData` field names: `availableDates` → `datePrices`, `benchmarkDateEntries` → `communityEntries` (currently mismatched with actual DTO)

---

## Frontend Changes

### 1. `shared/types/api.ts` — update `FoodPreview`

```typescript
export interface FoodPreview {
  // ... existing fields
  currentUserVote: boolean | null
}
```

### 2. `shared/api/queries.ts` — add `useVote` mutation

Optimistic updates:
- Track previous vote state for rollback
- On mutate: immediately update cache's `currentUserVote` + counts
- On error: rollback
- On success: invalidate related queries to ensure consistency

### 3. `EateryPanel.tsx` — interactive FoodEntryCard

- Highlight thumbs: green fill when `currentUserVote === true`, red fill when `false`
- Click handler: determine new vote (toggle), update local state optimistically
- If `submitterId === currentUserId`: disable buttons + tooltip
- Invalidate `QK.EATERIES_DETAIL` on vote

### 4. `CommunityEntryRow.tsx` — same as above

- Same interactive thumbs + optimistic vote
- Invalidate `QK.FOOD_ENTRY_HISTORICAL` on vote

### 5. `Navbar.tsx` — full refresh on logout

```typescript
const handleLogout = () => {
  logout()
  window.location.href = '/'
}
```

### 6. MSW handlers

Stateful per-entry per-user vote tracking. On vote:
- Update internally tracked `currentUserVote`
- Adjust `upvotes`/`downvotes` counts in mock data

---

## Files Changed

### Backend (8 files)
| File | Change |
|------|--------|
| `FoodEntryPreviewDTO.java` | Add `Boolean currentUserVote` |
| `VoteRequest.java` | `isUpvote` → Boolean (nullable) |
| `FoodEntryVoteRepository.java` | Add batch lookup + delete methods |
| `FoodEntryService.java` | Upsert vote logic; populate currentUserVote; accept userId |
| `EateryService.java` | Accept userId; populate currentUserVote |
| `EateryController.java` | Accept `X-User-Id` header |
| `FoodEntryController.java` | Accept `X-User-Id` header on historical-data |
| `api-spec.yaml` | Add `currentUserVote`; nullable `isUpvote`; fix FoodHistoricalData fields |

### Frontend (6 files)
| File | Change |
|------|--------|
| `shared/types/api.ts` | `currentUserVote` on FoodPreview |
| `shared/api/queries.ts` | `useVote` mutation with optimistic update |
| `pages/HomePage/EateryPanel.tsx` | Interactive vote buttons, self-vote disabled |
| `pages/FoodEntryDetailPage/CommunityEntryRow.tsx` | Interactive vote buttons, self-vote disabled |
| `components/Navbar.tsx` | `window.location.href = '/'` on logout |
| `mocks/handlers.ts` | Stateful vote MSW handler |
