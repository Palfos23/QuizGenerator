<template>
  <div>
    <!-- List view -->
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>Flashback <span v-if="!loading && years.length" class="header-count">{{ years.length }}</span></h1>
          <p class="page-subtitle">
            Create and manage Flashback years - a secret year, plus up to 5 clues about it, ordered hardest to easiest.
          </p>
        </div>
        <button class="btn btn-primary" @click="openCreate">+ Create year</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

      <div v-else-if="!years.length" class="empty-state friendly">
        No Flashback years yet. Create your first one here.
      </div>

      <template v-else>
      <BoardListToolbar
        v-model:search="searchTerm"
        v-model:sort-key="sortKey"
        v-model:sort-dir="sortDir"
        :sorts="yearSorts"
        :total-count="years.length"
        :filtered-count="filteredYears.length"
        placeholder="Search years by title or year…"
      />

      <div v-if="!filteredYears.length" class="empty-state">No years match your search.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="y in pagedYears" :key="y.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              {{ y.title }}
              <span v-if="y.excludedFromFlashback" class="tag" style="background:rgba(255,77,109,0.15); color:var(--coral); margin-left:6px;">Not in Flashback</span>
            </div>
            <div class="saved-quiz-meta">
              {{ y.year }} · {{ y.hintCount }} hint{{ y.hintCount === 1 ? '' : 's' }}
            </div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(y.id)">Edit</button>
            <button class="btn btn-secondary btn-sm" @click="duplicateYear(y.id)">⧉ Duplicate</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(y)">Delete</button>
          </div>
        </div>
      </div>

      <Pagination v-model:page="yearPage" :page-size="10" :total-items="filteredYears.length" />
      </template>
    </template>

    <!-- Builder view -->
    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <h1 style="margin:0;">{{ editingYearId ? 'Edit year' : 'Create year' }}</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Back to list</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Title <span class="picker-hint">admin-only label, not shown to players</span></label>
        <input type="text" v-model="form.title" placeholder="e.g. Moon landing" />
      </div>

      <div class="field" style="max-width:200px;">
        <label>Year <span class="picker-hint">what players are guessing</span></label>
        <input type="number" v-model.number="form.year" placeholder="1969" />
      </div>

      <div class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.excludedFromFlashback" style="width:auto;" />
          Exclude from Flashback
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          Use this once a year's clues are outdated or wrong - it stops being offered to
          Flashback's random pick without deleting it.
        </p>
      </div>

      <div class="field">
        <label>
          Hints <span class="picker-hint">up to 5, ordered hardest to easiest - reorder with the arrows</span>
        </label>

        <div v-if="!form.hints.length" class="empty-state" style="padding:20px;">
          No hints added yet - add the first (hardest) one below.
        </div>

        <div v-else style="display:flex; flex-direction:column; gap:8px;">
          <div v-for="(hint, i) in form.hints" :key="i" class="candidate-row">
            <div style="display:flex; align-items:center; gap:8px; flex:1; min-width:0;">
              <span class="tag" style="background:rgba(255,255,255,0.06); color:var(--text-dim); flex-shrink:0;">{{ i + 1 }}</span>
              <input type="text" v-model="form.hints[i]" :placeholder="`Hint ${i + 1}`" style="flex:1;" />
            </div>
            <div style="display:flex; gap:4px;">
              <button class="btn btn-secondary btn-sm icon-btn" :disabled="i === 0" @click="moveHint(i, -1)" aria-label="Move up">↑</button>
              <button class="btn btn-secondary btn-sm icon-btn" :disabled="i === form.hints.length - 1" @click="moveHint(i, 1)" aria-label="Move down">↓</button>
              <button class="btn btn-danger btn-sm" @click="form.hints.splice(i, 1)">✕</button>
            </div>
          </div>
        </div>

        <button
          class="btn btn-secondary btn-sm"
          style="margin-top:10px;"
          :disabled="form.hints.length >= 5"
          @click="form.hints.push('')"
        >+ Add hint</button>
      </div>

      <div style="display:flex; gap:10px; flex-wrap:wrap;">
        <button v-if="editingYearId" class="btn btn-secondary" @click="duplicateAsNewVersion">
          ⧉ Duplicate as new version
        </button>
        <button class="btn btn-primary" :disabled="saving" @click="saveYear">
          {{ saving ? 'Saving…' : 'Save year' }}
        </button>
      </div>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this year?"
      :message="`'${pendingDelete.title}' will be permanently removed.`"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import ConfirmModal from '../components/ConfirmModal.vue'
import Pagination from '../components/Pagination.vue'
import BoardListToolbar from '../components/BoardListToolbar.vue'
import { useBoardList } from '../composables/useBoardList'

const view = ref('list')
const years = ref([])
const loading = ref(true)

const {
  searchTerm, sortKey, sortDir, page: yearPage,
  filtered: filteredYears, paged: pagedYears, sorts: yearSorts
} = useBoardList(years, {
  pageSize: 10,
  searchFields: [y => y.title, y => String(y.year)],
  sorts: [
    { key: 'title', label: 'Title', accessor: y => y.title },
    { key: 'year', label: 'Year', accessor: y => y.year, dir: 'desc' },
    { key: 'hints', label: 'Hints', accessor: y => y.hintCount, dir: 'desc' }
  ]
})
const error = ref('')
const saving = ref(false)
const pendingDelete = ref(null)
const editingYearId = ref(null)

const form = reactive({
  title: '',
  year: null,
  excludedFromFlashback: false,
  hints: ['']
})

onMounted(loadYears)

async function loadYears() {
  loading.value = true
  error.value = ''
  try {
    years.value = await api.adminListFlashbackYears()
  } catch (e) {
    error.value = 'Could not load Flashback years.'
  } finally {
    loading.value = false
  }
}

function moveHint(idx, direction) {
  const target = idx + direction
  if (target < 0 || target >= form.hints.length) return
  const list = form.hints
  const [moved] = list.splice(idx, 1)
  list.splice(target, 0, moved)
}

function resetForm() {
  form.title = ''
  form.year = null
  form.excludedFromFlashback = false
  form.hints = ['']
}

function openCreate() {
  resetForm()
  editingYearId.value = null
  view.value = 'builder'
}

async function openEdit(id) {
  error.value = ''
  try {
    const detail = await api.adminGetFlashbackYear(id)
    form.title = detail.title
    form.year = detail.year
    form.excludedFromFlashback = detail.excludedFromFlashback
    form.hints = detail.hints.length ? [...detail.hints] : ['']

    editingYearId.value = id
    view.value = 'builder'
  } catch (e) {
    error.value = 'Could not load that year.'
  }
}

// One-click duplicate straight from the list - loads the year into the
// builder exactly like Edit would, then immediately detaches it into a new
// unsaved copy via duplicateAsNewVersion, same pattern as Bullseye's list.
async function duplicateYear(id) {
  await openEdit(id)
  if (editingYearId.value === id) duplicateAsNewVersion()
}

// Keeps every current form field exactly as-is - only clearing the editing
// id, so the next save creates a brand new year via POST instead of
// overwriting the original via PUT.
function duplicateAsNewVersion() {
  editingYearId.value = null
  form.title = form.title + ' (updated)'
  form.excludedFromFlashback = false
  toast.show('Now editing a new duplicate - the original year is untouched. Remember to save this copy.')
}

async function saveYear() {
  error.value = ''
  if (!form.title.trim() || form.year === null) {
    error.value = 'Title and year are both required.'
    return
  }
  const hints = form.hints.map(h => h.trim()).filter(Boolean)
  if (!hints.length) {
    error.value = 'Add at least 1 hint.'
    return
  }
  if (hints.length > 5) {
    error.value = 'A year can have at most 5 hints.'
    return
  }

  const payload = {
    title: form.title,
    year: form.year,
    excludedFromFlashback: form.excludedFromFlashback,
    hints
  }

  saving.value = true
  try {
    if (editingYearId.value) {
      await api.adminUpdateFlashbackYear(editingYearId.value, payload)
    } else {
      await api.adminCreateFlashbackYear(payload)
    }
    toast.show('Year saved.')
    view.value = 'list'
    loadYears()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save this year.'
  } finally {
    saving.value = false
  }
}

function requestDelete(y) {
  pendingDelete.value = y
}

async function doDelete() {
  const y = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.adminDeleteFlashbackYear(y.id)
    toast.show('Year deleted.')
    loadYears()
  } catch (e) {
    error.value = 'Could not delete that year.'
  }
}
</script>
