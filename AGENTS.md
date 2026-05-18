# SaLoB - SG Crowdsourced Price Intelligence Platform

## Workflow
Before implementing any feature or fixing any bug, follow this workflow:
1. **Git Branch**: Create a new branch from `main` named `<prefix>/<name>`, where `<prefix>` is `feature`, `fix`, `docs`, `chore`, etc...
2. **Plan:** Outline your approach, files to modify, risks, and dependencies.
  a. When planning the frontend, you can refer to the `docs/moodboard` directory for UI inspiration and design patterns.
3. **Execute & Iterate:** Implement the plan. If checks fail, diagnose, update the plan, and re-execute.
  a. The frontend should refer to `/frontend/FRONTEND.md` and the backend to `/backend/BACKEND.md` for specific guidelines.
  b. Always keep security & logging in mind (see sections below).
4. **Document:** Update PRD/ADR/ROADMAP as needed and record key decisions in `docs/PROGRESS.md`.
  a. PRD can be found at `docs/PRD.md`, ADRs at `docs/ADRS.md`, and the roadmap at `docs/ROADMAP.md`.
5. **Write Tests:** Create or update unit, integration, and UI tests for new behavior.
  a. Integration tests should use Testcontainers, and UI changes require Playwright smoke tests with screenshots.
6. **Verify:** Run tests and formatting checks. If failures persist, return to step 2.
7. **Commit & PR:** Commit with a clear message and open a PR describing the change, rationale, and context.
  a. Always run pre-commit checks first.
  b. Scan the repository for leaked secrets, JWTs, tokens, etc...
  c. Messages: `type(scope): description` (`feature|fix|docs|style|refactor|test|chore`).
  d. PRs describe changes, rationale, context.
  e. Use a lightweight subagent for PR review (quality, architecture, API contract).

## Security
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
