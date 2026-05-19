# Architectural Decision Record (ADR)

## ADR-001: HomePage Architecture — Full-Screen Map with Overlay Controls

**Status:** Accepted  
**Context:** The HomePage (replacing the MapView stub) needed a layout that puts the map first, with controls layered on top.

**Decision:**
- Full-screen Leaflet map as the base layer.
- Mode toggle (Eatery/Food) and search bar float at top center, semi-transparent, with `pointer-events` isolation to allow map interaction through the transparent areas.
- Eatery panel slides in from the left as an absolutely-positioned `<aside>` overlaid on the map.
- The controls are NOT inside the sidebar; they're always visible at the top regardless of sidebar state.

**Consequences:**
- Clean separation of concerns: map layer, overlay controls, and sidebar panel are independent.
- Controls are always accessible, even when sidebar is collapsed.
- Requires careful z-index management (`z-10` for controls, `z-20` for sidebar).
- The `h-[calc(100vh-57px)]` on the page container assumes a 57px Navbar height — fragile if Navbar height changes.

---

## ADR-002: TanStack Query + Zustand Split

**Status:** Accepted  
**Context:** The codebase had no state management or server-state caching wired up. Both TanStack Query and Zustand were installed but unused.

**Decision:**
- **TanStack Query** owns all server state: eatery bounds queries, eatery details, food searches, food entry details, historical data. Each query has a `staleTime` tuned to its volatility (30s for bounds, 60s for details, 120s for history).
- **Zustand** owns only cross-component UI state: map center/zoom/bounds, selected mode, selected eatery, sidebar open state, selected foods, search query text.

**Consequences:**
- No duplication of server state in client stores (per FRONTEND.md guidelines).
- Bounds-based queries are automatically debounced via `useDebounce()` before being passed to the query key — prevents excessive API calls during pan/zoom.
- The Zustand store is page-specific (HomePage) but lives in `src/stores/mapStore.ts` because it's referenced across multiple sub-components in different directories.

---

## ADR-003: PascalCase Page Directories

**Status:** Accepted  
**Context:** FRONTEND.md says "create `src/pages/<page-name>/`" but doesn't specify casing.

**Decision:** Use PascalCase (`HomePage/`) for page directories and `index.tsx` as the entry point. This matches React component naming conventions (components are PascalCase) and keeps the directory name consistent with the component name.

---

## ADR-004: Axios over Native fetch

**Status:** Accepted  
**Context:** A shared API client was needed. Axios was already a dependency.

**Decision:** Use axios with a shared instance (`apiClient`) configured with the base URL from `VITE_API_BASE_URL` (defaults to `/api`). MSW intercepts at the network level, so it works identically for both axios and fetch.

**Consequences:**
- Adding request/response interceptors for auth tokens later will be trivial.
- Axios auto-parses JSON, provides typed generics (`apiClient.get<T>(url)`).

---

## ADR-005: react-leaflet-cluster for Marker Clustering

**Status:** Accepted  
**Context:** Maps can display hundreds of markers (eateries or food entries), hurting performance and readability.

**Decision:** Use `react-leaflet-cluster` (already a dependency) to automatically cluster markers at low zoom levels and uncluster them on zoom-in. No custom clustering logic needed.

**Consequences:**
- Zero additional clustering code — the library handles spidering, cluster icons, and animation.
- Eatery mode and food mode markers both use the same `<MarkerClusterGroup>` wrapper.
- If custom cluster styling is needed later (e.g., showing price ranges), the library's `iconCreateFunction` can be customized.

---

## ADR-006: DivIcon over Default Leaflet Markers for Eatery/Food Icons

**Status:** Accepted  
**Context:** Eatery markers need type-specific colors (Hawker Stalls = blue, Cafes = amber, etc.). Food markers need distinct colors per entry. Default Leaflet markers are monochrome and not customizable per-feature.

**Decision:** Use `L.divIcon` with inline HTML/CSS for all map markers. Each marker is a colored circle with an emoji icon.

**Consequences:**
- Full visual control per marker type — colors are driven by the `typeColors` lookup table for eateries and a rotating `foodColorPalette` for food entries.
- No external sprite sheets or icon image dependencies.
- DivIcons are slightly less performant than image icons at very high counts (1000+), but react-leaflet-cluster handles aggregation before that threshold matters.

---

## ADR-007: Batch-Fetch Eatery Details for Food Mode (N+1)

**Status:** Accepted (with caveat)  
**Context:** Food mode needs to show food entry markers on the map. The only way to get food entries is via `/api/eateries/:id` (which returns `foodPreviews`). There is no batch endpoint.

**Decision:** In food mode, after fetching eateries within bounds, batch-fetch details for ALL eateries in parallel using `Promise.all`. The results are cached by TanStack Query with a 30s stale time.

**Caveat:** This is an N+1 pattern — one request for the bounds query + N requests for individual eatery details. Acceptable for MVP because:
- The mock dataset has 5 eateries.
- In production with hundreds of eateries, a dedicated `/api/food-entries/within-bounds` endpoint should be added to the backend.
- The batch fetch runs only when bounds change, not on every render.
- TanStack Query's `staleTime` prevents redundant re-fetches.

**Consequences:**
- N+1 is documented here for future optimization.
- A backend endpoint change should unblock this — the ADR can be revisited.

---

## ADR-008: Navbar Simplification — Remove "Map View" Link

**Status:** Accepted  
**Context:** The homepage IS the map view at the index route `/`. Having both a "Map View" nav link and the brand/home link pointing to the same page is redundant.

**Decision:** Removed the "Map View" entry from the Navbar's `navItems`. The brand ("SaLoB") serves as the home link, and the index route renders the map.

**Consequences:**
- One fewer nav item, reducing visual clutter.
- Users already on the map have one less distracting link.
- The route `/` still correctly renders the HomePage.
