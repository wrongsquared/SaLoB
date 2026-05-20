# SaLoB - SG Crowdsourced Price Intelligence Platform

## Workflow
Before implementing any feature or fixing any bug, follow this workflow:
1. **Git Branch**: Create a new branch from `main` named `<prefix>/<name>`, where `<prefix>` is `feature`, `fix`, `docs`, `chore`, etc...
2. **Plan:** Outline your approach, files to modify, risks, and dependencies.
  a. CLARIFY ANY AND ALL UNCERTAINTIES BEFORE CODING. If you can think of anything that might be a possible point of contention, ask about it. This includes design decisions, API contracts, data models, etc... Better to ask upfront than to implement something that needs to be reworked.
  b. **API contracts**: Before writing any frontend query or backend endpoint, read `docs/api-spec.yaml`. The OpenAPI spec is the single source of truth for URL paths, HTTP methods, parameters, request bodies, and response shapes. After making changes, update the spec to reflect them.
  c. When planning the frontend, refer to `frontend/FRONTEND.md` for guidelines, and `docs/moodboard` for UI inspiration and design patterns.
3. **Execute & Iterate:** Implement the plan. If checks fail, diagnose, update the plan, and re-execute.
  a. The frontend should refer to `/frontend/FRONTEND.md` and the backend to `/backend/BACKEND.md` for specific guidelines.
  b. Always keep security & logging in mind (see sections below).
4. **Document:** Update PRD/ADR/ROADMAP as needed and record key decisions in `docs/PROGRESS.md`.
  a. PRD can be found at `docs/PRD.md`, ADRs at `docs/ADRS.md`, and the roadmap at `docs/ROADMAP.md`.
5. **Write Tests:** Create or update unit, integration, and UI tests for new behavior.
   a. Integration tests should use Testcontainers, and UI changes require Playwright smoke tests with screenshots.
   b. For frontend tests, prefer MSW (Mock Service Worker) over hitting live backends — it's deterministic, fast, and works offline.
   c. Seeded data order: user-service must seed BEFORE food-service (food-service queries user-service via gRPC for user IDs).
   d. Add screenshot tests for every new screen or UI state. Run `npx playwright test --update-snapshots` to create baselines. The CI will compare against these.
6. **Verify:** Run tests and formatting checks. If failures persist, return to step 2.
   a. ALWAYS run `pre-commit run --all-files` before any commit.
   b. Pre-commit hooks are pinned to specific versions for deterministic behavior.
   c. To compile-check backend: `./gradlew compileJava` in each service (shared-proto must be published to MavenLocal first).
   d. To type-check frontend: `npx tsc --noEmit` from `frontend/`.
   e. Pre-push hooks automatically compile backend and type-check frontend.
   f. **Visual verification**: After any frontend change, check your work visually. Run the dev server (`npm run dev`), open the browser, and verify the UI matches expectations. Run `npx playwright test` to verify smoke and screenshot tests.
7. **Commit & PR:** Commit with a clear message and open a PR describing the change, rationale, and context.
   a. Always run pre-commit checks first (hooks run automatically on `git commit`).
   b. Scan the repository for leaked secrets, JWTs, tokens, etc... (`detect-secrets` baseline is in `.secrets.baseline`).
   c. Messages: `type(scope): description` (`feature|fix|docs|style|refactor|test|chore`).
   d. PRs describe changes, rationale, context.
   e. Use a lightweight subagent for PR review (quality, architecture, API contract).

## Security
- Always keep the OWASP Top 10 in mind when designing and implementing features.
- All npm packages MUST be pinned to exact versions in package.json (no `^`/`~` ranges) to prevent accidental upgrades that may introduce breaking changes or vulnerabilities.
- All API responses set security headers.
- Secrets via env only; never hardcode.
- Public endpoints require rate limiting + input validation.
- Enforce authn/authz server-side; never trust the client.
- Never log secrets or sensitive data; redact if needed.

## Logging
- Log key actions/outcomes for monitoring (no secrets).
- Never read full logs (burns tokens); use `tail` or `grep -C`.

## Guardrails
- Ask before undocumented dependency overrides, API breaks, or access keys.
- 3-strike rule: Stop and request direction.
- No new MD files post-feature; update existing docs.
- Favor optimistic UI for user actions (show success state immediately, revert on error). Examples: "Report as Closed" button, vote counts, submission confirmations.

## Playwright + MSW Configuration
- MSW must be awaited before `createRoot()`. Fire-and-forget `enableMocking()` causes race conditions where API calls escape to the network before the service worker is ready.
- In `playwright.config.ts`, set MSW via `webServer.env: { VITE_ENABLE_MSW: "true" }` — shell prefix syntax (`VITE_ENABLE_MSW=true npm run dev`) fails in Playwright's child process.
- Vite defaults to port 5173. If Playwright expects port 3000, add `server: { port: 3000 }` to `vite.config.ts`.
- Pre-commit `check-added-large-files` default is 512KB. Playwright screenshots are ~1MB each. Raise to `--maxkb=3072` in `.pre-commit-config.yaml`.

## Backend DTOs
- Java records are positional — adding a field changes constructor parameter order. Always update the service builder call to match the new field order.

# Re-iteration ad-nauseam, because this is the most critical part of the process:
- If you have any doubts - ANY DOUBTS - about the implementation, design, API contract, data model, etc... ASK BEFORE CODING. This is critical to avoid rework and ensure alignment.
