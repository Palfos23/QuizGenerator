<template>
  <div>
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
        <h1 style="margin:0;">Imposter-brett <span v-if="!loading && grids.length" class="header-count">{{ grids.length }}</span></h1>
        <button class="btn btn-primary" @click="openCreate">+ Nytt brett</button>
      </div>
      <p class="page-subtitle">
        Et sett med synlige ruter der de fleste emnene passer til et tema og noen få er bevisste imposters.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">Laster…</div>
      <div v-else-if="!grids.length" class="empty-state friendly">Ingen brett ennå - lag ditt første over.</div>

      <template v-else>
      <BoardListToolbar
        v-model:search="searchTerm"
        v-model:sort-key="sortKey"
        v-model:sort-dir="sortDir"
        :sorts="gridSorts"
        :total-count="grids.length"
        :filtered-count="filteredGrids.length"
        placeholder="Søk etter brett på tittel eller kategori…"
      />

      <div v-if="!filteredGrids.length" class="empty-state">Ingen brett samsvarer med søket ditt.</div>

      <table v-else class="table">
        <thead>
          <tr><th>Tittel</th><th>Kategori</th><th>Ruter</th><th>Imposters</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="g in pagedGrids" :key="g.id">
            <td>
              {{ g.title }}
              <span v-if="g.canExpire" class="tag" style="background:rgba(255,196,0,0.15); color:var(--gold); margin-left:6px;">Kan utløpe</span>
            </td>
            <td>{{ g.sport }}</td>
            <td>{{ g.tileCount }}</td>
            <td>{{ g.imposterCount }}</td>
            <td style="text-align:right; white-space:nowrap;">
              <button class="btn btn-secondary btn-sm" @click="openEdit(g.id)">Rediger</button>
              <button class="btn btn-secondary btn-sm" style="margin-left:6px;" @click="duplicateBoard(g.id)">⧉ Dupliser</button>
              <button class="btn btn-secondary btn-sm" style="margin-left:6px;" @click="confirmDelete(g)">Slett</button>
            </td>
          </tr>
        </tbody>
      </table>

      <Pagination v-model:page="gridPage" :page-size="10" :total-items="filteredGrids.length" />
      </template>
    </template>

    <template v-else>
      <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Tilbake til brett</button>

      <div v-if="error" class="banner error" style="margin-top:16px;">{{ error }}</div>

      <div class="field" style="margin-top:16px;">
        <label>Tittel<input type="text" v-model="form.title" placeholder="f.eks. Topp 10 målscorere i Premier League" /></label>
      </div>

      <div class="field">
        <label>Beskrivelse <span class="picker-hint">valgfritt</span><input type="text" v-model="form.description" placeholder="Vises for spillerne over brettet" /></label>
      </div>

      <div class="field">
        <label>Kategori <span class="picker-hint">velg en</span></label>
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
        <label>Visningsmodus</label>
        <div class="language-row">
          <button class="language-btn" :class="{ active: form.displayMode === 'NAME_AND_PHOTO' }" @click="form.displayMode = 'NAME_AND_PHOTO'">Navn + bilde</button>
          <button class="language-btn" :class="{ active: form.displayMode === 'NAME_AND_LOGO' }" @click="form.displayMode = 'NAME_AND_LOGO'">Navn + logo</button>
          <button class="language-btn" :class="{ active: form.displayMode === 'PHOTO_ONLY' }" @click="form.displayMode = 'PHOTO_ONLY'">Bare bilde</button>
          <button class="language-btn" :class="{ active: form.displayMode === 'NAME_ONLY' }" @click="form.displayMode = 'NAME_ONLY'">Bare navn</button>
          <button class="language-btn" :class="{ active: form.displayMode === 'NAME_UNTIL_REVEALED' }" @click="form.displayMode = 'NAME_UNTIL_REVEALED'">
            Navn, deretter bilde ved snuing
          </button>
        </div>
      </div>

      <div v-if="form.displayMode !== 'NAME_ONLY'" class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.fitImages" style="width:auto;" />
          Tilpass hele bildet i hver rute
        </label>
        <p class="picker-hint" style="margin:4px 0 0;">
          For flagg og logoer i full ramme - viser hele bildet med litt luft i stedet for å beskjære det til å fylle ruten. La stå av for portrettbilder.
        </p>
      </div>

      <div class="field option-card">
        <input type="checkbox" id="canExpire" v-model="form.canExpire" style="width:auto; margin-top:3px;" />
        <label for="canExpire" style="margin:0; text-transform:none; font-weight:400;">
          Kan utløpe
          <div style="color:var(--text-dim); font-size:0.8rem; font-weight:400; margin-top:2px;">
            f.eks. "nåværende all-time toppscorer" - merk dette slik at det vises på Innsikt-siden «Kan utløpe» for jevnlig gjennomgang. La stå uten hake for faste fakta.
          </div>
        </label>
      </div>

      <div class="field" style="position:relative;">
        <label>Legg til emner på brettet<input type="text" v-model="athleteSearchTerm" placeholder="Søk etter emner på navn…" /></label>
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

      <div v-if="!tiles.length" class="empty-state" style="padding:20px;">Ingen emner lagt til ennå - søk over.</div>

      <div v-for="(t, i) in tiles" :key="i" class="field" style="border:1px solid var(--border); border-radius: 2px; padding:12px 14px;">
        <div style="display:flex; justify-content:space-between; align-items:center; gap:10px;">
          <strong>{{ t.name }}</strong>
          <div style="display:flex; gap:6px;">
            <button type="button" class="btn btn-secondary btn-sm" @click="openEditAthlete(t)">Rediger</button>
            <button type="button" class="chip-remove-btn" @click="tiles.splice(i, 1)">✕</button>
          </div>
        </div>

        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:400; margin-top:10px;">
          <input type="checkbox" v-model="t.imposter" style="width:auto;" @change="onImposterToggle(t)" />
          Dette er en imposter <span class="picker-hint">passer egentlig ikke til temaet</span>
        </label>

        <div v-if="t.imposter" style="margin-top:10px; position:relative;">
          <label style="text-transform:none; font-weight:400; font-size:0.85rem; color:var(--text-dim);">
            Hvilket emne erstattet dette? <span class="picker-hint">vises for spillerne først når brettet er helt snudd</span>
          </label>
          <input
            type="text"
            v-model="t.replacedSearchTerm"
            placeholder="Søk etter emner på navn…"
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
            Erstatter: <strong style="color:var(--text);">{{ t.replacedAthleteName }}</strong>
            <button type="button" class="chip-remove-btn" style="margin-left:6px;" @click="t.replacedAthleteId = null; t.replacedAthleteName = ''">✕</button>
          </div>
        </div>

        <div v-if="form.displayMode === 'NAME_AND_LOGO'" class="field" style="margin-top:10px; margin-bottom:0;">
          <label style="text-transform:none; font-weight:400; font-size:0.85rem; color:var(--text-dim);">Klubblogo for denne ruten</label>
          <select v-model="t.clubId">
            <option :value="null">Ingen logo</option>
            <option v-for="club in clubOptions" :key="club.id" :value="club.id">{{ club.name }}</option>
          </select>
        </div>

        <div v-if="form.displayMode !== 'NAME_ONLY' && t.additionalPhotos && t.additionalPhotos.length" class="field" style="margin-top:10px; margin-bottom:0;">
          <label style="text-transform:none; font-weight:400; font-size:0.85rem; color:var(--text-dim);">Bilde for denne ruten</label>
          <select v-model="t.selectedPhotoId">
            <option :value="null">Standardbilde</option>
            <option v-for="p in t.additionalPhotos" :key="p.id" :value="p.id">{{ p.label || 'Bilde uten tittel' }}</option>
          </select>
        </div>

        <div v-if="form.displayMode !== 'NAME_ONLY' && t.additionalPhotos && t.additionalPhotos.length" class="field" style="margin-top:10px; margin-bottom:0;">
          <label style="text-transform:none; font-weight:400; font-size:0.85rem; color:var(--text-dim);">
            Bilde når avslørt som {{ t.imposter ? 'imposteren' : 'riktig' }} <span class="picker-hint">valgfritt</span>
          </label>
          <select v-if="t.imposter" v-model="t.revealImposterPhotoId">
            <option :value="null">Som over</option>
            <option :value="'DEFAULT'">Standardbilde</option>
            <option v-for="p in t.additionalPhotos" :key="p.id" :value="p.id">{{ p.label || 'Bilde uten tittel' }}</option>
          </select>
          <select v-else v-model="t.revealCorrectPhotoId">
            <option :value="null">Som over</option>
            <option :value="'DEFAULT'">Standardbilde</option>
            <option v-for="p in t.additionalPhotos" :key="p.id" :value="p.id">{{ p.label || 'Bilde uten tittel' }}</option>
          </select>
        </div>
      </div>

      <div v-if="tiles.length" style="margin-top:24px;">
        <h3 style="margin-bottom:10px;">Forhåndsvisning</h3>
        <div class="grid-tiles">
          <div v-for="t in tiles" :key="t.athleteId" class="grid-tile" :class="{ correct: !t.imposter, 'revealed-only': t.imposter }">
            <div v-if="form.displayMode === 'NAME_ONLY'" class="grid-tile-name-fill">{{ t.name }}</div>
            <template v-else>
              <img
                v-if="previewImage(t)"
                :src="previewImage(t)"
                alt=""
                class="grid-tile-logo"
                :class="{ 'is-photo': previewIsPhoto(t), 'is-fit': form.fitImages && previewIsPhoto(t) }"
                @error="$event.target.style.display = 'none'"
              />
              <div v-if="form.displayMode !== 'PHOTO_ONLY'" class="grid-tile-name">{{ t.name }}</div>
            </template>
          </div>
        </div>
      </div>

      <div style="margin-top:20px; display:flex; align-items:center; gap:16px;">
        <button v-if="editingId" class="btn btn-secondary" @click="duplicateAsNewVersion">
          ⧉ Dupliser som ny versjon
        </button>
        <button class="btn btn-primary" :disabled="saving || !canSave" @click="save">
          {{ saving ? 'Lagrer…' : 'Lagre brett' }}
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
      title="Slette dette brettet?"
      :message="`«${pendingDelete.title}» fjernes for godt.`"
      confirm-text="Slett"
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
import Pagination from '../components/Pagination.vue'
import BoardListToolbar from '../components/BoardListToolbar.vue'
import { useBoardList } from '../composables/useBoardList'
import toast from '../services/toast'

const view = ref('list')
const error = ref('')
const loading = ref(true)
const grids = ref([])

// Search / sort / paginate the board list.
const {
  searchTerm, sortKey, sortDir, page: gridPage,
  filtered: filteredGrids, paged: pagedGrids, sorts: gridSorts
} = useBoardList(grids, {
  pageSize: 10,
  searchFields: [g => g.title, g => g.sport, g => g.description],
  sorts: [
    { key: 'title', label: 'Tittel', accessor: g => g.title },
    { key: 'category', label: 'Kategori', accessor: g => g.sport },
    { key: 'tiles', label: 'Ruter', accessor: g => g.tileCount, dir: 'desc' },
    { key: 'imposters', label: 'Imposters', accessor: g => g.imposterCount, dir: 'desc' }
  ]
})
const saving = ref(false)
const pendingDelete = ref(null)
const editingAthleteForModal = ref(null)
const editingId = ref(null)

const form = ref({ title: '', description: '', sport: '', displayMode: 'NAME_AND_PHOTO', fitImages: false, canExpire: false })
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
    error.value = 'Kunne ikke laste brett.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { title: '', description: '', sport: gridCategories.categories.value[0] || '', displayMode: 'NAME_AND_PHOTO', fitImages: false, canExpire: false }
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
      displayMode: detail.displayMode,
      fitImages: detail.fitImages || false,
      canExpire: detail.canExpire || false
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
    error.value = 'Kunne ikke laste brettet.'
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
  toast.show('Emnet er oppdatert.')
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

// One-click duplicate straight from the list - loads the board into the
// builder exactly like Edit would, then immediately detaches it into a new
// unsaved copy via duplicateAsNewVersion, so there's no need to open Edit
// first just to reach the button at the bottom of the builder.
async function duplicateBoard(id) {
  await openEdit(id)
  if (editingId.value === id) duplicateAsNewVersion()
}

// Keeps every current form field and tile exactly as-is - only clearing the
// editing id, so the next save creates a brand new board via POST instead
// of overwriting the original via PUT. The original stays completely
// untouched until this copy is saved.
function duplicateAsNewVersion() {
  editingId.value = null
  form.value.title = form.value.title + ' (oppdatert)'
  toast.show('Du redigerer nå en ny duplikat - det opprinnelige brettet er urørt. Husk å lagre denne kopien.')
}

async function save() {
  error.value = ''
  saving.value = true
  const payload = {
    title: form.value.title,
    description: form.value.description,
    sport: form.value.sport,
    displayMode: form.value.displayMode,
    fitImages: form.value.fitImages,
    canExpire: form.value.canExpire,
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
    error.value = e.response?.data?.message || 'Kunne ikke lagre dette brettet.'
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
    error.value = 'Kunne ikke slette brettet.'
  }
}
</script>
