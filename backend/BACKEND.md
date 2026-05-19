# Backend Guidelines

## Structure
- Each service lives in its own folder (`api-gateway`, `food-service`, `user-service`, etc...); keep boundaries strict.
- Shared contracts live in `shared-proto`; avoid shared business logic across services.

## Build & Run
- Use the Gradle wrapper (`./gradlew`) per service.
- Keep Dockerfiles in sync with the service build.
- Local infra dependencies belong in `backend/docker-compose.yaml`.

## Seeding Order (Critical)
Services have a hard seeding dependency due to soft FKs via gRPC:

1. **Start infra:** `docker compose up -d`
2. **Create `.env` files:** `cp .env.example .env` in each service directory
3. **Seed user-service first:** Run with dev profile (`SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun`) — creates roles and users
4. **Seed food-service second:** Run with dev profile (`SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun`) — queries user-service via gRPC for user IDs, then seeds eateries, foods, entries, votes, and flags
5. **Start api-gateway:** `./gradlew bootRun` (no seed data needed)

**Never run food-service seeding before user-service seeding** — it will fail because no user IDs exist to reference.

## Testing

### Seeded test data approach
The seeders create realistic Singapore data (~70 eateries, 65 foods, randomized entries/votes). This is the primary testing strategy — run the services in dev profile and verify against live seeded data.

### MSW (Mock Service Worker)
Strongly recommended for frontend tests. MSW intercepts HTTP at the network level in both Vitest unit tests and Playwright E2E tests, allowing:
- No backend dependency for frontend tests
- Fast, deterministic responses
- Error state simulation
- Consistent fixtures across test runs

Add MSW setup:
```
npm install msw --save-dev
npx msw init public/
```

## APIs & Data
- Validate input at the service boundary.
- Keep API changes backward compatible; ask before breaking changes.

## Config & Security
- Config via env; never commit secrets.
- Enforce authn/authz server-side.

## Infrastructure
- Kubernetes manifests live in `backend/k8s/`; keep changes declarative.
