# Brag Plan: Quizzes.no

## What is this app?
A dark, gold-accented quiz platform (live at quizzes.no) that started as "build a custom trivia quiz from a question bank" and quietly grew into ten different playable game modes — solo, pass-the-device, and live online with room codes — all sharing one signature reveal-and-lives visual language.

## The angle
The landing page still says "Quizzes, party games, and trivia — all in one place," like it's one thing. The dashboard underneath it is a wall of ten completely different games (Weekly Grid, Starting XI, Grid Battle, XI Battle, Tension, 501, Imposter, Bullseye, Flashback, plus the original quiz generator). The video's joke/flex is the gap between the modest pitch and the actual scope: an honest product tour where the count keeps climbing past what anyone expects from a quiz app.

## Hook (first 2-3 seconds)
The landing hero on screen exactly as shipped: "Quizzes, party games, and trivia — all in one place." Calm, undersold, dark-ink background with the soft gold/violet glow blobs.

## Key moments (the middle)
- The Dashboard's "More ways to play" card grid populating one by one — Weekly Grid, Tension, 501, Imposter, Bullseye, Flashback, Grid Battle, XI Battle — with a running counter ticking up in the corner, landing on "10 ways to play."
- Weekly Grid in play: a 4×4 tile board, a correct guess flipping a tile teal with the tile-pop animation, a lives-hearts row losing a heart with the strike-pop shake.
- A reveal ladder (Bullseye/Tension's shared card-stack UI) filling in row by row, distance bars growing, ending on a coral elimination banner.

## Outro / punchline
The app's own favicon mark — the gold "Q" on its dark rounded card — scales up into "Quizzes.no", under it a tagline earned by the count-up: "Ten ways to argue with your friends." Hold, then out.

## User flow worth showing
Entry → key action → result, using the app's real screens:
1. Dashboard grid → tap the Weekly Grid card (or Bullseye).
2. A guess is typed into the pill search box / an answer is picked.
3. Result: a tile pops correct (teal, scale-pop) or a reveal-ladder row animates in with its distance bar — the app's own signature "reveal" motion, not a generic checkmark.

## Tone
- Preset: app-store
- Creative direction: an honest, feature-card product tour — the app already renders itself as a grid of cards, so the video's structure mirrors the product's own UI instead of inventing one.
- Interpretation: clean title-case labels, confident but not loud, feature-card reveals with clean slide/wipe transitions (0.35-0.45s), no aggression — the surprise comes from volume of features shown matter-of-factly, not from hype language.

## Format: landscape — 1920x1080
## Duration: 20s

## Visual identity (from the project)
- Background: `#101119` (--ink), raised surface `#1a1c28` (--ink-raised)
- Accent: `#f2b705` (--gold, primary), plus `#3ddc97` (--teal, success), `#ff4d6d` (--coral, danger/elimination), `#8b7cff` (--violet, secondary accent)
- Text: `#f1efe7` (--text) on dark, dimmed `#9a9dae` (--text-dim) for secondary
- Display font: Space Grotesk (700 weight, headlines/card titles)
- Body font: Inter
- Mono accent: JetBrains Mono (used for scores/counters in the real app — good fit for the on-screen counter)
- Strongest visual element: the reveal-ladder card stack (rank badge + distance bar, fades/scales in per row) and the tile-pop / strike-pop micro-animations — these are the product's actual signature motion, already built as CSS keyframes (`tile-pop`, `strike-pop`, `bullseye-banner-pop`)

## Share copy (draft)
Built a quiz app. It did not stop at quizzes — 10 game modes, live multiplayer rooms, and a PDF export later: quizzes.no.

## Audio direction
- Role: sparse professional accents over a clean, confident bed
- Music: upbeat but restrained instrumental/electronic bed, steady mid-tempo (~100-115 BPM feel), nothing chaotic
- Music treatment: fades in under the hook at low volume, builds slightly through the card-grid count-up, small swell under the outro logo lock, quick fade on the final hold
- Music cue guidance: to be detected at composition time (no bundled preset chosen yet); target one strong cue at the count-up landing on "10", one at the elimination-banner beat, one at the logo lock
- Audio-reactive treatment: subtle — card entrances and the counter tick can nudge slightly with the beat; no waveform/bar visuals
- SFX posture: moderate, motion-matched — a soft card-arrival tick per dashboard card, a crisp pop on the tile/heart animations, a short "whoosh" on scene transitions, a clean UI chime on the logo lock
- Audio-coupled moments: dashboard cards arriving one by one (tick per card), counter incrementing toward 10, tile-pop/strike-pop moments, elimination banner impact
- Restraint rule: never let SFX read as generic UI stock sounds fighting the music; keep music underneath, never overpowering; no laugh track/hype-voice stingers — app-store tone stays clean

## Storyboard

### Scene 1 — The undersell — 3s
Landing hero recreated: dark ink background with the soft violet/gold glow blobs, headline "Quizzes, party games, and trivia — all in one place" in Space Grotesk, subtext line below in Inter/text-dim. Calm, static-feeling composition.
Sequential/interaction: none
Audio intent: quiet, calm open — sets a baseline before the reveal
Audio-coupled idea: none
Music: bed fades in low, restrained
Transition mood: clean slide → Scene 2

### Scene 2 — The real count — 5s
Cut to the Dashboard's "More ways to play" card grid. Cards slide/pop in one by one in real order: Weekly Grid, Starting XI, Grid Battle, XI Battle, Tension, 501, Imposter, Bullseye, Flashback (skip templates/my-quizzes/utility cards — game modes only). A small mono-font counter in the corner ticks 1→9(or 10 incl. quiz generator) in sync with arrivals, landing on a bold "10 ways to play."
Sequential/interaction: yes — 9-10 feature cards arrive one by one, title-case names, one-line real descriptions from the app (e.g. "Guess every answer that fits the week's theme"); counter increments with each arrival
Audio intent: build, mild excitement without breaking app-store restraint
Audio-coupled idea: soft tick per card arrival, counter increment synced to ticks
Transition mood: clean slide → Scene 3

### Scene 3 — Play it: Weekly Grid — 4s
Recreate the Weekly Grid board: a 4×4 tile grid (some tiles already solved teal, most unsolved dark), the sticky gold-outlined search pill above it. Simulate typing a guess into the search box, then a tile flips correct — scale-pop, teal glow, teal status badge — while the lives-hearts row above loses no heart here (clean hit) to keep this beat purely positive.
Sequential/interaction: yes — simulated typing into the guess box, then one tile performs its real tile-pop animation
Audio intent: satisfying, tactile "got it" moment
Audio-coupled idea: key-tick sounds while typing, a crisp pop synced to the tile animation
Transition mood: hard-ish clean cut (fast product-tour pace) → Scene 4

### Scene 4 — Reveal ladder — 4s
Recreate the shared reveal-ladder UI (Bullseye/Tension style): rank-badge cards fade/scale in one by one, each with a growing distance bar (teal → gold → coral by rank). Final row triggers a coral "eliminated" banner with its real pop-in animation.
Sequential/interaction: yes — 3-4 ladder rows arrive in sequence, distance bars animate width, banner pops last
Audio intent: dramatic beat within a still-restrained frame — the one moment allowed a bit more punch
Audio-coupled idea: a slightly heavier hit/impact synced to the elimination banner's pop
Transition mood: dramatic wipe (earned by this being the video's biggest beat) → Scene 5

### Scene 5 — One app, played together — 2s
Quick beat: a room/join screen moment — a short room code, a small row of player chips joining, the gold pulsing "Start" button (btn-ready-pulse) glowing. Communicates "online, with friends" fast without a full flow.
Sequential/interaction: yes — 2-3 player chips arrive quickly one after another
Audio intent: light, quick lift before the outro
Audio-coupled idea: soft chip-arrival ticks
Transition mood: clean slide → Scene 6

### Scene 6 — Outro / logo lock — 2s
The app's own favicon mark (gold "Q" on its dark rounded card) scales up and settles center-frame, "Quizzes.no" sets in Space Grotesk beside/below it, tagline beneath in smaller Inter: "Ten ways to argue with your friends." Hold.
Sequential/interaction: none — single confident settle
Audio intent: resolved, confident close
Audio-coupled idea: one clean chime/hit on the logo settle, matched to a music swell
Transition mood: soft hold → end

**Music mood for this video:** upbeat, clean, confident instrumental — app-store restraint, not chaotic hype
**Audio summary:** A quiet open, a building tick-driven card count-up, one bigger impact on the ladder's elimination beat, a light lift into the multiplayer flash, and a resolved chime on the logo lock — restrained throughout, never louder than the visuals it's supporting.
