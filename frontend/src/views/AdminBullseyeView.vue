<template>
  <div>
    <!-- List view -->
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>Bullseye</h1>
          <p class="page-subtitle">
            Create and manage Bullseye questions - a target number, and every
            athlete's real stat value for it.
          </p>
        </div>
        <button class="btn btn-primary" @click="openCreate">+ Create question</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

      <div v-else-if="!questions.length" class="empty-state friendly">
        No Bullseye questions yet. Create your first one here.
      </div>

      <div v-else class="saved-quiz-list">
        <div v-for="q in questions" :key="q.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              {{ q.title }}
              <span v-if="q.excludedFromBullseye" class="tag" style="background:rgba(255,77,109,0.15); color:var(--coral); margin-left:6px;">Not in Bullseye</span>
              <span v-if="q.entireCategoryPool" class="tag" style="background:rgba(61,220,151,0.15); color:var(--teal); margin-left:6px;">Auto pool</span>
            </div>
            <div class="saved-quiz-meta">
              {{ sportLabel(q.sport) }} · {{ q.entryCount }} answers · "{{ q.targetValue }} {{ q.statLabel }}"
            </div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(q.id)">Edit</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(q)">Delete</button>
          </div>
        </div>
      </div>
    </template>

    <!-- Builder view -->
    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <h1 style="margin:0;">{{ editingQuestionId ? 'Edit question' : 'Create question' }}</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Back to list</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Title <span class="picker-hint">admin-only label, not shown to players</span></label>
        <input type="text" v-model="form.title" placeholder="e.g. Premier League 2024/25 goalscorers" />
      </div>

      <div class="field">
        <label>Category <span class="picker-hint">choose one</span></label>
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
          <label>Target number</label>
          <input type="number" v-model.number="form.targetValue" placeholder="13" />
        </div>
        <div style="flex:2; min-width:220px;">
          <label>Rest of the prompt</label>
          <input type="text" v-model="form.statLabel" placeholder="goals in the Premier League 2024/25" />
        </div>
      </div>
      <p class="page-subtitle" style="margin-top:-8px;">
        Players will see: <strong>"{{ form.targetValue ?? '…' }} {{ form.statLabel || '…' }}"</strong>
      </p>

      <div class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.entireCategoryPool" style="width:auto;" />
          Use every subject in "{{ form.sport || '…' }}" as the pool
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          Every subject in this category becomes guessable automatically, including ones added to it
          later - nothing to re-import. Anyone not listed below as an answer just resolves to 0 if
          picked. You still add the ones with a real stat value below.
        </p>
      </div>

      <div class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.excludedFromBullseye" style="width:auto;" />
          Exclude from Bullseye
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          Use this once a question's stat values are outdated - it stops being
          offered to Bullseye's random or manual pick without deleting it.
        </p>
      </div>

      <div class="field">
        <label>
          {{ form.entireCategoryPool ? 'Answers with a known value' : 'Answers' }}
          <span class="picker-hint">
            {{ form.entireCategoryPool ? 'everyone else in the category is guessable too, resolving to 0' : 'every athlete a player could name, with their real stat value' }}
          </span>
        </label>

        <div style="display:flex; gap:10px; flex-wrap:wrap; margin-bottom:10px;">
          <input
            type="text"
            v-model="athleteSearchTerm"
            placeholder="Search subjects by name…"
            style="flex:1; min-width:180px;"
          />
        </div>

        <div style="margin-bottom:10px; display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
          <button class="btn btn-secondary btn-sm" :disabled="!form.sport || importingAllInCategory" @click="importAllInCategory">
            {{ importingAllInCategory ? 'Importing…' : `+ Import every subject in "${form.sport || '…'}"` }}
          </button>
          <button class="btn btn-secondary btn-sm" :disabled="!form.sport || importingCsv" @click="triggerCsvUpload">
            {{ importingCsv ? 'Importing…' : '+ Import from CSV' }}
          </button>
          <input ref="csvInput" type="file" accept=".csv,text/csv" style="display:none;" @change="handleCsvFile" />
        </div>
        <p class="page-subtitle" style="margin-top:-4px;">
          CSV format: one row per answer, <code>name,value</code> - e.g. <code>Erling Haaland,27</code>.
          Names are matched against existing subjects in "{{ form.sport || '…' }}"; anything unmatched is skipped
          so you can add that subject first and re-import. Every row here still needs a value filled in before saving.
        </p>

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
          No answers added yet - search above to add subjects with a real stat value.
        </div>

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
          <div v-for="e in pagedEntries" :key="e.athleteId" class="candidate-row">
            <div style="font-weight:600;">
              {{ e.name }} <span style="color:var(--text-dim); font-weight:400; font-size:0.85rem;">{{ e.team }}</span>
            </div>
            <div style="display:flex; gap:8px; align-items:center;">
              <input type="number" v-model.number="e.statValue" placeholder="Stat value" style="width:110px;" />
            </div>
            <button class="btn btn-danger btn-sm" @click="removeEntry(e)">✕</button>
          </div>

          <Pagination v-model:page="entryPage" :page-size="ENTRY_PAGE_SIZE" :total-items="filteredEntries.length" />
          </template>
        </div>
      </div>

      <div style="display:flex; gap:10px; flex-wrap:wrap;">
        <button class="btn btn-primary" :disabled="saving" @click="saveQuestion">
          {{ saving ? 'Saving…' : 'Save question' }}
        </button>
      </div>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this question?"
      :message="`'${pendingDelete.title}' will be permanently removed.`"
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
import Pagination from '../components/Pagination.vue'
import { sportLabel } from '../constants'
import gridCategories from '../services/gridCategories'

const view = ref('list')
const questions = ref([])
const loading = ref(true)
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
  entireCategoryPool: false
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
    error.value = 'Could not load Bullseye questions.'
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

const importingAllInCategory = ref(false)
async function importAllInCategory() {
  if (!form.sport) return
  error.value = ''
  importingAllInCategory.value = true
  try {
    const all = await api.adminSearchAthletes({ sport: form.sport })
    const added = addEntriesBulk(all)
    toast.show(`Imported ${added} subject(s) from "${form.sport}" (${all.length - added} were already in this list). Fill in each one's stat value, or remove the ones you don't need.`)
  } catch (e) {
    error.value = 'Could not import subjects for that category.'
  } finally {
    importingAllInCategory.value = false
  }
}

const csvInput = ref(null)
const importingCsv = ref(false)

function triggerCsvUpload() {
  csvInput.value?.click()
}

// Matches each CSV row's name against subjects already in this category (same
// pool importAllInCategory draws from) - deliberately doesn't create new
// Athlete records on the fly, since a typo in the CSV would otherwise silently
// create a junk duplicate subject instead of surfacing as a skipped row.
async function handleCsvFile(event) {
  const file = event.target.files?.[0]
  event.target.value = '' // lets the same file be re-selected after fixing it
  if (!file || !form.sport) return

  error.value = ''
  importingCsv.value = true
  try {
    const rows = parseCsv(await file.text())
    if (!rows.length) {
      toast.show('That CSV had no valid "name,value" rows to import.', 'error')
      return
    }

    const pool = await api.adminSearchAthletes({ sport: form.sport })
    const athleteByName = new Map(pool.map(a => [a.name.trim().toLowerCase(), a]))

    let added = 0
    let updated = 0
    const unmatched = []
    for (const row of rows) {
      const athlete = athleteByName.get(row.name.trim().toLowerCase())
      if (!athlete) {
        unmatched.push(row.name)
        continue
      }
      const existing = entries.value.find(e => e.athleteId === athlete.id)
      if (existing) {
        existing.statValue = row.value
        updated++
      } else {
        entries.value.push({ athleteId: athlete.id, name: athlete.name, team: athlete.team, statValue: row.value })
        added++
      }
    }
    entryPage.value = 1
    entryFilterTerm.value = ''

    let message = `CSV import: ${added} answer(s) added, ${updated} updated.`
    if (unmatched.length) {
      message += ` ${unmatched.length} name(s) not found in "${form.sport}" and skipped: ${unmatched.slice(0, 5).join(', ')}${unmatched.length > 5 ? ', …' : ''}.`
    }
    toast.show(message, unmatched.length ? 'error' : 'success')
  } catch (e) {
    error.value = 'Could not read that CSV file.'
  } finally {
    importingCsv.value = false
  }
}

// Deliberately minimal - just "name,value" rows, no quoted/embedded commas,
// which is all this import needs. Skips a header row if the first row's
// second column isn't a number, and skips blank lines.
function parseCsv(text) {
  const lines = text.split(/\r?\n/).map(l => l.trim()).filter(Boolean)
  let start = 0
  if (lines.length) {
    const firstCols = lines[0].split(',')
    if (firstCols.length >= 2 && Number.isNaN(Number(firstCols[1].trim()))) {
      start = 1 // first row looks like a header - skip it
    }
  }
  const rows = []
  for (let i = start; i < lines.length; i++) {
    const cols = lines[i].split(',')
    if (cols.length < 2) continue
    const name = cols[0].trim().replace(/^"|"$/g, '')
    const value = Number(cols[1].trim())
    if (!name || Number.isNaN(value)) continue
    rows.push({ name, value })
  }
  return rows
}

function resetForm() {
  form.title = ''
  form.sport = gridCategories.categories.value[0] || ''
  form.targetValue = null
  form.statLabel = ''
  form.excludedFromBullseye = false
  form.entireCategoryPool = false
  entries.value = []
  entryPage.value = 1
  entryFilterTerm.value = ''
  athleteSearchTerm.value = ''
  athleteSearchResults.value = []
}

function openCreate() {
  resetForm()
  editingQuestionId.value = null
  view.value = 'builder'
}

async function openEdit(id) {
  error.value = ''
  try {
    const detail = await api.adminGetBullseyeQuestion(id)
    form.title = detail.title
    form.sport = detail.sport
    form.targetValue = detail.targetValue
    form.statLabel = detail.statLabel
    form.excludedFromBullseye = detail.excludedFromBullseye
    form.entireCategoryPool = detail.entireCategoryPool || false

    entries.value = detail.entries.map(e => ({
      athleteId: e.athlete.id, name: e.athlete.name, team: e.athlete.team, statValue: e.statValue
    }))
    entryPage.value = 1
    entryFilterTerm.value = ''

    editingQuestionId.value = id
    view.value = 'builder'
  } catch (e) {
    error.value = 'Could not load that question.'
  }
}

async function saveQuestion() {
  error.value = ''
  if (!form.title.trim() || !form.sport || form.targetValue === null || !form.statLabel.trim()) {
    error.value = 'Title, category, target number, and prompt are all required.'
    return
  }
  if (entries.value.length < 2) {
    error.value = 'Add at least 2 athletes with a stat value.'
    return
  }
  const missingValue = entries.value.find(e => e.statValue === null || e.statValue === '')
  if (missingValue) {
    error.value = `'${missingValue.name}' is missing a stat value - fill one in or remove that row.`
    return
  }

  const payload = {
    title: form.title,
    sport: form.sport,
    targetValue: form.targetValue,
    statLabel: form.statLabel,
    excludedFromBullseye: form.excludedFromBullseye,
    entireCategoryPool: form.entireCategoryPool,
    entries: entries.value.map(e => ({ athleteId: e.athleteId, statValue: e.statValue }))
  }

  saving.value = true
  try {
    if (editingQuestionId.value) {
      await api.adminUpdateBullseyeQuestion(editingQuestionId.value, payload)
    } else {
      await api.adminCreateBullseyeQuestion(payload)
    }
    toast.show('Question saved.')
    view.value = 'list'
    loadQuestions()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save the question.'
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
    toast.show('Question deleted.')
    loadQuestions()
  } catch (e) {
    error.value = 'Could not delete that question.'
  }
}
</script>
