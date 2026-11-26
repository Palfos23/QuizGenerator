<template>
  <div class="dq-page dq-page--wide">
    <div class="dq-topbar">
      <router-link to="/admin/daily-quiz-review" class="dq-back">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6" /></svg>
        {{ $t('Gjennomgang av dagens quiz') }}
      </router-link>
      <div v-if="day" class="dq-topbar-actions">
        <button class="dq-chip-btn" @click="showQuestions = true">{{ $t('Spørsmål§AdminDailyQuizDayViewT') }}</button>
      </div>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>
    <LoadingSkeleton v-if="loading" />

    <template v-else-if="day">
      <header class="dq-hero">
        <span class="dq-eyebrow">{{ $t('Spillere') }}</span>
        <h1 class="dq-title">{{ formatLong(day.quizDate) }}</h1>
        <p class="dq-sub">{{ $t('Alle som sendte inn quizen denne dagen, inkludert spillere der alle svarene er avgjort. Åpne en for å gjennomgå eller endre en avgjørelse.') }}</p>
      </header>

      <div class="dq-day-tools">
        <input
          v-model="search"
          type="search"
          class="dq-day-search"
          :placeholder="$t('Søk etter spiller…')"
          :aria-label="$t('Søk etter spiller')"
        />
        <div class="dq-segment dq-day-filter" role="group" :aria-label="$t('Vis spillere')">
          <button type="button" :class="{ 'is-on': filter === 'ALL' }" @click="filter = 'ALL'">{{ $t('Alle§AdminDailyQuizDayView') }} · {{ day.total }}</button>
          <button type="button" :class="{ 'is-on': filter === 'PENDING' }" @click="filter = 'PENDING'">{{ $t('Venter§AdminDailyQuizDayView') }} · {{ day.pendingTotal }}</button>
          <button type="button" :class="{ 'is-on': filter === 'GRADED' }" @click="filter = 'GRADED'">{{ $t('Ferdig') }} · {{ day.gradedTotal }}</button>
        </div>
      </div>

      <div v-if="!day.total" class="dq-card dq-empty">
        <strong>{{ $t('Ingen innsendinger ennå') }}</strong>
        {{ $t('Ingen har sendt inn denne quizen ennå.') }}
      </div>

      <div v-else-if="!day.attempts.length" class="dq-card dq-empty">
        <strong>{{ $t('Ingen treff') }}</strong>
        {{ $t('Ingen spillere passer søket eller filteret.') }}
      </div>

      <div v-else class="dq-list" :class="{ 'is-refreshing': refreshing }">
        <router-link v-for="a in day.attempts" :key="a.attemptId" :to="`/admin/daily-quiz-review/${a.attemptId}`" class="dq-row">
          <span class="dq-avatar">{{ initials(a.playerName) }}</span>
          <div class="dq-row-main">
            <div class="dq-row-title">{{ a.playerName }}</div>
            <div class="dq-row-meta">{{ a.status === 'GRADED' ? $t('Alle svar er avgjort') : $t('Venter på gjennomgang§AdminDailyQuizDayViewM') }}</div>
          </div>
          <div class="dq-row-end">
            <span v-if="a.status === 'GRADED'" class="dq-pill dq-pill--ok dq-keep-on-mobile">{{ a.score }} / {{ a.maxScore }}</span>
            <span v-else class="dq-pill dq-pill--wait dq-keep-on-mobile">{{ a.pendingCount }} {{ $t('til gjennomgang') }}</span>
            <svg class="dq-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6" /></svg>
          </div>
        </router-link>
      </div>

      <Pagination :page="page" :page-size="PAGE_SIZE" :total-items="day.matching" @update:page="goToPage" />
    </template>

    <AdminDailyQuizQuestionsModal
      v-if="showQuestions"
      :set-id="route.params.id"
      :title="$t('Spørsmål - {0}', [formatLong(day.quizDate)])"
      @close="showQuestions = false"
    />
  </div>
</template>

<script setup>
import LoadingSkeleton from '../components/LoadingSkeleton.vue'
import Pagination from '../components/Pagination.vue'
import { t as _t } from '../i18n'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../services/api'
import AdminDailyQuizQuestionsModal from '../components/AdminDailyQuizQuestionsModal.vue'

const PAGE_SIZE = 50
const FILTERS = ['ALL', 'PENDING', 'GRADED']

const route = useRoute()
const router = useRouter()
const day = ref(null)
const showQuestions = ref(false)
const loading = ref(true)
const refreshing = ref(false)
const error = ref('')

// Search, filter and page live in the URL, so coming back from a player's answers lands on the same spot.
const search = ref(typeof route.query.q === 'string' ? route.query.q : '')
const filter = ref(FILTERS.includes(route.query.filter) ? route.query.filter : 'ALL')
const page = ref(Math.max(1, parseInt(route.query.page, 10) || 1))

let requestSeq = 0
async function load() {
  const seq = ++requestSeq
  if (day.value) refreshing.value = true
  try {
    const result = await api.adminListDailyQuizSetAttempts(route.params.id, {
      q: search.value.trim(), filter: filter.value, page: page.value - 1, size: PAGE_SIZE
    })
    if (seq !== requestSeq) return // a newer search already went out
    day.value = result
    page.value = result.page + 1 // the server clamps a page that no longer exists
    error.value = ''
    syncUrl()
  } catch (e) {
    if (seq === requestSeq) error.value = _t('Kunne ikke laste spillerne for denne dagen.')
  } finally {
    if (seq === requestSeq) {
      loading.value = false
      refreshing.value = false
    }
  }
}

function syncUrl() {
  const query = {}
  if (search.value.trim()) query.q = search.value.trim()
  if (filter.value !== 'ALL') query.filter = filter.value
  if (page.value > 1) query.page = String(page.value)
  router.replace({ query })
}

function goToPage(p) {
  page.value = p
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

let searchTimer = null
watch(search, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { page.value = 1; load() }, 250)
})
watch(filter, () => { page.value = 1; load() })

onMounted(load)
onBeforeUnmount(() => clearTimeout(searchTimer))

function initials(name) {
  return (name || '?').split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('')
}

function formatLong(iso) {
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { weekday: 'long', day: 'numeric', month: 'long' })
}
</script>

<style scoped>
.dq-day-tools { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 14px; }
.dq-day-search {
  flex: 1 1 220px;
  min-width: 0;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--dq-line);
  background: var(--field-bg);
  color: var(--text);
  font: inherit;
}
.dq-day-filter { flex-wrap: wrap; }
.dq-day-filter button.is-on { background: var(--text); color: rgb(var(--paper-rgb)); }
.dq-list.is-refreshing { opacity: 0.6; transition: opacity 0.15s; }
</style>
