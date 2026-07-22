<template>
  <div>
    <h1>My quizzes</h1>
    <p class="page-subtitle">Quizzes you've explicitly saved. Generating one doesn't save it automatically.</p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <!-- List view -->
    <section v-if="!openQuiz">
      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

      <div v-else-if="!quizzes.length" class="empty-state friendly">
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
          <div style="display:flex; gap:8px; flex-wrap:wrap;">
            <button class="btn btn-secondary btn-sm" @click="openOne(q.id)">View &amp; edit</button>
            <button
              class="btn btn-secondary btn-sm"
              :disabled="duplicatingId === q.id"
              @click="duplicateAndRegenerate(q)"
            >
              {{ duplicatingId === q.id ? 'Regenerating…' : 'Duplicate & regenerate' }}
            </button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(q)">Delete</button>
          </div>
        </div>
      </div>
    </section>

    <!-- Detail / edit view -->
    <section v-else>
      <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <div style="flex:1; min-width:220px;">
          <input
            type="text"
            v-model="openQuiz.title"
            @input="dirty = true"
            class="title-edit-input"
            aria-label="Quiz title"
          />
          <span style="color:var(--text-dim); font-size:0.9rem;">{{ openQuiz.questions.length }} questions</span>
        </div>
        <button class="btn btn-secondary btn-sm no-print" @click="backToList">← Back to list</button>
      </div>

      <div v-if="openQuiz.warnings && openQuiz.warnings.length" class="banner error">
        <div v-for="(w, i) in openQuiz.warnings" :key="i">{{ w }}</div>
      </div>

      <p class="page-subtitle" style="margin-top:-10px;">
        <template v-if="isDraft">This is a freshly regenerated copy - hit "Save as new" once you're happy with it.</template>
        <template v-else>Use the ↑ / ↓ buttons to reorder, or discard/replace questions - then hit "Update" to save your changes.</template>
      </p>

      <QuizReviewEditor
        :quiz="openQuiz"
        @error="error = $event"
        @changed="dirty = true"
      >
        <template #actions>
          <div style="display:flex; flex-direction:column; gap:10px; width:100%;">
            <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:400; font-size:0.9rem; color:var(--text-dim);">
              <input type="checkbox" v-model="includeAnswersInPdf" style="width:auto;" />
              Include answers in PDF
            </label>
            <div style="display:flex; gap:12px; flex-wrap:wrap;">
              <button class="btn btn-primary" :disabled="exportingPdf" @click="downloadPdf">
                {{ exportingPdf ? 'Preparing PDF…' : 'Download as PDF' }}
              </button>
              <button
                v-if="isDraft"
                class="btn"
                :class="justUpdated ? 'btn-primary' : 'btn-secondary'"
                :disabled="updating"
                @click="saveAsNew"
              >
                {{ updating ? 'Saving…' : justUpdated ? 'Saved ✓' : 'Save as new' }}
              </button>
              <button
                v-else
                class="btn"
                :class="justUpdated ? 'btn-primary' : 'btn-secondary'"
                :disabled="updating"
                @click="updateQuiz"
              >
                {{ updating ? 'Updating…' : justUpdated ? 'Updated ✓' : 'Update' }}
              </button>
            </div>
          </div>
        </template>
      </QuizReviewEditor>
    </section>

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this quiz?"
      :message="`'${pendingDelete.title}' will be permanently removed. This can't be undone.`"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />

    <ConfirmModal
      v-if="showUnsavedConfirm"
      title="Discard changes?"
      message="You have unsaved changes to this quiz. Going back will lose them unless you save first."
      confirm-text="Discard &amp; go back"
      @confirm="confirmBackToList"
      @cancel="showUnsavedConfirm = false"
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

const quizzes = ref([])
const loading = ref(true)
const error = ref('')
const openQuiz = ref(null)
const exportingPdf = ref(false)
const updating = ref(false)
const justUpdated = ref(false)
const dirty = ref(false)
const includeAnswersInPdf = ref(true)
const pendingDelete = ref(null)
const showUnsavedConfirm = ref(false)
const duplicatingId = ref(null)

// A regenerated-but-not-yet-saved copy has no id from the server yet - that's
// the signal for "Save as new" instead of "Update".
const isDraft = computed(() => !!openQuiz.value && !openQuiz.value.id)

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
    dirty.value = false
  } catch (e) {
    error.value = 'Could not load that quiz.'
  }
}

// Reuses the category/count/language shape of an already-saved quiz to pull a
// fresh set of random questions - same idea, new content. Note: the original
// difficulty range isn't stored with a saved quiz (only its language is), so
// the regenerated copy draws from the full 1-10 range rather than whatever
// narrower range was used originally.
async function duplicateAndRegenerate(summary) {
  duplicatingId.value = summary.id
  error.value = ''
  try {
    const full = await api.getSavedQuiz(summary.id)
    const counts = {}
    for (const q of full.questions) {
      counts[q.category] = (counts[q.category] || 0) + 1
    }
    const categorySelections = Object.entries(counts)
      .map(([category, numberOfQuestions]) => ({ category, numberOfQuestions }))

    const result = await api.generateQuiz({
      title: `${full.title} (copy)`,
      language: full.language,
      minDifficulty: 1,
      maxDifficulty: 10,
      categorySelections
    })
    openQuiz.value = result
    dirty.value = true
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not regenerate that quiz.'
  } finally {
    duplicatingId.value = null
  }
}

function backToList() {
  if (dirty.value) {
    showUnsavedConfirm.value = true
    return
  }
  openQuiz.value = null
}

function confirmBackToList() {
  showUnsavedConfirm.value = false
  openQuiz.value = null
  dirty.value = false
  loadList()
}

function requestDelete(q) {
  pendingDelete.value = q
}

async function doDelete() {
  const q = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.deleteSavedQuiz(q.id)
    toast.show('Quiz deleted.')
    loadList()
  } catch (e) {
    error.value = 'Could not delete that quiz.'
  }
}

async function downloadPdf() {
  exportingPdf.value = true
  error.value = ''
  try {
    const blob = await api.exportPdf(openQuiz.value, includeAnswersInPdf.value)
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

async function updateQuiz() {
  updating.value = true
  error.value = ''
  try {
    openQuiz.value = await api.updateSavedQuiz(openQuiz.value.id, openQuiz.value)
    dirty.value = false
    justUpdated.value = true
    setTimeout(() => { justUpdated.value = false }, 3000)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not update the quiz.'
  } finally {
    updating.value = false
  }
}

async function saveAsNew() {
  updating.value = true
  error.value = ''
  try {
    openQuiz.value = await api.saveQuiz(openQuiz.value)
    dirty.value = false
    justUpdated.value = true
    loadList()
    setTimeout(() => { justUpdated.value = false }, 3000)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save the quiz.'
  } finally {
    updating.value = false
  }
}
</script>
