<template>
  <div>
    <h1>Weekly quiz - review answers</h1>
    <p class="page-subtitle">Answers that weren't an exact match to the stored answer - decide if they should still count.</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="!pending.length" class="empty-state friendly">Nothing waiting for review.</div>

    <div v-else class="saved-quiz-list">
      <div v-for="a in pending" :key="a.id" class="saved-quiz-row" style="align-items:flex-start;">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ a.questionText }}</div>
          <div class="saved-quiz-meta">Correct answer: <strong>{{ a.correctAnswer }}</strong></div>
          <div style="color:var(--text-dim); font-size:0.85rem; margin-top:6px;">
            {{ a.playerName }} answered: <strong>{{ a.submittedAnswer || '(blank)' }}</strong>
          </div>
        </div>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" :disabled="busyId === a.id" @click="resolve(a, true)">Mark correct</button>
          <button class="btn btn-danger btn-sm" :disabled="busyId === a.id" @click="resolve(a, false)">Mark incorrect</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'

const pending = ref([])
const loading = ref(true)
const error = ref('')
const busyId = ref(null)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    pending.value = await api.adminListWeeklyQuizPending()
  } catch (e) {
    error.value = 'Could not load pending answers.'
  } finally {
    loading.value = false
  }
}

async function resolve(answer, correct) {
  busyId.value = answer.id
  error.value = ''
  try {
    await api.adminResolveWeeklyQuizAnswer(answer.id, correct)
    pending.value = pending.value.filter(p => p.id !== answer.id)
    toast.show(correct ? 'Marked correct.' : 'Marked incorrect.')
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not resolve that answer.'
  } finally {
    busyId.value = null
  }
}
</script>
