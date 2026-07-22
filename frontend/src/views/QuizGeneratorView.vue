<template>
  <div>
    <h1>Create a quiz</h1>
    <p class="page-subtitle">Pick a language, a difficulty range, and how many questions you want from each category.</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="successMessage" class="banner success">{{ successMessage }}</div>
    <div v-if="quiz && quiz.warnings && quiz.warnings.length" class="banner error">
      <div v-for="(w, i) in quiz.warnings" :key="i">{{ w }}</div>
    </div>

    <!-- Step 1: settings form -->
    <section v-if="!quiz">
      <div class="field">
        <label>Quiz title</label>
        <input type="text" v-model="form.title" placeholder="e.g. Sarah's 30th Birthday Quiz" />
      </div>

      <div class="field">
        <label>Language</label>
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

      <div class="field">
        <label>Difficulty range <span style="text-transform:none;font-weight:400;">(1 = easiest, 10 = hardest)</span></label>
        <div class="difficulty-slider-group">
          <div class="difficulty-slider-row">
            <span class="slider-label">Min</span>
            <input type="range" min="1" max="10" v-model.number="form.minDifficulty" @input="clampRange('min')" />
            <output>{{ form.minDifficulty }}</output>
          </div>
          <div class="difficulty-slider-row">
            <span class="slider-label">Max</span>
            <input type="range" min="1" max="10" v-model.number="form.maxDifficulty" @input="clampRange('max')" />
            <output>{{ form.maxDifficulty }}</output>
          </div>
        </div>
      </div>

      <div class="field">
        <label>Categories &amp; how many questions from each</label>

        <div v-if="!form.categorySelections.length" class="empty-state" style="padding:20px;">
          No categories added yet - pick one below to get started.
        </div>

        <div
          v-for="(sel, idx) in form.categorySelections"
          :key="sel.category"
          class="category-row"
          :class="{ 'just-added': sel.category === justAddedCategory }"
        >
          <span class="category-name">{{ sel.category }}</span>
          <input type="number" min="1" max="50" v-model.number="sel.numberOfQuestions" />
          <span style="color:var(--text-dim); font-size:0.85rem;">questions</span>
          <button class="btn btn-danger btn-sm" @click="removeCategorySelection(idx)">✕</button>
        </div>

        <div class="category-picker-row" v-if="remainingCategories.length">
          <select v-model="categoryToAdd">
            <option disabled value="">Add a category…</option>
            <option v-for="c in remainingCategories" :key="c" :value="c">{{ c }}</option>
          </select>
          <input type="number" min="1" max="50" v-model.number="countToAdd" style="width:70px;" />
          <button class="btn btn-secondary" @click="addCategorySelection">+ Add</button>
        </div>
        <span v-if="!availableCategories.length" style="color:var(--text-dim); font-size:0.85rem;">
          No {{ languageLabel(form.language) }} categories yet - add some questions in that language in the admin page first.
        </span>
      </div>

      <button class="btn btn-primary" :disabled="generating || !form.categorySelections.length" @click="generateQuiz">
        {{ generating ? 'Generating…' : 'Generate quiz' }}
      </button>
    </section>

    <!-- Step 2: review, reorder + finalize -->
    <section v-else>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <h2 style="margin:0;">{{ quiz.title }} <span style="color:var(--text-dim); font-weight:400; font-size:1rem;">· {{ quiz.questions.length }} questions</span></h2>
        <button class="btn btn-secondary btn-sm no-print" @click="startOver">Start over</button>
      </div>
      <p class="page-subtitle" style="margin-top:-10px;">Use the ↑ / ↓ buttons to reorder questions.</p>

      <div class="card-stack">
        <QuestionCard
          v-for="(q, idx) in quiz.questions"
          :key="q.id"
          :question="q"
          :index="idx"
          editable
          :busy="busyIndex === idx"
          :is-first="idx === 0"
          :is-last="idx === quiz.questions.length - 1"
          @discard="discardAndReplace(idx)"
          @remove="removeQuestion(idx)"
          @move-up="moveQuestion(idx, -1)"
          @move-down="moveQuestion(idx, 1)"
        />
      </div>

      <div v-if="!quiz.questions.length" class="empty-state">
        You removed every question. <button class="btn btn-secondary btn-sm" @click="startOver">Start over</button>
      </div>

      <div v-else class="no-print" style="margin-top:32px; display:flex; gap:12px; flex-wrap:wrap;">
        <button class="btn btn-primary" :disabled="exportingPdf" @click="downloadPdf">
          {{ exportingPdf ? 'Preparing PDF…' : 'Download as PDF' }}
        </button>
        <button class="btn btn-secondary" :disabled="saving" @click="saveQuiz">
          {{ saving ? 'Saving…' : 'Save to My Quizzes' }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import QuestionCard from '../components/QuestionCard.vue'
import api from '../services/api'
import { LANGUAGES, languageLabel } from '../constants'

const form = reactive({
  title: '',
  language: 'EN',
  minDifficulty: 1,
  maxDifficulty: 10,
  categorySelections: [] // [{ category, numberOfQuestions }]
})

const availableCategories = ref([])
const categoryToAdd = ref('')
const countToAdd = ref(5)
const justAddedCategory = ref('')

const quiz = ref(null)
const generating = ref(false)
const error = ref('')
const successMessage = ref('')
const busyIndex = ref(-1)
const exportingPdf = ref(false)
const saving = ref(false)

const remainingCategories = computed(() =>
  availableCategories.value.filter(c => !form.categorySelections.some(sel => sel.category === c))
)

onMounted(loadCategories)

async function loadCategories() {
  try {
    availableCategories.value = await api.getCategories(form.language)
  } catch (e) {
    error.value = 'Could not load categories from the server.'
  }
}

function selectLanguage(code) {
  if (form.language === code) return
  form.language = code
  form.categorySelections = [] // category names don't necessarily carry over between languages
  loadCategories()
}

function clampRange(which) {
  if (form.minDifficulty > form.maxDifficulty) {
    if (which === 'min') form.maxDifficulty = form.minDifficulty
    else form.minDifficulty = form.maxDifficulty
  }
}

function addCategorySelection() {
  if (!categoryToAdd.value) return
  const category = categoryToAdd.value
  form.categorySelections.push({ category, numberOfQuestions: countToAdd.value || 1 })
  categoryToAdd.value = ''
  countToAdd.value = 5

  justAddedCategory.value = category
  setTimeout(() => {
    if (justAddedCategory.value === category) justAddedCategory.value = ''
  }, 1100)
}

function removeCategorySelection(idx) {
  form.categorySelections.splice(idx, 1)
}

async function generateQuiz() {
  error.value = ''
  successMessage.value = ''
  generating.value = true
  try {
    const result = await api.generateQuiz({
      title: form.title || 'My Quiz',
      language: form.language,
      minDifficulty: form.minDifficulty,
      maxDifficulty: form.maxDifficulty,
      categorySelections: form.categorySelections
    })
    quiz.value = result
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not generate a quiz. Try different filters.'
  } finally {
    generating.value = false
  }
}

async function discardAndReplace(idx) {
  error.value = ''
  busyIndex.value = idx
  try {
    const discarded = quiz.value.questions[idx]
    const replacement = await api.replaceQuestion({
      category: discarded.category,
      language: form.language,
      minDifficulty: form.minDifficulty,
      maxDifficulty: form.maxDifficulty,
      excludeIds: quiz.value.questions.map(q => q.id)
    })
    quiz.value.questions.splice(idx, 1, replacement)
  } catch (e) {
    error.value = e.response?.data?.message || 'No replacement question was available.'
  } finally {
    busyIndex.value = -1
  }
}

function removeQuestion(idx) {
  quiz.value.questions.splice(idx, 1)
}

function moveQuestion(idx, direction) {
  const target = idx + direction
  if (target < 0 || target >= quiz.value.questions.length) return
  const questions = quiz.value.questions
  const [moved] = questions.splice(idx, 1)
  questions.splice(target, 0, moved)
}

function startOver() {
  quiz.value = null
  error.value = ''
  successMessage.value = ''
}

async function downloadPdf() {
  exportingPdf.value = true
  error.value = ''
  try {
    const blob = await api.exportPdf(quiz.value, true)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${quiz.value.title.replace(/[^a-zA-Z0-9-_]/g, '_')}.pdf`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    error.value = 'Could not generate the PDF.'
  } finally {
    exportingPdf.value = false
  }
}

async function saveQuiz() {
  saving.value = true
  error.value = ''
  successMessage.value = ''
  try {
    await api.saveQuiz(quiz.value)
    successMessage.value = 'Saved - find it any time under "My Quizzes".'
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save the quiz.'
  } finally {
    saving.value = false
  }
}
</script>
