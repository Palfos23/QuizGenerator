<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px;">
      <div>
        <h1>Question bank</h1>
        <p class="page-subtitle">Every question here is available to the quiz generator. Add, edit or remove them.</p>
      </div>
      <button class="btn btn-primary" @click="openCreate">+ Add question</button>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="successMessage" class="banner success">{{ successMessage }}</div>

    <div class="field" style="max-width:220px;">
      <label>Filter by language</label>
      <select v-model="languageFilter">
        <option value="ALL">All languages</option>
        <option v-for="lang in LANGUAGES" :key="lang.code" :value="lang.code">{{ lang.flag }} {{ lang.label }}</option>
      </select>
    </div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="!filteredQuestions.length" class="empty-state">
      No questions yet{{ languageFilter !== 'ALL' ? ' in ' + languageLabel(languageFilter) : '' }}.
      Click "Add question" to create one.
    </div>

    <div v-else class="table-scroll">
      <table class="table">
        <thead>
          <tr>
            <th>Question</th>
            <th>Category</th>
            <th>Lang</th>
            <th>Difficulty</th>
            <th>Answer</th>
            <th>Could change?</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="q in filteredQuestions" :key="q.id">
            <td style="max-width:280px;">{{ q.questionText }}</td>
            <td>{{ q.category }}</td>
            <td>{{ flagFor(q.language) }}</td>
            <td><span class="tag difficulty-tag" :style="{ background: difficultyColor(q.difficultyLevel) }">{{ q.difficultyLevel }}/10</span></td>
            <td>{{ q.answer }}</td>
            <td>
              <span v-if="q.couldChange" class="tag changeable">May change</span>
              <span v-else style="color:var(--text-dim); font-size:0.85rem;">Stable</span>
            </td>
            <td style="white-space:nowrap;">
              <button class="btn btn-secondary btn-sm" @click="openEdit(q)">Edit</button>
              <button class="btn btn-danger btn-sm" style="margin-left:6px;" @click="confirmDelete(q)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <QuestionFormModal
      v-if="showModal"
      :question="editingQuestion"
      @close="showModal = false"
      @saved="onSaved"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import QuestionFormModal from '../components/QuestionFormModal.vue'
import { LANGUAGES, languageLabel, difficultyColor } from '../constants'

const questions = ref([])
const loading = ref(true)
const error = ref('')
const successMessage = ref('')
const languageFilter = ref('ALL')

const showModal = ref(false)
const editingQuestion = ref(null)

const filteredQuestions = computed(() =>
  languageFilter.value === 'ALL'
    ? questions.value
    : questions.value.filter(q => q.language === languageFilter.value)
)

onMounted(loadQuestions)

async function loadQuestions() {
  loading.value = true
  error.value = ''
  try {
    questions.value = await api.adminListQuestions()
  } catch (e) {
    error.value = 'Could not load questions from the server.'
  } finally {
    loading.value = false
  }
}

function flagFor(code) {
  return LANGUAGES.find(l => l.code === code)?.flag || code
}

function openCreate() {
  editingQuestion.value = null
  showModal.value = true
}

function openEdit(q) {
  editingQuestion.value = q
  showModal.value = true
}

function onSaved() {
  showModal.value = false
  successMessage.value = 'Question saved.'
  loadQuestions()
}

async function confirmDelete(q) {
  if (!window.confirm(`Delete this question?\n\n"${q.questionText}"`)) return
  error.value = ''
  try {
    await api.adminDeleteQuestion(q.id)
    successMessage.value = 'Question deleted.'
    loadQuestions()
  } catch (e) {
    error.value = 'Could not delete the question.'
  }
}
</script>
