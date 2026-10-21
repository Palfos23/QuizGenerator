<template>
  <div>
    <LoadingState v-if="loading" :message="$t('Laster runden…')" full />
    <div v-if="error" class="banner error">{{ error }}</div>

    <template v-if="state && !state.finished">
      <div class="grid-status-bar">
        <div class="grid-progress" style="text-align:center; width:100%;">{{ $t('Runde') }} {{ state.currentRoundIndex + 1 }} / {{ state.totalRounds }}</div>
      </div>

      <h1 style="text-align:center; margin:6px 0 20px;">{{ $t('Runde') }} {{ state.currentRoundIndex + 1 }}</h1>

      <div class="mp-player-row">
        <div
          v-for="p in state.players"
          :key="p.participantId"
          class="mp-player-card"
          :class="{ disconnected: p.connected === false && p.participantId !== props.yourParticipantId }"
          :style="{ borderColor: p.color }"
        >
          <strong>{{ p.name }}</strong>
          <span v-if="p.connected === false && p.participantId !== props.yourParticipantId" class="tag offline" style="display:block; margin-top:4px;">{{ $t('Frakoblet§OnlineFlashbackGameT') }}</span>
          <div v-if="!state.roundRevealed" style="font-size:0.8rem; margin-top:4px;" :style="{ color: p.hasAnsweredCurrentHint ? 'var(--teal)' : 'var(--text-dim)' }">
            {{ p.hasAnsweredCurrentHint ? $t('✓ svart') : $t('— venter —') }}
          </div>
          <div style="font-size:0.8rem; color:var(--text-dim); margin-top:4px;">{{ $t('Poeng:') }} {{ p.totalScore }}</div>
        </div>
      </div>

      <div v-if="!state.roundRevealed" class="guess-box-wrap no-print">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <template v-if="!haveIAnswered">
            <form @submit.prevent="submitGuess">
              <input
                type="number"
                v-model.number="guessValue"
                :placeholder="$t('Gjett et år…')"
                style="background-image:none; padding-left:20px; text-align:center;"
                autocomplete="off"
                @input="duplicateGuessError = false"
              />
            </form>
            <p v-if="duplicateGuessError" style="color:var(--coral); font-size:0.85rem; margin:8px 0 0; text-align:center;">
              {{ $t('Det året er allerede gjettet på et tidligere hint.') }}
            </p>
            <button
              type="button"
              class="btn btn-primary"
              style="margin-top:10px; width:100%;"
              :disabled="guessValue === null || guessValue === '' || submitting"
              @click="submitGuess"
            >{{ $t('Gjett') }}</button>
          </template>
          <p v-else style="text-align:center; color:var(--text-dim); margin:0;">
            {{ $t('Venter på at alle andre skal svare…') }}
          </p>
        </div>
      </div>

      <!-- Host only, and only while the round is untouched (first clue, nobody's
           answered) - swapping later would throw guesses away. -->
      <div
        v-if="isHost && !state.roundRevealed && state.hintIndex === 0 && !state.players.some(p => p.hasAnsweredCurrentHint)"
        class="no-print"
        style="text-align:center; margin-top:10px;"
      >
        <button type="button" class="btn btn-secondary btn-sm" :disabled="swapping" @click="swapYear">
          {{ swapping ? $t('Finner et nytt…') : $t('↻ Hatt denne før? Velg et annet år') }}
        </button>
      </div>

      <div class="tension-reveal-list" style="max-width:560px; margin:20px auto 0;">
        <div v-for="(hint, hIdx) in state.visibleHints" :key="hIdx" class="tension-reveal-row is-revealed">
          <div class="tension-reveal-rank">{{ hIdx + 1 }}</div>
          <div class="tension-reveal-main">
            <div class="tension-reveal-answer" style="font-weight:400; font-style:italic;">{{ hint }}</div>
          </div>
          <div class="tension-reveal-guessers">
            <span v-if="!guessesForHint(hIdx).length" class="tension-reveal-nobody">{{ $t('Ingen gjettet') }}</span>
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
          <h2 style="margin-top:0;">{{ $t('Runden er ferdig!') }}</h2>

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
            {{ advancing ? $t('Laster…') : (state.currentRoundIndex + 1 < state.totalRounds ? $t('Neste runde') : $t('Fullfør spillet')) }}
          </button>
          <div v-else style="margin-top:16px; text-align:center; color:var(--text-dim);">{{ $t('Venter på at verten skal fortsette…') }}</div>
        </div>
      </div>
    </template>

    <div style="display:flex; align-items:center; gap:12px; margin-top:20px; flex-wrap:wrap;">
      <button class="btn btn-secondary btn-sm no-print" @click="leave">{{ $t('← Forlat spillet') }}</button>
      <span class="tag no-print" style="background:rgb(var(--fg-rgb) / 0.06); color:var(--text-dim);">{{ $t('Rom:') }} {{ roomCode }}</span>
    </div>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import LoadingState from './LoadingState.vue'
import { useRoomChannel, createStaleGuard } from '../composables/useRoomChannel'

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

// No turn order in Flashback - anyone can guess whenever they like, as many
// times as there are hints left this round. This just tracks whether YOU
// specifically have already answered the current hint, to swap your own
// input for a "waiting on the rest of the table" message.
const haveIAnswered = computed(() =>
  !!state.value?.players.find(p => p.participantId === props.yourParticipantId)?.hasAnsweredCurrentHint
)

function guessesForHint(hIdx) {
  return (state.value?.guesses || []).filter(g => g.hintIndex === hIdx)
}
function colorOf(name) {
  return state.value?.players.find(p => p.name === name)?.color || 'var(--border)'
}

const resultLabel = computed(() => {
  if (!state.value) return ''
  const names = (state.value.roundWinners || []).join(_t('og'))
  if (state.value.wasExactMatch) {
    return state.value.roundWinners.length > 1 ? _t('🎯 {0} traff begge!', [names]) : _t('🎯 {0} traff!', [names])
  }
  if (state.value.roundWinners.length > 1) return _t('Ingen traff helt riktig - {0} var like nær', [names])
  if (state.value.roundWinners.length === 1) return _t('Ingen traff helt riktig - {0} var nærmest', [names])
  return _t('Året var')
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

const staleGuard = createStaleGuard()

async function poll() {
  const stillFresh = staleGuard.begin()
  try {
    const fresh = await api.getFlashbackOnlineState(props.roomCode)
    if (stillFresh()) applyState(fresh)
  } catch (e) {
    error.value = _t('Mistet forbindelsen til rommet - prøver igjen…')
  } finally {
    loading.value = false
  }
}

function applyState(fresh) {
  staleGuard.claim()
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
  const stillFresh = staleGuard.begin()
  try {
    const fresh = await api.submitFlashbackOnlineGuess(props.roomCode, Math.trunc(guessValue.value))
    guessValue.value = null
    if (stillFresh()) applyState(fresh)
  } catch (e) {
    const message = e.response?.data?.message || _t('Kunne ikke sende inn gjetningen.')
    error.value = message
    if (/allerede gjettet|already been guessed/i.test(message)) {
      duplicateGuessError.value = true
      shakeGuessBox.value = true
      setTimeout(() => { shakeGuessBox.value = false }, 400)
    }
  } finally {
    submitting.value = false
  }
}

const swapping = ref(false)
async function swapYear() {
  swapping.value = true
  const stillFresh = staleGuard.begin()
  try {
    const fresh = await api.rerollFlashbackOnlineYear(props.roomCode)
    if (stillFresh()) applyState(fresh)
  } catch (e) {
    toast.show(e.response?.data?.message || _t('Kunne ikke bytte til et annet år - prøv igjen.'), 'error')
  } finally {
    swapping.value = false
  }
}

async function nextRound() {
  advancing.value = true
  const stillFresh = staleGuard.begin()
  try {
    const fresh = await api.advanceFlashbackOnlineRound(props.roomCode)
    if (stillFresh()) applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke gå videre til neste runde.')
  } finally {
    advancing.value = false
  }
}

function leave() {
  stopPolling()
  emit('leave')
}
</script>
