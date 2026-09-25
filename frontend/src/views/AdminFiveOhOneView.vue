<template>
  <div>
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
        <h1 style="margin:0;">501 categories <span v-if="!loading && categories.length" class="header-count">{{ categories.length }}</span></h1>
        <button class="btn btn-primary" @click="openCreate">+ New category</button>
      </div>
      <p class="page-subtitle">Each category is a ranked set of names and numbers - e.g. "Premier League appearances".</p>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
      <div v-else-if="!categories.length" class="empty-state friendly">No categories yet - create the first one.</div>

      <template v-else>
      <BoardListToolbar
        v-model:search="searchTerm"
        v-model:sort-key="sortKey"
        v-model:sort-dir="sortDir"
        :sorts="categorySorts"
        :total-count="categories.length"
        :filtered-count="filteredCategories.length"
        placeholder="Search categories by title or description…"
      />

      <div v-if="!filteredCategories.length" class="empty-state">No categories match your search.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="c in pagedCategories" :key="c.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              {{ c.title }}
              <span v-if="c.entireCategoryPool" class="tag" style="background:rgba(61,220,151,0.15); color:var(--teal); margin-left:6px;">Auto pool</span>
              <span v-if="c.canExpire" class="tag" style="background:rgba(255,196,0,0.15); color:var(--gold); margin-left:6px;">Can expire</span>
            </div>
            <div class="saved-quiz-meta">{{ c.entryCount }} entries<span v-if="c.description"> · {{ c.description }}</span></div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" :disabled="openingId === c.id" @click="openEdit(c.id)">
              {{ openingId === c.id ? 'Loading…' : 'Edit' }}
            </button>
            <button class="btn btn-danger btn-sm" :disabled="openingId === c.id" @click="requestDelete(c)">Delete</button>
          </div>
        </div>
      </div>

      <Pagination v-model:page="categoryPage" :page-size="10" :total-items="filteredCategories.length" />
      </template>
    </template>

    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <h1 style="margin:0;">{{ editingId ? 'Edit category' : 'New category' }}</h1>
        <button class="btn btn-secondary" @click="view = 'list'">← Back to list</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Title<input type="text" v-model="form.title" placeholder="e.g. Premier League appearances" /></label>
      </div>

      <div class="field">
        <label>Description <span class="picker-hint">optional flavor text</span><input type="text" v-model="form.description" placeholder="Shown to players before they pick this category" /></label>
      </div>

      <div class="field">
        <label>Subjects category <span class="picker-hint">optional - lets CSV-imported names be checked against this category's subjects</span></label>
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

      <div class="field" style="display:flex; align-items:flex-start; gap:8px;">
        <input type="checkbox" id="canExpire" v-model="form.canExpire" style="width:auto; margin-top:3px;" />
        <label for="canExpire" style="margin:0; text-transform:none; font-weight:400;">
          Can expire
          <div style="color:var(--text-dim); font-size:0.8rem; font-weight:400; margin-top:2px;">
            e.g. "countries by current population" - flag this so it shows up on the "Can expire" Insights page for periodic review. Leave unchecked for stable facts.
          </div>
        </label>
      </div>

      <div v-if="form.sport" class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.entireCategoryPool" style="width:auto;" />
          Use every subject in "{{ form.sport }}" as the pool
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          Every subject in "{{ form.sport }}" becomes throwable too, including ones added to it later -
          nothing to re-import. Anyone not listed below with a real checkout value just throws for 0.
          You still add the ones worth a real value below.
        </p>
      </div>

      <div v-if="form.sport" style="display:flex; gap:10px; flex-wrap:wrap; margin-bottom:10px;">
        <input
          type="text"
          v-model="athleteSearchTerm"
          placeholder="Search subjects by name…"
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
          {{ importingCsv ? 'Importing…' : '+ Import from CSV' }}
        </button>
        <input ref="csvInput" type="file" accept=".csv,text/csv" style="display:none;" @change="handleCsvFile" />
        <button v-if="form.sport" class="btn btn-secondary btn-sm" :disabled="!entries.length" @click="checkEntriesAgainstSubjects">
          Check names against subjects
        </button>
      </div>
      <p class="page-subtitle" style="margin-top:-4px;">
        CSV format: one row per entry, <code>name,value</code> - e.g. <code>Mohamed Salah,233</code>.
        Adds to (or updates) what's below, doesn't replace it.
      </p>

      <div v-if="csvUnmatchedNames.length" style="background:rgba(242,183,5,0.1); border:1px solid rgba(242,183,5,0.3); border-radius:var(--radius-md); padding:14px 16px; margin-bottom:14px;">
        <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:10px; flex-wrap:wrap;">
          <strong style="color:var(--gold);">
            {{ csvUnmatchedNames.length }} name{{ csvUnmatchedNames.length > 1 ? 's' : '' }} weren't found in "{{ form.sport }}"
          </strong>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-primary btn-sm" @click="showAddSubjectsModal = true">+ Add as subjects</button>
            <button class="btn btn-secondary btn-sm" @click="csvUnmatchedNames = []">Dismiss</button>
          </div>
        </div>
        <p class="page-subtitle" style="margin:6px 0 10px;">
          These entries are already in the list below either way - this just offers to add them to
          "{{ form.sport }}" too, so other games can find them as subjects.
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
        <label>Entries <span class="picker-hint">{{ entries.length }} total</span></label>
        <div v-if="!entries.length" class="empty-state" style="padding:20px;">No entries yet - import a CSV above, or add one at a time below.</div>
        <div v-else class="candidate-list">
          <input
            type="text"
            v-model="entryFilterTerm"
            placeholder="Find someone already in this list…"
            class="search-input"
            style="margin-bottom:12px;"
          />

          <div v-if="!filteredEntries.length" class="empty-state" style="padding:20px;">
            Nobody in this list matches "{{ entryFilterTerm }}".
          </div>

          <template v-else>
          <div v-for="(e, idx) in pagedEntries" :key="idx" class="candidate-row">
            <span
              v-if="e.athleteId"
              class="tag"
              style="background:rgba(61,220,151,0.15); color:var(--teal); flex-shrink:0;"
              title="Linked to a subject"
            >✓ Subject</span>
            <input
              type="text"
              v-model="e.name"
              placeholder="Name"
              style="flex:1;"
              @input="e.athleteId = null"
            />
            <input type="number" v-model.number="e.value" placeholder="Value" style="width:100px;" />
            <button class="btn btn-danger btn-sm" @click="removeEntry(e)">✕</button>
          </div>
          <Pagination v-model:page="entryPage" :page-size="ENTRY_PAGE_SIZE" :total-items="filteredEntries.length" />
          </template>
        </div>
        <div style="display:flex; gap:10px; margin-top:10px;">
          <button class="btn btn-secondary btn-sm" @click="addBlankEntry">+ Add one manually</button>
          <button class="btn btn-secondary btn-sm" :disabled="!entries.length" @click="downloadEntriesCsv">⇩ Download CSV</button>
        </div>
      </div>

      <button class="btn btn-primary" :disabled="saving" @click="saveCategory" style="margin-top:20px;">
        {{ saving ? 'Saving…' : 'Save category' }}
      </button>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this category?"
      :message="`'${pendingDelete.title}' and all ${pendingDelete.entryCount} entries will be removed.`"
      :busy="deleting"
      busy-text="Deleting…"
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
    { key: 'title', label: 'Title', accessor: c => c.title },
    { key: 'entries', label: 'Entries', accessor: c => c.entryCount, dir: 'desc' }
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
    error.value = 'Could not load categories.'
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
    error.value = 'Could not load that category.'
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
      toast.show('That CSV had no valid "name,value" rows to import.', 'error')
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
    error.value = 'Could not read that CSV file.'
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
  toast.show(`${pendingImportLabel.value}: ${added} entry(ies) added, ${updated} updated.`)
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
      unmatched.length ? `${unmatched.length} name(s) aren't subjects in "${form.sport}" yet.` : 'Every entry is already a subject.',
      unmatched.length ? 'error' : 'success'
    )
  } catch (e) {
    error.value = 'Could not check entries against subjects.'
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
    error.value = 'Give this category a title.'
    return
  }
  if (!cleanEntries.length) {
    error.value = 'Add at least one entry.'
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
      toast.show('Category updated.')
    } else {
      await api.adminCreateFiveOhOneCategory(payload)
      toast.show('Category created.')
    }
    view.value = 'list'
    loadCategories()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save that category.'
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
    toast.show('Category deleted.')
    loadCategories()
  } catch (e) {
    pendingDelete.value = null
    error.value = 'Could not delete that category.'
  } finally {
    deleting.value = false
  }
}
</script>
