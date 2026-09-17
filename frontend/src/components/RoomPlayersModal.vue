<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal" role="dialog" aria-modal="true" aria-label="Manage players">
      <h2>Players</h2>
      <p class="page-subtitle" style="margin-bottom:16px;">
        {{ isHost ? 'Remove a player who can\'t reconnect so the game can continue.' : 'If the host has disconnected, you can take over so the game can continue.' }}
      </p>

      <div v-if="error" class="banner error" style="margin-bottom:12px;">{{ error }}</div>

      <div class="saved-quiz-list">
        <div v-for="p in participants" :key="p.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              <span style="display:inline-block; width:10px; height:10px; border-radius:50%; margin-right:6px;" :style="{ background: p.color }"></span>
              {{ p.displayName }}
              <span v-if="p.host" class="tag" style="margin-left:6px;">Host</span>
            </div>
          </div>
          <div style="display:flex; align-items:center; gap:8px;">
            <span v-if="p.connected === false && p.id !== yourParticipantId" class="tag offline">Disconnected</span>
            <span v-else class="tag" :style="{ background: 'rgba(61,220,151,0.15)', color: 'var(--teal)' }">In room</span>
            <button
              v-if="isHost && !p.host && p.id !== yourParticipantId"
              class="btn btn-danger btn-sm"
              :disabled="busyId === p.id"
              @click="kick(p)"
            >{{ busyId === p.id ? 'Removing…' : 'Remove' }}</button>
          </div>
        </div>
      </div>

      <p v-if="!isHost && hostIsDisconnected" style="margin-top:16px; color:var(--text-dim); font-size:0.9rem;">
        The host looks disconnected. If they don't come back, you can take over hosting.
      </p>

      <div style="display:flex; gap:10px; justify-content:flex-end; margin-top:20px;">
        <button v-if="!isHost && hostIsDisconnected" class="btn btn-primary" :disabled="claiming" @click="claim">
          {{ claiming ? 'Taking over…' : 'Take over as host' }}
        </button>
        <button class="btn btn-secondary" @click="$emit('close')">Close</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import api from '../services/api'
import { useEscapeKey } from '../composables/useEscapeKey'

const props = defineProps({
  roomCode: { type: String, required: true },
  participants: { type: Array, default: () => [] },
  yourParticipantId: { type: [Number, String], default: null },
  isHost: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'changed'])

useEscapeKey(() => emit('close'))

const error = ref('')
const busyId = ref(null)
const claiming = ref(false)

const hostIsDisconnected = computed(() =>
  props.participants.some(p => p.host && p.connected === false)
)

async function kick(p) {
  error.value = ''
  busyId.value = p.id
  try {
    const dto = await api.kickParticipant(props.roomCode, p.id)
    emit('changed', dto)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not remove that player.'
  } finally {
    busyId.value = null
  }
}

async function claim() {
  error.value = ''
  claiming.value = true
  try {
    const dto = await api.claimHost(props.roomCode)
    emit('changed', dto)
    emit('close')
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not take over as host.'
  } finally {
    claiming.value = false
  }
}
</script>
