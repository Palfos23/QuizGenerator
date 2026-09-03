<template>
  <div>
    <!-- List view -->
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>Penalty Shootouts <span v-if="!loading && shootouts.length" class="header-count">{{ shootouts.length }}</span></h1>
          <p class="page-subtitle">
            Create and manage "guess the penalty taker" boards for a specific shootout.
          </p>
        </div>
        <button class="btn btn-primary" @click="openCreate">+ Create shootout</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
      <div v-else-if="!shootouts.length" class="empty-state friendly">
        No penalty shootouts yet. Add some football subjects on the Subjects page, then create your first shootout here.
      </div>

      <template v-else>
      <BoardListToolbar
        v-model:search="searchTerm"
        v-model:sort-key="sortKey"
        v-model:sort-dir="sortDir"
        :sorts="shootoutSorts"
        :total-count="shootouts.length"
        :filtered-count="filteredShootouts.length"
        placeholder="Search shootouts by title or team…"
      />

      <div v-if="!filteredShootouts.length" class="empty-state">No shootouts match your search.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="s in pagedShootouts" :key="s.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ s.title }}</div>
            <div class="saved-quiz-meta">
              {{ s.teamName }} vs {{ s.opponentName }}
              <template v-if="s.teamPensScored != null && s.opponentPensScored != null"> ({{ s.teamPensScored }}-{{ s.opponentPensScored }} pens)</template>
              · {{ s.kickCount }} kick{{ s.kickCount === 1 ? '' : 's' }} · {{ s.maxStrikes }} {{ s.maxStrikes === 1 ? 'life' : 'lives' }}
            </div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(s.id)">Edit</button>
            <button class="btn btn-secondary btn-sm" @click="duplicateShootout(s.id)">⧉ Duplicate</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(s)">Delete</button>
          </div>
        </div>
      </div>

      <Pagination v-model:page="shootoutPage" :page-size="10" :total-items="filteredShootouts.length" />
      </template>
    </template>

    <!-- Builder view -->
    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <h1 style="margin:0;">{{ editingShootoutId ? 'Edit penalty shootout' : 'Create penalty shootout' }}</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">← Back to list</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Title</label>
        <input type="text" v-model="form.title" placeholder="e.g. 2006 World Cup Final" />
      </div>

      <div class="field">
        <label>Competition / context <span class="picker-hint">optional</span></label>
        <textarea v-model="form.competition" placeholder="e.g. Italy win 5-3 on penalties after a 1-1 draw."></textarea>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>Match date <span class="picker-hint">optional</span></label>
          <input type="date" v-model="form.matchDate" />
        </div>
        <div style="flex:1; min-width:140px;">
          <label>Max strikes</label>
          <input type="number" min="1" max="20" v-model.number="form.maxStrikes" />
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:220px;">
          <label>Team</label>
          <input type="text" v-model="form.teamName" placeholder="e.g. Italy" />
        </div>
        <div style="flex:1; min-width:220px;">
          <label>Team crest URL <span class="picker-hint">optional</span></label>
          <input type="text" v-model="form.teamCrestUrl" placeholder="https://…" />
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:220px;">
          <label>Opponent</label>
          <input type="text" v-model="form.opponentName" placeholder="e.g. France" />
        </div>
        <div style="flex:1; min-width:220px;">
          <label>Opponent crest URL <span class="picker-hint">optional</span></label>
          <input type="text" v-model="form.opponentCrestUrl" placeholder="https://…" />
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:140px;">
          <label>{{ form.teamName || 'Team' }} pens scored <span class="picker-hint">optional, e.g. "5"</span></label>
          <input type="number" min="0" v-model.number="form.teamPensScored" />
        </div>
        <div style="flex:1; min-width:140px;">
          <label>{{ form.opponentName || 'Opponent' }} pens scored <span class="picker-hint">optional, e.g. "3"</span></label>
          <input type="number" min="0" v-model.number="form.opponentPensScored" />
        </div>
      </div>

      <div class="field">
        <label>Kicks <span class="picker-hint">in real shootout order - search adds to the end, reorder with the arrows</span></label>

        <input type="text" v-model="kickSearchTerm" placeholder="Search for who took a kick…" style="width:100%; margin-bottom:10px;" />

        <div v-if="kickSearchResults.length" class="guess-results" style="margin-bottom:10px;">
          <button v-for="a in kickSearchResults" :key="a.id" class="guess-result-row" @click="addKick(a)">
            {{ a.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ a.team }}</span>
          </button>
        </div>

        <div v-if="!kicks.length" class="empty-state" style="padding:20px;">
          No kicks added yet - search above for the first kicker.
        </div>

        <div v-else>
          <div v-for="(k, i) in kicks" :key="i" class="candidate-row">
            <div style="display:flex; align-items:center; gap:8px; font-weight:600;">
              <span class="tag" style="background:rgba(255,255,255,0.06); color:var(--text-dim);">{{ i + 1 }}</span>
              {{ k.name }} <span style="color:var(--text-dim); font-weight:400; font-size:0.85rem;">{{ k.team }}</span>
            </div>
            <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
              <div class="language-row" style="gap:6px;">
                <button type="button" class="language-btn" :class="{ active: k.forTeam }" @click="k.forTeam = true">{{ form.teamName || 'Team' }}</button>
                <button type="button" class="language-btn" :class="{ active: !k.forTeam }" @click="k.forTeam = false">{{ form.opponentName || 'Opponent' }}</button>
              </div>
              <label style="display:flex; align-items:center; gap:6px; text-transform:none; font-weight:400; font-size:0.82rem; color:var(--text-dim); margin:0;">
                <input type="checkbox" v-model="k.scored" style="width:auto;" />
                Scored
              </label>
            </div>
            <div style="display:flex; gap:4px;">
              <button class="btn btn-secondary btn-sm icon-btn" :disabled="i === 0" @click="moveKick(i, -1)" aria-label="Move up">↑</button>
              <button class="btn btn-secondary btn-sm icon-btn" :disabled="i === kicks.length - 1" @click="moveKick(i, 1)" aria-label="Move down">↓</button>
              <button class="btn btn-danger btn-sm" @click="kicks.splice(i, 1)">✕</button>
            </div>
          </div>
        </div>
      </div>

      <div style="display:flex; gap:10px; flex-wrap:wrap;">
        <button class="btn btn-secondary" :disabled="!kicks.length" @click="showPreview = true">
          Preview ({{ kicks.length }} kick{{ kicks.length === 1 ? '' : 's' }})
        </button>
        <button v-if="editingShootoutId" class="btn btn-secondary" @click="duplicateAsNewVersion">
          ⧉ Duplicate as new version
        </button>
        <button class="btn btn-primary" :disabled="saving" @click="saveShootout">
          {{ saving ? 'Saving…' : 'Save shootout' }}
        </button>
      </div>
    </template>

    <div v-if="showPreview" class="modal-backdrop" @click.self="showPreview = false">
      <div class="modal" style="max-width:640px;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
          <h2 style="margin:0;">{{ form.title || 'Untitled shootout' }}</h2>
          <button class="btn btn-secondary btn-sm" @click="showPreview = false">Close</button>
        </div>
        <p class="page-subtitle" style="margin-top:0;">Everything shown revealed, for a quick check that kicks and order look right.</p>

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
      title="Delete this shootout?"
      :message="`'${pendingDelete.title}' will be permanently removed.`"
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
    { key: 'date', label: 'Match date', accessor: s => s.matchDate, dir: 'desc' },
    { key: 'title', label: 'Title', accessor: s => s.title },
    { key: 'team', label: 'Team', accessor: s => s.teamName }
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
    error.value = 'Could not load penalty shootouts.'
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
    error.value = 'Could not load that shootout.'
  }
}

// One-click duplicate straight from the list, same pattern as AdminLineupsView.
async function duplicateShootout(id) {
  await openEdit(id)
  if (editingShootoutId.value === id) duplicateAsNewVersion()
}

function duplicateAsNewVersion() {
  editingShootoutId.value = null
  form.title = form.title + ' (updated)'
  toast.show('Now editing a new duplicate - the original shootout is untouched. Remember to save this copy.')
}

async function saveShootout() {
  error.value = ''
  if (!form.title.trim()) {
    error.value = 'Title is required.'
    return
  }
  if (!form.teamName.trim() || !form.opponentName.trim()) {
    error.value = 'Team and opponent are both required.'
    return
  }
  if (!kicks.value.length) {
    error.value = 'Add at least one kick.'
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
    toast.show('Penalty shootout saved.')
    view.value = 'list'
    loadShootouts()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save this shootout.'
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
    toast.show('Shootout deleted.')
    loadShootouts()
  } catch (e) {
    error.value = 'Could not delete that shootout.'
  }
}
</script>
