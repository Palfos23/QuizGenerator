<template>
  <div>
    <!-- List view -->
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>{{ $t('Ukentlige grids') }} <span v-if="!loading && grids.length" class="header-count">{{ grids.length }}</span></h1>
          <p class="page-subtitle">
            {{ $t('Lag og administrer de ukentlige gjette-grid-utfordringene.') }}
            <router-link to="/admin/clubs">{{ $t('Administrer klubber →') }}</router-link>
          </p>
        </div>
        <button class="btn btn-primary" @click="openCreate">{{ $t('+ Lag grid') }}</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

      <div v-else-if="!grids.length" class="empty-state friendly">
        {{ $t('Ingen grids ennå. Legg til noen emner på siden Emner, og lag ditt første grid her.') }}
      </div>

      <template v-else>
      <BoardListToolbar
        v-model:search="searchTerm"
        v-model:sort-key="sortKey"
        v-model:sort-dir="sortDir"
        :sorts="gridSorts"
        :total-count="grids.length"
        :filtered-count="filteredGrids.length"
        :placeholder="$t('Søk etter grids på tittel eller kategori…')"
      />

      <div v-if="!filteredGrids.length" class="empty-state">{{ $t('Ingen grids samsvarer med søket ditt.') }}</div>

      <div v-else class="saved-quiz-list">
        <div v-for="g in pagedGrids" :key="g.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              {{ g.title }}
              <span v-if="g.excludedFromGridBattle" class="tag" style="background:rgba(179, 38, 30, 0.15); color:var(--coral); margin-left:6px;">{{ $t('Ikke i Grid Battle') }}</span>
              <span v-if="g.entireCategoryPool" class="tag" style="background:rgba(31, 122, 80, 0.15); color:var(--teal); margin-left:6px;">{{ $t('Automatisk utvalg') }}</span>
              <span v-if="g.canExpire" class="tag" style="background:rgba(255,196,0,0.15); color:var(--gold); margin-left:6px;">{{ $t('Kan utløpe') }}</span>
            </div>
            <div class="saved-quiz-meta">{{ sportLabel(g.sport) }} · {{ g.entryCount }} {{ $t('oppføringer ·') }} {{ $t('{0} liv¦{0} liv', [g.maxStrikes]) }} {{ $t('· uke') }} {{ g.weekStartDate }}</div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(g.id)">{{ $t('Rediger') }}</button>
            <button class="btn btn-secondary btn-sm" @click="duplicateGrid(g.id)">{{ $t('⧉ Dupliser') }}</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(g)">{{ $t('Slett') }}</button>
          </div>
        </div>
      </div>

      <Pagination v-model:page="gridPage" :page-size="10" :total-items="filteredGrids.length" />
      </template>
    </template>

    <!-- Builder view -->
    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <h1 style="margin:0;">{{ editingGridId ? $t('Rediger grid') : $t('Lag grid') }}</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">{{ $t('← Tilbake til listen') }}</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>{{ $t('Tittel') }}<input type="text" v-model="form.title" :placeholder="$t('f.eks. Tottenham-spillere med 10+ mål')" /></label>
      </div>

      <div class="field">
        <label>{{ $t('Tema / instruksjoner') }}<textarea v-model="form.theme" :placeholder="$t('Nevn hver Tottenham-spiller med mer enn 10 mål.')"></textarea></label>
      </div>

      <div class="field">
        <label>{{ $t('Kategori') }} <span class="picker-hint">{{ $t('velg en') }}</span></label>
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
          <label>{{ $t('Startdato for uken') }}<input type="date" v-model="form.weekStartDate" /></label>
          <p v-if="weekConflict" style="margin:6px 0 0; color:var(--coral); font-size:0.82rem;">
            ⚠ '{{ weekConflict.title }}{{ $t('\' bruker allerede denne uken.') }}
          </p>
        </div>
        <div style="flex:1; min-width:140px;">
          <label>{{ $t('Maks liv') }}<input type="number" min="1" max="20" v-model.number="form.maxStrikes" /></label>
        </div>
      </div>

      <div class="field">
        <label>{{ $t('Rekkefølge på rutene') }} <span class="picker-hint">{{ $t('hvordan rutene sorteres, eller om de rangeres i det hele tatt') }}</span></label>
        <div class="language-row">
          <button type="button" class="language-btn" :class="{ active: form.ranked && !form.sortAscending }" @click="form.ranked = true; form.sortAscending = false">
            {{ $t('Høyest først') }} <span style="color:var(--text-dim); font-weight:400;">{{ $t('(mål, kamper…)') }}</span>
          </button>
          <button type="button" class="language-btn" :class="{ active: form.ranked && form.sortAscending }" @click="form.ranked = true; form.sortAscending = true">
            {{ $t('Lavest først') }} <span style="color:var(--text-dim); font-weight:400;">{{ $t('(sluttplassering…)') }}</span>
          </button>
          <button type="button" class="language-btn" :class="{ active: !form.ranked }" @click="form.ranked = false">
            {{ $t('Ingen rangering') }} <span style="color:var(--text-dim); font-weight:400;">{{ $t('(bildet er hintet - filmplakater…)') }}</span>
          </button>
        </div>
      </div>

      <div class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.excludedFromGridBattle" style="width:auto;" />
          {{ $t('Utelat fra Grid Battle') }}
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          {{ $t('Bruk dette når et grids tropp er utdatert (f.eks. en spiller passerte en statistikkgrense) - det forblir helt intakt for alle som allerede har spilt det, og vises fortsatt i det vanlige arkivet for Ukens grid, men slutter å tilbys i Grid Battles tilfeldige eller manuelle valg. Dupliser det først for å lage en oppdatert versjon med samme innhold å redigere.') }}
        </p>
      </div>

      <div class="field option-card">
        <input type="checkbox" id="canExpire" v-model="form.canExpire" style="width:auto; margin-top:3px;" />
        <label for="canExpire" style="margin:0; text-transform:none; font-weight:400;">
          {{ $t('Kan utløpe') }}
          <div style="color:var(--text-dim); font-size:0.8rem; font-weight:400; margin-top:2px;">
            {{ $t('f.eks. "nåværende all-time toppscorer" - merk dette slik at det vises på Innsikt-siden «Kan utløpe» for jevnlig gjennomgang. La stå uten hake for faste fakta.§AdminGridsViewT') }}
          </div>
        </label>
      </div>

      <div class="field">
        <label>{{ $t('Rutehint') }} <span class="picker-hint">{{ $t('det en spiller ser før den gjetter denne oppføringen') }}</span></label>
        <div class="language-row">
          <button type="button" class="language-btn" :class="{ active: form.revealMode === 'PHOTO' }" @click="form.revealMode = 'PHOTO'">
            {{ $t('Bilde') }} <span style="color:var(--text-dim); font-weight:400;">{{ $t('(det vanlige, for grids uten rangering)') }}</span>
          </button>
          <button type="button" class="language-btn" :class="{ active: form.revealMode === 'DESCRIPTION' }" @click="form.revealMode = 'DESCRIPTION'">
            {{ $t('Beskrivelse') }} <span style="color:var(--text-dim); font-weight:400;">{{ $t('(et sitat vist som hint - for bøker osv.)') }}</span>
          </button>
        </div>
      </div>

      <div v-if="form.revealMode === 'PHOTO'" class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.fitImages" style="width:auto;" />
          {{ $t('Tilpass hele bildet i hver rute') }}
        </label>
        <p class="picker-hint" style="margin:4px 0 0;">
          {{ $t('For flagg og logoer i full ramme - viser hele bildet med litt luft i stedet for å beskjære det til å fylle ruten. La stå av for portrettbilder.') }}
        </p>
      </div>

      <div class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.entireCategoryPool" style="width:auto;" @change="onEntireCategoryToggle" />
          {{ $t('Bruk alle emner i "') }}{{ form.sport || '…' }}{{ $t('" som utvalg') }}
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          {{ $t('Alle emner i denne kategorien kan gjettes automatisk, inkludert de som legges til senere - ingenting å importere på nytt. Du velger fortsatt hvilke som er de riktige svarene under.') }}
        </p>
      </div>

      <div class="field">
        <label>
          {{ form.entireCategoryPool ? $t('Riktige svar') : $t('Kandidatutvalg') }}
          <span class="picker-hint">
            {{ form.entireCategoryPool ? $t('søk for å markere et emne som riktig svar') : $t('alle som kan gjettes i dette gridet - riktige og avledere') }}
          </span>
        </label>

        <div style="display:flex; gap:10px; flex-wrap:wrap; margin-bottom:10px;">
          <input
            type="text"
            v-model="athleteSearchTerm"
            :placeholder="form.entireCategoryPool ? $t('Søk etter emner å markere som riktig svar…') : $t('Søk etter emner på navn…')"
            style="flex:1; min-width:180px;"
          />
        </div>

        <template v-if="!form.entireCategoryPool">
          <div v-if="poolsForSport.length" style="margin-bottom:10px;">
            <label style="font-size:0.85rem; color:var(--text-dim); text-transform:none; font-weight:400;">
              {{ $t('...eller importer fra et lagret utvalg') }}
            </label>
            <div style="display:flex; gap:8px; margin-top:6px; flex-wrap:wrap;">
              <select v-model="selectedPoolId" style="flex:1; min-width:200px;">
                <option :value="null">{{ $t('Velg et utvalg…') }}</option>
                <option v-for="p in poolsForSport" :key="p.id" :value="p.id">{{ p.name }} ({{ p.memberCount }})</option>
              </select>
              <button class="btn btn-secondary btn-sm" :disabled="!selectedPoolId" @click="importFromPool">
                {{ $t('+ Importer utvalg') }}
              </button>
            </div>
          </div>

          <div style="margin-bottom:10px;">
            <button class="btn btn-secondary btn-sm" :disabled="!form.sport || importingAllInCategory" @click="importAllInCategory">
              {{ importingAllInCategory ? $t('Importerer…') : $t('+ Importer alle emner i "{0}"', [form.sport || '…']) }}
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
          {{ form.entireCategoryPool ? $t('Ingen riktige svar markert ennå - søk over.') : $t('Ingen emner lagt til ennå - søk over, eller importer et lagret utvalg.') }}
        </div>

        <div v-else class="candidate-list">
          <input
            type="text"
            v-model="candidateFilterTerm"
            :placeholder="$t('Finn noen som allerede er i dette utvalget…')"
            class="search-input"
            style="margin-bottom:12px;"
          />

          <div v-if="!filteredCandidatesForEditing.length" class="empty-state" style="padding:20px;">
            {{ $t('Ingen i dette utvalget samsvarer med "') }}{{ candidateFilterTerm }}".
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
              <input type="text" v-model="c.hintLabel" :placeholder="$t('Etikett (valgfritt, f.eks. FW)')" style="width:110px;" />
              <input v-if="form.ranked" type="number" v-model.number="c.hintValue" :placeholder="$t('Verdi')" style="width:80px;" />
              <select :value="logoSelectValue(c)" @change="onLogoSelectChange(c, $event.target.value)" style="width:190px;">
                <option value="none">{{ $t('Ingen logo') }}</option>
                <option v-if="c.photoUrl" value="own">{{ $t('Bruk emnets eget bilde') }}</option>
                <option v-for="club in clubOptions" :key="club.id" :value="String(club.id)">{{ club.name }}</option>
              </select>
              <select v-if="c.additionalPhotos && c.additionalPhotos.length" v-model="c.selectedPhotoId" style="width:190px;">
                <option :value="null">{{ $t('Standardbilde') }}</option>
                <option v-for="p in c.additionalPhotos" :key="p.id" :value="p.id">{{ p.label || $t('Bilde uten tittel') }}</option>
              </select>
              <select v-if="form.revealMode === 'DESCRIPTION' && c.additionalDescriptions && c.additionalDescriptions.length" v-model="c.selectedDescriptionId" style="width:190px;">
                <option :value="null">{{ $t('Første beskrivelse') }}</option>
                <option v-for="d in c.additionalDescriptions" :key="d.id" :value="d.id">{{ d.label || (d.text.length > 30 ? d.text.slice(0, 30) + '…' : d.text) }}</option>
              </select>
              <label style="display:flex; align-items:center; gap:6px; text-transform:none; font-weight:400; font-size:0.82rem; color:var(--text-dim); margin:0;">
                <input type="checkbox" v-model="c.showLogo" style="width:auto;" :disabled="!c.clubId && !c.useOwnPhotoAsLogo" />
                {{ $t('Vis logo') }}
              </label>
            </div>
            <button class="btn btn-secondary btn-sm" @click="openEditAthlete(c)">{{ $t('Rediger') }}</button>
            <button class="btn btn-danger btn-sm" @click="removeCandidate(c)">✕</button>
          </div>

          <Pagination v-model:page="candidatePage" :page-size="CANDIDATE_PAGE_SIZE" :total-items="filteredCandidatesForEditing.length" />
          </template>
        </div>
      </div>

      <TranslationEditor v-if="editingGridId" type="GRID" :entity-id="editingGridId" />

      <div style="display:flex; gap:10px; flex-wrap:wrap;">
        <button class="btn btn-secondary" :disabled="!correctCandidates.length" @click="showPreview = true">
          {{ $t('Forhåndsvisning (') }}{{ correctCandidates.length }} {{ $t('ruter)') }}
        </button>
        <button v-if="editingGridId" class="btn btn-secondary" @click="duplicateAsNewVersion">
          {{ $t('⧉ Dupliser som ny versjon') }}
        </button>
        <button class="btn btn-primary" :disabled="saving" @click="saveGrid">
          {{ saving ? $t('Lagrer…') : $t('Lagre grid') }}
        </button>
      </div>
    </template>

    <div v-if="showPreview" class="modal-backdrop" @click.self="showPreview = false">
      <div class="modal" style="max-width:800px;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
          <h2 style="margin:0;">{{ form.title || $t('Grid uten tittel') }}</h2>
          <button class="btn btn-secondary btn-sm" @click="showPreview = false">{{ $t('Lukk§AdminGridsViewT') }}</button>
        </div>
        <p class="page-subtitle" style="margin-top:0;">
          {{ $t('Alt vist avslørt, for en rask sjekk av at hint, logoer og bilder ser riktige ut.') }}
        </p>

        <div v-if="!correctCandidates.length" class="empty-state" style="padding:20px;">
          {{ $t('Ingen ruter markert som «riktig» ennå - huk av ved en kandidat over.') }}
        </div>
        <div v-else class="grid-tiles">
          <div v-for="c in sortedCorrectCandidates" :key="c.athleteId" class="grid-tile correct">
            <span class="grid-tile-status correct">✓</span>
            <div v-if="form.revealMode === 'DESCRIPTION'" class="grid-tile-description">
              {{ previewDescription(c) || $t('(ingen sitat satt for dette emnet)') }}
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
      :title="$t('Slette dette gridet?')"
      :message="$t('«{0}» og alles fremgang på det fjernes for godt.', [pendingDelete.title])"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import TranslationEditor from '../components/TranslationEditor.vue'
import { t as _t } from '../i18n'
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
    { key: 'week', label: _t('Uke'), accessor: g => g.weekStartDate, dir: 'desc' },
    { key: 'title', label: _t('Tittel'), accessor: g => g.title },
    { key: 'category', label: _t('Kategori'), accessor: g => g.sport },
    { key: 'entries', label: _t('Oppføringer'), accessor: g => g.entryCount, dir: 'desc' }
  ]
})

const editingGridId = ref(null)

// The date picker itself can't show which weeks are taken - the browser owns
// that popup entirely - so this flags a collision as soon as one is picked
// instead of only after "Save grid" round-trips to the server and back with
// an error. Mirrors GridAdminService.validateDateNotTaken. Excludes whichever
// grid is currently being edited, so editing a grid doesn't flag its own
// existing date as a conflict with itself.
const weekConflict = computed(() =>
  grids.value.find(g => g.id !== editingGridId.value && g.weekStartDate === form.weekStartDate) || null
)

const form = reactive({
  title: '',
  theme: '',
  sport: gridCategories.categories.value[0] || '',
  weekStartDate: '',
  maxStrikes: 5,
  sortAscending: false,
  ranked: true,
  excludedFromGridBattle: false,
  canExpire: false,
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
  toast.show(_t('Emnet er oppdatert.'))
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
    error.value = _t('Kunne ikke laste grids.')
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
    toast.show(_t('Importerte {0} emne(r) fra "{1}". Dette gridet får nå automatisk nye medlemmer som legges til i det utvalget senere.', [added, pool.name]))
    selectedPoolId.value = null
  } catch (e) {
    error.value = _t('Kunne ikke importere utvalget.')
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
    toast.show(_t('Importerte {0} emne(r) fra "{1}" ({2} var allerede i dette gridet).', [added, form.sport, all.length - added]))
  } catch (e) {
    error.value = _t('Kunne ikke importere emner for den kategorien.')
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
  form.canExpire = false
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
    form.canExpire = detail.canExpire || false
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
    error.value = _t('Kunne ikke laste gridet.')
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
  form.title = form.title + _t(' (oppdatert)')
  form.excludedFromGridBattle = false
  toast.show(_t('Du redigerer nå en ny duplikat - det opprinnelige gridet er urørt. Husk å lagre denne kopien.'))
}

async function saveGrid() {
  error.value = ''
  if (!form.title.trim() || !form.weekStartDate) {
    error.value = _t('Tittel og startdato for uken må fylles ut.')
    return
  }
  const entries = candidates.value.filter(c => c.correct)
  if (!entries.length) {
    error.value = _t('Marker minst én kandidat som riktig svar.')
    return
  }
  if (form.ranked && entries.some(c => c.hintValue === null || c.hintValue === '')) {
    error.value = _t('Hvert riktige svar trenger en hintverdi (etiketten er valgfri).')
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
    canExpire: form.canExpire,
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
    toast.show(_t('Gridet er lagret.'))
    view.value = 'list'
    loadGrids()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke lagre gridet.')
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
    toast.show(_t('Gridet er slettet.'))
    loadGrids()
  } catch (e) {
    error.value = _t('Kunne ikke slette gridet.')
  }
}
</script>
