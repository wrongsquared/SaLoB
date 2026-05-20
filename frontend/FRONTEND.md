# Frontend Guidelines

## API Contracts
- All API endpoints are documented in `docs/api-spec.yaml`. Before writing or modifying any API call, read the spec for the exact URL, method, params, request body, and response shape.
- Frontend query URLs must exactly match the `paths` in the OpenAPI spec. The spec is the single source of truth.

## Components
- Prefer small, focused components; compose over monoliths.
- Co-locate page-specific components, hooks, types, and helpers with the page.

## Pages
- All pages live in `src/pages/`.
- If a page needs multiple components, create `src/pages/<page-name>/` and place `index.tsx` as the page entry.
- Keep page-only data and logic inside the page folder (not `shared`).

## Design Philosophy
- Keep responsive design in mind; Think about mobile-first and progressive enhancement. Test at 375px, 768px, and 1440px widths.
- Every view must handle loading state (skeleton/spinner), empty state (meaningful "no data" message), and error state (retry option or graceful fallback).
- Pages must not be zoomable. Add `user-scalable=no, maximum-scale=1.0` to viewport meta; disable Leaflet double-click zoom.
- Favor optimistic UI: show success state immediately on user actions, revert on error (e.g., "Report as Closed" toggle, vote counts).

## Shared vs Reusable
- `src/components/` is for truly reusable UI components; design for reuse.
- `src/shared/` is for cross-page, non-UI code (types, hooks, utilities, constants).
- If it is not shared across pages, keep it local to the page.

## State & Data
- Props first; use Zustand only for cross-page or cross-tree state.
- Server state belongs in TanStack Query; avoid duplicating it in global state.
- When using Zustand, always use selectors (`useStore((s) => s.field)`) instead of destructuring the full store. This prevents unnecessary re-renders when unrelated state fields change.

## React Practices
- Keep dependencies explicit via props.
- Prefer stable selectors like `data-testid` for UI tests.
- Avoid infinite loops: event handlers that update global state must guard against re-triggering themselves. Compare old/new values before setting state.

## Testing
- Every UI change requires Playwright smoke tests that assert key elements are visible.
- Add screenshot tests for new screens or UI states. Use `maxDiffPixels: 1000` — map tile rendering varies between runs.
- Run `npx playwright test --update-snapshots` to create/update baseline images when layout intentionally changes.
- `getByRole('dialog')` throws strict-mode violations when multiple dialogs exist. Always scope: `getByRole('dialog', { name: 'Specific Name' })`.

## Leaflet + React StrictMode
- Leaflet's `load` event fires only once. Under React StrictMode double-mount, the second mount's listener never fires. Use `useMap()` + `useEffect` to sync state on mount instead of relying on `load`.
- The navbar must have `sticky top-0 z-50` to render above the map container. Without it, `absolute`/`fixed` map overlays cover the navbar.
- Keep map container as `absolute inset-0` within a `relative` parent (not `fixed`). Add `isolate` class to contain Leaflet's internal high z-index panes.
