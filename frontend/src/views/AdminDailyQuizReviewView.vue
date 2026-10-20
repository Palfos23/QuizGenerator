<template>
  <div class="dq-page dq-page--wide">
    <header class="dq-hero">
      <span class="dq-eyebrow">Admin</span>
      <h1 class="dq-title">{{ $t('Gjennomgang av dagens quiz') }}</h1>
      <p class="dq-sub">{{ $t('Svar som ikke var et nøyaktig treff trenger en avgjørelse - åpne en spiller for å gjennomgå svarene deres.') }}</p>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>

    <h2 class="dq-section-title" style="margin-top:8px;">
      {{ $t('Venter på gjennomgang§AdminDailyQuizReviewViewT') }}
      <span v-if="pendingAttempts.length" class="dq-pill dq-pill--wait">{{ pendingAttempts.length }}</span>
    </h2>

    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

    <div v-else-if="!pendingAttempts.length" class="dq-card dq-empty">
      <div class="dq-empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>
      </div>
      <strong>{{ $t('Alt er gjennomgått') }}</strong>
      {{ $t('Ingenting venter på gjennomgang.') }}
    </div>

    <div v-else class="dq-list">
      <router-link v-for="a in pendingAttempts" :key="a.attemptId" :to="`/admin/daily-quiz-review/${a.attemptId}`" class="dq-row">
        <span class="dq-avatar">{{ initials(a.playerName) }}</span>
        <div class="dq-row-main">
          <div class="dq-row-title">{{ a.playerName }}</div>
          <div class="dq-row-meta">{{ formatDate(a.quizDate) }}</div>
        </div>
        <div class="dq-row-end">
          <span class="dq-pill dq-pill--wait dq-keep-on-mobile">{{ a.pendingCount }} {{ $t('til gjennomgang') }}</span>
          <svg class="dq-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6" /></svg>
        </div>
      </router-link>
    </div>

    <DailyQuizWeeklyPanel style="margin-top:34px;" />

    <h2 class="dq-section-title">
      {{ $t('Dager') }}
      <small>{{ $t('åpne en dag for å se hvem som spilte og rette en avgjørelse - quizer fjernes når de er 7 dager gamle') }}</small>
    </h2>

    <div v-if="quizzesLoading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>
    <div v-else-if="!allQuizzes.length" class="dq-card dq-card--flat dq-empty" style="padding:22px;">{{ $t('Ingen daglige quizer ennå.') }}</div>
    <div v-else class="dq-list">
      <div v-for="q in allQuizzes" :key="q.id" class="dq-row">
        <div class="dq-row-main">
          <div class="dq-row-title">{{ formatLong(q.quizDate) }}</div>
        </div>
        <div class="dq-row-end">
          <router-link :to="`/admin/daily-quiz-day/${q.id}`" class="dq-chip-btn">{{ $t('Spillere') }}</router-link>
          <button class="dq-chip-btn" @click="questionsFor = q">{{ $t('Spørsmål§AdminDailyQuizReviewViewT') }}</button>
          <button class="dq-chip-btn" @click="openScoreboard(q)">{{ $t('Toppliste') }}</button>
        </div>
      </div>
    </div>

    <AdminDailyQuizQuestionsModal
      v-if="questionsFor"
      :set-id="questionsFor.id"
      :title="$t('Spørsmål - {0}', [formatDate(questionsFor.quizDate)])"
      @close="questionsFor = null"
    />

    <DailyQuizScoreboardModal
      v-if="showScoreboard"
      :title="$t('Toppliste - {0}', [formatDate(scoreboardQuizDate)])"
      :data="scoreboardData"
      :loading="scoreboardLoading"
      @close="showScoreboard = false"
    />
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { onMounted, ref } from 'vue'
import api from '../services/api'
import DailyQuizWeeklyPanel from '../components/DailyQuizWeeklyPanel.vue'
import DailyQuizScoreboardModal from '../components/DailyQuizScoreboardModal.vue'
import AdminDailyQuizQuestionsModal from '../components/AdminDailyQuizQuestionsModal.vue'

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
    error.value = _t('Kunne ikke laste ventende svar.')
  } finally {
    loading.value = false
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

// --- Days - every quiz still stored (active + archive), independent of the waiting list above, so an
// admin can check scores or go back into a decision even when nothing needs reviewing. Reuses the same
// active/archive endpoints the player-facing list page calls - an admin account hits them like any
// other signed-in user (see SecurityConfig's /api/daily-quiz/** matcher); the per-user status and score
// fields in the response just aren't used here.
const allQuizzes = ref([])
const quizzesLoading = ref(true)

async function loadQuizzes() {
  quizzesLoading.value = true
  try {
    const [active, archive] = await Promise.all([api.getActiveDailyQuizzes(), api.getArchiveDailyQuizzes()])
    allQuizzes.value = [...active, ...archive]
  } catch (e) {
    // a nice-to-have alongside the waiting list above - fail quietly, the empty state covers it
  } finally {
    quizzesLoading.value = false
  }
}

const questionsFor = ref(null)
const showScoreboard = ref(false)
const scoreboardData = ref(null)
const scoreboardLoading = ref(false)
const scoreboardQuizDate = ref(null)

async function openScoreboard(quiz) {
  scoreboardQuizDate.value = quiz.quizDate
  showScoreboard.value = true
  scoreboardLoading.value = true
  try {
    scoreboardData.value = await api.getDailyQuizScoreboard(quiz.id)
  } catch (e) {
    // same fail-quietly as above - the empty state covers it
  } finally {
    scoreboardLoading.value = false
  }
}
</script>
