# Architectural Decision Record (ADR)

## ADR-001: Full-Screen Leaflet Map with Floating Overlay Controls
Mode toggle + search float at top (pointer-events isolation). Eatery panel slides in from left. Controls NOT inside sidebar.
`src/pages/HomePage/`

## ADR-002: TanStack Query + Zustand Split
Server state → TanStack Query (hooks in `src/shared/api/queries.ts`). UI-only cross-component state → Zustand (`src/stores/mapStore.ts`).
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

## ADR-008: Navbar — Removed "Map View" Link
Homepage IS the map at `/`. Redundant nav link removed. Brand ("SaLoB") serves as home link.

## ADR-009: React StrictMode + Leaflet Load Event
Leaflet's `load` event fires only once. Under React StrictMode double-mount, the second mount's listener never fires. Solution: use `useMap()` + `useEffect` to sync state on mount instead of relying on `load`.

## ADR-010: Map Container Positioning
Map uses `absolute inset-0` within a `relative` parent (not `fixed`). Navbar uses `sticky top-0 z-50` to render above the map. Map wrapper has `isolate` class to contain Leaflet's internal high z-index panes.
