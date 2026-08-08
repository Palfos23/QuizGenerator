<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
      <div>
        <h1>Subjects</h1>
        <p class="page-subtitle">Whoever or whatever a grid is about - people, movies, countries, anything - used to build weekly grid candidate pools.</p>
      </div>
      <div style="display:flex; gap:8px;">
        <button class="btn btn-secondary" @click="triggerFilePicker">📄 Import CSV</button>
        <button class="btn btn-primary" @click="openCreate">+ Add subject</button>
      </div>
      <input ref="fileInput" type="file" accept=".csv,text/csv" style="display:none;" @change="onFileSelected" />
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="filter-bar">
      <div class="field" style="margin-bottom:0; flex:2; min-width:200px;">
        <label>Search</label>
        <input type="text" v-model="searchText" placeholder="Search name or group…" />
      </div>
      <div class="field" style="margin-bottom:0; flex:1; min-width:160px;">
        <label>Category</label>
        <select v-model="sportFilter">
          <option value="ALL">All categories</option>
          <option v-for="s in gridCategories.categories.value" :key="s" :value="s">{{ s }}</option>
        </select>
      </div>
    </div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="!athletes.length" class="empty-state friendly">
      No subjects yet. Add a few here before building a weekly grid.
    </div>

    <div v-else-if="!filteredAthletes.length" class="empty-state">No subjects match those filters.</div>

    <div v-else class="table-scroll">
      <table class="table">
        <thead>
          <tr>
            <th>Photo</th>
            <th>Name</th>
            <th>Category</th>
            <th>Group</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in pagedAthletes" :key="a.id">
            <td><img v-if="a.photoUrl" :src="a.photoUrl" alt="" class="club-logo-thumb" /></td>
            <td>{{ a.name }}</td>
            <td>{{ sportLabel(a.sport) }}</td>
            <td>{{ a.team }}</td>
            <td style="white-space:nowrap;">
              <button class="btn btn-secondary btn-sm" @click="openEdit(a)">Edit</button>
              <button class="btn btn-danger btn-sm" style="margin-left:6px;" @click="requestDelete(a)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>

      <Pagination v-model:page="page" :page-size="PAGE_SIZE" :total-items="filteredAthletes.length" />
    </div>

    <AthleteFormModal
      v-if="showModal"
      :athlete="editingAthlete"
      @close="showModal = false"
      @saved="onSaved"
    />

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this subject?"
      :message="`'${pendingDelete.name}' will be removed from the roster.`"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />

    <div v-if="pendingGridUsage" class="modal-backdrop" @click.self="pendingGridUsage = null">
      <div class="modal">
        <h2 style="margin-top:0;">'{{ pendingGridUsage.athlete.name }}' is used in {{ pendingGridUsage.usage.length }} grid(s)</h2>
        <p class="page-subtitle" style="margin-top:0;">
          Deleting this subject first removes them from every grid listed below.
        </p>
        <ul style="margin:0 0 16px; padding-left:20px; line-height:1.8;">
          <li v-for="u in pendingGridUsage.usage" :key="u.gridId">
            {{ u.gridTitle }}
            <span v-if="u.isCorrectAnswer" style="color:var(--coral); font-weight:600;"> - a correct answer here, not just a decoy</span>
          </li>
        </ul>
        <p v-if="pendingGridUsage.usage.some(u => u.isCorrectAnswer)" style="color:var(--coral); font-size:0.9rem;">
          For the grid(s) marked above, this genuinely changes that grid's answer key - including for anyone who's
          already played it. If that grid is meant to stay exactly as-is, consider duplicating it as a new version
          and editing the copy instead, rather than deleting this subject outright.
        </p>
        <div style="display:flex; gap:10px; justify-content:flex-end;">
          <button class="btn btn-secondary" @click="pendingGridUsage = null">Cancel</button>
          <button class="btn btn-danger" @click="confirmRemoveFromGridsAndDelete">Remove from grids and delete</button>
        </div>
      </div>
    </div>

    <div v-if="showImportPreview" class="modal-backdrop" @click.self="closeImportPreview">
      <div class="modal" style="max-width:640px;">
        <h2 style="margin-top:0;">Import from CSV</h2>
        <p class="page-subtitle">
          Expected columns: <code>name, category, group, photoUrl</code> (group and photoUrl optional) - "group" is
          whatever fits the category: team, director, nationality, studio, anything.
          Category must be one of: {{ gridCategories.categories.value.join(', ') }}.
        </p>

        <div v-if="importRows.length" style="max-height:320px; overflow-y:auto; margin-bottom:16px;">
          <table class="table" style="min-width:0; table-layout:fixed;">
            <thead>
              <tr><th style="width:28%;">Name</th><th style="width:20%;">Category</th><th style="width:28%;">Group</th><th>Status</th></tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in importRows" :key="i" :class="{ 'tension-row-trap': !row.valid }">
                <td>{{ row.name }}</td>
                <td>{{ row.sport }}</td>
                <td>{{ row.team }}</td>
                <td style="color: var(--coral); font-size:0.85rem;">{{ row.valid ? '' : row.error }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <p style="font-size:0.9rem; color:var(--text-dim);">
          {{ validImportCount }} of {{ importRows.length }} row(s) look valid and will be imported.
          {{ importRows.length - validImportCount }} will be skipped.
        </p>

        <div style="display:flex; gap:10px; justify-content:flex-end;">
          <button class="btn btn-secondary" @click="closeImportPreview">Cancel</button>
          <button class="btn btn-primary" :disabled="!validImportCount || importing" @click="confirmImport">
            {{ importing ? `Importing… (${importProgress}/${validImportCount})` : `Import ${validImportCount} subject(s)` }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import AthleteFormModal from '../components/AthleteFormModal.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import Pagination from '../components/Pagination.vue'
import { sportLabel } from '../constants'
import gridCategories from '../services/gridCategories'

const athletes = ref([])
const loading = ref(true)
const error = ref('')
const searchText = ref('')
const sportFilter = ref('ALL')

const showModal = ref(false)
const editingAthlete = ref(null)
const pendingDelete = ref(null)
const pendingGridUsage = ref(null)

const fileInput = ref(null)
const showImportPreview = ref(false)
const importRows = ref([])
const importing = ref(false)
const importProgress = ref(0)

const validImportCount = computed(() => importRows.value.filter(r => r.valid).length)

function triggerFilePicker() {
  fileInput.value?.click()
}

function parseCsvLine(line) {
  const cells = []
  let current = ''
  let inQuotes = false
  for (let i = 0; i < line.length; i++) {
    const char = line[i]
    if (inQuotes) {
      if (char === '"' && line[i + 1] === '"') { current += '"'; i++ }
      else if (char === '"') { inQuotes = false }
      else { current += char }
    } else if (char === '"') {
      inQuotes = true
    } else if (char === ',') {
      cells.push(current)
      current = ''
    } else {
      current += char
    }
  }
  cells.push(current)
  return cells.map(c => c.trim())
}

function onFileSelected(event) {
  const file = event.target.files[0]
  event.target.value = '' // allow picking the same file again later
  if (!file) return

  const reader = new FileReader()
  reader.onload = () => {
    const lines = String(reader.result).split(/\r?\n/).filter(l => l.trim().length > 0)
    if (!lines.length) {
      error.value = 'That file appears to be empty.'
      return
    }

    const header = parseCsvLine(lines[0]).map(h => h.toLowerCase())
    const nameIdx = header.indexOf('name')
    const sportIdx = header.indexOf('category') !== -1 ? header.indexOf('category') : header.indexOf('sport')
    const teamIdx = header.indexOf('group') !== -1 ? header.indexOf('group') : header.indexOf('team')
    const photoIdx = header.indexOf('photourl')

    if (nameIdx === -1 || sportIdx === -1) {
      error.value = 'CSV needs at least "name" and "sport" columns.'
      return
    }

    const validSports = gridCategories.categories.value
    const existingKeys = new Set(athletes.value.map(a => `${a.sport}::${a.name.trim().toLowerCase()}`))
    const seenInFile = new Set()

    importRows.value = lines.slice(1).map(line => {
      const cells = parseCsvLine(line)
      const name = cells[nameIdx] || ''
      const rawSport = (cells[sportIdx] || '').trim()
      // Case-insensitive match against the real category list, normalized to
      // that category's actual stored casing - so "football" in a CSV still
      // correctly matches the "Football" category rather than being rejected.
      const matchedSport = validSports.find(s => s.toLowerCase() === rawSport.toLowerCase())
      const sport = matchedSport || rawSport
      const team = teamIdx !== -1 ? (cells[teamIdx] || '') : ''
      const photoUrl = photoIdx !== -1 ? (cells[photoIdx] || '') : ''
      const key = `${sport}::${name.trim().toLowerCase()}`

      let rowError = ''
      if (!name) rowError = 'Missing name'
      else if (!matchedSport) rowError = `Invalid category (must be one of: ${validSports.join(', ')})`
      else if (existingKeys.has(key)) rowError = 'Duplicate - already exists'
      else if (seenInFile.has(key)) rowError = 'Duplicate - repeated in this file'

      if (!rowError) seenInFile.add(key)

      return { name, sport, team, photoUrl, valid: !rowError, error: rowError }
    })
    showImportPreview.value = true
  }
  reader.readAsText(file)
}

function closeImportPreview() {
  showImportPreview.value = false
  importRows.value = []
}

async function confirmImport() {
  importing.value = true
  importProgress.value = 0
  let failed = 0
  const toImport = importRows.value.filter(r => r.valid)

  for (const row of toImport) {
    try {
      await api.adminCreateAthlete({
        name: row.name, sport: row.sport, team: row.team || null, photoUrl: row.photoUrl || null
      })
    } catch (e) {
      failed++
    }
    importProgress.value++
  }

  importing.value = false
  showImportPreview.value = false
  importRows.value = []
  loadAthletes()
  toast.show(failed
    ? `Imported ${toImport.length - failed} of ${toImport.length} - ${failed} failed.`
    : `Imported ${toImport.length} subject(s).`)
}

const filteredAthletes = computed(() => {
  const term = searchText.value.trim().toLowerCase()
  return athletes.value.filter(a => {
    if (sportFilter.value !== 'ALL' && a.sport !== sportFilter.value) return false
    if (term && !`${a.name} ${a.team || ''}`.toLowerCase().includes(term)) return false
    return true
  })
})

const PAGE_SIZE = 15
const page = ref(1)
const pagedAthletes = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return filteredAthletes.value.slice(start, start + PAGE_SIZE)
})
watch([searchText, sportFilter], () => { page.value = 1 })

onMounted(() => {
  loadAthletes()
  gridCategories.ensureLoaded()
})

async function loadAthletes() {
  loading.value = true
  error.value = ''
  try {
    athletes.value = await api.adminSearchAthletes()
  } catch (e) {
    error.value = 'Could not load subjects.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingAthlete.value = null
  showModal.value = true
}

function openEdit(a) {
  editingAthlete.value = a
  showModal.value = true
}

function onSaved() {
  showModal.value = false
  toast.show('Subject saved.')
  loadAthletes()
}

function requestDelete(a) {
  pendingDelete.value = a
}

async function doDelete() {
  const a = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.adminDeleteAthlete(a.id)
    toast.show('Subject deleted.')
    loadAthletes()
  } catch (e) {
    if (e.response?.status === 400) {
      try {
        const usage = await api.adminGetAthleteGridUsage(a.id)
        pendingGridUsage.value = { athlete: a, usage }
        return
      } catch (e2) {
        // fall through to the generic error below
      }
    }
    error.value = e.response?.data?.message || 'Could not delete that subject.'
  }
}

async function confirmRemoveFromGridsAndDelete() {
  const { athlete } = pendingGridUsage.value
  pendingGridUsage.value = null
  error.value = ''
  try {
    await api.adminDeleteAthlete(athlete.id, true)
    toast.show('Subject removed from those grids and deleted.')
    loadAthletes()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not delete that subject.'
  }
}
</script>
