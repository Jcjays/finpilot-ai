# Implementation Status

Updated after every milestone (and after meaningful increments within one). Nothing below is marked done unless the build and tests were actually run.

## Milestones

| Milestone | Status | Notes |
|---|---|---|
| 0 — Planning | ✅ Done (2026-10-17) | Environment inspected, free-tier limits and library compatibility verified, architecture/schema/API/screens approved. See [PROJECT_PLAN.md](PROJECT_PLAN.md). |
| 1 — Foundation | ✅ Done (2026-10-17) | Repo `Jcjays/finpilot-ai` (public), Ktor and Android skeletons, Docker Compose, CI green |
| 2 — Database and Authentication | ✅ Done (2026-10-17) | Flyway schema with forced RLS, Supabase JWT verification, `/me` profiles, Android sign-in/sign-up, end-to-end verified against the local Supabase stack |
| 3 — Core Finance | ⬜ Not started | |
| 4 — Receipt Intelligence | ⬜ Not started | |
| 5 — AI Assistant | ⬜ Not started | |
| 6 — Savings Planner | ⬜ Not started | |
| 7 — Security and Quality | ⬜ Not started | |
| 8 — Portfolio Release | ⬜ Not started | |

## Latest verified state (2026-10-17)

- **Latest code commit:** `1f0b6a5` — `docs: document the Windows Supabase CLI workaround and pin local config to CLI 2.95.4` (this status update follows it)
- **Pushed to GitHub:** yes — https://github.com/Jcjays/finpilot-ai, `origin/main` = `1f0b6a5`
- **GitHub Actions:** Backend CI, Android CI and Security green on every push through `a8012e0` (the keepalive workflow is skipped until the `SUPABASE_URL` repository variable exists). Runs for `0de32c3`/`1f0b6a5` were in progress at the time of writing.
- **Backend** (`backend/`): `./gradlew test detekt` → 45 tests passed (3 health/OpenAPI, 11 JWT verifier, 8 `/me` route with database, 6 migration, 7 row-level security, 4 profile repository, 4 migrate CLI, 2 profile service), 0 detekt issues. Smoke on a real server: `/health` reports `db: ok`, `/api/v1/me` returns the persisted profile for a valid token (idempotent), 401 without one.
- **Android** (`android/`): `./gradlew testDebugUnitTest detekt :app:lintDebug :app:assembleDebug` → 20 unit tests passed (MVI base 3, sign-in ViewModel 6, dashboard ViewModel 4, bearer interceptor 4, token authenticator 4), detekt and Android Lint clean, debug APK built.
- **End to end** (emulator Pixel 9 Pro API 34, local Supabase stack, backend on 8080, Compose Postgres): instrumented `SignUpToDashboardE2ETest` passed — a new account was created in Supabase Auth, the app called `GET /api/v1/me` with the issued token, the backend verified it and created the profile row, and the dashboard displayed the signed-in email with currency PHP.

## Completed work

### Milestone 2 — Database and Authentication (2026-10-17)
- **Schema:** Flyway `V1__init.sql` creates the twelve approved tables (minor-unit money, text enums with CHECKs, soft deletes, transfer/refund invariants, `updated_at` triggers), the least-privilege `finpilot_api` role (no DELETE on content, append-only audit log), and forced Row Level Security with one policy shape that works for both the backend (`app.current_user_id` set per transaction) and PostgREST (JWT claims). `V2__seed_categories.sql` seeds 28 Philippine categories with fixed ids. `./gradlew :data:migrate` applies migrations with schema-owner credentials.
- **Backend auth:** `SupabaseJwtVerifierFactory` verifies ES256/RS256 tokens against the project JWKS (cached 10 minutes, keyed by `kid`) with issuer, audience, role and expiry checks; the legacy HS256 secret is accepted only when `APP_ENV=local`, and startup fails if it is set elsewhere. `GET /api/v1/me` creates the caller's profile on first sign-in through `ProfileService` → `Transactor.withUser` (Exposed, sets the RLS context) → `ProfileRepository`. `/health` reports database status; a database is mandatory in prod.
- **Android auth:** `core:network` with an `AuthRepository` over supabase-kt (session state, sign-in, sign-up, sign-out, refresh), a bearer-token interceptor and one-shot 401 authenticator scoped to the FinPilot API host, and a Retrofit client; `feature:auth` sign-in/sign-up screen with an MVI ViewModel; session gate at the app root; dashboard shows the profile from `/me` and offers sign-out; BuildConfig endpoints from `local.properties` with emulator defaults; debug-only cleartext policy for the emulator host.
- **Tests:** written before implementation at each step (migrations, RLS, verifier, routes, repository, ViewModels, interceptor, authenticator), plus the instrumented end-to-end test, which skips itself when no local backend is reachable.
- **Docs and ops:** [docs/supabase-setup.md](docs/supabase-setup.md) runbook (local stack and hosted Free project, session pooler, ES256 keys, API role password), `supabase/config.toml`, and a keepalive workflow for the hosted project.

### Milestone 1 — Foundation (2026-10-17)
- GitHub CLI installed; repository initialised with `.gitignore`, LF `.gitattributes`, `.editorconfig`, MIT `LICENSE`, `.env.example`, README and plan.
- Ktor backend skeleton (`app`, `api`, `domain`, `data`, `ai`) with Koin, request ids, JSON envelopes, StatusPages, `/health`, generated `/openapi.json` + Swagger UI, detekt; Docker Compose Postgres 17 with role initialisation.
- Android skeleton (`app`, `core:ui`, `core:testing`, `feature:dashboard`) with Compose BOM 2026.09.00, Hilt via KSP, Navigation 3, emerald Material 3 theme, `MviViewModel` base, detekt; CI workflows for backend, Android and gitleaks.

### Milestone 0 — Planning (2026-10-17)
- Environment inspected; Supabase Free limits, Anthropic API billing, GitHub Actions allowances and the Figma kit licence verified; compatible toolchain pinned; schema, REST contract, calculation rules, AI tool loop and evaluation harness designed.

## Known issues

- **Supabase CLI on this Windows machine:** releases from v2.100 ship a launcher that needs `taskkill.exe`, which this user account cannot run; the local stack is driven with the last pure-Go release (v2.95.4), and two newer `config.toml` entries (`db.orioledb_version`, `[local_smtp]`) are commented out for it. Documented in the runbook.
- **Test role password is cluster-wide:** integration tests set `finpilot_api` to the Compose dev password; running them against a database shared with another environment would change that role's password.
- **compileSdk 37.1 required** by Compose 1.12 and Navigation 3 1.2; SDK Platform 37.1 must be installed.
- **Android Studio regenerates `gradle/gradle-daemon-jvm.properties`** (JDK 25); it is git-ignored and detekt's JVM target is pinned to 17 so builds stay green either way.
- Instrumented tests are not part of CI (nightly `workflow_dispatch` only); the E2E test needs the local stack.

## Next task

**Milestone 3 — Core Finance:** accounts and categories endpoints, transactions CRUD with cursor pagination and idempotency keys, deterministic calculation module with edge-case tests (Asia/Manila month boundaries, refunds, transfers, pending), `GET /finance/summary` and `/finance/category-breakdown`, budgets, Room cache with a transaction outbox and `SyncWorker`, and the dashboard, transaction list, editor, details and budgets screens.
