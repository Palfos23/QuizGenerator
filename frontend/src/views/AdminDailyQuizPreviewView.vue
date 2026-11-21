<template>
  <div class="dq-page dq-page--wide pv">
    <header class="dq-hero">
      <span class="dq-eyebrow">{{ $t('Forhåndsvisning') }}</span>
      <h1 class="dq-title">{{ $t('Morgendagens quiz') }}</h1>
      <p v-if="preview" class="dq-sub">
        {{ $t('{0} - dette er quizen spillerne får i morgen. Se gjennom spørsmålene, bytt ut de som ikke passer, eller utelat en hel type for dagen.', [formatLong(preview.date)]) }}
      </p>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>
    <LoadingSkeleton v-if="loading" />

    <template v-else-if="preview">
      <!-- ruled-out types -->
      <section class="dq-card pv-card">
        <h2 class="dq-section-title">{{ $t('Utelatt i morgen') }}</h2>
        <p class="pv-hint">{{ $t('Spørsmål av en utelatt type trekkes ikke - de som allerede er med byttes ut med andre.') }}</p>
        <div class="pv-chips">
          <span v-for="e in preview.exclusions" :key="e.kind + e.value" class="pv-chip pv-chip--out">
            {{ e.kind === 'LABEL' ? $t('Merkelapp') : $t('Kategori§pv') }}: <b>{{ e.value }}</b>
            <button type="button" :aria-label="$t('Fjern')" :disabled="busy" @click="removeExclusion(e)">✕</button>
          </span>
          <span v-if="!preview.exclusions.length" class="pv-none">{{ $t('Ingenting er utelatt.') }}</span>
        </div>
        <form class="pv-add" @submit.prevent="addFromForm">
          <select v-model="newKind" :aria-label="$t('Type')">
            <option value="CATEGORY">{{ $t('Kategori§pv') }}</option>
            <option value="LABEL">{{ $t('Merkelapp') }}</option>
          </select>
          <select v-model="newValue" :aria-label="$t('Velg')">
            <option value="" disabled>{{ $t('Velg...') }}</option>
            <option v-for="v in optionsForKind" :key="v" :value="v">{{ v }}</option>
          </select>
          <button class="btn btn-secondary btn-sm" type="submit" :disabled="busy || !newValue">{{ $t('Utelat i morgen') }}</button>
        </form>
      </section>

      <!-- what the last few days looked like, per category in tomorrow's quiz -->
      <section v-if="preview.categoryHistory.length" class="dq-card pv-card">
        <h2 class="dq-section-title">{{ $t('Kategorier de siste dagene') }}</h2>
        <p class="pv-hint">{{ $t('Antall spørsmål per kategori. Bruk dette til å se om en type kommer for ofte.') }}</p>
        <div class="pv-table-wrap">
          <table class="pv-table">
            <thead>
              <tr>
                <th>{{ $t('Kategori§pv') }}</th>
                <th v-for="(d, i) in preview.historyDays" :key="d">{{ i === 0 ? $t('I morgen') : i === 1 ? $t('I dag') : shortDay(d) }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in preview.categoryHistory" :key="row.category">
                <td>
                  <div class="pv-catcell">
                    <span>{{ row.category }}</span>
                    <button class="dq-chip-btn" :disabled="busy || isExcluded('CATEGORY', row.category)" @click="addExclusion('CATEGORY', row.category)">{{ $t('Utelat') }}</button>
                  </div>
                </td>
                <td v-for="(n, i) in row.counts" :key="i" :class="{ 'pv-zero': !n, 'pv-hot': n && i > 0 && row.counts[0] }">{{ n || '·' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- the questions -->
      <section class="pv-list">
        <article v-for="q in preview.questions" :key="q.id" class="dq-qrow pv-q" :class="{ 'pv-q--out': q.excluded }">
          <span class="dq-q-num">{{ q.position }}</span>
          <div class="dq-qrow-body">
            <div class="dq-qrow-text">{{ q.text }}</div>
            <img v-if="q.photoUrl" :src="q.photoUrl" alt="" class="dq-photo dq-photo--small" @error="e => e.target.style.display = 'none'" />
            <div class="dq-qrow-answer"><span>{{ $t('Svar§AdminDailyQuizQuestionsModalT') }}</span><b>{{ q.answer }}</b></div>
            <div class="pv-meta">
              <span class="pv-tag">{{ q.category }}</span>
              <span v-for="l in q.labels" :key="l" class="pv-tag pv-tag--label">{{ l }}</span>
              <span v-if="q.slot === 'LOGO'" class="dq-pill dq-pill--gold">{{ $t('Logo-plassen') }}</span>
              <span v-if="q.slot === 'YEAR'" class="dq-pill dq-pill--year">{{ $t('År-plassen') }}</span>
              <span v-if="q.excluded" class="dq-pill dq-pill--bad">{{ $t('Utelatt type - fant ingen erstatning') }}</span>
              <span v-if="q.lastUsed" class="pv-used">{{ $t('Brukt sist {0}', [shortDay(q.lastUsed)]) }}</span>
            </div>
          </div>
          <div class="pv-actions">
            <button class="dq-chip-btn" :disabled="busy" @click="replace(q)">{{ $t('Bytt ut') }}</button>
            <button class="dq-chip-btn" :disabled="busy" @click="picking = q">{{ $t('Velg selv') }}</button>
            <button class="dq-chip-btn" :disabled="busy" @click="edit(q)">{{ $t('Rediger') }}</button>
            <button class="dq-chip-btn" :disabled="busy || isExcluded('CATEGORY', q.category)" @click="addExclusion('CATEGORY', q.category)">{{ $t('Ingen flere «{0}»', [q.category]) }}</button>
          </div>
        </article>
      </section>
    </template>

    <!-- pick a specific replacement -->
    <div v-if="picking" class="modal-backdrop" @click.self="picking = null">
      <div class="modal dq-modal dq-modal--wide" role="dialog" aria-modal="true">
        <h2 class="dq-modal-title">{{ $t('Velg et nytt spørsmål') }}</h2>
        <p class="dq-modal-sub">{{ $t('Erstatter spørsmål {0}: «{1}»', [picking.position, picking.text]) }}</p>
        <input v-model="term" class="dq-input" type="search" :placeholder="$t('Søk i spørsmål, svar eller kategori...')" @input="searchSoon" />
        <LoadingSkeleton v-if="searching" />
        <div v-else class="dq-qlist pv-candidates">
          <article v-for="c in candidates" :key="c.id" class="dq-qrow">
            <div class="dq-qrow-body">
              <div class="dq-qrow-text">{{ c.text }}</div>
              <div class="dq-qrow-answer"><span>{{ $t('Svar§AdminDailyQuizQuestionsModalT') }}</span><b>{{ c.answer }}</b></div>
              <div class="pv-meta">
                <span class="pv-tag">{{ c.category }}</span>
                <span v-for="l in c.labels" :key="l" class="pv-tag pv-tag--label">{{ l }}</span>
                <span v-if="c.lastUsed" class="pv-used">{{ $t('Brukt sist {0}', [shortDay(c.lastUsed)]) }}</span>
              </div>
            </div>
            <button class="btn btn-primary btn-sm" :disabled="busy" @click="choose(c)">{{ $t('Velg') }}</button>
          </article>
          <p v-if="!candidates.length" class="pv-none">{{ $t('Ingen treff.') }}</p>
        </div>
        <div class="dq-modal-actions">
          <button class="btn btn-secondary" @click="picking = null">{{ $t('Lukk§AdminDailyQuizQuestionsModalT') }}</button>
        </div>
      </div>
    </div>

    <QuestionFormModal v-if="editing" :question="editing" @close="editing = null" @saved="onEdited" />
  </div>
</template>

<script setup>
import LoadingSkeleton from '../components/LoadingSkeleton.vue'
import QuestionFormModal from '../components/QuestionFormModal.vue'
import { t as _t } from '../i18n'
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import { useEscapeKey } from '../composables/useEscapeKey'

// Tomorrow's daily quiz, ahead of time. Opening this creates it (players can't see it until its day); every
// change is saved at once on the server, so there is nothing to "save" here.
const preview = ref(null)
const loading = ref(true)
const busy = ref(false)
const error = ref('')

const newKind = ref('CATEGORY')
const newValue = ref('')
const optionsForKind = computed(() => (newKind.value === 'LABEL' ? preview.value?.allLabels : preview.value?.allCategories) || [])

const picking = ref(null)
const term = ref('')
const candidates = ref([])
const searching = ref(false)
const editing = ref(null)

useEscapeKey(() => { picking.value = null })

onMounted(async () => {
  try {
    preview.value = await api.adminDailyQuizPreview()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke hente morgendagens quiz.')
  } finally {
    loading.value = false
  }
})

// Runs a change and shows the quiz it returns.
async function change(action, okMessage) {
  busy.value = true
  error.value = ''
  try {
    preview.value = await action()
    if (okMessage) toast.show(okMessage)
  } catch (e) {
    toast.show(e.response?.data?.message || _t('Kunne ikke lagre endringen.'), 'error')
  } finally {
    busy.value = false
  }
}

const replace = q => change(() => api.adminDailyQuizReplace(q.id), _t('Spørsmålet er byttet ut.'))

function addExclusion(kind, value) {
  return change(() => api.adminDailyQuizAddExclusion(kind, value), _t('«{0}» er utelatt i morgen.', [value]))
}
const addFromForm = async () => {
  await addExclusion(newKind.value, newValue.value)
  newValue.value = ''
}
const removeExclusion = e => change(() => api.adminDailyQuizRemoveExclusion(e.kind, e.value))
const isExcluded = (kind, value) => !!preview.value?.exclusions.some(e => e.kind === kind && e.value.toLowerCase() === (value || '').trim().toLowerCase())

// ---- picking a replacement by hand ----
let searchTimer = null
async function loadCandidates() {
  if (!picking.value) return
  searching.value = true
  try {
    candidates.value = await api.adminDailyQuizCandidates(picking.value.id, term.value)
  } catch (e) {
    candidates.value = []
  } finally {
    searching.value = false
  }
}
function searchSoon() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadCandidates, 250)
}

watch(picking, q => { term.value = ''; candidates.value = []; if (q) loadCandidates() })

async function choose(c) {
  const q = picking.value
  await change(() => api.adminDailyQuizChoose(q.id, c.id), _t('Spørsmålet er byttet.'))
  picking.value = null
}

// ---- fixing a question in place ----
async function edit(q) {
  try {
    editing.value = await api.adminGetQuestion(q.id)
  } catch (e) {
    toast.show(_t('Kunne ikke åpne spørsmålet.'), 'error')
  }
}
async function onEdited() {
  editing.value = null
  try { preview.value = await api.adminDailyQuizPreview() } catch (e) { /* the old view stays */ }
}

function formatLong(iso) {
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { weekday: 'long', day: 'numeric', month: 'long' })
}
function shortDay(iso) {
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { day: 'numeric', month: 'short' })
}
</script>

<style scoped>
.pv-card { margin-bottom: 18px; }
.pv-hint { margin: 0 0 12px; color: var(--text-dim); font-size: 0.9rem; }
.pv-chips { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; align-items: center; }
.pv-none { color: var(--text-dim); font-size: 0.9rem; }
.pv-chip { display: inline-flex; align-items: center; gap: 8px; padding: 6px 8px 6px 12px; border-radius: 999px; background: rgb(var(--fg-rgb) / 0.08); font-size: 0.88rem; }
.pv-chip--out { background: color-mix(in srgb, var(--dq-bad, #c0392b) 18%, transparent); }
.pv-chip button { border: 0; background: transparent; color: inherit; cursor: pointer; font-size: 0.85rem; padding: 2px 6px; border-radius: 50%; }
.pv-chip button:hover { background: rgb(var(--fg-rgb) / 0.15); }
.pv-add { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.pv-add select { min-width: 0; max-width: 100%; }

.pv-table-wrap { overflow-x: hidden; }
.pv-table { width: 100%; border-collapse: collapse; font-size: 0.92rem; }
.pv-table th, .pv-table td { padding: 8px 6px; text-align: center; border-bottom: 1px solid rgb(var(--fg-rgb) / 0.1); }
.pv-table th:first-child, .pv-table td:first-child { text-align: left; }
.pv-table th { font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.08em; color: var(--text-dim); font-weight: 700; }
.pv-zero { color: var(--text-dim); }
.pv-hot { font-weight: 800; color: var(--accent, inherit); }
.pv-catcell { display: flex; flex-wrap: wrap; gap: 4px 10px; align-items: center; }
.pv-catcell .dq-chip-btn { padding: 3px 10px; font-size: 0.78rem; }

.pv-list { display: grid; gap: 10px; margin-top: 6px; }
.pv-q { align-items: flex-start; }
.pv-q--out { outline: 2px solid color-mix(in srgb, var(--dq-bad, #c0392b) 60%, transparent); }
.pv-meta { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; align-items: center; }
.pv-tag { padding: 2px 9px; border-radius: 999px; font-size: 0.74rem; font-weight: 700; background: rgb(var(--fg-rgb) / 0.1); }
.pv-tag--label { font-weight: 500; background: transparent; box-shadow: inset 0 0 0 1px rgb(var(--fg-rgb) / 0.2); }
.pv-used { font-size: 0.78rem; color: var(--text-dim); }
.pv-actions { display: flex; flex-direction: column; gap: 6px; align-items: stretch; flex-shrink: 0; width: 190px; }
.pv-actions .dq-chip-btn { white-space: normal; text-align: center; }
.pv-candidates { margin-top: 12px; max-height: 50vh; overflow-y: auto; }

@media (max-width: 720px) {
  .pv-q { flex-wrap: wrap; }
  .pv-actions { flex-direction: row; flex-wrap: wrap; width: 100%; }
}
</style>
