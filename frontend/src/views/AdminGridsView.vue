<template>
  <div>
    <!-- List view -->
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>Weekly grids</h1>
          <p class="page-subtitle">
            Create and manage the weekly guessing-grid challenges.
            <router-link to="/admin/clubs">Manage clubs →</router-link>
          </p>
        </div>
        <button class="btn btn-primary" @click="openCreate">+ Create grid</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

      <div v-else-if="!grids.length" class="empty-state friendly">
        No grids yet. Add a few athletes on the Athletes page, then create your first grid here.
      </div>

      <div v-else class="saved-quiz-list">
        <div v-for="g in grids" :key="g.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ g.title }}</div>
            <div class="saved-quiz-meta">{{ sportLabel(g.sport) }} · {{ g.entryCount }} entries · week of {{ g.weekStartDate }}</div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(g.id)">Edit</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(g)">Delete</button>
          </div>
        </div>
      </div>
    </template>

    <!-- Builder view -->
    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <h1 style="margin:0;">{{ editingGridId ? 'Edit grid' : 'Create grid' }}</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Back to list</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Title</label>
        <input type="text" v-model="form.title" placeholder="e.g. Tottenham 10+ goal scorers" />
      </div>

      <div class="field">
        <label>Theme / instructions</label>
        <textarea v-model="form.theme" placeholder="Name every Tottenham player with more than 10 goals."></textarea>
      </div>

      <div class="field">
        <label>Sport <span class="picker-hint">choose one</span></label>
        <div class="language-row">
          <button
            v-for="s in SPORTS"
            :key="s.code"
            class="language-btn"
            :class="{ active: form.sport === s.code }"
            @click="changeSport(s.code)"
          >
            {{ s.label }}
          </button>
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:180px;">
          <label>Week start date</label>
          <input type="date" v-model="form.weekStartDate" />
        </div>
        <div style="flex:1; min-width:140px;">
          <label>Max strikes</label>
          <input type="number" min="1" max="20" v-model.number="form.maxStrikes" />
        </div>
      </div>

      <div class="field">
        <label>Candidate pool <span class="picker-hint">everyone guessable in this grid - correct and decoy</span></label>

        <div style="display:flex; gap:10px; flex-wrap:wrap; margin-bottom:10px;">
          <input type="text" v-model="athleteSearchTerm" placeholder="Search athletes by name…" style="flex:1; min-width:180px;" />
          <input type="text" v-model="bulkTeam" placeholder="...or add a whole team" style="flex:1; min-width:160px;" />
          <button class="btn btn-secondary btn-sm" @click="addAllByTeam" :disabled="!bulkTeam.trim()">+ Add team</button>
        </div>

        <div v-if="athleteSearchResults.length" class="guess-results" style="margin-bottom:10px;">
          <button
            v-for="a in athleteSearchResults"
            :key="a.id"
            class="guess-result-row"
            @click="addCandidate(a)"
          >
            {{ a.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ a.team }}</span>
          </button>
        </div>

        <div v-if="!candidates.length" class="empty-state" style="padding:20px;">
          No athletes added yet - search above or add a whole team.
        </div>

        <div v-else class="candidate-list">
          <div v-for="(c, idx) in candidates" :key="c.athleteId" class="candidate-row">
            <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600; margin:0;">
              <input type="checkbox" v-model="c.correct" style="width:auto;" />
              {{ c.name }} <span style="color:var(--text-dim); font-weight:400; font-size:0.85rem;">{{ c.team }}</span>
            </label>
            <div v-if="c.correct" style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
              <input type="text" v-model="c.hintLabel" placeholder="Label (e.g. FW)" style="width:110px;" />
              <input type="number" v-model.number="c.hintValue" placeholder="Value" style="width:80px;" />
              <select v-model="c.clubId" style="width:170px;">
                <option :value="null">No logo</option>
                <option v-for="club in clubOptions" :key="club.id" :value="club.id">{{ club.name }}</option>
              </select>
              <label style="display:flex; align-items:center; gap:6px; text-transform:none; font-weight:400; font-size:0.82rem; color:var(--text-dim); margin:0;">
                <input type="checkbox" v-model="c.showLogo" style="width:auto;" :disabled="!c.clubId" />
                Show logo
              </label>
            </div>
            <button class="btn btn-danger btn-sm" @click="candidates.splice(idx, 1)">✕</button>
          </div>
        </div>
      </div>

      <button class="btn btn-primary" :disabled="saving" @click="saveGrid">
        {{ saving ? 'Saving…' : 'Save grid' }}
      </button>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this grid?"
      :message="`'${pendingDelete.title}' and everyone's progress on it will be permanently removed.`"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import ConfirmModal from '../components/ConfirmModal.vue'
import { SPORTS, sportLabel } from '../constants'

const view = ref('list')
const grids = ref([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const pendingDelete = ref(null)

const editingGridId = ref(null)
const form = reactive({
  title: '',
  theme: '',
  sport: 'FOOTBALL',
  weekStartDate: '',
  maxStrikes: 5
})
const candidates = ref([]) // [{ athleteId, name, team, correct, hintLabel, hintValue, clubId, showLogo }]
const clubOptions = ref([])

const athleteSearchTerm = ref('')
const athleteSearchResults = ref([])
const bulkTeam = ref('')

onMounted(() => {
  loadGrids()
  loadClubOptions()
})

async function loadClubOptions() {
  try {
    clubOptions.value = await api.adminSearchClubs(form.sport)
  } catch (e) {
    // non-critical - the club dropdown just stays empty
  }
}

async function loadGrids() {
  loading.value = true
  error.value = ''
  try {
    grids.value = await api.adminListGrids()
  } catch (e) {
    error.value = 'Could not load grids.'
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
  loadClubOptions()
}

function addCandidate(athlete) {
  if (candidates.value.some(c => c.athleteId === athlete.id)) return
  candidates.value.push({
    athleteId: athlete.id, name: athlete.name, team: athlete.team,
    correct: false, hintLabel: '', hintValue: null, clubId: null, showLogo: true
  })
}

async function addAllByTeam() {
  error.value = ''
  try {
    const results = await api.adminSearchAthletes({ sport: form.sport, team: bulkTeam.value.trim() })
    results.forEach(addCandidate)
    bulkTeam.value = ''
  } catch (e) {
    error.value = 'Could not search for that team.'
  }
}

function resetForm() {
  form.title = ''
  form.theme = ''
  form.sport = 'FOOTBALL'
  form.weekStartDate = ''
  form.maxStrikes = 5
  candidates.value = []
  athleteSearchTerm.value = ''
  athleteSearchResults.value = []
  bulkTeam.value = ''
  loadClubOptions()
}

function openCreate() {
  resetForm()
  editingGridId.value = null
  view.value = 'builder'
}

async function openEdit(id) {
  error.value = ''
  try {
    const detail = await api.adminGetGrid(id)
    form.title = detail.title
    form.theme = detail.theme
    form.sport = detail.sport
    form.weekStartDate = detail.weekStartDate
    form.maxStrikes = detail.maxStrikes

    const entryByAthleteId = new Map(detail.entries.map(e => [e.athlete.id, e]))
    candidates.value = detail.candidates.map(a => {
      const entry = entryByAthleteId.get(a.id)
      return {
        athleteId: a.id, name: a.name, team: a.team,
        correct: !!entry,
        hintLabel: entry?.hintLabel || '',
        hintValue: entry?.hintValue ?? null,
        clubId: entry?.club?.id ?? null,
        showLogo: entry?.showLogo ?? true
      }
    })

    editingGridId.value = id
    view.value = 'builder'
    loadClubOptions()
  } catch (e) {
    error.value = 'Could not load that grid.'
  }
}

async function saveGrid() {
  error.value = ''
  if (!form.title.trim() || !form.weekStartDate) {
    error.value = 'Title and week start date are required.'
    return
  }
  const entries = candidates.value.filter(c => c.correct)
  if (!entries.length) {
    error.value = 'Mark at least one candidate as a correct answer.'
    return
  }
  if (entries.some(c => !c.hintLabel?.trim() || c.hintValue === null || c.hintValue === '')) {
    error.value = 'Every correct answer needs both a hint label and a hint value.'
    return
  }

  const payload = {
    title: form.title,
    theme: form.theme,
    sport: form.sport,
    weekStartDate: form.weekStartDate,
    maxStrikes: form.maxStrikes,
    candidateAthleteIds: candidates.value.map(c => c.athleteId),
    entries: entries.map(c => ({
      athleteId: c.athleteId, hintLabel: c.hintLabel, hintValue: c.hintValue,
      clubId: c.clubId, showLogo: c.showLogo
    }))
  }

  saving.value = true
  try {
    if (editingGridId.value) {
      await api.adminUpdateGrid(editingGridId.value, payload)
    } else {
      await api.adminCreateGrid(payload)
    }
    toast.show('Grid saved.')
    view.value = 'list'
    loadGrids()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save the grid.'
  } finally {
    saving.value = false
  }
}

function requestDelete(g) {
  pendingDelete.value = g
}

async function doDelete() {
  const g = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.adminDeleteGrid(g.id)
    toast.show('Grid deleted.')
    loadGrids()
  } catch (e) {
    error.value = 'Could not delete that grid.'
  }
}
</script>
