<template>
  <div>
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>Tension questions</h1>
          <p class="page-subtitle">
            Each question needs up to 10 ranked safe answers, plus any number of tension (trap) answers.
            <router-link to="/admin/tension-categories">Manage answer categories →</router-link>
          </p>
        </div>
        <button class="btn btn-primary" @click="openCreate">+ Create question</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

      <div v-else-if="!questions.length" class="empty-state friendly">
        No tension questions yet. Create your first one to be able to start a game.
      </div>

      <div v-else>
        <input
          type="text"
          v-model="questionSearchTerm"
          placeholder="Search by title or category…"
          class="search-input"
          style="margin-bottom:16px;"
        />

        <div v-if="!filteredQuestions.length" class="empty-state" style="padding:20px;">
          No questions match "{{ questionSearchTerm }}".
        </div>

        <div v-else class="saved-quiz-list">
          <div v-for="q in pagedQuestions" :key="q.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ q.title }}</div>
              <div class="saved-quiz-meta">
                {{ q.mainCategory || 'Uncategorized' }} · {{ q.safeCount }} safe · {{ q.tensionCount }} tension
                <span v-if="q.canExpire" class="tag" style="color:var(--gold); border-color:var(--gold);">Can expire</span>
              </div>
            </div>
            <div style="display:flex; gap:8px;">
              <button class="btn btn-secondary btn-sm" @click="openEdit(q.id)">Edit</button>
              <button class="btn btn-danger btn-sm" @click="requestDelete(q)">Delete</button>
            </div>
          </div>
        </div>

        <Pagination v-model:page="questionPage" :page-size="QUESTION_PAGE_SIZE" :total-items="filteredQuestions.length" />
      </div>
    </template>

    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
        <h1 style="margin:0;">{{ editingId ? 'Edit question' : 'Create question' }}</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Back to list</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Title / prompt</label>
        <input type="text" v-model="form.title" placeholder="e.g. Name a country in Europe" />
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:200px;">
          <label>Main category <span class="picker-hint">used to filter which questions a game draws from - type anything, or pick a suggestion</span></label>
          <input type="text" v-model="form.mainCategory" placeholder="e.g. Geography" list="main-category-suggestions" />
          <datalist id="main-category-suggestions">
            <option v-for="c in mainCategorySuggestions" :key="c" :value="c" />
          </datalist>
        </div>
        <div style="flex:1; min-width:240px;">
          <label>Answers source <span class="picker-hint">powers the answer-box autocomplete</span></label>
          <div style="display:flex; gap:16px; margin-bottom:8px;">
            <label style="display:flex; align-items:center; gap:6px; text-transform:none; font-weight:400;">
              <input type="radio" :value="false" v-model="form.answersFromSubjects" style="width:auto;" />
              Tension category
            </label>
            <label style="display:flex; align-items:center; gap:6px; text-transform:none; font-weight:400;">
              <input type="radio" :value="true" v-model="form.answersFromSubjects" style="width:auto;" />
              Subjects
            </label>
          </div>
          <template v-if="!form.answersFromSubjects">
            <SearchableSelect
              v-model="form.answersCategory"
              :options="tensionCategories.map(c => c.name)"
              placeholder="Search categories…"
            />
            <p v-if="!tensionCategories.length" style="color:var(--coral); font-size:0.85rem; margin-top:6px;">
              No categories exist yet - add one on the Tension categories page first.
            </p>
          </template>
          <template v-else>
            <select v-model="form.answersSport">
              <option value="">Choose a category…</option>
              <option v-for="s in gridCategories.categories.value" :key="s" :value="s">{{ s }}</option>
            </select>
            <p style="color:var(--text-dim); font-size:0.85rem; margin-top:6px;">
              Answers are drawn from Subjects in this category, e.g. "Football" players by name.
            </p>
          </template>
        </div>
      </div>

      <div class="field">
        <label>Source <span class="picker-hint">optional - shown to players so they know where the data came from</span></label>
        <input type="text" v-model="form.source" placeholder="e.g. Lionpopulation.com" />
      </div>

      <div class="field">
        <label>Tiebreaker <span class="picker-hint">optional - shown to players, describes how to resolve a tie</span></label>
        <input type="text" v-model="form.tiebreaker" placeholder="e.g. If tied, the country founded first wins" />
      </div>

      <div class="field" style="display:flex; align-items:flex-start; gap:8px;">
        <input type="checkbox" id="canExpire" v-model="form.canExpire" style="width:auto; margin-top:3px;" />
        <label for="canExpire" style="margin:0; text-transform:none; font-weight:400;">
          Can expire
          <div style="color:var(--text-dim); font-size:0.8rem; font-weight:400; margin-top:2px;">
            e.g. "current all-time top scorer" - flag this so it shows up on the "Can expire" Insights page for periodic review. Leave unchecked for stable facts.
          </div>
        </label>
      </div>

      <div class="field">
        <label>Safe answers <span class="picker-hint">ranked 1-10 - higher rank scores more points</span></label>
        <p v-if="!(form.answersFromSubjects ? form.answersSport : form.answersCategory).trim()" style="color:var(--text-dim); font-size:0.85rem; margin-top:-4px;">
          Set an answers source above to choose from its option list.
        </p>
        <p v-else-if="!categoryOptions.length" style="color:var(--coral); font-size:0.85rem; margin-top:-4px;">
          <template v-if="form.answersFromSubjects">No subjects found in "{{ form.answersSport }}" - add some on the Subjects page first.</template>
          <template v-else>No options found for "{{ form.answersCategory }}" - add some on the Tension categories page first.</template>
        </p>
        <div v-for="(a, idx) in form.safeAnswers" :key="idx" class="candidate-row">
          <input type="number" min="1" max="10" v-model.number="a.rank" style="width:70px;" placeholder="Rank" />
          <div style="flex:1;">
            <SearchableSelect v-model="a.text" :options="categoryOptions" placeholder="Search answers…" />
          </div>
          <button class="btn btn-danger btn-sm" @click="form.safeAnswers.splice(idx, 1)">✕</button>
        </div>
        <button class="btn btn-secondary btn-sm" style="margin-top:8px;" @click="addSafeAnswer">+ Add safe answer</button>
      </div>

      <div class="field">
        <label>Tension answers <span class="picker-hint">ranked separately - any of these costs -5 if guessed</span></label>
        <div v-for="(a, idx) in form.tensionAnswers" :key="idx" class="candidate-row">
          <input type="number" min="1" v-model.number="a.rank" style="width:70px;" placeholder="Rank" />
          <div style="flex:1;">
            <SearchableSelect v-model="a.text" :options="categoryOptions" placeholder="Search answers…" />
          </div>
          <button class="btn btn-danger btn-sm" @click="form.tensionAnswers.splice(idx, 1)">✕</button>
        </div>
        <button class="btn btn-secondary btn-sm" style="margin-top:8px;" @click="addTensionAnswer">+ Add tension answer</button>
      </div>

      <button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? 'Saving…' : 'Save question' }}</button>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this question?"
      :message="`'${pendingDelete.title}' will be permanently removed.`"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import ConfirmModal from '../components/ConfirmModal.vue'
import Pagination from '../components/Pagination.vue'
import SearchableSelect from '../components/SearchableSelect.vue'
import gridCategories from '../services/gridCategories'

const view = ref('list')
const questions = ref([])

const questionSearchTerm = ref('')
const filteredQuestions = computed(() => {
  const term = questionSearchTerm.value.trim().toLowerCase()
  if (!term) return questions.value
  return questions.value.filter(q =>
    q.title.toLowerCase().includes(term) ||
    (q.mainCategory || '').toLowerCase().includes(term) ||
    (q.answersCategory || '').toLowerCase().includes(term)
  )
})
const QUESTION_PAGE_SIZE = 25
const questionPage = ref(1)
const pagedQuestions = computed(() => {
  const start = (questionPage.value - 1) * QUESTION_PAGE_SIZE
  return filteredQuestions.value.slice(start, start + QUESTION_PAGE_SIZE)
})
watch(questionSearchTerm, () => { questionPage.value = 1 })
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const editingId = ref(null)
const pendingDelete = ref(null)

const form = reactive({
  title: '', mainCategory: '', answersCategory: '', answersFromSubjects: false, answersSport: '',
  source: '', tiebreaker: '', canExpire: false, safeAnswers: [], tensionAnswers: []
})

// Powers the Safe/Tension answer SearchableSelects - pulled from whichever
// answer source is currently chosen: a Tension answer category's word list,
// or Subjects (athletes) in a chosen sport/category.
const categoryOptions = ref([])
let categoryOptionsDebounce = null
watch(() => [form.answersFromSubjects, form.answersCategory, form.answersSport], () => {
  clearTimeout(categoryOptionsDebounce)
  const fromSubjects = form.answersFromSubjects
  const key = (fromSubjects ? form.answersSport : form.answersCategory).trim()
  if (!key) {
    categoryOptions.value = []
    return
  }
  categoryOptionsDebounce = setTimeout(async () => {
    try {
      categoryOptions.value = fromSubjects
        ? (await api.adminSearchAthletes({ sport: key })).map(a => a.name)
        : await api.fetchTensionAnswerOptions(key)
    } catch (e) {
      categoryOptions.value = []
    }
  }, 400)
})

onMounted(loadQuestions)
onMounted(() => gridCategories.ensureLoaded())

const tensionCategories = ref([])
onMounted(async () => {
  try {
    tensionCategories.value = await api.adminListTensionCategories()
  } catch (e) {
    // non-critical - the dropdown just stays empty, with its own message shown
  }
})

// Suggestions for the "main category" field, pooled from categories already
// used across existing Tension questions (so the 170-odd questions already
// authored keep working exactly as before) plus the normal question bank's
// categories (so new Tension questions can be tagged consistently with the
// rest of the app). It's still a plain text input underneath - these are
// suggestions via <datalist>, not a forced picker, so typing a brand new
// category works exactly like it always has.
const mainCategorySuggestions = ref([])
onMounted(async () => {
  try {
    const [tensionCats, subjectCats] = await Promise.all([
      api.fetchTensionMainCategories(),
      api.adminListQuestionCategories()
    ])
    const seen = new Set()
    const merged = []
    for (const c of [...tensionCats, ...subjectCats]) {
      const key = c.trim().toLowerCase()
      if (!key || seen.has(key)) continue
      seen.add(key)
      merged.push(c)
    }
    mainCategorySuggestions.value = merged.sort((a, b) => a.localeCompare(b))
  } catch (e) {
    // non-critical - the field still works as plain free text without suggestions
  }
})

async function loadQuestions() {
  loading.value = true
  error.value = ''
  try {
    questions.value = await api.adminListTensionQuestions()
  } catch (e) {
    error.value = 'Could not load tension questions.'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.title = ''
  form.mainCategory = ''
  form.answersCategory = ''
  form.answersFromSubjects = false
  form.answersSport = ''
  form.source = ''
  form.tiebreaker = ''
  form.canExpire = false
  // Defaults to a full 1-10 safe ranking plus 2 tension slots already laid
  // out, since almost every question ends up using all 10 anyway - saves
  // clicking "+ Add safe answer" ten times on every single new question.
  form.safeAnswers = Array.from({ length: 10 }, (_, i) => ({ rank: i + 1, text: '' }))
  form.tensionAnswers = Array.from({ length: 2 }, (_, i) => ({ rank: i + 1, text: '' }))
}

function addSafeAnswer() {
  const nextRank = form.safeAnswers.length ? Math.max(...form.safeAnswers.map(a => a.rank)) + 1 : 1
  form.safeAnswers.push({ rank: Math.min(nextRank, 10), text: '' })
}
function addTensionAnswer() {
  const nextRank = form.tensionAnswers.length ? Math.max(...form.tensionAnswers.map(a => a.rank)) + 1 : 1
  form.tensionAnswers.push({ rank: nextRank, text: '' })
}

function openCreate() {
  resetForm()
  editingId.value = null
  view.value = 'builder'
}

async function openEdit(id) {
  error.value = ''
  try {
    const detail = await api.adminGetTensionQuestion(id)
    form.title = detail.title
    form.mainCategory = detail.mainCategory || ''
    form.answersCategory = detail.answersCategory || ''
    form.answersFromSubjects = detail.answersFromSubjects || false
    form.answersSport = detail.answersSport || ''
    form.source = detail.source || ''
    form.tiebreaker = detail.tiebreaker || ''
    form.canExpire = detail.canExpire || false
    form.safeAnswers = detail.safeAnswers.map(a => ({ rank: a.rank, text: a.text }))
    form.tensionAnswers = detail.tensionAnswers.map(a => ({ rank: a.rank, text: a.text }))
    editingId.value = id
    view.value = 'builder'
  } catch (e) {
    error.value = 'Could not load that question.'
  }
}

async function save() {
  error.value = ''
  if (!form.title.trim()) {
    error.value = 'A title is required.'
    return
  }
  if (!form.safeAnswers.length || form.safeAnswers.some(a => !a.text.trim())) {
    error.value = 'Add at least one safe answer, and make sure none are blank.'
    return
  }
  if (form.tensionAnswers.some(a => !a.text.trim())) {
    error.value = 'Tension answers can\'t be blank - remove any empty rows.'
    return
  }

  saving.value = true
  try {
    if (editingId.value) {
      await api.adminUpdateTensionQuestion(editingId.value, form)
    } else {
      await api.adminCreateTensionQuestion(form)
    }
    toast.show('Question saved.')
    view.value = 'list'
    loadQuestions()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save the question.'
  } finally {
    saving.value = false
  }
}

function requestDelete(q) {
  pendingDelete.value = q
}

async function doDelete() {
  const q = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.adminDeleteTensionQuestion(q.id)
    toast.show('Question deleted.')
    loadQuestions()
  } catch (e) {
    error.value = 'Could not delete that question.'
  }
}
</script>
