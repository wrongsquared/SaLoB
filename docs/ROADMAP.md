# Roadmap

## Touch Ups

### Map Icon Visibility
Icons are hard to see against noisy OSM tiles (especially the small 14px Lucide SVGs on eatery markers).
**Open questions:**
- Darken or blur the tile layer behind markers? (CSS `mix-blend-mode` or a semi-transparent overlay)
- Increase circle size and icon scale on hover/selected state?
- Add a subtle white stroke/shadow around the icon SVG to separate it from the background?
- Possibly reduce tile opacity globally and render a flat coloured underlay?

### Map Clustering Aesthetics
Cluster numbers are barely readable — small text on a tiny circle.
**Open questions:**
- Replace the default spider/cluster with a larger circle (e.g. 48px diameter, bold white number on dark background)?
- Show a stacked row of miniature "pins" instead of a number badge (like Google Maps clusters)?
- Customize the `MarkerClusterGroup` iconCreateFunction to render a larger circle with the count?
- Should clusters show a sum of icons (e.g. 3 small Store icons) instead of a numeric count?

---

## Vote UI (Frontend)
**Description:** Add upvote/downvote interaction to the food entry detail page. Backend endpoint (`POST /api/food-entries/{id}/vote`) and event pipeline are complete.
**Points of contention:**
- Auth dependency: vote endpoint requires `X-User-Id` header — is the auth/JWT flow wired on the frontend yet, or do we use the current hardcoded UUID placeholder?
- Optimistic vs server-confirmed: backend has a UK constraint + self-vote check. If we update the count optimistically and the server rejects, we must revert. Is that acceptable UX, or do we wait for the server response?
- Scope: only on the detail page, or also on map popups and community entry rows?
- Refresh strategy: invalidate the detail query after voting (triggers full refetch) vs manual count increment/decrement on the cached data (snappy but fragile)?
**Key files:** `FoodEntryDetailPage/index.tsx`, `CommunityEntryRow.tsx`, `queries.ts` (new mutation), `api-spec.yaml` (already updated ✅)

---

## Historical Data Charts — Confidence-Based Time Series
**Description:** For each time interval going backwards from today, find the food entry with the best confidence score at that interval. Display as a Recharts line chart on the FoodEntryDetailPage.
**Open question:** What are the time intervals (daily/weekly/monthly)? If no entries exist for an interval, should we interpolate, skip, or show last known price?
**Dependencies:** New backend endpoint or query logic to aggregate confidence-weighted prices per interval
**Key files:** `FoodEntryService.java`, `FoodEntryDetailPage/PriceChart.tsx`

## Rate Limiting Consolidation
**Description:** Move all rate limiting from individual endpoints to the API Gateway layer. Gateway already has IP-based Redis token-bucket. Add per-user rate limiting for authenticated endpoints (e.g., `/submit` should have stricter cap than map queries). Business-logic limits: max submissions per eatery per day per user.
**Dependencies:** Gateway filter modification, env-var configurable limits for testing
**Key files:** `api-gateway/src/main/java/.../RateLimitFilter.java` (new), remove `RateLimiter` from food-service endpoints

## OneMap Integration (Search Augmentation)
**Description:** Augment eatery search with OneMap's POI database for Singapore-specific autocomplete. When a user searches for an eatery that doesn't exist in our DB, OneMap provides address, block number, and building name for lazy-insertion.
**Dependencies:** OneMap API key (add to `.env`), new endpoint `POST /api/eateries` with address validation
**Key files:** `EateryService.java` (currently has TODO for OneMap), `EateryController.java`

## Eatery Closure Flow
**Description:** Two-part feature — (1) "Report as Closed" button (frontend) → `POST /api/eateries/{id}/report-closed` (already exists ✅), (2) admin review flow that aggregates closure flags, auto-closes eatery after threshold, or flags for admin review.
**Dependencies:** Admin RBAC, notification system
**Key files:** `EateryClosureFlagRepository.java`, `EateryService.reportClosed()`, admin dashboard page

## AI Food Verification
**Description:** When a user creates a new food type (not in seeded database), automatically run through AI legitimacy check. AI approves/rejects based on plausibility (e.g., "Chicken Rice at $500" → reject, "Hainanese Chicken Rice at a McDonald's" → suspicious). No human moderation — AI decision is final. No corrections suggested.
**Dependencies:** LLM provider integration (OpenAI/Anthropic), prompt engineering for food legitimacy
**Key files:** `FoodService.java` (currently has TODO for AI pipeline), `FoodController.java`

## AI Assistant (Natural Language Query)
**Description:** In-app chat widget that acts as a natural language query interface. Examples: "What is the price of chicken rice at Maxwell Food Centre in early 2023?" Can also perform navigation actions — "Do you want me to show you?" → click yes → navigates to relevant page.
**Design status:** Needs expansion via grill-me workflow. Key questions: chat widget vs full-page, LLM provider, data access scope (full DB vs aggregated), navigation action implementation.
**Dependencies:** LLM provider, chat UI component, intent parsing for navigation actions

## Settings Page
**Description:** User profile editing, preferences, notification settings. Deferred from MVP.
**Dependencies:** Auth flow completion, user profile endpoints
