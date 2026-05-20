# Roadmap

## [HIGH] User WTF Recalculation Algorithm
**Description:** Implement the full WTF scoring algorithm for users (currently static baseline of 50). Combines tenure score, vote score, flag score, and volume score with activity-based decay multiplier. Triggered asynchronously via RabbitMQ events when entries/votes/flags change.
**Dependencies:** RabbitMQ event publishing from food-service, user-service event consumer, Redis cache invalidation
**Key files:** `UserService.java`, `WtfRecalculationService.java` (new), `ConfidenceAlgorithm.java` (uses result)
**Formula:** See `docs/TECHNICAL.md` for full algorithm specification

## Vote Endpoint + WebSocket Live Updates
**Description:** Implement `POST /api/food-entries/{id}/vote` endpoint with UK constraint (one vote per user per entry). Broadcast vote count changes via WebSocket (STOMP over RabbitMQ) so all connected clients see updates in real-time.
**Dependencies:** WebSocket infrastructure (already has `@stomp/stompjs` + `sockjs-client` in frontend), RabbitMQ exchange for broadcast
**Key files:** `FoodEntryController.java` (new endpoint), `VoteService.java` (new), frontend vote mutation hook

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
**Note:** Update `README.md` to document OneMap API key as a required `.env` blank

## AI Food Verification
**Description:** When a user creates a new food type (not in seeded database), automatically run through AI legitimacy check. AI approves/rejects based on plausibility (e.g., "Chicken Rice at $500" → reject, "Hainanese Chicken Rice at a McDonald's" → suspicious). No human moderation — AI decision is final. No corrections suggested.
**Dependencies:** LLM provider integration (OpenAI/Anthropic), prompt engineering for food legitimacy
**Key files:** `FoodService.java` (currently has TODO for AI pipeline), `FoodController.java`

## Eatery Closure Flow Completion
**Description:** Backend has `EateryClosureFlagRepository` and frontend has "Report as Closed" button, but no admin review flow. Complete the flow: aggregate closure flags, auto-close eatery after threshold, or flag for admin review.
**Dependencies:** Admin RBAC, notification system
**Key files:** `EateryClosureFlagRepository.java`, `EateryService.reportClosed()`, admin dashboard page

## AI Assistant (Natural Language Query)
**Description:** In-app chat widget that acts as a natural language query interface. Examples: "What is the price of chicken rice at Maxwell Food Centre in early 2023?" Can also perform navigation actions — "Do you want me to show you?" → click yes → navigates to relevant page.
**Design status:** Needs expansion via grill-me workflow. Key questions: chat widget vs full-page, LLM provider, data access scope (full DB vs aggregated), navigation action implementation.
**Dependencies:** LLM provider, chat UI component, intent parsing for navigation actions

## Settings Page
**Description:** User profile editing, preferences, notification settings. Deferred from MVP.
**Dependencies:** Auth flow completion, user profile endpoints
