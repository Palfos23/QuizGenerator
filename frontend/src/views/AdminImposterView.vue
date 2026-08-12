<template>
  <div>
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
        <h1 style="margin:0;">Imposter boards</h1>
        <button class="btn btn-primary" @click="openCreate">+ New board</button>
      </div>
      <p class="page-subtitle">
        A set of visible tiles where most subjects fit a theme and a few are deliberate imposters.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
      <div v-else-if="!grids.length" class="empty-state friendly">No boards yet - create your first one above.</div>

      <table v-else class="table">
        <thead>
          <tr><th>Title</th><th>Category</th><th>Tiles</th><th>Imposters</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="g in grids" :key="g.id">
            <td>{{ g.title }}</td>
            <td>{{ g.sport }}</td>
            <td>{{ g.tileCount }}</td>
            <td>{{ g.imposterCount }}</td>
            <td style="text-align:right; white-space:nowrap;">
              <button class="btn btn-secondary btn-sm" @click="openEdit(g.id)">Edit</button>
              <button class="btn btn-secondary btn-sm" style="margin-left:6px;" @click="confirmDelete(g)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </template>

    <template v-else>
      <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Back to boards</button>

      <div v-if="error" class="banner error" style="margin-top:16px;">{{ error }}</div>

      <div class="field" style="margin-top:16px;">
        <label>Title</label>
        <input type="text" v-model="form.title" placeholder="e.g. Top 10 Premier League goalscorers" />
      </div>

      <div class="field">
        <label>Description <span class="picker-hint">optional</span></label>
        <input type="text" v-model="form.description" placeholder="Shown to players above the board" />
      </div>

      <div class="field">
        <label>Category <span class="picker-hint">choose one</span></label>
        <div class="language-row">
          <button
            v-for="s in gridCategories.categories.value"
            :key="s"
            class="language-btn"
            :class="{ active: form.sport === s }"
            @click="form.sport = s; loadClubOptions()"
          >{{ s }}</button>
        </div>
      </div>

      <div class="field">
        <label>Display mode</label>
        <div class="language-row">
          <button class="language-btn" :class="{ active: form.displayMode === 'NAME_AND_PHOTO' }" @click="form.displayMode = 'NAME_AND_PHOTO'">Name + photo</button>
          <button class="language-btn" :class="{ active: form.displayMode === 'NAME_AND_LOGO' }" @click="form.displayMode = 'NAME_AND_LOGO'">Name + logo</button>
          <button class="language-btn" :class="{ active: form.displayMode === 'PHOTO_ONLY' }" @click="form.displayMode = 'PHOTO_ONLY'">Photo only</button>
        </div>
      </div>

      <div class="field" style="position:relative;">
        <label>Add subjects to the board</label>
        <input type="text" v-model="athleteSearchTerm" placeholder="Search subjects by name…" />
        <div v-if="athleteSearchResults.length" class="guess-results">
          <button
            v-for="a in athleteSearchResults"
            :key="a.id"
            type="button"
            class="guess-result-row"
            @click="addTile(a)"
          >{{ a.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ a.team }}</span></button>
        </div>
      </div>

      <div v-if="!tiles.length" class="empty-state" style="padding:20px;">No subjects added yet - search above.</div>

      <div v-for="(t, i) in tiles" :key="i" class="field" style="border:1px solid var(--border); border-radius:var(--radius-md); padding:12px 14px;">
        <div style="display:flex; justify-content:space-between; align-items:center; gap:10px;">
          <strong>{{ t.name }}</strong>
          <div style="display:flex; gap:6px;">
            <button type="button" class="btn btn-secondary btn-sm" @click="openEditAthlete(t)">Edit</button>
            <button type="button" class="chip-remove-btn" @click="tiles.splice(i, 1)">✕</button>
          </div>
        </div>

        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:400; margin-top:10px;">
          <input type="checkbox" v-model="t.imposter" style="width:auto;" @change="onImposterToggle(t)" />
          This is an imposter <span class="picker-hint">doesn't actually fit the theme</span>
        </label>

        <div v-if="t.imposter" style="margin-top:10px; position:relative;">
          <label style="text-transform:none; font-weight:400; font-size:0.85rem; color:var(--text-dim);">
            Which subject did this replace? <span class="picker-hint">revealed to players only after the board is fully flipped</span>
          </label>
          <input
            type="text"
            v-model="t.replacedSearchTerm"
            placeholder="Search subjects by name…"
            style="margin-top:6px;"
            @input="searchReplaced(t)"
          />
          <div v-if="t.replacedSearchResults && t.replacedSearchResults.length" class="guess-results">
            <button
              v-for="a in t.replacedSearchResults"
              :key="a.id"
              type="button"
              class="guess-result-row"
              @click="chooseReplaced(t, a)"
            >{{ a.name }}</button>
          </div>
          <div v-if="t.replacedAthleteId" style="margin-top:6px; color:var(--text-dim); font-size:0.85rem;">
            Replaces: <strong style="color:var(--text);">{{ t.replacedAthleteName }}</strong>
            <button type="button" class="chip-remove-btn" style="margin-left:6px;" @click="t.replacedAthleteId = null; t.replacedAthleteName = ''">✕</button>
          </div>
        </div>

        <div v-if="form.displayMode === 'NAME_AND_LOGO'" class="field" style="margin-top:10px; margin-bottom:0;">
          <label style="text-transform:none; font-weight:400; font-size:0.85rem; color:var(--text-dim);">Club logo for this tile</label>
          <select v-model="t.clubId">
            <option :value="null">No logo</option>
            <option v-for="club in clubOptions" :key="club.id" :value="club.id">{{ club.name }}</option>
          </select>
        </div>

        <div v-if="t.additionalPhotos && t.additionalPhotos.length" class="field" style="margin-top:10px; margin-bottom:0;">
          <label style="text-transform:none; font-weight:400; font-size:0.85rem; color:var(--text-dim);">Photo for this tile</label>
          <select v-model="t.selectedPhotoId">
            <option :value="null">Default photo</option>
            <option v-for="p in t.additionalPhotos" :key="p.id" :value="p.id">{{ p.label || 'Untitled photo' }}</option>
          </select>
        </div>

        <div v-if="t.additionalPhotos && t.additionalPhotos.length" class="field" style="margin-top:10px; margin-bottom:0;">
          <label style="text-transform:none; font-weight:400; font-size:0.85rem; color:var(--text-dim);">
            Photo once revealed as {{ t.imposter ? 'the imposter' : 'correct' }} <span class="picker-hint">optional</span>
          </label>
          <select v-if="t.imposter" v-model="t.revealImposterPhotoId">
            <option :value="null">Same as above</option>
            <option :value="'DEFAULT'">Default photo</option>
            <option v-for="p in t.additionalPhotos" :key="p.id" :value="p.id">{{ p.label || 'Untitled photo' }}</option>
          </select>
          <select v-else v-model="t.revealCorrectPhotoId">
            <option :value="null">Same as above</option>
            <option :value="'DEFAULT'">Default photo</option>
            <option v-for="p in t.additionalPhotos" :key="p.id" :value="p.id">{{ p.label || 'Untitled photo' }}</option>
          </select>
        </div>
      </div>

      <div v-if="tiles.length" style="margin-top:24px;">
        <h3 style="margin-bottom:10px;">Preview</h3>
        <div class="grid-tiles">
          <div v-for="t in tiles" :key="t.athleteId" class="grid-tile" :class="{ correct: !t.imposter, 'revealed-only': t.imposter }">
            <img
              v-if="previewImage(t)"
              :src="previewImage(t)"
              alt=""
              class="grid-tile-logo"
              :class="{ 'is-photo': previewIsPhoto(t) }"
              @error="$event.target.style.display = 'none'"
            />
            <div v-if="form.displayMode !== 'PHOTO_ONLY'" class="grid-tile-name">{{ t.name }}</div>
          </div>
        </div>
      </div>

      <div style="margin-top:20px; display:flex; align-items:center; gap:16px;">
        <button class="btn btn-primary" :disabled="saving || !canSave" @click="save">
          {{ saving ? 'Saving…' : 'Save board' }}
        </button>
      </div>
    </template>

    <AthleteFormModal
      v-if="editingAthleteForModal"
      :athlete="editingAthleteForModal"
      @close="editingAthleteForModal = null"
      @saved="onAthleteEdited"
    />

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this board?"
      :message="`'${pendingDelete.title}' will be permanently removed.`"
      confirm-text="Delete"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import gridCategories from '../services/gridCategories'
import ConfirmModal from '../components/ConfirmModal.vue'
import AthleteFormModal from '../components/AthleteFormModal.vue'
import toast from '../services/toast'

const view = ref('list')
const error = ref('')
const loading = ref(true)
const grids = ref([])
const saving = ref(false)
const pendingDelete = ref(null)
const editingAthleteForModal = ref(null)
const editingId = ref(null)

const form = ref({ title: '', description: '', sport: '', displayMode: 'NAME_AND_PHOTO' })
const tiles = ref([]) // [{ athleteId, name, imposter, replacedAthleteId, replacedAthleteName, replacedSearchTerm, replacedSearchResults, clubId }]

const clubOptions = ref([])
const athleteSearchTerm = ref('')
const athleteSearchResults = ref([])

const canSave = computed(() =>
  form.value.title.trim() && form.value.sport && tiles.value.length >= 2
)

onMounted(async () => {
  await gridCategories.ensureLoaded()
  await loadList()
})

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    grids.value = await api.adminListImposterGrids()
  } catch (e) {
    error.value = 'Could not load boards.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { title: '', description: '', sport: gridCategories.categories.value[0] || '', displayMode: 'NAME_AND_PHOTO' }
  tiles.value = []
  athleteSearchTerm.value = ''
  athleteSearchResults.value = []
  error.value = ''
  loadClubOptions()
  view.value = 'edit'
}

async function openEdit(id) {
  error.value = ''
  try {
    const detail = await api.adminGetImposterGrid(id)
    editingId.value = id
    form.value = {
      title: detail.title,
      description: detail.description || '',
      sport: detail.sport,
      displayMode: detail.displayMode
    }
    tiles.value = detail.tiles.map(t => ({
      athleteId: t.athlete.id,
      name: t.athlete.name,
      team: t.athlete.team,
      imposter: t.imposter,
      replacedAthleteId: t.replacedAthlete?.id ?? null,
      replacedAthleteName: t.replacedAthlete?.name ?? '',
      replacedSearchTerm: '',
      replacedSearchResults: [],
      clubId: t.club?.id ?? null,
      additionalPhotos: t.athlete.additionalPhotos || [],
      selectedPhotoId: t.selectedPhotoId ?? null,
      revealCorrectPhotoId: t.revealCorrectUseDefaultPhoto ? 'DEFAULT' : (t.revealCorrectPhotoId ?? null),
      revealImposterPhotoId: t.revealImposterUseDefaultPhoto ? 'DEFAULT' : (t.revealImposterPhotoId ?? null),
      photoUrl: t.athlete.photoUrl || null
    }))
    athleteSearchTerm.value = ''
    athleteSearchResults.value = []
    await loadClubOptions()
    view.value = 'edit'
  } catch (e) {
    error.value = 'Could not load that board.'
  }
}

async function loadClubOptions() {
  try {
    clubOptions.value = await api.adminSearchClubs(form.value.sport)
  } catch (e) {
    // non-critical - the club dropdown just stays empty
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
      athleteSearchResults.value = await api.adminSearchAthletes({ sport: form.value.sport, name: val })
    } catch (e) {
      // non-critical
    }
  }, 250)
})

function previewClub(t) {
  return clubOptions.value.find(c => c.id === t.clubId) || null
}
function previewImage(t) {
  if (form.value.displayMode === 'NAME_AND_LOGO') {
    return previewClub(t)?.logoUrl || null
  }
  if (t.selectedPhotoId && t.additionalPhotos) {
    const selected = t.additionalPhotos.find(p => p.id === t.selectedPhotoId)
    if (selected) return selected.photoUrl
  }
  return t.photoUrl || null
}
function previewIsPhoto(t) {
  return form.value.displayMode !== 'NAME_AND_LOGO'
}

function openEditAthlete(t) {
  editingAthleteForModal.value = {
    id: t.athleteId, name: t.name, sport: form.value.sport, team: t.team,
    photoUrl: t.photoUrl, additionalPhotos: t.additionalPhotos || []
  }
}

function onAthleteEdited(saved) {
  editingAthleteForModal.value = null
  const t = tiles.value.find(t => t.athleteId === saved.id)
  if (t) {
    t.name = saved.name
    t.team = saved.team
    t.photoUrl = saved.photoUrl
    t.additionalPhotos = saved.additionalPhotos || []
  }
  toast.show('Subject updated.')
}

function addTile(athlete) {
  if (tiles.value.some(t => t.athleteId === athlete.id)) return
  tiles.value.push({
    athleteId: athlete.id, name: athlete.name, team: athlete.team, imposter: false,
    replacedAthleteId: null, replacedAthleteName: '', replacedSearchTerm: '', replacedSearchResults: [],
    clubId: null, additionalPhotos: athlete.additionalPhotos || [], selectedPhotoId: null,
    revealCorrectPhotoId: null, revealImposterPhotoId: null,
    photoUrl: athlete.photoUrl || null
  })
  athleteSearchTerm.value = ''
  athleteSearchResults.value = []
}

function onImposterToggle(t) {
  if (!t.imposter) {
    t.replacedAthleteId = null
    t.replacedAthleteName = ''
  }
}

let replacedDebounce = null
function searchReplaced(t) {
  clearTimeout(replacedDebounce)
  const val = t.replacedSearchTerm
  if (!val || val.trim().length < 2) {
    t.replacedSearchResults = []
    return
  }
  replacedDebounce = setTimeout(async () => {
    try {
      t.replacedSearchResults = await api.adminSearchAthletes({ sport: form.value.sport, name: val })
    } catch (e) {
      // non-critical
    }
  }, 250)
}

function chooseReplaced(t, athlete) {
  t.replacedAthleteId = athlete.id
  t.replacedAthleteName = athlete.name
  t.replacedSearchTerm = ''
  t.replacedSearchResults = []
}

async function save() {
  error.value = ''
  saving.value = true
  const payload = {
    title: form.value.title,
    description: form.value.description,
    sport: form.value.sport,
    displayMode: form.value.displayMode,
    tiles: tiles.value.map(t => ({
      athleteId: t.athleteId,
      imposter: t.imposter,
      replacedAthleteId: t.imposter ? t.replacedAthleteId : null,
      clubId: t.clubId,
      selectedPhotoId: t.selectedPhotoId,
      revealCorrectPhotoId: t.revealCorrectPhotoId === 'DEFAULT' ? null : t.revealCorrectPhotoId,
      revealImposterPhotoId: t.revealImposterPhotoId === 'DEFAULT' ? null : t.revealImposterPhotoId,
      revealCorrectUseDefaultPhoto: t.revealCorrectPhotoId === 'DEFAULT',
      revealImposterUseDefaultPhoto: t.revealImposterPhotoId === 'DEFAULT'
    }))
  }
  try {
    if (editingId.value) {
      await api.adminUpdateImposterGrid(editingId.value, payload)
    } else {
      await api.adminCreateImposterGrid(payload)
    }
    await loadList()
    view.value = 'list'
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save this board.'
  } finally {
    saving.value = false
  }
}

function confirmDelete(g) {
  pendingDelete.value = g
}

async function doDelete() {
  const g = pendingDelete.value
  pendingDelete.value = null
  try {
    await api.adminDeleteImposterGrid(g.id)
    await loadList()
  } catch (e) {
    error.value = 'Could not delete that board.'
  }
}
</script>
