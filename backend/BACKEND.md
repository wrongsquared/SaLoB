# Backend Guidelines

## Structure
- Each service lives in its own folder (`api-gateway`, `food-service`, `user-service`, etc...); keep boundaries strict.
- Shared contracts live in `shared-proto`; avoid shared business logic across services.

## Build & Run
- Use the Gradle wrapper (`./gradlew`) per service.
- Keep Dockerfiles in sync with the service build.
- Local infra dependencies belong in `backend/docker-compose.yaml`.

## APIs & Data
- Validate input at the service boundary.
- Keep API changes backward compatible; ask before breaking changes.

## Config & Security
- Config via env; never commit secrets.
- Enforce authn/authz server-side.

## Infrastructure
- Kubernetes manifests live in `backend/k8s/`; keep changes declarative.
