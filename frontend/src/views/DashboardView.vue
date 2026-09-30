<template>
  <div class="dashboard">
    <p class="dashboard-welcome">Welcome back, <strong>{{ auth.state.displayName }}</strong></p>

    <div class="dashboard-hero-row">
      <section class="dashboard-hero">
        <h1>Create a quiz</h1>
        <p class="page-subtitle landing-lede">
          Build a custom quiz for friends, colleagues, or your local pub night.
        </p>
        <div class="dashboard-hero-actions">
          <router-link to="/generate" class="btn btn-primary btn-lg">Create a quiz →</router-link>
        </div>
      </section>

      <!-- Not folded into the "Create a quiz" hero above - joining a game
           someone else is hosting has nothing to do with making a quiz, it's
           the entry point into the games/rooms side of the app. Still gets
           top billing of its own instead, right beside the other hero. -->
      <section class="dashboard-join-card">
        <h2>Join a game</h2>
        <p class="page-subtitle">Got a room code from someone hosting? Jump straight in.</p>
        <router-link to="/join" class="btn btn-secondary btn-lg">Join with a code →</router-link>
      </section>
    </div>

    <template v-for="section in dashboardSections" :key="section.title">
      <h2 class="dashboard-section-title">{{ section.title }}</h2>
      <div class="dashboard-features">
        <!-- Grouped into pairs so mobile can lay each pair out as its own
             2-tall column and scroll horizontally between columns (see
             .dashboard-feature-column's mobile rule in style.css). On desktop
             display:contents makes this wrapper invisible to layout, so cards
             flow straight into the existing wrapping grid as before. -->
        <div v-for="(column, i) in section.columns" :key="i" class="dashboard-feature-column">
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
    </template>

    <!-- Deliberately not another small tile in the grid above - buried among
         12 game cards it was easy to miss, and it's not a "way to play" like
         the rest of this page. A full-width, always-visible strip is the
         obvious/findable version of the same link. -->
    <router-link to="/report-problem" class="dashboard-report-callout">
      <div>
        <h3>Found a bug or something confusing?</h3>
        <p>Let us know and we'll take a look.</p>
      </div>
      <span class="btn btn-secondary">Report a problem →</span>
    </router-link>

    <!-- One nudge per day, per browser (see dismissDailyQuizNudge) - not on
         every dashboard visit, just the first one that finds today's quiz
         still un-submitted. Guests/admins never trigger the check that would
         show this at all (see the onMounted guard below). -->
    <div v-if="showDailyQuizNudge" class="modal-backdrop" @click.self="dismissDailyQuizNudge">
      <div class="modal" style="max-width:380px; text-align:center;">
        <h2 style="margin-top:0;">Today's Daily Quiz is waiting</h2>
        <p class="page-subtitle">
          {{ dailyQuizNudge.questionCount }} questions, pub-quiz style -
          {{ dailyQuizNudge.status === 'IN_PROGRESS' ? "pick up where you left off." : "takes just a few minutes." }}
        </p>
        <div style="display:flex; gap:8px; margin-top:16px;">
          <button class="btn btn-secondary" style="flex:1;" @click="dismissDailyQuizNudge">Maybe later</button>
          <router-link :to="`/daily-quiz/${dailyQuizNudge.id}`" class="btn btn-primary" style="flex:1;" @click="dismissDailyQuizNudge">
            {{ dailyQuizNudge.status === 'IN_PROGRESS' ? 'Continue' : 'Play now' }}
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import auth from '../services/auth'
import api from '../services/api'

// Cards are grouped into named sections (rendered as their own labeled block,
// see dashboardSections below) instead of one flat 13-card grid - weekly
// solo quizzes, the pass-the-device/online games, and the non-game "manage
// your quizzes" actions each read as their own thing now, not just a color.
// "Report a problem" isn't in any section - see the callout below instead.
const WEEKLY_QUIZ_ACCENT = 'var(--teal)'
const GAME_ACCENT = 'var(--violet)'
const QUIZ_MANAGEMENT_ACCENT = 'var(--gold)'

const dashboardSectionDefs = [
  {
    title: 'Daily & weekly quizzes',
    cards: [
      { to: '/daily-quiz', title: 'Daily quiz', description: '15 random questions, pub-quiz style - free text, a fresh set every day.', accent: WEEKLY_QUIZ_ACCENT },
      { to: '/weekly-grid', title: 'Weekly grid', description: "Guess every answer that fits this week's theme before you run out of strikes.", accent: WEEKLY_QUIZ_ACCENT },
      { to: '/starting-xi', title: 'Starting XI', description: "Guess a full lineup, position by position, before the week's board runs out of lives.", accent: WEEKLY_QUIZ_ACCENT }
    ]
  },
  {
    title: 'Games',
    cards: [
      { to: '/grid-battle', title: 'Grid Battle', description: 'A pass-the-device multiplayer version of Weekly Grid - take turns, or lose a life trying.', accent: GAME_ACCENT },
      { to: '/starting-xi-battle', title: 'XI Battle', description: 'Same idea as Grid Battle, for a Starting XI board - take turns naming the lineup.', accent: GAME_ACCENT },
      { to: '/tension', title: 'Tension', description: 'A pass-the-device party quiz - push for a high-value guess, or play it safe.', accent: GAME_ACCENT },
      { to: '/501', title: '501', description: 'A darts-style countdown from 501 - 1v1, checkout between 0 and -10 to win.', accent: GAME_ACCENT },
      { to: '/imposter', title: 'Imposter', description: "One player doesn't get the answer - find out who by asking around the table.", accent: GAME_ACCENT },
      { to: '/bullseye', title: 'Bullseye', description: 'Everyone answers, lowest score is eliminated each round, until one player is left.', accent: GAME_ACCENT },
      // No standalone card for Penalty Shootout - it's reached from the XI
      // Battle card above instead, not as its own destination.
      { to: '/flashback', title: 'Flashback', description: "Guess the exact year from a clue - it gets easier each round, but the points don't wait.", accent: GAME_ACCENT }
    ]
  },
  {
    title: 'Manage your quizzes',
    cards: [
      { to: '/my-quizzes?tab=templates', title: 'Quiz templates', description: 'Pre-made quizzes published by an admin - download a PDF right away, or copy one to edit.', accent: QUIZ_MANAGEMENT_ACCENT },
      { to: '/my-quizzes', title: 'My quizzes', description: "Revisit, edit or re-download anything you've saved.", accent: QUIZ_MANAGEMENT_ACCENT },
      { to: '/suggest-question', title: 'Suggest a question', description: 'Add to the shared question bank - admin-reviewed, and usable in your own quizzes either way.', accent: QUIZ_MANAGEMENT_ACCENT }
    ]
  }
]

function toColumns(cards) {
  const columns = []
  for (let i = 0; i < cards.length; i += 2) {
    columns.push(cards.slice(i, i + 2))
  }
  return columns
}

const dashboardSections = computed(() =>
  dashboardSectionDefs.map(section => ({ title: section.title, columns: toColumns(section.cards) }))
)

// --- Daily Quiz nudge - once per day, per browser, for a real (non-guest,
// non-admin) user who hasn't finished today's quiz yet. Reuses the same
// /daily-quiz/active endpoint the Daily Quiz list page itself calls (it
// lazily creates today's set if it doesn't exist yet, and reports this
// user's own attempt status against it) rather than adding a new endpoint
// just for this check.
const dailyQuizNudge = ref(null)
const showDailyQuizNudge = ref(false)
const DAILY_QUIZ_NUDGE_KEY = 'daily_quiz_nudge_dismissed_date'

onMounted(async () => {
  if (auth.isAdmin.value || auth.isGuest.value) return
  try {
    const [active] = await api.getActiveDailyQuizzes()
    if (!active || (active.status !== 'NOT_STARTED' && active.status !== 'IN_PROGRESS')) return

    // Keyed on the quiz's own date (a server-assigned string), not the
    // browser's local date - sidesteps any client/server timezone mismatch
    // and doubles as "one nudge per quiz day" regardless of how that lines
    // up with the visitor's own clock.
    let dismissedDate = null
    try { dismissedDate = localStorage.getItem(DAILY_QUIZ_NUDGE_KEY) } catch (e) { /* private window etc. - just always show it */ }
    if (dismissedDate === active.quizDate) return

    dailyQuizNudge.value = active
    showDailyQuizNudge.value = true
  } catch (e) {
    // the nudge is a nice-to-have, not core dashboard functionality - fail quietly
  }
})

function dismissDailyQuizNudge() {
  showDailyQuizNudge.value = false
  try {
    if (dailyQuizNudge.value) localStorage.setItem(DAILY_QUIZ_NUDGE_KEY, dailyQuizNudge.value.quizDate)
  } catch (e) { /* ignore - worst case the nudge reappears next visit */ }
}
</script>
