<template>
  <div>
    <p class="page-subtitle">Alle får snurre hjulet én gang. La appen trekke hvem som står for tur, og skriv inn bonuspoengene hjulet ga.</p>
    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="loading" style="color:var(--text-dim);">Laster…</div>
    <template v-else>
      <!-- Whose turn? -->
      <div class="wh-stage">
        <div class="wh-left">Venter på å snurre: {{ data.candidates.length }}</div>
        <div class="wh-name" :class="{ 'is-rolling': rolling }">{{ shown || '?' }}</div>
        <button class="btn btn-primary" :disabled="rolling || !data.candidates.length" @click="pick">
          {{ !data.candidates.length ? 'Alle har snurret' : rolling ? 'Trekker…' : current ? 'Trekk på nytt' : 'Trekk hvem som snurrer' }}
        </button>
      </div>

      <!-- Record what it landed on -->
      <div v-if="current && !rolling" class="wh-form">
        <h2 style="margin:0 0 8px; font-size:1.2rem;">{{ current.name }} snurret…</h2>
        <div class="field">
          <label>Hva landet hjulet på?</label>
          <div class="wh-chips">
            <button v-for="n in WHEEL_POINTS" :key="n" type="button" class="wh-chip" :class="{ 'is-on': points === n }" :aria-pressed="points === n" @click="points = n">{{ label(n) }}</button>
          </div>
        </div>
        <div class="field">
          <label>Landet på <span class="picker-hint">valgfritt</span></label>
          <input type="text" v-model="note" maxlength="100" placeholder="F.eks. Dobbel opp" @keydown.enter="record" />
        </div>
        <button class="btn btn-primary" :disabled="saving || points === null || points === ''" @click="record">{{ saving ? 'Lagrer…' : 'Lagre spinnet' }}</button>
        <button class="btn btn-secondary" style="margin-left:8px;" @click="current = null; shown = ''">Avbryt</button>
      </div>

      <!-- Without the picker: record for someone chosen by hand -->
      <details v-if="data.candidates.length && !current" class="wh-manual">
        <summary>Legg inn for en bestemt spiller</summary>
        <div style="display:flex; gap:8px; flex-wrap:wrap; margin-top:8px;">
          <select v-model.number="manualId">
            <option :value="null">Velg spiller…</option>
            <option v-for="p in data.candidates" :key="p.id" :value="p.id">{{ p.name }}</option>
          </select>
          <button class="btn btn-secondary" :disabled="!manualId" @click="current = data.candidates.find(p => p.id === manualId); shown = current.name; points = null; note = ''">Velg</button>
        </div>
      </details>

      <h2 style="font-size:1.2rem; margin:26px 0 8px;">Har snurret ({{ data.spins.length }})</h2>
      <div v-if="!data.spins.length" class="empty-state">Ingen har snurret ennå.</div>
      <div v-else class="candidate-list">
        <div v-for="s in data.spins" :key="s.guestId" class="candidate-row wh-row">
          <span class="wh-row-name">{{ s.name }}</span>
          <select class="wh-row-pts" :value="edits[s.guestId]?.points ?? s.bonusPoints" aria-label="Bonuspoeng"
                  @change="edit(s).points = Number($event.target.value)">
            <option v-for="n in WHEEL_POINTS" :key="n" :value="n">{{ label(n) }}</option>
          </select>
          <input type="text" class="wh-row-note" maxlength="100" :value="edits[s.guestId]?.note ?? (s.note || '')" placeholder="Landet på"
                 @input="edit(s).note = $event.target.value" @keydown.enter="saveEdit(s)" />
          <button class="btn btn-secondary btn-sm" :disabled="!edited(s)" @click="saveEdit(s)">Lagre</button>
          <button class="btn btn-danger btn-sm" title="Fjerner spinnet - spilleren får tur igjen" @click="remove(s)">Fjern</button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import bdayApi from '../services/bdayApi'

// The host's wheel tab: the spinning itself happens on the real wheel - this keeps score. "Trekk" has the app pick a
// random guest who hasn't spun yet (with a little roll through the names), then the host saves the bonus points the
// wheel gave. Each guest spins once; removing a spin gives them the turn back.
// what the wheel can land on - the server accepts nothing else
const WHEEL_POINTS = [-3, -1, 1, 2, 3, 5]
const label = n => (n > 0 ? '+' + n : '−' + Math.abs(n))

const data = ref({ candidates: [], spins: [] })
const loading = ref(true)
const error = ref('')
const shown = ref('') // the name on the big display
const current = ref(null) // who is spinning now
const rolling = ref(false)
const points = ref(null)
const note = ref('')
const saving = ref(false)
const manualId = ref(null)
const edits = reactive({}) // guest id -> { points, note } being edited in the list
let timer = null

onMounted(load)
onBeforeUnmount(() => clearTimeout(timer))

async function load() {
  try {
    data.value = await bdayApi.adminWheel()
  } catch (e) {
    error.value = 'Kunne ikke laste hjulet.'
  } finally {
    loading.value = false
  }
}

async function pick() {
  error.value = ''
  let winner
  try {
    winner = await bdayApi.adminPickSpinner()
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke trekke en spiller.'
    return
  }
  current.value = null
  points.value = null
  note.value = ''
  rolling.value = true
  // a short roll through the names that are still in play, easing off, then landing on the one the server picked
  const names = data.value.candidates.map(c => c.name)
  let step = 0
  const total = 18
  const tick = () => {
    if (step >= total) {
      shown.value = winner.name
      current.value = winner
      rolling.value = false
      return
    }
    shown.value = names[Math.floor(Math.random() * names.length)]
    step++
    timer = setTimeout(tick, 60 + step * 9)
  }
  tick()
}

async function record() {
  if (!current.value || points.value === null || points.value === '') return
  saving.value = true
  error.value = ''
  try {
    data.value = await bdayApi.adminRecordSpin(current.value.id, points.value, note.value)
    current.value = null
    shown.value = ''
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke lagre spinnet.'
    await load()
  } finally {
    saving.value = false
  }
}

function edit(s) {
  if (!edits[s.guestId]) edits[s.guestId] = { points: s.bonusPoints, note: s.note || '' }
  return edits[s.guestId]
}

function edited(s) {
  const e = edits[s.guestId]
  return !!e && e.points !== null && (e.points !== s.bonusPoints || (e.note || '') !== (s.note || ''))
}

async function saveEdit(s) {
  if (!edited(s)) return
  error.value = ''
  try {
    data.value = await bdayApi.adminUpdateSpin(s.guestId, edits[s.guestId].points, edits[s.guestId].note)
    delete edits[s.guestId]
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke lagre.'
  }
}

async function remove(s) {
  error.value = ''
  try {
    data.value = await bdayApi.adminDeleteSpin(s.guestId)
    delete edits[s.guestId]
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke fjerne spinnet.'
  }
}
</script>

<style scoped>
.wh-stage { text-align: center; border: 2px solid var(--rule); border-radius: 3px; padding: 22px 14px; background: var(--ink-raised); }
.wh-left { font-family: var(--font-mono); font-size: 0.78rem; letter-spacing: 0.06em; text-transform: uppercase; color: var(--text-dim); }
.wh-name { font-family: var(--font-display); font-weight: 700; font-size: 2.6rem; line-height: 1.15; min-height: 3.2rem; margin: 8px 0 14px; overflow-wrap: anywhere; }
.wh-name.is-rolling { opacity: 0.6; }
.wh-form { margin-top: 16px; border: 2px solid var(--gold); border-radius: 3px; padding: 14px; }
.wh-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.wh-chip { padding: 3px 12px; background: transparent; color: var(--text); border: 1.5px solid var(--rule); border-radius: 3px; font-family: var(--font-mono); cursor: pointer; }
.wh-chip { padding: 8px 18px; font-size: 1.05rem; }
.wh-chip.is-on { background: var(--text); color: var(--ink-raised); }
.wh-chip:hover { background: var(--text); color: var(--ink-raised); }
.wh-manual { margin-top: 14px; }
.wh-manual summary { cursor: pointer; color: var(--text-dim); font-size: 0.9rem; }
.wh-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.wh-row-name { flex: 1; min-width: 110px; }
.wh-row-pts { width: 84px; padding: 6px 8px; }
.wh-row-note { flex: 1.2; min-width: 120px; padding: 6px 8px; }
</style>
