# Implementation Status

Updated after every milestone (and after meaningful increments within one). Nothing below is marked done unless the build and tests were actually run.

## Milestones

| Milestone | Status | Notes |
|---|---|---|
| 0 — Planning | ✅ Done (2026-10-17) | Environment inspected, free-tier limits and library compatibility verified, architecture/schema/API/screens approved. See [PROJECT_PLAN.md](PROJECT_PLAN.md). |
| 1 — Foundation | 🟡 Built and verified locally; **push and CI run pending GitHub sign-in** | Repo, tooling, Ktor and Android skeletons, Docker Compose, CI workflows, docs |
| 2 — Database and Authentication | ⬜ Not started | |
| 3 — Core Finance | ⬜ Not started | |
| 4 — Receipt Intelligence | ⬜ Not started | |
| 5 — AI Assistant | ⬜ Not started | |
| 6 — Savings Planner | ⬜ Not started | |
| 7 — Security and Quality | ⬜ Not started | |
| 8 — Portfolio Release | ⬜ Not started | |

## Latest verified state (2026-10-17)

- **Latest code commit:** `14c9771` — `ci: add Android lint, unit test and APK checks` (this docs update follows it)
- **Pushed to GitHub:** not yet — `gh auth login` is waiting for the account owner to complete the browser flow; the repository `Jcjays/finpilot-ai` has not been created yet
- **Backend** (`backend/`, Kotlin 2.3.21, Ktor 3.6.0, Gradle 9.3.1): `./gradlew test detekt` → 3 tests passed, 0 detekt issues; `:app:buildFatJar` → `app-all.jar` (55 MB); smoke run on port 18080: `GET /health` 200 with `X-Request-Id`, `GET /openapi.json` 200 listing `/health`, `GET /swagger` 200, unknown route 404 with error envelope
- **Android** (`android/`, Kotlin 2.4.21, AGP 9.3.3, Gradle 9.5.0, compileSdk 37.1): `./gradlew testDebugUnitTest detekt :app:lintDebug :app:assembleDebug` → 5 unit tests passed (3 `MviViewModelTest`, 2 `DashboardViewModelTest`), 0 detekt issues, Android Lint clean, `app-debug.apk` (11.9 MB)
- **CI:** `backend-ci.yml`, `android-ci.yml`, `security.yml` (gitleaks) committed; not yet executed on GitHub Actions
- **Not yet verified:** app launch on an emulator (planned for the Milestone 2 sign-in flow), Docker Compose Postgres (Docker daemon not running on this machine during the session)

## Completed work

### Milestone 1 — Foundation (2026-10-17)
- Installed GitHub CLI 2.102.0 via winget; initialised git on `main`; added `.gitignore`, `.gitattributes` (LF), `.editorconfig`, MIT `LICENSE`, `.env.example`, `README.md`, `PROJECT_PLAN.md`.
- **Backend skeleton:** five Gradle modules (`app`, `api`, `domain`, `data`, `ai`); Ktor server with Koin, typed `AppConfig` (env + AI provider), request ids (`X-Request-Id`) with MDC logging, JSON success/error envelopes, StatusPages mapping (404/405/400/500), `/health`, OpenAPI document generated from the routing tree at `/openapi.json` plus Swagger UI at `/swagger`, Ktor OpenAPI compiler extension enabled, detekt, route tests written first (RED) then implemented (GREEN).
- **Docker Compose:** `postgres:17-alpine` with init script creating the least-privilege `finpilot_api` role and a `finpilot_test` database.
- **Android skeleton:** `app`, `core:ui` (emerald Material 3 theme, `MviViewModel` base, Navigation 3 route keys), `core:testing` (shared dispatcher rule), `feature:dashboard` (contract, Hilt ViewModel, stateless screen with previews); single `NavDisplay` back stack; Hilt via KSP under AGP 9 built-in Kotlin; detekt; tests written first for the MVI base and the dashboard ViewModel.
- **CI:** backend workflow (Postgres service container, detekt, tests, fat jar), Android workflow (SDK 37, detekt, unit tests, lint, APK artifact), gitleaks secret scanning with an allowlist for documented dev placeholders.

### Milestone 0 — Planning (2026-10-17)
- Inspected the Windows development environment (JDK 21, Android Studio Rabbit 2026.2.1, Android SDK, Docker Desktop, Git); verified Supabase Free limits, Anthropic API billing, GitHub Actions allowances, and the CC BY 4.0 licence of the Figma inspiration kit.
- Pinned a compatible toolchain and designed the schema, REST contract, deterministic calculation rules, AI tool loop, and evaluation harness.

## Known issues

- **GitHub push pending:** the interactive `gh auth login` must be completed by the account owner before the repository can be created and the five local commits pushed.
- **Docker Desktop did not start** when launched from the session (no daemon). Needed from Milestone 2 for local Postgres and Testcontainers; CI uses a Postgres service container instead.
- **compileSdk 37.1 required:** AndroidX Compose 1.12.x and Navigation 3 1.2.0 declare a minimum compileSdk of 37, so SDK Platform 37.1 must be installed (the plan assumed 36).
- `hiltViewModel()` for Compose now lives in `androidx.hilt:hilt-lifecycle-viewmodel-compose` (the navigation-compose variant is deprecated); the project uses the new artifact.

## Next task

Complete `gh auth login`, create `Jcjays/finpilot-ai` (public), push `main`, confirm the three workflows are green, then start **Milestone 2 — Database and Authentication**: Flyway `V1__init.sql` + seed categories, RLS policies and the `finpilot_api` role, Supabase JWT verification (JWKS + local HS256), `/me`, the Supabase project runbook, and Android sign-in/sign-up.
