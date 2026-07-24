<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:12px;">
      <h1 style="margin:0;">Suggest a question</h1>
      <router-link to="/dashboard" class="btn btn-secondary btn-sm">← Back to dashboard</router-link>
    </div>
    <p class="page-subtitle">
      Submit a question for admin review. If it's approved, it joins the shared question bank for everyone.
      If it's rejected, you'll see why here - and you can still use it in your own quizzes either way.
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field">
      <label>Question</label>
      <input type="text" v-model="form.questionText" placeholder="e.g. What is the capital of Norway?" />
    </div>

    <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
      <div style="flex:1; min-width:160px;">
        <label>Category</label>
        <input type="text" v-model="form.category" placeholder="e.g. Geography" />
      </div>
      <div style="flex:1; min-width:160px;">
        <label>Difficulty <span class="picker-hint">1 (easy) - 10 (hard)</span></label>
        <input type="number" min="1" max="10" v-model.number="form.difficultyLevel" />
      </div>
    </div>

    <div class="field">
      <label>Language <span class="picker-hint">choose one</span></label>
      <div class="language-row">
        <button
          v-for="lang in LANGUAGES"
          :key="lang.code"
          class="language-btn"
          :class="{ active: form.language === lang.code }"
          @click="form.language = lang.code"
        >
          <span>{{ lang.flag }}</span> {{ lang.label }}
        </button>
      </div>
    </div>

    <div class="field">
      <label>Answer</label>
      <input type="text" v-model="form.answer" />
    </div>

    <div class="field">
      <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:400;">
        <input type="checkbox" v-model="form.couldChange" style="width:auto;" />
        This answer could change over time (e.g. a current record or title-holder)
      </label>
    </div>

    <button class="btn btn-primary" :disabled="submitting" @click="submit">
      {{ submitting ? 'Submitting…' : 'Submit for review' }}
    </button>

    <h2 style="margin-top:40px;">Your submissions</h2>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
    <div v-else-if="!submissions.length" class="empty-state">You haven't submitted any questions yet.</div>

    <div v-else class="saved-quiz-list">
      <div v-for="s in submissions" :key="s.id" class="saved-quiz-row" style="align-items:flex-start;">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ s.questionText }}</div>
          <div class="saved-quiz-meta">
            {{ s.category }} · {{ languageLabel(s.language) }} · difficulty {{ s.difficultyLevel }}
          </div>
          <div v-if="s.status === 'REJECTED'" style="color:var(--coral); font-size:0.85rem; margin-top:6px;">
            Rejected: {{ s.rejectionReason }}
          </div>
        </div>
        <span
          class="tag"
          :style="statusStyle(s.status)"
        >{{ statusLabel(s.status) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import { LANGUAGES, languageLabel } from '../constants'

const form = reactive({
  questionText: '', category: '', difficultyLevel: 5, language: 'EN', answer: '', couldChange: false
})
const submitting = ref(false)
const error = ref('')
const submissions = ref([])
const loading = ref(true)

onMounted(loadMine)

async function loadMine() {
  loading.value = true
  try {
    submissions.value = await api.myQuestionSubmissions()
  } catch (e) {
    error.value = 'Could not load your submissions.'
  } finally {
    loading.value = false
  }
}

async function submit() {
  error.value = ''
  if (!form.questionText.trim() || !form.category.trim() || !form.answer.trim()) {
    error.value = 'Question, category and answer are all required.'
    return
  }
  submitting.value = true
  try {
    await api.submitQuestion(form)
    toast.show('Submitted for review.')
    form.questionText = ''
    form.category = ''
    form.answer = ''
    form.couldChange = false
    loadMine()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit that question.'
  } finally {
    submitting.value = false
  }
}

function statusLabel(status) {
  return { PENDING: 'Pending review', APPROVED: 'Approved', REJECTED: 'Rejected' }[status] || status
}
function statusStyle(status) {
  if (status === 'APPROVED') return { background: 'rgba(61,220,151,0.15)', color: 'var(--teal)' }
  if (status === 'REJECTED') return { background: 'rgba(255,77,109,0.15)', color: 'var(--coral)' }
  return { background: 'rgba(242,183,5,0.15)', color: 'var(--gold)' }
}
</script>
