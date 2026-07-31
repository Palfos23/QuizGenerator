<template>
  <div>
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
        <h1 style="margin:0;">Athlete pools</h1>
        <button class="btn btn-primary" @click="openCreate">+ New pool</button>
      </div>
      <p class="page-subtitle">
        Reusable, curated lists of athletes - import one into any grid's candidate pool in one click,
        instead of searching and adding the same players over and over.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <input
        type="text"
        v-model="poolSearchTerm"
        placeholder="Search pool names…"
        class="search-input"
        style="margin-bottom:16px;"
      />

      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
      <div v-else-if="!filteredPools.length" class="empty-state friendly">
        {{ poolSearchTerm ? `No pools match "${poolSearchTerm}".` : 'No pools yet - create your first one.' }}
      </div>

      <div v-else class="saved-quiz-list">
        <div v-for="p in pagedPools" :key="p.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ p.name }}</div>
            <div class="saved-quiz-meta">{{ sportLabel(p.sport) }} · {{ p.memberCount }} athlete(s)</div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(p.id)">Edit</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(p)">Delete</button>
          </div>
        </div>
      </div>

      <Pagination v-model:page="poolPage" :page-size="POOL_PAGE_SIZE" :total-items="filteredPools.length" />
    </template>

    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <h1 style="margin:0;">{{ editingId ? 'Edit pool' : 'New pool' }}</h1>
        <button class="btn btn-secondary" @click="view = 'list'">← Back to list</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:200px;">
          <label>Name</label>
          <input type="text" v-model="form.name" placeholder="e.g. Tottenham PL squad" />
        </div>
        <div style="flex:1; min-width:200px;">
          <label>Sport</label>
          <select v-model="form.sport" @change="onSportChange">
            <option v-for="s in SPORTS" :key="s.code" :value="s.code">{{ s.label }}</option>
          </select>
        </div>
      </div>

      <div class="field">
        <label>Members <span class="picker-hint">{{ members.length }} athlete(s)</span></label>

        <input type="text" v-model="athleteSearchTerm" placeholder="Search athletes by name…" style="margin-bottom:10px;" />

        <div v-if="teamOptions.length" style="margin-bottom:10px;">
          <label style="font-size:0.85rem; color:var(--text-dim); text-transform:none; font-weight:400;">
            ...or add whole teams <span class="picker-hint" v-if="bulkTeams.length">{{ bulkTeams.length }} selected</span>
          </label>
          <div style="display:flex; gap:8px; flex-wrap:wrap; margin-top:6px;">
            <button
              v-for="t in teamOptions"
              :key="t"
              type="button"
              class="team-chip"
              :class="{ active: bulkTeams.includes(t) }"
              @click="toggleBulkTeam(t)"
            >{{ t }}</button>
          </div>
          <button class="btn btn-secondary btn-sm" style="margin-top:10px;" :disabled="!bulkTeams.length" @click="addAllByTeam">
            + Add {{ bulkTeams.length > 1 ? `${bulkTeams.length} teams` : 'team' }}
          </button>
        </div>

        <div v-if="athleteSearchResults.length" class="guess-results" style="margin-bottom:10px;">
          <button v-for="a in athleteSearchResults" :key="a.id" class="guess-result-row" @click="addMember(a)">
            {{ a.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ a.team }}</span>
          </button>
        </div>

        <div v-if="!members.length" class="empty-state" style="padding:20px;">
          No members yet - search above or add a whole team.
        </div>
        <div v-else class="candidate-list">
          <div v-for="m in pagedMembers" :key="m.athleteId" class="candidate-row">
            <span>{{ m.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ m.team }}</span></span>
            <button class="btn btn-danger btn-sm" @click="removeMember(m)">✕</button>
          </div>
          <Pagination v-model:page="memberPage" :page-size="MEMBER_PAGE_SIZE" :total-items="members.length" />
        </div>
      </div>

      <button class="btn btn-primary" :disabled="saving" @click="savePool">
        {{ saving ? 'Saving…' : 'Save pool' }}
      </button>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this pool?"
      :message="`'${pendingDelete.name}' will no longer be available to import into grids. Grids that already used it are unaffected.`"
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
import { SPORTS, sportLabel } from '../constants'

const view = ref('list')
const error = ref('')
const loading = ref(true)
const pools = ref([])
const saving = ref(false)
const editingId = ref(null)
const pendingDelete = ref(null)

const form = reactive({ name: '', sport: 'FOOTBALL' })
const members = ref([]) // [{ athleteId, name, team }]

const poolSearchTerm = ref('')
const filteredPools = computed(() => {
  const term = poolSearchTerm.value.trim().toLowerCase()
  if (!term) return pools.value
  return pools.value.filter(p => p.name.toLowerCase().includes(term))
})
const POOL_PAGE_SIZE = 25
const poolPage = ref(1)
const pagedPools = computed(() => {
  const start = (poolPage.value - 1) * POOL_PAGE_SIZE
  return filteredPools.value.slice(start, start + POOL_PAGE_SIZE)
})
watch(poolSearchTerm, () => { poolPage.value = 1 })

const MEMBER_PAGE_SIZE = 25
const memberPage = ref(1)
const pagedMembers = computed(() => {
  const start = (memberPage.value - 1) * MEMBER_PAGE_SIZE
  return members.value.slice(start, start + MEMBER_PAGE_SIZE)
})

const athleteSearchTerm = ref('')
const athleteSearchResults = ref([])
let searchDebounce = null
watch(athleteSearchTerm, (val) => {
  clearTimeout(searchDebounce)
  if (!val || val.trim().length < 3) {
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

const teamOptions = ref([])
const bulkTeams = ref([])

async function loadTeamOptions() {
  try {
    const athletes = await api.adminSearchAthletes({ sport: form.sport })
    teamOptions.value = [...new Set(athletes.map(a => a.team).filter(Boolean))].sort()
  } catch (e) {
    // non-critical - the team dropdown just stays empty
  }
}

function toggleBulkTeam(team) {
  if (bulkTeams.value.includes(team)) {
    bulkTeams.value = bulkTeams.value.filter(t => t !== team)
  } else {
    bulkTeams.value = [...bulkTeams.value, team]
  }
}

function onSportChange() {
  bulkTeams.value = []
  loadTeamOptions()
}

onMounted(loadPools)

async function loadPools() {
  loading.value = true
  error.value = ''
  try {
    pools.value = await api.adminListAthletePools()
  } catch (e) {
    error.value = 'Could not load pools.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.sport = 'FOOTBALL'
  members.value = []
  athleteSearchTerm.value = ''
  athleteSearchResults.value = []
  bulkTeams.value = []
  memberPage.value = 1
  error.value = ''
  loadTeamOptions()
  view.value = 'form'
}

async function openEdit(id) {
  error.value = ''
  try {
    const detail = await api.adminGetAthletePool(id)
    editingId.value = id
    form.name = detail.name
    form.sport = detail.sport
    members.value = detail.members.map(a => ({ athleteId: a.id, name: a.name, team: a.team }))
    athleteSearchTerm.value = ''
    athleteSearchResults.value = []
    bulkTeams.value = []
    memberPage.value = 1
    loadTeamOptions()
    view.value = 'form'
  } catch (e) {
    error.value = 'Could not load that pool.'
  }
}

function addMember(athlete) {
  if (members.value.some(m => m.athleteId === athlete.id)) return
  members.value.push({ athleteId: athlete.id, name: athlete.name, team: athlete.team })
}

async function addAllByTeam() {
  error.value = ''
  try {
    const resultsPerTeam = await Promise.all(
      bulkTeams.value.map(team => api.adminSearchAthletes({ sport: form.sport, team }))
    )
    resultsPerTeam.flat().forEach(addMember)
    toast.show(`Added athletes from ${bulkTeams.value.length} team(s).`)
    bulkTeams.value = []
  } catch (e) {
    error.value = 'Could not search for those teams.'
  }
}

function removeMember(m) {
  const idx = members.value.indexOf(m)
  if (idx !== -1) members.value.splice(idx, 1)
  const maxPage = Math.max(1, Math.ceil(members.value.length / MEMBER_PAGE_SIZE))
  if (memberPage.value > maxPage) memberPage.value = maxPage
}

async function savePool() {
  error.value = ''
  if (!form.name.trim()) {
    error.value = 'Give this pool a name.'
    return
  }
  if (!members.value.length) {
    error.value = 'Add at least one athlete.'
    return
  }
  saving.value = true
  try {
    const payload = { name: form.name.trim(), sport: form.sport, athleteIds: members.value.map(m => m.athleteId) }
    if (editingId.value) {
      await api.adminUpdateAthletePool(editingId.value, payload)
      toast.show('Pool updated.')
    } else {
      await api.adminCreateAthletePool(payload)
      toast.show('Pool created.')
    }
    view.value = 'list'
    loadPools()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save that pool.'
  } finally {
    saving.value = false
  }
}

function requestDelete(p) {
  pendingDelete.value = p
}

async function doDelete() {
  const p = pendingDelete.value
  pendingDelete.value = null
  try {
    await api.adminDeleteAthletePool(p.id)
    toast.show('Pool deleted.')
    loadPools()
  } catch (e) {
    error.value = 'Could not delete that pool.'
  }
}
</script>
