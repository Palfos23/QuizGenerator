<template>
  <div>
    <h1>{{ $t('Mine quizer') }}</h1>

    <div v-if="error" class="banner error">{{ error }}</div>

    <!-- List view -->
    <section v-if="!openQuiz">
      <div class="view-tabs" v-if="showTemplatesTab">
        <button type="button" class="view-tab" :class="{ active: activeTab === 'mine' }" @click="activeTab = 'mine'">
          {{ $t('Mine quizer') }} <span class="view-tab-count">{{ quizzes.length }}</span>
        </button>
        <button type="button" class="view-tab" :class="{ active: activeTab === 'templates' }" @click="activeTab = 'templates'">
          {{ $t('Quizmaler') }} <span class="view-tab-count">{{ templates.length }}</span>
        </button>
      </div>

      <template v-if="activeTab === 'templates' && showTemplatesTab">
        <p class="page-subtitle" style="margin-top:0;">{{ $t('Publisert av en administrator - kopier en for å få din egen versjon du kan redigere.') }}</p>
        <div class="saved-quiz-list">
          <div v-for="t in templates" :key="t.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ t.title }}</div>
              <div class="saved-quiz-meta">{{ languageLabel(t.language) }} · {{ t.questionCount }} {{ $t('spørsmål§MyQuizzesViewT') }}</div>
            </div>
            <button class="btn btn-secondary btn-sm" :disabled="copyingId === t.id" @click="copyTemplate(t)">
              {{ copyingId === t.id ? $t('Kopierer…') : $t('Kopier til Mine quizer') }}
            </button>
          </div>
        </div>
      </template>

      <template v-else>
        <p class="page-subtitle" style="margin-top:0;">{{ $t('Quizer du har lagret selv. Å lage en quiz lagrer den ikke automatisk.') }}</p>

        <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

        <div v-else-if="!quizzes.length" class="empty-state friendly">
          {{ $t('Ingen lagrede quizer ennå. Lag en på siden') }} <router-link to="/generate">{{ $t('Lag en quiz') }}</router-link> {{ $t(', og trykk deretter «Lagre i Mine quizer»') }}<template v-if="showTemplatesTab">{{ $t(', eller kopier en') }} <a href="#" @click.prevent="activeTab = 'templates'">{{ $t('quizmal') }}</a> {{ $t('for å komme i gang') }}</template>.
        </div>

        <div v-else class="saved-quiz-list">
          <div v-for="q in quizzes" :key="q.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ q.title }}</div>
              <div class="saved-quiz-meta">
                {{ flagFor(q.language) }} {{ languageLabel(q.language) }} · {{ q.questionCount }} {{ $t('spørsmål · lagret') }} {{ formatDate(q.createdAt) }}
              </div>
            </div>
            <div style="display:flex; gap:8px; flex-wrap:wrap;">
              <button class="btn btn-secondary btn-sm" @click="openOne(q.id)">{{ $t('Vis og rediger') }}</button>
              <button
                class="btn btn-secondary btn-sm"
                :disabled="duplicatingId === q.id"
                @click="duplicateAndRegenerate(q)"
              >
                {{ duplicatingId === q.id ? $t('Lager på nytt…') : $t('Dupliser og lag på nytt') }}
              </button>
              <button class="btn btn-danger btn-sm" @click="requestDelete(q)">{{ $t('Slett') }}</button>
            </div>
          </div>
        </div>
      </template>
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
            :aria-label="$t('Quiztittel')"
          />
          <span style="color:var(--text-dim); font-size:0.9rem;">{{ openQuiz.questions.length }} {{ $t('spørsmål§MyQuizzesViewT') }}</span>
        </div>
        <button class="btn btn-secondary btn-sm no-print" @click="backToList">{{ $t('← Tilbake til listen') }}</button>
      </div>

      <div v-if="openQuiz.warnings && openQuiz.warnings.length" class="banner error">
        <div v-for="(w, i) in openQuiz.warnings" :key="i">{{ w }}</div>
      </div>

      <p class="page-subtitle" style="margin-top:-10px;">
        <template v-if="isDraft">{{ $t('Dette er en nylig laget kopi - trykk «Lagre som ny» når du er fornøyd med den.') }}</template>
        <template v-else>{{ $t('Bruk knappene ↑ / ↓ for å endre rekkefølgen, eller forkast/bytt ut spørsmål - trykk deretter «Oppdater» for å lagre endringene.') }}</template>
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
              {{ $t('Ta med svar i PDF') }}
            </label>
            <div style="display:flex; gap:12px; flex-wrap:wrap;">
              <button class="btn btn-primary" :disabled="exportingPdf" @click="downloadPdf">
                {{ exportingPdf ? $t('Forbereder PDF…') : $t('Last ned som PDF') }}
              </button>
              <button
                v-if="isDraft"
                class="btn"
                :class="justUpdated ? 'btn-primary' : 'btn-secondary'"
                :disabled="updating"
                @click="saveAsNew"
              >
                {{ updating ? $t('Lagrer…') : justUpdated ? $t('Lagret ✓') : $t('Lagre som ny') }}
              </button>
              <button
                v-else
                class="btn"
                :class="justUpdated ? 'btn-primary' : 'btn-secondary'"
                :disabled="updating"
                @click="updateQuiz"
              >
                {{ updating ? $t('Oppdaterer…§MyQuizzesViewM') : justUpdated ? $t('Oppdatert ✓') : $t('Oppdater§MyQuizzesViewM') }}
              </button>
            </div>
          </div>
        </template>
      </QuizReviewEditor>
    </section>

    <ConfirmModal
      v-if="pendingDelete"
      :title="$t('Slette denne quizen?')"
      :message="$t('«{0}» fjernes for godt. Dette kan ikke angres.', [pendingDelete.title])"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />

    <ConfirmModal
      v-if="showUnsavedConfirm"
      :title="$t('Forkaste endringene?')"
      :message="$t('Du har ulagrede endringer i denne quizen. Hvis du går tilbake, mister du dem med mindre du lagrer først.')"
      :confirm-text="$t('Forkast og gå tilbake')"
      @confirm="confirmBackToList"
      @cancel="showUnsavedConfirm = false"
    />

    <ConfirmModal
      v-if="showLeaveConfirm"
      :title="$t('Forlate denne quizen?')"
      :message="$t('Du har ulagrede endringer i denne quizen. Hvis du forlater siden, mister du dem med mindre du lagrer først.')"
      :confirm-text="$t('Forlat likevel')"
      @confirm="confirmLeave"
      @cancel="cancelLeave"
    />
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute } from 'vue-router'
import api from '../services/api'
import navTrigger from '../services/navTrigger'
import toast from '../services/toast'
import QuizReviewEditor from '../components/QuizReviewEditor.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { LANGUAGES, languageLabel } from '../constants'

const route = useRoute()

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
const templates = ref([])
const copyingId = ref(null)

// The dashboard's "Quiz templates" and "My quizzes" cards both land here but
// promise two different things - this lets a click on "Quiz templates" open
// straight to that tab instead of dumping the visitor onto their (possibly
// empty) own list with the templates they actually wanted buried above it.
const activeTab = ref(route.query.tab === 'templates' ? 'templates' : 'mine')
const showTemplatesTab = computed(() => templates.value.length > 0)

// A regenerated-but-not-yet-saved copy has no id from the server yet - that's
// the signal for "Save as new" instead of "Update".
const isDraft = computed(() => !!openQuiz.value && !openQuiz.value.id)

// Protects against losing edits to an open quiz by clicking a nav link - the
// "Back to list" button already warns about this, but leaving via the nav bar
// previously lost your work silently.
let pendingLeaveResolve = null
const showLeaveConfirm = ref(false)

onBeforeRouteLeave(() => {
  if (!openQuiz.value || !dirty.value) return true
  return new Promise((resolve) => {
    pendingLeaveResolve = resolve
    showLeaveConfirm.value = true
  })
})

// Clicking the "My quizzes" nav tab while already on this page doesn't trigger any
// navigation event on its own, so it needs its own trigger to close whatever quiz
// is open and go back to the list - reusing the same discard-confirmation flow.
// Also jumps back to the "My quizzes" tab itself, since that's what the nav
// link says regardless of which tab was left open.
watch(() => navTrigger.state.myQuizzes, () => {
  activeTab.value = 'mine'
  if (openQuiz.value) backToList()
})

function confirmLeave() {
  showLeaveConfirm.value = false
  pendingLeaveResolve?.(true)
}

function cancelLeave() {
  showLeaveConfirm.value = false
  pendingLeaveResolve?.(false)
}

onMounted(() => {
  loadList()
  loadTemplates()
})

async function loadTemplates() {
  try {
    templates.value = await api.listQuizTemplates()
  } catch (e) {
    // non-critical - the templates section just stays empty
  }
}

async function copyTemplate(t) {
  copyingId.value = t.id
  error.value = ''
  try {
    await api.copyQuizTemplate(t.id)
    toast.show(_t('«{0}» er lagt til i Mine quizer.', [t.title]))
    loadList()
  } catch (e) {
    error.value = _t('Kunne ikke kopiere malen.')
  } finally {
    copyingId.value = null
  }
}

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    quizzes.value = await api.listSavedQuizzes()
  } catch (e) {
    error.value = _t('Kunne ikke laste de lagrede quizene dine.')
  } finally {
    loading.value = false
  }
}

function flagFor(code) {
  return LANGUAGES.find(l => l.code === code)?.flag || code
}

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { year: 'numeric', month: 'short', day: 'numeric' })
}

async function openOne(id) {
  error.value = ''
  try {
    openQuiz.value = await api.getSavedQuiz(id)
    dirty.value = false
  } catch (e) {
    error.value = _t('Kunne ikke laste quizen.')
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
      title: _t('{0} (kopi)', [full.title]),
      language: full.language,
      minDifficulty: 1,
      maxDifficulty: 10,
      categorySelections
    })
    openQuiz.value = result
    dirty.value = true
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke lage quizen på nytt.')
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
  loadList()
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
    toast.show(_t('Quizen er slettet.'))
    loadList()
  } catch (e) {
    error.value = _t('Kunne ikke slette quizen.')
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
    error.value = _t('Kunne ikke lage PDF-en.')
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
    error.value = e.response?.data?.message || _t('Kunne ikke oppdatere quizen.')
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
    error.value = e.response?.data?.message || _t('Kunne ikke lagre quizen.')
  } finally {
    updating.value = false
  }
}
</script>
