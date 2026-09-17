<template>
  <div class="dashboard">
    <p class="dashboard-welcome">Welcome back, <strong>{{ auth.state.displayName }}</strong></p>

    <section class="dashboard-hero">
      <h1>Create a quiz</h1>
      <p class="page-subtitle landing-lede">
        Build a custom quiz for friends, colleagues, or your local pub night.
      </p>
      <div class="dashboard-hero-actions">
        <router-link to="/generate" class="btn btn-primary btn-lg">Create a quiz →</router-link>
        <router-link to="/join" class="btn btn-secondary btn-lg">Join a game with a code →</router-link>
      </div>
    </section>

    <h2 class="dashboard-section-title">More ways to play</h2>
    <div class="dashboard-features">
      <!-- Grouped into pairs so mobile can lay each pair out as its own
           2-tall column and scroll horizontally between columns (see
           .dashboard-feature-column's mobile rule in style.css). On desktop
           display:contents makes this wrapper invisible to layout, so cards
           flow straight into the existing wrapping grid as before. -->
      <div v-for="(column, i) in featureCardColumns" :key="i" class="dashboard-feature-column">
        <router-link
          v-for="card in column"
          :key="card.to"
          :to="card.to"
          class="dashboard-feature-card"
          :style="{ '--card-accent': card.accent }"
        >
          <h3>{{ card.title }}</h3>
          <p>{{ card.description }}</p>
          <span class="dashboard-feature-play">Play →</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import auth from '../services/auth'

// One card per dashboard link - was hand-duplicated markup per game before,
// now a single loop so the visual treatment (accent border, Play pill) lives
// in one place instead of copy-pasted 13 times. Same routes, labels and
// descriptions as before - no destination changed.
// Accent groups (not a per-card rotation anymore): weekly solo quizzes share
// one color, every other game mode shares a second, and the three non-game
// utility cards (manage quizzes, suggest, report) each get their own -
// "My quizzes" isn't a game either, so it's grouped with "Quiz templates" as
// the closest thing to a fourth explicit group (manage-your-quizzes).
const WEEKLY_QUIZ_ACCENT = 'var(--teal)'
const GAME_ACCENT = 'var(--violet)'
const QUIZ_MANAGEMENT_ACCENT = 'var(--gold)'

const featureCards = [
  { to: '/my-quizzes?tab=templates', title: 'Quiz templates', description: 'Pre-made quizzes published by an admin - download a PDF right away, or copy one to edit.', accent: QUIZ_MANAGEMENT_ACCENT },
  { to: '/weekly-grid', title: 'Weekly grid', description: "Guess every answer that fits this week's theme before you run out of strikes.", accent: WEEKLY_QUIZ_ACCENT },
  { to: '/starting-xi', title: 'Starting XI', description: "Guess a full lineup, position by position, before the week's board runs out of lives.", accent: WEEKLY_QUIZ_ACCENT },
  { to: '/grid-battle', title: 'Grid Battle', description: 'A pass-the-device multiplayer version of Weekly Grid - take turns, or lose a life trying.', accent: GAME_ACCENT },
  { to: '/starting-xi-battle', title: 'XI Battle', description: 'Same idea as Grid Battle, for a Starting XI board - take turns naming the lineup.', accent: GAME_ACCENT },
  { to: '/tension', title: 'Tension', description: 'A pass-the-device party quiz - push for a high-value guess, or play it safe.', accent: GAME_ACCENT },
  { to: '/501', title: '501', description: 'A darts-style countdown from 501 - 1v1, checkout between 0 and -10 to win.', accent: GAME_ACCENT },
  { to: '/imposter', title: 'Imposter', description: "One player doesn't get the answer - find out who by asking around the table.", accent: GAME_ACCENT },
  { to: '/bullseye', title: 'Bullseye', description: 'Everyone answers, lowest score is eliminated each round, until one player is left.', accent: GAME_ACCENT },
  { to: '/flashback', title: 'Flashback', description: "Guess the exact year from a clue - it gets easier each round, but the points don't wait.", accent: GAME_ACCENT },
  // No standalone card for Penalty Shootout - it's reached from the XI
  // Battle card above instead, not as its own destination.
  { to: '/my-quizzes', title: 'My quizzes', description: "Revisit, edit or re-download anything you've saved.", accent: QUIZ_MANAGEMENT_ACCENT },
  { to: '/suggest-question', title: 'Suggest a question', description: 'Add to the shared question bank - admin-reviewed, and usable in your own quizzes either way.', accent: 'var(--blue)' },
  { to: '/report-problem', title: 'Report a problem', description: 'Found a bug or something confusing? Let us know.', accent: 'var(--coral)' }
]

const featureCardColumns = computed(() => {
  const columns = []
  for (let i = 0; i < featureCards.length; i += 2) {
    columns.push(featureCards.slice(i, i + 2))
  }
  return columns
})
</script>
