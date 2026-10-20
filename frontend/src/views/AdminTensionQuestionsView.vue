<template>
  <div>
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>{{ $t('Tension-spørsmål') }}</h1>
          <p class="page-subtitle">
            {{ $t('Hvert spørsmål trenger opptil 10 rangerte trygge svar, pluss et hvilket som helst antall tension-svar (feller).') }}
            <router-link to="/admin/tension-categories">{{ $t('Administrer svarkategorier →') }}</router-link>
          </p>
        </div>
        <button class="btn btn-primary" @click="openCreate">{{ $t('+ Lag spørsmål') }}</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

      <div v-else-if="!questions.length" class="empty-state friendly">
        {{ $t('Ingen Tension-spørsmål ennå. Lag ditt første for å kunne starte et spill.') }}
      </div>

      <div v-else>
        <input
          type="text"
          v-model="questionSearchTerm"
          :placeholder="$t('Søk etter tittel eller kategori…')"
          class="search-input"
          style="margin-bottom:16px;"
        />

        <div v-if="!filteredQuestions.length" class="empty-state" style="padding:20px;">
          {{ $t('Ingen spørsmål samsvarer med "') }}{{ questionSearchTerm }}".
        </div>

        <div v-else class="saved-quiz-list">
          <div v-for="q in pagedQuestions" :key="q.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ q.title }}</div>
              <div class="saved-quiz-meta">
                {{ q.mainCategory || $t('Uten kategori') }} · {{ q.safeCount }} {{ $t('trygge ·') }} {{ q.tensionCount }} tension
                <span v-if="q.canExpire" class="tag" style="color:var(--gold); border-color:var(--gold);">{{ $t('Kan utløpe') }}</span>
              </div>
            </div>
            <div style="display:flex; gap:8px;">
              <button class="btn btn-secondary btn-sm" @click="openEdit(q.id)">{{ $t('Rediger') }}</button>
              <button class="btn btn-danger btn-sm" @click="requestDelete(q)">{{ $t('Slett') }}</button>
            </div>
          </div>
        </div>

        <Pagination v-model:page="questionPage" :page-size="QUESTION_PAGE_SIZE" :total-items="filteredQuestions.length" />
      </div>
    </template>

    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
        <h1 style="margin:0;">{{ editingId ? $t('Rediger spørsmål') : $t('Lag spørsmål') }}</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">{{ $t('← Tilbake til listen') }}</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>{{ $t('Tittel / oppgave') }}<input type="text" v-model="form.title" :placeholder="$t('f.eks. Nevn et land i Europa')" /></label>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:200px;">
          <label>{{ $t('Hovedkategori') }} <span class="picker-hint">{{ $t('brukes til å filtrere hvilke spørsmål et spill trekker fra - skriv hva som helst, eller velg et forslag') }}</span><input type="text" v-model="form.mainCategory" :placeholder="$t('f.eks. Geografi')" list="main-category-suggestions" /></label>
          <datalist id="main-category-suggestions">
            <option v-for="c in mainCategorySuggestions" :key="c" :value="c" />
          </datalist>
        </div>
        <div style="flex:1; min-width:240px;">
          <label>{{ $t('Svarkilde') }} <span class="picker-hint">{{ $t('driver autofullføringen i svarfeltet') }}</span></label>
          <div style="display:flex; gap:16px; margin-bottom:8px;">
            <label style="display:flex; align-items:center; gap:6px; text-transform:none; font-weight:400;">
              <input type="radio" :value="false" v-model="form.answersFromSubjects" style="width:auto;" />
              {{ $t('Tension-kategori') }}
            </label>
            <label style="display:flex; align-items:center; gap:6px; text-transform:none; font-weight:400;">
              <input type="radio" :value="true" v-model="form.answersFromSubjects" style="width:auto;" />
              {{ $t('Emner') }}
            </label>
          </div>
          <template v-if="!form.answersFromSubjects">
            <SearchableSelect
              v-model="form.answersCategory"
              :options="tensionCategories.map(c => c.name)"
              :placeholder="$t('Søk etter kategorier…')"
            />
            <p v-if="!tensionCategories.length" style="color:var(--coral); font-size:0.85rem; margin-top:6px;">
              {{ $t('Ingen kategorier finnes ennå - legg først til en på siden for Tension-kategorier.') }}
            </p>
          </template>
          <template v-else>
            <select v-model="form.answersSport">
              <option value="">{{ $t('Velg en kategori…§AdminTensionQuestionsViewT') }}</option>
              <option v-for="s in gridCategories.categories.value" :key="s" :value="s">{{ s }}</option>
            </select>
            <p style="color:var(--text-dim); font-size:0.85rem; margin-top:6px;">
              {{ $t('Svarene hentes fra emner i denne kategorien, f.eks. "Football"-spillere etter navn.') }}
            </p>
          </template>
        </div>
      </div>

      <div class="field">
        <label>{{ $t('Tilleggsinformasjon') }} <span class="picker-hint">{{ $t('valgfritt - vises for spillerne under spørsmålet (f.eks. hvor dataene kommer fra)') }}</span><input type="text" v-model="form.source" :placeholder="$t('f.eks. Lionpopulation.com')" /></label>
      </div>

      <div class="field">
        <label>{{ $t('Avgjørende spørsmål') }} <span class="picker-hint">{{ $t('valgfritt - vises for spillerne, beskriver hvordan likt avgjøres') }}</span><input type="text" v-model="form.tiebreaker" :placeholder="$t('f.eks. Ved likt vinner landet som ble grunnlagt først')" /></label>
      </div>

      <div class="field option-card">
        <input type="checkbox" id="canExpire" v-model="form.canExpire" style="width:auto; margin-top:3px;" />
        <label for="canExpire" style="margin:0; text-transform:none; font-weight:400;">
          {{ $t('Kan utløpe') }}
          <div style="color:var(--text-dim); font-size:0.8rem; font-weight:400; margin-top:2px;">
            {{ $t('f.eks. "nåværende all-time toppscorer" - merk dette slik at det vises på Innsikt-siden «Kan utløpe» for jevnlig gjennomgang. La stå uten hake for faste fakta.§AdminTensionQuestionsViewT') }}
          </div>
        </label>
      </div>

      <div class="field">
        <label>{{ $t('Trygge svar') }} <span class="picker-hint">{{ $t('rangert 1-10 - høyere plassering gir flere poeng') }}</span></label>
        <p v-if="!(form.answersFromSubjects ? form.answersSport : form.answersCategory).trim()" style="color:var(--text-dim); font-size:0.85rem; margin-top:-4px;">
          {{ $t('Velg en svarkilde over for å velge fra alternativlisten.') }}
        </p>
        <p v-else-if="!categoryOptions.length" style="color:var(--coral); font-size:0.85rem; margin-top:-4px;">
          <template v-if="form.answersFromSubjects">{{ $t('Ingen emner funnet i "') }}{{ form.answersSport }}{{ $t('" - legg først til noen på siden Emner.') }}</template>
          <template v-else>{{ $t('Ingen alternativer funnet for "') }}{{ form.answersCategory }}{{ $t('" - legg først til noen på siden Tension-kategorier.') }}</template>
        </p>
        <div v-for="(a, idx) in form.safeAnswers" :key="idx" class="candidate-row">
          <input type="number" min="1" max="10" v-model.number="a.rank" style="width:70px;" :placeholder="$t('Plassering')" />
          <div style="flex:1;">
            <SearchableSelect v-model="a.text" :options="categoryOptions" :placeholder="$t('Søk etter svar…')" />
          </div>
          <button class="btn btn-danger btn-sm" @click="form.safeAnswers.splice(idx, 1)">✕</button>
        </div>
        <button class="btn btn-secondary btn-sm" style="margin-top:8px;" @click="addSafeAnswer">{{ $t('+ Legg til trygt svar') }}</button>
      </div>

      <div class="field">
        <label>{{ $t('Tension-svar§AdminTensionQuestionsViewT') }} <span class="picker-hint">{{ $t('rangert separat - hvert av disse koster -5 hvis det gjettes') }}</span></label>
        <div v-for="(a, idx) in form.tensionAnswers" :key="idx" class="candidate-row">
          <input type="number" min="1" v-model.number="a.rank" style="width:70px;" :placeholder="$t('Plassering')" />
          <div style="flex:1;">
            <SearchableSelect v-model="a.text" :options="categoryOptions" :placeholder="$t('Søk etter svar…')" />
          </div>
          <button class="btn btn-danger btn-sm" @click="form.tensionAnswers.splice(idx, 1)">✕</button>
        </div>
        <button class="btn btn-secondary btn-sm" style="margin-top:8px;" @click="addTensionAnswer">{{ $t('+ Legg til Tension-svar') }}</button>
      </div>

      <TranslationEditor v-if="editingId" type="TENSION" :entity-id="editingId" />

      <button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? $t('Lagrer…') : $t('Lagre spørsmål') }}</button>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      :title="$t('Slette dette spørsmålet?')"
      :message="$t('«{0}» fjernes for godt.', [pendingDelete.title])"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import TranslationEditor from '../components/TranslationEditor.vue'
import { t as _t } from '../i18n'
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
    error.value = _t('Kunne ikke laste Tension-spørsmål.')
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
    error.value = _t('Kunne ikke laste spørsmålet.')
  }
}

async function save() {
  error.value = ''
  if (!form.title.trim()) {
    error.value = _t('En tittel må fylles ut.')
    return
  }
  if (!form.safeAnswers.length || form.safeAnswers.some(a => !a.text.trim())) {
    error.value = _t('Legg til minst ett trygt svar, og sørg for at ingen er tomme.')
    return
  }
  if (form.tensionAnswers.some(a => !a.text.trim())) {
    error.value = _t('Tension-svar kan ikke være tomme - fjern eventuelle tomme rader.')
    return
  }

  saving.value = true
  try {
    if (editingId.value) {
      await api.adminUpdateTensionQuestion(editingId.value, form)
    } else {
      await api.adminCreateTensionQuestion(form)
    }
    toast.show(_t('Spørsmålet er lagret.'))
    view.value = 'list'
    loadQuestions()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke lagre spørsmålet.')
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
    toast.show(_t('Spørsmålet er slettet.'))
    loadQuestions()
  } catch (e) {
    error.value = _t('Kunne ikke slette spørsmålet.§AdminTensionQuestionsViewJ')
  }
}
</script>
