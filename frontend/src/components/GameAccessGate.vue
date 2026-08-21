<template>
  <LoadingState v-if="state === 'checking'" full />

  <div v-else-if="state === 'denied'" class="empty-state" style="max-width:480px; margin:80px auto; padding:32px 28px; text-align:center;">
    <p style="margin:0; font-size:1.05rem;">{{ message }}</p>
  </div>

  <slot v-else />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'
import LoadingState from './LoadingState.vue'

const props = defineProps({
  // Matches PlayAccessService.requireAccessForKey's keys - see api.js's route
  // list ('tension', 'grid-battle', '501', 'imposter', 'starting-xi-battle', 'bullseye').
  game: { type: String, required: true }
})

const state = ref('checking') // 'checking' | 'denied' | 'allowed'
const message = ref('')

// Runs before any of the wrapped game's own setup UI ever renders, so a
// restricted user only ever sees this message - never player-name inputs,
// mode choices, or a "Create game" button they'd just get rejected from later.
onMounted(async () => {
  try {
    await api.checkGameAccess(props.game)
    state.value = 'allowed'
  } catch (e) {
    message.value = e.response?.data?.message || "You don't currently have access to this game - contact an administrator."
    state.value = 'denied'
  }
})
</script>
