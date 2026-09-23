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
              <span v-if="c.canExpire" class="tag" style="background:rgba(255,196,0,0.15); color:var(--gold); margin-left:6px;">Can expire</span>
            </div>
            <div class="saved-quiz-meta">{{ c.entryCount }} entries<span v-if="c.description"> · {{ c.description }}</span></div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(c.id)">Edit</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(c)">Delete</button>
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
        <label>Subjects category <span class="picker-hint">optional - lets bulk-pasted names be checked against this category's subjects</span></label>
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

      <details class="advanced-disclosure">
        <summary>Bulk paste entries</summary>
        <div style="margin-top:12px;">
          <p class="page-subtitle" style="margin-top:0;">One per line, as <code>Name, Number</code> - e.g. <code>Mohamed Salah, 233</code>. Adds to (or updates) what's below, doesn't replace it.</p>
          <textarea v-model="bulkText" rows="6" placeholder="Mohamed Salah, 233&#10;Harry Kane, 189"></textarea>
          <button class="btn btn-secondary btn-sm" style="margin-top:8px;" @click="applyBulkPaste">Add to list</button>
        </div>
      </details>

      <div v-if="bulkUnmatchedNames.length" style="background:rgba(242,183,5,0.1); border:1px solid rgba(242,183,5,0.3); border-radius:var(--radius-md); padding:14px 16px; margin-top:14px;">
        <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:10px; flex-wrap:wrap;">
          <strong style="color:var(--gold);">
            {{ bulkUnmatchedNames.length }} name{{ bulkUnmatchedNames.length > 1 ? 's' : '' }} weren't found in "{{ form.sport }}"
          </strong>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-primary btn-sm" @click="showAddSubjectsModal = true">+ Add as subjects</button>
            <button class="btn btn-secondary btn-sm" @click="bulkUnmatchedNames = []">Dismiss</button>
          </div>
        </div>
        <p class="page-subtitle" style="margin:6px 0 10px;">
          These entries are already in the list below either way - this just offers to add them to
          "{{ form.sport }}" too, so other games can find them as subjects.
        </p>
        <ul style="margin:0; padding-left:20px; max-height:200px; overflow-y:auto; line-height:1.8;">
          <li v-for="row in bulkUnmatchedNames" :key="row.name">{{ row.name }}</li>
        </ul>
      </div>

      <AddSubjectsModal
        v-if="showAddSubjectsModal"
        :names="bulkUnmatchedNames"
        :sport="form.sport"
        @close="showAddSubjectsModal = false"
        @added="onSubjectsAdded"
      />

      <div class="field" style="margin-top:20px;">
        <label>Entries <span class="picker-hint">{{ entries.length }} total</span></label>
        <div v-if="!entries.length" class="empty-state" style="padding:20px;">No entries yet - paste some above, or add one at a time below.</div>
        <div v-else class="candidate-list">
          <div v-for="(e, idx) in pagedEntries" :key="idx" class="candidate-row">
            <input type="text" v-model="e.name" placeholder="Name" style="flex:1;" />
            <input type="number" v-model.number="e.value" placeholder="Value" style="width:100px;" />
            <button class="btn btn-danger btn-sm" @click="removeEntry(e)">✕</button>
          </div>
          <Pagination v-model:page="entryPage" :page-size="ENTRY_PAGE_SIZE" :total-items="entries.length" />
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
import Pagination from '../components/Pagination.vue'
import BoardListToolbar from '../components/BoardListToolbar.vue'
import { useBoardList } from '../composables/useBoardList'
import gridCategories from '../services/gridCategories'
import { downloadCsv } from '../services/csv'

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
const pendingDelete = ref(null)

const form = reactive({ title: '', description: '', sport: '', canExpire: false })
const entries = ref([]) // [{ name, value }]
const bulkText = ref('')
const bulkUnmatchedNames = ref([]) // [{name, value}] - see AdminBullseyeView's csvUnmatchedNames
const showAddSubjectsModal = ref(false)

const ENTRY_PAGE_SIZE = 25
const entryPage = ref(1)
const pagedEntries = computed(() => {
  const start = (entryPage.value - 1) * ENTRY_PAGE_SIZE
  return entries.value.slice(start, start + ENTRY_PAGE_SIZE)
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
  entries.value = []
  bulkText.value = ''
  bulkUnmatchedNames.value = []
  showAddSubjectsModal.value = false
  entryPage.value = 1
  error.value = ''
  view.value = 'form'
}

async function openEdit(id) {
  error.value = ''
  try {
    const detail = await api.adminGetFiveOhOneCategory(id)
    editingId.value = id
    form.title = detail.title
    form.description = detail.description || ''
    form.sport = detail.sport || ''
    form.canExpire = detail.canExpire || false
    entries.value = detail.entries.map(e => ({ name: e.name, value: e.value }))
    bulkText.value = ''
    bulkUnmatchedNames.value = []
    showAddSubjectsModal.value = false
    entryPage.value = 1
    view.value = 'form'
  } catch (e) {
    error.value = 'Could not load that category.'
  }
}

async function applyBulkPaste() {
  const lines = bulkText.value.split('\n').map(l => l.trim()).filter(Boolean)
  const byName = new Map(entries.value.map(e => [e.name.toLowerCase(), e]))
  const parsed = []
  for (const line of lines) {
    const commaIdx = line.lastIndexOf(',')
    if (commaIdx === -1) continue
    const name = line.slice(0, commaIdx).trim()
    const value = parseInt(line.slice(commaIdx + 1).trim(), 10)
    if (!name || Number.isNaN(value)) continue
    parsed.push({ name, value })
    const existing = byName.get(name.toLowerCase())
    if (existing) {
      existing.value = value
    } else {
      const fresh = { name, value }
      entries.value.push(fresh)
      byName.set(name.toLowerCase(), fresh)
    }
  }
  bulkText.value = ''
  toast.show(`Added/updated ${lines.length} line(s).`)

  // Entries above are free text and always accepted regardless of this check -
  // unlike Bullseye, nothing here is skipped or blocked. This is purely a
  // convenience prompt to also grow the shared Subjects list other games draw
  // from, so it only runs when a subjects category is actually chosen.
  bulkUnmatchedNames.value = []
  if (form.sport && parsed.length) {
    try {
      const pool = await api.adminSearchAthletes({ sport: form.sport })
      const known = new Set(pool.map(a => a.name.trim().toLowerCase()))
      bulkUnmatchedNames.value = parsed.filter(row => !known.has(row.name.trim().toLowerCase()))
    } catch (e) {
      // non-critical - the subject check is just a convenience prompt
    }
  }
}

// AddSubjectsModal created these as real subjects - the entries themselves are
// already in the list (added above regardless of match), so there's nothing
// to wire in here beyond clearing the prompt.
function onSubjectsAdded() {
  bulkUnmatchedNames.value = []
  showAddSubjectsModal.value = false
}

// Same "name,value" shape the bulk-paste box above reads, so a downloaded
// file pastes straight back in.
function downloadEntriesCsv() {
  const rows = [['name', 'value'], ...entries.value.map(e => [e.name, e.value])]
  const filename = `${(form.title.trim() || '501').replace(/[^\w\- ]+/g, '').trim() || '501'}.csv`
  downloadCsv(filename, rows)
}

function addBlankEntry() {
  entries.value.push({ name: '', value: 0 })
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
    .map(e => ({ name: e.name.trim(), value: e.value }))
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
    const payload = { title: form.title.trim(), description: form.description.trim() || null, sport: form.sport || null, canExpire: form.canExpire, entries: cleanEntries }
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
  pendingDelete.value = null
  try {
    await api.adminDeleteFiveOhOneCategory(c.id)
    toast.show('Category deleted.')
    loadCategories()
  } catch (e) {
    error.value = 'Could not delete that category.'
  }
}
</script>
