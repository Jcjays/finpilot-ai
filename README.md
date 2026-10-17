# FinPilot AI

AI-powered personal finance for the Philippines: track income and expenses in PHP, scan receipts on-device, ask natural-language questions about your money, and review AI budget proposals before anything changes.

**Status:** Milestone 1 (Foundation) in progress — see [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md). Scope and architecture: [PROJECT_PLAN.md](PROJECT_PLAN.md).

> Portfolio project. Everything runs on free tools (local Docker, Supabase Free, GitHub Actions). The AI layer has a deterministic mock mode so development and CI cost nothing; the real Claude integration is optional and needs your own Anthropic API credits.

## What it does

| Area | Highlights |
|---|---|
| Transactions | Income / expense / transfer / refund with exact centavo arithmetic, categories, accounts, search and filters, offline add/edit with background sync |
| Dashboard | Monthly income, expenses, net cash flow, budget remaining, savings progress, category breakdown, month-over-month comparison — all computed deterministically on the backend |
| Receipt scanner | CameraX capture → on-device ML Kit OCR → Claude structured extraction → **you review before anything is saved** |
| Finance assistant | Claude answers questions through a controlled registry of six finance tools scoped to your account; every number comes from a tool result |
| Insights and planner | Backend computes the facts, Claude explains them; savings goals, deterministic what-if simulations, AI proposals that require explicit approval and are audit-logged |

## Technology

| Layer | Stack |
|---|---|
| Android | Kotlin, Jetpack Compose + Material 3, MVI (ViewModel + StateFlow), Navigation 3, Hilt, Room, Retrofit + kotlinx.serialization, WorkManager, CameraX, ML Kit Text Recognition |
| Backend | Kotlin, Ktor 3 (routing, auth-jwt, content negotiation, status pages, OpenAPI), Exposed, PostgreSQL, Flyway, HikariCP, Koin |
| Data and auth | Supabase PostgreSQL + Supabase Auth (JWT verified by the backend via JWKS), Row Level Security as defense-in-depth, local Postgres 17 via Docker Compose |
| AI | Anthropic Claude Messages API called with Ktor Client — structured outputs for extraction, strict tool use for the assistant, a deterministic mock implementation for tests and CI |
| Quality | JUnit 5, MockK, Ktor test host, Testcontainers, detekt, GitHub Actions, gitleaks, AI evaluation harness on synthetic data |

Architecture in one line: `Compose UI → ViewModel (MVI) → UseCase → Repository → Room / Retrofit` on the phone, and `Routes → Services → Domain (deterministic calculations) → Repositories → PostgreSQL` on the server, with Claude never touching the database directly.

## Repository layout

```
android/   Android app (independent Gradle build)
backend/   Ktor API (independent Gradle build): app, api, domain, data, ai
docs/      architecture notes, ADRs, exported OpenAPI, evaluation reports
supabase/  local Supabase CLI configuration (schema is owned by Flyway in backend/)
.github/   CI workflows
```

## Getting started

Prerequisites: JDK 21, Android Studio (Rabbit 2026.2.1 or newer) with SDK Platform 37.1 (the current AndroidX Compose and Navigation 3 releases require compileSdk 37), Docker Desktop, Git. Node 20+ is optional for the Supabase CLI via `npx`.

### Backend

```bash
cd backend
cp ../.env.example .env        # fill in values; .env is git-ignored
docker compose -f ../docker-compose.yml up -d
./gradlew run
```

Then open `http://localhost:8080/health` and the API docs at `http://localhost:8080/swagger` (`/openapi.json` for the raw spec).

### Android

1. Open `android/` in Android Studio.
2. Create `android/local.properties` with `sdk.dir=<path to your Android SDK>` (Android Studio does this for you).
3. Run the `app` configuration on an API 26+ emulator or device.

From the command line:

```bash
cd android
./gradlew testDebugUnitTest detekt :app:lintDebug :app:assembleDebug
```

### AI modes

| `AI_PROVIDER` | Behaviour |
|---|---|
| `mock` (default) | Deterministic fixtures; tool calls still execute against the real backend logic. Free. Used by CI. |
| `claude` | Calls the Anthropic Messages API with `ANTHROPIC_API_KEY`; model from `ANTHROPIC_MODEL` (default `claude-opus-5-5`). Requires prepaid API credits. |

## Free-tier notes

- **Supabase Free**: 2 active projects, 500 MB database, projects pause after 7 days without activity. The backend connects through the Supavisor session pooler because the Free plan has no IPv4 add-on.
- **Anthropic API**: no free tier; credits are prepaid. Claude Code or Claude.ai subscriptions do not include API access.
- **GitHub Actions**: unlimited on public repositories.

## Design

Visual direction is an original Material 3 identity with an emerald accent. The free Figma Community kit [Mini Finance](https://www.figma.com/community/file/1477789486167752692/mini-finance) by Emmanuel Ikechukwu (CC BY 4.0) was used as layout inspiration; no assets were copied.

## License

MIT — see [LICENSE](LICENSE).
