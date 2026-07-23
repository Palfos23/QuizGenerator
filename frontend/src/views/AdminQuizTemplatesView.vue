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

    <!-- Pick questions from the existing bank -->
    <template v-else-if="view === 'picker'">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
        <h1 style="margin:0;">{{ editingId ? 'Edit template' : 'Create template' }}</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Back to list</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Title</label>
        <input type="text" v-model="quiz.title" placeholder="e.g. Office Christmas Party Quiz" />
      </div>

      <div class="field">
        <label>Language <span class="picker-hint">choose one - changing this clears any questions already picked</span></label>
        <div class="language-row">
          <button
            v-for="lang in LANGUAGES"
            :key="lang.code"
            class="language-btn"
            :class="{ active: quiz.language === lang.code }"
            @click="selectLanguage(lang.code)"
          >
            <span>{{ lang.flag }}</span> {{ lang.label }}
          </button>
        </div>
      </div>

      <div class="filter-bar">
        <div class="field" style="margin-bottom:0; flex:2; min-width:200px;">
          <label>Search</label>
          <input type="text" v-model="searchText" placeholder="Search question, category or answer…" />
        </div>
        <div class="field" style="margin-bottom:0; flex:1; min-width:160px;">
          <label>Category</label>
          <select v-model="categoryFilter">
            <option value="ALL">All categories</option>
            <option v-for="cat in availableCategories" :key="cat" :value="cat">{{ cat }}</option>
          </select>
        </div>
      </div>

      <p style="font-weight:600;">{{ quiz.questions.length }} question(s) picked</p>

      <div v-if="!filteredBank.length" class="empty-state">
        No {{ languageLabel(quiz.language) }} questions match those filters.
      </div>

      <div v-else class="table-scroll" style="max-height:480px; overflow-y:auto;">
        <table class="table">
          <thead>
            <tr>
              <th>Question</th>
              <th>Category</th>
              <th>Difficulty</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="q in filteredBank" :key="q.id">
              <td style="max-width:340px;">{{ q.questionText }}</td>
              <td>{{ q.category }}</td>
              <td>{{ q.difficultyLevel }}/10</td>
              <td style="white-space:nowrap;">
                <button
                  class="btn btn-sm"
                  :class="isPicked(q.id) ? 'btn-primary' : 'btn-secondary'"
                  @click="togglePick(q)"
                >{{ isPicked(q.id) ? 'Added ✓' : '+ Add' }}</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <button class="btn btn-primary" style="margin-top:20px;" :disabled="!quiz.questions.length" @click="view = 'review'">
        Continue to review ({{ quiz.questions.length }} picked)
      </button>
    </template>

    <template v-else-if="view === 'review'">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
        <input type="text" v-model="quiz.title" class="title-edit-input" aria-label="Quiz title" />
        <button class="btn btn-secondary btn-sm" @click="view = 'picker'">← Pick more questions</button>
      </div>

      <QuizReviewEditor :quiz="quiz" @error="error = $event">
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
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import QuizReviewEditor from '../components/QuizReviewEditor.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { LANGUAGES, languageLabel } from '../constants'

const view = ref('list')
const templates = ref([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const editingId = ref(null)
const pendingDelete = ref(null)

const bank = ref([])
const availableCategories = ref([])
const searchText = ref('')
const categoryFilter = ref('ALL')
const quiz = ref(null)

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

async function loadBank() {
  try {
    bank.value = await api.adminListQuestions()
    availableCategories.value = [...new Set(
      bank.value.filter(q => q.language === quiz.value.language).map(q => q.category)
    )].sort()
  } catch (e) {
    error.value = 'Could not load the question bank.'
  }
}

const filteredBank = computed(() => {
  const term = searchText.value.trim().toLowerCase()
  return bank.value.filter(q => {
    if (q.language !== quiz.value.language) return false
    if (categoryFilter.value !== 'ALL' && q.category !== categoryFilter.value) return false
    if (term && !`${q.questionText} ${q.category} ${q.answer}`.toLowerCase().includes(term)) return false
    return true
  })
})

function isPicked(id) {
  return quiz.value.questions.some(q => q.id === id)
}

function togglePick(q) {
  if (isPicked(q.id)) {
    quiz.value.questions = quiz.value.questions.filter(picked => picked.id !== q.id)
  } else {
    quiz.value.questions.push({
      id: q.id, questionText: q.questionText, category: q.category,
      difficultyLevel: q.difficultyLevel, answer: q.answer
    })
  }
}

function selectLanguage(code) {
  quiz.value.language = code
  quiz.value.questions = []
  categoryFilter.value = 'ALL'
  loadBank()
}

function openCreate() {
  quiz.value = { title: '', language: 'EN', questions: [], warnings: [] }
  editingId.value = null
  searchText.value = ''
  categoryFilter.value = 'ALL'
  view.value = 'picker'
  loadBank()
}

async function openEdit(id) {
  error.value = ''
  try {
    quiz.value = await api.adminGetQuizTemplate(id)
    editingId.value = id
    view.value = 'review'
    loadBank()
  } catch (e) {
    error.value = 'Could not load that template.'
  }
}

async function saveTemplate() {
  error.value = ''
  if (!quiz.value.questions.length) {
    error.value = 'Pick at least one question first.'
    return
  }
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
