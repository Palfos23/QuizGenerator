<template>
  <div>
    <!-- List view -->
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>Starting XI <span v-if="!loading && lineups.length" class="header-count">{{ lineups.length }}</span></h1>
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

      <template v-else>
      <BoardListToolbar
        v-model:search="searchTerm"
        v-model:sort-key="sortKey"
        v-model:sort-dir="sortDir"
        :sorts="lineupSorts"
        :total-count="lineups.length"
        :filtered-count="filteredLineups.length"
        placeholder="Search boards by title, team or formation…"
      />

      <div v-if="!filteredLineups.length" class="empty-state">No boards match your search.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="l in pagedLineups" :key="l.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              {{ l.title }}
              <span v-if="l.excludedFromBattle" class="tag" style="background:rgba(255,77,109,0.15); color:var(--coral); margin-left:6px;">Not in XI Battle</span>
              <span v-if="l.entireCategoryPool" class="tag" style="background:rgba(61,220,151,0.15); color:var(--teal); margin-left:6px;">Auto pool</span>
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

      <Pagination v-model:page="lineupPage" :page-size="10" :total-items="filteredLineups.length" />
      </template>
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
          Exclude from XI Battle
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          Use this once a board's roster needs correcting - it stays fully playable solo, but stops
          being offered to XI Battle's random or manual pick.
        </p>
      </div>

      <div class="field">
        <label>Kit colors <span class="picker-hint">the goalkeeper always shows a different kit than the outfield players</span></label>
        <div style="display:flex; gap:20px; flex-wrap:wrap; align-items:center;">
          <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:400; margin:0;">
            <input type="color" v-model="form.kitColor" style="width:44px; height:32px; padding:2px; cursor:pointer;" />
            Outfield kit
          </label>
          <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:400; margin:0;">
            <input type="color" v-model="form.goalkeeperKitColor" style="width:44px; height:32px; padding:2px; cursor:pointer;" />
            Goalkeeper kit
          </label>
        </div>
      </div>

      <div class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.entireCategoryPool" style="width:auto;" @change="onEntireCategoryToggle" />
          Use every subject in "Football" as the pool
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          Every subject in this category becomes guessable automatically, including ones added to it
          later - nothing to re-import. You still pick which ones are the correct starters below.
        </p>
      </div>

      <div class="field">
        <label>Candidate pool <span class="picker-hint">{{ form.entireCategoryPool ? 'pick the correct starters - every football subject is guessable' : 'everyone guessable on this board - correct and decoy' }}</span></label>

        <input type="text" v-model="athleteSearchTerm" placeholder="Search football subjects by name…" style="width:100%; margin-bottom:10px;" />

        <template v-if="!form.entireCategoryPool">
        <div v-if="poolsForFootball.length" style="margin-bottom:10px;">
          <label style="font-size:0.85rem; color:var(--text-dim); text-transform:none; font-weight:400;">
            ...or import from a saved pool
          </label>
          <div style="display:flex; gap:8px; margin-top:6px; flex-wrap:wrap;">
            <select v-model="selectedPoolId" style="flex:1; min-width:200px;">
              <option :value="null">Choose a pool…</option>
              <option v-for="p in poolsForFootball" :key="p.id" :value="p.id">{{ p.name }} ({{ p.memberCount }})</option>
            </select>
            <button class="btn btn-secondary btn-sm" :disabled="!selectedPoolId" @click="importFromPool">
              + Import pool
            </button>
          </div>
        </div>

        <div style="margin-bottom:10px;">
          <button class="btn btn-secondary btn-sm" :disabled="importingAll" @click="importAllInCategory">
            {{ importingAll ? 'Importing…' : '+ Import every subject in "Football"' }}
          </button>
        </div>
        </template>

        <div v-if="athleteSearchResults.length" class="guess-results" style="margin-bottom:10px;">
          <button v-for="a in athleteSearchResults" :key="a.id" class="guess-result-row" @click="addCandidate(a, form.entireCategoryPool)">
            {{ a.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ a.team }}</span>
          </button>
        </div>

        <div v-if="!candidates.length" class="empty-state" style="padding:20px;">
          {{ form.entireCategoryPool ? 'No starters picked yet - search above to mark who\'s correct.' : 'No subjects added yet - search above, or import a saved pool.' }}
        </div>

        <div v-else>
          <input
            type="text"
            v-model="candidateFilterTerm"
            placeholder="Find someone already added…"
            class="search-input"
            style="margin-bottom:12px;"
          />

          <div v-if="!filteredCandidates.length" class="empty-state" style="padding:20px;">
            Nobody added yet matches "{{ candidateFilterTerm }}".
          </div>

          <template v-else>
          <div v-for="c in pagedCandidates" :key="c.athleteId" class="candidate-row">
            <label v-if="!form.entireCategoryPool" style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600; margin:0;">
              <input type="checkbox" v-model="c.correct" style="width:auto;" @change="onCorrectToggle(c)" />
              {{ c.name }} <span style="color:var(--text-dim); font-weight:400; font-size:0.85rem;">{{ c.team }}</span>
            </label>
            <div v-else style="display:flex; align-items:center; gap:8px; font-weight:600;">
              {{ c.name }} <span style="color:var(--text-dim); font-weight:400; font-size:0.85rem;">{{ c.team }}</span>
            </div>
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
            <button class="btn btn-secondary btn-sm" @click="openEditAthlete(c)">Edit</button>
            <button class="btn btn-danger btn-sm" @click="removeCandidate(c)">✕</button>
          </div>

          <Pagination v-model:page="candidatePage" :page-size="CANDIDATE_PAGE_SIZE" :total-items="filteredCandidates.length" />
          </template>
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
          <div class="pitch-scoreline-team">
            <img v-if="form.teamCrestUrl" :src="form.teamCrestUrl" alt="" class="pitch-scoreline-crest" />
            <span>{{ form.teamName || 'Team' }}</span>
          </div>
          <div v-if="form.scoreFor != null && form.scoreAgainst != null" class="pitch-scoreline-score">
            <span>{{ form.scoreFor }}</span><span class="dash">-</span><span>{{ form.scoreAgainst }}</span>
          </div>
          <div v-else class="pitch-scoreline-vs">vs</div>
          <div class="pitch-scoreline-team away">
            <img v-if="form.opponentCrestUrl" :src="form.opponentCrestUrl" alt="" class="pitch-scoreline-crest" />
            <span>{{ form.opponentName || 'Opponent' }}</span>
          </div>
        </div>

        <div class="pitch">
          <PitchMarkings />
          <div v-for="(row, ri) in previewRows" :key="ri" class="pitch-row" :class="`pitch-row--${row.kind}`">
            <div v-for="slot in row.items" :key="slot.slotIndex" class="pitch-slot">
              <div class="pitch-shirt" :class="{ goalkeeper: slot.slotIndex === 0 }" :style="shirtStyle(slot.slotIndex)">
                <span class="pitch-shirt-sleeve left"></span>
                <span class="pitch-shirt-sleeve right"></span>
                <span class="pitch-shirt-collar"></span>
                {{ slot.shirtNumber ?? '?' }}
                <span v-if="slot.captain" class="pitch-shirt-captain">C</span>
              </div>
              <div class="pitch-slot-name">{{ slot.name || '(empty)' }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <AthleteFormModal
      v-if="editingAthleteForModal"
      :athlete="editingAthleteForModal"
      @close="editingAthleteForModal = null"
      @saved="onAthleteEdited"
    />

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
import AthleteFormModal from '../components/AthleteFormModal.vue'
import Pagination from '../components/Pagination.vue'
import BoardListToolbar from '../components/BoardListToolbar.vue'
import { useBoardList } from '../composables/useBoardList'
import PitchMarkings from '../components/PitchMarkings.vue'
import { readableTextColor } from '../constants'
import { FORMATION_NAMES, slotLabels, slotCount, displayRowsFor } from '../services/formations'

const FOOTBALL_CATEGORY = 'Football'
const DEFAULT_KIT_COLOR = '#d92332'
const DEFAULT_GK_KIT_COLOR = '#f2c230'

const view = ref('list')
const lineups = ref([])
const loading = ref(true)

// Search / sort / paginate the board list.
const {
  searchTerm, sortKey, sortDir, page: lineupPage,
  filtered: filteredLineups, paged: pagedLineups, sorts: lineupSorts
} = useBoardList(lineups, {
  pageSize: 10,
  searchFields: [l => l.title, l => l.teamName, l => l.opponentName, l => l.formation],
  sorts: [
    { key: 'week', label: 'Week', accessor: l => l.weekStartDate, dir: 'desc' },
    { key: 'title', label: 'Title', accessor: l => l.title },
    { key: 'team', label: 'Team', accessor: l => l.teamName },
    { key: 'formation', label: 'Formation', accessor: l => l.formation }
  ]
})
const error = ref('')
const saving = ref(false)
const pendingDelete = ref(null)
const editingAthleteForModal = ref(null)
const editingLineupId = ref(null)
const showPreview = ref(false)

const form = reactive({
  title: '', competition: '', matchDate: '', weekStartDate: '', formation: '4-3-3',
  teamName: '', teamCrestUrl: '', opponentName: '', opponentCrestUrl: '',
  scoreFor: null, scoreAgainst: null, maxStrikes: 5, excludedFromBattle: false,
  kitColor: DEFAULT_KIT_COLOR, goalkeeperKitColor: DEFAULT_GK_KIT_COLOR, entireCategoryPool: false
})
const candidates = ref([])
const athleteSearchTerm = ref('')
const athleteSearchResults = ref([])

const athletePools = ref([])
const selectedPoolId = ref(null)
const linkedPoolIds = ref([]) // pools imported from during this session - sent on save
const poolsForFootball = computed(() => athletePools.value.filter(p => p.sport === FOOTBALL_CATEGORY))

const CANDIDATE_PAGE_SIZE = 25
const candidatePage = ref(1)
const candidateFilterTerm = ref('')
const filteredCandidates = computed(() => {
  const term = candidateFilterTerm.value.trim().toLowerCase()
  if (!term) return candidateDisplayOrder.value
  return candidateDisplayOrder.value.filter(c => c.name.toLowerCase().includes(term))
})
watch(candidateFilterTerm, () => { candidatePage.value = 1 })
const pagedCandidates = computed(() => {
  const start = (candidatePage.value - 1) * CANDIDATE_PAGE_SIZE
  return filteredCandidates.value.slice(start, start + CANDIDATE_PAGE_SIZE)
})

// For the editable candidate list specifically: correct answers (the actual
// starting XI) first, then everyone else - makes it easy to find and adjust
// the answers that matter most instead of hunting for them among decoys.
//
// This is a frozen snapshot, not a live computed - rebuilt explicitly when
// candidates are loaded/added/removed, deliberately NOT reactive to checking
// a candidate's own "correct" box. Otherwise a row would jump to a different
// spot in the list the moment you checked the very box you just clicked.
const candidateDisplayOrder = ref([])

function rebuildCandidateDisplayOrder() {
  const correct = candidates.value.filter(c => c.correct)
  const notCorrect = candidates.value.filter(c => !c.correct)
  candidateDisplayOrder.value = [...correct, ...notCorrect]
}

// Slot 0 is always the goalkeeper (see formations.js) - this is what lets the
// pitch render the keeper in a different kit without needing a dedicated
// "isGoalkeeper" field anywhere in the data model.
function shirtStyle(slotIndex) {
  const color = slotIndex === 0 ? (form.goalkeeperKitColor || DEFAULT_GK_KIT_COLOR) : (form.kitColor || DEFAULT_KIT_COLOR)
  return { '--kit-color': color, '--kit-text': readableTextColor(color) }
}

const requiredSlotCount = computed(() => slotCount(form.formation))
const filledSlotCount = computed(() => candidates.value.filter(c => c.correct && c.slotIndex != null).length)

const previewRows = computed(() => {
  const filled = candidates.value
    .filter(c => c.correct && c.slotIndex != null)
    .map(c => ({ slotIndex: c.slotIndex, shirtNumber: c.shirtNumber, captain: c.captain, name: c.name }))
  return displayRowsFor(form.formation, filled)
})

onMounted(() => {
  loadLineups()
  loadAthletePools()
})

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

async function loadAthletePools() {
  try {
    athletePools.value = await api.adminListAthletePools()
  } catch (e) {
    // non-critical - the pool dropdown just stays empty
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

function addCandidate(athlete, correct = false) {
  if (candidates.value.some(c => c.athleteId === athlete.id)) return
  candidates.value.push({
    athleteId: athlete.id, name: athlete.name, team: athlete.team, photoUrl: athlete.photoUrl,
    additionalPhotos: athlete.additionalPhotos || [],
    additionalDescriptions: athlete.additionalDescriptions || [],
    correct, shirtNumber: null, slotIndex: null, captain: false
  })
  athleteSearchTerm.value = ''
  athleteSearchResults.value = []
  rebuildCandidateDisplayOrder()
}

// When switching into "entire category" mode, decoy-only rows carry no
// data of their own (no assigned slot/shirt) and would otherwise sit in
// local state pointlessly, matching AdminGridsView's same toggle handler.
function onEntireCategoryToggle() {
  if (form.entireCategoryPool) {
    candidates.value = candidates.value.filter(c => c.correct)
    rebuildCandidateDisplayOrder()
  }
}

// For adding many at once (pool import, whole-category import) - same
// reasoning as AdminGridsView's addCandidatesBulk.
function addCandidatesBulk(athleteList) {
  const existingIds = new Set(candidates.value.map(c => c.athleteId))
  let added = 0
  for (const athlete of athleteList) {
    if (existingIds.has(athlete.id)) continue
    existingIds.add(athlete.id)
    candidates.value.push({
      athleteId: athlete.id, name: athlete.name, team: athlete.team, photoUrl: athlete.photoUrl,
      additionalPhotos: athlete.additionalPhotos || [],
      additionalDescriptions: athlete.additionalDescriptions || [],
      correct: false, shirtNumber: null, slotIndex: null, captain: false
    })
    added++
  }
  rebuildCandidateDisplayOrder()
  return added
}

function openEditAthlete(c) {
  editingAthleteForModal.value = {
    id: c.athleteId, name: c.name, sport: FOOTBALL_CATEGORY, team: c.team,
    photoUrl: c.photoUrl, additionalPhotos: c.additionalPhotos || [],
    additionalDescriptions: c.additionalDescriptions || []
  }
}

function onAthleteEdited(saved) {
  editingAthleteForModal.value = null
  const c = candidates.value.find(c => c.athleteId === saved.id)
  if (c) {
    c.name = saved.name
    c.team = saved.team
    c.photoUrl = saved.photoUrl
    c.additionalPhotos = saved.additionalPhotos || []
    c.additionalDescriptions = saved.additionalDescriptions || []
  }
  toast.show('Subject updated.')
}

async function importFromPool() {
  if (!selectedPoolId.value) return
  error.value = ''
  try {
    const pool = await api.adminGetAthletePool(selectedPoolId.value)
    const added = addCandidatesBulk(pool.members)
    if (!linkedPoolIds.value.includes(pool.id)) linkedPoolIds.value.push(pool.id)
    toast.show(`Imported ${added} subject(s) from "${pool.name}". This board will now automatically get any new members added to that pool later.`)
    selectedPoolId.value = null
  } catch (e) {
    error.value = 'Could not import that pool.'
  }
}

const importingAll = ref(false)
async function importAllInCategory() {
  error.value = ''
  importingAll.value = true
  try {
    const all = await api.adminSearchAthletes({ sport: FOOTBALL_CATEGORY })
    const added = addCandidatesBulk(all)
    toast.show(`Imported ${added} subject(s) from "Football" (${all.length - added} were already added).`)
  } catch (e) {
    error.value = 'Could not import subjects for that category.'
  } finally {
    importingAll.value = false
  }
}

function removeCandidate(c) {
  candidates.value = candidates.value.filter(x => x !== c)
  const maxPage = Math.max(1, Math.ceil(candidates.value.length / CANDIDATE_PAGE_SIZE))
  if (candidatePage.value > maxPage) candidatePage.value = maxPage
  rebuildCandidateDisplayOrder()
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
  form.kitColor = DEFAULT_KIT_COLOR
  form.goalkeeperKitColor = DEFAULT_GK_KIT_COLOR
  form.entireCategoryPool = false
  candidates.value = []
  candidatePage.value = 1
  candidateFilterTerm.value = ''
  athleteSearchTerm.value = ''
  athleteSearchResults.value = []
  selectedPoolId.value = null
  linkedPoolIds.value = []
  loadAthletePools()
  rebuildCandidateDisplayOrder()
}

function openCreate() {
  resetForm()
  editingLineupId.value = null
  view.value = 'builder'
}

async function openEdit(id) {
  error.value = ''
  linkedPoolIds.value = []
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
    form.kitColor = detail.kitColor || DEFAULT_KIT_COLOR
    form.goalkeeperKitColor = detail.goalkeeperKitColor || DEFAULT_GK_KIT_COLOR
    form.entireCategoryPool = detail.entireCategoryPool || false

    const entryByAthleteId = new Map(detail.entries.map(e => [e.athlete.id, e]))
    const sourceList = form.entireCategoryPool ? detail.entries.map(e => e.athlete) : detail.candidates
    candidates.value = sourceList.map(a => {
      const entry = entryByAthleteId.get(a.id)
      return {
        athleteId: a.id, name: a.name, team: a.team, photoUrl: a.photoUrl,
        additionalPhotos: a.additionalPhotos || [],
        additionalDescriptions: a.additionalDescriptions || [],
        correct: !!entry,
        shirtNumber: entry?.shirtNumber ?? null,
        slotIndex: entry?.slotIndex ?? null,
        captain: entry?.captain ?? false
      }
    })
    candidatePage.value = 1
    candidateFilterTerm.value = ''

    editingLineupId.value = id
    view.value = 'builder'
    loadAthletePools()
    rebuildCandidateDisplayOrder()
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
    kitColor: form.kitColor,
    goalkeeperKitColor: form.goalkeeperKitColor,
    entireCategoryPool: form.entireCategoryPool,
    candidateAthleteIds: form.entireCategoryPool ? [] : candidates.value.map(c => c.athleteId),
    entries: entries.map(c => ({
      athleteId: c.athleteId, shirtNumber: c.shirtNumber, slotIndex: c.slotIndex, captain: c.captain
    })),
    linkedPoolIds: linkedPoolIds.value
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
