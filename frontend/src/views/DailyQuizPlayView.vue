<template>
  <div class="dq-page">
    <div class="dq-topbar">
      <router-link to="/daily-quiz" class="dq-back">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6" /></svg>
        All daily quizzes
      </router-link>
      <div v-if="state" class="dq-topbar-actions">
        <button class="dq-chip-btn" @click="openScoreboard">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
          Leaderboard
        </button>
      </div>
    </div>

    <header v-if="state" class="dq-hero">
      <span class="dq-eyebrow">Daily quiz</span>
      <h1 class="dq-title">{{ formatDate(state.quizDate) }}</h1>
      <p class="dq-sub">{{ subtitle }}</p>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>
    <LoadingState v-if="loading" full message="Loading this quiz…" />

    <template v-else-if="state">
      <!-- ---------- Answering ---------- -->
      <form v-if="state.attemptStatus === 'IN_PROGRESS'" @submit.prevent="showSubmitConfirm = true">
        <div class="dq-progress">
          <div class="dq-progress-label">
            <span><strong>{{ answeredCount }}</strong> of {{ state.questions.length }} answered</span>
            <span v-if="answeredCount === state.questions.length">All done - ready to submit</span>
          </div>
          <div class="dq-progress-track"><div class="dq-progress-fill" :style="{ width: progressPct + '%' }"></div></div>
        </div>

        <div
          v-for="q in state.questions"
          :key="q.questionId"
          class="dq-q"
          :class="{ 'is-year': q.yearQuestion, 'is-answered': isFilled(q) }"
        >
          <div class="dq-q-head">
            <span class="dq-q-num">{{ q.questionNumber }}</span>
            <div class="dq-q-body">
              <div v-if="q.yearQuestion || q.photoUrl" class="dq-q-pills">
                <span v-if="q.yearQuestion" class="dq-pill dq-pill--year">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M3 10h18M8 3v4M16 3v4" /></svg>
                  Year question
                </span>
                <span v-if="q.photoUrl" class="dq-pill dq-pill--gold">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="16" rx="3" /><circle cx="9" cy="10" r="1.6" /><path d="M21 16l-5-5-8 8" /></svg>
                  Picture question
                </span>
              </div>
              <label class="dq-q-text" :for="`dq-a-${q.questionId}`">{{ q.text }}</label>
              <p v-if="q.yearQuestion" class="dq-q-hint">Guess the year - spot on is 2 points, one year off is 1 point.</p>
            </div>
          </div>

          <img v-if="q.photoUrl" :src="q.photoUrl" alt="" class="dq-photo" @error="e => e.target.style.display = 'none'" />

          <input
            :id="`dq-a-${q.questionId}`"
            class="dq-input"
            type="text"
            v-model="answers[q.questionId]"
            autocomplete="off"
            :inputmode="q.yearQuestion ? 'numeric' : undefined"
            :placeholder="q.yearQuestion ? 'Year, e.g. 1994' : 'Your answer'"
          />
        </div>

        <div class="dq-submit">
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            {{ submitting ? 'Submitting…' : 'Submit answers' }}
          </button>
          <p class="dq-submit-note">You can't change your answers once they're submitted.</p>
        </div>
      </form>

      <!-- ---------- Results ---------- -->
      <template v-else-if="state.result">
        <div v-if="state.result.score !== null" class="dq-card dq-score-hero">
          <div class="dq-ring" :aria-label="`${state.result.score} out of ${state.result.maxScore} points`">
            <svg viewBox="0 0 120 120">
              <circle class="dq-ring-track" cx="60" cy="60" r="52" />
              <circle
                class="dq-ring-fill"
                cx="60" cy="60" r="52"
                :stroke="scorePct >= 0.5 ? 'var(--dq-ok)' : 'var(--dq-wait)'"
                :stroke-dasharray="RING_LENGTH"
                :stroke-dashoffset="RING_LENGTH * (1 - scorePct)"
              />
            </svg>
            <div class="dq-ring-label">
              <strong>{{ state.result.score }}</strong>
              <span>of {{ state.result.maxScore }}</span>
            </div>
          </div>
          <div class="dq-score-copy">
            <h2>{{ scoreHeadline }}</h2>
            <p>You got {{ correctCount }} of {{ state.result.answers.length }} questions right. Check how you stack up on the leaderboard.</p>
          </div>
        </div>

        <div v-else class="dq-card dq-score-hero dq-score-hero--wait">
          <div class="dq-wait-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg>
          </div>
          <div class="dq-score-copy">
            <h2>Your answers are in</h2>
            <p>An admin is checking {{ pendingCount === 1 ? 'one answer' : `${pendingCount} answers` }} - come back later for your final score.</p>
          </div>
        </div>

        <div class="dq-result-list">
          <article
            v-for="a in state.result.answers"
            :key="a.questionNumber"
            class="dq-answer"
            :class="verdictClass(a)"
          >
            <span class="dq-q-num">{{ a.questionNumber }}</span>
            <div class="dq-answer-main">
              <div class="dq-answer-q">{{ a.questionText }}</div>
              <img v-if="a.photoUrl" :src="a.photoUrl" alt="" class="dq-photo dq-photo--small" @error="e => e.target.style.display = 'none'" />
              <div class="dq-answer-line">
                <span class="label">Your answer</span>
                <b>{{ a.yourAnswer || '(blank)' }}</b>
              </div>
              <div v-if="showCorrect(a)" class="dq-answer-line">
                <span class="label">Correct</span>
                <b>{{ a.correctAnswer }}</b>
              </div>
            </div>
            <div class="dq-answer-end">
              <span v-if="a.verdict === 'PENDING'" class="dq-pill dq-pill--wait">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg>
                Under review
              </span>
              <span v-else-if="a.yearQuestion" class="dq-pill" :class="a.pointsAwarded > 0 ? 'dq-pill--ok' : 'dq-pill--bad'">
                {{ a.pointsAwarded }} {{ a.pointsAwarded === 1 ? 'point' : 'points' }}
              </span>
              <span v-else class="dq-pill" :class="a.verdict === 'CORRECT' ? 'dq-pill--ok' : 'dq-pill--bad'">
                {{ a.verdict === 'CORRECT' ? 'Correct' : 'Wrong' }}
              </span>
              <span v-if="a.yearQuestion" class="dq-pill dq-pill--year">Year</span>
            </div>
          </article>
        </div>
      </template>
    </template>

    <!-- Submitting is final (the backend rejects a second submission), so ask first - and call out any
         blank boxes, which are marked wrong rather than skipped. -->
    <div v-if="showSubmitConfirm" class="modal-backdrop" @click.self="!submitting && (showSubmitConfirm = false)">
      <div class="modal dq-modal" role="alertdialog" aria-modal="true" aria-label="Submit your answers?">
        <div class="dq-modal-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9" /><path d="M8 12.5l2.7 2.7L16 9.5" /></svg>
        </div>
        <h2 class="dq-modal-title">Submit your answers?</h2>
        <p class="dq-modal-sub">
          You've answered {{ answeredCount }} of {{ state?.questions?.length }}. You can't change them once they're submitted.
        </p>
        <div v-if="blankCount" class="dq-note">
          {{ blankCount }} question{{ blankCount === 1 ? ' is' : 's are' }} still blank - blank answers count as wrong.
        </div>
        <div class="dq-modal-actions">
          <button class="btn btn-secondary" :disabled="submitting" @click="showSubmitConfirm = false">
            {{ blankCount ? 'Keep answering' : 'Go back' }}
          </button>
          <button class="btn btn-primary" :disabled="submitting" @click="submit">
            {{ submitting ? 'Submitting…' : 'Submit' }}
          </button>
        </div>
      </div>
    </div>

    <DailyQuizScoreboardModal
      v-if="showScoreboard"
      title="Leaderboard"
      compact
      :data="scoreboardData"
      :loading="scoreboardLoading"
      :preference="preferenceShown"
      @update:preference="updateLeaderboardPreference"
      @close="showScoreboard = false"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import toast from '../services/toast'
import LoadingState from '../components/LoadingState.vue'
import DailyQuizScoreboardModal from '../components/DailyQuizScoreboardModal.vue'
import { useEscapeKey } from '../composables/useEscapeKey'

const route = useRoute()
const quizId = route.params.id

const state = ref(null)
const loading = ref(true)
const error = ref('')
const answers = reactive({})
const submitting = ref(false)
const showSubmitConfirm = ref(false)

useEscapeKey(() => { if (showSubmitConfirm.value && !submitting.value) showSubmitConfirm.value = false })

// ---- answering ----

function isFilled(q) {
  return !!(answers[q.questionId] || '').trim()
}
const answeredCount = computed(() => (state.value?.questions || []).filter(isFilled).length)
const blankCount = computed(() => (state.value?.questions?.length || 0) - answeredCount.value)
const progressPct = computed(() => {
  const total = state.value?.questions?.length || 0
  return total ? (answeredCount.value / total) * 100 : 0
})

const subtitle = computed(() => {
  if (!state.value) return ''
  if (state.value.attemptStatus === 'IN_PROGRESS') {
    return `${state.value.questions.length} questions, pub-quiz style - type your answers, no multiple choice.`
  }
  return state.value.result?.score == null ? 'Submitted - waiting for an admin to finish checking.' : 'Here\'s how you did.'
})

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    state.value = await api.getDailyQuizPlayState(quizId)
  } catch (e) {
    error.value = 'Could not load this quiz.'
  } finally {
    loading.value = false
  }
}

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    const payload = state.value.questions.map(q => ({ questionId: q.questionId, answerText: answers[q.questionId] || '' }))
    state.value = await api.submitDailyQuizAnswers(quizId, payload)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit your answers.'
  } finally {
    submitting.value = false
    showSubmitConfirm.value = false
  }
}

// ---- results ----

const RING_LENGTH = 2 * Math.PI * 52

const scorePct = computed(() => {
  const r = state.value?.result
  return r && r.maxScore ? Math.max(0, Math.min(1, r.score / r.maxScore)) : 0
})
const scoreHeadline = computed(() => {
  const pct = scorePct.value
  if (pct >= 0.9) return 'Brilliant!'
  if (pct >= 0.7) return 'Great round!'
  if (pct >= 0.4) return 'Nice work'
  return 'Good effort - see you tomorrow'
})
const correctCount = computed(() => (state.value?.result?.answers || []).filter(a => a.verdict === 'CORRECT').length)
const pendingCount = computed(() => (state.value?.result?.answers || []).filter(a => a.verdict === 'PENDING').length)

function verdictClass(a) {
  if (a.verdict === 'PENDING') return 'dq-answer--wait'
  return a.verdict === 'CORRECT' ? 'dq-answer--ok' : 'dq-answer--bad'
}

// The right answer is shown whenever the player didn't already have it exactly: a wrong answer, or a
// Year guess that was one year off.
function showCorrect(a) {
  return a.verdict === 'INCORRECT' || (a.yearQuestion && a.pointsAwarded === 1)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' })
}

// ---- leaderboard ----

const showScoreboard = ref(false)
const scoreboardData = ref(null)
const scoreboardLoading = ref(false)
const leaderboardOptIn = ref(true)

// null hides the switch: only a player with a graded result of their own has a choice to make.
const preferenceShown = computed(() =>
  scoreboardData.value && scoreboardData.value.yourLeaderboardPreference !== null ? leaderboardOptIn.value : null
)

async function openScoreboard() {
  showScoreboard.value = true
  scoreboardLoading.value = true
  try {
    scoreboardData.value = await api.getDailyQuizScoreboard(quizId)
    leaderboardOptIn.value = scoreboardData.value.yourLeaderboardPreference ?? true
  } catch (e) {
    // the leaderboard is a nice-to-have - fail quietly, the empty state already covers it
  } finally {
    scoreboardLoading.value = false
  }
}

async function updateLeaderboardPreference(include) {
  const previous = leaderboardOptIn.value
  leaderboardOptIn.value = include
  try {
    await api.setDailyQuizLeaderboardPreference(quizId, include)
    scoreboardData.value = await api.getDailyQuizScoreboard(quizId)
  } catch (e) {
    toast.show('Could not update your leaderboard preference.')
    leaderboardOptIn.value = previous
  }
}
</script>
