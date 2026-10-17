# Implementation Status

Updated after every milestone (and after meaningful increments within one). Nothing below is marked done unless the build and tests were actually run.

## Milestones

| Milestone | Status | Notes |
|---|---|---|
| 0 — Planning | ✅ Done (2026-10-17) | Environment inspected, free-tier limits and library compatibility verified, architecture/schema/API/screens approved. See [PROJECT_PLAN.md](PROJECT_PLAN.md). |
| 1 — Foundation | ✅ Done (2026-10-17) | Repo `Jcjays/finpilot-ai` created (public), `main` pushed, Backend CI / Android CI / Security all green on `58d0b1e` |
| 2 — Database and Authentication | 🚧 In progress | Supabase JWT verification, `/me`, Flyway schema with RLS, Android sign-in |
| 3 — Core Finance | ⬜ Not started | |
| 4 — Receipt Intelligence | ⬜ Not started | |
| 5 — AI Assistant | ⬜ Not started | |
| 6 — Savings Planner | ⬜ Not started | |
| 7 — Security and Quality | ⬜ Not started | |
| 8 — Portfolio Release | ⬜ Not started | |

## Latest verified state (2026-10-17)

- **Latest code commit:** `58d0b1e` — `ci: mark the workspace as a safe directory for the gitleaks container`
- **Pushed to GitHub:** yes — https://github.com/Jcjays/finpilot-ai (public), `origin/main` = `58d0b1e`
- **GitHub Actions on `58d0b1e`:** Backend CI ✅ (2m30s, run 39020822625), Android CI ✅ (4m11s, run 39021058291), Security ✅ (run 39021058299). Three earlier runs failed and were fixed: gitleaks first-push range bug (switched to the pinned gitleaks image over full history), SDK package name (`platforms;android-37.1`), container git ownership (`safe.directory`).
- **Backend** (`backend/`, Kotlin 2.3.21, Ktor 3.6.0, Gradle 9.3.1): `./gradlew test detekt` → 3 tests passed, 0 detekt issues; `:app:buildFatJar` → `app-all.jar` (55 MB); smoke run on port 18080: `GET /health` 200 with `X-Request-Id`, `GET /openapi.json` 200 listing `/health`, `GET /swagger` 200, unknown route 404 with error envelope
- **Android** (`android/`, Kotlin 2.4.21, AGP 9.3.3, Gradle 9.5.0, compileSdk 37.1): `./gradlew testDebugUnitTest detekt :app:lintDebug :app:assembleDebug` → 5 unit tests passed (3 `MviViewModelTest`, 2 `DashboardViewModelTest`), 0 detekt issues, Android Lint clean, `app-debug.apk` (11.9 MB)
- **CI:** `backend-ci.yml`, `android-ci.yml`, `security.yml` (gitleaks) green on GitHub Actions
- **Not yet verified:** app launch on an emulator (planned for the Milestone 2 sign-in flow)

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

- **Workflows with `paths` filters do not run on the very first push of a new branch**; the backend and Android workflows were dispatched manually once. Later pushes trigger them normally.
- **Docker Desktop** needed a second launch and about ten minutes before its Linux engine became reachable.
- **compileSdk 37.1 required:** AndroidX Compose 1.12.x and Navigation 3 1.2.0 declare a minimum compileSdk of 37, so SDK Platform 37.1 must be installed (the plan assumed 36).
- `hiltViewModel()` for Compose now lives in `androidx.hilt:hilt-lifecycle-viewmodel-compose` (the navigation-compose variant is deprecated); the project uses the new artifact.

## Next task

**Milestone 2 — Database and Authentication**: Supabase JWT verification (JWKS ES256/RS256 + local HS256) with `/me`, Flyway `V1__init.sql` + seed categories, RLS policies and the `finpilot_api` role with cross-user tests, the Supabase project runbook and keepalive workflow, and Android sign-in/sign-up with token refresh.
