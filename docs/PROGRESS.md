# Progress

## Session 2026-05-19 — Architecture Alignment + Pre-commit Hooks + MSW
- ConfidenceAlgorithm: batch gRPC WTF fetch (was N+1 individual calls)
- Env standardization: `.env.local`/`.env.k8s` → `.env` across all 8 directories + `.env.example` files
- Pre-commit: 12 pinned hooks (trailing-whitespace, detect-secrets, ruff, shellcheck, etc.)
- Pre-push: backend-compile (Gradle), frontend-typecheck (tsc), frontend-lint (eslint)
- MSW: handlers for all 10 endpoints (Singapore mock data), node/browser workers, Playwright config + 3 smoke tests
- Root `.gitignore`, `README.md`, `.secrets.baseline`
- **Fix**: no-commit-to-branch was running during pre-push → added `stages: [pre-commit]`
- **Fix**: pre-push used `npx` → switched to `./node_modules/.bin/` binaries (nvm PATH issue)

## Session 2026-05-19 — Backend Unit Tests (46 tests)
- food-service: 32 tests (ConfidenceAlgorithm, EateryService, FoodService, FoodEntryService, Controllers)
- user-service: 14 tests (AuthService, UserService, Controllers)
- api-gateway: 5 tests (TokenRelayFilter unit + context-load)
- Discovered SB4 removals: `@WebMvcTest`, `@MockitoBean`, `@AutoConfigureWebTestClient` → use standalone MockMvc + `@ExtendWith(MockitoExtension.class)`
- Fix: `UserServiceApplicationTests` → `spring.main.allow-bean-definition-overriding=true` (net.devh gRPC vs SB4 built-in)

## Session 2026-05-19 — HomePage (Branch: feature/homepage, awaiting merge)
- Full-screen Leaflet map with clustering, eatery/food mode toggle, collapsible left-sidebar panel
- Food tag picker (search + chips, max 5), debounced bounds queries (300ms), MarkerLayers with type-colored DivIcons
- FoodEntryDetailPage with submitter info + historical entries
- Shared infra: axios client (`src/shared/api/client.ts`), 7 TanStack Query hooks (`src/shared/api/queries.ts`),
  Zustand map store (`src/stores/mapStore.ts`), useDebounce hook, API types (`src/shared/types/api.ts`)
- 8 ADRs in `docs/ADR.md`
- Review fixes applied: error states, ARIA, sidebar close on mode switch, onClick wired, extracted shared utils
- react-dom version 19.2.6 synced with react 19.2.6 (was mismatched)

## Open Branches
- `feature/homepage` — awaiting user merge to `main`
- `feature/api-gateway-tests` — awaiting user merge to `main`
