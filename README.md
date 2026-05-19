# SaLoB — Singapore Local Food Price Intelligence Platform

A crowdsourced platform tracking historical food price developments in Singapore. Users submit what they paid at hawker centres and eateries; a trust-weighted confidence algorithm computes consensus prices; everyone can see how prices trend over time.

**Status:** Active development (localhost only, env-parity config for future deployment).

---

## Quick Start

### Prerequisites

- Java 25+ (Temurin recommended)
- Node.js 22+
- Python 3.12+ (for pre-commit hooks)
- Docker & Docker Compose

### 1. Clone & First-Time Setup

```bash
git clone <repo-url> && cd SaLoB

# Copy env files (each service needs one)
for dir in backend/user-service backend/food-service backend/api-gateway backend/rabbitmq backend/minio; do
  cp "$dir/.env.example" "$dir/.env"
done

# Install pre-commit hooks
pip3 install --user --break-system-packages pre-commit
pre-commit install --hook-type pre-commit --hook-type pre-push
```

### 2. Start Infrastructure

```bash
cd backend
docker compose up -d
```

This starts: PostgreSQL ×2, PostGIS, Redis ×3, RabbitMQ, MinIO.

### 3. Seed & Start Backend Services

**Order matters** — food-service queries user-service via gRPC during seeding.

```bash
# Terminal 1: User Service (seeds users)
cd backend/user-service
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

# Terminal 2: Food Service (seeds eateries, foods, entries — waits for user-service gRPC)
cd backend/food-service
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

# Terminal 3: API Gateway
cd backend/api-gateway
./gradlew bootRun
```

Seeding takes 30–60 seconds per service on first run.

### 4. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Opens at `http://localhost:3000`.

---

## Project Structure

```
SaLoB/
├── backend/
│   ├── api-gateway/       # Spring Cloud Gateway (port 8081)
│   ├── user-service/      # Auth, profiles, WTF scores (port 8082, gRPC 9092)
│   ├── food-service/      # Eateries, foods, entries, votes (port 8083, gRPC 9093)
│   ├── shared-proto/      # Protobuf contracts for gRPC
│   ├── k8s/               # Kubernetes manifests + Kustomize
│   ├── docker-compose.yaml
│   └── run.py             # Launcher (opens terminals per service)
├── frontend/
│   ├── src/
│   │   ├── pages/         # Route-level page components
│   │   ├── components/    # Reusable UI components
│   │   └── shared/        # Hooks, types, utils, MSW mocks
│   ├── tests/             # Playwright E2E smoke tests
│   └── public/            # Static assets + MSW service worker
├── docs/
│   ├── PRD.md             # Full product requirements
│   ├── PROGRESS.md        # Session log & key decisions
│   ├── ADR.md             # Architectural decision records
│   └── ROADMAP.md         # High-level roadmap
├── misc/                  # Pseudocode, sketches
├── .pre-commit-config.yaml
├── .secrets.baseline
└── AGENTS.md              # AI agent workflow instructions
```

---

## Development Philosophy

1. **Fail fast, no silent defaults** — Application YAML has no `${VAR:default}` fallbacks. Missing env vars crash immediately.
2. **Env parity** — All config reads from `.env` files (gitignored). `.env.example` is the team contract for required variables.
3. **Deterministic builds** — Pre-commit hooks are pinned to exact versions (`rev: vX.Y.Z`, never `latest`).
4. **Test with MSW** — Frontend tests use Mock Service Worker (not live backends). Fast, deterministic, offline-friendly.
5. **Microservices for learning** — Architecture is intentionally over-engineered as a portfolio piece demonstrating Spring Boot, gRPC, RabbitMQ, PostGIS, Redis, and container orchestration.

---

## Pre-commit Hooks

All hooks are defined in `.pre-commit-config.yaml` with pinned revisions for deterministic behavior.

| Hook | Stage | What it checks |
|---|---|---|
| trailing-whitespace | commit | Trims trailing whitespace |
| end-of-file-fixer | commit | Ensures files end with newline |
| check-yaml / check-json | commit | Validates syntax |
| check-merge-conflict | commit | Detects unresolved merge markers |
| detect-private-key | commit | Blocks accidental key commits |
| no-commit-to-branch | commit | Prevents direct commits to `main` |
| check-added-large-files | commit | Blocks files >512KB |
| ruff (lint + format) | commit | Python linting & formatting |
| detect-secrets | commit | Scans for secrets against `.secrets.baseline` |
| shellcheck | commit | Shell script validation |
| backend-compile | pre-push | Gradle `compileJava` in all 3 services |
| frontend-typecheck | pre-push | `tsc --noEmit` |
| frontend-lint | pre-push | ESLint |

To run manually:

```bash
pre-commit run --all-files       # all hooks
pre-commit run detect-secrets    # single hook
```

---

## Testing

### Backend
- **Unit tests:** `./gradlew test` per service
- **Integration tests:** Requires Testcontainers (see AGENTS.md)
- **Compile check:** `./gradlew compileJava` per service (shared-proto must be published to MavenLocal first)

### Frontend
- **Type check:** `npx tsc --noEmit` from `frontend/`
- **E2E smoke tests:** `npm run test` (Playwright, requires dev server)
- **MSW:** All HTTP requests in tests are intercepted by Mock Service Worker. Handlers in `src/shared/test/mocks/`.

### Seeding Order
Backend seeders have a hard dependency: **user-service must seed BEFORE food-service** (food-service queries user-service via gRPC for user IDs during seeding).

---

## Config & Secrets

| File | Git status | Purpose |
|---|---|---|
| `.env.example` | Tracked | Template with dev defaults (safe) |
| `.env` | Ignored | Actual secrets per developer (never commit) |
| `.secrets.baseline` | Tracked | detect-secrets allowlist baseline |

Each backend service reads its `.env` via `spring.config.import: optional:file:.env[.properties]`.
