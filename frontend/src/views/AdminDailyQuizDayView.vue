<template>
  <div class="dq-page dq-page--wide">
    <div class="dq-topbar">
      <router-link to="/admin/daily-quiz-review" class="dq-back">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6" /></svg>
        Daily quiz review
      </router-link>
      <div v-if="day" class="dq-topbar-actions">
        <button class="dq-chip-btn" @click="showQuestions = true">Questions</button>
      </div>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <template v-else-if="day">
      <header class="dq-hero">
        <span class="dq-eyebrow">Players</span>
        <h1 class="dq-title">{{ formatLong(day.quizDate) }}</h1>
        <p class="dq-sub">Everyone who submitted this day's quiz, including players whose answers have all been decided. Open one to review or change a decision.</p>
      </header>

      <div v-if="!day.attempts.length" class="dq-card dq-empty">
        <strong>No submissions yet</strong>
        Nobody has submitted this quiz yet.
      </div>

      <div v-else class="dq-list">
        <router-link v-for="a in day.attempts" :key="a.attemptId" :to="`/admin/daily-quiz-review/${a.attemptId}`" class="dq-row">
          <span class="dq-avatar">{{ initials(a.playerName) }}</span>
          <div class="dq-row-main">
            <div class="dq-row-title">{{ a.playerName }}</div>
            <div class="dq-row-meta">{{ a.status === 'GRADED' ? 'All answers decided' : 'Waiting for review' }}</div>
          </div>
          <div class="dq-row-end">
            <span v-if="a.status === 'GRADED'" class="dq-pill dq-pill--ok dq-keep-on-mobile">{{ a.score }} / {{ a.maxScore }}</span>
            <span v-else class="dq-pill dq-pill--wait dq-keep-on-mobile">{{ a.pendingCount }} to review</span>
            <svg class="dq-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6" /></svg>
          </div>
        </router-link>
      </div>
    </template>

    <AdminDailyQuizQuestionsModal
      v-if="showQuestions"
      :set-id="route.params.id"
      :title="`Questions - ${formatLong(day.quizDate)}`"
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
    error.value = 'Could not load this day\'s players.'
  } finally {
    loading.value = false
  }
})

function initials(name) {
  return (name || '?').split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('')
}

function formatLong(iso) {
  return new Date(iso).toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' })
}
</script>
