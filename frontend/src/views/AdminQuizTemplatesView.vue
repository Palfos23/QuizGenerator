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

    <!-- Just title + language up front - question picking happens in the review step below,
         which already has both a random-batch adder and a search-and-add box built in. -->
    <template v-else-if="view === 'settings'">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
        <h1 style="margin:0;">Create template</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Back to list</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Title</label>
        <input type="text" v-model="newTitle" placeholder="e.g. Office Christmas Party Quiz" />
      </div>

      <div class="field">
        <label>Language <span class="picker-hint">choose one</span></label>
        <div class="language-row">
          <button
            v-for="lang in LANGUAGES"
            :key="lang.code"
            class="language-btn"
            :class="{ active: newLanguage === lang.code }"
            @click="newLanguage = lang.code"
          >
            <span>{{ lang.flag }}</span> {{ lang.label }}
          </button>
        </div>
      </div>

      <button class="btn btn-primary" @click="startBuilding">Start building →</button>
    </template>

    <template v-else-if="view === 'review'">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
        <input type="text" v-model="quiz.title" class="title-edit-input" aria-label="Quiz title" />
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Back to list</button>
      </div>
      <p class="page-subtitle" style="margin-top:-14px;">
        {{ languageLabel(quiz.language) }} · use the panels below to add a random batch, or search for specific questions.
      </p>

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
import { onMounted, ref } from 'vue'
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

const newTitle = ref('')
const newLanguage = ref('EN')
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

function openCreate() {
  newTitle.value = ''
  newLanguage.value = 'EN'
  editingId.value = null
  view.value = 'settings'
}

function startBuilding() {
  quiz.value = { title: newTitle.value || 'Untitled quiz', language: newLanguage.value, questions: [], warnings: [] }
  view.value = 'review'
}

async function openEdit(id) {
  error.value = ''
  try {
    quiz.value = await api.adminGetQuizTemplate(id)
    editingId.value = id
    view.value = 'review'
  } catch (e) {
    error.value = 'Could not load that template.'
  }
}

async function saveTemplate() {
  error.value = ''
  if (!quiz.value.questions.length) {
    error.value = 'Add at least one question first.'
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
