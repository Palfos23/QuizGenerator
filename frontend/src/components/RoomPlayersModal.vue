<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal" role="dialog" aria-modal="true" aria-label="Administrer spillere">
      <h2>Spillere</h2>
      <p class="page-subtitle" style="margin-bottom:16px;">
        {{ isHost ? 'Fjern en spiller som ikke kan koble til igjen, slik at spillet kan fortsette.' : 'Hvis verten har mistet forbindelsen, kan du overta slik at spillet kan fortsette.' }}
      </p>

      <div v-if="error" class="banner error" style="margin-bottom:12px;">{{ error }}</div>

      <div class="saved-quiz-list">
        <div v-for="p in participants" :key="p.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              <span style="display:inline-block; width:10px; height:10px; border-radius:50%; margin-right:6px;" :style="{ background: p.color }"></span>
              {{ p.displayName }}
              <span v-if="p.host" class="tag" style="margin-left:6px;">Vert</span>
            </div>
          </div>
          <div style="display:flex; align-items:center; gap:8px;">
            <span v-if="p.connected === false && p.id !== yourParticipantId" class="tag offline">Frakoblet</span>
            <span v-else class="tag" :style="{ background: 'rgba(31, 122, 80, 0.15)', color: 'var(--teal)' }">I rommet</span>
            <button
              v-if="isHost && !p.host && p.id !== yourParticipantId"
              class="btn btn-danger btn-sm"
              :disabled="busyId === p.id"
              @click="kick(p)"
            >{{ busyId === p.id ? 'Fjerner…' : 'Fjern' }}</button>
          </div>
        </div>
      </div>

      <p v-if="!isHost && hostIsDisconnected" style="margin-top:16px; color:var(--text-dim); font-size:0.9rem;">
        Verten ser ut til å ha mistet forbindelsen. Hvis vedkommende ikke kommer tilbake, kan du overta som vert.
      </p>

      <div style="display:flex; gap:10px; justify-content:flex-end; margin-top:20px;">
        <button v-if="!isHost && hostIsDisconnected" class="btn btn-primary" :disabled="claiming" @click="claim">
          {{ claiming ? 'Overtar…' : 'Overta som vert' }}
        </button>
        <button class="btn btn-secondary" @click="$emit('close')">Lukk</button>
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
    error.value = e.response?.data?.message || 'Kunne ikke fjerne spilleren.'
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
    error.value = e.response?.data?.message || 'Kunne ikke overta som vert.'
  } finally {
    claiming.value = false
  }
}
</script>
