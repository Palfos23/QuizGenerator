<template>
  <div class="dq-page">
    <div class="dq-topbar">
      <router-link to="/daily-quiz" class="dq-back">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6" /></svg>
        {{ $t('Alle daglige quizer') }}
      </router-link>
      <div v-if="state" class="dq-topbar-actions">
        <button class="dq-chip-btn" @click="openScoreboard">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
          {{ $t('Toppliste') }}
        </button>
      </div>
    </div>

    <header v-if="state" class="dq-hero">
      <span class="dq-eyebrow">{{ $t('Dagens quiz§DailyQuizPlayViewT') }}</span>
      <h1 class="dq-title">{{ formatDate(state.quizDate) }}</h1>
      <p class="dq-sub">{{ subtitle }}</p>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>
    <LoadingState v-if="loading" full :message="$t('Laster denne quizen…')" />

    <template v-else-if="state">
      <!-- ---------- Answering ---------- -->
      <form v-if="state.attemptStatus === 'IN_PROGRESS'" @submit.prevent="showSubmitConfirm = true">
        <div class="dq-progress">
          <div class="dq-progress-label">
            <span><strong>{{ answeredCount }}</strong> {{ $t('av') }} {{ state.questions.length }} {{ $t('besvart') }}</span>
            <span v-if="answeredCount === state.questions.length">{{ $t('Alt ferdig - klar til å sendes inn') }}</span>
            <button type="button" class="dq-save-chip" :class="{ 'is-saved': !dirty && savedAt }" :disabled="saving || submitting || !dirty" @click="saveDraft">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5" v-if="!dirty && savedAt" /><path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2zM17 21v-8H7v8M7 3v5h8" v-else /></svg>
              {{ saving ? $t('Lagrer…') : (!dirty && savedAt ? $t('Lagret') : $t('Lagre')) }}
            </button>
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
                  {{ $t('Årstallsspørsmål') }}
                </span>
                <span v-if="q.photoUrl" class="dq-pill dq-pill--gold">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="16" rx="3" /><circle cx="9" cy="10" r="1.6" /><path d="M21 16l-5-5-8 8" /></svg>
                  {{ $t('Bildespørsmål') }}
                </span>
              </div>
              <label class="dq-q-text" :for="`dq-a-${q.questionId}`">{{ q.text }}</label>
              <p v-if="q.yearQuestion" class="dq-q-hint">{{ $t('Gjett året - blink er 2 poeng, ett år unna er 1 poeng.') }}</p>
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
            :placeholder="q.yearQuestion ? $t('År, f.eks. 1994') : $t('Ditt svar')"
          />
        </div>

        <div class="dq-submit">
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            {{ submitting ? $t('Sender inn…') : $t('Send inn svar') }}
          </button>
          <button type="button" class="btn btn-secondary dq-save-btn" :disabled="saving || submitting || !dirty" @click="saveDraft">
            {{ saving ? $t('Lagrer…') : (!dirty && savedAt ? $t('Fremgang lagret') : $t('Lagre til senere')) }}
          </button>
          <p class="dq-submit-note">
            {{ $t('Ikke klar til å levere? Lagre svarene dine og fortsett senere - på hvilken som helst enhet.') }}<br />
            {{ $t('Du kan ikke endre svarene dine etter at de er sendt inn.') }}
          </p>
        </div>
      </form>

      <!-- ---------- Results ---------- -->
      <template v-else-if="state.result">
        <div v-if="state.result.score !== null" class="dq-card dq-score-hero">
          <div class="dq-ring" :aria-label="$t('{0} av {1} poeng', [state.result.score, state.result.maxScore])">
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
              <span>{{ $t('av') }} {{ state.result.maxScore }}</span>
            </div>
          </div>
          <div class="dq-score-copy">
            <h2>{{ scoreHeadline }}</h2>
            <p>{{ $t('Du fikk§DailyQuizPlayViewT') }} {{ correctCount }} {{ $t('av') }} {{ state.result.answers.length }} {{ $t('spørsmål riktig. Se hvordan du ligger an på topplisten.') }}</p>
          </div>
        </div>

        <div v-else class="dq-card dq-score-hero dq-score-hero--wait">
          <div class="dq-wait-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg>
          </div>
          <div class="dq-score-copy">
            <h2>{{ $t('Svarene dine er sendt inn') }}</h2>
            <p>{{ $t('En administrator sjekker') }} {{ pendingCount === 1 ? $t('ett svar') : $t('{0} svar', [pendingCount]) }}{{ $t('. Poengsummen din og de riktige svarene vises når alt er sjekket - kom tilbake senere.') }}</p>
          </div>
        </div>

        <ExpertCard
          v-if="state.result.expert && state.result.score !== null"
          :expert-value="state.result.expert.score"
          :max="state.result.expert.maxScore"
          :your-value="state.result.score"
        />

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
                <span class="label">{{ $t('Ditt svar') }}</span>
                <b>{{ a.yourAnswer || $t('(tomt)') }}</b>
              </div>
              <div v-if="showCorrect(a)" class="dq-answer-line">
                <span class="label">{{ $t('Riktig svar§DailyQuizPlayViewT') }}</span>
                <b>{{ a.correctAnswer }}</b>
              </div>
              <DailyQuizEasyMeter :percent="a.percentCorrect" :players="a.playersCounted" />
            </div>
            <div class="dq-answer-end">
              <template v-if="!underReview">
              <span v-if="a.verdict === 'PENDING'" class="dq-pill dq-pill--wait">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></svg>
                {{ $t('Til gjennomgang') }}
              </span>
              <span v-else-if="a.yearQuestion" class="dq-pill" :class="a.pointsAwarded > 0 ? 'dq-pill--ok' : 'dq-pill--bad'">
                {{ $t('{0} poeng¦{0} poeng', [a.pointsAwarded]) }}
              </span>
              <span v-else class="dq-pill" :class="a.verdict === 'CORRECT' ? 'dq-pill--ok' : 'dq-pill--bad'">
                {{ a.verdict === 'CORRECT' ? $t('Riktig§DailyQuizPlayViewM') : $t('Feil§DailyQuizPlayViewM') }}
              </span>
              </template>
              <span v-if="a.yearQuestion" class="dq-pill dq-pill--year">{{ $t('Årstall') }}</span>
            </div>
          </article>
        </div>
      </template>
    </template>

    <!-- Submitting is final (the backend rejects a second submission), so ask first - and call out any
         blank boxes, which are marked wrong rather than skipped. -->
    <div v-if="showSubmitConfirm" class="modal-backdrop" @click.self="!submitting && (showSubmitConfirm = false)">
      <div class="modal dq-modal" role="alertdialog" aria-modal="true" :aria-label="$t('Sende inn svarene dine?')">
        <div class="dq-modal-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9" /><path d="M8 12.5l2.7 2.7L16 9.5" /></svg>
        </div>
        <h2 class="dq-modal-title">{{ $t('Sende inn svarene dine?') }}</h2>
        <p class="dq-modal-sub">
          {{ $t('Du har svart på') }} {{ answeredCount }} {{ $t('av') }} {{ state?.questions?.length }}{{ $t('. Du kan ikke endre dem etter at de er sendt inn.') }}
        </p>
        <div v-if="blankCount" class="dq-note">
          {{ $t('{0} spørsmål er fortsatt ubesvart - ubesvarte spørsmål teller som feil.¦{0} spørsmål er fortsatt ubesvart - ubesvarte spørsmål teller som feil.', [blankCount]) }}
        </div>
        <div class="dq-modal-actions">
          <button class="btn btn-secondary" :disabled="submitting" @click="showSubmitConfirm = false">
            {{ blankCount ? $t('Fortsett å svare') : $t('Gå tilbake') }}
          </button>
          <button class="btn btn-primary" :disabled="submitting" @click="submit">
            {{ submitting ? $t('Sender inn…') : $t('Send inn') }}
          </button>
        </div>
      </div>
    </div>

    <DailyQuizScoreboardModal
      v-if="showScoreboard"
      :title="$t('Toppliste')"
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
import { t as _t } from '../i18n'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRoute } from 'vue-router'
import api from '../services/api'
import toast from '../services/toast'
import LoadingState from '../components/LoadingState.vue'
import DailyQuizScoreboardModal from '../components/DailyQuizScoreboardModal.vue'
import DailyQuizEasyMeter from '../components/DailyQuizEasyMeter.vue'
import ExpertCard from '../components/ExpertCard.vue'
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
    return _t('{0} spørsmål i pubquiz-stil - skriv svarene dine, ingen flervalg.', [state.value.questions.length])
  }
  return state.value.result?.score == null ? _t('Innsendt - venter på at en administrator skal bli ferdig med å sjekke.') : _t('Slik gikk det.')
})

// ---- save for later ----
// What's on the server (as a snapshot string) - anything different from it is an unsaved change.
const saving = ref(false)
const savedAt = ref(null)
const savedSnapshot = ref('{}')

function currentAnswers() {
  const out = {}
  for (const q of state.value?.questions || []) {
    const text = (answers[q.questionId] || '').trim()
    if (text) out[q.questionId] = text
  }
  return out
}
const dirty = computed(() => state.value?.attemptStatus === 'IN_PROGRESS' && JSON.stringify(currentAnswers()) !== savedSnapshot.value)

async function saveDraft() {
  saving.value = true
  try {
    const snapshot = JSON.stringify(currentAnswers())
    const payload = state.value.questions.map(q => ({ questionId: q.questionId, answerText: answers[q.questionId] || '' }))
    await api.saveDailyQuizDraft(quizId, payload)
    savedSnapshot.value = snapshot
    savedAt.value = Date.now()
    toast.show(_t('Lagret - du kan fortsette senere.'))
  } catch (e) {
    toast.show(e.response?.data?.message || _t('Kunne ikke lagre svarene dine. Prøv igjen.'))
  } finally {
    saving.value = false
  }
}

// Leaving with unsaved typing would lose it - ask first (in-app navigation and tab close/reload).
function warnBeforeUnload(e) {
  if (dirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}
window.addEventListener('beforeunload', warnBeforeUnload)
onBeforeUnmount(() => window.removeEventListener('beforeunload', warnBeforeUnload))
onBeforeRouteLeave(() => {
  if (dirty.value && !submitting.value) {
    return window.confirm(_t('Du har svar som ikke er lagret. Forlate siden uten å lagre?'))
  }
})

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    state.value = await api.getDailyQuizPlayState(quizId)
    if (state.value.attemptStatus === 'IN_PROGRESS' && state.value.draftAnswers) {
      for (const [questionId, text] of Object.entries(state.value.draftAnswers)) answers[questionId] = text
      savedSnapshot.value = JSON.stringify(currentAnswers())
      if (Object.keys(state.value.draftAnswers).length) savedAt.value = Date.now()
    }
  } catch (e) {
    error.value = _t('Kunne ikke laste denne quizen.')
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
    error.value = e.response?.data?.message || _t('Kunne ikke sende inn svarene dine.')
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
  if (pct >= 0.9) return _t('Strålende!')
  if (pct >= 0.7) return _t('Flott runde!')
  if (pct >= 0.4) return _t('Bra jobba')
  return _t('Bra innsats - vi sees i morgen')
})
const correctCount = computed(() => (state.value?.result?.answers || []).filter(a => a.verdict === 'CORRECT').length)
// The real number still waiting on an admin - until it's zero every row reads "pending", so counting rows would just say "all of them".
const pendingCount = computed(() => state.value?.result?.pendingCount ?? 0)
// Nothing but the player's own answers is shown until the whole quiz has been reviewed.
const underReview = computed(() => state.value?.result?.score === null)

function verdictClass(a) {
  if (underReview.value) return ''
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
  return new Date(dateStr).toLocaleDateString(window.QUIZ_LOCALE, { weekday: 'long', day: 'numeric', month: 'long' })
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
    toast.show(_t('Kunne ikke oppdatere innstillingen for topplisten.'))
    leaderboardOptIn.value = previous
  }
}
</script>
