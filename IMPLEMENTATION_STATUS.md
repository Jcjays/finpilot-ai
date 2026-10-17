# Implementation Status

Updated after every milestone (and after meaningful increments within one). Nothing below is marked done unless the build and tests were actually run.

## Milestones

| Milestone | Status | Notes |
|---|---|---|
| 0 — Planning | ✅ Done (2026-10-17) | Environment inspected, free-tier limits and library compatibility verified, architecture/schema/API/screens approved. See [PROJECT_PLAN.md](PROJECT_PLAN.md). |
| 1 — Foundation | 🚧 In progress | Repo, tooling, Android and Ktor skeletons, Docker Compose, CI, docs |
| 2 — Database and Authentication | ⬜ Not started | |
| 3 — Core Finance | ⬜ Not started | |
| 4 — Receipt Intelligence | ⬜ Not started | |
| 5 — AI Assistant | ⬜ Not started | |
| 6 — Savings Planner | ⬜ Not started | |
| 7 — Security and Quality | ⬜ Not started | |
| 8 — Portfolio Release | ⬜ Not started | |

## Latest verified state

- **Latest commit:** _none yet_
- **Pushed to GitHub:** _not yet_
- **Backend build/tests:** _not yet run_
- **Android build/tests:** _not yet run_
- **CI:** _not yet configured_

## Completed work

### Milestone 0 — Planning (2026-10-17)
- Inspected the Windows development environment (JDK 21, Android Studio Rabbit 2026.2.1, Android SDK 36, Docker Desktop, Git); installed the GitHub CLI.
- Verified Supabase Free limits (2 projects, 500 MB, pauses after 7 idle days, session pooler for IPv4), Anthropic API billing (prepaid credits, no free tier), GitHub Actions allowances, and the CC BY 4.0 licence of the Figma inspiration kit.
- Pinned a compatible toolchain: Android on Kotlin 2.4.21 / AGP 9.3.3 / Gradle 9.5 / Compose BOM 2026.09.00 / Navigation 3 1.2.0 / Hilt 2.60.1 / Room 2.8.5; backend on Kotlin 2.3.21 / Ktor 3.6.0 / Exposed 1.6.0 / Flyway 13.10.3 / HikariCP 7.1.0.
- Designed the schema (minor-unit money, explicit INCOME/EXPENSE/TRANSFER/REFUND semantics, RLS with a least-privilege API role), the REST contract, the deterministic calculation rules, the AI tool loop, and the evaluation harness.

## Known issues

- None recorded yet.

## Next task

Milestone 1: initialise the repository, create `Jcjays/finpilot-ai` on GitHub, scaffold the Ktor backend (`/health`, OpenAPI) and the Android app (Hilt, Navigation 3, theme), add Docker Compose and CI, then verify both builds and push.
