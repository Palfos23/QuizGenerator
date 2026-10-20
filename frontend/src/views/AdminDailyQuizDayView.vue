<template>
  <div class="dq-page dq-page--wide">
    <div class="dq-topbar">
      <router-link to="/admin/daily-quiz-review" class="dq-back">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6" /></svg>
        Gjennomgang av dagens quiz
      </router-link>
      <div v-if="day" class="dq-topbar-actions">
        <button class="dq-chip-btn" @click="showQuestions = true">Spørsmål</button>
      </div>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Laster…</div>

    <template v-else-if="day">
      <header class="dq-hero">
        <span class="dq-eyebrow">Spillere</span>
        <h1 class="dq-title">{{ formatLong(day.quizDate) }}</h1>
        <p class="dq-sub">Alle som sendte inn quizen denne dagen, inkludert spillere der alle svarene er avgjort. Åpne en for å gjennomgå eller endre en avgjørelse.</p>
      </header>

      <div v-if="!day.attempts.length" class="dq-card dq-empty">
        <strong>Ingen innsendinger ennå</strong>
        Ingen har sendt inn denne quizen ennå.
      </div>

      <div v-else class="dq-list">
        <router-link v-for="a in day.attempts" :key="a.attemptId" :to="`/admin/daily-quiz-review/${a.attemptId}`" class="dq-row">
          <span class="dq-avatar">{{ initials(a.playerName) }}</span>
          <div class="dq-row-main">
            <div class="dq-row-title">{{ a.playerName }}</div>
            <div class="dq-row-meta">{{ a.status === 'GRADED' ? 'Alle svar er avgjort' : 'Venter på gjennomgang' }}</div>
          </div>
          <div class="dq-row-end">
            <span v-if="a.status === 'GRADED'" class="dq-pill dq-pill--ok dq-keep-on-mobile">{{ a.score }} / {{ a.maxScore }}</span>
            <span v-else class="dq-pill dq-pill--wait dq-keep-on-mobile">{{ a.pendingCount }} til gjennomgang</span>
            <svg class="dq-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6" /></svg>
          </div>
        </router-link>
      </div>
    </template>

    <AdminDailyQuizQuestionsModal
      v-if="showQuestions"
      :set-id="route.params.id"
      :title="`Spørsmål - ${formatLong(day.quizDate)}`"
      @close="showQuestions = false"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import AdminDailyQuizQuestionsModal from '../components/AdminDailyQuizQuestionsModal.vue'

const route = useRoute()
const day = ref(null)
const showQuestions = ref(false)
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    day.value = await api.adminListDailyQuizSetAttempts(route.params.id)
  } catch (e) {
    error.value = 'Kunne ikke laste spillerne for denne dagen.'
  } finally {
    loading.value = false
  }
})

function initials(name) {
  return (name || '?').split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('')
}

function formatLong(iso) {
  return new Date(iso).toLocaleDateString('nb-NO', { weekday: 'long', day: 'numeric', month: 'long' })
}
</script>
