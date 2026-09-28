<template>
  <div style="max-width:720px; margin:0 auto;">
    <h1>Weekly quiz</h1>
    <p class="page-subtitle" v-if="state">Week of {{ formatDate(state.weekStartDate) }} - 15 questions, pub-quiz style.</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <LoadingState v-if="loading" full message="Loading this week's quiz…" />

    <template v-else-if="state">
      <template v-if="state.attemptStatus === 'IN_PROGRESS'">
        <form @submit.prevent="submit">
          <div v-for="q in state.questions" :key="q.questionId" class="field">
            <label>{{ q.questionNumber }}. {{ q.text }}</label>
            <input type="text" v-model="answers[q.questionId]" autocomplete="off" />
          </div>
          <button type="submit" class="btn btn-primary" style="width:100%; margin-top:12px;" :disabled="submitting">
            {{ submitting ? 'Submitting…' : 'Submit answers' }}
          </button>
        </form>
      </template>

      <template v-else-if="state.attemptStatus === 'SUBMITTED'">
        <div class="empty-state friendly">
          Thanks! An admin still needs to check a few of your answers - come back later for your score.
        </div>
      </template>

      <template v-else-if="state.attemptStatus === 'GRADED' && state.result">
        <h2 style="text-align:center;">You scored {{ state.result.score }} / {{ state.result.maxScore }}</h2>
        <div class="saved-quiz-list">
          <div v-for="a in state.result.answers" :key="a.questionNumber" class="saved-quiz-row" style="align-items:flex-start;">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ a.questionNumber }}. {{ a.questionText }}</div>
              <div class="saved-quiz-meta">
                Your answer: {{ a.yourAnswer || '(blank)' }}
                <span v-if="a.verdict === 'INCORRECT'"> · Correct answer: {{ a.correctAnswer }}</span>
              </div>
            </div>
            <span class="tag" :style="a.verdict === 'CORRECT' ? { background: 'rgba(61,220,151,0.15)', color: 'var(--teal)' } : { background: 'rgba(255,77,109,0.15)', color: 'var(--coral)' }">
              {{ a.verdict === 'CORRECT' ? '✓' : '✕' }}
            </span>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import LoadingState from '../components/LoadingState.vue'

const state = ref(null)
const loading = ref(true)
const error = ref('')
const answers = reactive({})
const submitting = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    state.value = await api.getWeeklyQuizPlayState()
  } catch (e) {
    error.value = 'Could not load this week\'s quiz.'
  } finally {
    loading.value = false
  }
}

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    const payload = state.value.questions.map(q => ({ questionId: q.questionId, answerText: answers[q.questionId] || '' }))
    state.value = await api.submitWeeklyQuizAnswers(payload)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit your answers.'
  } finally {
    submitting.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString(undefined, { day: 'numeric', month: 'long', year: 'numeric' })
}
</script>
