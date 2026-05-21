# SaLoB - SG Crowdsourced Price Intelligence Platform

## Workflow
1. **Branch:** Create from `main` as `<prefix>/<name>` (`feature`, `fix`, `docs`, `chore`, ...).
2. **Plan:** Outline approach, files, risks, dependencies.
   a. **ASK BEFORE CODING** — Clarify design, API contracts, data models, etc. upfront to avoid rework.
   b. **API contracts**: Read `docs/api-spec.yaml` before writing queries/endpoints; update it after changes.
   c. Frontend → `frontend/FRONTEND.md` + `docs/moodboard`. Backend → `backend/BACKEND.md`.
3. **Execute & Iterate:** Implement, diagnose failures, re-execute. Keep security & logging in mind (see sections below).
4. **Document:** Update `docs/PRD.md`, `docs/ADR.md`, `docs/ROADMAP.md`, `docs/TECHNICAL.md` as needed.
5. **Write Tests:** Unit, integration (Testcontainers), UI (Playwright + screenshots).
   a. Frontend: prefer MSW over live backends (see `frontend/FRONTEND.md` for Playwright + MSW setup).
   b. Seed order: user-service before food-service (gRPC dependency).
   c. Screenshot tests for new screens; `npx playwright test --update-snapshots` for baselines.
6. **Verify:**
   a. Run `pre-commit run --all-files` before commits.
   b. Compile-check: `./gradlew compileJava` per service (publish shared-proto to MavenLocal first).
   c. Type-check: `npx tsc --noEmit` from `frontend/`.
   d. Run dev server, verify UI, then `npx playwright test`.
7. **Commit & PR:**
   a. Pre-commit hooks run automatically on `git commit`.
   b. Scan for secrets (`detect-secrets` baseline in `.secrets.baseline`).
   c. Message format: `type(scope): description`.
   d. PR describes changes, rationale, context.
   e. Use subagent for PR review (quality, architecture, API contract).

## Security
- OWASP Top 10 awareness.
- Pin npm packages to exact versions (no `^`/`~`).
- Security headers on all API responses.
- Secrets via env only; never hardcode or log.
- Rate-limit + validate public endpoints.
- Authn/authz server-side; never trust the client.

## Logging
- Log key actions for monitoring; never log secrets.
- Prefer `tail` or `grep -C` over reading full logs.

## Guardrails
- Ask before: undocumented dependency overrides, API breaks, access keys.
- 3-strike rule: stop and request direction.
- No new MD files post-feature; update existing docs.
- Optimistic UI for user actions (show success immediately, revert on error).

## Backend DTOs
- Java records are positional — field order changes constructor parameter order. Update builder calls accordingly.
