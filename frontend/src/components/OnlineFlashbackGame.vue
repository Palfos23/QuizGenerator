<template>
  <div>
    <LoadingState v-if="loading" message="Loading the round…" full />
    <div v-if="error" class="banner error">{{ error }}</div>

    <template v-if="state && !state.finished">
      <div class="grid-status-bar">
        <div class="grid-progress" style="text-align:center; width:100%;">Round {{ state.currentRoundIndex + 1 }} / {{ state.totalRounds }}</div>
      </div>

      <h1 style="text-align:center; margin:6px 0 20px;">Round {{ state.currentRoundIndex + 1 }}</h1>

      <div class="mp-player-row">
        <div
          v-for="p in state.players"
          :key="p.participantId"
          class="mp-player-card"
          :class="{ 'active-turn': p.participantId === state.currentTurnParticipantId && !state.roundRevealed, disconnected: p.connected === false }"
          :style="{ borderColor: p.color }"
        >
          <strong>{{ p.name }}</strong>
          <span v-if="p.connected === false" class="tag offline" style="display:block; margin-top:4px;">Offline</span>
          <div style="font-size:0.8rem; color:var(--text-dim); margin-top:4px;">Score: {{ p.totalScore }}</div>
        </div>
      </div>

      <div v-if="!state.roundRevealed" class="guess-box-wrap no-print">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <template v-if="isYourTurn">
            <div class="guess-box-header">
              <p style="margin:0; color:var(--gold); font-weight:600;">Your turn</p>
            </div>
            <form @submit.prevent="submitGuess">
              <input
                type="number"
                v-model.number="guessValue"
                placeholder="Guess a year…"
                style="background-image:none; padding-left:20px; text-align:center;"
                autocomplete="off"
                @input="duplicateGuessError = false"
              />
            </form>
            <p v-if="duplicateGuessError" style="color:var(--coral); font-size:0.85rem; margin:8px 0 0; text-align:center;">
              That year's already been guessed on an earlier clue.
            </p>
            <button
              type="button"
              class="btn btn-primary"
              style="margin-top:10px; width:100%;"
              :disabled="guessValue === null || guessValue === '' || submitting"
              @click="submitGuess"
            >Guess</button>
          </template>
          <p v-else style="text-align:center; color:var(--text-dim); margin:0;">
            Waiting for {{ currentTurnName }}'s turn…
          </p>
        </div>
      </div>

      <div class="tension-reveal-list" style="max-width:560px; margin:20px auto 0;">
        <div v-for="(hint, hIdx) in state.visibleHints" :key="hIdx" class="tension-reveal-row is-revealed">
          <div class="tension-reveal-rank">{{ hIdx + 1 }}</div>
          <div class="tension-reveal-main">
            <div class="tension-reveal-answer" style="font-weight:400; font-style:italic;">{{ hint }}</div>
          </div>
          <div class="tension-reveal-guessers">
            <span v-if="!guessesForHint(hIdx).length" class="tension-reveal-nobody">Nobody guessed</span>
            <span
              v-for="g in guessesForHint(hIdx)"
              :key="g.name"
              class="tension-reveal-chip"
              :class="{ 'flashback-chip-winner': state.roundRevealed && (state.roundWinners || []).includes(g.name) }"
              :style="{ borderColor: colorOf(g.name) }"
            >{{ g.name }}: {{ g.year }}</span>
          </div>
        </div>
      </div>

      <div v-if="state.roundRevealed" class="modal-backdrop">
        <div class="completion-popup">
          <h2 style="margin-top:0;">Round complete!</h2>

          <div class="score-square-grid">
            <div v-for="(p, i) in leaderboardForRound" :key="p.name" class="score-square" :class="{ leader: i === 0 }">
              <div class="score-square-name">{{ p.name }}</div>
              <div class="score-square-number">{{ p.total }}</div>
              <div v-if="p.roundDelta > 0" class="score-square-delta">+{{ p.roundDelta }}</div>
            </div>
          </div>

          <div class="bullseye-truth-callout">
            <div class="bullseye-truth-label">{{ resultLabel }}</div>
            <div style="font-weight:700; font-size:1.4rem; margin-top:4px;">{{ state.year }}</div>
          </div>

          <button v-if="isHost" class="btn btn-primary" style="margin-top:16px; width:100%;" :disabled="advancing" @click="nextRound">
            {{ advancing ? 'Loading…' : (state.currentRoundIndex + 1 < state.totalRounds ? 'Next round' : 'Finish game') }}
          </button>
          <div v-else style="margin-top:16px; text-align:center; color:var(--text-dim);">Waiting for the host to continue…</div>
        </div>
      </div>
    </template>

    <div style="display:flex; align-items:center; gap:12px; margin-top:20px; flex-wrap:wrap;">
      <button class="btn btn-secondary btn-sm no-print" @click="leave">← Leave game</button>
      <span class="tag no-print" style="background:rgba(255,255,255,0.06); color:var(--text-dim);">Room: {{ roomCode }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import api from '../services/api'
import LoadingState from './LoadingState.vue'
import { useRoomChannel } from '../composables/useRoomChannel'
import { useTurnTitleAlert } from '../composables/useTurnTitleAlert'

const props = defineProps({
  roomCode: { type: String, required: true },
  yourParticipantId: { type: [Number, String], required: true },
  isHost: { type: Boolean, default: false }
})
const emit = defineEmits(['gameOver', 'leave'])

const state = ref(null)
const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const advancing = ref(false)
const guessValue = ref(null)
const duplicateGuessError = ref(false)
const shakeGuessBox = ref(false)

const isYourTurn = computed(() => !!state.value && state.value.currentTurnParticipantId === props.yourParticipantId)
useTurnTitleAlert(isYourTurn)
const currentTurnName = computed(() =>
  state.value?.players.find(p => p.participantId === state.value.currentTurnParticipantId)?.name || '…'
)

function guessesForHint(hIdx) {
  return (state.value?.guesses || []).filter(g => g.hintIndex === hIdx)
}
function colorOf(name) {
  return state.value?.players.find(p => p.name === name)?.color || 'var(--border)'
}

const resultLabel = computed(() => {
  if (!state.value) return ''
  const names = (state.value.roundWinners || []).join(' and ')
  if (state.value.wasExactMatch) {
    return state.value.roundWinners.length > 1 ? `🎯 ${names} both nailed it!` : `🎯 ${names} nailed it!`
  }
  if (state.value.roundWinners.length > 1) return `Nobody got it exactly - ${names} tied closest`
  if (state.value.roundWinners.length === 1) return `Nobody got it exactly - ${names} was closest`
  return 'The year was'
})

const leaderboardForRound = computed(() => {
  if (!state.value) return []
  return [...state.value.players]
    .map(p => ({
      name: p.name,
      total: p.totalScore,
      roundDelta: (state.value.roundWinners || []).includes(p.name) ? state.value.pointsAwarded : 0
    }))
    .sort((a, b) => b.total - a.total)
})

async function poll() {
  try {
    const fresh = await api.getFlashbackOnlineState(props.roomCode)
    applyState(fresh)
  } catch (e) {
    error.value = 'Lost connection to the room - retrying…'
  } finally {
    loading.value = false
  }
}

function applyState(fresh) {
  error.value = ''
  state.value = fresh
  if (fresh.finished) {
    stopPolling()
    const scores = fresh.players.map(p => [p.name, p.totalScore])
    emit('gameOver', scores)
  }
}

const { stop: stopPolling } = useRoomChannel(`/topic/rooms/${props.roomCode}/state`, { poll, onMessage: applyState })

async function submitGuess() {
  if (guessValue.value === null || guessValue.value === '' || submitting.value) return
  submitting.value = true
  try {
    const fresh = await api.submitFlashbackOnlineGuess(props.roomCode, Math.trunc(guessValue.value))
    guessValue.value = null
    applyState(fresh)
  } catch (e) {
    const message = e.response?.data?.message || 'Could not submit that guess.'
    error.value = message
    if (message.toLowerCase().includes('already been guessed')) {
      duplicateGuessError.value = true
      shakeGuessBox.value = true
      setTimeout(() => { shakeGuessBox.value = false }, 400)
    }
  } finally {
    submitting.value = false
  }
}

async function nextRound() {
  advancing.value = true
  try {
    const fresh = await api.advanceFlashbackOnlineRound(props.roomCode)
    applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not advance to the next round.'
  } finally {
    advancing.value = false
  }
}

function leave() {
  stopPolling()
  emit('leave')
}
</script>
