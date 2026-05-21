# Architectural Decision Record (ADR)

## ADR-001: Full-Screen Leaflet Map with Floating Overlay Controls
Mode toggle + search float at top (pointer-events isolation). Eatery panel slides in from left. Controls NOT inside sidebar.
`src/pages/HomePage/`

## ADR-002: TanStack Query + Zustand Split
Server state → TanStack Query (hooks in `src/shared/api/queries.ts`). UI-only cross-component state → Zustand (`src/stores/mapStore.ts`).
Per FRONTEND.md: "Server state belongs in TanStack Query; avoid duplicating it in global state."

## ADR-009: React StrictMode + Leaflet Load Event
Leaflet's `load` event fires only once. Under React StrictMode double-mount, the second mount's listener never fires. Solution: use `useMap()` + `useEffect` to sync state on mount instead of relying on `load`.

## ADR-010: Map Container Positioning
Map uses `absolute inset-0` within a `relative` parent (not `fixed`). Navbar uses `sticky top-0 z-50` to render above the map. Map wrapper has `isolate` class to contain Leaflet's internal high z-index panes.

## ADR-013: Idempotent Event Processing
User-service stores a hash of each processed event in a `processed_events` table. Before processing, the consumer checks if the hash exists. If yes, the event is skipped. This prevents double-counting when RabbitMQ redelivers unacknowledged messages after a consumer crash.
