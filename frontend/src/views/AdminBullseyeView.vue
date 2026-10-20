<template>
  <div>
    <!-- List view -->
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>Bullseye <span v-if="!loading && questions.length" class="header-count">{{ questions.length }}</span></h1>
          <p class="page-subtitle">
            {{ $t('Lag og administrer Bullseye-spørsmål - et måltall, og hver utøvers faktiske statistikkverdi for det.') }}
          </p>
        </div>
        <button class="btn btn-primary" @click="openCreate">{{ $t('+ Lag spørsmål') }}</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

      <div v-else-if="!questions.length" class="empty-state friendly">
        {{ $t('Ingen Bullseye-spørsmål ennå. Lag ditt første her.') }}
      </div>

      <template v-else>
      <BoardListToolbar
        v-model:search="searchTerm"
        v-model:sort-key="sortKey"
        v-model:sort-dir="sortDir"
        :sorts="questionSorts"
        :total-count="questions.length"
        :filtered-count="filteredQuestions.length"
        :placeholder="$t('Søk etter spørsmål på tittel, kategori eller statistikk…')"
      />

      <div v-if="!filteredQuestions.length" class="empty-state">{{ $t('Ingen spørsmål samsvarer med søket ditt.') }}</div>

      <div v-else class="saved-quiz-list">
        <div v-for="q in pagedQuestions" :key="q.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              {{ q.title }}
              <span v-if="q.excludedFromBullseye" class="tag" style="background:rgba(179, 38, 30, 0.15); color:var(--coral); margin-left:6px;">{{ $t('Ikke i Bullseye') }}</span>
              <span v-if="q.entireCategoryPool" class="tag" style="background:rgba(31, 122, 80, 0.15); color:var(--teal); margin-left:6px;">{{ $t('Automatisk utvalg') }}</span>
              <span v-if="q.canExpire" class="tag" style="background:rgba(216, 67, 43, 0.15); color:var(--gold); margin-left:6px;">{{ $t('Kan utløpe') }}</span>
            </div>
            <div class="saved-quiz-meta">
              {{ sportLabel(q.sport) }} · {{ q.entryCount }} {{ $t('svar · "') }}{{ q.groupDigits === false ? q.targetValue : formatNumber(q.targetValue) }} {{ q.statLabel }}"
            </div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(q.id)">{{ $t('Rediger') }}</button>
            <button class="btn btn-secondary btn-sm" @click="duplicateQuestion(q.id)">{{ $t('⧉ Dupliser') }}</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(q)">{{ $t('Slett') }}</button>
          </div>
        </div>
      </div>

      <Pagination v-model:page="questionPage" :page-size="10" :total-items="filteredQuestions.length" />
      </template>
    </template>

    <!-- Builder view -->
    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <h1 style="margin:0;">{{ editingQuestionId ? $t('Rediger spørsmål') : $t('Lag spørsmål') }}</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">{{ $t('← Tilbake til listen') }}</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>{{ $t('Tittel') }} <span class="picker-hint">{{ $t('intern merkelapp, vises ikke for spillere') }}</span><input type="text" v-model="form.title" :placeholder="$t('f.eks. Målscorere i Premier League 2024/25')" /></label>
      </div>

      <div class="field">
        <label>{{ $t('Kategori') }} <span class="picker-hint">{{ $t('velg en') }}</span></label>
        <div class="language-row">
          <button
            v-for="s in gridCategories.categories.value"
            :key="s"
            class="language-btn"
            :class="{ active: form.sport === s }"
            @click="changeSport(s)"
          >
            {{ s }}
          </button>
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:140px;">
          <label>{{ $t('Måltall') }}<input type="number" v-model.number="form.targetValue" placeholder="13" /></label>
        </div>
        <div style="flex:2; min-width:220px;">
          <label>{{ $t('Resten av oppgaven') }}<input type="text" v-model="form.statLabel" :placeholder="$t('mål i Premier League 2024/25')" /></label>
        </div>
      </div>
      <p class="page-subtitle" style="margin-top:-8px;">
        {{ $t('Spillere ser:') }} <strong>"{{ form.targetValue !== null ? (form.groupDigits ? formatNumber(form.targetValue) : form.targetValue) : '…' }} {{ form.statLabel || '…' }}"</strong>
      </p>

      <div class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.groupDigits" style="width:auto;" />
          {{ $t('Grupper sifre (1 450 000)') }}
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          {{ $t('Slå dette av for et tall som ikke er en telling - for eksempel et årstall, der gruppering ("1 798") ville blitt misforstått som en verdi i tusener.') }}
        </p>
      </div>

      <div class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.entireCategoryPool" style="width:auto;" />
          {{ $t('Bruk alle emner i "') }}{{ form.sport || '…' }}{{ $t('" som utvalg') }}
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          {{ $t('Alle emner i denne kategorien kan gjettes automatisk, inkludert de som legges til senere - ingenting å importere på nytt. Alle som ikke er oppført under som svar, gir 0 hvis de velges. Du legger fortsatt til de med en faktisk statistikkverdi under.') }}
        </p>
      </div>

      <div class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.excludedFromBullseye" style="width:auto;" />
          {{ $t('Utelat fra Bullseye') }}
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          {{ $t('Bruk dette når et spørsmåls statistikkverdier er utdaterte - det slutter å tilbys i Bullseyes tilfeldige eller manuelle valg uten å bli slettet.') }}
        </p>
      </div>

      <div class="field option-card">
        <input type="checkbox" id="canExpire" v-model="form.canExpire" style="width:auto; margin-top:3px;" />
        <label for="canExpire" style="margin:0; text-transform:none; font-weight:400;">
          {{ $t('Kan utløpe') }}
          <div style="color:var(--text-dim); font-size:0.8rem; font-weight:400; margin-top:2px;">
            {{ $t('f.eks. "nåværende all-time toppscorer" - merk dette slik at det vises på Innsikt-siden «Kan utløpe» for jevnlig gjennomgang. La stå uten hake for faste fakta.§AdminBullseyeViewT') }}
          </div>
        </label>
      </div>

      <div class="field">
        <label>
          {{ form.entireCategoryPool ? $t('Svar med en kjent verdi') : $t('Svar§AdminBullseyeViewM') }}
          <span class="picker-hint">
            {{ form.entireCategoryPool ? $t('alle andre i kategorien kan også gjettes, og gir 0') : $t('hver utøver en spiller kan nevne, med faktisk statistikkverdi') }}
          </span>
        </label>

        <div style="display:flex; gap:10px; flex-wrap:wrap; margin-bottom:10px;">
          <input
            type="text"
            v-model="athleteSearchTerm"
            :placeholder="$t('Søk etter emner på navn…')"
            style="flex:1; min-width:180px;"
          />
        </div>

        <div style="margin-bottom:10px; display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
          <button class="btn btn-secondary btn-sm" :disabled="!form.sport || importingAllInCategory" @click="importAllInCategory">
            {{ importingAllInCategory ? $t('Importerer…') : $t('+ Importer alle emner i "{0}"', [form.sport || '…']) }}
          </button>
          <button class="btn btn-secondary btn-sm" :disabled="!form.sport || importingCsv" @click="triggerCsvUpload">
            {{ importingCsv ? $t('Importerer…') : $t('+ Importer fra CSV') }}
          </button>
          <button class="btn btn-secondary btn-sm" :disabled="!form.sport || loadingFiveOhOneCategories" @click="toggleFiveOhOnePicker">
            {{ loadingFiveOhOneCategories ? $t('Laster…') : $t('+ Importer fra 501-quiz') }}
          </button>
          <button class="btn btn-secondary btn-sm" :disabled="!entries.length" @click="downloadEntriesCsv">
            {{ $t('⇩ Last ned CSV') }}
          </button>
          <input ref="csvInput" type="file" accept=".csv,text/csv" style="display:none;" @change="handleCsvFile" />
        </div>
        <p class="page-subtitle" style="margin-top:-4px;">
          {{ $t('CSV-format: én rad per svar,') }} <code>name,value</code> - e.g. <code>Erling Haaland,27</code>{{ $t('. Emner og verdier fra en 501-quiz importeres på samme måte. Navn matches mot eksisterende emner i "') }}{{ form.sport || '…' }}{{ $t('"; alt som ikke matches hoppes over, slik at du kan legge til emnet først og importere på nytt. Hver rad her må fortsatt ha en verdi fylt inn før lagring.') }}
        </p>

        <div
          v-if="showFiveOhOnePicker"
          style="display:flex; gap:10px; align-items:center; flex-wrap:wrap; margin-bottom:10px; padding:12px 14px; border:1px solid var(--border); border-radius: 2px; background:rgba(27, 26, 23, 0.06);"
        >
          <span v-if="loadingFiveOhOneCategories" style="color:var(--text-dim);">{{ $t('Laster 501-quizer…') }}</span>
          <span v-else-if="!fiveOhOneCategories.length" style="color:var(--text-dim);">{{ $t('Ingen 501-quizer finnes ennå.') }}</span>
          <template v-else>
            <div class="field" style="margin-bottom:0; flex:1; min-width:200px;">
              <select v-model.number="selectedFiveOhOneCategoryId">
                <option :value="null" disabled>{{ $t('Velg en 501-quiz…') }}</option>
                <option v-for="c in fiveOhOneCategories" :key="c.id" :value="c.id">
                  {{ c.title }} ({{ $t('{0} emne¦{0} emner', [c.entryCount]) }})
                </option>
              </select>
            </div>
            <button
              class="btn btn-primary btn-sm"
              :disabled="!selectedFiveOhOneCategoryId || importingFiveOhOne"
              @click="importFromFiveOhOne"
            >
              {{ importingFiveOhOne ? $t('Importerer…') : $t('Importer') }}
            </button>
          </template>
          <button class="btn btn-secondary btn-sm" @click="showFiveOhOnePicker = false">{{ $t('Avbryt') }}</button>
        </div>

        <div v-if="csvUnmatchedNames.length" style="background:rgba(216, 67, 43, 0.1); border:1px solid rgba(216, 67, 43, 0.3); border-radius: 2px; padding:14px 16px; margin-bottom:10px;">
          <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:10px; flex-wrap:wrap;">
            <strong style="color:var(--gold);">
              {{ csvUnmatchedNames.length }} navn ble ikke funnet i "{{ form.sport }}"
            </strong>
            <div style="display:flex; gap:8px;">
              <button class="btn btn-primary btn-sm" @click="showAddSubjectsModal = true">{{ $t('+ Legg til som emner') }}</button>
              <button class="btn btn-secondary btn-sm" @click="csvUnmatchedNames = []">{{ $t('Lukk§AdminBullseyeViewT') }}</button>
            </div>
          </div>
          <p class="page-subtitle" style="margin:6px 0 10px;">
            {{ $t('Legg disse til som emner nå, eller rett en skrivefeil i kilden og importer på nytt.') }}
          </p>
          <ul style="margin:0; padding-left:20px; max-height:200px; overflow-y:auto; line-height:1.8;">
            <li v-for="row in csvUnmatchedNames" :key="row.name">{{ row.name }}</li>
          </ul>
        </div>

        <AddSubjectsModal
          v-if="showAddSubjectsModal"
          :names="csvUnmatchedNames"
          :sport="form.sport"
          @close="showAddSubjectsModal = false"
          @added="onSubjectsAdded"
        />

        <ImportEntriesModal
          v-if="showImportReviewModal"
          :rows="pendingImportRows"
          :source-label="pendingImportLabel"
          :pool="pendingImportPool"
          @close="showImportReviewModal = false"
          @confirm="applyImportSelection"
        />

        <div v-if="athleteSearchResults.length" class="guess-results" style="margin-bottom:10px;">
          <button
            v-for="a in athleteSearchResults"
            :key="a.id"
            class="guess-result-row"
            @click="addEntry(a)"
          >
            {{ a.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ a.team }}</span>
          </button>
        </div>

        <div v-if="!entries.length" class="empty-state" style="padding:20px;">
          {{ $t('Ingen svar lagt til ennå - søk over for å legge til emner med en faktisk statistikkverdi.') }}
        </div>

        <div v-else class="candidate-list">
          <input
            type="text"
            v-model="entryFilterTerm"
            :placeholder="$t('Finn noen som allerede er i denne listen…')"
            class="search-input"
            style="margin-bottom:12px;"
          />

          <div v-if="!filteredEntries.length" class="empty-state" style="padding:20px;">
            {{ $t('Ingen i denne listen samsvarer med "') }}{{ entryFilterTerm }}".
          </div>

          <template v-else>
          <div v-for="e in pagedEntries" :key="e.athleteId" class="candidate-row">
            <div style="font-weight:600;">
              {{ e.name }} <span style="color:var(--text-dim); font-weight:400; font-size:0.85rem;">{{ e.team }}</span>
            </div>
            <div style="display:flex; gap:8px; align-items:center;">
              <input type="number" v-model.number="e.statValue" :placeholder="$t('Statistikkverdi')" style="width:110px;" />
            </div>
            <button class="btn btn-danger btn-sm" @click="removeEntry(e)">✕</button>
          </div>

          <Pagination v-model:page="entryPage" :page-size="ENTRY_PAGE_SIZE" :total-items="filteredEntries.length" />
          </template>
        </div>
      </div>

      <TranslationEditor v-if="editingQuestionId" type="BULLSEYE" :entity-id="editingQuestionId" />

      <div style="display:flex; gap:10px; flex-wrap:wrap;">
        <button v-if="editingQuestionId" class="btn btn-secondary" @click="duplicateAsNewVersion">
          {{ $t('⧉ Dupliser som ny versjon') }}
        </button>
        <button class="btn btn-primary" :disabled="saving" @click="saveQuestion">
          {{ saving ? $t('Lagrer…') : $t('Lagre spørsmål') }}
        </button>
      </div>
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
import AddSubjectsModal from '../components/AddSubjectsModal.vue'
import ImportEntriesModal from '../components/ImportEntriesModal.vue'
import Pagination from '../components/Pagination.vue'
import BoardListToolbar from '../components/BoardListToolbar.vue'
import { useBoardList } from '../composables/useBoardList'
import { formatNumber, sportLabel } from '../constants'
import gridCategories from '../services/gridCategories'
import { downloadCsv, parseNameValueCsv } from '../services/csv'

const view = ref('list')
const questions = ref([])
const loading = ref(true)

// Search / sort / paginate the question list.
const {
  searchTerm, sortKey, sortDir, page: questionPage,
  filtered: filteredQuestions, paged: pagedQuestions, sorts: questionSorts
} = useBoardList(questions, {
  pageSize: 10,
  searchFields: [q => q.title, q => q.sport, q => q.statLabel],
  sorts: [
    { key: 'title', label: _t('Tittel'), accessor: q => q.title },
    { key: 'category', label: _t('Kategori'), accessor: q => q.sport },
    { key: 'answers', label: _t('Svar§sort'), accessor: q => q.entryCount, dir: 'desc' },
    { key: 'target', label: _t('Måltall'), accessor: q => q.targetValue, dir: 'desc' }
  ]
})
const error = ref('')
const saving = ref(false)
const pendingDelete = ref(null)

const editingQuestionId = ref(null)
const form = reactive({
  title: '',
  sport: gridCategories.categories.value[0] || '',
  targetValue: null,
  statLabel: '',
  excludedFromBullseye: false,
  canExpire: false,
  entireCategoryPool: false,
  groupDigits: true
})
// [{ athleteId, name, team, statValue }] - every row here is an answer with a
// real, required value. Coverage for "everyone else in the category, no value
// needed" comes from form.entireCategoryPool instead (see BullseyePlayService).
const entries = ref([])

const ENTRY_PAGE_SIZE = 25
const entryPage = ref(1)
const entryFilterTerm = ref('')
const filteredEntries = computed(() => {
  const term = entryFilterTerm.value.trim().toLowerCase()
  if (!term) return entries.value
  return entries.value.filter(e =>
    e.name.toLowerCase().includes(term) || (e.team || '').toLowerCase().includes(term)
  )
})
watch(entryFilterTerm, () => { entryPage.value = 1 })

const pagedEntries = computed(() => {
  const start = (entryPage.value - 1) * ENTRY_PAGE_SIZE
  return filteredEntries.value.slice(start, start + ENTRY_PAGE_SIZE)
})

const athleteSearchTerm = ref('')
const athleteSearchResults = ref([])

onMounted(async () => {
  loadQuestions()
  await gridCategories.ensureLoaded()
  if (!form.sport) form.sport = gridCategories.categories.value[0] || ''
})

async function loadQuestions() {
  loading.value = true
  error.value = ''
  try {
    questions.value = await api.adminListBullseyeQuestions()
  } catch (e) {
    error.value = _t('Kunne ikke laste Bullseye-spørsmål.')
  } finally {
    loading.value = false
  }
}

let searchDebounce = null
watch(athleteSearchTerm, (val) => {
  clearTimeout(searchDebounce)
  if (!val || val.trim().length < 2) {
    athleteSearchResults.value = []
    return
  }
  searchDebounce = setTimeout(async () => {
    try {
      athleteSearchResults.value = await api.adminSearchAthletes({ sport: form.sport, name: val })
    } catch (e) {
      // non-critical
    }
  }, 250)
})

function changeSport(code) {
  form.sport = code
  athleteSearchResults.value = []
}

function addEntry(athlete) {
  if (entries.value.some(e => e.athleteId === athlete.id)) return
  entries.value.push({ athleteId: athlete.id, name: athlete.name, team: athlete.team, statValue: null })
}

function addEntriesBulk(athleteList) {
  const existingIds = new Set(entries.value.map(e => e.athleteId))
  let added = 0
  for (const athlete of athleteList) {
    if (existingIds.has(athlete.id)) continue
    existingIds.add(athlete.id)
    entries.value.push({ athleteId: athlete.id, name: athlete.name, team: athlete.team, statValue: null })
    added++
  }
  return added
}

function removeEntry(e) {
  const realIndex = entries.value.indexOf(e)
  if (realIndex !== -1) entries.value.splice(realIndex, 1)
  const maxPage = Math.max(1, Math.ceil(entries.value.length / ENTRY_PAGE_SIZE))
  if (entryPage.value > maxPage) entryPage.value = maxPage
}

// Same "name,value" shape the CSV importer above reads back in, so a
// downloaded file round-trips straight back through "+ Import from CSV".
function downloadEntriesCsv() {
  const rows = [['name', 'value'], ...entries.value.map(e => [e.name, e.statValue])]
  const filename = `${(form.title.trim() || 'bullseye').replace(/[^\w\- ]+/g, '').trim() || 'bullseye'}.csv`
  downloadCsv(filename, rows)
}

const importingAllInCategory = ref(false)
async function importAllInCategory() {
  if (!form.sport) return
  error.value = ''
  importingAllInCategory.value = true
  try {
    const all = await api.adminSearchAthletes({ sport: form.sport })
    const added = addEntriesBulk(all)
    toast.show(_t('Importerte {0} emne(r) fra "{1}" ({2} var allerede i denne listen). Fyll inn statistikkverdien for hver, eller fjern de du ikke trenger.', [added, form.sport, all.length - added]))
  } catch (e) {
    error.value = _t('Kunne ikke importere emner for den kategorien.')
  } finally {
    importingAllInCategory.value = false
  }
}

const csvInput = ref(null)
const importingCsv = ref(false)
const csvUnmatchedNames = ref([]) // [{name, value}] full list, shown in-page - the toast only ever had room for a few
const showAddSubjectsModal = ref(false)

// AddSubjectsModal created these as real subjects - wire each straight into
// entries with the value its import row already had, same as importRows does
// for a name that matched on the first try.
function onSubjectsAdded(createdWithValues) {
  for (const { athlete, value } of createdWithValues) {
    if (entries.value.some(e => e.athleteId === athlete.id)) continue
    entries.value.push({ athleteId: athlete.id, name: athlete.name, team: athlete.team, statValue: value })
  }
  const addedNames = new Set(createdWithValues.map(c => c.athlete.name.trim().toLowerCase()))
  csvUnmatchedNames.value = csvUnmatchedNames.value.filter(row => !addedNames.has(row.name.trim().toLowerCase()))
  showAddSubjectsModal.value = false
}

function triggerCsvUpload() {
  csvInput.value?.click()
}

// Shared by CSV import and the "import from 501 quiz" picker below - both
// just produce a list of {name, value} rows. Resolves each against subjects
// already in this category (same pool importAllInCategory draws from), then
// hands the annotated rows to ImportEntriesModal for a review/select step -
// nothing lands in entries until the admin confirms which ones. Deliberately
// doesn't create new Athlete records on the fly for an unmatched row, since a
// typo in the source would otherwise silently create a junk duplicate
// subject instead of surfacing as a row to fix or explicitly add.
const pendingImportRows = ref([])
const pendingImportLabel = ref('')
const pendingImportPool = ref([])
const showImportReviewModal = ref(false)

async function importRows(rows, sourceLabel) {
  const pool = await api.adminSearchAthletes({ sport: form.sport })
  const athleteByName = new Map(pool.map(a => [a.name.trim().toLowerCase(), a]))
  pendingImportRows.value = rows.map(row => ({
    name: row.name,
    value: row.value,
    athlete: athleteByName.get(row.name.trim().toLowerCase()) || null
  }))
  pendingImportLabel.value = sourceLabel
  pendingImportPool.value = pool
  showImportReviewModal.value = true
}

// ImportEntriesModal confirmed this subset - only these actually get applied.
function applyImportSelection(selectedRows) {
  showImportReviewModal.value = false
  let added = 0
  let updated = 0
  const unmatched = []
  for (const row of selectedRows) {
    if (!row.athlete) {
      // Keeps the row's value alongside the name - AddSubjectsModal needs it
      // to wire a freshly-created subject straight into entries once accepted,
      // instead of the admin having to look the value back up in the source.
      unmatched.push({ name: row.name, value: row.value })
      continue
    }
    const existing = entries.value.find(e => e.athleteId === row.athlete.id)
    if (existing) {
      existing.statValue = row.value
      updated++
    } else {
      entries.value.push({ athleteId: row.athlete.id, name: row.athlete.name, team: row.athlete.team, statValue: row.value })
      added++
    }
  }
  entryPage.value = 1
  entryFilterTerm.value = ''
  csvUnmatchedNames.value = unmatched

  let message = _t('{0}: {1} svar lagt til, {2} oppdatert.', [pendingImportLabel.value, added, updated])
  if (unmatched.length) {
    message += _t('{0} navn ble ikke funnet - se listen under.', [unmatched.length])
  }
  toast.show(message, unmatched.length ? 'error' : 'success')
}

async function handleCsvFile(event) {
  const file = event.target.files?.[0]
  event.target.value = '' // lets the same file be re-selected after fixing it
  if (!file || !form.sport) return

  error.value = ''
  csvUnmatchedNames.value = []
  importingCsv.value = true
  try {
    const rows = parseNameValueCsv(await file.text())
    if (!rows.length) {
      toast.show(_t('Den CSV-filen hadde ingen gyldige "name,value"-rader å importere.'), 'error')
      return
    }
    await importRows(rows, 'CSV import')
  } catch (e) {
    error.value = _t('Kunne ikke lese CSV-filen.')
  } finally {
    importingCsv.value = false
  }
}

// "Copy a 501 quiz into Bullseye" - a 501 category is already just a list of
// {name, value} rows (the darts-checkout number each subject throws for), the
// exact same shape a CSV row is - so this reuses importRows rather than
// needing its own matching logic.
const fiveOhOneCategories = ref([])
const loadingFiveOhOneCategories = ref(false)
const showFiveOhOnePicker = ref(false)
const selectedFiveOhOneCategoryId = ref(null)
const importingFiveOhOne = ref(false)

async function toggleFiveOhOnePicker() {
  showFiveOhOnePicker.value = !showFiveOhOnePicker.value
  if (showFiveOhOnePicker.value && !fiveOhOneCategories.value.length) {
    loadingFiveOhOneCategories.value = true
    try {
      fiveOhOneCategories.value = await api.adminListFiveOhOneCategories()
    } catch (e) {
      error.value = _t('Kunne ikke laste 501-quizer.')
    } finally {
      loadingFiveOhOneCategories.value = false
    }
  }
}

async function importFromFiveOhOne() {
  if (!selectedFiveOhOneCategoryId.value || !form.sport) return
  error.value = ''
  csvUnmatchedNames.value = []
  importingFiveOhOne.value = true
  try {
    const category = await api.adminGetFiveOhOneCategory(selectedFiveOhOneCategoryId.value)
    const rows = category.entries.map(e => ({ name: e.name, value: e.value }))
    if (!rows.length) {
      toast.show(_t('"{0}" har ingen emner å importere.', [category.title]), 'error')
      return
    }
    await importRows(rows, _t('Importert fra "{0}"', [category.title]))
    showFiveOhOnePicker.value = false
  } catch (e) {
    error.value = _t('Kunne ikke importere fra den 501-quizen.')
  } finally {
    importingFiveOhOne.value = false
  }
}

function resetForm() {
  form.title = ''
  form.sport = gridCategories.categories.value[0] || ''
  form.targetValue = null
  form.statLabel = ''
  form.excludedFromBullseye = false
  form.canExpire = false
  form.entireCategoryPool = false
  form.groupDigits = true
  entries.value = []
  entryPage.value = 1
  entryFilterTerm.value = ''
  athleteSearchTerm.value = ''
  athleteSearchResults.value = []
  csvUnmatchedNames.value = []
  showAddSubjectsModal.value = false
  showImportReviewModal.value = false
  pendingImportRows.value = []
  pendingImportPool.value = []
  showFiveOhOnePicker.value = false
  selectedFiveOhOneCategoryId.value = null
}

function openCreate() {
  resetForm()
  editingQuestionId.value = null
  view.value = 'builder'
}

async function openEdit(id) {
  error.value = ''
  csvUnmatchedNames.value = []
  showAddSubjectsModal.value = false
  showImportReviewModal.value = false
  pendingImportRows.value = []
  pendingImportPool.value = []
  showFiveOhOnePicker.value = false
  selectedFiveOhOneCategoryId.value = null
  try {
    const detail = await api.adminGetBullseyeQuestion(id)
    form.title = detail.title
    form.sport = detail.sport
    form.targetValue = detail.targetValue
    form.statLabel = detail.statLabel
    form.excludedFromBullseye = detail.excludedFromBullseye
    form.canExpire = detail.canExpire || false
    form.entireCategoryPool = detail.entireCategoryPool || false
    form.groupDigits = detail.groupDigits !== false

    entries.value = detail.entries.map(e => ({
      athleteId: e.athlete.id, name: e.athlete.name, team: e.athlete.team, statValue: e.statValue
    }))
    entryPage.value = 1
    entryFilterTerm.value = ''

    editingQuestionId.value = id
    view.value = 'builder'
  } catch (e) {
    error.value = _t('Kunne ikke laste spørsmålet.')
  }
}

// One-click duplicate straight from the list - loads the question into the
// builder exactly like Edit would, then immediately detaches it into a new
// unsaved copy via duplicateAsNewVersion, so there's no need to open Edit
// first just to reach the button at the bottom of the builder.
async function duplicateQuestion(id) {
  await openEdit(id)
  if (editingQuestionId.value === id) duplicateAsNewVersion()
}

// Keeps every current form field and entry exactly as-is - only clearing
// the editing id, so the next save creates a brand new question via POST
// instead of overwriting the original via PUT. The original stays
// completely untouched until this copy is saved.
function duplicateAsNewVersion() {
  editingQuestionId.value = null
  form.title = form.title + _t(' (oppdatert)')
  form.excludedFromBullseye = false
  toast.show(_t('Du redigerer nå en ny duplikat - det opprinnelige spørsmålet er urørt. Husk å lagre denne kopien.'))
}

async function saveQuestion() {
  error.value = ''
  if (!form.title.trim() || !form.sport || form.targetValue === null || !form.statLabel.trim()) {
    error.value = _t('Tittel, kategori, måltall og oppgave må fylles ut.')
    return
  }
  if (entries.value.length < 2) {
    error.value = _t('Legg til minst 2 utøvere med en statistikkverdi.')
    return
  }
  const missingValue = entries.value.find(e => e.statValue === null || e.statValue === '')
  if (missingValue) {
    error.value = _t('«{0}» mangler en statistikkverdi - fyll inn en eller fjern den raden.', [missingValue.name])
    return
  }

  const payload = {
    title: form.title,
    sport: form.sport,
    targetValue: form.targetValue,
    statLabel: form.statLabel,
    excludedFromBullseye: form.excludedFromBullseye,
    canExpire: form.canExpire,
    entireCategoryPool: form.entireCategoryPool,
    groupDigits: form.groupDigits,
    entries: entries.value.map(e => ({ athleteId: e.athleteId, statValue: e.statValue }))
  }

  saving.value = true
  try {
    if (editingQuestionId.value) {
      await api.adminUpdateBullseyeQuestion(editingQuestionId.value, payload)
    } else {
      await api.adminCreateBullseyeQuestion(payload)
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
    await api.adminDeleteBullseyeQuestion(q.id)
    toast.show(_t('Spørsmålet er slettet.'))
    loadQuestions()
  } catch (e) {
    error.value = _t('Kunne ikke slette spørsmålet.§AdminBullseyeViewJ')
  }
}
</script>
