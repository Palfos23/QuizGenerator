<template>
  <div>
    <router-link to="/admin/daily-quiz-review" class="btn btn-secondary btn-sm">← Daily quiz review</router-link>

    <div v-if="error" class="banner error" style="margin-top:16px;">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim); margin-top:16px;">Loading…</div>

    <template v-else-if="day">
      <h1 style="margin-top:16px;">Players - {{ formatDate(day.quizDate) }}</h1>
      <p class="page-subtitle">Everyone who submitted this day's quiz, including players whose answers have all been decided. Open one to review or change a decision.</p>

      <div v-if="!day.attempts.length" class="empty-state friendly">Nobody has submitted this quiz yet.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="a in day.attempts" :key="a.attemptId" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ a.playerName }}</div>
            <div class="saved-quiz-meta">
              <template v-if="a.status === 'GRADED'">Graded · {{ a.score }} / {{ a.maxScore }}</template>
              <template v-else>{{ a.pendingCount }} answer{{ a.pendingCount === 1 ? '' : 's' }} waiting for review</template>
            </div>
          </div>
          <router-link :to="`/admin/daily-quiz-review/${a.attemptId}`" class="btn btn-secondary btn-sm">Review answers</router-link>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'

const route = useRoute()
const day = ref(null)
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

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>
