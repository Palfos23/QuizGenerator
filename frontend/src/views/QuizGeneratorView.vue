<template>
  <div>
    <div class="step-indicator">{{ quiz ? 'Steg 2 av 2 - Gå gjennom og fullfør' : 'Steg 1 av 2 - Sett opp' }}</div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px;">
      <h1 style="margin:0;">Lag en quiz</h1>
      <router-link v-if="!quiz" to="/suggest-question" class="btn btn-secondary btn-sm">+ Foreslå et spørsmål</router-link>
    </div>
    <p class="page-subtitle">
      Velg språk, vanskelighetsgrad og hvor mange spørsmål du vil ha fra hver kategori.
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="quiz && quiz.warnings && quiz.warnings.length" class="banner error">
      <div v-for="(w, i) in quiz.warnings" :key="i">{{ w }}</div>
    </div>

    <!-- Step 1: settings form -->
    <section v-if="!quiz">
      <div class="field">
        <label>Quiztittel<input type="text" v-model="form.title" placeholder="f.eks. Saras 30-årsquiz" /></label>
      </div>

      <div class="field">
        <label>Språk <span class="picker-hint">velg en</span></label>
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
        <label>Kategorier og hvor mange spørsmål fra hver</label>

        <div v-if="categoriesLoading" style="color:var(--text-dim); padding:20px;">Laster kategorier…</div>
        <div v-else-if="!availableCategories.length" class="empty-state friendly" style="padding:20px;">
          Ingen {{ languageLabel(form.language) }} kategorier ennå - legg først til noen spørsmål på det språket på administrasjonssiden.
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
                <button aria-label="Færre spørsmål" @click="stepCount(cat, -1)">−</button>
                <span>{{ countFor(cat) }}</span>
                <button aria-label="Flere spørsmål" @click="stepCount(cat, 1)">+</button>
              </div>
              <button class="chip-remove-btn" aria-label="Fjern kategori" @click="toggleCategory(cat)">✕</button>
            </template>
          </div>
        </div>
      </div>

      <details class="advanced-disclosure">
        <summary>Avanserte valg</summary>

        <div class="field" style="margin-top:16px;">
          <label>Vanskelighetsgrad <span style="text-transform:none;font-weight:400;">(1 = lettest, 10 = vanskeligst)</span></label>
          <div class="difficulty-slider-group">
            <div class="difficulty-slider-row">
              <span class="slider-label">Min</span>
              <input
                type="range" min="1" max="10" v-model.number="form.minDifficulty"
                @input="clampRange('min')" aria-label="Laveste vanskelighetsgrad"
              />
              <output>{{ form.minDifficulty }} <span class="difficulty-word">{{ difficultyWord(form.minDifficulty) }}</span></output>
            </div>
            <div class="difficulty-slider-row">
              <span class="slider-label">Maks</span>
              <input
                type="range" min="1" max="10" v-model.number="form.maxDifficulty"
                @input="clampRange('max')" aria-label="Høyeste vanskelighetsgrad"
              />
              <output>{{ form.maxDifficulty }} <span class="difficulty-word">{{ difficultyWord(form.maxDifficulty) }}</span></output>
            </div>
          </div>
        </div>

        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:400;">
          <input type="checkbox" v-model="form.includeMySubmissions" style="width:auto;" />
          Ta med mine egne innsendte spørsmål <span class="picker-hint">også de som fortsatt venter eller er avvist - bare du kan bruke disse</span>
        </label>
      </details>

      <div style="display:flex; align-items:center; gap:16px; flex-wrap:wrap; margin-top:20px;">
        <button class="btn btn-primary" :disabled="generating || !form.categorySelections.length" @click="generateQuiz">
          {{ generating ? 'Lager…' : 'Lag quiz' }}
        </button>
        <button class="btn btn-secondary" @click="startBlank">
          Eller start tom og søk etter bestemte spørsmål →
        </button>
      </div>
    </section>

    <!-- Step 2: review, reorder + finalize -->
    <section v-else>
      <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <div style="flex:1; min-width:220px;">
          <input type="text" v-model="quiz.title" @input="reviewDirty = true" class="title-edit-input" aria-label="Quiztittel" />
          <span style="color:var(--text-dim); font-size:0.9rem;">{{ quiz.questions.length }} spørsmål</span>
        </div>
        <button class="btn btn-secondary btn-sm no-print" @click="requestStartOver">← Tilbake til innstillinger</button>
      </div>
      <p class="page-subtitle" style="margin-top:-10px;">Bruk knappene ↑ / ↓ for å endre rekkefølgen på spørsmålene.</p>

      <QuizReviewEditor
        :quiz="quiz"
        :min-difficulty="form.minDifficulty"
        :max-difficulty="form.maxDifficulty"
        @error="error = $event"
        @changed="reviewDirty = true"
      >
        <template #empty>
          Ingen spørsmål her ennå - legg til en tilfeldig gruppe, eller søk etter bestemte, under.
        </template>
        <template #actions>
          <div class="review-actions">
            <label class="review-actions-toggle">
              <input type="checkbox" v-model="includeAnswersInPdf" />
              Ta med svar i PDF
            </label>
            <div class="review-actions-buttons">
              <button class="btn btn-primary" :disabled="exportingPdf" @click="downloadPdf">
                {{ exportingPdf ? 'Forbereder PDF…' : 'Last ned som PDF' }}
              </button>
              <button
                class="btn"
                :class="justSaved ? 'btn-primary' : 'btn-secondary'"
                :disabled="saving"
                @click="saveQuiz"
              >
                {{ saving ? 'Lagrer…' : justSaved ? 'Lagret i Mine quizer ✓' : 'Lagre i Mine quizer' }}
              </button>
            </div>
          </div>
        </template>
      </QuizReviewEditor>
    </section>

    <ConfirmModal
      v-if="showStartOverConfirm"
      title="Forkaste endringene?"
      message="Du har forkastet, byttet ut eller endret rekkefølgen på spørsmål i denne quizen. Hvis du går tilbake til innstillingene, mister du disse endringene med mindre du allerede har lagret quizen."
      confirm-text="Tilbake til innstillinger"
      @confirm="confirmStartOver"
      @cancel="showStartOverConfirm = false"
    />

    <ConfirmModal
      v-if="showLeaveConfirm"
      title="Forlate denne quizen?"
      message="Du har forkastet, byttet ut eller endret rekkefølgen på spørsmål i denne quizen. Hvis du forlater siden, mister du disse endringene med mindre du allerede har lagret quizen."
      confirm-text="Forlat likevel"
      @confirm="confirmLeave"
      @cancel="cancelLeave"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import navTrigger from '../services/navTrigger'
import QuizReviewEditor from '../components/QuizReviewEditor.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import api from '../services/api'
import { LANGUAGES, languageLabel } from '../constants'

const DRAFT_KEY = 'quiz_draft_form'

// Merged with the defaults, not replaced by them - an old draft saved before
// a new field was added to this form would otherwise permanently miss that
// field, since it would just become the entire form object as-is. Spreading
// the draft's own values second means anything it does have still wins,
// while anything it's missing falls back to the default.
const form = reactive({
  title: '',
  language: 'EN',
  minDifficulty: 1,
  maxDifficulty: 10,
  categorySelections: [], // [{ category, numberOfQuestions }]
  includeMySubmissions: false,
  ...(loadDraft() || {})
})

const availableCategories = ref([])
const categoriesLoading = ref(true)
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
const showLeaveConfirm = ref(false)
let pendingLeaveResolve = null

// Protects against losing an in-progress review by clicking a nav link (or any
// other navigation) - previously only the "Back to settings" button warned about
// this, so leaving via the nav bar lost your work silently.
onBeforeRouteLeave(() => {
  if (!reviewDirty.value) return true
  return new Promise((resolve) => {
    pendingLeaveResolve = resolve
    showLeaveConfirm.value = true
  })
})

// Clicking the "Create a quiz" nav tab while already on this page doesn't trigger
// any navigation event on its own, so it needs its own trigger to reset back to
// step 1 - reusing the same discard-confirmation flow as the in-page button.
watch(() => navTrigger.state.generate, () => {
  requestStartOver()
})

function confirmLeave() {
  showLeaveConfirm.value = false
  pendingLeaveResolve?.(true)
}

function cancelLeave() {
  showLeaveConfirm.value = false
  pendingLeaveResolve?.(false)
}

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
  categoriesLoading.value = true
  try {
    availableCategories.value = await api.getCategories(form.language)
  } catch (e) {
    error.value = 'Kunne ikke laste kategorier fra serveren.'
  } finally {
    categoriesLoading.value = false
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
  if (value <= 3) return 'Lett'
  if (value <= 7) return 'Middels'
  return 'Vanskelig'
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

function startBlank() {
  quiz.value = {
    title: form.title || 'Min quiz',
    language: form.language,
    questions: [],
    warnings: []
  }
}

async function generateQuiz() {
  error.value = ''
  generating.value = true
  try {
    const result = await api.generateQuiz({
      title: form.title || 'Min quiz',
      language: form.language,
      minDifficulty: form.minDifficulty,
      maxDifficulty: form.maxDifficulty,
      categorySelections: form.categorySelections,
      includeMySubmissions: form.includeMySubmissions
    })
    quiz.value = result
    reviewDirty.value = false
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke lage en quiz. Prøv andre filtre.'
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
    error.value = 'Kunne ikke lage PDF-en.'
  } finally {
    exportingPdf.value = false
  }
}

async function saveQuiz() {
  saving.value = true
  error.value = ''
  try {
    await api.saveQuiz(quiz.value)
    reviewDirty.value = false
    justSaved.value = true
    setTimeout(() => { justSaved.value = false }, 3000)
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke lagre quizen.'
  } finally {
    saving.value = false
  }
}
</script>
