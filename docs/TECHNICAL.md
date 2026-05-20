# Technical Documentation

## Inter-Service Communication

### Gateway → Services (HTTP REST)
- API Gateway (`:8081`) routes to user-service (`:8082`) and food-service (`:8083`)
- JWT validated at gateway, user identity injected as headers: `X-User-Id`, `X-User-Name`, `X-User-Roles`
- Services trust these headers — no secondary JWT validation needed

### Food-Service → User-Service (gRPC)
- `getUserWtfScoreBatch` — batch-fetch WTF scores for multiple users in one round-trip
- `getUserDetails` — fetch single user profile (username, photo, tenure, WTF score)
- Proto definitions in `backend/shared-proto/`
- **Optimization note:** `ConfidenceAlgorithm` currently uses per-vote individual gRPC calls. Should switch to `getUserWtfScoreBatch` — collect all unique voterIds, fetch in one call, build `Map<UUID, Double>` lookup.

### Events (RabbitMQ)
- Food-service publishes to RabbitMQ exchange on: new food entry, new vote, new flag
- User-service consumes events → recalculates WTF for affected user → updates denormalized stats → invalidates Redis cache
- Async flow prevents locking during confidence reads

## WTF Recalculation Flow

```
1. Food service publishes event to RabbitMQ exchange when:
   - New food entry submitted (by user X)
   - Vote cast on a food entry owned by user X
   - Flag raised on a food entry owned by user X
2. User service consumes event, recalculates WTF for that user
3. Updates denormalized stats (totalSubmissions, upvotesReceived, downvotesReceived, anomaliesFlagged)
4. Saves new WTF score, invalidates Redis cache for that user's WTF
```

### User WTF Score Algorithm (planned, not yet implemented)
```
sub-scores (each 0-100):
  tenureScore    = min(daysSinceRegistration / 365, 1) × 100
  voteScore      = logistic(upvotesReceived / max(downvotesReceived, 1)) × 100
  flagScore      = 100 × (1 - min(flaggedSubmissions / max(totalSubmissions, 1), 0.5))
  volumeScore    = min(totalSubmissions / 20, 1) × 100

rawScore         = w₁·tenureScore + w₂·voteScore + w·flagScore + w₄·volumeScore
                     (weights sum to 1 — suggest w₁=0.15, w₂=0.40, w₃=0.30, w₄=0.15)

wtfScore         = clamp(rawScore × activityMultiplier, 0, 100)
activityMultiplier = f(recencyOfLastActivity) — 1.0 if active within 30 days, decaying to 0.5 after 180 days
```

### Food Entry Confidence Score (implemented)
```
valuePerVote = 100 / VOTES_REQUIRED_FOR_MAX_SCORE  // 150
rawConfidence = 0
for each vote in foodEntry.votes:
    wtfScore = getVoterWtfScore(vote.voterId)     // via gRPC (cached or batched)
    multiplier = LagrangeInterpolate(wtfScore)     // 0→0.1, 50→1.0, 100→2.5
    rawConfidence += (vote.isUpvote ? +valuePerVote : -valuePerVote) × multiplier

ageYears = now - foodEntry.createdAt
nonDecayPercent = -(ageYears²) + 100
finalConfidence = clamp(rawConfidence × nonDecayPercent / 100, 0, 100)
```

Lagrange multiplier curve: WTF 0 → 0.1x, WTF 50 → 1.0x, WTF 100 → 2.5x
Time decay: concave quadratic. 1 year: 99% preserved. 5 years: 75%. 10 years: 0%.

## Caching Strategy (Three Levels)

| Level | Key | Value | Cache Location | TTL | Invalidation Trigger |
|---|---|---|---|---|---|
| **L1: User WTF** | `user_wtf:{userId}` | double | user-service Redis | 15 min | RabbitMQ event on WTF recalc |
| **L2: Entry Confidence** | `food_entry_conf:{entryId}` | double | food-service Redis | 1 hour | New vote/flag on that entry |
| **L3: Eatery Consensus** | `eatery_consensus:{eateryId}` | `EateryDetailedDTO` | food-service Redis | 1 hour | New food entry at that eatery |

**Key insight:** `@Cacheable` on `private` methods fails because Spring AOP cannot intercept private calls. Extract into a dedicated `@Component` bean with `public` methods.

**Batch optimization:** `getUserWtfScoreBatch` gRPC method exists in both proto and user-service — `ConfidenceAlgorithm` should use it instead of N individual calls.

## Key Data Flows

### User views an eatery on the map
1. User pans/zooms map
2. Frontend debounces (300ms), calls `GET /api/eateries/within-bounds`
3. API Gateway validates JWT (if present), adds `X-User-Id` header
4. Gateway routes to food-service
5. `RateLimiter` checks Redis token bucket (60 req/min/IP)
6. `BboxKeyGenerator` rounds coords to 0.01° grid for cache bucketing
7. `@Cacheable` check — hit? return cached. Miss? query PostGIS `ST_Within`
8. Return `List<EateryMapDTO>` (id, name, lat, lon, typeLabel)
9. Frontend renders markers on Leaflet map

### User clicks an eatery
1. Frontend calls `GET /api/eateries/{eateryId}`
2. Food service loads eatery entity + fetches all food entries (LAZY)
3. For each food entry: collect unique voter IDs, batch-fetch WTF scores via gRPC, compute confidence, track best entry per food label
4. Return `EateryDetailedDTO` with deduplicated foodPreviews
5. Frontend slides in left panel with eatery info + food entry rows

### User submits a price entry
1. User clicks "+" → wizard overlay
2. Search/select eatery → search/select food → enter price
3. `POST /api/food-entries/submit` with `X-User-Id` header
4. Food service creates `FoodEntry` entity
5. RabbitMQ event published: "new_entry" with submitterId
6. User service consumes → recalculates WTF → updates cache
7. Frontend invalidates eatery query → refetch shows new entry

### User votes on an entry (endpoint TODO)
1. Frontend: `POST /api/food-entries/{id}/vote`
2. Food service: validate voter hasn't already voted (UK constraint)
3. Vote saved, upvoteCount/downvoteCount incremented
4. L2 cache (entry confidence) invalidated
5. RabbitMQ event published: "new_vote" with entry owner ID
6. WebSocket broadcast to subscribed clients: `/topic/eatery/{eateryId}/entries`
7. Other users see vote count update live
