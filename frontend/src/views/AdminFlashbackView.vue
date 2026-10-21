<template>
  <div>
    <!-- List view -->
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
        <div>
          <h1>Flashback <span v-if="!loading && years.length" class="header-count">{{ years.length }}</span></h1>
          <p class="page-subtitle">
            {{ $t('Lag og administrer Flashback-år - et hemmelig år, pluss opptil 5 hint om det, ordnet fra vanskeligst til lettest.') }}
          </p>
        </div>
        <button class="btn btn-primary" @click="openCreate">{{ $t('+ Lag år') }}</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

      <div v-else-if="!years.length" class="empty-state friendly">
        {{ $t('Ingen Flashback-år ennå. Lag ditt første her.') }}
      </div>

      <template v-else>
      <BoardListToolbar
        v-model:search="searchTerm"
        v-model:sort-key="sortKey"
        v-model:sort-dir="sortDir"
        :sorts="yearSorts"
        :total-count="years.length"
        :filtered-count="filteredYears.length"
        :placeholder="$t('Søk etter år på tittel eller årstall…')"
      />

      <div v-if="!filteredYears.length" class="empty-state">{{ $t('Ingen år samsvarer med søket ditt.') }}</div>

      <div v-else class="saved-quiz-list">
        <div v-for="y in pagedYears" :key="y.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              {{ y.title }}
              <span v-if="y.excludedFromFlashback" class="tag" style="background:rgb(var(--coral-rgb) / 0.15); color:var(--coral); margin-left:6px;">{{ $t('Ikke i Flashback') }}</span>
              <span v-if="y.canExpire" class="tag" style="background:rgb(var(--fg-rgb) / 0.06); color:var(--text-dim); margin-left:6px;">{{ $t('Kan utløpe') }}</span>
            </div>
            <div class="saved-quiz-meta">
              {{ y.year }} · {{ $t('{0} hint¦{0} hint', [y.hintCount]) }}
            </div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(y.id)">{{ $t('Rediger') }}</button>
            <button class="btn btn-secondary btn-sm" @click="duplicateYear(y.id)">{{ $t('⧉ Dupliser') }}</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(y)">{{ $t('Slett') }}</button>
          </div>
        </div>
      </div>

      <Pagination v-model:page="yearPage" :page-size="10" :total-items="filteredYears.length" />
      </template>
    </template>

    <!-- Builder view -->
    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; flex-wrap:wrap; gap:10px;">
        <h1 style="margin:0;">{{ editingYearId ? $t('Rediger år') : $t('Lag år') }}</h1>
        <button class="btn btn-secondary btn-sm" @click="view = 'list'">{{ $t('← Tilbake til listen') }}</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>{{ $t('Tittel') }} <span class="picker-hint">{{ $t('intern merkelapp, vises ikke for spillere') }}</span><input type="text" v-model="form.title" :placeholder="$t('f.eks. Månelandingen')" /></label>
      </div>

      <div class="field" style="max-width:200px;">
        <label>{{ $t('År') }} <span class="picker-hint">{{ $t('det spillerne gjetter') }}</span><input type="number" v-model.number="form.year" placeholder="1969" /></label>
      </div>

      <div class="field">
        <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:600;">
          <input type="checkbox" v-model="form.excludedFromFlashback" style="width:auto;" />
          {{ $t('Utelat fra Flashback') }}
        </label>
        <p class="page-subtitle" style="margin-top:4px;">
          {{ $t('Bruk dette når et års hint er utdaterte eller feil - det slutter å tilbys i Flashbacks tilfeldige valg uten å bli slettet.') }}
        </p>
      </div>

      <div class="field option-card">
        <input type="checkbox" id="canExpire" v-model="form.canExpire" style="width:auto; margin-top:3px;" />
        <label for="canExpire" style="margin:0; text-transform:none; font-weight:400;">
          {{ $t('Kan utløpe') }}
          <div style="color:var(--text-dim); font-size:0.8rem; font-weight:400; margin-top:2px;">
            {{ $t('f.eks. "nåværende rekordholder" - merk dette slik at det vises på Innsikt-siden «Kan utløpe» for jevnlig gjennomgang. La stå uten hake for faste fakta.') }}
          </div>
        </label>
      </div>

      <div class="field">
        <label>
          {{ $t('Hint') }} <span class="picker-hint">{{ $t('opptil 5, ordnet fra vanskeligst til lettest - endre rekkefølge med pilene') }}</span>
        </label>

        <div v-if="!form.hints.length" class="empty-state" style="padding:20px;">
          {{ $t('Ingen hint lagt til ennå - legg til det første (vanskeligste) under.') }}
        </div>

        <div v-else style="display:flex; flex-direction:column; gap:8px;">
          <div v-for="(hint, i) in form.hints" :key="i" class="candidate-row">
            <div style="display:flex; align-items:center; gap:8px; flex:1; min-width:0;">
              <span class="tag" style="background:rgb(var(--fg-rgb) / 0.06); color:var(--text-dim); flex-shrink:0;">{{ i + 1 }}</span>
              <input type="text" v-model="form.hints[i]" :placeholder="`Hint ${i + 1}`" style="flex:1;" />
            </div>
            <div style="display:flex; gap:4px;">
              <button class="btn btn-secondary btn-sm icon-btn" :disabled="i === 0" @click="moveHint(i, -1)" :aria-label="$t('Flytt opp')">↑</button>
              <button class="btn btn-secondary btn-sm icon-btn" :disabled="i === form.hints.length - 1" @click="moveHint(i, 1)" :aria-label="$t('Flytt ned')">↓</button>
              <button class="btn btn-danger btn-sm" @click="form.hints.splice(i, 1)">✕</button>
            </div>
          </div>
        </div>

        <button
          class="btn btn-secondary btn-sm"
          style="margin-top:10px;"
          :disabled="form.hints.length >= 5"
          @click="form.hints.push('')"
        >{{ $t('+ Legg til hint') }}</button>
      </div>

      <TranslationEditor v-if="editingYearId" type="FLASHBACK" :entity-id="editingYearId" />

      <div style="display:flex; gap:10px; flex-wrap:wrap;">
        <button v-if="editingYearId" class="btn btn-secondary" @click="duplicateAsNewVersion">
          {{ $t('⧉ Dupliser som ny versjon') }}
        </button>
        <button class="btn btn-primary" :disabled="saving" @click="saveYear">
          {{ saving ? $t('Lagrer…') : $t('Lagre år') }}
        </button>
      </div>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      :title="$t('Slette dette året?')"
      :message="$t('«{0}» fjernes for godt.', [pendingDelete.title])"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import TranslationEditor from '../components/TranslationEditor.vue'
import { t as _t } from '../i18n'
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import ConfirmModal from '../components/ConfirmModal.vue'
import Pagination from '../components/Pagination.vue'
import BoardListToolbar from '../components/BoardListToolbar.vue'
import { useBoardList } from '../composables/useBoardList'

const view = ref('list')
const years = ref([])
const loading = ref(true)

const {
  searchTerm, sortKey, sortDir, page: yearPage,
  filtered: filteredYears, paged: pagedYears, sorts: yearSorts
} = useBoardList(years, {
  pageSize: 10,
  searchFields: [y => y.title, y => String(y.year)],
  sorts: [
    { key: 'title', label: _t('Tittel'), accessor: y => y.title },
    { key: 'year', label: _t('År'), accessor: y => y.year, dir: 'desc' },
    { key: 'hints', label: _t('Hint'), accessor: y => y.hintCount, dir: 'desc' }
  ]
})
const error = ref('')
const saving = ref(false)
const pendingDelete = ref(null)
const editingYearId = ref(null)

const form = reactive({
  title: '',
  year: null,
  excludedFromFlashback: false,
  canExpire: false,
  hints: ['']
})

onMounted(loadYears)

async function loadYears() {
  loading.value = true
  error.value = ''
  try {
    years.value = await api.adminListFlashbackYears()
  } catch (e) {
    error.value = _t('Kunne ikke laste Flashback-år.')
  } finally {
    loading.value = false
  }
}

function moveHint(idx, direction) {
  const target = idx + direction
  if (target < 0 || target >= form.hints.length) return
  const list = form.hints
  const [moved] = list.splice(idx, 1)
  list.splice(target, 0, moved)
}

function resetForm() {
  form.title = ''
  form.year = null
  form.excludedFromFlashback = false
  form.canExpire = false
  form.hints = ['']
}

function openCreate() {
  resetForm()
  editingYearId.value = null
  view.value = 'builder'
}

async function openEdit(id) {
  error.value = ''
  try {
    const detail = await api.adminGetFlashbackYear(id)
    form.title = detail.title
    form.year = detail.year
    form.excludedFromFlashback = detail.excludedFromFlashback
    form.canExpire = detail.canExpire
    form.hints = detail.hints.length ? [...detail.hints] : ['']

    editingYearId.value = id
    view.value = 'builder'
  } catch (e) {
    error.value = _t('Kunne ikke laste året.')
  }
}

// One-click duplicate straight from the list - loads the year into the
// builder exactly like Edit would, then immediately detaches it into a new
// unsaved copy via duplicateAsNewVersion, same pattern as Bullseye's list.
async function duplicateYear(id) {
  await openEdit(id)
  if (editingYearId.value === id) duplicateAsNewVersion()
}

// Keeps every current form field exactly as-is - only clearing the editing
// id, so the next save creates a brand new year via POST instead of
// overwriting the original via PUT.
function duplicateAsNewVersion() {
  editingYearId.value = null
  form.title = form.title + _t(' (oppdatert)')
  form.excludedFromFlashback = false
  toast.show(_t('Du redigerer nå en ny duplikat - det opprinnelige året er urørt. Husk å lagre denne kopien.'))
}

async function saveYear() {
  error.value = ''
  if (!form.title.trim() || form.year === null) {
    error.value = _t('Tittel og år må fylles ut.')
    return
  }
  const hints = form.hints.map(h => h.trim()).filter(Boolean)
  if (!hints.length) {
    error.value = _t('Legg til minst 1 hint.')
    return
  }
  if (hints.length > 5) {
    error.value = _t('Et år kan ha høyst 5 hint.')
    return
  }

  const payload = {
    title: form.title,
    year: form.year,
    excludedFromFlashback: form.excludedFromFlashback,
    canExpire: form.canExpire,
    hints
  }

  saving.value = true
  try {
    if (editingYearId.value) {
      await api.adminUpdateFlashbackYear(editingYearId.value, payload)
    } else {
      await api.adminCreateFlashbackYear(payload)
    }
    toast.show(_t('Året er lagret.'))
    view.value = 'list'
    loadYears()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke lagre dette året.')
  } finally {
    saving.value = false
  }
}

function requestDelete(y) {
  pendingDelete.value = y
}

async function doDelete() {
  const y = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.adminDeleteFlashbackYear(y.id)
    toast.show(_t('Året er slettet.'))
    loadYears()
  } catch (e) {
    error.value = _t('Kunne ikke slette året.')
  }
}
</script>
