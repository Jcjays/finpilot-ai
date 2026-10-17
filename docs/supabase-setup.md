# Supabase setup runbook

FinPilot AI runs against either the **local Supabase CLI stack** (default for development) or a **hosted Supabase Free project**. Both paths use the same Flyway migrations and the same backend configuration names. Nothing here requires a paid plan.

## A. Local stack (free, offline)

Prerequisites: Docker Desktop running, and the Supabase CLI (Node 20+ for `npx`, or the standalone binary).

```bash
npx supabase start         # Postgres 54322, API 54321, Studio 54323, Mailpit 54324
```

The repository already contains `supabase/config.toml` (project id `finpilot-ai`, Postgres 17, email confirmations off), so `supabase init` is not needed.

If the npm wrapper fails on Windows (seen as `EPERM: operation not permitted, uv_spawn 'taskkill'`, or PowerShell refusing to run `npx.ps1`), use a standalone pre-launcher release instead: releases up to **v2.95.4** ship a single Go `supabase.exe`, while v2.100 and later wrap it in the same Node-style launcher that triggers the `taskkill` error. Download `supabase_windows_amd64.tar.gz` and `supabase_<version>_checksums.txt` for v2.95.4 from the [supabase/cli releases](https://github.com/supabase/cli/releases), verify the SHA-256, extract, and run `supabase.exe start` from the repository root.

`supabase start` prints the local keys. The backend already defaults to the local stack:

| Variable | Local value |
|---|---|
| `SUPABASE_URL` | `http://127.0.0.1:54321` |
| `AUTH_HS256_SECRET` | the demo secret printed by `supabase status` (`super-secret-jwt-token-with-at-least-32-characters-long`); accepted only when `APP_ENV=local` |
| `SUPABASE_PUBLISHABLE_KEY` | the publishable/anon key printed by `supabase status` (used by the Android app) |

To use asymmetric keys locally like production does: `npx supabase gen signing-key --algorithm ES256 > supabase/signing_keys.json`, set `[auth] signing_keys_path = "./signing_keys.json"` in `supabase/config.toml`, restart the stack, and leave `AUTH_HS256_SECRET` empty.

Database for the backend: the plain `docker compose up -d` Postgres (port 5432) is used for tests and local development. Apply migrations with:

```bash
cd backend
set DATABASE_URL=jdbc:postgresql://localhost:5432/finpilot
set MIGRATION_DATABASE_USER=finpilot_migrator
set MIGRATION_DATABASE_PASSWORD=finpilot_migrator_dev_password
./gradlew :data:migrate
```

(On macOS/Linux use `export`.) The Compose init script already created the `finpilot_api` role with the dev password from `.env.example`.

## B. Hosted project (Supabase Free)

### 1. Create the project

1. Dashboard → **New project**. Name `finpilot-ai`, region **Southeast Asia (Singapore)** for Philippine users, Postgres **17** (the default). Save the database password in your password manager; it is the `postgres` password.
2. Note the **Project ref** (the subdomain of `https://<ref>.supabase.co`).

Free-plan facts to remember: 2 active projects, 500 MB database, 1 GB storage, **projects pause after 7 days without activity** (restore from the dashboard; the keepalive workflow below prevents this), no IPv4 add-on.

### 2. Turn on asymmetric JWT signing keys

Dashboard → **Authentication → JWT Keys** (or *Settings → JWT Signing Keys*) → **Migrate JWT secret** → create a standby **ES256 (P-256)** key → **Rotate** so it becomes the current signing key. New access tokens are now ES256 and verifiable through

```
https://<ref>.supabase.co/auth/v1/.well-known/jwks.json
```

The backend verifies tokens with that JWKS (cached 10 minutes, like Supabase's edge). Do **not** set `AUTH_HS256_SECRET` outside local development; the server refuses to start if you do.

For development convenience you may disable *Confirm email* under **Authentication → Providers → Email**; keep it enabled for a public demo.

### 3. Collect connection details

Dashboard → **Connect** (top bar):

| Purpose | Use | Why |
|---|---|---|
| Backend runtime (API role) | **Session pooler**: `aws-0-<region>.pooler.supabase.com:5432`, database `postgres`, user `finpilot_api.<ref>` | IPv4-compatible on the Free plan; supports prepared statements |
| Migrations (owner role) | **Session pooler** with user `postgres.<ref>` | Same host; the `postgres` role owns the schema |
| Not for Free plan | *Direct connection* host `db.<ref>.supabase.co` | IPv6 only without the paid IPv4 add-on |
| Avoid | *Transaction pooler* port 6543 | Rejects prepared statements used by the JDBC driver |

JDBC URL shape: `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require`

### 4. Apply migrations

From `backend/`:

```bash
set MIGRATION_DATABASE_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
set MIGRATION_DATABASE_USER=postgres.<ref>
set MIGRATION_DATABASE_PASSWORD=<database password>
./gradlew :data:migrate
```

`V1__init.sql` creates the `finpilot_api` role if it does not exist (without a password), the tables, grants and RLS policies; `V2__seed_categories.sql` seeds the shared categories.

### 5. Give the API role a password

Dashboard → **SQL Editor**:

```sql
ALTER ROLE finpilot_api WITH LOGIN PASSWORD '<generate a long random password>';
```

Then verify RLS is in force (should return `t, t` for every table):

```sql
SELECT relname, relrowsecurity, relforcerowsecurity FROM pg_class
WHERE relnamespace = 'public'::regnamespace AND relkind = 'r' ORDER BY relname;
```

### 6. Configure the backend

`backend/.env` (git-ignored):

```
APP_ENV=prod
SUPABASE_URL=https://<ref>.supabase.co
DATABASE_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
DATABASE_USER=finpilot_api.<ref>
DATABASE_PASSWORD=<the password from step 5>
DATABASE_POOL_SIZE=4
AI_PROVIDER=mock
```

Start the server and check `GET /health` reports `"db": "ok"`. Sign in from the Android app (or mint a token with the Supabase client) and call `GET /api/v1/me`; the first call creates the profile row.

### 7. Keep the Free project awake

Add two **Actions variables** (Settings → Secrets and variables → Actions → Variables) to the GitHub repository: `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` (the publishable key is public by design). The `supabase-keepalive.yml` workflow pings the project every three days so it is not paused for inactivity.

## Troubleshooting

- `password authentication failed for user "finpilot_api.<ref>"` — step 5 was skipped, or the username lacks the `.<ref>` suffix required by the pooler.
- `Network is unreachable` / IPv6 errors — you used the direct host; switch to the session pooler.
- `relation ... does not exist` — migrations were not applied to this project (step 4).
- Rows invisible to the API — expected when `app.current_user_id` is not set; the backend sets it per transaction. Query as the owner role to inspect data.
