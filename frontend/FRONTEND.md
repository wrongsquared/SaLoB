# Frontend Guidelines

## API Contracts
- All endpoints documented in `docs/api-spec.yaml` — read it before writing/modifying any API call.
- Frontend query URLs must exactly match the OpenAPI spec paths. The spec is the single source of truth.

## Components & Pages
- Prefer small, focused components; compose over monoliths.
- Co-locate page-specific components, hooks, types, and helpers with the page.
- Page files in `src/pages/`. Multi-component pages → `src/pages/<page-name>/` with `index.tsx` as entry.
- `src/components/` for truly reusable UI components; design for reuse.
- `src/shared/` for cross-page, non-UI code (types, hooks, utilities, constants).
- Keep page-only data and logic in the page folder, not shared.

## State & Data
- Props first; Zustand only for cross-page or cross-tree state.
- Server state belongs in TanStack Query — avoid duplicating in global state.
- Zustand: use selectors (`useStore((s) => s.field)`) instead of destructuring the full store to prevent unnecessary re-renders.

## Query Keys (`qk` / `QK` constants)
- All query keys are defined at the top of `src/shared/api/queries.ts` as two exports:
  - **`QK` (uppercase):** prefix-only arrays for invalidation (`queryClient.invalidateQueries({ queryKey: QK.EATERIES_DETAIL })`).
  - **`qk` (lowercase):** factory functions returning the full key including params, for `useQuery({ queryKey: qk.eateryDetail(id) })`.
- Bounds parameters are rounded to 2 decimal places in the key to avoid cache misses from floating-point drift.

## Error Handling Convention
- **`mutationFn`** — chain sequential API calls here. If any call throws, the mutation fails and the store is untouched on that step.
- The **token must be saved inside `mutationFn`** when a subsequent call depends on it (e.g. login → save token → `/users/me`). This is a necessary side effect — if `/users/me` fails, the token alone is a safe partial state because `AuthInitializer` in `App.tsx` will retry the user fetch.
- **`onSuccess`** — cache invalidation, navigation, final state that's safe to skip on partial failure.
- **Successive API calls** (e.g. register → login → fetch user) are chained in `mutationFn`. If any step throws, the whole mutation fails and steps after the throw are skipped — no partial state leaks.
- Components display errors via `mutation.error` or try/catch on `mutateAsync`.

## Design & UX
- Mobile-first responsive design. Test at 375px, 768px, and 1440px.
- Every view handles: loading (skeleton/spinner), empty (meaningful message), and error (retry or graceful fallback).
- Pages must not be zoomable — `user-scalable=no, maximum-scale=1.0` in viewport meta; disable Leaflet double-click zoom.
- Favor optimistic UI: show success immediately on user actions, revert on error (e.g., "Report as Closed" toggle, vote counts).

## React Practices
- Keep dependencies explicit via props.
- Prefer stable selectors like `data-testid` for UI tests.
- Avoid infinite loops: event handlers updating global state must guard against re-triggering themselves. Compare old/new values before setting state.

## Testing
- Every UI change needs Playwright smoke tests asserting key elements are visible.
- Screenshot tests for new screens/UI states. Use `maxDiffPixels: 1000` — map tile rendering varies between runs.
- `npx playwright test --update-snapshots` to create/update baseline images when layout intentionally changes.
- `getByRole('dialog')` throws strict-mode violations with multiple dialogs. Always scope: `getByRole('dialog', { name: 'Specific Name' })`.

## OneMap Search Flow
- StepEatery calls `GET /api/eateries/search/combined?search=...` which returns `{ local: [...], onemap: [...] }`.
- OneMap results show a "New" badge. Clicking one calls `POST /api/eateries` with `{ name, address, typeId }`.
- The frontend fetches eatery types via `GET /api/eatery-types` on mount and uses the first type as default.
- The backend deduplicates by name before creating — if the same name already exists locally, it returns the existing eatery.

## UI Components & Styling
- shadCN components at `src/components/ui/`. Install via `npx shadcn@latest add <component>`.
- Use semantic Tailwind colors: `primary`, `secondary`, `accent`, `destructive`, `muted` — never hardcode hex values.
- Only components actually used should be installed (no bulk installs).

## Leaflet + React StrictMode
- Leaflet's `load` event fires once. Under StrictMode double-mount, the second listener never fires. Use `useMap()` + `useEffect` to sync state on mount instead of `load`.
- Navbar: `sticky top-0 z-50` to render above the map container. Without it, `absolute`/`fixed` map overlays cover the navbar.
- Map container: `absolute inset-0` within a `relative` parent (not `fixed`). Add `isolate` class to contain Leaflet's high z-index panes.
