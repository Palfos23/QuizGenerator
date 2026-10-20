<template>
  <div class="dq-page dq-page--wide">
    <div class="dq-topbar">
      <router-link to="/admin/daily-quiz-review" class="dq-back">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6" /></svg>
        {{ $t('Gjennomgang av dagens quiz') }}
      </router-link>
      <div v-if="attempt" class="dq-topbar-actions">
        <router-link :to="`/admin/daily-quiz-day/${attempt.setId}`" class="dq-chip-btn">{{ $t('Spillere denne dagen') }}</router-link>
        <button class="dq-chip-btn" @click="showQuestions = true">{{ $t('Spørsmål§AdminDailyQuizAttemptViewT') }}</button>
        <button class="dq-chip-btn" @click="openScoreboard">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
          {{ $t('Toppliste') }}
        </button>
      </div>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

    <template v-else-if="attempt">
      <header class="dq-attempt-head">
        <span class="dq-avatar dq-avatar--lg">{{ initials(attempt.playerName) }}</span>
        <div class="dq-hero-text">
          <span class="dq-eyebrow">{{ formatLong(attempt.quizDate) }}</span>
          <h1 class="dq-title" style="margin:2px 0 0;">{{ attempt.playerName }}</h1>
        </div>
        <span v-if="attempt.status === 'GRADED'" class="dq-pill dq-pill--ok" style="font-size:1rem; padding:7px 16px;">{{ attempt.score }} / {{ attempt.maxScore }}</span>
        <span v-else class="dq-pill dq-pill--wait" style="font-size:0.9rem; padding:7px 16px;">{{ pendingCount }} {{ $t('til gjennomgang') }}</span>
      </header>

      <div class="dq-list" style="gap:14px;">
        <article v-for="a in attempt.answers" :key="a.answerId" class="dq-review" :class="{ 'is-wait': a.verdict === 'PENDING' }">
          <div class="dq-review-top">
            <span class="dq-q-num">{{ a.questionNumber }}</span>
            <div class="dq-review-q">{{ a.questionText }}</div>
            <span v-if="a.yearQuestion" class="dq-pill dq-pill--year">{{ $t('Årstall') }}</span>
          </div>

          <img v-if="a.photoUrl" :src="a.photoUrl" alt="" class="dq-photo dq-photo--small" @error="e => e.target.style.display = 'none'" />
          <DailyQuizEasyMeter :percent="a.percentCorrect" :players="a.playersCounted" />

          <div class="dq-compare">
            <div class="dq-compare-cell dq-compare-cell--theirs">
              <span>{{ $t('Svaret deres') }}</span>
              <b>{{ a.submittedAnswer || $t('(tomt)') }}</b>
            </div>
            <div class="dq-compare-cell dq-compare-cell--answer">
              <span>{{ $t('Riktig svar§AdminDailyQuizAttemptViewT') }}</span>
              <b>{{ a.correctAnswer }}</b>
            </div>
          </div>

          <div class="dq-review-foot">
            <!-- A decision is never final - a mis-click is one click to undo, and the score above updates. -->
            <div v-if="a.verdict === 'PENDING' || a.reviewable" class="dq-segment" role="group" :aria-label="$t('Avgjørelse')">
              <button
                type="button"
                class="ok"
                :class="{ 'is-on': a.verdict === 'CORRECT' }"
                :disabled="busyId === a.answerId"
                @click="decide(a, true)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>
                {{ $t('Riktig§AdminDailyQuizAttemptViewT') }}
              </button>
              <button
                type="button"
                class="bad"
                :class="{ 'is-on': a.verdict === 'INCORRECT' }"
                :disabled="busyId === a.answerId"
                @click="decide(a, false)"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M6 6l12 12M18 6L6 18" /></svg>
                {{ $t('Feil§AdminDailyQuizAttemptViewT') }}
              </button>
            </div>
            <span v-else class="dq-row-meta" style="margin:0;">{{ $t('Rettet automatisk') }}</span>

            <span class="spacer"></span>
            <span v-if="a.verdict === 'CORRECT'" class="dq-pill dq-pill--ok">{{ a.yearQuestion ? $t('+{0} poeng¦+{0} poeng', [a.points]) : $t('Riktig§AdminDailyQuizAttemptViewM') }}</span>
            <span v-else-if="a.verdict === 'INCORRECT'" class="dq-pill dq-pill--bad">{{ a.yearQuestion ? $t('0 poeng') : $t('Feil§AdminDailyQuizAttemptViewM') }}</span>
            <span v-else class="dq-pill dq-pill--wait">{{ $t('Trenger en avgjørelse') }}</span>
          </div>
        </article>
      </div>

      <div v-if="!pendingCount" class="dq-card dq-card--flat dq-empty" style="margin-top:18px; padding:22px;">
        {{ $t('Alle svarene til') }} {{ attempt.playerName }}{{ $t('er avgjort - du kan fortsatt endre hvilke som helst av dem over.') }}
      </div>
    </template>

    <AdminDailyQuizQuestionsModal
      v-if="showQuestions && attempt"
      :set-id="attempt.setId"
      :title="$t('Spørsmål - {0}', [formatDate(attempt.quizDate)])"
      @close="closeQuestions"
    />

    <DailyQuizScoreboardModal
      v-if="showScoreboard"
      :title="$t('Toppliste - {0}', [formatDate(attempt.quizDate)])"
      :data="scoreboardData"
      :loading="scoreboardLoading"
      @close="showScoreboard = false"
    />
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import toast from '../services/toast'
import DailyQuizScoreboardModal from '../components/DailyQuizScoreboardModal.vue'
import AdminDailyQuizQuestionsModal from '../components/AdminDailyQuizQuestionsModal.vue'
import DailyQuizEasyMeter from '../components/DailyQuizEasyMeter.vue'

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
    error.value = _t('Kunne ikke laste svarene til denne spilleren.')
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
    toast.show(correct ? _t('Markert som riktig.') : _t('Markert som feil.'))
    // Re-read from the server rather than patching the row locally: deciding or changing an answer
    // can grade the attempt or move its score, and the page shows that. Quietly - no loading
    // state, so the list doesn't blank out under the admin's cursor.
    attempt.value = await api.adminGetDailyQuizAttempt(attemptId)
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke oppdatere svaret.')
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
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { year: 'numeric', month: 'short', day: 'numeric' })
}

function formatLong(iso) {
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { weekday: 'long', day: 'numeric', month: 'long' })
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
