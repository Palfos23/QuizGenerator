# QuizMaker

A web app for generating custom quizzes (e.g. for a birthday party) from a
question bank that an admin curates.

- **Backend:** Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA, H2 (dev) / PostgreSQL (prod)
- **Frontend:** Vue 3 (Composition API, `<script setup>`), Vue Router, Axios, Vite

## How it works

1. **Admin** logs in with a username/password (kept entirely separate from
   regular users - see "Authentication" below) and adds questions to the
   bank: question text, category, a difficulty from 1 (easiest) to 10
   (hardest), a language, the single correct answer, and whether the
   answer "could change" over time.
2. **User** signs in with Google, goes to `/generate`, picks a language and
   a difficulty range, then builds their quiz category by category - e.g.
   3 questions from Sport, 4 from Film, 6 from Literature. The backend
   randomly picks matching questions from the bank for each category.
3. The user reviews the draft: questions can be **reordered** with the
   ↑/↓ buttons, **discarded and replaced** with another from the *same
   category* (so the counts you asked for stay correct), or just removed.
   Each question's answer is hidden by default behind a "Show answer" toggle,
   so you can read through questions without spoiling yourself. An
   **"Add more questions"** control at the bottom of the review step also
   lets you bump up a category's count, or pull in a category that wasn't
   part of the original selection at all - without starting over. This
   lives in the shared `QuizReviewEditor` component, so it's available
   everywhere that component is used: generating a fresh quiz, editing a
   saved one, and building an admin quiz template.
4. Once happy, the user can **download the quiz as a PDF**, and/or hit
   **"Save to My Quizzes"** to keep a permanent copy they can come back to
   later at `/my-quizzes` - nothing is saved automatically just from
   generating a quiz, only on that explicit action. A saved quiz isn't
   frozen once saved, either: reopening it from `/my-quizzes` gives the
   same reorder/discard/replace/remove/add tools as the original review
   step, with an "Update" button to overwrite the saved copy in place.

An admin can also publish a **quiz template** at `/admin/quiz-templates`
(same generate-and-review flow as above, just saved differently) - these
show up in an "Available quizzes" section at the top of every user's
`/my-quizzes` page, with a "Copy to My Quizzes" button. Copying creates a
brand-new, fully independent `SavedQuiz` for that user - editing their
copy never touches the template, and the admin editing the template later
never touches anyone's existing copies.

The whole frontend is responsive and works on mobile - the top nav
collapses to a bottom tab bar below 760px wide.

Worth knowing: the original difficulty range used to *generate* a quiz isn't
stored with it (only its language is) - so when discarding/replacing a
question on an already-saved quiz, the replacement is pulled from the full
1-10 range rather than whatever narrower range you generated with
originally. The category is still matched, same as before.

### Languages

Questions can be written in English, German, French, Spanish or Norwegian.
This is content-level only: each question belongs to exactly one language
(see the data model note below), and a generated quiz is entirely in one
language at a time. The interface itself (buttons, labels, etc.) is still
English-only - translating the UI chrome would be a separate, bigger
project if you want it too.

### Weekly grid

A second, separate game mode alongside the quiz generator - a themed
guessing grid (think "name every Cardinals skill-position player who
started 35+ games," inspired by sites like BDGE's Daily Grid), covering
**football or cycling**, admin's choice per grid.

How it fits together:

- An admin maintains an **athlete roster** (`/admin/athletes`) - just a
  name, sport, and team/team-name per athlete. Nothing else about them is
  tracked in the system; any stats relevant to a specific grid (goals,
  wins, games started) are entered per-grid, not stored on the athlete
  permanently, since the same athlete could reasonably show up in a future
  grid with a different relevant stat.
- An admin builds a **grid** (`/admin/grids`): a title, a theme description,
  a week (grids run Monday-Sunday), a strike limit, and two things built
  from the roster:
  - a **candidate pool** - every athlete guessable in this grid's search
    box (there's a quick "add a whole team" helper for this)
  - a subset of that pool flagged as **correct answers**, each with a
    short hint label and number (e.g. "FW" / 14) shown on its tile before
    it's solved
- A **decoy is still guessable and still costs a strike** - the pool
  intentionally includes candidates who *don't* satisfy the theme (e.g.
  every other Spurs player who didn't hit 10 goals), so guessing them is a
  real wrong answer, not something the search box filters out for you.
- Tiles can show a **club crest as an extra hint**, managed at `/admin/clubs`
  (name, sport, and a logo image URL - see the note below on why it's a
  URL rather than a file upload). The logo lives on the *entry*, not the
  athlete, which is what makes all of these work with the same mechanism:
  - A club-themed grid ("Tottenham's top scorers") - pick the same crest
    on every entry.
  - A cross-club grid ("all-time Premier League top scorers") - pick
    whichever club is actually relevant *for that player* on each entry
    individually.
  - Too easy with the logo showing? Untick "Show logo" on that one entry -
    the hint label and number still show, just not the crest.
  Like the pool candidates, a club is entirely optional per entry - a grid
  with no logos at all works exactly as before.
- Players search and guess from `/weekly-grid`; correct guesses reveal a
  tile, wrong guesses cost a strike. Running out of strikes ends the
  attempt - from there you can **reveal** the remaining answers, or keep
  guessing in **Overtime** (further correct guesses still reveal tiles,
  but nothing in overtime affects your strikes or counts as a "clean"
  solve). One attempt is tracked per user per grid, so progress survives a
  refresh or a return visit later in the week.
- Started weekly, on purpose - the README's own admonition to itself: once
  the athlete roster and question bank both have real depth, daily grids
  are a straightforward next step (just a narrower active-window
  calculation), not a redesign.

Club logos are a **plain URL field**, not a file upload. Render's
filesystem is ephemeral (wiped on every redeploy), so storing an uploaded
image file locally would just lose it on the next deploy - actually
persisting uploads would need a separate cloud storage integration (S3,
Supabase Storage, etc.). For now, point the logo URL at wherever you're
already hosting the crest image (your own hosting, a public Supabase
Storage URL, etc.).

Athletes can also have a **photo URL**, same tradeoff and same reasoning
as club logos - a hosted image link, not an upload. The photo only ever
appears on a tile *after* it's solved (the club logo is the hint
beforehand); the backend enforces this the same way it already withholds
the athlete's name until solved, so the photo can't be used to visually
recognize someone before actually guessing them. If no photo is set, a
solved tile just keeps showing the club logo instead - it never ends up
looking emptier than an unsolved one.

Each club can also have its own **hint badge color** (a hex value, e.g.
`#F2B705`) instead of every badge being the same gold - useful if you want
a tile's badge to actually match the team's colors. Leave it blank and the
badge falls back to the app's default gold. The badge's text color adapts
automatically (black or white) based on the chosen background's
brightness, so an admin picking a very light or very dark color doesn't
end up with unreadable text.

### Tension

A third game mode, ported from a separate standalone project (originally
Spring Boot + MongoDB + React) into this app's stack (Spring Boot + Postgres
+ Vue). It's a pass-the-device party game, not an async one like the
weekly grid - everyone plays together in one sitting, so unlike the grid
there's **no server-side game-state to track**. The backend's only job is
serving up questions and autocomplete word lists; turn order, scoring, and
the reveal sequence all live in the browser for the duration of one game,
exactly like the original.

**How a round works:** each question has a "safe" list of up to 10 ranked
answers (rank 10 - the least obvious still-safe answer - scores the most;
rank 1 scores the least) plus a separate "tension" list ranked just past
that cutoff. Players take turns (rotating who goes first each question)
typing one guess each, picking from an autocomplete box scoped to the
question's answer category. Once everyone's answered, a short countdown
triggers a reveal, one answer at a time:
- Matches a safe answer → score = that answer's own rank
- Matches a tension answer → **-5** flat, regardless of its rank
- Matches nothing at all → **-3**

Highest total after the last question wins. The name comes from the
temptation to push toward a higher-scoring (rank 10) guess versus playing
it safe - go one rank too far and you're in tension territory.

Content is managed at `/admin/tension-questions` (each question's safe and
tension answer lists) and `/admin/tension-categories` (the autocomplete
word lists - a broader suggestion pool, not necessarily all correct
answers, just plausible same-category words to type from). Since this was
ported from a separate Mongo-backed project, none of that original
question content carries over automatically - it has to be re-entered (or
imported from an export) into the new Postgres-backed admin UI.

### User-submitted questions

Any logged-in user can suggest a question at `/suggest-question`, which an
admin reviews at `/admin/question-submissions`. Approving copies it into
the shared `questions` table (available to everyone via the normal
generator); rejecting does **not** discard it - it just never leaves the
`submitted_questions` table, and the submitter can see the reason why on
their own submissions list.

The interesting part: a rejected (or still-pending) submission stays
usable, but only by the person who wrote it. On the generator page, a
user can tick "Include my own submitted questions" - when they do, their
own submissions (any status except APPROVED, since those are already in
the shared pool by then) become eligible candidates for *their own*
quizzes only, never anyone else's. Internally this works by mapping a
submission straight to a `QuestionDto` with a **negative id** - real
questions always have positive ids, so this guarantees a personal
submission can never collide with an unrelated shared question that
happens to share the same numeric id during discard/replace.

## Authentication

Two completely separate login paths, matching two different trust levels:

| Who | How | Where |
|---|---|---|
| Regular user | Google Sign-In (their Google account, no password of ours) | `/` (the landing page) |
| Admin | Username + password, checked against a dedicated `admin_users` table | `/admin` |

Notes on the design:

- **Regular users never touch a password of ours.** `AppUser` rows are
  created automatically the first time someone signs in with Google - there
  is no `AppUser` -> `AdminUser` upgrade path in the code, so a compromised
  or careless regular account can't become an admin account.
- **The admin login lives at its own URL (`/admin`)**, separate from the
  regular sign-in - it's not linked from the main navigation, since regular
  users have no reason to see it. This is just tidiness, **not the actual
  security boundary** - the real protection is that every `/api/admin/**`
  endpoint requires a valid JWT with the `ADMIN` role, checked server-side
  on every request (see `SecurityConfig`). Someone finding or guessing the
  `/admin` URL doesn't grant them anything; only a correct username/password does.
- Both login flows return the same kind of thing: a JWT the frontend stores
  and sends as `Authorization: Bearer <token>` on every API call. The token
  carries the role (`USER` or `ADMIN`), which the backend re-checks on every
  request - it never trusts the frontend's idea of who's logged in.
- `/api/quiz/**` requires *any* logged-in user. `/api/admin/**` requires the
  `ADMIN` role specifically.

### Routes

| Path | Who sees it | Purpose |
|---|---|---|
| `/` | Everyone | Landing page: what the app does + Google sign-in |
| `/generate` | Logged-in users | Build and export a quiz |
| `/my-quizzes` | Logged-in users | View/edit/re-download/duplicate/delete previously saved quizzes |
| `/weekly-grid` | Logged-in users | This week's grid(s) to play, plus a "previous boards" archive |
| `/weekly-grid/:id` | Logged-in users | Play a specific grid |
| `/admin` | Everyone (but pointless without credentials) | Admin username/password login |
| `/admin/questions` | Logged-in admins | Manage the question bank |
| `/admin/athletes` | Logged-in admins | Manage the athlete roster used to build grids |
| `/admin/grids` | Logged-in admins | Create and edit weekly grids |
| `/admin/clubs` | Logged-in admins | Manage club crests used as logo hints (linked from the grid builder, not the main nav) |
| `/tension` | Logged-in users | Play Tension - landing, player setup, live game, and results all in one flow |
| `/admin/tension-questions` | Logged-in admins | Manage Tension questions (safe + tension answer lists) |
| `/admin/tension-categories` | Logged-in admins | Manage the autocomplete word lists used while answering |

### Setting up Google Sign-In

1. In the [Google Cloud Console](https://console.cloud.google.com/apis/credentials),
   create an OAuth 2.0 **Web application** client ID.
2. Add `http://localhost:5173` (and your real domain later) under
   "Authorized JavaScript origins".
3. Put that client ID in **both**:
   - `frontend/.env` → `VITE_GOOGLE_CLIENT_ID`
   - `backend/src/main/resources/application.properties` → `app.google.client-id`
   (they must match - the backend verifies the token was issued for this exact client ID).

### Admin account

A default admin (`admin` / `changeme123`) is seeded automatically on first
boot if the `admin_users` table is empty - **change the password
immediately**, either via `app.admin.default-username` /
`app.admin.default-password` before the first run, or later directly in the
database. There's no UI for creating additional admins in this version;
insert rows into `admin_users` directly (password hashed with BCrypt) if you
need more than one.

### JWT secret

`app.jwt.secret` in `application.properties` **must** be replaced with a
long random value before any real deployment (`openssl rand -base64 48` is
an easy way to generate one). Anyone who has this secret can mint valid
tokens, including admin ones.

## Data model

```
questions
├── question_id     BIGINT PK
├── question_text   TEXT
├── category        VARCHAR
├── difficulty_level INT      (1 = easiest ... 10 = hardest)
├── language        VARCHAR   (EN | DE | FR | ES | NO)
├── answer          VARCHAR   -- the single correct answer, no multiple choice
└── could_change    BOOLEAN   -- flags answers that can go stale (e.g. "current
                               -- Premier League top scorer") vs. permanent facts
                               -- (e.g. "capital of Norway"), so admins can find
                               -- and recheck them later

app_users                      admin_users
├── id            BIGINT PK    ├── id             BIGINT PK
├── email         VARCHAR      ├── username        VARCHAR
├── name          VARCHAR      └── password_hash   VARCHAR  (BCrypt)
├── google_subject VARCHAR
└── created_at    TIMESTAMP

saved_quizzes                       saved_quiz_questions
├── id            BIGINT PK         ├── id               BIGINT PK
├── owner_id      BIGINT FK -> app_users.id
├── title         VARCHAR           ├── saved_quiz_id    BIGINT FK -> saved_quizzes.id
├── language      VARCHAR           ├── order_index      INT     (preserves reordering)
└── created_at    TIMESTAMP         ├── question_text    TEXT
                                     ├── category         VARCHAR
                                     ├── difficulty_level  INT
                                     └── answer           VARCHAR
```

`app_users` and `admin_users` are intentionally separate tables with no
foreign key or conversion path between them.

`saved_quiz_questions` is a **frozen copy** of each question at the moment
the user hit "Save to My Quizzes" - not a foreign key back to `questions`.
If an admin later edits or deletes the original question, quizzes already
saved by users are unaffected; they show exactly what the user saved,
including whatever order they'd put the questions in. Both tables are
brand new, so unlike the schema changes further down, there's nothing to
migrate on an existing deployment - `ddl-auto=update` just creates them.

Each question belongs to exactly one language - there's no automatic
translation or linking between a question and its equivalent in another
language. An admin adding "capital of France" in English, German, French,
Spanish and Norwegian creates five independent rows. This keeps the model
simple, at the cost of the admin having to enter each language by hand.

```
athletes                            grids
├── id       BIGINT PK              ├── id               BIGINT PK
├── name     VARCHAR                ├── title            VARCHAR
├── sport    VARCHAR (FOOTBALL|CYCLING)  ├── theme        TEXT
├── team     VARCHAR                ├── sport            VARCHAR
└── photo_url VARCHAR (nullable; shown only once a tile is solved)
                                     ├── week_start_date  DATE   (grid is "live" for this Mon-Sun)
grid_candidates                     └── max_strikes      INT
├── id         BIGINT PK
├── grid_id    BIGINT FK -> grids.id       grid_entries
└── athlete_id BIGINT FK -> athletes.id    ├── id          BIGINT PK
                                            ├── grid_id     BIGINT FK -> grids.id
grid_attempts                              ├── athlete_id  BIGINT FK -> athletes.id
├── id            BIGINT PK                ├── hint_label  VARCHAR  (e.g. "FW")
├── grid_id       BIGINT FK -> grids.id    ├── hint_value  INT      (e.g. 14)
├── user_id       BIGINT FK -> app_users.id├── order_index INT
├── strikes_used  INT                      ├── club_id     BIGINT FK -> clubs.id  (nullable)
├── completed     BOOLEAN                  └── show_logo   BOOLEAN (nullable; null = shown)
├── overtime      BOOLEAN
├── revealed      BOOLEAN                  clubs
└── (solved entry ids in a separate         ├── id       BIGINT PK
    grid_attempt_solved_entries join table) ├── name     VARCHAR
                                             ├── sport    VARCHAR (FOOTBALL|CYCLING)
                                             ├── logo_url VARCHAR (a hosted image URL, not a file upload)
                                             └── color    VARCHAR (hex; nullable, falls back to default gold)
```

`grid_candidates` is the full searchable pool for a grid's guess box -
decoys and correct answers alike. `grid_entries` is the subset that are
actually correct, each carrying the hint shown on its tile before it's
solved. An athlete only gets a hint (and only becomes visible on a tile)
by being in `grid_entries`; being in `grid_candidates` alone just makes
them guessable - and wrong, if guessed. One `grid_attempts` row tracks a
single user's progress on a single grid (unique on `grid_id` + `user_id`),
so progress survives a refresh or a later visit.

`club_id` and `show_logo` on `grid_entries` are both nullable by design -
that's what makes this a safe addition to an existing, non-empty
`grid_entries` table (`ddl-auto=update` just adds them, no manual SQL
needed, unlike the earlier `difficulty`/`language` migration below). A
null `club_id` means no logo; a null `show_logo` is treated as "shown" -
see `GridEntry.isShowLogo()` in the backend.

```
tension_questions                   tension_answer_entries
├── id               BIGINT PK      ├── id          BIGINT PK
├── title            VARCHAR        ├── question_id BIGINT FK -> tension_questions.id
├── main_category    VARCHAR        ├── rank        INT      (own 1..N sequence per tension flag)
└── answers_category VARCHAR        ├── text        VARCHAR
                                     └── tension     BOOLEAN
tension_categories
├── id   BIGINT PK
└── name VARCHAR (unique)

tension_category_options            (a plain string list, one row per word,
├── category_id BIGINT FK            joined back to tension_categories - an
└── option_text VARCHAR              @ElementCollection, not its own entity)
```

All four tables are brand new, so there's nothing to migrate here either.
`rank` on `tension_answer_entries` is scoped *within* its own `tension`
flag - safe answers are ranked 1..10 independently from tension answers,
which have their own separate ranking - matching the original app's
two-separate-lists design rather than one continuous ranking.

```
submitted_questions
├── id               BIGINT PK
├── question_text    VARCHAR
├── category          VARCHAR
├── difficulty_level  INT
├── language          VARCHAR
├── answer            VARCHAR
├── could_change      BOOLEAN
├── submitted_by      BIGINT FK -> app_users.id
├── status            VARCHAR (PENDING|APPROVED|REJECTED)
├── rejection_reason   VARCHAR (nullable - only set when REJECTED)
└── created_at         TIMESTAMP
```

Also brand new - approving one copies its fields into a fresh row in the
existing `questions` table (no FK back to the submission); rejecting just
sets `status` and `rejection_reason` in place.

```
quiz_templates                      quiz_template_questions
├── id         BIGINT PK            ├── id               BIGINT PK
├── title      VARCHAR              ├── template_id       BIGINT FK -> quiz_templates.id
├── language   VARCHAR              ├── order_index       INT
└── created_at TIMESTAMP            ├── question_text     VARCHAR
                                     ├── category          VARCHAR
                                     ├── difficulty_level  INT
                                     └── answer            VARCHAR
```

Deliberately shaped identically to `saved_quizzes`/`saved_quiz_questions` -
a template is really just a `SavedQuiz` with no owner. Copying one calls
the same question-snapshot logic and creates a normal, independent
`SavedQuiz` row for whoever copied it.

> **If you already deployed the previous schema** (with a `difficulty`
> enum column): `ddl-auto=update` only *adds* columns, it doesn't migrate
> data, and the new `difficulty_level`/`language` columns are `NOT NULL`
> with no default - which will fail against a table that already has rows.
> Run this once against your database before redeploying:
> ```sql
> ALTER TABLE questions ADD COLUMN difficulty_level INT;
> ALTER TABLE questions ADD COLUMN language VARCHAR(10);
> UPDATE questions SET
>   difficulty_level = CASE difficulty WHEN 'EASY' THEN 2 WHEN 'MEDIUM' THEN 5 ELSE 8 END,
>   language = 'EN';
> ALTER TABLE questions ALTER COLUMN difficulty_level SET NOT NULL;
> ALTER TABLE questions ALTER COLUMN language SET NOT NULL;
> ALTER TABLE questions DROP COLUMN difficulty;
> ```
> This is exactly the kind of thing Flyway/Liquibase exists to handle
> automatically - worth adopting before the schema changes again.

## Running it locally

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Starts on `http://localhost:8080`, using an in-memory H2 database seeded
with sample questions in all five languages (including one flagged
`could_change = true`) and a default admin account, so you have something
to try immediately. The H2 console is at `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:quizdb`, user `sa`, empty password).

To switch to Postgres for a real deployment, edit
`backend/src/main/resources/application.properties`: comment out the H2
block and uncomment the Postgres block, then create the `quizdb` database.
`spring.jpa.hibernate.ddl-auto=update` will create the tables for you on
first boot - swap to a proper migration tool (Flyway/Liquibase) before
production use.

### Frontend

```bash
cd frontend
npm install
cp .env.example .env   # set the backend URL and your Google client ID
npm run dev
```

Starts on `http://localhost:5173`.

## API summary

| Method | Path                          | Auth required     | Purpose                                      |
|--------|-------------------------------|--------------------|-----------------------------------------------|
| POST   | `/api/auth/google`            | none               | Exchange a Google ID token for our JWT        |
| POST   | `/api/auth/admin/login`       | none (rate-limited - see below) | Admin username/password login    |
| GET    | `/api/quiz/categories?language=EN` | none          | Categories that have questions in that language |
| POST   | `/api/quiz/generate`          | any logged-in user | Generate a quiz from per-category selections  |
| POST   | `/api/quiz/replace-question`  | any logged-in user | Swap one question for another in the same category |
| POST   | `/api/quiz/add-questions`     | any logged-in user | Add N more questions (new or existing category) to a quiz being reviewed/edited |
| POST   | `/api/quiz/export/pdf`        | any logged-in user | Download a finalized quiz as PDF              |
| POST   | `/api/quiz/saved`             | any logged-in user | Save a finalized quiz ("My Quizzes")          |
| GET    | `/api/quiz/saved`             | any logged-in user | List the caller's own saved quizzes           |
| GET    | `/api/quiz/saved/{id}`        | any logged-in user | Fetch one of the caller's own saved quizzes   |
| PUT    | `/api/quiz/saved/{id}`        | any logged-in user | Overwrite a saved quiz's title/questions in place |
| DELETE | `/api/quiz/saved/{id}`        | any logged-in user | Delete one of the caller's own saved quizzes  |
| GET    | `/api/admin/questions`        | ADMIN role         | List all questions                            |
| GET    | `/api/admin/questions/stats`  | ADMIN role         | Coverage breakdown (count/difficulty range per category+language) |
| POST   | `/api/admin/questions/import` | ADMIN role         | Bulk-add questions from a CSV upload          |
| POST   | `/api/admin/questions`        | ADMIN role         | Create a question                             |
| PUT    | `/api/admin/questions/{id}`   | ADMIN role         | Update a question                             |
| DELETE | `/api/admin/questions/{id}`   | ADMIN role         | Delete a question                             |
| GET    | `/api/grids/active`           | any logged-in user | This week's live grid(s)                      |
| GET    | `/api/grids/archive`          | any logged-in user | Past grids, most recent first                 |
| GET    | `/api/grids/{id}/play`        | any logged-in user | Grid + the caller's own attempt state (creates one if none exists) |
| GET    | `/api/grids/{id}/candidates`  | any logged-in user | Search that grid's guessable pool by name     |
| POST   | `/api/grids/{id}/guess`       | any logged-in user | Submit a guess (athlete id)                   |
| POST   | `/api/grids/{id}/overtime`    | any logged-in user | Keep guessing after running out of strikes (no longer scored) |
| POST   | `/api/grids/{id}/reveal`      | any logged-in user | Give up and reveal the remaining answers      |
| GET    | `/api/admin/grids`            | ADMIN role         | List all grids                                |
| GET    | `/api/admin/grids/{id}`       | ADMIN role         | Full grid detail for editing                  |
| POST   | `/api/admin/grids`            | ADMIN role         | Create a grid (candidates + entries)          |
| PUT    | `/api/admin/grids/{id}`       | ADMIN role         | Replace a grid's candidates/entries           |
| DELETE | `/api/admin/grids/{id}`       | ADMIN role         | Delete a grid (and everyone's progress on it) |
| GET    | `/api/admin/athletes`         | ADMIN role         | Search the roster (`sport`, `team`, `name` filters) |
| POST   | `/api/admin/athletes`         | ADMIN role         | Add an athlete                                |
| PUT    | `/api/admin/athletes/{id}`    | ADMIN role         | Update an athlete                             |
| DELETE | `/api/admin/athletes/{id}`    | ADMIN role         | Delete an athlete (blocked if used in a grid) |
| GET    | `/api/admin/clubs`            | ADMIN role         | Search clubs (`sport`, `name` filters)        |
| POST   | `/api/admin/clubs`            | ADMIN role         | Add a club                                    |
| PUT    | `/api/admin/clubs/{id}`       | ADMIN role         | Update a club                                 |
| DELETE | `/api/admin/clubs/{id}`       | ADMIN role         | Delete a club (blocked if used as a logo on any entry) |
| GET    | `/api/tension/questions/random` | any logged-in user | Draw random questions to start a game (`count`, `category`) |
| GET    | `/api/tension/questions/categories` | any logged-in user | Distinct main categories, for the landing page's filter |
| GET    | `/api/tension/categories/{name}/options` | any logged-in user | Autocomplete word list for the answer box |
| GET    | `/api/admin/tension/questions` | ADMIN role         | List all Tension questions                    |
| GET    | `/api/admin/tension/questions/{id}` | ADMIN role    | Full question detail for editing              |
| POST   | `/api/admin/tension/questions` | ADMIN role         | Create a question (safe + tension answer lists) |
| PUT    | `/api/admin/tension/questions/{id}` | ADMIN role    | Replace a question's answer lists              |
| DELETE | `/api/admin/tension/questions/{id}` | ADMIN role    | Delete a question                              |
| GET    | `/api/admin/tension/categories` | ADMIN role        | List answer categories                         |
| POST   | `/api/admin/tension/categories` | ADMIN role        | Add an answer category                         |
| PUT    | `/api/admin/tension/categories/{id}` | ADMIN role   | Update an answer category                      |
| DELETE | `/api/admin/tension/categories/{id}` | ADMIN role   | Delete an answer category                      |
| POST   | `/api/questions/submissions`  | any logged-in user | Submit a question for review                  |
| GET    | `/api/questions/submissions/mine` | any logged-in user | Your own submissions, any status          |
| GET    | `/api/admin/question-submissions` | ADMIN role     | All submissions, for the review queue         |
| POST   | `/api/admin/question-submissions/{id}/approve` | ADMIN role | Approve - copies into the shared bank |
| POST   | `/api/admin/question-submissions/{id}/reject` | ADMIN role | Reject with a reason (stays usable by the submitter) |
| GET    | `/api/quiz-templates`         | any logged-in user | Browse published templates                    |
| POST   | `/api/quiz-templates/{id}/copy` | any logged-in user | Copy a template into your own My Quizzes     |
| GET    | `/api/admin/quiz-templates`   | ADMIN role         | List all templates                            |
| GET    | `/api/admin/quiz-templates/{id}` | ADMIN role      | Full template detail for editing              |
| POST   | `/api/admin/quiz-templates`   | ADMIN role         | Publish a new template                        |
| PUT    | `/api/admin/quiz-templates/{id}` | ADMIN role      | Replace a template's questions                |
| DELETE | `/api/admin/quiz-templates/{id}` | ADMIN role      | Delete a template (existing copies unaffected) |

### Admin login rate limiting

`/api/auth/admin/login` allows 5 failed attempts per IP per 15-minute
window before returning `429 Too Many Requests`; a successful login clears
the count. This is intentionally simple - in-memory, per Render instance -
which is fine for this app's single-instance deployment but would need a
shared store (Redis, etc.) if it ever ran across multiple instances behind
a load balancer. See `LoginRateLimiter`.

### Bulk question import

`POST /api/admin/questions/import` accepts a CSV file (`multipart/form-data`,
field name `file`) with a header row:

```csv
question,category,difficulty,language,answer,couldChange
"What is the capital of Italy?",Geography,2,EN,Rome,false
"Premier League's all-time top scorer?",Sports,6,EN,Alan Shearer,true
```

- `difficulty`: 1-10
- `language`: one of `EN`, `DE`, `FR`, `ES`, `NO`
- `couldChange`: optional, defaults to `false` if omitted (`true`/`yes`/`1` all count as true)

Invalid rows are skipped and reported individually (with a reason and row
number) rather than failing the whole import - the response shape is
`{ imported, skipped, errors[] }`.

Files saved as "CSV UTF-8" from Excel or Google Sheets (the right choice
for accented characters) include a leading byte-order-mark - the importer
strips it automatically, so you don't need to worry about it either way.

`POST /api/quiz/generate` body shape:

```json
{
  "title": "Sarah's 30th Birthday Quiz",
  "language": "EN",
  "minDifficulty": 3,
  "maxDifficulty": 7,
  "categorySelections": [
    { "category": "Sport", "numberOfQuestions": 3 },
    { "category": "Film", "numberOfQuestions": 4 },
    { "category": "Literature", "numberOfQuestions": 6 }
  ]
}
```

`minDifficulty`/`maxDifficulty` are both optional (1-10, omit either or
both for "any difficulty").

The response includes a `warnings` array if any category came up short
(e.g. only 2 of the 4 requested Film questions exist yet).

## Deployment (Neon/Supabase + Render + GitHub Pages)

This is the stack the project is set up for out of the box: Postgres on
Neon or Supabase, the Spring Boot backend on Render, and the Vue frontend
on GitHub Pages. Total cost: $0, as long as you're fine with Render's free
web service spinning down after 15 minutes of inactivity (the first
request after a quiet spell takes up to ~1 minute to wake back up).

### 1. Database - Neon or Supabase

Both have a Postgres free tier that doesn't expire (unlike Render's own
free Postgres, which is deleted 30 days after creation). Either works
identically here since the app just talks JDBC/Postgres:

1. Create a project/database on [neon.tech](https://neon.tech) or
   [supabase.com](https://supabase.com).
2. Copy the connection details they give you. You'll need them as three
   separate Render env vars in step 2: a JDBC URL, username, and password -
   e.g. from a connection string like
   `postgresql://myuser:mypassword@ep-abc-123.neon.tech/quizdb?sslmode=require`,
   set:
   - `DATABASE_URL=jdbc:postgresql://ep-abc-123.neon.tech/quizdb?sslmode=require`
     (note the added `jdbc:` prefix, and the username/password stripped back
     out of the URL - the JDBC driver wants those as separate properties,
     not embedded in the URL the way `psql`-style connection strings show them)
   - `DATABASE_USERNAME=myuser`
   - `DATABASE_PASSWORD=mypassword`

> **Using Supabase specifically:** its *direct* connection host
> (`db.<project-ref>.supabase.co`) only resolves to an IPv6 address unless
> you're on a paid plan with the IPv4 add-on - and **Render doesn't support
> IPv6**, so that host will time out from a Render service. Use Supabase's
> **connection pooler** instead: on your project's **Connect** dialog, pick
> **"Session pooler"** (not "Transaction pooler" - transaction-mode pooling
> can break Hibernate's prepared-statement caching). The pooler gives you a
> different host (`aws-0-<region>.pooler.supabase.com`) *and* a different
> username - it becomes `postgres.<project-ref>` instead of plain
> `postgres`. Neon doesn't have this problem; its default host is IPv4-reachable.

### 2. Backend - Render

**Render has no native Java runtime** (only Node, Python, Ruby, Go, Rust and
Elixir build natively) - so the backend deploys as a **Docker** service.
`backend/Dockerfile` handles the whole build (Maven compiles the jar, a
slim JRE image runs it), and `render.yaml` points Render at it:

1. Push this repo to GitHub.
2. In Render, **New -> Blueprint**, point it at your repo. Render reads
   `render.yaml` and creates the `quiz-backend` web service with
   `runtime: docker`.
3. Fill in the env vars Render will prompt for (they're listed with
   `sync: false` in `render.yaml` so nothing sensitive is committed):
   `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`,
   `CORS_ALLOWED_ORIGINS`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`,
   `ADMIN_DEFAULT_PASSWORD`.
   - `CORS_ALLOWED_ORIGINS` should be your GitHub Pages origin, e.g.
     `https://your-username.github.io` (no trailing path, no trailing slash).
   - `JWT_SECRET`: generate with `openssl rand -base64 48`.
4. Deploy. Render gives you a URL like `https://quiz-backend-xxxx.onrender.com`
   - you'll need it in step 3.

> **Picking a region:** unlike Render's manual "New -> Web Service" wizard
> (which shows a region dropdown), a **Blueprint's region isn't chosen in
> the dashboard at all** - it's set via the `region:` field on the service
> in `render.yaml` (currently `frankfurt`; other options are `oregon`,
> `ohio`, `virginia`, `singapore`). **Match this to your database's
> region**, not just "closest to you" - every request that touches the
> database pays for the round-trip between them, and a mismatch (e.g.
> backend in Oregon, database in Ireland) adds a very noticeable delay to
> every login and every quiz generation. Render also **doesn't support
> changing a service's region after creation** - to move it, create a new
> service in the target region (via Blueprint again, after editing
> `region:`), verify it works, repoint the frontend's `VITE_API_BASE_URL`
> at it, then delete the old one.

> **Memory on Starter (512Mi):** the Dockerfile explicitly caps the JVM
> (`-Xmx280m -XX:MaxMetaspaceSize=160m`) and `application-prod.properties`
> caps Tomcat's thread pool at 20, rather than leaving everything at JVM/
> Tomcat defaults. Without this, the container can get OOM-killed by
> Render outright (visible in the deploy logs as `Out of memory (used over
> 512Mi)` followed by a forced restart) - Java's default heap sizing
> leaves **Metaspace** (class metadata) completely uncapped, and as more
> entities/services/controllers get added over time, Metaspace usage grows
> right along with them. If this happens again after further growth, these
> numbers may need raising, and/or it's a sign the app has outgrown a
> 512Mi instance entirely - upgrading the Render plan for more RAM is the
> more durable fix at that point, not something further JVM tuning can
> keep working around indefinitely.

> **If you already created the service manually** (via "New -> Web
> Service" rather than "New -> Blueprint") and it's failing with something
> like `Couldn't find a package.json file` / `yarn run v1.22...` - that
> means it got created with the Node runtime instead of Docker (an earlier
> version of this guide mistakenly specified a `runtime: java`, which
> doesn't exist on Render and silently fell back to Node). Render doesn't
> let you switch a manually-created service's runtime after the fact, so
> the fix is to delete that service and recreate it either via **New ->
> Blueprint** (recommended - picks up `runtime: docker` automatically), or
> manually via **New -> Web Service** with **Runtime: Docker**,
> **Root Directory: `backend`**, **Dockerfile Path: `./Dockerfile`**,
> **Docker Build Context Directory: `.`**.

### 3. Frontend - GitHub Pages

1. In your repo: **Settings -> Pages -> Source -> GitHub Actions** (not
   "Deploy from a branch" - the included workflow handles the build itself).
2. **Settings -> Secrets and variables -> Actions -> Variables tab**
   (plain repo *variables*, not secrets - these values end up embedded in
   the public JS bundle regardless, so there's nothing to hide):
   - `VITE_API_BASE_URL` = `https://quiz-backend-xxxx.onrender.com/api`
     (your Render URL from step 2, plus `/api`)
   - `VITE_GOOGLE_CLIENT_ID` = your Google OAuth client ID
3. Push to `main`. The `.github/workflows/deploy-frontend.yml` workflow
   builds the app (setting the correct base path for a project site
   automatically from the repo name) and publishes it to GitHub Pages.
4. Your app will be live at `https://your-username.github.io/your-repo-name/`.

Notes on how the frontend is wired for this:
- **Routing:** the router uses hash-based history
  (`https://.../#/generate` instead of `https://.../generate`). GitHub
  Pages can't do server-side rewrites, so a plain path-based route 404s on
  refresh; hash routing sidesteps that entirely with no extra server
  config needed.
- **Base path:** `vite.config.js` reads a `BASE_PATH` env var at build
  time (the workflow sets it to `/<repo-name>/` automatically) so assets
  resolve correctly under a GitHub Pages project subpath.

### 4. Point Google Sign-In at the new origin

Back in the [Google Cloud Console](https://console.cloud.google.com/apis/credentials),
add `https://your-username.github.io` to the OAuth client's "Authorized
JavaScript origins" (alongside `http://localhost:5173` for local dev).
Google Sign-In will silently fail on any origin that isn't listed here.

### Checklist if something doesn't work after deploying

- Blank page / console CORS errors → `CORS_ALLOWED_ORIGINS` on Render
  doesn't exactly match your GitHub Pages origin.
- Google button doesn't render or sign-in silently fails → origin missing
  from Google Cloud Console, or `VITE_GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_ID`
  don't match between frontend and backend.
- First request after a while is very slow → expected Render free-tier
  cold start (~1 minute), not a bug.
- `error Couldn't find a package.json file` / service runs `yarn start` →
  the Render service was created with the Node runtime instead of Docker -
  see the callout in the Render section above.
- Connection hangs, then times out, with a Supabase `DATABASE_URL` →
  you're likely on Supabase's direct-connection host, which is IPv6-only;
  switch to the session pooler host (see the database section above).
- 500 errors on every request → check the Render service logs; almost
  always a missing/misformatted env var (`DATABASE_URL` is the usual
  culprit - it needs the `jdbc:` prefix even though Neon/Supabase give you
  a plain `postgresql://` string).

## Known gaps to close before production

- **No automated tests** were included - worth adding `@DataJpaTest` for
  the repository/service layer and a couple of `@WebMvcTest` slices for
  the controllers (including one asserting a `USER`-role token gets a 403
  from `/api/admin/**`).
- **DB migrations:** replace `ddl-auto=update` with Flyway or Liquibase
  once the schema stabilizes.
- **No password reset / admin-management UI** - handled via direct DB
  access for now, noted above.
- **JWTs aren't revocable** - logging out just deletes the token
  client-side; a stolen token remains valid until it expires
  (`app.jwt.expiration-minutes`). A production version would want a
  denylist or short-lived tokens with refresh.
- **Render's free web service tier** spins down after 15 minutes idle and
  caps you at 750 instance-hours/month - fine for a personal project, not
  for anything that needs to always be instantly responsive. Upgrading to
  a paid Render instance type removes both limits without any code change.

## Scalability

This app was built and tuned for its actual scope - personal/small-group
use with a question bank of a few hundred to a few thousand rows. Several
things below were deliberate simplifications that were the right call at
that scale, and would need revisiting at real scale (thousands of users,
tens of thousands of questions):

- **Fixed (this pass): quiz generation no longer loads every question in
  a language into memory before filtering.** `QuizService.candidatePool()`
  used to call `findByLanguage()` (fetching the *entire* language's
  question set as Hibernate entities) and then filter by category and
  difficulty in a Java stream. This ran on every quiz generation, every
  discard-and-replace, and every "add more questions" call - the single
  most frequently hit heavy operation in the app - so it was the first
  thing worth fixing. `QuestionRepository.findCandidates()` now does the
  filtering in the SQL query itself (`WHERE language = ? AND
  difficulty_level BETWEEN ? AND ? AND LOWER(category) = LOWER(?)`), so
  only matching rows are ever loaded.

  **This fix isn't fully realized without a matching index.** Without
  one, Postgres still does a full sequential scan to answer that query -
  much cheaper than deserializing every row into Hibernate entities
  client-side, but still not what you want at real volume. Run this once
  (safe against an existing table, doesn't affect any data):
  ```sql
  CREATE INDEX IF NOT EXISTS idx_questions_lang_cat_diff
  ON questions (language, category, difficulty_level);
  ```

- **Still todo - same root cause, different endpoints.** The exact same
  "fetch everything, filter in memory" pattern exists in
  `AthleteService.search()`, `TensionQuestionService.getRandom()`/
  `getDistinctMainCategories()`, and the admin question-bank list (which
  sends the *entire* table to the browser and filters client-side in
  Vue). None of these hurt at a few hundred rows; all of them would at
  tens of thousands. Same fix shape each time: push the filter into the
  repository query, and paginate rather than returning full tables.
- **No caching layer.** Categories, clubs, and athletes are re-queried
  from Postgres on every request even though they barely ever change.
  Even a short-TTL in-memory cache (Spring's `@Cacheable` needs no new
  infra) on these reference-data lookups would cut a large share of DB
  load with no logic changes. Pairs naturally with the point above -
  worth doing both together.
- **Login rate limiting is in-memory** (`LoginRateLimiter`), which is a
  hard blocker for horizontal scaling specifically: each backend instance
  would keep its own independent counter, silently turning "5 attempts"
  into "5 × instance count." This needs to move to something shared
  (Redis, or a DB-backed counter) *before* running more than one backend
  instance - not just a nice-to-have at that point.
- **No rate limiting on gameplay/generation endpoints** (quiz generation,
  Tension guesses, Weekly Grid guesses) - only admin login has a limiter
  today. PDF generation in particular is genuinely CPU-heavy per request.
  At real concurrent load this is where you'd see slowness before
  anything above becomes the bottleneck.
- **Connection pool sizing**: if the backend ever runs as multiple
  instances, each one's HikariCP pool adds to the total connections
  against Supabase's pooler, which has its own ceiling per plan tier.
  Capacity planning, not a code change - just worth knowing before adding
  instance #2.

If traffic or data volume ever genuinely trended toward "viral," the
order I'd tackle this in: **add basic observability first** (you can't
tell which of the above is the actual bottleneck from Render's logs
alone), then the remaining "fetch everything" endpoints + caching
together (same root cause), then the login rate limiter fix - but only
once actually adding a second instance, since it's not urgent before
that point.
