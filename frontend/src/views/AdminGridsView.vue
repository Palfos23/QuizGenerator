<template>
  <div>
    <!-- List view -->
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>Weekly grids <span v-if="!loading && grids.length" class="header-count">{{ grids.length }}</span></h1>
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
        No grids yet. Add a few subjects on the Subjects page, then create your first grid here.
      </div>

      <template v-else>
      <BoardListToolbar
        v-model:search="searchTerm"
        v-model:sort-key="sortKey"
        v-model:sort-dir="sortDir"
        :sorts="gridSorts"
        :total-count="grids.length"
        :filtered-count="filteredGrids.length"
        placeholder="Search grids by title or category…"
      />

      <div v-if="!filteredGrids.length" class="empty-state">No grids match your search.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="g in pagedGrids" :key="g.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              {{ g.title }}
              <span v-if="g.excludedFromGridBattle" class="tag" style="background:rgba(255,77,109,0.15); color:var(--coral); margin-left:6px;">Not in Grid Battle</span>
              <span v-if="g.entireCategoryPool" class="tag" style="background:rgba(61,220,151,0.15); color:var(--teal); margin-left:6px;">Auto pool</span>
            </div>
            <div class="saved-quiz-meta">{{ sportLabel(g.sport) }} · {{ g.entryCount }} entries · {{ g.maxStrikes }} {{ g.maxStrikes === 1 ? 'life' : 'lives' }} · week of {{ g.weekStartDate }}</div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(g.id)">Edit</button>
            <button class="btn btn-secondary btn-sm" @click="duplicateGrid(g.id)">⧉ Duplicate</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(g)">Delete</button>
          </div>
        </div>
      </div>

      <Pagination v-model:page="gridPage" :page-size="10" :total-items="filteredGrids.length" />
      </template>
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
        <label>Tile order <span class="picker-hint">how tiles are sorted, or whether they're ranked at all</span></label>
        <div class="language-row">
          <button type="button" class="language-btn" :class="{ active: form.ranked && !form.sortAscending }" @click="form.ranked = true; form.sortAscending = false">
            Highest first <span style="color:var(--text-dim); font-weight:400;">(goals, appearances…)</span>
          </button>
          <button type="button" class="language-btn" :class="{ active: form.ranked && form.sortAscending }" @click="form.ranked = true; form.sortAscending = true">
            Lowest first <span style="color:var(--text-dim); font-weight:400;">(finishing position…)</span>
          </button>
          <button type="button" class="language-btn" :class="{ active: !form.ranked }" @click="form.ranked = false">
            No ranking <span style="color:var(--text-dim); font-weight:400;">(image is the hint - movie covers…)</span>
          </button>
        </div>
      </div>

      <div class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.excludedFromGridBattle" style="width:auto;" />
          Exclude from Grid Battle
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          Use this once a grid's roster is outdated (e.g. a player crossed a stat threshold) -
          it stays fully intact for anyone who already played it, and still shows in the regular
          Weekly Grid archive, but stops being offered to Grid Battle's random or manual pick.
          Duplicate it first to create an updated version with the same content to edit.
        </p>
      </div>

      <div class="field">
        <label>Tile hint <span class="picker-hint">what a player sees before guessing this entry</span></label>
        <div class="language-row">
          <button type="button" class="language-btn" :class="{ active: form.revealMode === 'PHOTO' }" @click="form.revealMode = 'PHOTO'">
            Photo <span style="color:var(--text-dim); font-weight:400;">(the usual, for unranked grids)</span>
          </button>
          <button type="button" class="language-btn" :class="{ active: form.revealMode === 'DESCRIPTION' }" @click="form.revealMode = 'DESCRIPTION'">
            Description <span style="color:var(--text-dim); font-weight:400;">(a quote shown as the hint - for books, etc.)</span>
          </button>
        </div>
      </div>

      <div v-if="form.revealMode === 'PHOTO'" class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.fitImages" style="width:auto;" />
          Fit the whole image in each tile
        </label>
        <p class="picker-hint" style="margin:4px 0 0;">
          For flags and full-frame logos - shows the entire image with a little padding
          instead of cropping it to fill the square. Leave off for portrait photos.
        </p>
      </div>

      <div class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.entireCategoryPool" style="width:auto;" @change="onEntireCategoryToggle" />
          Use every subject in "{{ form.sport || '…' }}" as the pool
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          Every subject in this category becomes guessable automatically, including ones added to it
          later - nothing to re-import. You still pick which ones are the correct answers below.
        </p>
      </div>

      <div class="field">
        <label>
          {{ form.entireCategoryPool ? 'Correct answers' : 'Candidate pool' }}
          <span class="picker-hint">
            {{ form.entireCategoryPool ? 'search to mark a subject as a correct answer' : 'everyone guessable in this grid - correct and decoy' }}
          </span>
        </label>

        <div style="display:flex; gap:10px; flex-wrap:wrap; margin-bottom:10px;">
          <input
            type="text"
            v-model="athleteSearchTerm"
            :placeholder="form.entireCategoryPool ? 'Search subjects to mark as a correct answer…' : 'Search subjects by name…'"
            style="flex:1; min-width:180px;"
          />
        </div>

        <template v-if="!form.entireCategoryPool">
          <div v-if="poolsForSport.length" style="margin-bottom:10px;">
            <label style="font-size:0.85rem; color:var(--text-dim); text-transform:none; font-weight:400;">
              ...or import from a saved pool
            </label>
            <div style="display:flex; gap:8px; margin-top:6px; flex-wrap:wrap;">
              <select v-model="selectedPoolId" style="flex:1; min-width:200px;">
                <option :value="null">Choose a pool…</option>
                <option v-for="p in poolsForSport" :key="p.id" :value="p.id">{{ p.name }} ({{ p.memberCount }})</option>
              </select>
              <button class="btn btn-secondary btn-sm" :disabled="!selectedPoolId" @click="importFromPool">
                + Import pool
              </button>
            </div>
          </div>

          <div style="margin-bottom:10px;">
            <button class="btn btn-secondary btn-sm" :disabled="!form.sport || importingAllInCategory" @click="importAllInCategory">
              {{ importingAllInCategory ? 'Importing…' : `+ Import every subject in "${form.sport || '…'}"` }}
            </button>
          </div>
        </template>

        <div v-if="athleteSearchResults.length" class="guess-results" style="margin-bottom:10px;">
          <button
            v-for="a in athleteSearchResults"
            :key="a.id"
            class="guess-result-row"
            @click="addCandidate(a, form.entireCategoryPool)"
          >
            {{ a.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ a.team }}</span>
          </button>
        </div>

        <div v-if="!candidates.length" class="empty-state" style="padding:20px;">
          {{ form.entireCategoryPool ? 'No correct answers marked yet - search above.' : 'No subjects added yet - search above, or import a saved pool.' }}
        </div>

        <div v-else class="candidate-list">
          <input
            type="text"
            v-model="candidateFilterTerm"
            placeholder="Find someone already in this pool…"
            class="search-input"
            style="margin-bottom:12px;"
          />

          <div v-if="!filteredCandidatesForEditing.length" class="empty-state" style="padding:20px;">
            Nobody in this pool matches "{{ candidateFilterTerm }}".
          </div>

          <template v-else>
          <div v-for="c in pagedCandidates" :key="c.athleteId" class="candidate-row">
            <label v-if="!form.entireCategoryPool" style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600; margin:0;">
              <input type="checkbox" v-model="c.correct" style="width:auto;" />
              {{ c.name }} <span style="color:var(--text-dim); font-weight:400; font-size:0.85rem;">{{ c.team }}</span>
            </label>
            <div v-else style="font-weight:600;">
              {{ c.name }} <span style="color:var(--text-dim); font-weight:400; font-size:0.85rem;">{{ c.team }}</span>
            </div>
            <div v-if="c.correct" style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
              <input type="text" v-model="c.hintLabel" placeholder="Label (optional, e.g. FW)" style="width:110px;" />
              <input v-if="form.ranked" type="number" v-model.number="c.hintValue" placeholder="Value" style="width:80px;" />
              <select :value="logoSelectValue(c)" @change="onLogoSelectChange(c, $event.target.value)" style="width:190px;">
                <option value="none">No logo</option>
                <option v-if="c.photoUrl" value="own">Use this subject's own photo</option>
                <option v-for="club in clubOptions" :key="club.id" :value="String(club.id)">{{ club.name }}</option>
              </select>
              <select v-if="c.additionalPhotos && c.additionalPhotos.length" v-model="c.selectedPhotoId" style="width:190px;">
                <option :value="null">Default photo</option>
                <option v-for="p in c.additionalPhotos" :key="p.id" :value="p.id">{{ p.label || 'Untitled photo' }}</option>
              </select>
              <select v-if="form.revealMode === 'DESCRIPTION' && c.additionalDescriptions && c.additionalDescriptions.length" v-model="c.selectedDescriptionId" style="width:190px;">
                <option :value="null">First description</option>
                <option v-for="d in c.additionalDescriptions" :key="d.id" :value="d.id">{{ d.label || (d.text.length > 30 ? d.text.slice(0, 30) + '…' : d.text) }}</option>
              </select>
              <label style="display:flex; align-items:center; gap:6px; text-transform:none; font-weight:400; font-size:0.82rem; color:var(--text-dim); margin:0;">
                <input type="checkbox" v-model="c.showLogo" style="width:auto;" :disabled="!c.clubId && !c.useOwnPhotoAsLogo" />
                Show logo
              </label>
            </div>
            <button class="btn btn-secondary btn-sm" @click="openEditAthlete(c)">Edit</button>
            <button class="btn btn-danger btn-sm" @click="removeCandidate(c)">✕</button>
          </div>

          <Pagination v-model:page="candidatePage" :page-size="CANDIDATE_PAGE_SIZE" :total-items="filteredCandidatesForEditing.length" />
          </template>
        </div>
      </div>

      <div style="display:flex; gap:10px; flex-wrap:wrap;">
        <button class="btn btn-secondary" :disabled="!correctCandidates.length" @click="showPreview = true">
          Preview ({{ correctCandidates.length }} tiles)
        </button>
        <button v-if="editingGridId" class="btn btn-secondary" @click="duplicateAsNewVersion">
          ⧉ Duplicate as new version
        </button>
        <button class="btn btn-primary" :disabled="saving" @click="saveGrid">
          {{ saving ? 'Saving…' : 'Save grid' }}
        </button>
      </div>
    </template>

    <div v-if="showPreview" class="modal-backdrop" @click.self="showPreview = false">
      <div class="modal" style="max-width:800px;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
          <h2 style="margin:0;">{{ form.title || 'Untitled grid' }}</h2>
          <button class="btn btn-secondary btn-sm" @click="showPreview = false">Close</button>
        </div>
        <p class="page-subtitle" style="margin-top:0;">
          Everything shown revealed, for a quick check that hints, logos, and photos look right.
        </p>

        <div v-if="!correctCandidates.length" class="empty-state" style="padding:20px;">
          No tiles marked "correct" yet - check the box next to a candidate above.
        </div>
        <div v-else class="grid-tiles">
          <div v-for="c in sortedCorrectCandidates" :key="c.athleteId" class="grid-tile correct">
            <span class="grid-tile-status correct">✓</span>
            <div v-if="form.revealMode === 'DESCRIPTION'" class="grid-tile-description">
              {{ previewDescription(c) || '(no quote set for this subject)' }}
            </div>
            <img
              v-else-if="previewImage(c)"
              :src="previewImage(c)"
              alt=""
              class="grid-tile-logo"
              :class="{ 'is-photo': previewIsPhoto(c), 'is-fit': form.fitImages && previewIsPhoto(c) }"
              @error="$event.target.style.display = 'none'"
            />
            <div v-if="form.ranked || c.hintLabel" class="grid-tile-hint" :style="{ background: previewClubColor(c) || 'var(--gold)', color: readableTextColor(previewClubColor(c)) }">
              {{ form.ranked ? formatHint(c.hintLabel, c.hintValue ?? '?') : c.hintLabel }}
            </div>
            <div class="grid-tile-name">{{ c.name }}</div>
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
      title="Delete this grid?"
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
import { sportLabel, readableTextColor, formatHint } from '../constants'
import gridCategories from '../services/gridCategories'

const view = ref('list')
const grids = ref([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const pendingDelete = ref(null)
const editingAthleteForModal = ref(null)

// Search / sort / paginate the grid list.
const {
  searchTerm, sortKey, sortDir, page: gridPage,
  filtered: filteredGrids, paged: pagedGrids, sorts: gridSorts
} = useBoardList(grids, {
  pageSize: 10,
  searchFields: [g => g.title, g => g.sport],
  sorts: [
    { key: 'week', label: 'Week', accessor: g => g.weekStartDate, dir: 'desc' },
    { key: 'title', label: 'Title', accessor: g => g.title },
    { key: 'category', label: 'Category', accessor: g => g.sport },
    { key: 'entries', label: 'Entries', accessor: g => g.entryCount, dir: 'desc' }
  ]
})

const editingGridId = ref(null)
const form = reactive({
  title: '',
  theme: '',
  sport: gridCategories.categories.value[0] || '',
  weekStartDate: '',
  maxStrikes: 5,
  sortAscending: false,
  ranked: true,
  excludedFromGridBattle: false,
  revealMode: 'PHOTO',
  fitImages: false,
  entireCategoryPool: false
})
const candidates = ref([]) // [{ athleteId, name, team, correct, hintLabel, hintValue, clubId, showLogo, useOwnPhotoAsLogo }]

const CANDIDATE_PAGE_SIZE = 25
const candidatePage = ref(1)
const candidateFilterTerm = ref('')
const filteredCandidatesForEditing = computed(() => {
  const term = candidateFilterTerm.value.trim().toLowerCase()
  if (!term) return candidateDisplayOrder.value
  return candidateDisplayOrder.value.filter(c =>
    c.name.toLowerCase().includes(term) || (c.team || '').toLowerCase().includes(term)
  )
})
watch(candidateFilterTerm, () => { candidatePage.value = 1 })

const pagedCandidates = computed(() => {
  const start = (candidatePage.value - 1) * CANDIDATE_PAGE_SIZE
  return filteredCandidatesForEditing.value.slice(start, start + CANDIDATE_PAGE_SIZE)
})

const showPreview = ref(false)
const correctCandidates = computed(() => candidates.value.filter(c => c.correct))
const sortedCorrectCandidates = computed(() => {
  if (!form.ranked) return correctCandidates.value
  return form.sortAscending
    ? [...correctCandidates.value].sort((a, b) => (a.hintValue ?? 0) - (b.hintValue ?? 0))
    : [...correctCandidates.value].sort((a, b) => (b.hintValue ?? 0) - (a.hintValue ?? 0))
})

// For the editable candidate list specifically: correct answers first (in the
// same order they'll actually appear in the grid), then everyone else - makes
// it easy to find and adjust the answers that matter most.
//
// This is a frozen snapshot, not a live computed - rebuilt explicitly when
// candidates are loaded/added/removed or the sort direction is toggled, but
// deliberately NOT reactive to editing a candidate's own hint value or
// correct/decoy status. Otherwise a row could jump to a different position in
// the list the moment you changed the very value you were editing.
const candidateDisplayOrder = ref([])

function rebuildCandidateDisplayOrder() {
  const correct = candidates.value.filter(c => c.correct)
  const sortedCorrect = !form.ranked
    ? correct
    : form.sortAscending
      ? [...correct].sort((a, b) => (a.hintValue ?? 0) - (b.hintValue ?? 0))
      : [...correct].sort((a, b) => (b.hintValue ?? 0) - (a.hintValue ?? 0))
  const notCorrect = candidates.value.filter(c => !c.correct)
  candidateDisplayOrder.value = [...sortedCorrect, ...notCorrect]
}

watch(() => form.ranked, rebuildCandidateDisplayOrder)
watch(() => form.sortAscending, rebuildCandidateDisplayOrder)

function previewClub(c) {
  return clubOptions.value.find(club => club.id === c.clubId) || null
}
function previewClubColor(c) {
  return previewClub(c)?.color || null
}
function previewImage(c) {
  if (c.selectedPhotoId && c.additionalPhotos) {
    const selected = c.additionalPhotos.find(p => p.id === c.selectedPhotoId)
    if (selected) return selected.photoUrl
  }
  if (c.photoUrl) return c.photoUrl
  if (c.showLogo) return previewClub(c)?.logoUrl || null
  return null
}
function previewIsPhoto(c) {
  if (c.selectedPhotoId && c.additionalPhotos?.some(p => p.id === c.selectedPhotoId)) return true
  return !!c.photoUrl
}
// Mirrors the backend's resolvedDescription(): the entry's specifically
// picked description if one was chosen, else the subject's first
// description, else nothing to show.
function previewDescription(c) {
  if (c.selectedDescriptionId && c.additionalDescriptions) {
    const selected = c.additionalDescriptions.find(d => d.id === c.selectedDescriptionId)
    if (selected) return selected.text
  }
  return c.additionalDescriptions?.[0]?.text || null
}

function logoSelectValue(c) {
  if (c.useOwnPhotoAsLogo) return 'own'
  if (c.clubId) return String(c.clubId)
  return 'none'
}
function onLogoSelectChange(c, value) {
  if (value === 'none') {
    c.clubId = null
    c.useOwnPhotoAsLogo = false
  } else if (value === 'own') {
    c.clubId = null
    c.useOwnPhotoAsLogo = true
  } else {
    c.clubId = Number(value)
    c.useOwnPhotoAsLogo = false
  }
}

function openEditAthlete(c) {
  editingAthleteForModal.value = {
    id: c.athleteId, name: c.name, sport: form.sport, team: c.team,
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

function removeCandidate(c) {
  const realIndex = candidates.value.indexOf(c)
  if (realIndex !== -1) candidates.value.splice(realIndex, 1)
  const maxPage = Math.max(1, Math.ceil(candidates.value.length / CANDIDATE_PAGE_SIZE))
  if (candidatePage.value > maxPage) candidatePage.value = maxPage
  rebuildCandidateDisplayOrder()
}
const clubOptions = ref([])
const athletePools = ref([])
const selectedPoolId = ref(null)
const linkedPoolIds = ref([]) // pools imported from during this session - sent on save
const poolsForSport = computed(() => athletePools.value.filter(p => p.sport === form.sport))

const athleteSearchTerm = ref('')
const athleteSearchResults = ref([])

onMounted(async () => {
  loadGrids()
  loadClubOptions()
  loadAthletePools()
  await gridCategories.ensureLoaded()
  if (!form.sport) form.sport = gridCategories.categories.value[0] || ''
})

async function loadClubOptions() {
  try {
    clubOptions.value = await api.adminSearchClubs(form.sport)
  } catch (e) {
    // non-critical - the club dropdown just stays empty
  }
}

async function loadAthletePools() {
  try {
    athletePools.value = await api.adminListAthletePools()
  } catch (e) {
    // non-critical - the pool dropdown just stays empty
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

// correct defaults to false (a plain decoy) - the "entire category" search
// box passes true instead, since there's no reason to ever add a decoy by
// hand in that mode (everyone else in the category already is one).
function addCandidate(athlete, correct = false) {
  if (candidates.value.some(c => c.athleteId === athlete.id)) return
  candidates.value.push({
    athleteId: athlete.id, name: athlete.name, team: athlete.team, photoUrl: athlete.photoUrl,
    additionalPhotos: athlete.additionalPhotos || [],
    additionalDescriptions: athlete.additionalDescriptions || [],
    correct, hintLabel: '', hintValue: null, clubId: null, showLogo: true, useOwnPhotoAsLogo: false,
    selectedPhotoId: null, selectedDescriptionId: null
  })
  rebuildCandidateDisplayOrder()
}

// Decoys are meaningless once every subject in the category is automatically
// guessable - drop any that were added before switching modes so the list
// only shows what actually matters here (the correct answers).
function onEntireCategoryToggle() {
  if (form.entireCategoryPool) {
    candidates.value = candidates.value.filter(c => c.correct)
    rebuildCandidateDisplayOrder()
  }
}

// For adding many at once (pool import, whole-category import) - rebuilds the
// display order once at the end instead of once per item, which matters once
// the list being imported gets into the hundreds.
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
      correct: false, hintLabel: '', hintValue: null, clubId: null, showLogo: true, useOwnPhotoAsLogo: false,
      selectedPhotoId: null, selectedDescriptionId: null
    })
    added++
  }
  rebuildCandidateDisplayOrder()
  return added
}

async function importFromPool() {
  if (!selectedPoolId.value) return
  error.value = ''
  try {
    const pool = await api.adminGetAthletePool(selectedPoolId.value)
    const added = addCandidatesBulk(pool.members)
    if (!linkedPoolIds.value.includes(pool.id)) linkedPoolIds.value.push(pool.id)
    toast.show(`Imported ${added} subject(s) from "${pool.name}". This grid will now automatically get any new members added to that pool later.`)
    selectedPoolId.value = null
  } catch (e) {
    error.value = 'Could not import that pool.'
  }
}

const importingAllInCategory = ref(false)
async function importAllInCategory() {
  if (!form.sport) return
  error.value = ''
  importingAllInCategory.value = true
  try {
    const all = await api.adminSearchAthletes({ sport: form.sport })
    const added = addCandidatesBulk(all)
    toast.show(`Imported ${added} subject(s) from "${form.sport}" (${all.length - added} were already in this grid).`)
  } catch (e) {
    error.value = 'Could not import subjects for that category.'
  } finally {
    importingAllInCategory.value = false
  }
}

function resetForm() {
  form.title = ''
  form.theme = ''
  form.sport = gridCategories.categories.value[0] || ''
  form.weekStartDate = ''
  form.maxStrikes = 5
  form.sortAscending = false
  form.ranked = true
  form.excludedFromGridBattle = false
  form.revealMode = 'PHOTO'
  form.fitImages = false
  form.entireCategoryPool = false
  candidates.value = []
  candidatePage.value = 1
  candidateFilterTerm.value = ''
  athleteSearchTerm.value = ''
  athleteSearchResults.value = []
  selectedPoolId.value = null
  linkedPoolIds.value = []
  loadClubOptions()
  loadAthletePools()
  rebuildCandidateDisplayOrder()
}

function openCreate() {
  resetForm()
  editingGridId.value = null
  view.value = 'builder'
}

async function openEdit(id) {
  error.value = ''
  linkedPoolIds.value = []
  try {
    const detail = await api.adminGetGrid(id)
    form.title = detail.title
    form.theme = detail.theme
    form.sport = detail.sport
    form.weekStartDate = detail.weekStartDate
    form.maxStrikes = detail.maxStrikes
    form.sortAscending = detail.sortAscending
    form.ranked = detail.ranked
    form.excludedFromGridBattle = detail.excludedFromGridBattle
    form.revealMode = detail.revealMode || 'PHOTO'
    form.fitImages = detail.fitImages || false
    form.entireCategoryPool = detail.entireCategoryPool || false

    // In "entire category" mode there's no stored candidate list at all (see
    // GridAdminService) - the editable list is built from the correct answers
    // (entries) instead, since that's the only place their athlete data lives.
    const entryByAthleteId = new Map(detail.entries.map(e => [e.athlete.id, e]))
    const sourceList = form.entireCategoryPool ? detail.entries.map(e => e.athlete) : detail.candidates
    candidates.value = sourceList.map(a => {
      const entry = entryByAthleteId.get(a.id)
      return {
        athleteId: a.id, name: a.name, team: a.team, photoUrl: a.photoUrl,
        additionalPhotos: a.additionalPhotos || [],
        additionalDescriptions: a.additionalDescriptions || [],
        correct: !!entry,
        hintLabel: entry?.hintLabel || '',
        hintValue: entry?.hintValue ?? null,
        clubId: entry?.club?.id ?? null,
        showLogo: entry?.showLogo ?? true,
        useOwnPhotoAsLogo: entry?.useOwnPhotoAsLogo ?? false,
        selectedPhotoId: entry?.selectedPhotoId ?? null,
        selectedDescriptionId: entry?.selectedDescriptionId ?? null
      }
    })
    candidatePage.value = 1
    candidateFilterTerm.value = ''

    editingGridId.value = id
    view.value = 'builder'
    loadClubOptions()
    loadAthletePools()
    rebuildCandidateDisplayOrder()
  } catch (e) {
    error.value = 'Could not load that grid.'
  }
}

// One-click duplicate straight from the list - loads the grid into the
// builder exactly like Edit would, then immediately detaches it into a new
// unsaved copy via duplicateAsNewVersion, so there's no need to open Edit
// first just to reach the button buried at the bottom of the builder.
async function duplicateGrid(id) {
  await openEdit(id)
  if (editingGridId.value === id) duplicateAsNewVersion()
}

function duplicateAsNewVersion() {
  // Keeps every current form field and candidate/entry exactly as-is - only
  // clearing the editing id, so the next save creates a brand new grid via
  // POST instead of overwriting the original via PUT. The original stays
  // completely untouched until you separately go back and check "Exclude
  // from Grid Battle" on it once you're happy with this copy.
  editingGridId.value = null
  form.title = form.title + ' (updated)'
  form.excludedFromGridBattle = false
  toast.show('Now editing a new duplicate - the original grid is untouched. Remember to save this copy.')
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
  if (form.ranked && entries.some(c => c.hintValue === null || c.hintValue === '')) {
    error.value = 'Every correct answer needs a hint value (the label is optional).'
    return
  }

  const payload = {
    title: form.title,
    theme: form.theme,
    sport: form.sport,
    weekStartDate: form.weekStartDate,
    maxStrikes: form.maxStrikes,
    sortAscending: form.sortAscending,
    ranked: form.ranked,
    excludedFromGridBattle: form.excludedFromGridBattle,
    revealMode: form.revealMode,
    fitImages: form.fitImages,
    entireCategoryPool: form.entireCategoryPool,
    candidateAthleteIds: form.entireCategoryPool ? [] : candidates.value.map(c => c.athleteId),
    linkedPoolIds: linkedPoolIds.value,
    entries: entries.map(c => ({
      athleteId: c.athleteId, hintLabel: c.hintLabel, hintValue: form.ranked ? c.hintValue : null,
      clubId: c.clubId, showLogo: c.showLogo, useOwnPhotoAsLogo: c.useOwnPhotoAsLogo,
      selectedPhotoId: c.selectedPhotoId, selectedDescriptionId: c.selectedDescriptionId
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
