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
- **Submission wizard**: multi-step overlay (search eatery → search food → enter price)
- **OneMap integration**: replace OSM tiles with Singapore-specific OneMap
- **Settings page**: user profile editing, preferences
- **Login/Auth**: wire Google OAuth, JWT token management
- **Historical data charts**: price trend visualization on food entry detail page
- **Live voting**: RabbitMQ WebSocket for real-time upvote/downvote

## Later
- Admin dashboard
- Mobile responsive layout
- Backend search improvements (full-text, fuzzy)
- Receipt upload (currently out-of-scope per PRD)
- Containerized deployment (was removed from docker-compose)
