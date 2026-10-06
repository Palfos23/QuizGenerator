<template>
  <div>
    <div style="display:flex; gap:8px; margin-bottom:6px;">
      <router-link to="/admin/daily-quiz-review" class="btn btn-secondary btn-sm">← All players</router-link>
      <button v-if="attempt" class="btn btn-secondary btn-sm" @click="openScoreboard">Scoreboard</button>
    </div>

    <div v-if="error" class="banner error" style="margin-top:16px;">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim); margin-top:16px;">Loading…</div>

    <template v-else-if="attempt">
      <h1 style="margin-top:16px;">{{ attempt.playerName }}</h1>
      <p class="page-subtitle">{{ formatDate(attempt.quizDate) }}</p>

      <div class="saved-quiz-list">
        <div v-for="a in attempt.answers" :key="a.answerId" class="saved-quiz-row" style="align-items:flex-start;">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ a.questionNumber }}. {{ a.questionText }}</div>
            <img v-if="a.photoUrl" :src="a.photoUrl" alt="" class="daily-quiz-photo" @error="e => e.target.style.display = 'none'" />
            <div class="saved-quiz-meta">Correct answer: <strong>{{ a.correctAnswer }}</strong></div>
            <div style="color:var(--text-dim); font-size:0.85rem; margin-top:6px;">
              Answered: <strong>{{ a.submittedAnswer || '(blank)' }}</strong>
            </div>
          </div>
          <div v-if="a.verdict === 'PENDING'" style="display:flex; gap:8px;">
            <button class="btn btn-primary btn-sm" :disabled="busyId === a.answerId" @click="resolve(a, true)">Mark correct</button>
            <button class="btn btn-danger btn-sm" :disabled="busyId === a.answerId" @click="resolve(a, false)">Mark incorrect</button>
          </div>
          <span
            v-else
            class="tag"
            :style="a.verdict === 'CORRECT' ? { background: 'rgba(61,220,151,0.15)', color: 'var(--teal)' } : { background: 'rgba(255,77,109,0.15)', color: 'var(--coral)' }"
          >{{ a.yearQuestion ? (a.verdict === 'CORRECT' ? `✓ +${a.points}` : '✕ 0') : (a.verdict === 'CORRECT' ? '✓' : '✕') }}</span>
        </div>
      </div>

      <div v-if="!pendingCount" class="empty-state friendly" style="margin-top:20px;">
        All of {{ attempt.playerName }}'s answers have been resolved.
      </div>
    </template>

    <div v-if="showScoreboard" class="modal-backdrop" @click.self="showScoreboard = false">
      <div class="modal">
        <h2 style="margin-top:0;">Scoreboard - {{ formatDate(attempt.quizDate) }}</h2>

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
import { useRoute } from 'vue-router'
import api from '../services/api'
import toast from '../services/toast'

const route = useRoute()
const attemptId = route.params.id

const attempt = ref(null)
const loading = ref(true)
const error = ref('')
const busyId = ref(null)

const pendingCount = computed(() => attempt.value?.answers.filter(a => a.verdict === 'PENDING').length || 0)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    attempt.value = await api.adminGetDailyQuizAttempt(attemptId)
  } catch (e) {
    error.value = 'Could not load this player\'s answers.'
  } finally {
    loading.value = false
  }
}

async function resolve(answer, correct) {
  busyId.value = answer.answerId
  error.value = ''
  try {
    await api.adminResolveDailyQuizAnswer(answer.answerId, correct)
    answer.verdict = correct ? 'CORRECT' : 'INCORRECT'
    toast.show(correct ? 'Marked correct.' : 'Marked incorrect.')
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not resolve that answer.'
  } finally {
    busyId.value = null
  }
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

// --- Scoreboard modal for this attempt's day - same endpoint/shape as the
// player-facing one in DailyQuizPlayView, minus the leaderboard opt-in
// checkbox (that's a player preference, not an admin action).
const showScoreboard = ref(false)
const scoreboardData = ref(null)
const scoreboardLoading = ref(false)

const scoreboardEntries = computed(() => scoreboardData.value?.entries || [])

async function openScoreboard() {
  showScoreboard.value = true
  scoreboardLoading.value = true
  try {
    scoreboardData.value = await api.getDailyQuizScoreboard(attempt.value.setId)
  } catch (e) {
    // scoreboard is a nice-to-have - fail quietly, empty state already covers it
  } finally {
    scoreboardLoading.value = false
  }
}
</script>

<style scoped>
/* The question's picture (e.g. a logo) - the admin needs to see it to judge a
   non-exact answer. */
.daily-quiz-photo {
  display: block;
  max-width: 100%;
  max-height: 140px;
  margin: 6px 0 8px;
  border-radius: var(--radius-sm);
  background: #fff;
  object-fit: contain;
}
</style>
