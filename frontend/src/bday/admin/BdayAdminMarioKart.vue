<template>
  <div>
    <div v-if="error" class="banner error">{{ error }}</div>

    <!-- Registration and the points table -->
    <div class="mk-box" v-if="settings">
      <label class="mk-switch">
        <input type="checkbox" :checked="settings.marioKartOpen" :disabled="savingSettings" @change="setOpen($event.target.checked)" />
        <span class="mk-track" aria-hidden="true"></span>
        <span><strong>Gjestene kan registrere tid</strong> <small>{{ settings.marioKartOpen ? '- åpent nå' : '- stengt' }}</small></span>
      </label>

      <div class="mk-points">
        <div class="field" style="flex:1; min-width:200px; margin:0;">
          <label>Poeng per plass <span class="picker-hint">1., 2., 3. ... atskilt med komma</span></label>
          <input type="text" v-model="pointsDraft" @keydown.enter="saveSettings" />
        </div>
        <div class="field" style="width:130px; margin:0;">
          <label>Alle andre</label>
          <input type="number" min="0" step="1" v-model.number="otherDraft" @keydown.enter="saveSettings" />
        </div>
        <button class="btn btn-secondary" :disabled="savingSettings || !settingsChanged" @click="saveSettings">{{ savingSettings ? 'Lagrer…' : 'Lagre poeng' }}</button>
      </div>
      <p class="picker-hint" style="margin:8px 0 0;">Raskest får første tall, nest raskest det andre, og så videre. Lik tid gir lik plass. Alle som har en tid, men havner utenfor tabellen, får «alle andre»-poengene.</p>
    </div>

    <h2 style="font-size:1.2rem; margin:22px 0 8px;">Tider</h2>
    <div v-if="loading" style="color:var(--text-dim);">Laster…</div>
    <div v-else-if="!rows.length" class="empty-state">Ingen spillere ennå - legg dem til under Spillere.</div>
    <div v-else class="candidate-list">
      <div v-for="g in rows" :key="g.guestId" class="candidate-row mk-row">
        <span class="mk-rank">{{ g.rank ?? '–' }}</span>
        <span class="mk-name">{{ g.name }}</span>
        <LapTimeInput small :model-value="drafts[g.guestId] ?? ''" @update:model-value="v => drafts[g.guestId] = v" @enter="save(g)" />
        <button class="btn btn-secondary btn-sm" :disabled="savingId === g.guestId || !timeChanged(g)" @click="save(g)">Lagre</button>
        <span v-if="g.timeMs !== null" class="mk-time">{{ g.points }} p</span>
        <button v-if="g.timeMs !== null" class="btn btn-danger btn-sm" @click="clear(g)">Fjern</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import bdayApi from '../services/bdayApi'
import LapTimeInput from '../components/LapTimeInput.vue'
import { formatLap } from '../lib/lap'

// The host's Mario Kart tab: open/close time registration, set how lap times turn into points, and enter, fix or remove
// anyone's time. Guests enter their own on /bday/mariokart; the host can always override here.
const settings = ref(null)
const pointsDraft = ref('')
const otherDraft = ref(1)
const rows = ref([])
const loading = ref(true)
const error = ref('')
const savingSettings = ref(false)
const savingId = ref(null)
const drafts = reactive({}) // guest id -> the time typed in that row

const settingsChanged = computed(() => settings.value &&
  (pointsDraft.value.trim() !== settings.value.marioKartPoints || otherDraft.value !== settings.value.marioKartOtherPoints))

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await bdayApi.adminMarioKart()
    rows.value = data.guests
    // each row's boxes start on the time already saved, so a wrong one is fixed in place
    for (const g of data.guests) {
      if (g.timeMs !== null) drafts[g.guestId] = formatLap(g.timeMs)
      else if (!(g.guestId in drafts)) drafts[g.guestId] = ''
    }
    settings.value = data.settings
    pointsDraft.value = data.settings.marioKartPoints
    otherDraft.value = data.settings.marioKartOtherPoints
  } catch (e) {
    error.value = 'Kunne ikke laste Mario Kart.'
  } finally {
    loading.value = false
  }
}

async function pushSettings(next) {
  savingSettings.value = true
  error.value = ''
  try {
    await bdayApi.adminUpdateSettings(next)
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke lagre.'
    await load()
  } finally {
    savingSettings.value = false
  }
}

function setOpen(open) {
  return pushSettings({ ...settings.value, marioKartOpen: open })
}

function saveSettings() {
  if (!settingsChanged.value) return
  return pushSettings({ marioKartOpen: settings.value.marioKartOpen, marioKartPoints: pointsDraft.value, marioKartOtherPoints: otherDraft.value ?? 0 })
}

function timeChanged(g) {
  const draft = drafts[g.guestId]
  return !!draft && draft !== formatLap(g.timeMs)
}

async function save(g) {
  const time = drafts[g.guestId]
  if (!timeChanged(g)) return
  savingId.value = g.guestId
  error.value = ''
  try {
    await bdayApi.adminSetLapTime(g.guestId, time)
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke lagre tiden.'
  } finally {
    savingId.value = null
  }
}

async function clear(g) {
  error.value = ''
  try {
    await bdayApi.adminClearLapTime(g.guestId)
    drafts[g.guestId] = ''
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke fjerne tiden.'
  }
}
</script>

<style scoped>
.mk-box { border: 2px solid var(--rule); border-radius: 3px; padding: 14px; background: var(--ink-raised); }
.mk-switch { display: flex; align-items: center; gap: 12px; cursor: pointer; margin-bottom: 14px; text-transform: none; font-weight: 400; letter-spacing: normal; color: var(--text); font-size: 1rem; }
.mk-switch input { position: absolute; opacity: 0; pointer-events: none; }
.mk-track { width: 46px; height: 26px; border: 2px solid var(--rule); border-radius: 13px; position: relative; flex-shrink: 0; transition: background 0.15s; }
.mk-track::after { content: ''; position: absolute; top: 2px; left: 2px; width: 18px; height: 18px; border-radius: 50%; background: var(--text); transition: transform 0.15s; }
.mk-switch input:checked + .mk-track { background: var(--teal); border-color: var(--teal); }
.mk-switch input:checked + .mk-track::after { transform: translateX(20px); background: var(--ink-raised); }
.mk-switch small { color: var(--text-dim); }
.mk-points { display: flex; gap: 10px; align-items: flex-end; flex-wrap: wrap; }
.mk-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.mk-rank { width: 22px; font-family: var(--font-mono); color: var(--text-dim); font-size: 0.85rem; }
.mk-name { flex: 1; min-width: 110px; }
.mk-time { font-family: var(--font-mono); font-size: 0.82rem; color: var(--text-dim); white-space: nowrap; }
</style>
