<template>
  <BdayPinGate>
    <div style="max-width:560px; margin:0 auto; padding:20px 20px 60px;">
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <h1 style="margin:0;">Manage players</h1>
        <div style="display:flex; gap:8px;">
          <router-link to="/bday" class="btn btn-secondary btn-sm">Back to quiz</router-link>
          <router-link to="/bday/leaderboard" class="btn btn-secondary btn-sm">Leaderboard</router-link>
        </div>
      </div>
      <p class="page-subtitle">Register everyone here before they start picking their name to play.</p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field" style="margin-top:20px;">
        <label>Add a guest</label>
        <div style="display:flex; gap:10px;">
          <input type="text" v-model="newGuestName" placeholder="Name" @keydown.enter="addGuest" />
          <button class="btn btn-secondary" :disabled="addingGuest" @click="addGuest">Add</button>
        </div>
      </div>

      <div v-if="loading" style="color:var(--text-dim); margin-top:20px;">Loading…</div>
      <template v-else>
        <div v-if="pending.length" class="field" style="margin-top:24px;">
          <label>Waiting to play</label>
          <div class="candidate-list">
            <div v-for="g in pending" :key="g.id" class="candidate-row">
              <span style="flex:1;">{{ g.name }}</span>
              <button class="btn btn-danger btn-sm" @click="removeGuest(g)">Remove</button>
            </div>
          </div>
        </div>

        <div v-if="done.length" class="field" style="margin-top:24px;">
          <label>Already finished</label>
          <div class="candidate-list">
            <div v-for="g in done" :key="g.id" class="candidate-row">
              <span style="flex:1;">{{ g.name }}</span>
              <span style="color:var(--text-dim); font-size:0.9rem;">Scored {{ g.score }}</span>
            </div>
          </div>
        </div>

        <div v-if="!pending.length && !done.length" class="empty-state" style="margin-top:24px;">No players registered yet.</div>
      </template>
    </div>
  </BdayPinGate>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import bdayApi from '../services/bdayApi'
import BdayPinGate from '../components/BdayPinGate.vue'

const guests = ref([])
const loading = ref(true)
const error = ref('')
const newGuestName = ref('')
const addingGuest = ref(false)

// listGuests() only ever returned PENDING guests (see BdayQuizService -
// BdayHostView's picker never needed to know about finished ones) - this
// admin page wants the full picture, so it also pulls the leaderboard
// (DONE guests) and merges the two rather than needing a new endpoint.
const pending = computed(() => guests.value.filter(g => g.status !== 'DONE'))
const done = computed(() => guests.value.filter(g => g.status === 'DONE'))

onMounted(loadGuests)

async function loadGuests() {
  loading.value = true
  error.value = ''
  try {
    const [pendingGuests, finishedGuests] = await Promise.all([
      bdayApi.listGuests(),
      bdayApi.leaderboard()
    ])
    guests.value = [...pendingGuests, ...finishedGuests]
  } catch (e) {
    error.value = 'Could not load the guest list.'
  } finally {
    loading.value = false
  }
}

async function addGuest() {
  const name = newGuestName.value.trim()
  if (!name) return
  addingGuest.value = true
  error.value = ''
  try {
    const guest = await bdayApi.addGuest(name)
    guests.value.push(guest)
    newGuestName.value = ''
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not add that guest.'
  } finally {
    addingGuest.value = false
  }
}

async function removeGuest(guest) {
  error.value = ''
  try {
    await bdayApi.deleteGuest(guest.id)
    guests.value = guests.value.filter(g => g.id !== guest.id)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not remove that guest.'
  }
}
</script>
