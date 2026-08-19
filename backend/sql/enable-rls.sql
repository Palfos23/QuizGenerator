-- Enables Row Level Security (RLS) on every table this app's Hibernate layer
-- creates in the `public` schema.
--
-- Why this is needed: the production database is hosted on Supabase (see
-- render.yaml's DATABASE_URL comment). Supabase auto-exposes every table in
-- `public` as a REST endpoint via PostgREST, using the project's public
-- "anon" API key - completely independent of this app's own Spring Boot API
-- and its JWT-based auth (SecurityConfig). A table with RLS disabled is
-- therefore readable/writable by anyone with that anon key, bypassing this
-- app's authentication entirely. This app never uses Supabase's client SDK
-- or REST API at all - all access goes through the Spring Boot backend via a
-- direct JDBC connection - so nothing should be reachable this way.
--
-- Why this is safe to run without adding any policies: Postgres RLS does not
-- restrict the table owner by default (only PostgREST's "anon"/"authenticated"
-- roles), and the JDBC role this app connects with (DATABASE_USERNAME) is the
-- owner of every table below, since Hibernate's ddl-auto=update created them
-- under that same role. Enabling RLS with zero policies blocks the public API
-- surface completely while leaving the app's own database access untouched.
-- (Deliberately NOT using "FORCE ROW LEVEL SECURITY" - that would also apply
-- to the table owner and could lock this app's own JDBC connection out.)
--
-- Run this once, directly, in the Supabase SQL editor (or via psql) against
-- the production database - Claude cannot and should not run this for you.
-- Safe to re-run: enabling RLS on a table that already has it enabled is a
-- no-op.

-- --- Core game content ---
ALTER TABLE IF EXISTS public.admin_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.app_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.athletes ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.athlete_descriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.athlete_photos ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.athlete_pools ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.athlete_pool_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.clubs ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.reports ENABLE ROW LEVEL SECURITY;

-- --- Quiz generator ---
ALTER TABLE IF EXISTS public.questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.question_labels ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.question_label_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.quiz_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.quiz_template_questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.saved_quizzes ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.saved_quiz_questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.submitted_questions ENABLE ROW LEVEL SECURITY;

-- --- Weekly Grid + Grid Battle ---
ALTER TABLE IF EXISTS public.grids ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.grid_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.grid_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.grid_candidates ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.grid_pool_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.grid_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.grid_attempt_solved_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.grid_attempt_overtime_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.grid_battle_room_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.grid_battle_participant_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.grid_battle_solved_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.grid_battle_grid_sequence ENABLE ROW LEVEL SECURITY;

-- --- Weekly Starting XI + Starting XI Battle ---
ALTER TABLE IF EXISTS public.lineups ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.lineup_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.lineup_candidates ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.lineup_pool_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.lineup_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.lineup_attempt_solved_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.lineup_battle_room_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.lineup_battle_participant_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.lineup_battle_solved_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.lineup_battle_lineup_sequence ENABLE ROW LEVEL SECURITY;

-- --- Imposter ---
ALTER TABLE IF EXISTS public.imposter_grids ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.imposter_tiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.imposter_flipped_tiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.imposter_room_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.imposter_participant_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.imposter_battle_grid_sequence ENABLE ROW LEVEL SECURITY;

-- --- Tension ---
ALTER TABLE IF EXISTS public.tension_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.tension_category_options ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.tension_questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.tension_answer_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.tension_room_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.tension_room_question_sequence ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.tension_participant_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.tension_round_answers ENABLE ROW LEVEL SECURITY;

-- --- 501 ---
ALTER TABLE IF EXISTS public.five_oh_one_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.five_oh_one_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.five_oh_one_room_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.five_oh_one_participant_states ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.five_oh_one_throws ENABLE ROW LEVEL SECURITY;

-- --- Shared online-room framework (used by Grid Battle / Starting XI Battle lobbies) ---
ALTER TABLE IF EXISTS public.game_rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.game_room_participants ENABLE ROW LEVEL SECURITY;
