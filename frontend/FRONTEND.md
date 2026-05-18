# Frontend Guidelines

## Components
- Prefer small, focused components; compose over monoliths.
- Co-locate page-specific components, hooks, types, and helpers with the page.

## Pages
- All pages live in `src/pages/`.
- If a page needs multiple components, create `src/pages/<page-name>/` and place `index.tsx` as the page entry.
- Keep page-only data and logic inside the page folder (not `shared`).

## Shared vs Reusable
- `src/components/` is for truly reusable UI components; design for reuse.
- `src/shared/` is for cross-page, non-UI code (types, hooks, utilities, constants).
- If it is not shared across pages, keep it local to the page.

## State & Data
- Props first; use Zustand only for cross-page or cross-tree state.
- Server state belongs in TanStack Query; avoid duplicating it in global state.

## React Practices
- Use functional components and hooks.
- Keep dependencies explicit via props.
- Prefer stable selectors like `data-testid` for UI tests.
