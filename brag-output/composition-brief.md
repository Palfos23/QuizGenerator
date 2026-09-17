# Hyperframes Composition Brief: Quizzes.no

## Objective
Create a short launch-style brag video for Quizzes.no.

## Output
- Composition directory: `brag-output/composition/`
- Rendered video: `brag-output/brag.mp4`
- Format: landscape — 1920x1080
- Duration: 20 seconds (15-25s acceptable)

## Source Material
- Project root: `/Users/palfos/Downloads/quiz-app 4`
- Primary files read: `frontend/index.html`, `frontend/src/style.css`, `README.md`, `frontend/src/views/HomeView.vue`, `frontend/src/views/DashboardView.vue`, `frontend/public/favicon.svg`, `frontend/package.json`
- Product name: Quizzes.no (also "QuizMaker" internally; ship as "Quizzes.no" — it's the live domain and the favicon mark)
- Tagline / strongest claim: The landing page's own line — "Quizzes, party games, and trivia — all in one place" — undersells what's actually there.
- Key UI or visual moment to recreate: the Dashboard's "More ways to play" card grid (10 real game-mode cards), the Weekly Grid 4x4 tile board with its `tile-pop` correct animation, and the shared reveal-ladder UI (`.bullseye-reveal-row` / `.tension-reveal-row`) with rank badges + distance bars + the coral elimination banner (`.bullseye-elimination-banner`, `bullseye-banner-pop` keyframe).
- Copy that must appear verbatim:
  - "Quizzes, party games, and trivia — all in one place"
  - Real dashboard card titles: "Weekly grid", "Starting XI", "Grid Battle", "XI Battle", "Tension", "501", "Imposter", "Bullseye", "Flashback" (pick the subset that fits scene 2's timing; use their real one-line descriptions from `DashboardView.vue`)
  - Outro tagline: "Ten ways to argue with your friends."

## Creative Direction
- Tone preset: app-store
- Creative direction: honest feature-card product tour — the app already renders itself as a grid of cards, so the video's own structure mirrors the product's real UI instead of inventing a new visual language.
- Interpretation: title-case labels, confident but not loud, clean slide/wipe transitions (0.35-0.45s), no ALL CAPS hype, no exclamation points. The surprise is volume of real features shown matter-of-factly.
- Angle: the landing page says "all in one place" like it's one thing; the dashboard underneath is ten distinct games. The video's job is to walk from the modest pitch to the actual count.
- Hook: the real landing hero line, calm, on the real dark/glow background.
- Outro / punchline: the favicon "Q" mark locks in as "Quizzes.no", tagline "Ten ways to argue with your friends."
- Avoid:
  - Generic SaaS language ("streamline", "engagement", "seamless")
  - Abstract filler visuals (no unrelated stock motion graphics)
  - Redesigning the app's look — reuse its real dark/gold/teal/coral/violet system exactly

## Visual Identity
- Background: `#101119` (ink), raised surface `#1a1c28` (ink-raised)
- Text: `#f1efe7` (text), dim `#9a9dae` (text-dim)
- Accent: `#f2b705` (gold, primary), `#3ddc97` (teal, success/correct), `#ff4d6d` (coral, danger/elimination), `#8b7cff` (violet, secondary)
- Display font: Space Grotesk (700), fallback sans-serif
- Body font: Inter, fallback sans-serif; mono accent JetBrains Mono for counters/scores
- Visual references from the project: soft radial glow blobs behind hero content (violet top-left, gold top-right, per `body` background in `style.css`); pill-shaped gold-outlined search/guess inputs; rounded `border-radius: 20px` cards with `1px solid rgba(241,239,231,0.12)` borders; the gold gradient primary button (`linear-gradient(135deg, #ffce3d, #f2b705)`) with its `btn-ready-pulse` glow animation; the favicon: a `#1a1c28` rounded-rect card with a bold gold serif "Q"

## Storyboard
Use the full storyboard in `brag-output/brag-plan.md` as the creative contract. Scene summary:

1. The undersell — 3s — landing hero line + subtext on the real dark/glow background, calm and static.
2. The real count — 5s — Dashboard "More ways to play" cards arrive one by one (9-10 real game-mode cards, real titles + one-line copy), mono counter ticks up, lands on "10 ways to play."
3. Play it: Weekly Grid — 4s — 4x4 tile board, simulated typed guess into the gold-outlined pill input, one tile performs its real `tile-pop` correct animation (teal glow).
4. Reveal ladder — 4s — 3-4 reveal-ladder rows fade/scale in one by one with growing distance bars, final row triggers the coral elimination banner pop.
5. One app, played together — 2s — a room code + 2-3 player chips arriving, the gold `btn-ready-pulse` "Start" button glowing.
6. Outro / logo lock — 2s — favicon "Q" mark scales up, "Quizzes.no" sets beside it, tagline "Ten ways to argue with your friends." underneath, hold.

## Audio
- Audio role: sparse professional accents over a clean, confident bed
- Audio arc: quiet open → building tick-driven card count-up → one bigger impact on the ladder's elimination beat → light lift into the multiplayer flash → resolved chime on logo lock
- Music: Hyperframes to choose an upbeat-but-restrained instrumental bed (~100-115 BPM feel) from its own catalog/registry; not chaotic, not corporate-stock-sounding
- Music treatment: fade in low under scene 1, mild build through scene 2's count-up, small swell into scene 6's logo lock, quick fade on the final hold
- Music cue guidance: no bundled preset chosen yet — detect at composition time (`hyperframes beats` or `analyze_music_cues.py`); target one strong cue near the "10 ways to play" landing (end of scene 2), one near the elimination banner impact (scene 4), one at the logo lock (scene 6)
- Audio-reactive treatment: subtle — card entrances / counter ticks and the hero glow may breathe slightly with music RMS; no waveform/equalizer/particle visuals
- Audio-coupled moments:
  - Scene 2 — card arrivals — soft tick per card, counter increments synced to ticks
  - Scene 3 — simulated typing — key-tick sounds, then a crisp pop synced to the tile-pop animation
  - Scene 4 — reveal ladder rows — soft card-arrival sound per row; heavier impact synced to the elimination banner pop
  - Scene 5 — player chips — soft chip-arrival ticks
  - Scene 6 — logo lock — one clean chime/hit matched to the music swell
- SFX selection guidance: match real motion — card/tick sounds for card-like reveals, a crisp "pop" for the tile/heart micro-animations (these already exist as CSS keyframes: `tile-pop`, `strike-pop`, `bullseye-banner-pop` — mirror that energy), restrained UI clicks for typing, one clean announcement-style hit for the elimination banner and the logo lock
- SFX analysis guidance: use `skills/brag/assets/sfx/sfx-analysis.md` if present; prefer lower high-frequency-risk sounds for the repeated card/tick moments in scene 2
- Exact SFX choice: Hyperframes chooses exact filenames, timestamps, density, and volume based on the implemented animation
- Audio files: copy chosen music (and any Hyperframes-selected SFX) into `brag-output/composition/assets/`

## Hyperframes Instructions
Load the composition-building Hyperframes domain skills — `hyperframes-core` (composition contract + `data-*` timing), `hyperframes-animation` (motion), `hyperframes-creative` (design spec, beats, audio-reactive), `hyperframes-keyframes` (seek-safe keyframes), and `hyperframes-cli` (lint/check/render), all installed locally at `~/.claude/skills/hyperframes-*`. /brag is its own workflow: do not enter the `hyperframes` entry-point intent interview and do not route into its generic promo / launch-video workflow. Prefer native Hyperframes conventions over anything in `/brag`.

Requirements:
- Show at least one real UI, copy, or visual element from the source project (the dashboard card grid, the grid tiles, and the reveal ladder all qualify — use at least the dashboard grid and one game-play recreation).
- Keep all text readable in the final render.
- Keep the video within 15-25 seconds.
- Include the planned music/SFX layer (not disabled by the user).
- Treat this brief's audio notes as guidance, not a fixed cue sheet. Choose SFX after the visual animation exists.
- Treat music cue metadata as optional timing hints; ignore cues that hurt readability, scene pacing, or the product story.
- Major reveals may move toward nearby strong cues within about 0.15s. Smaller entrances may align to nearby beat points within about 0.10s. Use only 1-3 strong cue locks in this 20s video.
- Use SFX to support motion and interaction: card sounds for card-like reveals, short announcement cues for the elimination banner and logo lock, key/click sounds for the simulated typing, restraint elsewhere.
- Honor the planned music treatment (fade-in, mild build, swell, fade-out) using the best Hyperframes-supported implementation.
- Extract audio data and wire at least one existing visual element (hero glow, card presence) to RMS/frequency energy, subtly — no waveform/equalizer/particle graphics.
- Use local assets for audio and any required runtime/media dependencies when possible.
- Run `hyperframes check` before render — it is brag's single gate.
