<template>
  <div>
    <h1>Daily quiz</h1>
    <p class="page-subtitle">15 random questions, pub-quiz style - free text, no multiple choice.</p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="!activeQuizzes.length" class="empty-state friendly">
      No quiz today yet - check back soon.
    </div>

    <div v-else class="saved-quiz-list">
      <div v-for="q in activeQuizzes" :key="q.id" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ formatDate(q.quizDate) }}</div>
          <div class="saved-quiz-meta">{{ q.questionCount }} questions</div>
        </div>
        <div style="display:flex; align-items:center; gap:12px;">
          <span class="tag" :style="statusStyle(q.status)">{{ statusLabel(q) }}</span>
          <router-link :to="`/daily-quiz/${q.id}`" class="btn btn-primary btn-sm">
            {{ buttonLabel(q.status) }}
          </router-link>
        </div>
      </div>
    </div>

    <div class="field" style="margin-top:32px;">
      <label style="cursor:pointer;" @click="showArchive = !showArchive">
        {{ showArchive ? 'Hide' : 'Show' }} previous quizzes
      </label>
      <div v-if="showArchive">
        <div v-if="!archiveQuizzes.length" style="color:var(--text-dim); font-size:0.9rem;">No previous quizzes yet.</div>
        <div v-else class="saved-quiz-list">
          <div v-for="q in archiveQuizzes" :key="q.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ formatDate(q.quizDate) }}</div>
              <div class="saved-quiz-meta">{{ q.questionCount }} questions</div>
            </div>
            <div style="display:flex; align-items:center; gap:12px;">
              <span class="tag" :style="statusStyle(q.status)">{{ statusLabel(q) }}</span>
              <router-link :to="`/daily-quiz/${q.id}`" class="btn btn-secondary btn-sm">
                {{ buttonLabel(q.status) }}
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'

const activeQuizzes = ref([])
const archiveQuizzes = ref([])
const loading = ref(true)
const error = ref('')
const showArchive = ref(false)

onMounted(async () => {
  try {
    activeQuizzes.value = await api.getActiveDailyQuizzes()
  } catch (e) {
    error.value = 'Could not load today\'s quiz.'
  } finally {
    loading.value = false
  }
  try {
    archiveQuizzes.value = await api.getArchiveDailyQuizzes()
  } catch (e) {
    // archive is a nice-to-have - fail quietly
  }
})

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

function buttonLabel(status) {
  if (status === 'NOT_STARTED') return 'Play'
  if (status === 'IN_PROGRESS') return 'Continue'
  return 'View'
}

function statusLabel(q) {
  if (q.status === 'GRADED') return `Graded · ${q.score}/${q.questionCount}`
  if (q.status === 'SUBMITTED') return 'Under review'
  if (q.status === 'IN_PROGRESS') return 'In progress'
  return 'Not started'
}

function statusStyle(status) {
  if (status === 'GRADED') return { background: 'rgba(61,220,151,0.15)', color: 'var(--teal)' }
  if (status === 'SUBMITTED') return { background: 'rgba(242,183,5,0.15)', color: 'var(--gold)' }
  if (status === 'IN_PROGRESS') return { background: 'rgba(242,183,5,0.15)', color: 'var(--gold)' }
  return { background: 'rgba(255,255,255,0.06)', color: 'var(--text-dim)' }
}
</script>
