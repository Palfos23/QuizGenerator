<template>
  <div style="max-width:720px; margin:0 auto;">
    <div style="display:flex; gap:8px; margin-bottom:6px;">
      <router-link to="/daily-quiz" class="btn btn-secondary btn-sm">← All daily quizzes</router-link>
      <button v-if="state" class="btn btn-secondary btn-sm" @click="openScoreboard">Results</button>
    </div>
    <h1>Daily quiz</h1>
    <p class="page-subtitle" v-if="state">{{ formatDate(state.quizDate) }} - 15 questions, pub-quiz style.</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <LoadingState v-if="loading" full message="Loading this quiz…" />

    <template v-else-if="state">
      <template v-if="state.attemptStatus === 'IN_PROGRESS'">
        <form @submit.prevent="showSubmitConfirm = true">
          <div v-for="q in state.questions" :key="q.questionId" class="field">
            <label>{{ q.questionNumber }}. {{ q.text }}</label>
            <img v-if="q.photoUrl" :src="q.photoUrl" alt="" class="daily-quiz-photo" @error="e => e.target.style.display = 'none'" />
            <input type="text" v-model="answers[q.questionId]" autocomplete="off" />
          </div>
          <button type="submit" class="btn btn-primary" style="width:100%; margin-top:12px;" :disabled="submitting">
            {{ submitting ? 'Submitting…' : 'Submit answers' }}
          </button>
        </form>
      </template>

      <template v-else-if="state.result">
        <h2 v-if="state.result.score !== null" style="text-align:center;">You scored {{ state.result.score }} / {{ state.result.maxScore }}</h2>
        <div v-else class="empty-state friendly">
          Thanks! An admin still needs to check a few of your answers - come back later for your final score.
        </div>
        <div class="saved-quiz-list">
          <div v-for="a in state.result.answers" :key="a.questionNumber" class="saved-quiz-row" style="align-items:flex-start;">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ a.questionNumber }}. {{ a.questionText }}</div>
              <img v-if="a.photoUrl" :src="a.photoUrl" alt="" class="daily-quiz-photo small" @error="e => e.target.style.display = 'none'" />
              <div class="saved-quiz-meta">
                Your answer: {{ a.yourAnswer || '(blank)' }}
                <span v-if="a.verdict === 'INCORRECT'"> · Correct answer: {{ a.correctAnswer }}</span>
              </div>
            </div>
            <span
              v-if="a.verdict === 'PENDING'"
              class="tag"
              style="background:rgba(242,183,5,0.15); color:var(--gold); display:flex; align-items:center; gap:4px;"
            >⏳ Under review</span>
            <span
              v-else
              class="tag"
              :style="a.verdict === 'CORRECT' ? { background: 'rgba(61,220,151,0.15)', color: 'var(--teal)' } : { background: 'rgba(255,77,109,0.15)', color: 'var(--coral)' }"
            >{{ a.verdict === 'CORRECT' ? '✓' : '✕' }}</span>
          </div>
        </div>
      </template>
    </template>

    <!-- Submitting is final (the backend rejects a second submission), so this
         asks first - and calls out any blank boxes, which are auto-marked
         wrong rather than skipped. Not ConfirmModal: that one's confirm
         button is hard-wired to the destructive red style. -->
    <div v-if="showSubmitConfirm" class="modal-backdrop" @click.self="!submitting && (showSubmitConfirm = false)">
      <div class="modal" role="alertdialog" aria-modal="true" aria-label="Submit your answers?" style="max-width:400px;">
        <h2 style="margin-top:0;">Submit your answers?</h2>
        <p class="page-subtitle" style="margin-bottom:8px;">You can't change them once they're submitted.</p>
        <p v-if="blankCount" style="color:var(--coral); font-size:0.9rem; margin:0 0 8px;">
          {{ blankCount }} question{{ blankCount === 1 ? ' is' : 's are' }} still blank - blank answers count as wrong.
        </p>
        <div style="display:flex; gap:10px; justify-content:flex-end; margin-top:20px;">
          <button class="btn btn-secondary" :disabled="submitting" @click="showSubmitConfirm = false">
            {{ blankCount ? 'Keep answering' : 'Go back' }}
          </button>
          <button class="btn btn-primary" :disabled="submitting" @click="submit">
            {{ submitting ? 'Submitting…' : 'Submit' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showScoreboard" class="modal-backdrop" @click.self="showScoreboard = false">
      <div class="modal">
        <h2 style="margin-top:0;">Scoreboard</h2>

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
            <tr v-for="(s, i) in topFive" :key="s.userName + i" :class="{ 'you-row': s.isYou }">
              <td>{{ i + 1 }}</td>
              <td>{{ firstName(s.userName) }}</td>
              <td style="text-align:right;">{{ s.score }} / {{ s.maxScore }}</td>
            </tr>
            <tr v-if="yourRank && yourRank.rank > 5">
              <td colspan="3" style="text-align:center; color:var(--text-dim); padding:4px 0;">···</td>
            </tr>
            <tr v-if="yourRank && yourRank.rank > 5" class="you-row">
              <td>{{ yourRank.rank }}</td>
              <td>{{ firstName(yourRank.entry.userName) }}</td>
              <td style="text-align:right;">{{ yourRank.entry.score }} / {{ yourRank.entry.maxScore }}</td>
            </tr>
          </tbody>
        </table>

        <label
          v-if="scoreboardData && scoreboardData.yourLeaderboardPreference !== null"
          style="display:flex; align-items:center; gap:8px; margin-top:16px; text-transform:none; font-weight:400; color:var(--text-dim); font-size:0.9rem; cursor:pointer;"
        >
          <input type="checkbox" v-model="leaderboardOptIn" @change="updateLeaderboardPreference" style="width:auto;" />
          Show my name on this leaderboard
        </label>

        <button class="btn btn-secondary" style="margin-top:16px; width:100%;" @click="showScoreboard = false">Close</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import toast from '../services/toast'
import LoadingState from '../components/LoadingState.vue'
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

const blankCount = computed(() =>
  (state.value?.questions || []).filter(q => !(answers[q.questionId] || '').trim()).length
)

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
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit your answers.'
  } finally {
    submitting.value = false
    showSubmitConfirm.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString(undefined, { day: 'numeric', month: 'long', year: 'numeric' })
}

function firstName(name) {
  return (name || '').split(' ')[0]
}

// --- Scoreboard modal - same pattern as WeeklyGridPlayView ---
const showScoreboard = ref(false)
const scoreboardData = ref(null)
const scoreboardLoading = ref(false)
const leaderboardOptIn = ref(true)

const scoreboardEntries = computed(() => scoreboardData.value?.entries || [])
const topFive = computed(() => scoreboardEntries.value.slice(0, 5))
const yourRank = computed(() => {
  const idx = scoreboardEntries.value.findIndex(s => s.isYou)
  if (idx === -1) return null
  return { rank: idx + 1, entry: scoreboardEntries.value[idx] }
})

async function openScoreboard() {
  showScoreboard.value = true
  scoreboardLoading.value = true
  try {
    scoreboardData.value = await api.getDailyQuizScoreboard(quizId)
    leaderboardOptIn.value = scoreboardData.value.yourLeaderboardPreference ?? true
  } catch (e) {
    // scoreboard is a nice-to-have - fail quietly, empty state already covers it
  } finally {
    scoreboardLoading.value = false
  }
}

async function updateLeaderboardPreference() {
  try {
    await api.setDailyQuizLeaderboardPreference(quizId, leaderboardOptIn.value)
    scoreboardData.value = await api.getDailyQuizScoreboard(quizId)
  } catch (e) {
    toast.show('Could not update your leaderboard preference.')
    leaderboardOptIn.value = !leaderboardOptIn.value
  }
}
</script>

<style scoped>
/* A question's picture (the daily Logo question) - sits between the question text
   and the answer box, capped so a large source image can't push the form off screen. */
.daily-quiz-photo {
  display: block;
  max-width: 100%;
  max-height: 260px;
  margin: 8px 0 12px;
  border-radius: var(--radius-sm);
  background: #fff;
  object-fit: contain;
}
.daily-quiz-photo.small {
  max-height: 120px;
  margin: 6px 0 8px;
}
</style>
