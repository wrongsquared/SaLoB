# Roadmap

## Done
- Pre-commit hooks (+ MSW, detect-secrets, env standardization)
- Backend unit tests (food-service: 32, user-service: 14, api-gateway: 5)
- Shared infra: TanStack Query, Zustand stores, API types, axios client
- 8 ADRs documenting architecture decisions
- Food entry detail page (basic layout)

## In Progress
- `feature/homepage` branch — full homepage rebuild (map, panel, wizard, new backend endpoints)

## Next
- **OneMap integration**: replace OSM tiles with Singapore-specific OneMap
- **Settings page**: user profile editing, preferences
- **Login/Auth**: wire Google OAuth, JWT token management
- **AI-powered Intelligence Brief**: generate contextual price insights for eatery panels
- **AI food verification**: when users create new food names, run through AI legitimacy check before approval

## Later
- **Food moderation system**: Admin page to approve/reject user-submitted foods (RBAC with existing role system)
- **Rate limiting**: Per-user cap on food submissions (prevent spam)
- Admin dashboard (approve/reject foods, user management)
- Historical data charts (price trend visualization on food entry detail page)
- Live voting (RabbitMQ WebSocket for real-time upvote/downvote)
- Mobile responsive layout
- Backend search improvements (full-text, fuzzy)
