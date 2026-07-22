<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px;">
      <div>
        <h1>Question bank</h1>
        <p class="page-subtitle">Every question here is available to the quiz generator. Add, edit or remove them.</p>
      </div>
      <div style="display:flex; gap:10px;">
        <button class="btn btn-secondary" @click="triggerImport" :disabled="importing">
          {{ importing ? 'Importing…' : 'Import CSV' }}
        </button>
        <input ref="fileInput" type="file" accept=".csv,text/csv" style="display:none;" @change="onFileChosen" />
        <button class="btn btn-primary" @click="openCreate">+ Add question</button>
      </div>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="successMessage" class="banner success">{{ successMessage }}</div>

    <div v-if="importResult" class="banner" :class="importResult.errors.length ? 'error' : 'success'">
      <div>Imported {{ importResult.imported }} question(s){{ importResult.skipped ? `, skipped ${importResult.skipped}` : '' }}.</div>
      <ul v-if="importResult.errors.length" style="margin:8px 0 0; padding-left:18px;">
        <li v-for="(e, i) in importResult.errors" :key="i">{{ e }}</li>
      </ul>
      <button class="btn btn-secondary btn-sm" style="margin-top:10px;" @click="importResult = null">Dismiss</button>
    </div>

    <!-- Bank overview / coverage -->
    <div class="stats-panel">
      <button class="btn btn-secondary btn-sm" @click="showStats = !showStats">
        {{ showStats ? 'Hide' : 'Show' }} bank overview
      </button>
      <div v-if="showStats" class="table-scroll" style="margin-top:14px;">
        <table class="table">
          <thead>
            <tr>
              <th>Category</th>
              <th>Language</th>
              <th>Questions</th>
              <th>Difficulty range</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in stats" :key="s.category + s.language">
              <td>{{ s.category }}</td>
              <td>{{ flagFor(s.language) }} {{ s.language }}</td>
              <td>
                <span :class="{ 'stat-low': s.count < 3 }">{{ s.count }}</span>
                <span v-if="s.count < 3" class="tag changeable" style="margin-left:8px;">Low coverage</span>
              </td>
              <td>{{ s.minDifficulty }}-{{ s.maxDifficulty }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="filter-bar">
      <div class="field" style="margin-bottom:0; flex:2; min-width:200px;">
        <label>Search</label>
        <input type="text" v-model="searchText" placeholder="Search question, category or answer…" />
      </div>
      <div class="field" style="margin-bottom:0; flex:1; min-width:160px;">
        <label>Language</label>
        <select v-model="languageFilter">
          <option value="ALL">All languages</option>
          <option v-for="lang in LANGUAGES" :key="lang.code" :value="lang.code">{{ lang.flag }} {{ lang.label }}</option>
        </select>
      </div>
      <div class="field" style="margin-bottom:0; flex:1; min-width:160px;">
        <label>Could change?</label>
        <select v-model="couldChangeFilter">
          <option value="ALL">Any</option>
          <option value="YES">May change</option>
          <option value="NO">Stable</option>
        </select>
      </div>
    </div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="!questions.length" class="empty-state">
      <p style="margin-top:0;">No questions in the bank yet.</p>
      <button class="btn btn-primary" :disabled="seedingStarterPack" @click="addStarterPack">
        {{ seedingStarterPack ? 'Adding…' : 'Add a starter pack of sample questions' }}
      </button>
    </div>

    <div v-else-if="!filteredQuestions.length" class="empty-state">
      No questions match those filters.
    </div>

    <template v-else>
      <div class="table-scroll">
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
            <tr v-for="q in pagedQuestions" :key="q.id">
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
                <button class="btn btn-danger btn-sm" style="margin-left:6px;" @click="requestDelete(q)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-bar" v-if="totalPages > 1">
        <button class="btn btn-secondary btn-sm" :disabled="page === 1" @click="page--">← Prev</button>
        <span>Page {{ page }} of {{ totalPages }} · {{ filteredQuestions.length }} questions</span>
        <button class="btn btn-secondary btn-sm" :disabled="page === totalPages" @click="page++">Next →</button>
      </div>
      <p v-else style="color:var(--text-dim); font-size:0.85rem;">{{ filteredQuestions.length }} question(s)</p>
    </template>

    <QuestionFormModal
      v-if="showModal"
      :question="editingQuestion"
      @close="showModal = false"
      @saved="onSaved"
    />

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this question?"
      :message="`'${pendingDelete.questionText}' will be permanently removed from the bank.`"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import QuestionFormModal from '../components/QuestionFormModal.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { LANGUAGES, difficultyColor } from '../constants'

const PAGE_SIZE = 15

const questions = ref([])
const loading = ref(true)
const error = ref('')
const successMessage = ref('')

const searchText = ref('')
const languageFilter = ref('ALL')
const couldChangeFilter = ref('ALL')
const page = ref(1)

const showModal = ref(false)
const editingQuestion = ref(null)
const pendingDelete = ref(null)
const seedingStarterPack = ref(false)

const fileInput = ref(null)
const importing = ref(false)
const importResult = ref(null)

const showStats = ref(false)
const stats = ref([])

const filteredQuestions = computed(() => {
  const term = searchText.value.trim().toLowerCase()
  return questions.value.filter(q => {
    if (languageFilter.value !== 'ALL' && q.language !== languageFilter.value) return false
    if (couldChangeFilter.value === 'YES' && !q.couldChange) return false
    if (couldChangeFilter.value === 'NO' && q.couldChange) return false
    if (term) {
      const haystack = `${q.questionText} ${q.category} ${q.answer}`.toLowerCase()
      if (!haystack.includes(term)) return false
    }
    return true
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredQuestions.value.length / PAGE_SIZE)))

const pagedQuestions = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return filteredQuestions.value.slice(start, start + PAGE_SIZE)
})

// jump back to a valid page whenever the filtered set shrinks (new filter, deletion, etc.)
watch([searchText, languageFilter, couldChangeFilter], () => { page.value = 1 })
watch(totalPages, (newTotal) => { if (page.value > newTotal) page.value = newTotal })

onMounted(() => {
  loadQuestions()
  loadStats()
})

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

async function loadStats() {
  try {
    stats.value = await api.adminGetStats()
  } catch (e) {
    // non-critical - the overview panel just stays empty
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
  loadStats()
}

function requestDelete(q) {
  pendingDelete.value = q
}

async function doDelete() {
  const q = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.adminDeleteQuestion(q.id)
    successMessage.value = 'Question deleted.'
    loadQuestions()
    loadStats()
  } catch (e) {
    error.value = 'Could not delete the question.'
  }
}

async function addStarterPack() {
  seedingStarterPack.value = true
  error.value = ''
  try {
    const result = await api.adminAddStarterPack()
    successMessage.value = `Added ${result.added} sample questions across all five languages.`
    loadQuestions()
    loadStats()
  } catch (e) {
    error.value = 'Could not add the starter pack.'
  } finally {
    seedingStarterPack.value = false
  }
}

function triggerImport() {
  fileInput.value?.click()
}

async function onFileChosen(event) {
  const file = event.target.files?.[0]
  event.target.value = '' // allow re-choosing the same file later
  if (!file) return

  importing.value = true
  error.value = ''
  importResult.value = null
  try {
    importResult.value = await api.adminImportCsv(file)
    loadQuestions()
    loadStats()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not import that file.'
  } finally {
    importing.value = false
  }
}
</script>
