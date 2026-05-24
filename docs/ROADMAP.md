# Roadmap

## ✅ Done

### OneMap Integration (Search Augmentation)
Backend OneMap client + service + controller (search combined, dedup by name, geocode on create). Frontend autocomplete in StepEatery and homepage SearchBar. Flyway migrations, DB trigger, EateryTypeController, API spec.

---

## Bugs

### mapStore crash — "Maximum update depth exceeded"
The frontend occasionally crashes with a React error about repeated `setState` calls originating from `mapStore`. Likely a circular update loop — map events → store update → re-render → map events. Needs investigation and fix.

---

## Vote UI (Frontend)
**Description:** Add upvote/downvote interaction to the food entry detail page AND the homepage eatery panel. Backend endpoint (`POST /api/food-entries/{id}/vote`) ✅ exists.
**Scope:**
- **EateryPanel (homepage)**: logged-in user sees their vote on each food entry, can toggle upvote/downvote/unvote.
- **FoodEntryDetailPage / CommunityEntryRow**: same interaction on the community entries list.
- **Backend gap**: `FoodEntryPreviewDTO` lacks `currentUserVote` — must be added and populated from the vote table.
- **Logout**: `window.location.href = '/'` for full refresh to clear stale state.
**Key files:** `EateryPanel.tsx`, `CommunityEntryRow.tsx`, `FoodEntryPreviewDTO.java`, `FoodEntryService.java`, `EateryService.java`, `EateryController.java`, `FoodEntryController.java`, `queries.ts`

---

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
