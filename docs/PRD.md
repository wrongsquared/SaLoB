# Product Requirements Document (PRD)

## Overview

SaLoB (Singapore Local Food Price) is a price intelligence platform that chronicles historical local food price developments using crowdsourced community intelligence. It tracks cost adjustments transparently, granting consumers actionable data into regional inflation patterns and micro-economic trends in Singapore.

**Elevator pitch:** "Waze for food prices" — users submit what they paid for chicken rice at their local hawker, the platform computes a trust-weighted consensus price, and everyone can see how prices trend over time.

**Status:** Brownfield project in active development. All backend services run on localhost with env-parity configuration.

---

## Tech Stack

### Backend (Java 25, Spring Boot 4.0.6, Spring Cloud 2025.1.1)

| Service | Framework | Port (HTTP) | Port (gRPC) | Persistence | Description |
|---|---|---|---|---|---|
| **api-gateway** | Spring Cloud Gateway (WebFlux) | 8081 | — | Redis | JWT validation, rate limiting, route dispatch |
| **user-service** | Spring Boot Web MVC | 8082 | 9092 | PostgreSQL + Redis | Auth, user profiles, WTF scores |
| **food-service** | Spring Boot Web MVC | 8083 | 9093 | PostGIS + Redis | Eateries, foods, entries, votes, flags, confidence |
| **shared-proto** | Protobuf library | — | — | — | gRPC contract definitions |

**Gradle:** All services use Gradle 9.4.1 wrapper (shared-proto: 9.3.0).

### Frontend (React 19 RC, Vite, TypeScript)

| Layer | Library | Purpose |
|---|---|---|
| **Routing** | react-router-dom v7 | Client-side routing (createBrowserRouter) |
| **HTTP** | axios | API client |
| **Server state** | @tanstack/react-query v5 | Caching, refetching, mutations |
| **Global state** | zustand v5 | Cross-page UI state (auth token, mode toggle) |
| **Maps** | leaflet + react-leaflet | Interactive map with markers/clustering |
| **UI primitives** | @base-ui/react | Headless, accessible components |
| **CSS** | Tailwind CSS v4 | Utility-first styling |
| **Icons** | lucide-react | Icon set |
| **Charts** | recharts | Historical price charts |
| **WebSocket** | @stomp/stompjs + sockjs-client | Live vote/entry updates |
| **Auth** | @react-oauth/google | Google OAuth integration |
| **Schema validation** | zod | Form/request validation |
| **Font** | @fontsource-variable/inter | Inter variable font |
| **E2E tests** | @playwright/test | UI smoke tests with screenshots |

### Infrastructure (Docker Compose — local dev only)

| Service | Image | Port |
|---|---|---|
| Redis (api-gateway) | redis:8.6.2-alpine | 6380 |
| PostgreSQL (user) | postgres:17.9-alpine | 5433 |
| Redis (user) | redis:8.6.2-alpine | 6381 |
| PostGIS (food) | postgis/postgis:17-3.5 | 5434 |
| Redis (food) | redis:8.6.2-alpine | 6382 |
| RabbitMQ | rabbitmq:3-management | 5672, 15672 |
| MinIO | minio/minio:latest | 9000, 9001 |

**Config philosophy:** Standardized on `.env` files. Each service directory has a `.env.example` (committed) with local dev defaults. Developers copy to `.env` (gitignored) and fill in real secrets. `application.yaml` loads `file:.env[.properties]` with `optional:` prefix for Docker/K8s compatibility where env vars come from container environment. The `.env.k8s` files in `backend/k8s/` follow the same pattern for Minikube deployment.

---

## Architecture

```
[Browser] ──HTTPS──> [API Gateway :8081]
                            │
                ┌───────────┴───────────┐
                │                       │
         /api/auth/*              /api/eateries/*
         /api/users/*             /api/foods/*
         /.well-known/*           /api/food-entries/*
                │                       │
                ▼                       ▼
        [user-service :8082]     [food-service :8083]
        [gRPC :9092]             [gRPC :9093]
                │                       │
                │ ≈ ≈ ≈ gRPC ≈ ≈ ≈ ≈ ≈ │
                │                       │
                ▼                       ▼
        PostgreSQL(5433)          PostGIS(5434)
        Redis(6381)              Redis(6382)
        MinIO(avatars)           MinIO(photos)
```

### Inter-service communication
- **Gateway → Services:** HTTP REST with JWT relay (X-User-Id, X-User-Name, X-User-Roles headers)
- **Food-service → User-service:** gRPC for WTF scores, user details, batch queries
- **Events:** RabbitMQ for async events (vote/flag/submission → WTF recalculation trigger)

---

## Domain Model (Key Entities)

```
User ──┬── Role (ManyToMany)
       ├── wtfScore (double, baseline 50, range 0-100)
       ├── totalSubmissions, upvotesReceived, downvotesReceived, anomaliesFlagged
       └── avatarObjKey → MinIO

Eatery ───┬── location (PostGIS Point, SRID 4326)
          ├── type → EateryType (Hawker Stall, Cafe, Restaurant...)
          ├── isOpen (boolean)
          ├── photoObjKey → MinIO
          ├── foodEntries[] (OneToMany)
          └── closureFlags[] (OneToMany)

Food ──┬── label (unique, e.g. "Chicken Rice")
       ├── photoObjKey → MinIO
       └── foodEntries[] (OneToMany)

FoodEntry ──┬── food (ManyToOne)
            ├── eatery (ManyToOne)
            ├── sgCents (int, price in cents)
            ├── upvoteCount, downvoteCount
            ├── submitterId (UUID, denormalized)
            ├── votes[] (OneToMany ─── FoodEntryVote)
            └── flags[] (OneToMany ─── FoodEntryFlag)

FoodEntryVote ──┬── voterId
                ├── isUpvote (boolean)
                └── UK(voter_id, food_entry_id)

FoodEntryFlag ──┬── flaggerId
                └── reason

EateryClosureFlag ──┬── flaggerId
```

---

## UI Flow

### Screens (referenced from docs/moodboard/)

#### 1. HomePage (HomePage.jpg)
- **Map covers full viewport** below the navbar
- **Segmented toggle button** (top-right of map or below navbar) switches between:
  - **"Eatery Mode"** — Shows eatery markers (icons colored by type). Calls `GET /api/eateries/within-bounds` on every pan/zoom with debounced queries.
  - **"Food Mode"** — Shows food-type markers. Batches `GET /api/food-entries/within-bounds` (or aggregates via eatery bbox + deduped food entries). Future: could use debounced queries for high-throughput simulation.
- **Floating "+" CTA button** (bottom-right) — Triggers the submission wizard overlay. Homepage background blurs behind the overlay.

#### 2. Eatery Panel (HomePage_EateryPanelExpanded.jpg)
- **Collapsible left-side panel** that slides in when clicking an eatery marker
- Shows: eatery name, address, type label, photo
- **Food entries list** — One row per **unique food type** (not per entry). Each row shows:
  - Food name + photo
  - Consensus price (from entry with highest confidence)
  - Upvote/downvote counts
  - The row is the "best entry" — the one with the highest `ConfidenceAlgorithm.computeFinalConfidence()` among all entries of that food at this eatery
- Clicking a food entry row → navigates to food entry detail page

#### 3. Food Entry Detail (FoodEntryPage.jpg)
- Shows detailed info about a specific food entry:
  - Food photo (presigned URL from MinIO)
  - Submitter info: username, profile photo, WTF score, account tenure, total entries submitted
  - Submitted at timestamp
- **Historical data section** (calls `GET /api/food-entries/historical-data/{id}?startDate=`):
  - Available dates calendar/timeline (dates with entries)
  - Consensus price highlighted
  - Benchmark date entries shown for the consensus entry's date
  - Future: recharts line chart of price over time

#### 4. Submission Wizard (HomePage_SubmissionWizard.jpg)
- **Overlay** triggered by floating "+" button
- **No receipt upload/validation** — removed from scope
- Steps:
  1. Search/select eatery (`GET /api/eateries/search`) — or enter name (future: on-demand creation via OneMap/Google Places)
  2. Search/select food (`GET /api/foods/search`) — or enter new food name
  3. Enter price in cents
  4. Submit → `POST /api/food-entries/submit` → overlay closes, panel refreshes

#### 5. Login (LoginPage.jpg)
- **Split screen:** Branding left (tagline: "Keep Tabs on the Price of Living"), form right
- Email/password login (`POST /api/auth/login`)
- Registration link
- Google OAuth (`POST /api/auth/google` — ID Token flow, no redirect)
- New Google users auto-registered on first login
- LOCAL + GOOGLE email collision: reject with "use password login" message

---

## WTF Confidence System

### What it is
The Weighted Trust Factor (WTF) system ensures price data quality through community verification. Two distinct mechanisms:

### 1. User WTF Score (per-user trustworthiness, 0–100)

**Current state:** Static baseline of 50 per user, assigned randomly by seeder. Stored denormalized in user-service DB.

**Planned algorithm (to be implemented):**
```
sub-scores (each 0-100):
  tenureScore    = min(daysSinceRegistration / 365, 1) × 100
  voteScore      = logistic(upvotesReceived / max(downvotesReceived, 1)) × 100
  flagScore      = 100 × (1 - min(flaggedSubmissions / max(totalSubmissions, 1), 0.5))
  volumeScore    = min(totalSubmissions / 20, 1) × 100

rawScore         = w₁·tenureScore + w₂·voteScore + w₃·flagScore + w₄·volumeScore
                     (weights sum to 1, TODO — suggest w₁=0.15, w₂=0.40, w₃=0.30, w₄=0.15)

wtfScore         = clamp(rawScore × activityMultiplier, 0, 100)
activityMultiplier = f(recencyOfLastActivity) — 1.0 if active within 30 days, decaying to 0.5 after 180 days
```

### 2. Food Entry Confidence Score (per-entry reliability, 0–100)

The confidence of a specific price entry, computed in food-service:

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

**Lagrange multiplier curve (wtfScore→multiplier):**
| WTF Score | Multiplier | Meaning |
|---|---|---|
| 0 | 0.1 | Vote barely counts |
| 50 | 1.0 | Neutral influence |
| 100 | 2.5 | Vote matters 2.5x |

**Time decay:** Concave quadratic. After 1 year: 99% preserved. After 5 years: 75% preserved. After 10 years: 0%.

### 3. Consensus Entry Selection

When displaying an eatery's food entries, group by food label, pick the entry with the highest confidence score per group. This is the "best" entry shown in the eatery panel.

---

## Future Considerations (Not in MVP)

- **On-demand eatery creation:** When a user searches for an eatery that doesn't exist, allow creating it via OneMap/Google Places API. Requires `POST /api/eateries` endpoint with address validation.
- **WebSocket live updates:** STOMP over RabbitMQ for live vote/entry updates. Food service publishes to exchange, frontend subscribes via gateway.
- **High-throughput simulation:** Plan to simulate concurrent upvotes for portfolio benchmarking numbers. The caching strategy is designed for this.
- **Receipt upload & validation:** Explicitly removed from scope.
- **Settings page:** Deferred.
- **OneMap API integration:** For smarter eatery search/autocomplete (noted as TODO in `EateryService`).
- **Google OAuth:** Dependency installed, not wired up yet.
- **Kubernetes manifests:** Exist in `backend/k8s/` but not actively used.

---

## Design Decisions (ADR-style)

1. **Microservices for learning** — Not because SaLoB needs them, but to demonstrate Spring Boot, Redis, gRPC, RabbitMQ, PostGIS, and container orchestration skills on a portfolio project.
2. **Debounced map queries** — Chosen over continuous websocket push for simplicity and to generate benchmarkable request counts for portfolio metrics.
3. **Denormalized WTF+stats in user-service** — Separate ownership: food-service owns events, user-service owns WTF computation. Updated async via RabbitMQ to avoid locking during confidence reads.
4. **Three-level caching** — Addresses the O(n·m·gRPC) bottleneck without premature optimization. L1 (WTF cache) alone eliminates the worst offender.
5. **Env-parity config** — All config reads from `.env` files via `spring.config.import: optional:file:.env[.properties]`. Each service has a `.env.example` template (committed) with working dev defaults; `.env` (gitignored) holds local overrides and secrets. The `optional:` prefix is kept intentionally for Docker/K8s compatibility where env vars come from container environment, not a filesystem `.env` file.
6. **No receipt upload** — Removed to keep MVP scope manageable. Photos will come from seeded Unsplash data + future user upload.

---

## Semantic Hints for Future Agents

Key files and their responsibilities:

- `ConfidenceAlgorithm.java` — Core business logic for entry confidence scoring. Uses Lagrange interpolation + time decay. Should switch from per-vote gRPC to `getUserWtfScoreBatch`.
- `EateryService.getEateryDetailed()` — Groups entries by food label, picks best per confidence. Currently uncached — should use L3 consensus cache.
- `UserService.getUserWtfScoreBatch()` — Batch WTF query exists but is unused by food-service. Batch size = number of unique voters for an entry.
- `UserGrpcHandler.getUserWtfScoreBatch()` — Proto-defined RPC with `repeated UserWtfBatchResponseItem`.
- `RateLimiter.java` — Redis token-bucket. Currently hardcoded 60 req/min.
- `BboxKeyGenerator.java` — Rounds bbox coords to 0.01° for cache bucketing.
- `MinioStorageService.java` — Presigned URL generation with 30-min default expiry.
- `FoodEntryService.toDetailed()` — Calls gRPC `getUserDetails` for submitter info. Already uses individual gRPC, consider batching if called in loops.
- `GlobalGrpcExceptionHandler.java` / `GlobalHttpExceptionHandler.java` — Exception handling pattern.
- Seeder files in `backend/food-service/src/main/java/com/salob/food_service/seeder/` — Seed data including 70+ Singapore eateries, 65 foods, with Unsplash images.
