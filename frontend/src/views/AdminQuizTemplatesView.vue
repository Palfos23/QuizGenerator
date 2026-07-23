<template>
  <div>
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px;">
        <div>
          <h1>Quiz templates</h1>
          <p class="page-subtitle">Published templates users can copy into their own My Quizzes.</p>
        </div>
        <button class="btn btn-primary" @click="openCreate">+ Create template</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

      <div v-else-if="!templates.length" class="empty-state friendly">
        No templates yet. Create one below - it'll be available for any user to copy into their own quizzes.
      </div>

      <div v-else class="saved-quiz-list">
        <div v-for="t in templates" :key="t.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ t.title }}</div>
            <div class="saved-quiz-meta">{{ languageLabel(t.language) }} · {{ t.questionCount }} questions</div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(t.id)">Edit</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(t)">Delete</button>
          </div>
        </div>
      </div>
    </template>

    <template v-else-if="view === 'settings'">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
        <h1 style="margin:0;">{{ editingId ? 'Edit template' : 'Create template' }}</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Back to list</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Title</label>
        <input type="text" v-model="form.title" placeholder="e.g. Office Christmas Party Quiz" />
      </div>

      <div class="field">
        <label>Language <span class="picker-hint">choose one</span></label>
        <div class="language-row">
          <button
            v-for="lang in LANGUAGES"
            :key="lang.code"
            class="language-btn"
            :class="{ active: form.language === lang.code }"
            @click="selectLanguage(lang.code)"
          >
            <span>{{ lang.flag }}</span> {{ lang.label }}
          </button>
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px;">
        <div style="flex:1;">
          <label>Min difficulty</label>
          <input type="range" min="1" max="10" v-model.number="form.minDifficulty" />
          <output>{{ form.minDifficulty }}</output>
        </div>
        <div style="flex:1;">
          <label>Max difficulty</label>
          <input type="range" min="1" max="10" v-model.number="form.maxDifficulty" />
          <output>{{ form.maxDifficulty }}</output>
        </div>
      </div>

      <div class="field">
        <label>Categories <span class="picker-hint">pick as many as you like</span></label>
        <div class="category-chip-grid">
          <div
            v-for="cat in availableCategories"
            :key="cat"
            class="category-chip"
            :class="{ selected: isSelected(cat) }"
          >
            <span class="category-chip-name" @click="toggleCategory(cat)">{{ cat }}</span>
            <template v-if="isSelected(cat)">
              <div class="stepper">
                <button @click="adjustCount(cat, -1)">−</button>
                <span>{{ countFor(cat) }}</span>
                <button @click="adjustCount(cat, 1)">+</button>
              </div>
              <button class="chip-remove-btn" aria-label="Remove category" @click="toggleCategory(cat)">✕</button>
            </template>
          </div>
        </div>
      </div>

      <button class="btn btn-primary" :disabled="generating || !form.categorySelections.length" @click="generateDraft">
        {{ generating ? 'Generating…' : 'Generate draft' }}
      </button>
    </template>

    <template v-else-if="view === 'review'">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
        <input type="text" v-model="quiz.title" class="title-edit-input" aria-label="Quiz title" />
        <button class="btn btn-secondary btn-sm" @click="view = 'settings'">← Adjust settings</button>
      </div>

      <QuizReviewEditor :quiz="quiz" :min-difficulty="form.minDifficulty" :max-difficulty="form.maxDifficulty" @error="error = $event">
        <template #actions>
          <button class="btn btn-primary" :disabled="saving" @click="saveTemplate">
            {{ saving ? 'Saving…' : 'Save template' }}
          </button>
        </template>
      </QuizReviewEditor>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this template?"
      :message="`'${pendingDelete.title}' will no longer be available to copy. Copies users already made are unaffected.`"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import QuizReviewEditor from '../components/QuizReviewEditor.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { LANGUAGES, languageLabel } from '../constants'

const view = ref('list')
const templates = ref([])
const loading = ref(true)
const error = ref('')
const generating = ref(false)
const saving = ref(false)
const editingId = ref(null)
const pendingDelete = ref(null)
const availableCategories = ref([])
const quiz = ref(null)

const form = reactive({
  title: '', language: 'EN', minDifficulty: 1, maxDifficulty: 10, categorySelections: []
})

onMounted(loadTemplates)

async function loadTemplates() {
  loading.value = true
  error.value = ''
  try {
    templates.value = await api.adminListQuizTemplates()
  } catch (e) {
    error.value = 'Could not load templates.'
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    availableCategories.value = await api.getCategories(form.language)
  } catch (e) {
    error.value = 'Could not load categories.'
  }
}

function selectLanguage(code) {
  form.language = code
  form.categorySelections = []
  loadCategories()
}

function isSelected(cat) {
  return form.categorySelections.some(s => s.category === cat)
}
function countFor(cat) {
  return form.categorySelections.find(s => s.category === cat)?.numberOfQuestions || 0
}
function toggleCategory(cat) {
  if (isSelected(cat)) {
    form.categorySelections = form.categorySelections.filter(s => s.category !== cat)
  } else {
    form.categorySelections.push({ category: cat, numberOfQuestions: 5 })
  }
}
function adjustCount(cat, delta) {
  const sel = form.categorySelections.find(s => s.category === cat)
  if (sel) sel.numberOfQuestions = Math.max(1, sel.numberOfQuestions + delta)
}

function resetForm() {
  form.title = ''
  form.language = 'EN'
  form.minDifficulty = 1
  form.maxDifficulty = 10
  form.categorySelections = []
}

function openCreate() {
  resetForm()
  editingId.value = null
  quiz.value = null
  view.value = 'settings'
  loadCategories()
}

async function openEdit(id) {
  error.value = ''
  try {
    const detail = await api.adminGetQuizTemplate(id)
    form.title = detail.title
    form.language = detail.language
    form.minDifficulty = 1
    form.maxDifficulty = 10
    form.categorySelections = []
    editingId.value = id
    quiz.value = detail
    view.value = 'review'
    loadCategories()
  } catch (e) {
    error.value = 'Could not load that template.'
  }
}

async function generateDraft() {
  error.value = ''
  generating.value = true
  try {
    quiz.value = await api.generateQuiz({
      title: form.title || 'Untitled quiz',
      language: form.language,
      minDifficulty: form.minDifficulty,
      maxDifficulty: form.maxDifficulty,
      categorySelections: form.categorySelections
    })
    view.value = 'review'
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not generate a draft.'
  } finally {
    generating.value = false
  }
}

async function saveTemplate() {
  error.value = ''
  saving.value = true
  try {
    if (editingId.value) {
      await api.adminUpdateQuizTemplate(editingId.value, quiz.value)
    } else {
      await api.adminCreateQuizTemplate(quiz.value)
    }
    toast.show('Template saved.')
    view.value = 'list'
    loadTemplates()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save the template.'
  } finally {
    saving.value = false
  }
}

function requestDelete(t) {
  pendingDelete.value = t
}

async function doDelete() {
  const t = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.adminDeleteQuizTemplate(t.id)
    toast.show('Template deleted.')
    loadTemplates()
  } catch (e) {
    error.value = 'Could not delete that template.'
  }
}
</script>
