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
   so you can read through questions without spoiling yourself.
4. Once happy, the user can **download the quiz as a PDF**, and/or hit
   **"Save to My Quizzes"** to keep a permanent copy they can come back to
   later at `/my-quizzes` - nothing is saved automatically just from
   generating a quiz, only on that explicit action.

The whole frontend is responsive and works on mobile - the top nav
collapses to a bottom tab bar below 760px wide.

### Languages

Questions can be written in English, German, French, Spanish or Norwegian.
This is content-level only: each question belongs to exactly one language
(see the data model note below), and a generated quiz is entirely in one
language at a time. The interface itself (buttons, labels, etc.) is still
English-only - translating the UI chrome would be a separate, bigger
project if you want it too.

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
| `/my-quizzes` | Logged-in users | View/re-download/delete previously saved quizzes |
| `/admin` | Everyone (but pointless without credentials) | Admin username/password login |
| `/admin/questions` | Logged-in admins | Manage the question bank |

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
| POST   | `/api/quiz/export/pdf`        | any logged-in user | Download a finalized quiz as PDF              |
| POST   | `/api/quiz/saved`             | any logged-in user | Save a finalized quiz ("My Quizzes")          |
| GET    | `/api/quiz/saved`             | any logged-in user | List the caller's own saved quizzes           |
| GET    | `/api/quiz/saved/{id}`        | any logged-in user | Fetch one of the caller's own saved quizzes   |
| DELETE | `/api/quiz/saved/{id}`        | any logged-in user | Delete one of the caller's own saved quizzes  |
| GET    | `/api/admin/questions`        | ADMIN role         | List all questions                            |
| GET    | `/api/admin/questions/stats`  | ADMIN role         | Coverage breakdown (count/difficulty range per category+language) |
| POST   | `/api/admin/questions/import` | ADMIN role         | Bulk-add questions from a CSV upload          |
| POST   | `/api/admin/questions`        | ADMIN role         | Create a question                             |
| PUT    | `/api/admin/questions/{id}`   | ADMIN role         | Update a question                             |
| DELETE | `/api/admin/questions/{id}`   | ADMIN role         | Delete a question                             |

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
