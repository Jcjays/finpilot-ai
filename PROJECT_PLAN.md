# FinPilot AI — Project Plan

Agreed scope and implementation plan (approved 2026-10-17). This file records *what* we are building and *how*; [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md) records *where we are*.

## 1. Product

**FinPilot AI** is a native Android personal-finance app for the Philippines (currency PHP, timezone Asia/Manila) powered by Claude. Users can track income and expenses, scan receipts, analyse spending and budgets, ask natural-language questions, set savings goals, simulate budget changes, and approve AI-generated budget proposals. Ordinary transaction management, summaries, and calculations must keep working when Claude is unavailable.

## 2. Non-negotiable constraints

- Free tools and free tiers only; nothing mandatory requires payment. Services needing a card or paid usage are documented (Anthropic API credits are prepaid and optional).
- AI must be mockable: a deterministic `MockAiAssistantService` powers development and CI at zero cost.
- Synthetic data only for demos and tests.
- Secrets never enter git, the APK, or logs. `.env` is ignored; `.env.example` documents every variable.
- Work milestone by milestone; run builds and tests after each meaningful unit; never claim a pass without running it.
- Small, meaningful, conventional commits pushed continuously to `main` (feature branches for substantial features). Never force-push or rewrite published history.

## 3. Decisions

| Topic | Decision | Why |
|---|---|---|
| Repository | Public monorepo `Jcjays/finpilot-ai`: `android/` and `backend/` are independent Gradle builds | Portfolio visibility; unlimited Actions minutes; one history for the whole system |
| Android DI | Hilt (KSP) | Industry standard; compile-time safety |
| Backend DI | Koin | Lightweight; Ktor-native integration |
| Claude integration | Ktor Client against the Messages API (no SDK) | Small surface (messages, tools, structured outputs); full control over validation and timeouts |
| Default model | `claude-opus-5-5`, env-configurable with optional per-task overrides | Strongest tool use; cost can be tuned later without code changes |
| Auth | Supabase Auth; backend verifies JWTs via JWKS (ES256/RS256); HS256 demo secret accepted only when `APP_ENV=local` | Backend never trusts the client; local stack still works |
| Database access | Backend connects as least-privilege role `finpilot_api` subject to RLS; per-transaction `app.current_user_id` setting | Defense-in-depth even though every query also filters by user |
| Hosting | Supabase Free via the Supavisor **session** pooler (IPv4); local Postgres 17 via Docker Compose; optional `npx supabase start` for the full local Auth stack | Free plan has no IPv4 add-on; CI needs plain Postgres |
| Kotlin versions | Android 2.4.21 (matches AGP 9.3 / Gradle 9.5 support matrix); backend 2.3.21 (matches Ktor 3.6.0's compiler plugin) | Kotlin compiler plugins are version-sensitive |
| Offline | Backend is the source of truth; Room caches reads and queues transaction writes in an outbox replayed by WorkManager with idempotency keys | Logging an expense must work without signal; calculations stay server-side so the assistant and the UI agree |
| Design | Original Material 3 identity, emerald accent, light default + dark; Figma "Mini Finance" (CC BY 4.0) as inspiration only | Spec requires an original identity |

## 4. Technology stack (pinned)

**Android:** AGP 9.3.3 (built-in Kotlin), Gradle 9.5.0, Kotlin 2.4.21, KSP 2.3.12, compileSdk 37.1 (required by Compose 1.12 and Navigation 3 1.2), targetSdk 36, minSdk 26, Compose BOM 2026.09.00 (Material 3 1.4.0), Navigation 3 1.2.0, Lifecycle 2.11.0, Hilt 2.60.1 + androidx.hilt 1.4.0, Room 2.8.5, DataStore 1.2.1, Retrofit 3.0.0 + kotlinx-serialization converter, OkHttp 5.5.0, kotlinx-serialization 1.11.0, kotlinx-coroutines 1.11.0, kotlinx-datetime 0.8.0, supabase-kt 3.8.0 (auth), CameraX 1.6.2, ML Kit text-recognition 16.0.1 (bundled), WorkManager 2.12.0, Vico 3.3.1, JUnit, MockK 1.14.11, Turbine, detekt 1.23.8.

**Backend:** Gradle 9.3.1, Kotlin 2.3.21, JDK 21, Ktor 3.6.0 (netty, routing, content-negotiation, auth-jwt, status-pages, call-id, call-logging, cors, rate-limit, routing-openapi, openapi, swagger, client-cio), Exposed 1.6.0, PostgreSQL JDBC 42.7.14, HikariCP 7.1.0, Flyway 13.10.3, Koin 4.2.2, auth0 java-jwt 4.6.2 + jwks-rsa 0.24.1, logback 1.6.5, JUnit 5, MockK, Testcontainers 1.21.4, Konsist, detekt 1.23.8.

## 5. Architecture

**Android:** `UI → ViewModel (MVI: State / Intent / Effect) → UseCase → Repository → Room | Retrofit`. One `MviViewModel<S, I, E>` base in `core:ui`; screens are pure `@Composable fun Screen(state, onIntent)` with previews. Navigation 3 single back stack with `@Serializable` route keys; each feature module contributes its entries. Modules: `app`, `core:model`, `core:ui`, `core:network`, `core:database`, `core:data`, `feature:auth`, `feature:dashboard`, `feature:transactions`, `feature:receipts`, `feature:budgets`, `feature:assistant`, `feature:settings`.

**Backend (modular monolith):** `Routes → Application Services → Domain (deterministic calc) → Repositories → PostgreSQL`. Modules: `app` (Ktor main, config, Koin, plugins, Flyway), `api` (routes, DTOs, validation, idempotency), `domain` (pure Kotlin entities, calc, repository interfaces, services), `data` (Exposed, repositories, migrations), `ai` (AI service abstraction, Claude + mock implementations, tool registry, eval harness). An architecture test enforces the layering.

**AI layer:** `AiAssistantService` with `ClaudeAiAssistantService` (Ktor Client) and `MockAiAssistantService`. Receipt extraction uses structured outputs with a strict JSON schema plus Kotlin validation (arithmetic consistency, date sanity, currency and category allowlists). Categorization is rule-first with Claude only for misses. Insights are computed by the backend and only explained by Claude, with a grounding gate that drops statements whose numbers do not match the supplied facts. The assistant uses `tool_choice: auto` with six strict-schema tools (`get_financial_summary`, `get_category_breakdown`, `get_transactions`, `get_budget_status`, `get_savings_goal_status`, `simulate_budget_change`), server-side user scoping, argument validation, at most 6 iterations and 30 seconds per request, and explicit handling of `max_tokens`, `refusal`, 429 and 5xx responses. Chat is not streamed initially.

**Calculations (pure Kotlin, unit-tested):** integer centavos; basis points for ratios; HALF_EVEN rounding only when applying percentages; calendar months in Asia/Manila; facts use CLEARED transactions only; `expense = gross expense − refunds`; `net = income − expense`; transfers never count; savings rate `net / income`; budget remaining, overrun, pace and end-of-month projection (labelled); goal progress and projected completion (labelled); month-over-month deltas; `simulateBudgetChange` shared by the API and the assistant tool.

## 6. Database schema (Flyway)

Tables: `profiles`, `accounts`, `categories` (system seeds have `user_id NULL`), `transactions`, `budgets`, `savings_goals`, `savings_goal_contributions`, `ai_insights`, `ai_action_proposals`, `audit_logs` (append-only), `idempotency_keys`, `receipt_extractions`. Conventions: `bigint` minor units + `char(3)` currency; `timestamptz`; text enums with CHECK constraints; soft delete via `deleted_at` with partial unique indexes; `created_at`/`updated_at` triggers. Transaction types `INCOME | EXPENSE | TRANSFER | REFUND` with CHECKs tying `transfer_group_id`/`transfer_direction` to TRANSFER and `refund_of_transaction_id` to REFUND; status `PENDING | CLEARED`; `categorization_source` `RULE | AI_SUGGESTED | USER_CONFIRMED`. Proposals follow `PROPOSED → APPROVED | REJECTED | EXPIRED` with idempotent approval. RLS enabled and forced on every user table with one policy form that works for both the backend role and PostgREST. Seed categories cover Food and Dining, Groceries, Transportation, Housing, Utilities, Healthcare, Shopping, Entertainment, Education, Savings, Salary and other income plus PH-specific extras (Remittance / Padala, Government Contributions, Mobile Load and Internet, 13th Month Pay).

## 7. API (`/api/v1`, Supabase JWT bearer)

`GET /health` · `GET /me` · `GET /categories` · `GET|POST /accounts` · `GET|POST /transactions`, `PATCH|DELETE /transactions/{id}` · `GET /finance/summary` · `GET /finance/category-breakdown` · `GET|POST /budgets`, `PATCH /budgets/{id}` · `GET|POST /savings-goals`, `POST /savings-goals/{id}/contributions` · `POST /receipts/extract` · `POST /assistant/chat` · `POST /finance/simulations` · `GET /finance/proposals`, `POST /finance/proposals/{id}/approve|reject` · `GET /insights`, `POST /insights/generate` · `GET /openapi.json`, `GET /swagger`.

Conventions: `{"data", "meta"}` success envelope and `{"error": {"code", "message", "details", "request_id"}}` error envelope; cursor pagination; `Idempotency-Key` required on state-changing POSTs; 404 (never 403) for other users' resources; documented error codes.

## 8. Screens

Onboarding and auth · Dashboard · Transaction list · Add/edit transaction · Transaction details · Receipt scanner and extraction review · Budgets and category breakdown · Savings goals · AI finance chat · AI insights · Budget simulation and proposal review · Settings and account. Every data screen has loading, empty, error and success states and Compose previews.

## 9. Milestones and gates

| # | Milestone | Gate |
|---|---|---|
| 0 | Planning | Approved |
| 1 | Foundation — repo, tooling, Android and Ktor skeletons, Docker Compose, detekt, CI, docs | Both builds and unit tests green locally and in CI; `/health` and `/openapi.json` served |
| 2 | Database and Authentication — migrations, seeds, RLS, API role, JWT verification, Supabase runbook, Android sign-in | DB integration and cross-user tests, auth route tests, emulator sign-in |
| 3 | Core Finance — transactions, categories, budgets, calculations, summary endpoints, Room cache and sync, dashboard and transaction screens | Calculation edge-case tests, route tests, Android unit tests, emulator smoke test |
| 4 | Receipt Intelligence — OCR, Claude extraction, validation, review-before-save | Extraction eval (mock), validation tests, manual scan |
| 5 | AI Assistant — Claude client, tool registry, bounded loop, insights, chat and insights screens | Tool-loop, injection, cross-user, timeout and rate-limit tests |
| 6 | Savings Planner — goals, simulations, proposals with approval and audit log | Duplicate-approval and determinism tests |
| 7 | Security and Quality — full eval harness, RLS verification, rate limiting, logging and redaction | Eval report committed; security checklist |
| 8 | Portfolio Release — polish, demo data, screenshots, demo recording, fresh-clone verification | README followed end to end on a clean machine/CI |

## 10. Testing and evaluation

Unit tests (calc, validators, ViewModels), Ktor route tests, database integration tests (Testcontainers locally, Postgres service container in CI), Android unit and selected UI tests, mock Claude in CI, and an evaluation harness with synthetic cases for extraction, categorization, tool selection, date ranges, numeric consistency, missing data, prompt injection, cross-user access, duplicate approvals, API failures and rate limits. Metrics tracked: extraction field accuracy, tool-call success, numeric grounding, latency, token usage. The real-API evaluation runs only when explicitly enabled with an API key and its results are never fabricated.

## 11. Definition of done

Android builds; Ktor runs locally with documented APIs; core finance works without Claude credentials; data persists in PostgreSQL; authentication and per-user authorization are tested; receipt extraction and savings proposals require confirmation; Claude only uses controlled tools; calculations are deterministic; tests and evaluation run without paid API calls; no secrets committed; synthetic data throughout; a new developer can follow the README; no mandatory paid component.
