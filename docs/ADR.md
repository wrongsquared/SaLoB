# Architectural Decision Record (ADR)

## ADR-001: Full-Screen Leaflet Map with Floating Overlay Controls
Mode toggle + search float at top (pointer-events isolation). Eatery panel slides in from left. Controls NOT inside sidebar.
`src/pages/HomePage/`

## ADR-002: TanStack Query + Zustand Split
Server state → TanStack Query (7 hooks in `src/shared/api/queries.ts`). UI-only cross-component state → Zustand (`src/stores/mapStore.ts`).
Per FRONTEND.md: "Server state belongs in TanStack Query; avoid duplicating it in global state."

## ADR-003: PascalCase Page Directories
`HomePage/index.tsx` not `homepage/index.tsx`. Matches React component casing conventions.

## ADR-004: Axios for API Client
`src/shared/api/client.ts` — base URL from `VITE_API_BASE_URL` (defaults to `/api`). MSW intercepts at network level, works with both.

## ADR-005: react-leaflet-cluster for Markers
Zero custom clustering logic. Handles spidering + animation. Both mode markers use `<MarkerClusterGroup>`.

## ADR-006: DivIcon for Custom Map Markers
`L.divIcon` with inline HTML/CSS. Eatery: `typeColors` table (by typeLabel). Food: rotating `foodColorPalette`.
Helper: `coloredCircleIcon(color, emoji, size)` in `MarkerLayers.tsx`.

## ADR-007: N+1 Batch Fetch for Food Mode (Caveat)
Food mode fetches `/api/eateries/:id` for ALL eateries in bounds (no batch endpoint exists). Fine for MVP (5 mock eateries).
**Needs:** dedicated `/api/food-entries/within-bounds` backend endpoint before scaling beyond ~50 eateries.

## ADR-008: Navbar — Removed "Map View" Link
Homepage IS the map at `/`. Redundant nav link removed. Brand ("SaLoB") serves as home link.
