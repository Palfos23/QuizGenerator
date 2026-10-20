<template>
  <div>
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
        <h1 style="margin:0;">{{ $t('Emneutvalg') }}</h1>
        <button class="btn btn-primary" @click="openCreate">{{ $t('+ Nytt utvalg') }}</button>
      </div>
      <p class="page-subtitle">
        {{ $t('Gjenbrukbare, kuraterte lister med emner - importer ett inn i et hvilket som helst grids kandidatutvalg med ett klikk, i stedet for å søke og legge til de samme om og om igjen.') }}
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <input
        type="text"
        v-model="poolSearchTerm"
        :placeholder="$t('Søk etter utvalgsnavn…')"
        class="search-input"
        style="margin-bottom:16px;"
      />

      <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>
      <div v-else-if="!filteredPools.length" class="empty-state friendly">
        {{ poolSearchTerm ? $t('Ingen utvalg samsvarer med «{0}».', [poolSearchTerm]) : $t('Ingen utvalg ennå - lag ditt første.') }}
      </div>

      <div v-else class="saved-quiz-list">
        <div v-for="p in pagedPools" :key="p.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ p.name }}</div>
            <div class="saved-quiz-meta">{{ sportLabel(p.sport) }} · {{ p.memberCount }} {{ $t('emne(r)') }}</div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(p.id)">{{ $t('Rediger') }}</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(p)">{{ $t('Slett') }}</button>
          </div>
        </div>
      </div>

      <Pagination v-model:page="poolPage" :page-size="POOL_PAGE_SIZE" :total-items="filteredPools.length" />
    </template>

    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <h1 style="margin:0;">{{ editingId ? $t('Rediger utvalg') : $t('Nytt utvalg') }}</h1>
        <button class="btn btn-secondary" @click="view = 'list'">{{ $t('← Tilbake til listen') }}</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:200px;">
          <label>{{ $t('Navn') }}<input type="text" v-model="form.name" :placeholder="$t('f.eks. Tottenhams PL-tropp, eller vinnere av Beste film')" /></label>
        </div>
        <div style="flex:1; min-width:200px;">
          <label>{{ $t('Kategori') }}
            <select v-model="form.sport" @change="onSportChange">
              <option v-for="s in gridCategories.categories.value" :key="s" :value="s">{{ s }}</option>
            </select>
          </label>
        </div>
      </div>

      <div class="field">
        <label>{{ $t('Medlemmer') }} <span class="picker-hint">{{ members.length }} {{ $t('emne(r)') }}</span></label>

        <input type="text" v-model="athleteSearchTerm" :placeholder="$t('Søk etter emner på navn…')" style="margin-bottom:10px;" />

        <div v-if="teamOptions.length" style="margin-bottom:10px;">
          <label style="font-size:0.85rem; color:var(--text-dim); text-transform:none; font-weight:400;">
            {{ $t('...eller legg til hele') }} {{ groupLabelPluralLower }} <span class="picker-hint" v-if="bulkTeams.length">{{ bulkTeams.length }} {{ $t('valgt§AdminAthletePoolsViewT') }}</span>
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
            {{ $t('+ Legg til') }} {{ bulkTeams.length > 1 ? `${bulkTeams.length} ${groupLabelPluralLower}` : groupLabelSingularLower }}
          </button>
        </div>

        <div v-if="athleteSearchResults.length" class="guess-results" style="margin-bottom:10px;">
          <button v-for="a in athleteSearchResults" :key="a.id" class="guess-result-row" @click="addMember(a)">
            {{ a.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ a.team }}</span>
          </button>
        </div>

        <div v-if="!members.length" class="empty-state" style="padding:20px;">
          {{ $t('Ingen medlemmer ennå - søk over eller legg til en hel') }} {{ groupLabelSingularLower }}.
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
        {{ saving ? $t('Lagrer…') : $t('Lagre utvalg') }}
      </button>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      :title="$t('Slette dette utvalget?')"
      :message="$t('«{0}» blir ikke lenger tilgjengelig for import i grids. Grids som allerede brukte det påvirkes ikke.', [pendingDelete.name])"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import ConfirmModal from '../components/ConfirmModal.vue'
import Pagination from '../components/Pagination.vue'
import { sportLabel } from '../constants'
import gridCategories from '../services/gridCategories'

const view = ref('list')
const error = ref('')
const loading = ref(true)
const pools = ref([])
const saving = ref(false)
const editingId = ref(null)
const pendingDelete = ref(null)

const form = reactive({ name: '', sport: '' })
const groupLabelSingularLower = computed(() => gridCategories.groupLabelFor(form.sport).toLowerCase())
const groupLabelPluralLower = computed(() => `${groupLabelSingularLower.value}s`)
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

onMounted(() => {
  loadPools()
  gridCategories.ensureLoaded()
})

async function loadPools() {
  loading.value = true
  error.value = ''
  try {
    pools.value = await api.adminListAthletePools()
  } catch (e) {
    error.value = _t('Kunne ikke laste utvalg.')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.sport = gridCategories.categories.value[0] || ''
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
    error.value = _t('Kunne ikke laste utvalget.')
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
    toast.show(_t('La til emner fra {0} gruppe(r).', [bulkTeams.value.length]))
    bulkTeams.value = []
  } catch (e) {
    error.value = _t('Kunne ikke søke etter disse lagene.')
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
    error.value = _t('Gi dette utvalget et navn.')
    return
  }
  if (!members.value.length) {
    error.value = _t('Legg til minst ett emne.')
    return
  }
  saving.value = true
  try {
    const payload = { name: form.name.trim(), sport: form.sport, athleteIds: members.value.map(m => m.athleteId) }
    if (editingId.value) {
      await api.adminUpdateAthletePool(editingId.value, payload)
      toast.show(_t('Utvalget er oppdatert.'))
    } else {
      await api.adminCreateAthletePool(payload)
      toast.show(_t('Utvalget er opprettet.'))
    }
    view.value = 'list'
    loadPools()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke lagre utvalget.')
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
    toast.show(_t('Utvalget er slettet.'))
    loadPools()
  } catch (e) {
    error.value = _t('Kunne ikke slette utvalget.')
  }
}
</script>
