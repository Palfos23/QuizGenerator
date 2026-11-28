<template>
  <div class="dq-page">
    <header class="dq-hero">
      <h1 class="dq-title">{{ $t('Dagens quiz§DailyQuizListViewT') }}</h1>
      <p class="dq-sub">{{ $t('15 spørsmål i pubquiz-stil - skriv svarene dine, ingen flervalg.') }}</p>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>

    <LoadingSkeleton v-if="loading" />

    <!-- Today -->
    <section v-else-if="today" class="dq-card dq-today">
      <span class="dq-eyebrow">{{ $t('I dag') }} · {{ formatLong(today.quizDate) }}</span>
      <!-- the same score ring as the dashboard's Dagens quiz box, so the two pages read as one -->
      <DailyQuizHeroPages :quiz="today" :lede="todayLede" today-only />
      <div class="dq-today-cta">
        <router-link :to="`/daily-quiz/${today.id}`" class="btn btn-primary">{{ todayButton(today.status) }} →</router-link>
      </div>
    </section>

    <div v-else class="dq-card dq-empty">
      <strong>{{ $t('Ingen quiz i dag ennå') }}</strong>
      {{ $t('Kom tilbake snart.') }}
    </div>

    <!-- Your running leagues with today's match (the same cards as the dashboard); none yet - a way to start one -->
    <div v-if="activeLeagues.length" class="dq-leagues">
      <LeagueCardItem v-for="card in activeLeagues" :key="card.id" :card="card" />
    </div>
    <router-link v-else to="/ligaer" class="dq-row" style="margin-top:14px;">
      <div class="dq-row-main">
        <div class="dq-row-title">{{ $t('Spill mot vennene dine') }}</div>
        <div class="dq-row-meta">{{ $t('Lag en liga på dagens quiz - head-to-head eller på poeng.') }}</div>
      </div>
      <div class="dq-row-end">
        <span class="dq-row-cta">{{ $t('Opprett en liga') }}</span>
        <svg class="dq-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6" /></svg>
      </div>
    </router-link>

    <DailyQuizWeeklyPanel style="margin-top:28px;" />

    <!-- Previous quizzes: the last week's sets as a row of day cards - the date, your score and a bar - always on
         show (there are never more than seven). Swipes sideways on a phone. -->
    <div class="dq-archive-head">
      <h2 class="dq-section-title">{{ $t('Tidligere quizer') }}</h2>
      <span class="dq-archive-note">{{ $t('Slettes etter 7 dager') }}</span>
    </div>
    <div v-if="!archiveQuizzes.length" class="dq-card dq-card--flat dq-empty" style="padding:22px;">{{ $t('Ingen tidligere quizer ennå.') }}</div>
    <template v-else>
      <div class="dq-days">
        <router-link v-for="q in archiveQuizzes" :key="q.id" :to="`/daily-quiz/${q.id}`" class="dq-day" :class="'is-' + q.status.toLowerCase()">
          <span class="dq-day-weekday">{{ weekday(q.quizDate) }}</span>
          <span class="dq-day-date">{{ dayMonth(q.quizDate) }}</span>
          <span class="dq-day-score">{{ q.status === 'GRADED' ? q.score : (q.status === 'SUBMITTED' ? '…' : '–') }}</span>
          <span class="dq-day-bar"><i :style="{ width: q.status === 'GRADED' ? Math.round(q.score / Math.max(1, q.maxScore) * 100) + '%' : '0%' }"></i></span>
          <span class="dq-day-note">{{ dayNote(q) }}</span>
        </router-link>
      </div>
      <p class="dq-archive-foot">{{ $t('Ukepoengene dine beholdes selv om quizen slettes.') }}</p>
    </template>
  </div>
</template>

<script setup>
import LoadingSkeleton from '../components/LoadingSkeleton.vue'
import { t as _t } from '../i18n'
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import DailyQuizWeeklyPanel from '../components/DailyQuizWeeklyPanel.vue'
import DailyQuizHeroPages from '../components/DailyQuizHeroPages.vue'
import LeagueCardItem from '../components/league/LeagueCardItem.vue'

const activeQuizzes = ref([])
const archiveQuizzes = ref([])
const loading = ref(true)
const error = ref('')
const leagues = ref([])
// Running leagues first, at most two - the full list is on Ligaer
const activeLeagues = computed(() => leagues.value.filter(l => l.state === 'ACTIVE' && l.inSeason !== false).slice(0, 2))

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
  api.getMyLeagues().then(list => { leagues.value = list || [] }).catch(() => { /* the plain row shows instead */ })
  try {
    archiveQuizzes.value = await api.getArchiveDailyQuizzes()
  } catch (e) {
    // archive is a nice-to-have - fail quietly
  }
})

function formatLong(iso) {
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { weekday: 'long', day: 'numeric', month: 'long' })
}

const todayLede = computed(() => {
  const s = today.value?.status
  if (s === 'IN_PROGRESS') return _t('Du er i gang - svarene du har lagret venter på deg.')
  if (s === 'SUBMITTED') return _t('En administrator sjekker noen av svarene dine.')
  return _t('15 spørsmål i pubquiz-stil - skriv svarene dine, ingen flervalg.')
})

function weekday(iso) {
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { weekday: 'short' }).replace('.', '')
}
function dayMonth(iso) {
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { day: 'numeric', month: 'short' }).replace('.', '')
}
function dayNote(q) {
  if (q.status === 'GRADED') return _t('av {0}', [q.maxScore])
  if (q.status === 'SUBMITTED') return _t('sjekkes')
  if (q.status === 'IN_PROGRESS') return _t('påbegynt')
  return _t('ikke spilt')
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
