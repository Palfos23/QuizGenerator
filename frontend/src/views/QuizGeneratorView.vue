<template>
  <div>
    <div class="step-indicator">{{ quiz ? 'Step 2 of 2 - Review & finalize' : 'Step 1 of 2 - Configure' }}</div>
    <h1>Create a quiz</h1>
    <p class="page-subtitle">
      Pick a language, a difficulty range, and how many questions you want from each category.
      Don't see a question you'd like included? <router-link to="/suggest-question">Suggest one →</router-link>
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>
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

      <div class="field">
        <label>Difficulty range <span style="text-transform:none;font-weight:400;">(1 = easiest, 10 = hardest)</span></label>
        <div class="difficulty-slider-group">
          <div class="difficulty-slider-row">
            <span class="slider-label">Min</span>
            <input
              type="range" min="1" max="10" v-model.number="form.minDifficulty"
              @input="clampRange('min')" aria-label="Minimum difficulty"
            />
            <output>{{ form.minDifficulty }} <span class="difficulty-word">{{ difficultyWord(form.minDifficulty) }}</span></output>
          </div>
          <div class="difficulty-slider-row">
            <span class="slider-label">Max</span>
            <input
              type="range" min="1" max="10" v-model.number="form.maxDifficulty"
              @input="clampRange('max')" aria-label="Maximum difficulty"
            />
            <output>{{ form.maxDifficulty }} <span class="difficulty-word">{{ difficultyWord(form.maxDifficulty) }}</span></output>
          </div>
        </div>
      </div>

      <div class="field">
        <label>Categories &amp; how many questions from each <span class="picker-hint">pick as many as you like</span></label>
        <p style="color:var(--text-dim); font-size:0.85rem; margin:-4px 0 12px;">Tap a category to add it, then use +/- to set the count.</p>

        <div v-if="!availableCategories.length" class="empty-state friendly" style="padding:20px;">
          No {{ languageLabel(form.language) }} categories yet - add some questions in that language in the admin page first.
        </div>

        <div v-else class="category-chip-grid">
          <div
            v-for="cat in availableCategories"
            :key="cat"
            class="category-chip"
            :class="{ selected: isSelected(cat), 'just-added': cat === justAddedCategory }"
          >
            <button v-if="!isSelected(cat)" class="chip-add-btn" @click="toggleCategory(cat)">
              + {{ cat }}
            </button>
            <template v-else>
              <span class="category-chip-name">{{ cat }}</span>
              <div class="stepper">
                <button aria-label="Decrease question count" @click="stepCount(cat, -1)">−</button>
                <span>{{ countFor(cat) }}</span>
                <button aria-label="Increase question count" @click="stepCount(cat, 1)">+</button>
              </div>
              <button class="chip-remove-btn" aria-label="Remove category" @click="toggleCategory(cat)">✕</button>
            </template>
          </div>
        </div>
      </div>

      <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:400; margin-bottom:16px;">
        <input type="checkbox" v-model="form.includeMySubmissions" style="width:auto;" />
        Include my own submitted questions <span class="picker-hint">even ones still pending or rejected - only you can draw from these</span>
      </label>

      <button class="btn btn-primary" :disabled="generating || !form.categorySelections.length" @click="generateQuiz">
        {{ generating ? 'Generating…' : 'Generate quiz' }}
      </button>
    </section>

    <!-- Step 2: review, reorder + finalize -->
    <section v-else>
      <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <div style="flex:1; min-width:220px;">
          <input type="text" v-model="quiz.title" @input="reviewDirty = true" class="title-edit-input" aria-label="Quiz title" />
          <span style="color:var(--text-dim); font-size:0.9rem;">{{ quiz.questions.length }} questions</span>
        </div>
        <button class="btn btn-secondary btn-sm no-print" @click="requestStartOver">Start over</button>
      </div>
      <p class="page-subtitle" style="margin-top:-10px;">Use the ↑ / ↓ buttons to reorder questions.</p>

      <QuizReviewEditor
        :quiz="quiz"
        :min-difficulty="form.minDifficulty"
        :max-difficulty="form.maxDifficulty"
        @error="error = $event"
        @changed="reviewDirty = true"
      >
        <template #empty>
          You removed every question. <button class="btn btn-secondary btn-sm" @click="requestStartOver">Start over</button>
        </template>
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
                class="btn"
                :class="justSaved ? 'btn-primary' : 'btn-secondary'"
                :disabled="saving"
                @click="saveQuiz"
              >
                {{ saving ? 'Saving…' : justSaved ? 'Saved to My Quizzes ✓' : 'Save to My Quizzes' }}
              </button>
            </div>
          </div>
        </template>
      </QuizReviewEditor>
    </section>

    <ConfirmModal
      v-if="showStartOverConfirm"
      title="Discard changes?"
      message="You've discarded, replaced or reordered questions in this quiz. Starting over will lose those changes unless you've already saved this quiz."
      confirm-text="Start over"
      @confirm="confirmStartOver"
      @cancel="showStartOverConfirm = false"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import QuizReviewEditor from '../components/QuizReviewEditor.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import api from '../services/api'
import { LANGUAGES, languageLabel } from '../constants'

const DRAFT_KEY = 'quiz_draft_form'

const form = reactive(loadDraft() || {
  title: '',
  language: 'EN',
  minDifficulty: 1,
  maxDifficulty: 10,
  categorySelections: [], // [{ category, numberOfQuestions }]
  includeMySubmissions: false
})

const availableCategories = ref([])
const justAddedCategory = ref('')

const quiz = ref(null)
const generating = ref(false)
const error = ref('')
const exportingPdf = ref(false)
const saving = ref(false)
const justSaved = ref(false)
const includeAnswersInPdf = ref(true)
const reviewDirty = ref(false)
const showStartOverConfirm = ref(false)

function loadDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

watch(form, (value) => {
  try {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(value))
  } catch (e) {
    // storage full or unavailable - not worth surfacing an error for a convenience feature
  }
}, { deep: true })

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

function difficultyWord(value) {
  if (value <= 3) return 'Easy'
  if (value <= 7) return 'Medium'
  return 'Hard'
}

function isSelected(cat) {
  return form.categorySelections.some(s => s.category === cat)
}

function countFor(cat) {
  return form.categorySelections.find(s => s.category === cat)?.numberOfQuestions ?? 0
}

function toggleCategory(cat) {
  const idx = form.categorySelections.findIndex(s => s.category === cat)
  if (idx !== -1) {
    form.categorySelections.splice(idx, 1)
    return
  }
  form.categorySelections.push({ category: cat, numberOfQuestions: 5 })
  justAddedCategory.value = cat
  setTimeout(() => {
    if (justAddedCategory.value === cat) justAddedCategory.value = ''
  }, 1100)
}

function stepCount(cat, delta) {
  const sel = form.categorySelections.find(s => s.category === cat)
  if (!sel) return
  sel.numberOfQuestions = Math.min(50, Math.max(1, sel.numberOfQuestions + delta))
}

async function generateQuiz() {
  error.value = ''
  generating.value = true
  try {
    const result = await api.generateQuiz({
      title: form.title || 'My Quiz',
      language: form.language,
      minDifficulty: form.minDifficulty,
      maxDifficulty: form.maxDifficulty,
      categorySelections: form.categorySelections,
      includeMySubmissions: form.includeMySubmissions
    })
    quiz.value = result
    reviewDirty.value = false
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not generate a quiz. Try different filters.'
  } finally {
    generating.value = false
  }
}

function requestStartOver() {
  if (reviewDirty.value) {
    showStartOverConfirm.value = true
  } else {
    doStartOver()
  }
}

function confirmStartOver() {
  showStartOverConfirm.value = false
  doStartOver()
}

function doStartOver() {
  quiz.value = null
  error.value = ''
  reviewDirty.value = false
}

async function downloadPdf() {
  exportingPdf.value = true
  error.value = ''
  try {
    const blob = await api.exportPdf(quiz.value, includeAnswersInPdf.value)
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
  try {
    await api.saveQuiz(quiz.value)
    justSaved.value = true
    setTimeout(() => { justSaved.value = false }, 3000)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save the quiz.'
  } finally {
    saving.value = false
  }
}
</script>
