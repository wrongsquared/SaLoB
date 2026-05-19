# Roadmap

## Done
- Pre-commit hooks (+ MSW, detect-secrets, env standardization)
- Backend unit tests (food-service: 32, user-service: 14, api-gateway: 5)
- HomePage with Leaflet map, marker clustering, eatery/food mode toggle, collapsible sidebar, food tag picker
- Food entry detail page (basic layout)
- Shared infra: TanStack Query, Zustand stores, API types, axios client
- 8 ADRs documenting architecture decisions

## In Progress
- `feature/homepage` branch awaiting merge into `main`

## Next
- **Submission wizard** (in progress on `feature/homepage`):
  - Multi-step overlay: search eatery → search food (free-text fallback) → enter price → confirm
  - FAB on HomePage + "Submit price" button in EateryPanel
- **OneMap integration**: replace OSM tiles with Singapore-specific OneMap
- **Settings page**: user profile editing, preferences
- **Login/Auth**: wire Google OAuth, JWT token management

## Later
- **Food moderation system**: Admin page to approve/reject user-submitted foods (RBAC with existing role system)
- **Rate limiting**: Per-user cap on food submissions (prevent spam)
- **AI verification pipeline**: Confidence scoring for user-submitted foods (cheap AI check on food name legitimacy)
- Admin dashboard (approve/reject foods, user management)
- Historical data charts (price trend visualization on food entry detail page)
- Live voting (RabbitMQ WebSocket for real-time upvote/downvote)
- Mobile responsive layout
- Backend search improvements (full-text, fuzzy)
