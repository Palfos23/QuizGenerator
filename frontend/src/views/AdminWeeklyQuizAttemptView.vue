<template>
  <div>
    <router-link to="/admin/weekly-quiz-review" class="btn btn-secondary btn-sm">← All players</router-link>

    <div v-if="error" class="banner error" style="margin-top:16px;">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim); margin-top:16px;">Loading…</div>

    <template v-else-if="attempt">
      <h1 style="margin-top:16px;">{{ attempt.playerName }}</h1>
      <p class="page-subtitle">Week of {{ formatDate(attempt.weekStartDate) }}</p>

      <div class="saved-quiz-list">
        <div v-for="a in attempt.answers" :key="a.answerId" class="saved-quiz-row" style="align-items:flex-start;">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ a.questionNumber }}. {{ a.questionText }}</div>
            <div class="saved-quiz-meta">Correct answer: <strong>{{ a.correctAnswer }}</strong></div>
            <div style="color:var(--text-dim); font-size:0.85rem; margin-top:6px;">
              Answered: <strong>{{ a.submittedAnswer || '(blank)' }}</strong>
            </div>
          </div>
          <div v-if="a.verdict === 'PENDING'" style="display:flex; gap:8px;">
            <button class="btn btn-primary btn-sm" :disabled="busyId === a.answerId" @click="resolve(a, true)">Mark correct</button>
            <button class="btn btn-danger btn-sm" :disabled="busyId === a.answerId" @click="resolve(a, false)">Mark incorrect</button>
          </div>
          <span
            v-else
            class="tag"
            :style="a.verdict === 'CORRECT' ? { background: 'rgba(61,220,151,0.15)', color: 'var(--teal)' } : { background: 'rgba(255,77,109,0.15)', color: 'var(--coral)' }"
          >{{ a.verdict === 'CORRECT' ? '✓' : '✕' }}</span>
        </div>
      </div>

      <div v-if="!pendingCount" class="empty-state friendly" style="margin-top:20px;">
        All of {{ attempt.playerName }}'s answers have been resolved.
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import toast from '../services/toast'

const route = useRoute()
const attemptId = route.params.id

const attempt = ref(null)
const loading = ref(true)
const error = ref('')
const busyId = ref(null)

const pendingCount = computed(() => attempt.value?.answers.filter(a => a.verdict === 'PENDING').length || 0)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    attempt.value = await api.adminGetWeeklyQuizAttempt(attemptId)
  } catch (e) {
    error.value = 'Could not load this player\'s answers.'
  } finally {
    loading.value = false
  }
}

async function resolve(answer, correct) {
  busyId.value = answer.answerId
  error.value = ''
  try {
    await api.adminResolveWeeklyQuizAnswer(answer.answerId, correct)
    answer.verdict = correct ? 'CORRECT' : 'INCORRECT'
    toast.show(correct ? 'Marked correct.' : 'Marked incorrect.')
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not resolve that answer.'
  } finally {
    busyId.value = null
  }
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>
