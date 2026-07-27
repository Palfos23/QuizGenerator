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

      <div v-else class="saved-quiz-list">
        <div v-for="q in questions" :key="q.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ q.title }}</div>
            <div class="saved-quiz-meta">
              {{ q.mainCategory || 'Uncategorized' }} · {{ q.safeAnswers.length }} safe · {{ q.tensionAnswers.length }} tension
            </div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(q.id)">Edit</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(q)">Delete</button>
          </div>
        </div>
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
          <label>Main category <span class="picker-hint">used to filter which questions a game draws from</span></label>
          <input type="text" v-model="form.mainCategory" placeholder="e.g. Geography" />
        </div>
        <div style="flex:1; min-width:200px;">
          <label>Answers category <span class="picker-hint">powers the answer-box autocomplete</span></label>
          <select v-model="form.answersCategory">
            <option value="" disabled>Choose a category…</option>
            <option v-for="c in tensionCategories" :key="c.name" :value="c.name">{{ c.name }}</option>
          </select>
          <p v-if="!tensionCategories.length" style="color:var(--coral); font-size:0.85rem; margin-top:6px;">
            No categories exist yet - add one on the Tension categories page first.
          </p>
        </div>
      </div>

      <div class="field">
        <label>Source <span class="picker-hint">optional - shown to players so they know where the data came from</span></label>
        <input type="text" v-model="form.source" placeholder="e.g. Lionpopulation.com" />
      </div>

      <div class="field">
        <label>Safe answers <span class="picker-hint">ranked 1-10 - higher rank scores more points</span></label>
        <p v-if="!form.answersCategory.trim()" style="color:var(--text-dim); font-size:0.85rem; margin-top:-4px;">
          Set an answers category above to choose from its option list.
        </p>
        <p v-else-if="!categoryOptions.length" style="color:var(--coral); font-size:0.85rem; margin-top:-4px;">
          No options found for "{{ form.answersCategory }}" - add some on the Tension categories page first.
        </p>
        <div v-for="(a, idx) in form.safeAnswers" :key="idx" class="candidate-row">
          <input type="number" min="1" max="10" v-model.number="a.rank" style="width:70px;" placeholder="Rank" />
          <select v-model="a.text" style="flex:1;" :disabled="!categoryOptions.length">
            <option value="" disabled>Choose an answer…</option>
            <option v-for="opt in categoryOptions" :key="opt" :value="opt">{{ opt }}</option>
          </select>
          <button class="btn btn-danger btn-sm" @click="form.safeAnswers.splice(idx, 1)">✕</button>
        </div>
        <button class="btn btn-secondary btn-sm" style="margin-top:8px;" @click="addSafeAnswer">+ Add safe answer</button>
      </div>

      <div class="field">
        <label>Tension answers <span class="picker-hint">ranked separately - any of these costs -5 if guessed</span></label>
        <div v-for="(a, idx) in form.tensionAnswers" :key="idx" class="candidate-row">
          <input type="number" min="1" v-model.number="a.rank" style="width:70px;" placeholder="Rank" />
          <select v-model="a.text" style="flex:1;" :disabled="!categoryOptions.length">
            <option value="" disabled>Choose an answer…</option>
            <option v-for="opt in categoryOptions" :key="opt" :value="opt">{{ opt }}</option>
          </select>
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
import { onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import ConfirmModal from '../components/ConfirmModal.vue'

const view = ref('list')
const questions = ref([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const editingId = ref(null)
const pendingDelete = ref(null)

const form = reactive({
  title: '', mainCategory: '', answersCategory: '', source: '', safeAnswers: [], tensionAnswers: []
})

const categoryOptions = ref([])
let categoryOptionsDebounce = null
watch(() => form.answersCategory, (val) => {
  clearTimeout(categoryOptionsDebounce)
  if (!val.trim()) {
    categoryOptions.value = []
    return
  }
  categoryOptionsDebounce = setTimeout(async () => {
    try {
      categoryOptions.value = await api.fetchTensionAnswerOptions(val.trim())
    } catch (e) {
      categoryOptions.value = []
    }
  }, 400)
})

onMounted(loadQuestions)

const tensionCategories = ref([])
onMounted(async () => {
  try {
    tensionCategories.value = await api.adminListTensionCategories()
  } catch (e) {
    // non-critical - the dropdown just stays empty, with its own message shown
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
  form.source = ''
  form.safeAnswers = []
  form.tensionAnswers = []
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
    form.source = detail.source || ''
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
