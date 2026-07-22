<template>
  <div>
    <h1>My quizzes</h1>
    <p class="page-subtitle">Quizzes you've explicitly saved. Generating one doesn't save it automatically.</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="successMessage" class="banner success">{{ successMessage }}</div>

    <!-- List view -->
    <section v-if="!openQuiz">
      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

      <div v-else-if="!quizzes.length" class="empty-state">
        No saved quizzes yet. Generate one on the <router-link to="/generate">Create a quiz</router-link> page,
        then hit "Save to My Quizzes".
      </div>

      <div v-else class="saved-quiz-list">
        <div v-for="q in quizzes" :key="q.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ q.title }}</div>
            <div class="saved-quiz-meta">
              {{ flagFor(q.language) }} {{ languageLabel(q.language) }} · {{ q.questionCount }} questions ·
              saved {{ formatDate(q.createdAt) }}
            </div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openOne(q.id)">View</button>
            <button class="btn btn-danger btn-sm" @click="confirmDelete(q)">Delete</button>
          </div>
        </div>
      </div>
    </section>

    <!-- Detail view -->
    <section v-else>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <h2 style="margin:0;">
          {{ openQuiz.title }}
          <span style="color:var(--text-dim); font-weight:400; font-size:1rem;">· {{ openQuiz.questions.length }} questions</span>
        </h2>
        <button class="btn btn-secondary btn-sm no-print" @click="openQuiz = null">← Back to list</button>
      </div>

      <div class="card-stack">
        <QuestionCard
          v-for="(q, idx) in openQuiz.questions"
          :key="idx"
          :question="q"
          :index="idx"
        />
      </div>

      <div class="no-print" style="margin-top:32px;">
        <button class="btn btn-primary" :disabled="exportingPdf" @click="downloadPdf">
          {{ exportingPdf ? 'Preparing PDF…' : 'Download as PDF' }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'
import QuestionCard from '../components/QuestionCard.vue'
import { LANGUAGES, languageLabel } from '../constants'

const quizzes = ref([])
const loading = ref(true)
const error = ref('')
const successMessage = ref('')
const openQuiz = ref(null)
const exportingPdf = ref(false)

onMounted(loadList)

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    quizzes.value = await api.listSavedQuizzes()
  } catch (e) {
    error.value = 'Could not load your saved quizzes.'
  } finally {
    loading.value = false
  }
}

function flagFor(code) {
  return LANGUAGES.find(l => l.code === code)?.flag || code
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

async function openOne(id) {
  error.value = ''
  try {
    openQuiz.value = await api.getSavedQuiz(id)
  } catch (e) {
    error.value = 'Could not load that quiz.'
  }
}

async function confirmDelete(q) {
  if (!window.confirm(`Delete "${q.title}"? This can't be undone.`)) return
  error.value = ''
  try {
    await api.deleteSavedQuiz(q.id)
    successMessage.value = 'Quiz deleted.'
    loadList()
  } catch (e) {
    error.value = 'Could not delete that quiz.'
  }
}

async function downloadPdf() {
  exportingPdf.value = true
  error.value = ''
  try {
    const blob = await api.exportPdf(openQuiz.value, true)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${openQuiz.value.title.replace(/[^a-zA-Z0-9-_]/g, '_')}.pdf`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    error.value = 'Could not generate the PDF.'
  } finally {
    exportingPdf.value = false
  }
}
</script>
