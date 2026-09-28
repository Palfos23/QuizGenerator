<template>
  <div>
    <h1>Daily quiz - review answers</h1>
    <p class="page-subtitle">Players whose answers weren't an exact match to the stored answer - click one to review just their answers.</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="!pendingAttempts.length" class="empty-state friendly">Nothing waiting for review.</div>

    <div v-else class="saved-quiz-list">
      <div v-for="a in pendingAttempts" :key="a.attemptId" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ a.playerName }}</div>
          <div class="saved-quiz-meta">{{ formatDate(a.quizDate) }} · {{ a.pendingCount }} answer{{ a.pendingCount === 1 ? '' : 's' }} to review</div>
        </div>
        <router-link :to="`/admin/daily-quiz-review/${a.attemptId}`" class="btn btn-primary btn-sm">Review</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'

const pendingAttempts = ref([])
const loading = ref(true)
const error = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    pendingAttempts.value = await api.adminListDailyQuizPendingAttempts()
  } catch (e) {
    error.value = 'Could not load pending answers.'
  } finally {
    loading.value = false
  }
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>
