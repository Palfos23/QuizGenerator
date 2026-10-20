<template>
  <div class="dq-page">
    <header class="dq-hero">
      <span class="dq-eyebrow">En ny hver dag</span>
      <h1 class="dq-title">Dagens quiz</h1>
      <p class="dq-sub">15 spørsmål i pubquiz-stil - skriv svarene dine, ingen flervalg.</p>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="loading" style="color:var(--text-dim);">Laster…</div>

    <!-- Today -->
    <section v-else-if="today" class="dq-card dq-today">
      <span class="dq-eyebrow">I dag</span>
      <div class="dq-today-date">{{ formatLong(today.quizDate) }}</div>
      <div class="dq-today-meta">
        <span class="dq-pill">{{ today.questionCount }} spørsmål</span>
        <span class="dq-pill">{{ today.maxScore }} poeng</span>
        <span class="dq-pill" :class="statusPill(today).cls">{{ statusPill(today).label }}</span>
      </div>
      <div class="dq-today-cta">
        <router-link :to="`/daily-quiz/${today.id}`" class="btn btn-primary">{{ todayButton(today.status) }}</router-link>
        <span v-if="today.status === 'GRADED'" class="dq-score-note">Du fikk <strong>{{ today.score }} / {{ today.maxScore }}</strong></span>
        <span v-else-if="today.status === 'SUBMITTED'" class="dq-score-note">En administrator sjekker noen av svarene dine.</span>
      </div>
    </section>

    <div v-else class="dq-card dq-empty">
      <strong>Ingen quiz i dag ennå</strong>
      Kom tilbake snart.
    </div>

    <DailyQuizWeeklyPanel style="margin-top:28px;" />

    <!-- Previous quizzes -->
    <h2 class="dq-section-title">Tidligere quizer <small>fjernes etter 7 dager - ukepoengene dine beholdes</small></h2>
    <button class="dq-disclosure" :class="{ 'is-open': showArchive }" @click="showArchive = !showArchive">
      {{ showArchive ? 'Skjul' : 'Vis' }} tidligere quizer
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6" /></svg>
    </button>

    <div v-if="showArchive" style="margin-top:8px;">
      <div v-if="!archiveQuizzes.length" class="dq-card dq-card--flat dq-empty" style="padding:22px;">Ingen tidligere quizer ennå.</div>
      <div v-else class="dq-list">
        <router-link v-for="q in archiveQuizzes" :key="q.id" :to="`/daily-quiz/${q.id}`" class="dq-row">
          <div class="dq-row-main">
            <div class="dq-row-title">{{ formatLong(q.quizDate) }}</div>
            <div class="dq-row-meta">{{ q.questionCount }} spørsmål · {{ q.maxScore }} poeng</div>
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
    error.value = 'Kunne ikke laste dagens quiz.'
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
  return new Date(iso).toLocaleDateString('nb-NO', { weekday: 'long', day: 'numeric', month: 'long' })
}

function todayButton(status) {
  if (status === 'NOT_STARTED') return 'Spill dagens quiz'
  if (status === 'IN_PROGRESS') return 'Fortsett'
  return 'Se svarene dine'
}

function statusPill(q) {
  if (q.status === 'GRADED') return { cls: 'dq-pill--ok', label: `${q.score} / ${q.maxScore}` }
  if (q.status === 'SUBMITTED') return { cls: 'dq-pill--wait', label: 'Til gjennomgang' }
  if (q.status === 'IN_PROGRESS') return { cls: 'dq-pill--gold', label: 'Pågår' }
  return { cls: '', label: 'Ikke spilt' }
}
</script>
