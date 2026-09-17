<template>
  <div class="dashboard">
    <p class="dashboard-welcome">Welcome back, <strong>{{ auth.state.displayName }}</strong></p>

    <section class="dashboard-hero">
      <div class="dashboard-hero-icon">📝</div>
      <div class="dashboard-hero-body">
        <h1>Create a quiz</h1>
        <p class="page-subtitle landing-lede">
          Pick your categories, how many questions from each, a difficulty and a language -
          then reorder, swap out, search in specific questions, or remove anything before you save or print it.
        </p>
        <div class="dashboard-hero-actions">
          <router-link to="/generate" class="btn btn-primary btn-lg">Create a quiz →</router-link>
          <router-link to="/join" class="btn btn-secondary btn-lg">Join a game with a code →</router-link>
        </div>
      </div>
    </section>

    <h2 class="dashboard-section-title">More ways to play</h2>
    <div class="dashboard-features">
      <router-link
        v-for="card in featureCards"
        :key="card.to"
        :to="card.to"
        class="dashboard-feature-card"
        :style="{ '--card-accent': card.accent }"
      >
        <div class="dashboard-feature-icon">{{ card.icon }}</div>
        <h3>{{ card.title }}</h3>
        <p>{{ card.description }}</p>
        <span class="dashboard-feature-play">Play →</span>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import auth from '../services/auth'

// One card per dashboard link - was hand-duplicated markup per game before,
// now a single loop so the visual treatment (icon badge, accent border, Play
// pill) lives in one place instead of copy-pasted 13 times. Same routes,
// labels and descriptions as before - no destination changed.
const featureCards = [
  { to: '/my-quizzes?tab=templates', icon: '📄', title: 'Quiz templates', description: 'Pre-made quizzes published by an admin - download a PDF right away, or copy one to edit.', accent: 'var(--gold)' },
  { to: '/weekly-grid', icon: '🔲', title: 'Weekly grid', description: "Guess every answer that fits this week's theme before you run out of strikes.", accent: 'var(--teal)' },
  { to: '/starting-xi', icon: '⚽', title: 'Starting XI', description: "Guess a full lineup, position by position, before the week's board runs out of lives.", accent: 'var(--violet)' },
  { to: '/grid-battle', icon: '⚔️', title: 'Grid Battle', description: 'A pass-the-device multiplayer version of Weekly Grid - take turns, or lose a life trying.', accent: 'var(--coral)' },
  { to: '/starting-xi-battle', icon: '🥅', title: 'XI Battle', description: 'Same idea as Grid Battle, for a Starting XI board - take turns naming the lineup.', accent: 'var(--gold)' },
  { to: '/tension', icon: '🧩', title: 'Tension', description: 'A pass-the-device party quiz - push for a high-value guess, or play it safe.', accent: 'var(--teal)' },
  { to: '/501', icon: '🎯', title: '501', description: 'A darts-style countdown from 501 - 1v1, checkout between 0 and -10 to win.', accent: 'var(--violet)' },
  { to: '/imposter', icon: '🕵️', title: 'Imposter', description: "One player doesn't get the answer - find out who by asking around the table.", accent: 'var(--coral)' },
  { to: '/bullseye', icon: '🏹', title: 'Bullseye', description: 'Everyone answers, lowest score is eliminated each round, until one player is left.', accent: 'var(--gold)' },
  { to: '/flashback', icon: '⏳', title: 'Flashback', description: "Guess the exact year from a clue - it gets easier each round, but the points don't wait.", accent: 'var(--teal)' },
  // No standalone card for Penalty Shootout - it's reached from the XI
  // Battle card above instead, not as its own destination.
  { to: '/my-quizzes', icon: '📚', title: 'My quizzes', description: "Revisit, edit or re-download anything you've saved.", accent: 'var(--violet)' },
  { to: '/suggest-question', icon: '💡', title: 'Suggest a question', description: 'Add to the shared question bank - admin-reviewed, and usable in your own quizzes either way.', accent: 'var(--coral)' },
  { to: '/report-problem', icon: '🐛', title: 'Report a problem', description: 'Found a bug or something confusing? Let us know.', accent: 'var(--gold)' }
]
</script>
