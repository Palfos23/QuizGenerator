<template>
  <div class="dq-page">
    <header class="dq-hero">
      <span class="dq-eyebrow">{{ $t('En ny hver dag') }}</span>
      <h1 class="dq-title">{{ $t('Dagens quiz§DailyQuizListViewT') }}</h1>
      <p class="dq-sub">{{ $t('15 spørsmål i pubquiz-stil - skriv svarene dine, ingen flervalg.') }}</p>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

    <!-- Today -->
    <section v-else-if="today" class="dq-card dq-today">
      <span class="dq-eyebrow">{{ $t('I dag') }}</span>
      <div class="dq-today-date">{{ formatLong(today.quizDate) }}</div>
      <div class="dq-today-meta">
        <span class="dq-pill">{{ today.questionCount }} {{ $t('spørsmål§DailyQuizListViewT') }}</span>
        <span class="dq-pill">{{ today.maxScore }} {{ $t('poeng§DailyQuizListViewT') }}</span>
        <span class="dq-pill" :class="statusPill(today).cls">{{ statusPill(today).label }}</span>
      </div>
      <div class="dq-today-cta">
        <router-link :to="`/daily-quiz/${today.id}`" class="btn btn-primary">{{ todayButton(today.status) }}</router-link>
        <span v-if="today.status === 'GRADED'" class="dq-score-note">{{ $t('Du fikk§DailyQuizListViewT') }} <strong>{{ today.score }} / {{ today.maxScore }}</strong></span>
        <span v-else-if="today.status === 'SUBMITTED'" class="dq-score-note">{{ $t('En administrator sjekker noen av svarene dine.') }}</span>
      </div>
    </section>

    <div v-else class="dq-card dq-empty">
      <strong>{{ $t('Ingen quiz i dag ennå') }}</strong>
      {{ $t('Kom tilbake snart.') }}
    </div>

    <DailyQuizWeeklyPanel style="margin-top:28px;" />

    <!-- Previous quizzes -->
    <h2 class="dq-section-title">{{ $t('Tidligere quizer') }} <small>{{ $t('fjernes etter 7 dager - ukepoengene dine beholdes') }}</small></h2>
    <button class="dq-disclosure" :class="{ 'is-open': showArchive }" @click="showArchive = !showArchive">
      {{ showArchive ? $t('Skjul') : $t('Vis§DailyQuizListViewM') }} {{ $t('tidligere quizer') }}
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6" /></svg>
    </button>

    <div v-if="showArchive" style="margin-top:8px;">
      <div v-if="!archiveQuizzes.length" class="dq-card dq-card--flat dq-empty" style="padding:22px;">{{ $t('Ingen tidligere quizer ennå.') }}</div>
      <div v-else class="dq-list">
        <router-link v-for="q in archiveQuizzes" :key="q.id" :to="`/daily-quiz/${q.id}`" class="dq-row">
          <div class="dq-row-main">
            <div class="dq-row-title">{{ formatLong(q.quizDate) }}</div>
            <div class="dq-row-meta">{{ q.questionCount }} {{ $t('spørsmål ·') }} {{ q.maxScore }} {{ $t('poeng§DailyQuizListViewT') }}</div>
          </div>
          <div class="dq-row-end">
            <span class="dq-pill dq-keep-on-mobile" :class="statusPill(q).cls">{{ statusPill(q).label }}</span>
            <svg class="dq-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6" /></svg>
          </div>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import DailyQuizWeeklyPanel from '../components/DailyQuizWeeklyPanel.vue'

const activeQuizzes = ref([])
const archiveQuizzes = ref([])
const loading = ref(true)
const error = ref('')
const showArchive = ref(false)

// Always exactly one active quiz - today's (see DailyQuizService#findActive).
const today = computed(() => activeQuizzes.value[0] || null)

onMounted(async () => {
  try {
    activeQuizzes.value = await api.getActiveDailyQuizzes()
  } catch (e) {
    error.value = _t('Kunne ikke laste dagens quiz.')
  } finally {
    loading.value = false
  }
  try {
    archiveQuizzes.value = await api.getArchiveDailyQuizzes()
  } catch (e) {
    // archive is a nice-to-have - fail quietly
  }
})

function formatLong(iso) {
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { weekday: 'long', day: 'numeric', month: 'long' })
}

function todayButton(status) {
  if (status === 'NOT_STARTED') return _t('Spill dagens quiz')
  if (status === 'IN_PROGRESS') return _t('Fortsett§DailyQuizListViewJ')
  return _t('Se svarene dine§DailyQuizListViewJ')
}

function statusPill(q) {
  if (q.status === 'GRADED') return { cls: 'dq-pill--ok', label: `${q.score} / ${q.maxScore}` }
  if (q.status === 'SUBMITTED') return { cls: 'dq-pill--wait', label: _t('Til gjennomgang') }
  if (q.status === 'IN_PROGRESS') return { cls: 'dq-pill--gold', label: _t('Pågår') }
  return { cls: '', label: _t('Ikke spilt') }
}
</script>
