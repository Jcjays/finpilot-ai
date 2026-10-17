-- Runs once when the local Postgres volume is first created.
-- Creates the least-privilege application role with a DEV-ONLY password.
-- On hosted Supabase the same role is created by the Flyway migration (without a password);
-- set its password there with: ALTER ROLE finpilot_api PASSWORD '<strong password>';
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'finpilot_api') THEN
    CREATE ROLE finpilot_api LOGIN NOBYPASSRLS NOSUPERUSER NOCREATEDB NOCREATEROLE
      PASSWORD 'finpilot_api_dev_password';
  END IF;
END
$$;

-- A dedicated test database so integration tests never touch development data.
SELECT 'CREATE DATABASE finpilot_test OWNER finpilot_migrator'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'finpilot_test')\gexec
