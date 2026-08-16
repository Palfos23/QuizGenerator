<template>
  <div>
    <!-- List view -->
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>Starting XI</h1>
          <p class="page-subtitle">
            Create and manage "guess the lineup" boards for a specific football match.
          </p>
        </div>
        <button class="btn btn-primary" @click="openCreate">+ Create board</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
      <div v-else-if="!lineups.length" class="empty-state friendly">
        No Starting XI boards yet. Add some football subjects on the Subjects page, then create your first board here.
      </div>

      <div v-else class="saved-quiz-list">
        <div v-for="l in lineups" :key="l.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              {{ l.title }}
              <span v-if="l.excludedFromBattle" class="tag" style="background:rgba(255,77,109,0.15); color:var(--coral); margin-left:6px;">Not in Starting XI Battle</span>
            </div>
            <div class="saved-quiz-meta">
              {{ l.teamName }} vs {{ l.opponentName }}
              <template v-if="l.scoreFor != null && l.scoreAgainst != null"> ({{ l.scoreFor }}-{{ l.scoreAgainst }})</template>
              · {{ l.formation }} · {{ l.maxStrikes }} {{ l.maxStrikes === 1 ? 'life' : 'lives' }} · week of {{ l.weekStartDate }}
            </div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(l.id)">Edit</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(l)">Delete</button>
          </div>
        </div>
      </div>
    </template>

    <!-- Builder view -->
    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <h1 style="margin:0;">{{ editingLineupId ? 'Edit Starting XI board' : 'Create Starting XI board' }}</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Back to list</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Title</label>
        <input type="text" v-model="form.title" placeholder="e.g. Community Shield 2014" />
      </div>

      <div class="field">
        <label>Competition / context <span class="picker-hint">optional</span></label>
        <textarea v-model="form.competition" placeholder="e.g. Arsenal's opening-day XI the season after winning the FA Cup."></textarea>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>Match date <span class="picker-hint">optional</span></label>
          <input type="date" v-model="form.matchDate" />
        </div>
        <div style="flex:1; min-width:160px;">
          <label>Week start date</label>
          <input type="date" v-model="form.weekStartDate" />
        </div>
        <div style="flex:1; min-width:140px;">
          <label>Max strikes</label>
          <input type="number" min="1" max="20" v-model.number="form.maxStrikes" />
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:220px;">
          <label>Team (the XI being guessed)</label>
          <input type="text" v-model="form.teamName" placeholder="e.g. Arsenal" />
        </div>
        <div style="flex:1; min-width:220px;">
          <label>Team crest URL <span class="picker-hint">optional</span></label>
          <input type="text" v-model="form.teamCrestUrl" placeholder="https://…" />
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:220px;">
          <label>Opponent <span class="picker-hint">context only, never guessable</span></label>
          <input type="text" v-model="form.opponentName" placeholder="e.g. Man City" />
        </div>
        <div style="flex:1; min-width:220px;">
          <label>Opponent crest URL <span class="picker-hint">optional</span></label>
          <input type="text" v-model="form.opponentCrestUrl" placeholder="https://…" />
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:140px;">
          <label>{{ form.teamName || 'Team' }} score</label>
          <input type="number" min="0" v-model.number="form.scoreFor" />
        </div>
        <div style="flex:1; min-width:140px;">
          <label>{{ form.opponentName || 'Opponent' }} score</label>
          <input type="number" min="0" v-model.number="form.scoreAgainst" />
        </div>
      </div>

      <div class="field">
        <label>Formation <span class="picker-hint">determines the 11 pitch slots below</span></label>
        <div class="language-row">
          <button
            v-for="f in FORMATION_NAMES"
            :key="f"
            type="button"
            class="language-btn"
            :class="{ active: form.formation === f }"
            @click="changeFormation(f)"
          >{{ f }}</button>
        </div>
      </div>

      <div class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.excludedFromBattle" style="width:auto;" />
          Exclude from Starting XI Battle
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          Use this once a board's roster needs correcting - it stays fully playable solo, but stops
          being offered to Starting XI Battle's random or manual pick.
        </p>
      </div>

      <div class="field">
        <label>Candidate pool <span class="picker-hint">everyone guessable on this board - correct and decoy</span></label>

        <input type="text" v-model="athleteSearchTerm" placeholder="Search football subjects by name…" style="width:100%; margin-bottom:10px;" />

        <div v-if="athleteSearchResults.length" class="guess-results" style="margin-bottom:10px;">
          <button v-for="a in athleteSearchResults" :key="a.id" class="guess-result-row" @click="addCandidate(a)">
            {{ a.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ a.team }}</span>
          </button>
        </div>

        <div v-if="!candidates.length" class="empty-state" style="padding:20px;">
          No subjects added yet - search above. Make sure the subject's category is "Football".
        </div>

        <div v-else>
          <div v-for="c in candidates" :key="c.athleteId" class="candidate-row">
            <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600; margin:0;">
              <input type="checkbox" v-model="c.correct" style="width:auto;" @change="onCorrectToggle(c)" />
              {{ c.name }} <span style="color:var(--text-dim); font-weight:400; font-size:0.85rem;">{{ c.team }}</span>
            </label>
            <div v-if="c.correct" style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
              <input type="number" v-model.number="c.shirtNumber" placeholder="No." style="width:70px;" min="1" max="99" />
              <select v-model.number="c.slotIndex" style="width:130px;">
                <option :value="null">Slot…</option>
                <option v-for="(label, idx) in slotLabels(form.formation)" :key="idx" :value="idx" :disabled="slotTaken(idx, c)">
                  {{ label }}
                </option>
              </select>
              <label style="display:flex; align-items:center; gap:6px; text-transform:none; font-weight:400; font-size:0.82rem; color:var(--text-dim); margin:0;">
                <input type="checkbox" v-model="c.captain" style="width:auto;" />
                Captain
              </label>
            </div>
            <button class="btn btn-danger btn-sm" @click="removeCandidate(c)">✕</button>
          </div>
        </div>
      </div>

      <div style="display:flex; gap:10px; flex-wrap:wrap;">
        <button class="btn btn-secondary" :disabled="filledSlotCount === 0" @click="showPreview = true">
          Preview ({{ filledSlotCount }}/{{ requiredSlotCount }} slots)
        </button>
        <button class="btn btn-primary" :disabled="saving" @click="saveLineup">
          {{ saving ? 'Saving…' : 'Save board' }}
        </button>
      </div>
    </template>

    <div v-if="showPreview" class="modal-backdrop" @click.self="showPreview = false">
      <div class="modal" style="max-width:640px;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
          <h2 style="margin:0;">{{ form.title || 'Untitled board' }}</h2>
          <button class="btn btn-secondary btn-sm" @click="showPreview = false">Close</button>
        </div>
        <p class="page-subtitle" style="margin-top:0;">Everything shown revealed, for a quick check that shirts and slots look right.</p>

        <div class="pitch-scoreline" v-if="form.teamName || form.opponentName">
          <span>{{ form.teamName }}<template v-if="form.scoreFor != null"> {{ form.scoreFor }}</template></span>
          <span class="score">-</span>
          <span><template v-if="form.scoreAgainst != null">{{ form.scoreAgainst }} </template>{{ form.opponentName }}</span>
        </div>

        <div class="pitch">
          <div v-for="(row, ri) in previewRows" :key="ri" class="pitch-row">
            <div v-for="slot in row" :key="slot.slotIndex" class="pitch-slot">
              <div class="pitch-shirt solved">
                {{ slot.shirtNumber ?? '?' }}
                <span v-if="slot.captain" class="pitch-shirt-captain">C</span>
              </div>
              <div class="pitch-slot-name">{{ slot.name || '(empty)' }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this Starting XI board?"
      :message="`'${pendingDelete.title}' and everyone's progress on it will be permanently removed.`"
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
import { FORMATION_NAMES, slotLabels, slotCount, rowsFor } from '../services/formations'

const FOOTBALL_CATEGORY = 'Football'

const view = ref('list')
const lineups = ref([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const pendingDelete = ref(null)
const editingLineupId = ref(null)
const showPreview = ref(false)

const form = reactive({
  title: '', competition: '', matchDate: '', weekStartDate: '', formation: '4-3-3',
  teamName: '', teamCrestUrl: '', opponentName: '', opponentCrestUrl: '',
  scoreFor: null, scoreAgainst: null, maxStrikes: 5, excludedFromBattle: false
})
const candidates = ref([])
const athleteSearchTerm = ref('')
const athleteSearchResults = ref([])

const requiredSlotCount = computed(() => slotCount(form.formation))
const filledSlotCount = computed(() => candidates.value.filter(c => c.correct && c.slotIndex != null).length)

const previewRows = computed(() => {
  const filled = candidates.value
    .filter(c => c.correct && c.slotIndex != null)
    .map(c => ({ slotIndex: c.slotIndex, shirtNumber: c.shirtNumber, captain: c.captain, name: c.name }))
  return rowsFor(form.formation, filled)
})

onMounted(loadLineups)

async function loadLineups() {
  loading.value = true
  error.value = ''
  try {
    lineups.value = await api.adminListLineups()
  } catch (e) {
    error.value = 'Could not load Starting XI boards.'
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
      athleteSearchResults.value = await api.adminSearchAthletes({ sport: FOOTBALL_CATEGORY, name: val })
    } catch (e) {
      // non-critical - the search box just stays empty
    }
  }, 250)
})

function addCandidate(athlete) {
  if (candidates.value.some(c => c.athleteId === athlete.id)) return
  candidates.value.push({
    athleteId: athlete.id, name: athlete.name, team: athlete.team,
    correct: false, shirtNumber: null, slotIndex: null, captain: false
  })
  athleteSearchTerm.value = ''
  athleteSearchResults.value = []
}

function removeCandidate(c) {
  candidates.value = candidates.value.filter(x => x !== c)
}

// Unchecking "correct" drops this candidate back to decoy-only - clear the
// slot assignment so it doesn't silently linger and get resurrected if the
// admin re-checks it later expecting a blank slate.
function onCorrectToggle(c) {
  if (!c.correct) {
    c.shirtNumber = null
    c.slotIndex = null
    c.captain = false
  }
}

function slotTaken(idx, forCandidate) {
  return candidates.value.some(c => c !== forCandidate && c.correct && c.slotIndex === idx)
}

// Changing formation can leave previously-assigned slots out of range (e.g.
// switching from 4-3-3's 11 slots down to nothing smaller - all formations
// here are 11 slots, but a slot's *meaning* changes) - clearing every
// assignment avoids two starters silently ending up in mismatched positions.
function changeFormation(f) {
  if (f === form.formation) return
  form.formation = f
  candidates.value.forEach(c => { c.slotIndex = null })
}

function resetForm() {
  form.title = ''
  form.competition = ''
  form.matchDate = ''
  form.weekStartDate = ''
  form.formation = '4-3-3'
  form.teamName = ''
  form.teamCrestUrl = ''
  form.opponentName = ''
  form.opponentCrestUrl = ''
  form.scoreFor = null
  form.scoreAgainst = null
  form.maxStrikes = 5
  form.excludedFromBattle = false
  candidates.value = []
  athleteSearchTerm.value = ''
  athleteSearchResults.value = []
}

function openCreate() {
  resetForm()
  editingLineupId.value = null
  view.value = 'builder'
}

async function openEdit(id) {
  error.value = ''
  try {
    const detail = await api.adminGetLineup(id)
    form.title = detail.title
    form.competition = detail.competition || ''
    form.matchDate = detail.matchDate || ''
    form.weekStartDate = detail.weekStartDate
    form.formation = detail.formation
    form.teamName = detail.teamName
    form.teamCrestUrl = detail.teamCrestUrl || ''
    form.opponentName = detail.opponentName
    form.opponentCrestUrl = detail.opponentCrestUrl || ''
    form.scoreFor = detail.scoreFor
    form.scoreAgainst = detail.scoreAgainst
    form.maxStrikes = detail.maxStrikes
    form.excludedFromBattle = detail.excludedFromBattle

    const entryByAthleteId = new Map(detail.entries.map(e => [e.athlete.id, e]))
    candidates.value = detail.candidates.map(a => {
      const entry = entryByAthleteId.get(a.id)
      return {
        athleteId: a.id, name: a.name, team: a.team,
        correct: !!entry,
        shirtNumber: entry?.shirtNumber ?? null,
        slotIndex: entry?.slotIndex ?? null,
        captain: entry?.captain ?? false
      }
    })

    editingLineupId.value = id
    view.value = 'builder'
  } catch (e) {
    error.value = 'Could not load that board.'
  }
}

async function saveLineup() {
  error.value = ''
  if (!form.title.trim() || !form.weekStartDate) {
    error.value = 'Title and week start date are required.'
    return
  }
  if (!form.teamName.trim() || !form.opponentName.trim()) {
    error.value = 'Team and opponent are both required.'
    return
  }
  const entries = candidates.value.filter(c => c.correct)
  if (entries.length !== requiredSlotCount.value) {
    error.value = `${form.formation} needs exactly ${requiredSlotCount.value} correct starters - currently ${entries.length}.`
    return
  }
  if (entries.some(c => !c.shirtNumber || c.slotIndex == null)) {
    error.value = 'Every starter needs both a shirt number and a formation slot.'
    return
  }

  const payload = {
    title: form.title,
    competition: form.competition,
    matchDate: form.matchDate || null,
    weekStartDate: form.weekStartDate,
    formation: form.formation,
    teamName: form.teamName,
    teamCrestUrl: form.teamCrestUrl || null,
    opponentName: form.opponentName,
    opponentCrestUrl: form.opponentCrestUrl || null,
    scoreFor: form.scoreFor,
    scoreAgainst: form.scoreAgainst,
    maxStrikes: form.maxStrikes,
    excludedFromBattle: form.excludedFromBattle,
    candidateAthleteIds: candidates.value.map(c => c.athleteId),
    entries: entries.map(c => ({
      athleteId: c.athleteId, shirtNumber: c.shirtNumber, slotIndex: c.slotIndex, captain: c.captain
    }))
  }

  saving.value = true
  try {
    if (editingLineupId.value) {
      await api.adminUpdateLineup(editingLineupId.value, payload)
    } else {
      await api.adminCreateLineup(payload)
    }
    toast.show('Starting XI board saved.')
    view.value = 'list'
    loadLineups()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save this board.'
  } finally {
    saving.value = false
  }
}

function requestDelete(l) {
  pendingDelete.value = l
}

async function doDelete() {
  const l = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.adminDeleteLineup(l.id)
    toast.show('Board deleted.')
    loadLineups()
  } catch (e) {
    error.value = 'Could not delete that board.'
  }
}
</script>
