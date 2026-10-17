-- FinPilot AI — initial schema.
--
-- Conventions
--   * Money is stored as bigint minor units (centavos for PHP) next to a char(3) currency.
--   * Timestamps are timestamptz (UTC); occurred_on is the calendar date in the user's timezone.
--   * Enumerations are text columns constrained by CHECK, which keeps later migrations simple.
--   * User content is soft-deleted through deleted_at; the API role has no DELETE privilege on it.
--   * Every user-owned table has Row Level Security enabled AND forced. The policies work for both
--     the Ktor backend (app.current_user_id set per transaction) and Supabase PostgREST (JWT claims).

-- ------------------------------------------------------------------------------------------------
-- Roles (idempotent). Passwords are never set in migrations: the local Docker init script sets the
-- dev password, and on Supabase the owner runs  ALTER ROLE finpilot_api PASSWORD '...'  once.
-- ------------------------------------------------------------------------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
    CREATE ROLE anon NOLOGIN;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
    CREATE ROLE authenticated NOLOGIN;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'finpilot_api') THEN
    CREATE ROLE finpilot_api LOGIN NOBYPASSRLS NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
  END IF;
END
$$;

-- ------------------------------------------------------------------------------------------------
-- Helper schema
-- ------------------------------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS app;

-- The caller's user id: set by the backend with set_config('app.current_user_id', <uuid>, true)
-- inside each transaction, or taken from the Supabase JWT when going through PostgREST.
CREATE OR REPLACE FUNCTION app.current_user_id() RETURNS uuid
LANGUAGE sql STABLE
AS $$
  SELECT COALESCE(
    NULLIF(current_setting('app.current_user_id', true), '')::uuid,
    (NULLIF(current_setting('request.jwt.claims', true), '')::jsonb ->> 'sub')::uuid
  );
$$;

CREATE OR REPLACE FUNCTION app.set_updated_at() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$;

-- ------------------------------------------------------------------------------------------------
-- Tables
-- ------------------------------------------------------------------------------------------------
CREATE TABLE profiles (
  id            uuid PRIMARY KEY,                       -- Supabase auth user id (no FK: keeps CI on plain Postgres)
  email         text,
  display_name  text CHECK (display_name IS NULL OR length(display_name) <= 80),
  currency      char(3) NOT NULL DEFAULT 'PHP',
  timezone      text NOT NULL DEFAULT 'Asia/Manila',
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
  id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id               uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  name                  text NOT NULL CHECK (length(name) BETWEEN 1 AND 80),
  type                  text NOT NULL CHECK (type IN ('CASH', 'BANK', 'EWALLET', 'CREDIT_CARD', 'OTHER')),
  currency              char(3) NOT NULL DEFAULT 'PHP',
  opening_balance_minor bigint NOT NULL DEFAULT 0,
  is_archived           boolean NOT NULL DEFAULT false,
  created_at            timestamptz NOT NULL DEFAULT now(),
  updated_at            timestamptz NOT NULL DEFAULT now(),
  deleted_at            timestamptz
);
CREATE UNIQUE INDEX accounts_user_name_unique ON accounts (user_id, lower(name)) WHERE deleted_at IS NULL;

CREATE TABLE categories (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     uuid REFERENCES profiles(id) ON DELETE CASCADE,  -- NULL = system seed shared by everyone
  name        text NOT NULL CHECK (length(name) BETWEEN 1 AND 60),
  kind        text NOT NULL CHECK (kind IN ('INCOME', 'EXPENSE')),
  icon        text,
  color       text CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$'),
  sort_order  integer NOT NULL DEFAULT 0,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now(),
  deleted_at  timestamptz
);
CREATE UNIQUE INDEX categories_unique_name ON categories (
  COALESCE(user_id, '00000000-0000-0000-0000-000000000000'::uuid), lower(name), kind
) WHERE deleted_at IS NULL;

CREATE TABLE receipt_extractions (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id          uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  ocr_text_sha256  text NOT NULL,
  extraction       jsonb NOT NULL,
  warnings         jsonb NOT NULL DEFAULT '[]'::jsonb,
  model            text,
  input_tokens     integer,
  output_tokens    integer,
  status           text NOT NULL DEFAULT 'EXTRACTED' CHECK (status IN ('EXTRACTED', 'SAVED', 'DISCARDED')),
  created_at       timestamptz NOT NULL DEFAULT now(),
  updated_at       timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX receipt_extractions_user_idx ON receipt_extractions (user_id, created_at DESC);

CREATE TABLE transactions (
  id                        uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id                   uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  account_id                uuid NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
  category_id               uuid REFERENCES categories(id) ON DELETE SET NULL,
  type                      text NOT NULL CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER', 'REFUND')),
  status                    text NOT NULL DEFAULT 'CLEARED' CHECK (status IN ('PENDING', 'CLEARED')),
  amount_minor              bigint NOT NULL CHECK (amount_minor > 0),
  currency                  char(3) NOT NULL DEFAULT 'PHP',
  occurred_at               timestamptz NOT NULL,
  occurred_on               date NOT NULL,
  merchant                  text CHECK (merchant IS NULL OR length(merchant) <= 120),
  note                      text CHECK (note IS NULL OR length(note) <= 500),
  transfer_group_id         uuid,
  transfer_direction        text CHECK (transfer_direction IS NULL OR transfer_direction IN ('OUT', 'IN')),
  refund_of_transaction_id  uuid REFERENCES transactions(id) ON DELETE RESTRICT,
  categorization_source     text CHECK (categorization_source IS NULL
                                        OR categorization_source IN ('RULE', 'AI_SUGGESTED', 'USER_CONFIRMED')),
  client_id                 uuid,
  receipt_extraction_id     uuid REFERENCES receipt_extractions(id) ON DELETE SET NULL,
  created_at                timestamptz NOT NULL DEFAULT now(),
  updated_at                timestamptz NOT NULL DEFAULT now(),
  deleted_at                timestamptz,
  -- A transfer is exactly a row with a group and a direction; nothing else may carry them.
  CONSTRAINT transactions_transfer_fields CHECK (
    (type = 'TRANSFER' AND transfer_group_id IS NOT NULL AND transfer_direction IS NOT NULL)
    OR (type <> 'TRANSFER' AND transfer_group_id IS NULL AND transfer_direction IS NULL)
  ),
  CONSTRAINT transactions_transfer_uncategorized CHECK (type <> 'TRANSFER' OR category_id IS NULL),
  CONSTRAINT transactions_refund_link CHECK (type = 'REFUND' OR refund_of_transaction_id IS NULL),
  CONSTRAINT transactions_source_requires_category CHECK (category_id IS NOT NULL OR categorization_source IS NULL)
);
CREATE INDEX transactions_user_date_idx ON transactions (user_id, occurred_on DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX transactions_user_category_idx ON transactions (user_id, category_id, occurred_on) WHERE deleted_at IS NULL;
CREATE INDEX transactions_transfer_group_idx ON transactions (user_id, transfer_group_id) WHERE transfer_group_id IS NOT NULL;
CREATE UNIQUE INDEX transactions_client_id_unique ON transactions (user_id, client_id) WHERE client_id IS NOT NULL;
CREATE INDEX transactions_refund_of_idx ON transactions (refund_of_transaction_id) WHERE refund_of_transaction_id IS NOT NULL;

CREATE TABLE budgets (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  category_id   uuid NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
  amount_minor  bigint NOT NULL CHECK (amount_minor > 0),
  currency      char(3) NOT NULL DEFAULT 'PHP',
  start_month   date NOT NULL CHECK (extract(day FROM start_month) = 1),
  end_month     date CHECK (end_month IS NULL OR (extract(day FROM end_month) = 1 AND end_month >= start_month)),
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now(),
  deleted_at    timestamptz
);
CREATE UNIQUE INDEX budgets_unique_period ON budgets (user_id, category_id, start_month) WHERE deleted_at IS NULL;

CREATE TABLE savings_goals (
  id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id              uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  name                 text NOT NULL CHECK (length(name) BETWEEN 1 AND 80),
  target_amount_minor  bigint NOT NULL CHECK (target_amount_minor > 0),
  saved_amount_minor   bigint NOT NULL DEFAULT 0 CHECK (saved_amount_minor >= 0),
  currency             char(3) NOT NULL DEFAULT 'PHP',
  target_date          date,
  linked_account_id    uuid REFERENCES accounts(id) ON DELETE SET NULL,
  status               text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'ARCHIVED')),
  created_at           timestamptz NOT NULL DEFAULT now(),
  updated_at           timestamptz NOT NULL DEFAULT now(),
  deleted_at           timestamptz
);
CREATE INDEX savings_goals_user_status_idx ON savings_goals (user_id, status);

CREATE TABLE savings_goal_contributions (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  goal_id         uuid NOT NULL REFERENCES savings_goals(id) ON DELETE CASCADE,
  user_id         uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  amount_minor    bigint NOT NULL CHECK (amount_minor <> 0),
  contributed_on  date NOT NULL,
  transaction_id  uuid REFERENCES transactions(id) ON DELETE SET NULL,
  note            text CHECK (note IS NULL OR length(note) <= 200),
  created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX savings_goal_contributions_goal_idx ON savings_goal_contributions (goal_id, contributed_on);

CREATE TABLE ai_insights (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  kind            text NOT NULL CHECK (kind IN ('MONTHLY_SUMMARY', 'CATEGORY_SPIKE', 'BUDGET_RISK', 'SAVINGS_PACE')),
  period_start    date NOT NULL,
  period_end      date NOT NULL CHECK (period_end >= period_start),
  facts           jsonb NOT NULL,
  narrative       jsonb NOT NULL,
  model           text,
  prompt_version  text,
  input_tokens    integer,
  output_tokens   integer,
  created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ai_insights_user_period_idx ON ai_insights (user_id, period_start DESC);

CREATE TABLE ai_action_proposals (
  id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id              uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  source               text NOT NULL CHECK (source IN ('ASSISTANT', 'INSIGHT', 'SIMULATION')),
  action_type          text NOT NULL CHECK (action_type IN
                         ('SET_BUDGET', 'ADJUST_BUDGET', 'CREATE_SAVINGS_GOAL', 'RECATEGORIZE_TRANSACTION')),
  payload              jsonb NOT NULL,
  rationale            text,
  simulation_result    jsonb,
  status               text NOT NULL DEFAULT 'PROPOSED' CHECK (status IN ('PROPOSED', 'APPROVED', 'REJECTED', 'EXPIRED')),
  expires_at           timestamptz NOT NULL DEFAULT now() + interval '7 days',
  decided_at           timestamptz,
  applied_entity_type  text,
  applied_entity_id    uuid,
  created_at           timestamptz NOT NULL DEFAULT now(),
  updated_at           timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT ai_action_proposals_applied_when_approved CHECK (status <> 'APPROVED' OR applied_entity_id IS NOT NULL)
);
CREATE INDEX ai_action_proposals_user_status_idx ON ai_action_proposals (user_id, status, created_at DESC);

CREATE TABLE audit_logs (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id      uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  actor        text NOT NULL CHECK (actor IN ('USER', 'SYSTEM', 'AI')),
  action       text NOT NULL CHECK (length(action) BETWEEN 1 AND 80),
  entity_type  text NOT NULL,
  entity_id    uuid,
  before       jsonb,
  after        jsonb,
  request_id   text,
  created_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX audit_logs_user_idx ON audit_logs (user_id, created_at DESC);
CREATE INDEX audit_logs_entity_idx ON audit_logs (entity_type, entity_id);

CREATE TABLE idempotency_keys (
  user_id          uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  key              text NOT NULL CHECK (length(key) BETWEEN 1 AND 128),
  request_hash     text NOT NULL,
  response_status  integer NOT NULL,
  response_body    jsonb,
  created_at       timestamptz NOT NULL DEFAULT now(),
  expires_at       timestamptz NOT NULL,
  PRIMARY KEY (user_id, key)
);
CREATE INDEX idempotency_keys_expires_idx ON idempotency_keys (expires_at);

-- ------------------------------------------------------------------------------------------------
-- updated_at triggers
-- ------------------------------------------------------------------------------------------------
CREATE TRIGGER profiles_set_updated_at BEFORE UPDATE ON profiles
  FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();
CREATE TRIGGER accounts_set_updated_at BEFORE UPDATE ON accounts
  FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();
CREATE TRIGGER categories_set_updated_at BEFORE UPDATE ON categories
  FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();
CREATE TRIGGER receipt_extractions_set_updated_at BEFORE UPDATE ON receipt_extractions
  FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();
CREATE TRIGGER transactions_set_updated_at BEFORE UPDATE ON transactions
  FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();
CREATE TRIGGER budgets_set_updated_at BEFORE UPDATE ON budgets
  FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();
CREATE TRIGGER savings_goals_set_updated_at BEFORE UPDATE ON savings_goals
  FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();
CREATE TRIGGER ai_action_proposals_set_updated_at BEFORE UPDATE ON ai_action_proposals
  FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();

-- ------------------------------------------------------------------------------------------------
-- Privileges: least privilege for the API role; PostgREST roles get nothing (Android never uses it)
-- ------------------------------------------------------------------------------------------------
GRANT USAGE ON SCHEMA public TO finpilot_api;
GRANT USAGE ON SCHEMA app TO finpilot_api, authenticated;
GRANT EXECUTE ON FUNCTION app.current_user_id() TO finpilot_api, authenticated;

GRANT SELECT, INSERT, UPDATE ON
  profiles, accounts, categories, transactions, budgets, savings_goals, savings_goal_contributions,
  ai_insights, ai_action_proposals, receipt_extractions
TO finpilot_api;
GRANT SELECT, INSERT ON audit_logs TO finpilot_api;
GRANT SELECT, INSERT, UPDATE, DELETE ON idempotency_keys TO finpilot_api;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO finpilot_api;

REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon, authenticated;

-- ------------------------------------------------------------------------------------------------
-- Row Level Security: enabled and forced everywhere, one policy shape for backend and PostgREST
-- ------------------------------------------------------------------------------------------------
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE profiles FORCE ROW LEVEL SECURITY;
CREATE POLICY profiles_own ON profiles FOR ALL TO finpilot_api, authenticated
  USING (id = app.current_user_id()) WITH CHECK (id = app.current_user_id());

ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE accounts FORCE ROW LEVEL SECURITY;
CREATE POLICY accounts_own ON accounts FOR ALL TO finpilot_api, authenticated
  USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());

ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories FORCE ROW LEVEL SECURITY;
CREATE POLICY categories_read ON categories FOR SELECT TO finpilot_api, authenticated
  USING (user_id IS NULL OR user_id = app.current_user_id());
CREATE POLICY categories_insert ON categories FOR INSERT TO finpilot_api, authenticated
  WITH CHECK (user_id = app.current_user_id());
CREATE POLICY categories_update ON categories FOR UPDATE TO finpilot_api, authenticated
  USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());

ALTER TABLE receipt_extractions ENABLE ROW LEVEL SECURITY;
ALTER TABLE receipt_extractions FORCE ROW LEVEL SECURITY;
CREATE POLICY receipt_extractions_own ON receipt_extractions FOR ALL TO finpilot_api, authenticated
  USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());

ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions FORCE ROW LEVEL SECURITY;
CREATE POLICY transactions_own ON transactions FOR ALL TO finpilot_api, authenticated
  USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());

ALTER TABLE budgets ENABLE ROW LEVEL SECURITY;
ALTER TABLE budgets FORCE ROW LEVEL SECURITY;
CREATE POLICY budgets_own ON budgets FOR ALL TO finpilot_api, authenticated
  USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());

ALTER TABLE savings_goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE savings_goals FORCE ROW LEVEL SECURITY;
CREATE POLICY savings_goals_own ON savings_goals FOR ALL TO finpilot_api, authenticated
  USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());

ALTER TABLE savings_goal_contributions ENABLE ROW LEVEL SECURITY;
ALTER TABLE savings_goal_contributions FORCE ROW LEVEL SECURITY;
CREATE POLICY savings_goal_contributions_own ON savings_goal_contributions FOR ALL TO finpilot_api, authenticated
  USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());

ALTER TABLE ai_insights ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_insights FORCE ROW LEVEL SECURITY;
CREATE POLICY ai_insights_own ON ai_insights FOR ALL TO finpilot_api, authenticated
  USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());

ALTER TABLE ai_action_proposals ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_action_proposals FORCE ROW LEVEL SECURITY;
CREATE POLICY ai_action_proposals_own ON ai_action_proposals FOR ALL TO finpilot_api, authenticated
  USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());

ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs FORCE ROW LEVEL SECURITY;
CREATE POLICY audit_logs_read ON audit_logs FOR SELECT TO finpilot_api, authenticated
  USING (user_id = app.current_user_id());
CREATE POLICY audit_logs_append ON audit_logs FOR INSERT TO finpilot_api, authenticated
  WITH CHECK (user_id = app.current_user_id());

ALTER TABLE idempotency_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE idempotency_keys FORCE ROW LEVEL SECURITY;
CREATE POLICY idempotency_keys_own ON idempotency_keys FOR ALL TO finpilot_api, authenticated
  USING (user_id = app.current_user_id()) WITH CHECK (user_id = app.current_user_id());
