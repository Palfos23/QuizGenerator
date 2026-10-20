<template>
  <div>
    <!-- List view -->
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>{{ $t('Straffekonkurranser') }} <span v-if="!loading && shootouts.length" class="header-count">{{ shootouts.length }}</span></h1>
          <p class="page-subtitle">
            {{ $t('Lag og administrer «gjett straffeskytteren»-brett for en bestemt straffekonkurranse.') }}
          </p>
        </div>
        <button class="btn btn-primary" @click="openCreate">{{ $t('+ Lag straffekonkurranse') }}</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>
      <div v-else-if="!shootouts.length" class="empty-state friendly">
        {{ $t('Ingen straffekonkurranser ennå. Legg til noen fotballemner på siden Emner, og lag din første straffekonkurranse her.') }}
      </div>

      <template v-else>
      <BoardListToolbar
        v-model:search="searchTerm"
        v-model:sort-key="sortKey"
        v-model:sort-dir="sortDir"
        :sorts="shootoutSorts"
        :total-count="shootouts.length"
        :filtered-count="filteredShootouts.length"
        :placeholder="$t('Søk etter straffekonkurranser på tittel eller lag…')"
      />

      <div v-if="!filteredShootouts.length" class="empty-state">{{ $t('Ingen straffekonkurranser samsvarer med søket ditt.') }}</div>

      <div v-else class="saved-quiz-list">
        <div v-for="s in pagedShootouts" :key="s.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ s.title }}</div>
            <div class="saved-quiz-meta">
              {{ s.teamName }} {{ $t('mot') }} {{ s.opponentName }}
              <template v-if="s.teamPensScored != null && s.opponentPensScored != null"> ({{ s.teamPensScored }}-{{ s.opponentPensScored }} {{ $t('straffer)') }}</template>
              · {{ $t('{0} spark¦{0} spark', [s.kickCount]) }} · {{ $t('{0} liv¦{0} liv', [s.maxStrikes]) }}
            </div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(s.id)">{{ $t('Rediger') }}</button>
            <button class="btn btn-secondary btn-sm" @click="duplicateShootout(s.id)">{{ $t('⧉ Dupliser') }}</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(s)">{{ $t('Slett') }}</button>
          </div>
        </div>
      </div>

      <Pagination v-model:page="shootoutPage" :page-size="10" :total-items="filteredShootouts.length" />
      </template>
    </template>

    <!-- Builder view -->
    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <h1 style="margin:0;">{{ editingShootoutId ? $t('Rediger straffekonkurranse') : $t('Lag straffekonkurranse') }}</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">{{ $t('← Tilbake til listen') }}</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>{{ $t('Tittel') }}<input type="text" v-model="form.title" :placeholder="$t('f.eks. VM-finalen 2006')" /></label>
      </div>

      <div class="field">
        <label>{{ $t('Turnering / sammenheng') }} <span class="picker-hint">{{ $t('valgfritt') }}</span><textarea v-model="form.competition" :placeholder="$t('f.eks. Italia vinner 5-3 på straffer etter 1-1.')"></textarea></label>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>{{ $t('Kampdato') }} <span class="picker-hint">{{ $t('valgfritt') }}</span><input type="date" v-model="form.matchDate" /></label>
        </div>
        <div style="flex:1; min-width:140px;">
          <label>{{ $t('Maks liv') }}<input type="number" min="1" max="20" v-model.number="form.maxStrikes" /></label>
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:220px;">
          <label>{{ $t('Lag') }}<input type="text" v-model="form.teamName" :placeholder="$t('f.eks. Italia')" /></label>
        </div>
        <div style="flex:1; min-width:220px;">
          <label>{{ $t('URL til lagets emblem') }} <span class="picker-hint">{{ $t('valgfritt') }}</span><input type="text" v-model="form.teamCrestUrl" placeholder="https://…" /></label>
          <div class="photo-upload-row"><PhotoUploadButton :label="$t('Last opp et emblem')" @uploaded="url => form.teamCrestUrl = url" /></div>
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:220px;">
          <label>{{ $t('Motstander') }}<input type="text" v-model="form.opponentName" :placeholder="$t('f.eks. Frankrike')" /></label>
        </div>
        <div style="flex:1; min-width:220px;">
          <label>{{ $t('URL til motstanderens emblem') }} <span class="picker-hint">{{ $t('valgfritt') }}</span><input type="text" v-model="form.opponentCrestUrl" placeholder="https://…" /></label>
          <div class="photo-upload-row"><PhotoUploadButton :label="$t('Last opp et emblem')" @uploaded="url => form.opponentCrestUrl = url" /></div>
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:140px;">
          <label>{{ form.teamName || $t('Lag') }} {{ $t('scorede straffer') }} <span class="picker-hint">{{ $t('valgfritt, f.eks. "5"') }}</span><input type="number" min="0" v-model.number="form.teamPensScored" /></label>
        </div>
        <div style="flex:1; min-width:140px;">
          <label>{{ form.opponentName || $t('Motstander') }} {{ $t('scorede straffer') }} <span class="picker-hint">{{ $t('valgfritt, f.eks. "3"') }}</span><input type="number" min="0" v-model.number="form.opponentPensScored" /></label>
        </div>
      </div>

      <div class="field">
        <label>{{ $t('Spark') }} <span class="picker-hint">{{ $t('i riktig rekkefølge - søk legger til på slutten, endre rekkefølge med pilene') }}</span></label>

        <input type="text" v-model="kickSearchTerm" :placeholder="$t('Søk etter hvem som tok et spark…')" style="width:100%; margin-bottom:10px;" />

        <div v-if="kickSearchResults.length" class="guess-results" style="margin-bottom:10px;">
          <button v-for="a in kickSearchResults" :key="a.id" class="guess-result-row" @click="addKick(a)">
            {{ a.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ a.team }}</span>
          </button>
        </div>

        <div v-if="!kicks.length" class="empty-state" style="padding:20px;">
          {{ $t('Ingen spark lagt til ennå - søk over etter den første skytteren.') }}
        </div>

        <div v-else>
          <div v-for="(k, i) in kicks" :key="i" class="candidate-row">
            <div style="display:flex; align-items:center; gap:8px; font-weight:600;">
              <span class="tag" style="background:rgba(27, 26, 23, 0.06); color:var(--text-dim);">{{ i + 1 }}</span>
              {{ k.name }} <span style="color:var(--text-dim); font-weight:400; font-size:0.85rem;">{{ k.team }}</span>
            </div>
            <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
              <div class="language-row" style="gap:6px;">
                <button type="button" class="language-btn" :class="{ active: k.forTeam }" @click="k.forTeam = true">{{ form.teamName || $t('Lag') }}</button>
                <button type="button" class="language-btn" :class="{ active: !k.forTeam }" @click="k.forTeam = false">{{ form.opponentName || $t('Motstander') }}</button>
              </div>
              <label style="display:flex; align-items:center; gap:6px; text-transform:none; font-weight:400; font-size:0.82rem; color:var(--text-dim); margin:0;">
                <input type="checkbox" v-model="k.scored" style="width:auto;" />
                {{ $t('Scoret') }}
              </label>
            </div>
            <div style="display:flex; gap:4px;">
              <button class="btn btn-secondary btn-sm icon-btn" :disabled="i === 0" @click="moveKick(i, -1)" :aria-label="$t('Flytt opp')">↑</button>
              <button class="btn btn-secondary btn-sm icon-btn" :disabled="i === kicks.length - 1" @click="moveKick(i, 1)" :aria-label="$t('Flytt ned')">↓</button>
              <button class="btn btn-danger btn-sm" @click="kicks.splice(i, 1)">✕</button>
            </div>
          </div>
        </div>
      </div>

      <TranslationEditor v-if="editingShootoutId" type="SHOOTOUT" :entity-id="editingShootoutId" />

      <div style="display:flex; gap:10px; flex-wrap:wrap;">
        <button class="btn btn-secondary" :disabled="!kicks.length" @click="showPreview = true">
          {{ $t('Forhåndsvisning ({0} spark)¦Forhåndsvisning ({0} spark)', [kicks.length]) }}
        </button>
        <button v-if="editingShootoutId" class="btn btn-secondary" @click="duplicateAsNewVersion">
          {{ $t('⧉ Dupliser som ny versjon') }}
        </button>
        <button class="btn btn-primary" :disabled="saving" @click="saveShootout">
          {{ saving ? $t('Lagrer…') : $t('Lagre straffekonkurranse') }}
        </button>
      </div>
    </template>

    <div v-if="showPreview" class="modal-backdrop" @click.self="showPreview = false">
      <div class="modal" style="max-width:640px;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
          <h2 style="margin:0;">{{ form.title || $t('Straffekonkurranse uten tittel') }}</h2>
          <button class="btn btn-secondary btn-sm" @click="showPreview = false">{{ $t('Lukk§AdminPenaltyShootoutsViewT') }}</button>
        </div>
        <p class="page-subtitle" style="margin-top:0;">{{ $t('Alt vist avslørt, for en rask sjekk av at spark og rekkefølge ser riktige ut.') }}</p>

        <PenaltyShootoutBoard
          :team-name="form.teamName"
          :team-crest-url="form.teamCrestUrl"
          :opponent-name="form.opponentName"
          :opponent-crest-url="form.opponentCrestUrl"
          :kicks="previewKicks"
        />
      </div>
    </div>

    <ConfirmModal
      v-if="pendingDelete"
      :title="$t('Slette denne straffekonkurransen?')"
      :message="$t('«{0}» fjernes for godt.', [pendingDelete.title])"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import TranslationEditor from '../components/TranslationEditor.vue'
import { t as _t } from '../i18n'
import PhotoUploadButton from '../components/PhotoUploadButton.vue'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import ConfirmModal from '../components/ConfirmModal.vue'
import Pagination from '../components/Pagination.vue'
import BoardListToolbar from '../components/BoardListToolbar.vue'
import { useBoardList } from '../composables/useBoardList'
import PenaltyShootoutBoard from '../components/PenaltyShootoutBoard.vue'

const FOOTBALL_CATEGORY = 'Football'

const view = ref('list')
const shootouts = ref([])
const loading = ref(true)

const {
  searchTerm, sortKey, sortDir, page: shootoutPage,
  filtered: filteredShootouts, paged: pagedShootouts, sorts: shootoutSorts
} = useBoardList(shootouts, {
  pageSize: 10,
  searchFields: [s => s.title, s => s.teamName, s => s.opponentName],
  sorts: [
    { key: 'date', label: _t('Kampdato'), accessor: s => s.matchDate, dir: 'desc' },
    { key: 'title', label: _t('Tittel'), accessor: s => s.title },
    { key: 'team', label: _t('Lag'), accessor: s => s.teamName }
  ]
})
const error = ref('')
const saving = ref(false)
const pendingDelete = ref(null)
const editingShootoutId = ref(null)
const showPreview = ref(false)

const form = reactive({
  title: '', competition: '', matchDate: '',
  teamName: '', teamCrestUrl: '', opponentName: '', opponentCrestUrl: '',
  teamPensScored: null, opponentPensScored: null, maxStrikes: 5
})
// Order in this array *is* the kick order (index + 1) - no separate candidate
// pool to build first and no manual kick-number field, unlike the first pass
// at this screen. Every shootout is football, always, so there's nothing to
// scope a candidate pool to in the first place - search below always draws
// from every football subject (see api.adminSearchAthletes), and the
// guessable pool at play time is that same full category, live (see
// PenaltyShootoutPlayService.searchCandidates) - not tied to what's picked
// here at all. Duplicates (the same athlete taking two kicks) are allowed,
// not deduped - real shootouts can cycle back to earlier takers once every
// outfield player's had a turn.
const kicks = ref([])
const kickSearchTerm = ref('')
const kickSearchResults = ref([])

const previewKicks = computed(() =>
  kicks.value.map((k, i) => ({
    id: i, kickOrder: i + 1, forTeam: k.forTeam, scored: k.scored,
    solved: true, athleteName: k.name, athletePhotoUrl: k.photoUrl
  }))
)

onMounted(loadShootouts)

async function loadShootouts() {
  loading.value = true
  error.value = ''
  try {
    shootouts.value = await api.adminListPenaltyShootouts()
  } catch (e) {
    error.value = _t('Kunne ikke laste straffekonkurranser.§AdminPenaltyShootoutsViewJ')
  } finally {
    loading.value = false
  }
}

let searchDebounce = null
watch(kickSearchTerm, (val) => {
  clearTimeout(searchDebounce)
  if (!val || val.trim().length < 2) {
    kickSearchResults.value = []
    return
  }
  searchDebounce = setTimeout(async () => {
    try {
      kickSearchResults.value = await api.adminSearchAthletes({ sport: FOOTBALL_CATEGORY, name: val })
    } catch (e) {
      // non-critical - the search box just stays empty
    }
  }, 250)
})

// Appends to the end - real shootout order, so the next kick added is
// whatever's about to happen next. Reorder afterward with the ↑/↓ buttons if
// something needs fixing, rather than typing kick numbers by hand.
function addKick(athlete) {
  kicks.value.push({
    athleteId: athlete.id, name: athlete.name, team: athlete.team, photoUrl: athlete.photoUrl,
    forTeam: true, scored: true
  })
  kickSearchTerm.value = ''
  kickSearchResults.value = []
}

function moveKick(idx, direction) {
  const target = idx + direction
  if (target < 0 || target >= kicks.value.length) return
  const list = kicks.value
  const [moved] = list.splice(idx, 1)
  list.splice(target, 0, moved)
}

function resetForm() {
  form.title = ''
  form.competition = ''
  form.matchDate = ''
  form.teamName = ''
  form.teamCrestUrl = ''
  form.opponentName = ''
  form.opponentCrestUrl = ''
  form.teamPensScored = null
  form.opponentPensScored = null
  form.maxStrikes = 5
  kicks.value = []
  kickSearchTerm.value = ''
  kickSearchResults.value = []
}

function openCreate() {
  resetForm()
  editingShootoutId.value = null
  view.value = 'builder'
}

async function openEdit(id) {
  error.value = ''
  try {
    const detail = await api.adminGetPenaltyShootout(id)
    form.title = detail.title
    form.competition = detail.competition || ''
    form.matchDate = detail.matchDate || ''
    form.teamName = detail.teamName
    form.teamCrestUrl = detail.teamCrestUrl || ''
    form.opponentName = detail.opponentName
    form.opponentCrestUrl = detail.opponentCrestUrl || ''
    form.teamPensScored = detail.teamPensScored
    form.opponentPensScored = detail.opponentPensScored
    form.maxStrikes = detail.maxStrikes

    // detail.kicks already comes back sorted by kickOrder (see
    // PenaltyShootoutAdminService.toDetailDto) - that array order is what
    // this screen treats as the kick order, so no re-sort needed here.
    kicks.value = detail.kicks.map(k => ({
      athleteId: k.athlete.id, name: k.athlete.name, team: k.athlete.team, photoUrl: k.athlete.photoUrl,
      forTeam: k.forTeam, scored: k.scored
    }))
    kickSearchTerm.value = ''
    kickSearchResults.value = []

    editingShootoutId.value = id
    view.value = 'builder'
  } catch (e) {
    error.value = _t('Kunne ikke laste straffekonkurransen.')
  }
}

// One-click duplicate straight from the list, same pattern as AdminLineupsView.
async function duplicateShootout(id) {
  await openEdit(id)
  if (editingShootoutId.value === id) duplicateAsNewVersion()
}

function duplicateAsNewVersion() {
  editingShootoutId.value = null
  form.title = form.title + _t(' (oppdatert)')
  toast.show(_t('Du redigerer nå en ny duplikat - den opprinnelige straffekonkurransen er urørt. Husk å lagre denne kopien.'))
}

async function saveShootout() {
  error.value = ''
  if (!form.title.trim()) {
    error.value = _t('Tittel må fylles ut.')
    return
  }
  if (!form.teamName.trim() || !form.opponentName.trim()) {
    error.value = _t('Lag og motstander må fylles ut.')
    return
  }
  if (!kicks.value.length) {
    error.value = _t('Legg til minst ett spark.')
    return
  }

  const payload = {
    title: form.title,
    competition: form.competition,
    matchDate: form.matchDate || null,
    teamName: form.teamName,
    teamCrestUrl: form.teamCrestUrl || null,
    opponentName: form.opponentName,
    opponentCrestUrl: form.opponentCrestUrl || null,
    teamPensScored: form.teamPensScored,
    opponentPensScored: form.opponentPensScored,
    maxStrikes: form.maxStrikes,
    // Array position is the kick order (1-based) - see the `kicks` ref's own
    // comment for why there's no manual kick-number field to read instead.
    kicks: kicks.value.map((k, i) => ({
      athleteId: k.athleteId, kickOrder: i + 1, forTeam: k.forTeam, scored: k.scored
    }))
  }

  saving.value = true
  try {
    if (editingShootoutId.value) {
      await api.adminUpdatePenaltyShootout(editingShootoutId.value, payload)
    } else {
      await api.adminCreatePenaltyShootout(payload)
    }
    toast.show(_t('Straffekonkurransen er lagret.'))
    view.value = 'list'
    loadShootouts()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke lagre straffekonkurransen.')
  } finally {
    saving.value = false
  }
}

function requestDelete(s) {
  pendingDelete.value = s
}

async function doDelete() {
  const s = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.adminDeletePenaltyShootout(s.id)
    toast.show(_t('Straffekonkurransen er slettet.'))
    loadShootouts()
  } catch (e) {
    error.value = _t('Kunne ikke slette straffekonkurransen.')
  }
}
</script>
