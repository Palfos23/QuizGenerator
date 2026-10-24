<template>
  <div>
    <p class="page-subtitle">Lagene i musikkquizen. Du fører poengsummen for hvert lag - alle på laget får den lagt til totalen sin.</p>
    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field">
      <label>Nytt lag</label>
      <div style="display:flex; gap:10px;">
        <input type="text" v-model="newName" placeholder="Lagnavn" maxlength="40" @keydown.enter="createTeam" />
        <button class="btn btn-secondary" style="white-space:nowrap;" :disabled="creating" @click="createTeam">Legg til</button>
      </div>
    </div>

    <div v-if="loading" style="color:var(--text-dim);">Laster…</div>
    <template v-else>
      <div v-if="!data.teams.length" class="empty-state">Ingen lag ennå.</div>

      <div v-for="t in data.teams" :key="t.id" class="tm-card">
        <div class="tm-head">
          <input type="text" class="tm-name" v-model="names[t.id]" maxlength="40" :aria-label="'Navn på ' + t.name" @keydown.enter="save(t)" />
          <label class="tm-score">
            Poeng
            <input type="number" step="1" v-model.number="scores[t.id]" @keydown.enter="save(t)" />
          </label>
          <button class="btn btn-secondary btn-sm" :disabled="saving === t.id || !changed(t)" @click="save(t)">{{ saving === t.id ? 'Lagrer…' : 'Lagre' }}</button>
          <button class="btn btn-danger btn-sm" @click="pendingDelete = t">Slett</button>
        </div>
        <div class="tm-quick">
          <span class="picker-hint">Legg til poeng:</span>
          <button v-for="n in [1, 2, 3, 5, 10]" :key="n" type="button" class="tm-chip" @click="bump(t, n)">+{{ n }}</button>
          <button type="button" class="tm-chip" @click="bump(t, -1)">−1</button>
        </div>

        <div class="tm-members">
          <span v-for="m in t.members" :key="m.id" class="tm-member">
            {{ m.name }}
            <button type="button" class="tm-x" :aria-label="'Fjern ' + m.name + ' fra laget'" @click="assign(m.id, null)">✕</button>
          </span>
          <span v-if="!t.members.length" class="picker-hint">Ingen på laget ennå.</span>
        </div>
        <select v-if="data.unassigned.length" class="tm-add" :value="''" @change="assign(Number($event.target.value), t.id); $event.target.value = ''">
          <option value="">+ Legg til spiller…</option>
          <option v-for="p in data.unassigned" :key="p.id" :value="p.id">{{ p.name }}</option>
        </select>
      </div>

      <p v-if="data.unassigned.length" class="picker-hint" style="margin-top:12px;">
        Uten lag ({{ data.unassigned.length }}): {{ data.unassigned.map(p => p.name).join(', ') }}
      </p>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      title="Slette dette laget?"
      :message="`${pendingDelete.name} slettes. Spillerne på laget blir værende i spillet, men mister lagets musikkpoeng.`"
      confirm-text="Slett laget"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import bdayApi from '../services/bdayApi'
import ConfirmModal from '../../components/ConfirmModal.vue'

// The host's music-quiz tab: create teams, put people on them, keep each team's score. Every member of a team gets
// the team's score added to their own total (see the totals tab).
const data = ref({ teams: [], unassigned: [] })
const loading = ref(true)
const error = ref('')
const newName = ref('')
const creating = ref(false)
const saving = ref(null)
const names = reactive({}) // team id -> name being edited
const scores = reactive({}) // team id -> score being edited
const pendingDelete = ref(null)

onMounted(async () => {
  await refresh(await safe(() => bdayApi.adminTeams()))
  loading.value = false
})

async function safe(fn) {
  error.value = ''
  try {
    return await fn()
  } catch (e) {
    error.value = e.response?.data?.message || 'Det gikk ikke - prøv igjen.'
    return null
  }
}

async function refresh(next) {
  if (!next) return
  data.value = next
  for (const t of next.teams) {
    names[t.id] = t.name
    scores[t.id] = t.musicScore
  }
}

function changed(t) {
  return names[t.id] !== t.name || scores[t.id] !== t.musicScore
}

async function createTeam() {
  const name = newName.value.trim()
  if (!name) return
  creating.value = true
  const next = await safe(() => bdayApi.adminCreateTeam(name))
  creating.value = false
  if (next) newName.value = ''
  await refresh(next)
}

async function save(t) {
  if (!changed(t)) return
  saving.value = t.id
  await refresh(await safe(() => bdayApi.adminUpdateTeam(t.id, names[t.id], scores[t.id] ?? 0)))
  saving.value = null
}

// +1/+2/... saves straight away - handy while the music quiz is running
async function bump(t, n) {
  scores[t.id] = (scores[t.id] ?? 0) + n
  await refresh(await safe(() => bdayApi.adminUpdateTeam(t.id, names[t.id], scores[t.id])))
}

async function assign(guestId, teamId) {
  if (!guestId) return
  await refresh(await safe(() => bdayApi.adminAssignTeam(guestId, teamId)))
}

async function doDelete() {
  const t = pendingDelete.value
  pendingDelete.value = null
  if (t) await refresh(await safe(() => bdayApi.adminDeleteTeam(t.id)))
}
</script>

<style scoped>
.tm-card { border: 2px solid var(--rule); border-radius: 3px; padding: 12px 14px; margin-bottom: 12px; background: var(--ink-raised); }
.tm-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.tm-name { flex: 1; min-width: 140px; font-family: var(--font-display); font-weight: 600; font-size: 1.1rem; }
.tm-score { display: flex; align-items: center; gap: 6px; margin: 0; text-transform: none; letter-spacing: normal; font-weight: 400; color: var(--text-dim); }
.tm-score input { width: 80px; padding: 6px 8px; }
.tm-quick { display: flex; align-items: center; gap: 6px; margin: 10px 0 4px; flex-wrap: wrap; }
.tm-chip { padding: 3px 12px; background: transparent; color: var(--text); border: 1.5px solid var(--rule); border-radius: 3px; font-family: var(--font-mono); font-size: 0.85rem; cursor: pointer; }
.tm-chip:hover { background: var(--text); color: var(--ink-raised); }
.tm-members { display: flex; flex-wrap: wrap; gap: 6px; margin: 10px 0; }
.tm-member { display: inline-flex; align-items: center; gap: 6px; padding: 3px 4px 3px 10px; border: 1.5px solid var(--line); border-radius: 3px; }
.tm-x { background: none; border: none; cursor: pointer; color: var(--text-dim); font-size: 0.8rem; padding: 2px 4px; }
.tm-x:hover { color: var(--coral); }
.tm-add { max-width: 260px; }
</style>
