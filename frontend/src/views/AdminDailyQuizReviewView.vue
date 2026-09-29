<template>
  <div>
    <h1>Daily quiz - review answers</h1>
    <p class="page-subtitle">Players whose answers weren't an exact match to the stored answer - click one to review just their answers.</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="!pendingAttempts.length" class="empty-state friendly">Nothing waiting for review.</div>

    <div v-else class="saved-quiz-list">
      <div v-for="a in pendingAttempts" :key="a.attemptId" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ a.playerName }}</div>
          <div class="saved-quiz-meta">{{ formatDate(a.quizDate) }} · {{ a.pendingCount }} answer{{ a.pendingCount === 1 ? '' : 's' }} to review</div>
        </div>
        <router-link :to="`/admin/daily-quiz-review/${a.attemptId}`" class="btn btn-primary btn-sm">Review</router-link>
      </div>
    </div>

    <h2 style="margin-top:40px;">Scoreboards</h2>
    <p class="page-subtitle">Pick a day to see how everyone scored - not tied to whether anything's still waiting for review above.</p>

    <div v-if="quizzesLoading" style="color:var(--text-dim);">Loading…</div>
    <div v-else-if="!allQuizzes.length" class="empty-state friendly">No daily quizzes yet.</div>
    <div v-else class="saved-quiz-list">
      <div v-for="q in allQuizzes" :key="q.id" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ formatDate(q.quizDate) }}</div>
        </div>
        <button class="btn btn-secondary btn-sm" @click="openScoreboard(q)">Scoreboard</button>
      </div>
    </div>

    <div v-if="showScoreboard" class="modal-backdrop" @click.self="showScoreboard = false">
      <div class="modal">
        <h2 style="margin-top:0;">Scoreboard - {{ formatDate(scoreboardQuizDate) }}</h2>

        <div v-if="scoreboardData && scoreboardEntries.length" class="stats-panel" style="text-align:center;">
          <div style="color:var(--text-dim); font-size:0.78rem; text-transform:uppercase; letter-spacing:0.5px;">Average score</div>
          <div style="font-size:1.5rem; font-weight:700; margin-top:2px;">{{ scoreboardData.averageScore.toFixed(1) }} / {{ scoreboardData.maxScore }}</div>
        </div>

        <div v-if="scoreboardLoading" style="color:var(--text-dim); font-size:0.9rem;">Loading…</div>
        <div v-else-if="!scoreboardEntries.length" style="color:var(--text-dim); font-size:0.9rem;">
          Nobody's been fully graded yet.
        </div>
        <table v-else class="table scoreboard-table">
          <thead>
            <tr><th style="width:14%;">#</th><th style="width:56%;">Player</th><th style="width:30%; text-align:right;">Score</th></tr>
          </thead>
          <tbody>
            <tr v-for="(s, i) in scoreboardEntries" :key="s.userName + i">
              <td>{{ i + 1 }}</td>
              <td>{{ s.userName }}</td>
              <td style="text-align:right;">{{ s.score }} / {{ s.maxScore }}</td>
            </tr>
          </tbody>
        </table>

        <button class="btn btn-secondary" style="margin-top:16px; width:100%;" @click="showScoreboard = false">Close</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'

const pendingAttempts = ref([])
const loading = ref(true)
const error = ref('')

onMounted(load)
onMounted(loadQuizzes)

async function load() {
  loading.value = true
  error.value = ''
  try {
    pendingAttempts.value = await api.adminListDailyQuizPendingAttempts()
  } catch (e) {
    error.value = 'Could not load pending answers.'
  } finally {
    loading.value = false
  }
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

// --- Scoreboards - every day (active + archive), independent of the pending-
// review queue above, so an admin can check scores even when nothing needs
// correcting. Reuses the same active/archive endpoints the player-facing
// DailyQuizListView calls - an admin account hits them the same way any
// other authenticated user does (see SecurityConfig's /api/daily-quiz/**
// matcher), the per-user status/score fields in the response just aren't
// used here.
const allQuizzes = ref([])
const quizzesLoading = ref(true)

async function loadQuizzes() {
  quizzesLoading.value = true
  try {
    const [active, archive] = await Promise.all([api.getActiveDailyQuizzes(), api.getArchiveDailyQuizzes()])
    allQuizzes.value = [...active, ...archive]
  } catch (e) {
    // scoreboard picker is a nice-to-have alongside the review queue above -
    // fail quietly, the empty state already covers it
  } finally {
    quizzesLoading.value = false
  }
}

const showScoreboard = ref(false)
const scoreboardData = ref(null)
const scoreboardLoading = ref(false)
const scoreboardQuizDate = ref(null)

const scoreboardEntries = computed(() => scoreboardData.value?.entries || [])

async function openScoreboard(quiz) {
  scoreboardQuizDate.value = quiz.quizDate
  showScoreboard.value = true
  scoreboardLoading.value = true
  try {
    scoreboardData.value = await api.getDailyQuizScoreboard(quiz.id)
  } catch (e) {
    // same fail-quietly as above - the empty state already covers it
  } finally {
    scoreboardLoading.value = false
  }
}
</script>
