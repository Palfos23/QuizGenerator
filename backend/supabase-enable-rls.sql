-- Run once in Supabase: SQL Editor.
--
-- Supabase exposes the `public` schema through its web "Data API". A table without row level security
-- (RLS) is then readable/writable by anyone holding the project's public key. This app never uses that
-- API - the backend talks to Postgres directly as the table owner, which bypasses RLS - so the safe
-- setup is: RLS ON, no policies. That denies the web API everything and changes nothing for the app.
-- (Do NOT use FORCE ROW LEVEL SECURITY: it would apply to the owner too and lock the backend out.)

-- 1) Lock down every existing table in the public schema.
DO $$
DECLARE r record;
BEGIN
  FOR r IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' LOOP
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', r.tablename);
  END LOOP;
END $$;

-- 2) Do the same automatically for tables created later (Hibernate creates new tables on deploy).
CREATE OR REPLACE FUNCTION public.rls_auto_enable() RETURNS event_trigger
LANGUAGE plpgsql
SET search_path = pg_catalog  -- pinned, so Supabase's advisor doesn't flag a mutable search_path
AS $$
DECLARE obj record;
BEGIN
  FOR obj IN SELECT * FROM pg_event_trigger_ddl_commands()
             WHERE command_tag = 'CREATE TABLE' AND schema_name = 'public'
  LOOP
    EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', obj.object_identity);
  END LOOP;
END $$;

DROP EVENT TRIGGER IF EXISTS rls_auto_enable;
CREATE EVENT TRIGGER rls_auto_enable ON ddl_command_end
  WHEN TAG IN ('CREATE TABLE') EXECUTE FUNCTION public.rls_auto_enable();

-- Optional extra: the app doesn't use the Data API at all, so it can be switched off entirely in
-- Project Settings > Data API. (Picture uploads use the separate Storage API and are unaffected.)
