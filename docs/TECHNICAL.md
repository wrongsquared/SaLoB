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
- Food-service publishes to `salob.events` topic exchange on: new food entry (`wtf.entry.created`), new vote (`wtf.vote.cast`), new flag (`wtf.flag.raised`)
- User-service queue `user-service.wtf.recalc` binds with routing key `wtf.#` — receives all three event types
- Processed event IDs are stored in a `processed_events` table for idempotency (prevents double-counting on crash recovery)
- Async flow prevents locking during confidence reads

## WTF Recalculation Flow

```
1. Food service publishes event to salob.events exchange when:
   - New food entry submitted (routing key "wtf.entry.created")
   - Vote cast on a food entry (routing key "wtf.vote.cast")
   - Flag raised on a food entry (routing key "wtf.flag.raised")
2. User service consumes from "user-service.wtf.recalc" queue
3. Checks processed_events table for idempotency (skip if duplicate)
4. Records event as processed
5. Increments the relevant counter on the target user
   - ENTRY_SUBMITTED → totalSubmissions += 1
   - VOTE_CAST → upvotesReceived/downvotesReceived += 1 (depends on isUpvote)
   - FLAG_RAISED → anomaliesFlagged += 1
6. Updates the actor's (voter/flagger/submitter) lastActivityAt
7. Recomputes WTF score using the formula
8. Saves new WTF score, Redis L1 cache invalidated on next read
```

### User WTF Score Algorithm
```

sub-scores (each 0-100):
  tenureScore    = min(daysSinceRegistration / TENURE_DAYS_MAX, 1) × 100
  voteScore      = 50 + 50 × tanh((upvotesReceived - downvotesReceived) / VOTE_SLOPE_DIVIDER)
  flagScore      = 100 × (1 - anomaliesFlagged / max(totalSubmissions, 1))
  volumeScore    = min(totalSubmissions / VOLUME_SUBMISSIONS_MAX, 1) × 100

rawScore         = w₁·tenureScore + w₂·voteScore + w₃·flagScore + w₄·volumeScore

wtfScore         = clamp(rawScore × activityMultiplier, 0, 100)
activityMultiplier  = f(daysSinceLastActivity)
                    = 1.0 - (1.0 - ACTIVITY_MIN_MULTIPLIER) × min(daysSinceLastActivity / ACTIVITY_DECAY_DAYS, 1)
```

**Parameters (tunable constants in `WtfRecalculationService`):**

| Parameter | Default | Description |
|---|---|---|
| `TENURE_DAYS_MAX` | 365 | Days to reach full tenure score |
| `VOTE_SLOPE_DIVIDER` | 10 | Steepness of tanh S-curve (lower = steeper) |
| `VOLUME_SUBMISSIONS_MAX` | 20 | Submissions to reach full volume score |
| `W_TENURE` | 0.15 | Weight for tenure sub-score |
| `W_VOTE` | 0.40 | Weight for vote sub-score |
| `W_FLAG` | 0.30 | Weight for flag sub-score |
| `W_VOLUME` | 0.15 | Weight for volume sub-score |
| `ACTIVITY_DECAY_DAYS` | 180 | Days of inactivity until multiplier reaches min |
| `ACTIVITY_MIN_MULTIPLIER` | 0.5 | Floor of the activity decay multiplier |

**Example walkthroughs:**

**New user (0 subs, 0 votes, 0 flags):**
tenureScore=0, voteScore=`50+50×tanh(0/10)=50`, flagScore=`100×(1-0/1)=100`, volumeScore=0
rawScore = `0.15×0 + 0.40×50 + 0.30×100 + 0.15×0 = 20 + 30 = 50`
active ≤30 days → multiplier=1.0 → wtfScore = **50** ✅

**Active trusted user (365 days tenure, 20 subs, 10 upvotes received, 0 downvotes, 0 flags, active today):**
tenureScore=`min(365/365,1)×100=100`, voteScore=`50+50×tanh(10/10)=50+50×0.76=88`, flagScore=100, volumeScore=100
rawScore = `0.15×100 + 0.40×88 + 0.30×100 + 0.15×100 = 15 + 35 + 30 + 15 = 95`
multiplier=1.0 → wtfScore = **95**

**Penalized user (0 tenure, 5 subs, 0 upvotes, 5 downvotes, 3 of 5 submissions flagged):**
tenureScore=0, voteScore=`50+50×tanh((0-5)/10)=50+50×(-0.46)=27`, flagScore=`100×(1-3/5)=40`, volumeScore=`min(5/20,1)×100=25`
rawScore = `0.15×0 + 0.40×27 + 0.30×40 + 0.15×25 = 0 + 11 + 12 + 4 = 27`
active ≤30 days → multiplier=1.0 → wtfScore = **27**

**Decayed user (same as trusted user above, but inactive 200 days):**
rawScore=95, daysSinceLastActivity=200
multiplier = `1.0 - (1.0 - 0.5) × min(200/180, 1) = 1.0 - 0.5 × 1.0 = 0.5`
wtfScore = `95 × 0.5 = 47`

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
5. RabbitMQ event published: `wtf.entry.created` with submitterId
6. User service consumes → recalculates WTF → updates cache
7. Frontend invalidates eatery query → refetch shows new entry

### User votes on an entry
1. Frontend: `POST /api/food-entries/{id}/vote` with body `{ isUpvote: boolean }`
2. Food service: self-vote check (voterId != submitterId)
3. Food service: UK constraint check (one vote per voter per entry)
4. Vote saved, upvoteCount/downvoteCount incremented atomically
5. RabbitMQ event published: `wtf.vote.cast` with voterId + entryOwnerId + isUpvote
6. User service consumes event → increments owner's upvotesReceived/downvotesReceived
7. User service recalculates owner's WTF, updates voter's lastActivityAt
8. L2 cache (entry confidence) invalidated on next confidence read
