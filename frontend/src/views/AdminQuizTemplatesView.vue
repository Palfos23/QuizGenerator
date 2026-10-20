<template>
  <div>
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>Quizmaler</h1>
          <p class="page-subtitle">Publiserte maler brukere kan kopiere inn i sine egne Mine quizer.</p>
        </div>
        <button class="btn btn-primary" @click="openCreate">+ Lag mal</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">Laster…</div>

      <div v-else-if="!templates.length" class="empty-state friendly">
        Ingen maler ennå. Lag en under - den blir tilgjengelig for alle brukere å kopiere inn i sine egne quizer.
      </div>

      <div v-else class="saved-quiz-list">
        <div v-for="t in templates" :key="t.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ t.title }}</div>
            <div class="saved-quiz-meta">{{ languageLabel(t.language) }} · {{ t.questionCount }} spørsmål</div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(t.id)">Rediger</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(t)">Slett</button>
          </div>
        </div>
      </div>
    </template>

    <!-- Just title + language up front - question picking happens in the review step below,
         which already has both a random-batch adder and a search-and-add box built in. -->
    <template v-else-if="view === 'settings'">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
        <h1 style="margin:0;">Lag mal</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Tilbake til listen</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Tittel<input type="text" v-model="newTitle" placeholder="f.eks. Quiz til julebordet på kontoret" /></label>
      </div>

      <div class="field">
        <label>Språk <span class="picker-hint">velg en</span></label>
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

      <button class="btn btn-primary" @click="startBuilding">Begynn å bygge →</button>
    </template>

    <template v-else-if="view === 'review'">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
        <input type="text" v-model="quiz.title" class="title-edit-input" aria-label="Quiztittel" />
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Tilbake til listen</button>
      </div>
      <p class="page-subtitle" style="margin-top:-14px;">
        {{ languageLabel(quiz.language) }} · bruk panelene under for å legge til en tilfeldig gruppe, eller søk etter bestemte spørsmål.
      </p>

      <QuizReviewEditor :quiz="quiz" @error="error = $event">
        <template #actions>
          <button class="btn btn-primary" :disabled="saving" @click="saveTemplate">
            {{ saving ? 'Lagrer…' : 'Lagre mal' }}
          </button>
        </template>
      </QuizReviewEditor>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      title="Slette denne malen?"
      :message="`«${pendingDelete.title}» blir ikke lenger tilgjengelig for kopiering. Kopier brukere allerede har laget påvirkes ikke.`"
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
    error.value = 'Kunne ikke laste maler.'
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
  quiz.value = { title: newTitle.value || 'Quiz uten tittel', language: newLanguage.value, questions: [], warnings: [] }
  view.value = 'review'
}

async function openEdit(id) {
  error.value = ''
  try {
    quiz.value = await api.adminGetQuizTemplate(id)
    editingId.value = id
    view.value = 'review'
  } catch (e) {
    error.value = 'Kunne ikke laste malen.'
  }
}

async function saveTemplate() {
  error.value = ''
  if (!quiz.value.questions.length) {
    error.value = 'Legg først til minst ett spørsmål.'
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.adminUpdateQuizTemplate(editingId.value, quiz.value)
    } else {
      await api.adminCreateQuizTemplate(quiz.value)
    }
    toast.show('Malen er lagret.')
    view.value = 'list'
    loadTemplates()
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke lagre malen.'
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
    toast.show('Malen er slettet.')
    loadTemplates()
  } catch (e) {
    error.value = 'Kunne ikke slette malen.'
  }
}
</script>
