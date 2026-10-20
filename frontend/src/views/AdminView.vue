<template>
  <div>
    <header class="admin-head">
      <div>
        <span class="dq-eyebrow">Spørsmål</span>
        <h1 class="dq-title">Spørsmålsbank</h1>
        <p class="dq-sub">
          Alle spørsmål her er tilgjengelige for quizgeneratoren.
          <router-link to="/admin/question-submissions">Gå gjennom innsendte spørsmål →</router-link> ·
          <router-link to="/admin/quiz-templates">Administrer quizmaler →</router-link>
        </p>
      </div>
      <div class="admin-head-actions">
        <button class="btn btn-secondary" @click="triggerImport" :disabled="importing">
          {{ importing ? 'Importerer…' : 'Importer CSV' }}
        </button>
        <input ref="fileInput" type="file" accept=".csv,text/csv" style="display:none;" @change="onFileChosen" />
        <button class="btn btn-primary" @click="openCreate">+ Legg til spørsmål</button>
      </div>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="importResult" class="banner" :class="importResult.errors.length ? 'error' : 'success'">
      <div>Importerte {{ importResult.imported }} spørsmål{{ importResult.skipped ? `, hoppet over ${importResult.skipped}` : '' }}.</div>
      <ul v-if="importResult.errors.length" style="margin:8px 0 0; padding-left:18px;">
        <li v-for="(e, i) in importResult.errors" :key="i">{{ e }}</li>
      </ul>
      <button class="btn btn-secondary btn-sm" style="margin-top:10px;" @click="importResult = null">Lukk</button>
    </div>

    <!-- Bank overview / coverage -->
    <div class="stats-panel">
      <button class="btn btn-secondary btn-sm" @click="showStats = !showStats">
        {{ showStats ? 'Skjul' : 'Vis' }} bankoversikt
      </button>
      <div v-if="showStats" class="table-scroll" style="margin-top:14px;">
        <table class="table">
          <thead>
            <tr>
              <th>Kategori</th>
              <th>Språk</th>
              <th>Spørsmål</th>
              <th>Vanskelighetsgrad</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in stats" :key="s.category + s.language">
              <td>{{ s.category }}</td>
              <td>{{ flagFor(s.language) }} {{ s.language }}</td>
              <td>
                <span :class="{ 'stat-low': s.count < 3 }">{{ s.count }}</span>
                <span v-if="s.count < 3" class="tag changeable" style="margin-left:8px;">Lav dekning</span>
              </td>
              <td>{{ s.minDifficulty }}-{{ s.maxDifficulty }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="filter-bar">
      <div class="field" style="margin-bottom:0; flex:2; min-width:200px;">
        <label>Søk<input type="text" v-model="searchText" placeholder="Søk etter spørsmål, kategori eller svar…" /></label>
      </div>
      <div class="field" style="margin-bottom:0; flex:1; min-width:160px;">
        <label>Språk
          <select v-model="languageFilter">
            <option value="ALL">Alle språk</option>
            <option v-for="lang in LANGUAGES" :key="lang.code" :value="lang.code">{{ lang.flag }} {{ lang.label }}</option>
          </select>
        </label>
      </div>
      <div class="field" style="margin-bottom:0; flex:1; min-width:160px;">
        <label>Kategori
          <select v-model="categoryFilter">
            <option value="ALL">Alle kategorier</option>
            <option v-for="c in availableCategories" :key="c" :value="c">{{ c }}</option>
          </select>
        </label>
      </div>
      <div class="field" style="margin-bottom:0; flex:1; min-width:160px;">
        <label>Kan endre seg?
          <select v-model="couldChangeFilter">
            <option value="ALL">Alle</option>
            <option value="YES">Kan endre seg</option>
            <option value="NO">Stabil</option>
          </select>
        </label>
      </div>
    </div>

    <div v-if="loading" style="color:var(--text-dim);">Laster…</div>

    <div v-else-if="!questions.length" class="empty-state friendly">
      <p style="margin-top:0;">Ingen spørsmål i banken ennå.</p>
      <button class="btn btn-primary" :disabled="seedingStarterPack" @click="addStarterPack">
        {{ seedingStarterPack ? 'Legger til…' : 'Legg til en startpakke med eksempelspørsmål' }}
      </button>
    </div>

    <div v-else-if="!filteredQuestions.length" class="empty-state">
      Ingen spørsmål samsvarer med disse filtrene.
    </div>

    <template v-else>
      <div class="table-scroll">
        <table class="table">
          <thead>
            <tr>
              <th class="sortable" @click="toggleSort('questionText')">Spørsmål{{ sortArrow('questionText') }}</th>
              <th class="sortable" @click="toggleSort('category')">Kategori{{ sortArrow('category') }}</th>
              <th>Språk</th>
              <th class="sortable" @click="toggleSort('difficultyLevel')">Vanskelighetsgrad{{ sortArrow('difficultyLevel') }}</th>
              <th>Svar</th>
              <th class="sortable" @click="toggleSort('couldChange')">Kan endre seg?{{ sortArrow('couldChange') }}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="q in pagedQuestions" :key="q.id">
              <td style="max-width:280px;" v-html="highlight(q.questionText)"></td>
              <td v-html="highlight(q.category)"></td>
              <td>{{ flagFor(q.language) }}</td>
              <td><span class="tag difficulty-tag" :style="{ background: difficultyColor(q.difficultyLevel), color: difficultyTextColor(q.difficultyLevel) }">{{ q.difficultyLevel }}/10</span></td>
              <td v-html="highlight(q.answer)"></td>
              <td>
                <span v-if="q.couldChange" class="tag changeable">Kan endre seg</span>
                <span v-else style="color:var(--text-dim); font-size:0.85rem;">Stabil</span>
              </td>
              <td style="white-space:nowrap;">
                <button class="btn btn-secondary btn-sm" @click="openEdit(q)">Rediger</button>
                <button class="btn btn-danger btn-sm" style="margin-left:6px;" @click="requestDelete(q)">Slett</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-bar" v-if="totalPages > 1">
        <button class="btn btn-secondary btn-sm" :disabled="page === 1" @click="page--">← Forrige</button>
        <span>Side {{ page }} av {{ totalPages }} · {{ filteredQuestions.length }} spørsmål</span>
        <button class="btn btn-secondary btn-sm" :disabled="page === totalPages" @click="page++">Neste →</button>
      </div>
      <p v-else style="color:var(--text-dim); font-size:0.85rem;">{{ filteredQuestions.length }} spørsmål</p>
    </template>

    <QuestionFormModal
      v-if="showModal"
      :question="editingQuestion"
      @close="showModal = false"
      @saved="onSaved"
    />

    <ConfirmModal
      v-if="pendingDelete"
      title="Slette dette spørsmålet?"
      :message="`«${pendingDelete.questionText}» fjernes fra banken for godt.`"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import QuestionFormModal from '../components/QuestionFormModal.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { LANGUAGES, difficultyColor, difficultyTextColor } from '../constants'

const PAGE_SIZE = 15

const questions = ref([])
const loading = ref(true)
const error = ref('')

const searchText = ref('')
const languageFilter = ref('ALL')
const categoryFilter = ref('ALL')
const couldChangeFilter = ref('ALL')
const page = ref(1)

const sortColumn = ref('category')
const sortDirection = ref('asc') // 'asc' | 'desc'

const showModal = ref(false)
const editingQuestion = ref(null)
const pendingDelete = ref(null)
const seedingStarterPack = ref(false)

const fileInput = ref(null)
const importing = ref(false)
const importResult = ref(null)

const showStats = ref(false)
const stats = ref([])

// Category is free text, not a managed lookup table, so the same category can
// carry different casing/whitespace across questions (e.g. "Geography" vs
// "GEOGRAPHY ") - same reasoning as QuestionService's server-side dedupe for
// the picker in QuestionFormModal. Options here are grouped the same way -
// case/whitespace-insensitively, keeping whichever exact spelling the most
// matching questions use - so the dropdown doesn't show duplicates, and only
// lists categories that actually exist for the currently-selected language.
const availableCategories = computed(() => {
  const pool = languageFilter.value === 'ALL'
    ? questions.value
    : questions.value.filter(q => q.language === languageFilter.value)

  const variantsByKey = new Map() // lowercase-trimmed -> Map(exact spelling -> count)
  for (const q of pool) {
    const raw = (q.category || '').trim()
    if (!raw) continue
    const key = raw.toLowerCase()
    const variants = variantsByKey.get(key) || new Map()
    variants.set(raw, (variants.get(raw) || 0) + 1)
    variantsByKey.set(key, variants)
  }

  const result = []
  for (const variants of variantsByKey.values()) {
    let best = null
    for (const [spelling, count] of variants) {
      if (!best || count > best.count || (count === best.count && spelling < best.spelling)) {
        best = { spelling, count }
      }
    }
    result.push(best.spelling)
  }
  return result.sort((a, b) => a.localeCompare(b))
})

// If the language filter narrows the list and the chosen category no longer
// exists under it, fall back to "All categories" instead of leaving the
// select pointed at a value with no matching <option>.
watch(availableCategories, (cats) => {
  if (categoryFilter.value !== 'ALL' && !cats.includes(categoryFilter.value)) {
    categoryFilter.value = 'ALL'
  }
})

const filteredQuestions = computed(() => {
  const term = searchText.value.trim().toLowerCase()
  const category = categoryFilter.value.trim().toLowerCase()
  return questions.value.filter(q => {
    if (languageFilter.value !== 'ALL' && q.language !== languageFilter.value) return false
    if (categoryFilter.value !== 'ALL' && (q.category || '').trim().toLowerCase() !== category) return false
    if (couldChangeFilter.value === 'YES' && !q.couldChange) return false
    if (couldChangeFilter.value === 'NO' && q.couldChange) return false
    if (term) {
      const haystack = `${q.questionText} ${q.category} ${q.answer}`.toLowerCase()
      if (!haystack.includes(term)) return false
    }
    return true
  })
})

const sortedQuestions = computed(() => {
  const col = sortColumn.value
  const dir = sortDirection.value === 'asc' ? 1 : -1
  return [...filteredQuestions.value].sort((a, b) => {
    let av = a[col]
    let bv = b[col]
    if (typeof av === 'string') av = av.toLowerCase()
    if (typeof bv === 'string') bv = bv.toLowerCase()
    if (av < bv) return -1 * dir
    if (av > bv) return 1 * dir
    return 0
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(sortedQuestions.value.length / PAGE_SIZE)))

const pagedQuestions = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return sortedQuestions.value.slice(start, start + PAGE_SIZE)
})

// jump back to a valid page whenever the filtered set shrinks (new filter, deletion, etc.)
watch([searchText, languageFilter, categoryFilter, couldChangeFilter], () => { page.value = 1 })
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
    error.value = 'Kunne ikke laste spørsmål fra serveren.'
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

function toggleSort(col) {
  if (sortColumn.value === col) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortColumn.value = col
    sortDirection.value = 'asc'
  }
}

function sortArrow(col) {
  if (sortColumn.value !== col) return ''
  return sortDirection.value === 'asc' ? ' ▲' : ' ▼'
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
}

// Wraps the current search term in <mark> so matches are easy to spot at a glance.
// Escapes the surrounding text first since question/category/answer content is
// admin-authored free text, not something we want rendered as raw HTML.
function highlight(text) {
  const escaped = escapeHtml(text)
  const term = searchText.value.trim()
  if (!term) return escaped
  const escapedTerm = escapeHtml(term).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escaped.replace(new RegExp(`(${escapedTerm})`, 'ig'), '<mark>$1</mark>')
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
  toast.show('Spørsmålet er lagret.')
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
    toast.show('Spørsmålet er slettet.')
    loadQuestions()
    loadStats()
  } catch (e) {
    error.value = 'Kunne ikke slette spørsmålet.'
  }
}

async function addStarterPack() {
  seedingStarterPack.value = true
  error.value = ''
  try {
    const result = await api.adminAddStarterPack()
    toast.show(`La til ${result.added} eksempelspørsmål på alle fem språk.`)
    loadQuestions()
    loadStats()
  } catch (e) {
    error.value = 'Kunne ikke legge til startpakken.'
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
    error.value = e.response?.data?.message || 'Kunne ikke importere filen.'
  } finally {
    importing.value = false
  }
}
</script>
