<template>
  <div>
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
        <h1 style="margin:0;">{{ $t('501-kategorier') }} <span v-if="!loading && categories.length" class="header-count">{{ categories.length }}</span></h1>
        <button class="btn btn-primary" @click="openCreate">{{ $t('+ Ny kategori') }}</button>
      </div>
      <p class="page-subtitle">{{ $t('Hver kategori er et rangert sett med navn og tall - f.eks. "Premier League-kamper".') }}</p>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>
      <div v-else-if="!categories.length" class="empty-state friendly">{{ $t('Ingen kategorier ennå - lag den første.') }}</div>

      <template v-else>
      <BoardListToolbar
        v-model:search="searchTerm"
        v-model:sort-key="sortKey"
        v-model:sort-dir="sortDir"
        :sorts="categorySorts"
        :total-count="categories.length"
        :filtered-count="filteredCategories.length"
        :placeholder="$t('Søk etter kategorier på tittel eller beskrivelse…')"
      />

      <div v-if="!filteredCategories.length" class="empty-state">{{ $t('Ingen kategorier samsvarer med søket ditt.') }}</div>

      <div v-else class="saved-quiz-list">
        <div v-for="c in pagedCategories" :key="c.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              {{ c.title }}
              <span v-if="c.entireCategoryPool" class="tag" style="background:rgb(var(--teal-rgb) / 0.15); color:var(--teal); margin-left:6px;">{{ $t('Automatisk utvalg') }}</span>
              <span v-if="c.canExpire" class="tag" style="background:rgba(255,196,0,0.15); color:var(--gold); margin-left:6px;">{{ $t('Kan utløpe') }}</span>
            </div>
            <div class="saved-quiz-meta">{{ c.entryCount }} {{ $t('oppføringer') }}<span v-if="c.description"> · {{ c.description }}</span></div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" :disabled="openingId === c.id" @click="openEdit(c.id)">
              {{ openingId === c.id ? $t('Laster…') : $t('Rediger') }}
            </button>
            <button class="btn btn-danger btn-sm" :disabled="openingId === c.id" @click="requestDelete(c)">{{ $t('Slett') }}</button>
          </div>
        </div>
      </div>

      <Pagination v-model:page="categoryPage" :page-size="10" :total-items="filteredCategories.length" />
      </template>
    </template>

    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <h1 style="margin:0;">{{ editingId ? $t('Rediger kategori') : $t('Ny kategori') }}</h1>
        <button class="btn btn-secondary" @click="view = 'list'">{{ $t('← Tilbake til listen') }}</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>{{ $t('Tittel') }}<input type="text" v-model="form.title" :placeholder="$t('f.eks. Premier League-kamper')" /></label>
      </div>

      <div class="field">
        <label>{{ $t('Beskrivelse') }} <span class="picker-hint">{{ $t('valgfri smakstekst') }}</span><input type="text" v-model="form.description" :placeholder="$t('Vises for spillerne før de velger denne kategorien')" /></label>
      </div>

      <div class="field">
        <label>{{ $t('Emnekategori') }} <span class="picker-hint">{{ $t('valgfritt - lar CSV-importerte navn sjekkes mot emnene i denne kategorien') }}</span></label>
        <div class="language-row">
          <button
            v-for="s in gridCategories.categories.value"
            :key="s"
            type="button"
            class="language-btn"
            :class="{ active: form.sport === s }"
            @click="form.sport = form.sport === s ? '' : s"
          >
            {{ s }}
          </button>
        </div>
      </div>

      <div class="field option-card">
        <input type="checkbox" id="canExpire" v-model="form.canExpire" style="width:auto; margin-top:3px;" />
        <label for="canExpire" style="margin:0; text-transform:none; font-weight:400;">
          {{ $t('Kan utløpe') }}
          <div style="color:var(--text-dim); font-size:0.8rem; font-weight:400; margin-top:2px;">
            {{ $t('f.eks. "land etter nåværende folketall" - merk dette slik at det vises på Innsikt-siden «Kan utløpe» for jevnlig gjennomgang. La stå uten hake for faste fakta.') }}
          </div>
        </label>
      </div>

      <div v-if="form.sport" class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.entireCategoryPool" style="width:auto;" />
          {{ $t('Bruk alle emner i "') }}{{ form.sport }}{{ $t('" som utvalg') }}
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          {{ $t('Alle emner i "') }}{{ form.sport }}{{ $t('" kan også kastes, inkludert de som legges til senere - ingenting å importere på nytt. Alle som ikke er oppført under med en faktisk utsjekkverdi, gir 0 når de kastes. Du legger fortsatt til de som er verdt en faktisk verdi under.') }}
        </p>
      </div>

      <div v-if="form.sport" style="display:flex; gap:10px; flex-wrap:wrap; margin-bottom:10px;">
        <input
          type="text"
          v-model="athleteSearchTerm"
          :placeholder="$t('Søk etter emner på navn…')"
          style="flex:1; min-width:180px;"
        />
      </div>
      <div v-if="athleteSearchResults.length" class="guess-results" style="margin-bottom:10px;">
        <button
          v-for="a in athleteSearchResults"
          :key="a.id"
          class="guess-result-row"
          @click="addAthleteEntry(a)"
        >
          {{ a.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ a.team }}</span>
        </button>
      </div>

      <div style="margin-bottom:10px; display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
        <button class="btn btn-secondary btn-sm" :disabled="importingCsv" @click="triggerCsvUpload">
          {{ importingCsv ? $t('Importerer…') : $t('+ Importer fra CSV') }}
        </button>
        <input ref="csvInput" type="file" accept=".csv,text/csv" style="display:none;" @change="handleCsvFile" />
        <button v-if="form.sport" class="btn btn-secondary btn-sm" :disabled="!entries.length" @click="checkEntriesAgainstSubjects">
          {{ $t('Sjekk navn mot emner') }}
        </button>
      </div>
      <p class="page-subtitle" style="margin-top:-4px;">
        {{ $t('CSV-format: én rad per oppføring,') }} <code>name,value</code> - e.g. <code>Mohamed Salah,233</code>{{ $t('. Legger til (eller oppdaterer) det som står under, erstatter det ikke.') }}
      </p>

      <div v-if="csvUnmatchedNames.length" style="background:rgb(var(--gold-rgb) / 0.1); border:1px solid rgb(var(--gold-rgb) / 0.3); border-radius: 2px; padding:14px 16px; margin-bottom:14px;">
        <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:10px; flex-wrap:wrap;">
          <strong style="color:var(--gold);">
            {{ csvUnmatchedNames.length }} navn ble ikke funnet i "{{ form.sport }}"
          </strong>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-primary btn-sm" @click="showAddSubjectsModal = true">{{ $t('+ Legg til som emner') }}</button>
            <button class="btn btn-secondary btn-sm" @click="csvUnmatchedNames = []">{{ $t('Lukk§AdminFiveOhOneViewT') }}</button>
          </div>
        </div>
        <p class="page-subtitle" style="margin:6px 0 10px;">
          {{ $t('Disse oppføringene er uansett allerede i listen under - dette tilbyr bare å legge dem til i "') }}{{ form.sport }}{{ $t('" også, slik at andre spill kan finne dem som emner.') }}
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

      <div class="field" style="margin-top:20px;">
        <label>{{ $t('Oppføringer') }} <span class="picker-hint">{{ entries.length }} {{ $t('totalt') }}</span></label>
        <div v-if="!entries.length" class="empty-state" style="padding:20px;">{{ $t('Ingen oppføringer ennå - importer en CSV over, eller legg til én om gangen under.') }}</div>
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
          <div v-for="(e, idx) in pagedEntries" :key="idx" class="candidate-row">
            <span
              v-if="e.athleteId"
              class="tag"
              style="background:rgb(var(--teal-rgb) / 0.15); color:var(--teal); flex-shrink:0;"
              :title="$t('Koblet til et emne')"
            >{{ $t('✓ Emne') }}</span>
            <input
              type="text"
              v-model="e.name"
              :placeholder="$t('Navn')"
              style="flex:1;"
              @input="e.athleteId = null"
            />
            <input type="number" v-model.number="e.value" :placeholder="$t('Verdi')" style="width:100px;" />
            <button class="btn btn-danger btn-sm" @click="removeEntry(e)">✕</button>
          </div>
          <Pagination v-model:page="entryPage" :page-size="ENTRY_PAGE_SIZE" :total-items="filteredEntries.length" />
          </template>
        </div>
        <div style="display:flex; gap:10px; margin-top:10px;">
          <button class="btn btn-secondary btn-sm" @click="addBlankEntry">{{ $t('+ Legg til en manuelt') }}</button>
          <button class="btn btn-secondary btn-sm" :disabled="!entries.length" @click="downloadEntriesCsv">{{ $t('⇩ Last ned CSV') }}</button>
        </div>
      </div>

      <TranslationEditor v-if="editingId" type="FIVEOHONE" :entity-id="editingId" />

      <button class="btn btn-primary" :disabled="saving" @click="saveCategory" style="margin-top:20px;">
        {{ saving ? $t('Lagrer…') : $t('Lagre kategori') }}
      </button>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      :title="$t('Slette denne kategorien?')"
      :message="$t('«{0}» og alle {1} oppføringene fjernes.', [pendingDelete.title, pendingDelete.entryCount])"
      :busy="deleting"
      :busy-text="$t('Sletter…')"
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
import gridCategories from '../services/gridCategories'
import { downloadCsv, parseNameValueCsv } from '../services/csv'

const view = ref('list')
const categories = ref([])
const loading = ref(true)

// Search / sort / paginate the category list.
const {
  searchTerm, sortKey, sortDir, page: categoryPage,
  filtered: filteredCategories, paged: pagedCategories, sorts: categorySorts
} = useBoardList(categories, {
  pageSize: 10,
  searchFields: [c => c.title, c => c.description],
  sorts: [
    { key: 'title', label: _t('Tittel'), accessor: c => c.title },
    { key: 'entries', label: _t('Oppføringer'), accessor: c => c.entryCount, dir: 'desc' }
  ]
})
const error = ref('')
const saving = ref(false)
const editingId = ref(null)
const openingId = ref(null)
const deleting = ref(false)
const pendingDelete = ref(null)

const form = reactive({ title: '', description: '', sport: '', canExpire: false, entireCategoryPool: false })
const entries = ref([]) // [{ name, value, athleteId }]
const csvInput = ref(null)
const importingCsv = ref(false)
const csvUnmatchedNames = ref([]) // [{name, value}] - see AdminBullseyeView's csvUnmatchedNames
const showAddSubjectsModal = ref(false)
const athleteSearchTerm = ref('')
const athleteSearchResults = ref([])

const ENTRY_PAGE_SIZE = 25
const entryPage = ref(1)
const entryFilterTerm = ref('')
const filteredEntries = computed(() => {
  const term = entryFilterTerm.value.trim().toLowerCase()
  if (!term) return entries.value
  return entries.value.filter(e => e.name.toLowerCase().includes(term))
})
watch(entryFilterTerm, () => { entryPage.value = 1 })

const pagedEntries = computed(() => {
  const start = (entryPage.value - 1) * ENTRY_PAGE_SIZE
  return filteredEntries.value.slice(start, start + ENTRY_PAGE_SIZE)
})

onMounted(() => {
  loadCategories()
  gridCategories.ensureLoaded()
})

async function loadCategories() {
  loading.value = true
  error.value = ''
  try {
    categories.value = await api.adminListFiveOhOneCategories()
  } catch (e) {
    error.value = _t('Kunne ikke laste kategorier.')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.title = ''
  form.description = ''
  form.sport = ''
  form.canExpire = false
  form.entireCategoryPool = false
  entries.value = []
  csvUnmatchedNames.value = []
  showAddSubjectsModal.value = false
  showImportReviewModal.value = false
  pendingImportRows.value = []
  pendingImportPool.value = []
  athleteSearchTerm.value = ''
  athleteSearchResults.value = []
  entryFilterTerm.value = ''
  entryPage.value = 1
  error.value = ''
  view.value = 'form'
}

async function openEdit(id) {
  error.value = ''
  openingId.value = id
  try {
    const detail = await api.adminGetFiveOhOneCategory(id)
    editingId.value = id
    form.title = detail.title
    form.description = detail.description || ''
    form.sport = detail.sport || ''
    form.canExpire = detail.canExpire || false
    form.entireCategoryPool = detail.entireCategoryPool || false
    entries.value = detail.entries.map(e => ({ name: e.name, value: e.value, athleteId: e.athleteId || null }))
    csvUnmatchedNames.value = []
    showAddSubjectsModal.value = false
    showImportReviewModal.value = false
    pendingImportRows.value = []
    pendingImportPool.value = []
    athleteSearchTerm.value = ''
    athleteSearchResults.value = []
    entryFilterTerm.value = ''
    entryPage.value = 1
    view.value = 'form'
  } catch (e) {
    error.value = _t('Kunne ikke laste kategorien.')
  } finally {
    openingId.value = null
  }
}

function triggerCsvUpload() {
  csvInput.value?.click()
}

// Entries are free text and always accepted regardless of a match - unlike
// Bullseye, nothing here is skipped or blocked. A match just also links the
// entry to that subject (see the "✓ Subject" tag), so matching only runs
// when a subjects category is actually chosen. ImportEntriesModal still gets
// a look at every row first so the admin can drop rows they don't want,
// same review step Bullseye's CSV/501 import got.
const pendingImportRows = ref([])
const pendingImportLabel = ref('')
const pendingImportPool = ref([])
const showImportReviewModal = ref(false)

async function handleCsvFile(event) {
  const file = event.target.files?.[0]
  event.target.value = '' // lets the same file be re-selected after fixing it
  if (!file) return

  error.value = ''
  csvUnmatchedNames.value = []
  importingCsv.value = true
  try {
    const rows = parseNameValueCsv(await file.text())
    if (!rows.length) {
      toast.show(_t('Den CSV-filen hadde ingen gyldige "name,value"-rader å importere.'), 'error')
      return
    }

    let athleteByName = new Map()
    let pool = []
    if (form.sport) {
      pool = await api.adminSearchAthletes({ sport: form.sport })
      athleteByName = new Map(pool.map(a => [a.name.trim().toLowerCase(), a]))
    }

    pendingImportRows.value = rows.map(row => ({
      name: row.name,
      value: row.value,
      athlete: athleteByName.get(row.name.trim().toLowerCase()) || null
    }))
    pendingImportLabel.value = 'CSV import'
    pendingImportPool.value = pool
    showImportReviewModal.value = true
  } catch (e) {
    error.value = _t('Kunne ikke lese CSV-filen.')
  } finally {
    importingCsv.value = false
  }
}

// ImportEntriesModal confirmed this subset - only these actually get applied.
function applyImportSelection(selectedRows) {
  showImportReviewModal.value = false
  const byName = new Map(entries.value.map(e => [e.name.toLowerCase(), e]))
  let added = 0
  let updated = 0
  const unmatched = []
  for (const row of selectedRows) {
    const existing = byName.get(row.name.toLowerCase())
    if (existing) {
      existing.value = row.value
      if (row.athlete) existing.athleteId = row.athlete.id
      updated++
    } else {
      const fresh = { name: row.name, value: row.value, athleteId: row.athlete ? row.athlete.id : null }
      entries.value.push(fresh)
      byName.set(row.name.toLowerCase(), fresh)
      added++
    }
    if (form.sport && !row.athlete) {
      unmatched.push({ name: row.name, value: row.value })
    }
  }
  entryPage.value = 1
  entryFilterTerm.value = ''
  toast.show(_t('{0}: {1} oppføring(er) lagt til, {2} oppdatert.', [pendingImportLabel.value, added, updated]))
  csvUnmatchedNames.value = unmatched
}

// Runs the same name-matching CSV import already does, but against every
// current entry regardless of how it got there (manually typed, an older
// import, pre-dating this category having a sport at all) - the CSV path
// alone only ever checks the rows from that one file.
async function checkEntriesAgainstSubjects() {
  if (!form.sport || !entries.value.length) return
  error.value = ''
  try {
    const pool = await api.adminSearchAthletes({ sport: form.sport })
    const athleteByName = new Map(pool.map(a => [a.name.trim().toLowerCase(), a]))
    const unmatched = []
    for (const e of entries.value) {
      const athlete = athleteByName.get(e.name.trim().toLowerCase())
      if (athlete) {
        e.athleteId = athlete.id
      } else if (!e.athleteId) {
        unmatched.push({ name: e.name, value: e.value })
      }
    }
    csvUnmatchedNames.value = unmatched
    toast.show(
      unmatched.length ? _t('{0} navn er ikke emner i "{1}" ennå.', [unmatched.length, form.sport]) : _t('Alle oppføringer er allerede emner.'),
      unmatched.length ? 'error' : 'success'
    )
  } catch (e) {
    error.value = _t('Kunne ikke sjekke oppføringer mot emner.')
  }
}

// AddSubjectsModal created these as real subjects - link each matching entry
// to its new subject (by name; entries themselves were already in the list
// either way).
function onSubjectsAdded(createdWithValues) {
  const byName = new Map(entries.value.map(e => [e.name.trim().toLowerCase(), e]))
  for (const { athlete } of createdWithValues) {
    const existing = byName.get(athlete.name.trim().toLowerCase())
    if (existing) existing.athleteId = athlete.id
  }
  csvUnmatchedNames.value = []
  showAddSubjectsModal.value = false
}

// Same "name,value" shape the CSV importer above reads back in, so a
// downloaded file round-trips straight back through "+ Import from CSV".
function downloadEntriesCsv() {
  const rows = [['name', 'value'], ...entries.value.map(e => [e.name, e.value])]
  const filename = `${(form.title.trim() || '501').replace(/[^\w\- ]+/g, '').trim() || '501'}.csv`
  downloadCsv(filename, rows)
}

function addBlankEntry() {
  entries.value.push({ name: '', value: 0, athleteId: null })
  entryPage.value = Math.max(1, Math.ceil(entries.value.length / ENTRY_PAGE_SIZE))
}

let athleteSearchDebounce = null
watch(athleteSearchTerm, (val) => {
  clearTimeout(athleteSearchDebounce)
  if (!val || val.trim().length < 2) {
    athleteSearchResults.value = []
    return
  }
  athleteSearchDebounce = setTimeout(async () => {
    try {
      athleteSearchResults.value = await api.adminSearchAthletes({ sport: form.sport, name: val })
    } catch (e) {
      // non-critical
    }
  }, 250)
})

function addAthleteEntry(athlete) {
  if (entries.value.some(e => e.athleteId === athlete.id)) return
  entries.value.push({ name: athlete.name, value: 0, athleteId: athlete.id })
  entryPage.value = Math.max(1, Math.ceil(entries.value.length / ENTRY_PAGE_SIZE))
}

function removeEntry(e) {
  const idx = entries.value.indexOf(e)
  if (idx !== -1) entries.value.splice(idx, 1)
  const maxPage = Math.max(1, Math.ceil(entries.value.length / ENTRY_PAGE_SIZE))
  if (entryPage.value > maxPage) entryPage.value = maxPage
}

async function saveCategory() {
  error.value = ''
  const cleanEntries = entries.value
    .map(e => ({ name: e.name.trim(), value: e.value, athleteId: e.athleteId || null }))
    .filter(e => e.name && Number.isFinite(e.value))

  if (!form.title.trim()) {
    error.value = _t('Gi denne kategorien en tittel.')
    return
  }
  if (!cleanEntries.length) {
    error.value = _t('Legg til minst én oppføring.')
    return
  }

  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      description: form.description.trim() || null,
      sport: form.sport || null,
      canExpire: form.canExpire,
      entireCategoryPool: !!(form.sport && form.entireCategoryPool),
      entries: cleanEntries
    }
    if (editingId.value) {
      await api.adminUpdateFiveOhOneCategory(editingId.value, payload)
      toast.show(_t('Kategorien er oppdatert.'))
    } else {
      await api.adminCreateFiveOhOneCategory(payload)
      toast.show(_t('Kategorien er opprettet.'))
    }
    view.value = 'list'
    loadCategories()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke lagre kategorien.§AdminFiveOhOneViewJ')
  } finally {
    saving.value = false
  }
}

function requestDelete(c) {
  pendingDelete.value = c
}

async function doDelete() {
  const c = pendingDelete.value
  deleting.value = true
  try {
    await api.adminDeleteFiveOhOneCategory(c.id)
    pendingDelete.value = null
    toast.show(_t('Kategorien er slettet.'))
    loadCategories()
  } catch (e) {
    pendingDelete.value = null
    error.value = _t('Kunne ikke slette kategorien.')
  } finally {
    deleting.value = false
  }
}
</script>
