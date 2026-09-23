<template>
  <BdayPinGate>
    <div style="max-width:640px; margin:40px auto; padding:0 20px; text-align:center;">
      <h1 style="font-size:2.4rem; margin-bottom:4px;">🎉 Leaderboard 🎉</h1>
      <p class="page-subtitle">Updates automatically as guests finish.</p>

      <div v-if="!entries.length" class="empty-state" style="margin-top:24px;">Nobody's finished yet…</div>
      <div v-else style="display:flex; flex-direction:column; gap:10px; margin-top:24px;">
        <div
          v-for="(e, i) in entries"
          :key="e.id"
          class="candidate-row"
          style="display:flex; align-items:center; gap:16px; padding:16px 20px; font-size:1.4rem;"
        >
          <span style="font-weight:700; color:var(--gold); min-width:40px;">{{ i + 1 }}</span>
          <span style="flex:1; text-align:left; font-weight:600;">{{ e.name }}</span>
          <span style="font-weight:700;">{{ e.score }}</span>
        </div>
      </div>
    </div>
  </BdayPinGate>
</template>

<script setup>
import { ref } from 'vue'
import bdayApi from '../services/bdayApi'
import BdayPinGate from '../components/BdayPinGate.vue'
import { usePolling } from '../../composables/usePolling'

const entries = ref([])

async function fetchLeaderboard() {
  try {
    entries.value = await bdayApi.leaderboard()
  } catch (e) {
    // display-only board - fail quietly and just try again next poll
  }
}

usePolling(fetchLeaderboard, 4000)
</script>
