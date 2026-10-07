<template>
  <div class="dq-page dq-page--wide">
    <div class="dq-topbar">
      <router-link to="/admin/daily-quiz-review" class="dq-back">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6" /></svg>
        Daily quiz review
      </router-link>
      <div v-if="attempt" class="dq-topbar-actions">
        <router-link :to="`/admin/daily-quiz-day/${attempt.setId}`" class="dq-chip-btn">This day's players</router-link>
        <button class="dq-chip-btn" @click="showQuestions = true">Questions</button>
        <button class="dq-chip-btn" @click="openScoreboard">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
          Leaderboard
        </button>
      </div>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <template v-else-if="attempt">
      <header class="dq-attempt-head">
        <span class="dq-avatar dq-avatar--lg">{{ initials(attempt.playerName) }}</span>
        <div class="dq-hero-text">
          <span class="dq-eyebrow">{{ formatLong(attempt.quizDate) }}</span>
          <h1 class="dq-title" style="margin:2px 0 0;">{{ attempt.playerName }}</h1>
        </div>
        <span v-if="attempt.status === 'GRADED'" class="dq-pill dq-pill--ok" style="font-size:1rem; padding:7px 16px;">{{ attempt.score }} / {{ attempt.maxScore }}</span>
        <span v-else class="dq-pill dq-pill--wait" style="font-size:0.9rem; padding:7px 16px;">{{ pendingCount }} to review</span>
      </header>

      <div class="dq-list" style="gap:14px;">
        <article v-for="a in attempt.answers" :key="a.answerId" class="dq-review" :class="{ 'is-wait': a.verdict === 'PENDING' }">
          <div class="dq-review-top">
            <span class="dq-q-num">{{ a.questionNumber }}</span>
            <div class="dq-review-q">{{ a.questionText }}</div>
            <span v-if="a.yearQuestion" class="dq-pill dq-pill--year">Year</span>
          </div>

          <img v-if="a.photoUrl" :src="a.photoUrl" alt="" class="dq-photo dq-photo--small" @error="e => e.target.style.display = 'none'" />

          <div class="dq-compare">
            <div class="dq-compare-cell dq-compare-cell--theirs">
              <span>Their answer</span>
              <b>{{ a.submittedAnswer || '(blank)' }}</b>
            </div>
            <div class="dq-compare-cell dq-compare-cell--answer">
              <span>Correct answer</span>
              <b>{{ a.correctAnswer }}</b>
            </div>
          </div>

          <div class="dq-review-foot">
            <!-- A decision is never final - a mis-click is one click to undo, and the score above updates. -->
            <div v-if="a.verdict === 'PENDING' || a.reviewable" class="dq-segment" role="group" aria-label="Decision">
              <button
                type="button"
                class="ok"
                :class="{ 'is-on': a.verdict === 'CORRECT' }"
                :disabled="busyId === a.answerId"
                @click="decide(a, true)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>
                Correct
              </button>
              <button
                type="button"
                class="bad"
                :class="{ 'is-on': a.verdict === 'INCORRECT' }"
                :disabled="busyId === a.answerId"
                @click="decide(a, false)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M6 6l12 12M18 6L6 18" /></svg>
                Incorrect
              </button>
            </div>
            <span v-else class="dq-row-meta" style="margin:0;">Graded automatically</span>

            <span class="spacer"></span>
            <span v-if="a.verdict === 'CORRECT'" class="dq-pill dq-pill--ok">{{ a.yearQuestion ? `+${a.points} ${a.points === 1 ? 'point' : 'points'}` : 'Correct' }}</span>
            <span v-else-if="a.verdict === 'INCORRECT'" class="dq-pill dq-pill--bad">{{ a.yearQuestion ? '0 points' : 'Incorrect' }}</span>
            <span v-else class="dq-pill dq-pill--wait">Needs a decision</span>
          </div>
        </article>
      </div>

      <div v-if="!pendingCount" class="dq-card dq-card--flat dq-empty" style="margin-top:18px; padding:22px;">
        All of {{ attempt.playerName }}'s answers have been decided - you can still change any of them above.
      </div>
    </template>

    <AdminDailyQuizQuestionsModal
      v-if="showQuestions && attempt"
      :set-id="attempt.setId"
      :title="`Questions - ${formatDate(attempt.quizDate)}`"
      @close="closeQuestions"
    />

    <DailyQuizScoreboardModal
      v-if="showScoreboard"
      :title="`Leaderboard - ${formatDate(attempt.quizDate)}`"
      :data="scoreboardData"
      :loading="scoreboardLoading"
      @close="showScoreboard = false"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import toast from '../services/toast'
import DailyQuizScoreboardModal from '../components/DailyQuizScoreboardModal.vue'
import AdminDailyQuizQuestionsModal from '../components/AdminDailyQuizQuestionsModal.vue'

const route = useRoute()
const attemptId = route.params.id

const attempt = ref(null)
const loading = ref(true)
const error = ref('')
const busyId = ref(null)
const showQuestions = ref(false)

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

// Clicking the side that's already selected is a no-op - nothing to save.
function decide(answer, correct) {
  if ((answer.verdict === 'CORRECT') === correct && answer.verdict !== 'PENDING') return
  resolve(answer, correct)
}

async function resolve(answer, correct) {
  busyId.value = answer.answerId
  error.value = ''
  try {
    await api.adminResolveDailyQuizAnswer(answer.answerId, correct)
    toast.show(correct ? 'Marked correct.' : 'Marked incorrect.')
    // Re-read from the server rather than patching the row locally: deciding or changing an answer
    // can grade the attempt or move its score, and the page shows that. Quietly - no loading
    // state, so the list doesn't blank out under the admin's cursor.
    attempt.value = await api.adminGetDailyQuizAttempt(attemptId)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not update that answer.'
  } finally {
    busyId.value = null
  }
}

// Questions may have been edited while the list was open - reload so the answers shown here
// (the correct answer next to each player's) reflect the fix.
async function closeQuestions() {
  showQuestions.value = false
  try {
    attempt.value = await api.adminGetDailyQuizAttempt(attemptId)
  } catch (e) {
    // keep showing what we had
  }
}

function initials(name) {
  return (name || '?').split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('')
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

function formatLong(iso) {
  return new Date(iso).toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' })
}

// --- Leaderboard for this attempt's day - same endpoint/shape as the player-facing one, shown in
// the shared modal without the "show my name" switch (that's a player preference, not an admin action).
const showScoreboard = ref(false)
const scoreboardData = ref(null)
const scoreboardLoading = ref(false)

async function openScoreboard() {
  showScoreboard.value = true
  scoreboardLoading.value = true
  try {
    scoreboardData.value = await api.getDailyQuizScoreboard(attempt.value.setId)
  } catch (e) {
    // a nice-to-have - fail quietly, the modal's empty state covers it
  } finally {
    scoreboardLoading.value = false
  }
}
</script>
