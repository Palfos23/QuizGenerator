<template>
  <div>
    <div class="grid-status-bar">
      <div class="grid-progress">Grid {{ (state?.currentGridIndex ?? 0) + 1 }} / {{ state?.totalGrids ?? '?' }}: {{ state?.gridTitle }}</div>
      <div v-if="state" style="color:var(--text-dim); font-size:0.85rem;">{{ state.gridTheme }}</div>
    </div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
    <div v-if="error" class="banner error">{{ error }}</div>

    <template v-if="state && !state.finished">
      <div class="mp-player-row">
        <div
          v-for="p in state.players"
          :key="p.participantId"
          class="mp-player-card"
          :class="{ 'active-turn': p.participantId === state.currentTurnParticipantId && !state.gridComplete, eliminated: p.eliminatedThisGrid }"
          :style="{ borderColor: p.color }"
        >
          <strong>{{ p.name }}</strong>
          <div class="lives-hearts" style="margin-top:4px;">
            <span
              v-for="i in state.maxStrikes"
              :key="i"
              class="life-heart"
              :class="{ lost: i <= p.livesUsed }"
            >{{ i <= p.livesUsed ? '🖤' : '❤️' }}</span>
          </div>
          <div style="font-size:0.8rem; color:var(--text-dim); margin-top:4px;">Total: {{ p.totalScore }}</div>
        </div>
      </div>

      <div v-if="!state.gridComplete && isYourTurn" class="guess-box" :class="{ shake: shakeGuessBox }">
        <p style="text-align:center; margin:0 0 8px; color:var(--gold); font-weight:600;">Your turn</p>
        <input
          type="text"
          v-model="searchTerm"
          placeholder="Search for a player…"
          aria-label="Search for an athlete"
          autocomplete="off"
          @keydown.esc="searchTerm = ''"
        />
        <div v-if="searchResults.length" class="guess-results">
          <button
            v-for="a in searchResults"
            :key="a.id"
            class="guess-result-row"
            :disabled="guessing"
            @click="submitGuess(a)"
          >
            {{ a.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ a.team }}</span>
          </button>
        </div>
      </div>

      <div v-else-if="!state.gridComplete" class="banner" style="text-align:center; background:rgba(255,255,255,0.03);">
        Waiting for {{ currentTurnName }}'s turn…
      </div>

      <div v-else class="banner success" style="text-align:center;">
        <div><strong>Grid complete!</strong></div>
        <button class="btn btn-primary" style="margin-top:12px;" :disabled="advancing" @click="nextGrid">
          {{ advancing ? 'Loading…' : (state.currentGridIndex + 1 < state.totalGrids ? 'Next grid' : 'Finish game') }}
        </button>
      </div>

      <div class="grid-tiles">
        <div
          v-for="e in state.entries"
          :key="e.id"
          class="grid-tile"
          :class="{ correct: e.solved, 'just-solved': e.id === justSolvedId }"
        >
          <span v-if="e.solved" class="grid-tile-status correct">✓</span>
          <img
            v-if="tileImage(e)"
            :src="tileImage(e)"
            alt=""
            class="grid-tile-logo"
            :class="{ 'is-photo': e.solved && e.athletePhotoUrl }"
          />
          <div
            class="grid-tile-hint"
            :style="{ background: e.hintColor || 'var(--gold)', color: readableTextColor(e.hintColor) }"
          >{{ e.hintLabel }} | {{ e.hintValue }}</div>
          <div class="grid-tile-name">{{ e.solved ? e.athleteName : '?' }}</div>
        </div>
      </div>
    </template>

    <button class="btn btn-secondary btn-sm no-print" style="margin-top:20px;" @click="leave">← Leave game</button>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import api from '../services/api'
import { readableTextColor } from '../constants'

const props = defineProps({
  roomCode: { type: String, required: true },
  yourParticipantId: { type: [Number, String], required: true }
})
const emit = defineEmits(['gameOver', 'leave'])

const state = ref(null)
const loading = ref(true)
const error = ref('')
const searchTerm = ref('')
const searchResults = ref([])
const guessing = ref(false)
const advancing = ref(false)
const justSolvedId = ref(null)
const shakeGuessBox = ref(false)

let pollTimer = null

const isYourTurn = computed(() => !!state.value && state.value.currentTurnParticipantId === props.yourParticipantId)
const currentTurnName = computed(() =>
  state.value?.players.find(p => p.participantId === state.value.currentTurnParticipantId)?.name || '…'
)

function tileImage(e) {
  return e.solved && e.athletePhotoUrl ? e.athletePhotoUrl : e.logoUrl
}

async function poll() {
  try {
    const fresh = await api.getGridBattleState(props.roomCode)
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
    clearInterval(pollTimer)
    const scores = {}
    fresh.players.forEach(p => { scores[p.name] = p.totalScore })
    emit('gameOver', scores)
  }
}

onMounted(() => {
  poll()
  pollTimer = setInterval(poll, 2000)
})
onUnmounted(() => clearInterval(pollTimer))

let searchDebounce = null
watch(searchTerm, (val) => {
  clearTimeout(searchDebounce)
  if (!val || val.trim().length < 2) {
    searchResults.value = []
    return
  }
  searchDebounce = setTimeout(async () => {
    try {
      searchResults.value = await api.searchGridCandidates(state.value.currentGridId, val)
    } catch (e) {
      // autocomplete failing isn't worth surfacing - just shows no results
    }
  }, 250)
})

async function submitGuess(athlete) {
  guessing.value = true
  searchTerm.value = ''
  searchResults.value = []
  try {
    const before = state.value.entries.filter(e => e.solved).length
    const fresh = await api.submitGridBattleGuess(props.roomCode, athlete.id)
    const after = fresh.entries.filter(e => e.solved).length
    if (after > before) {
      const newlySolved = fresh.entries.find(e => e.solved && !state.value.entries.find(old => old.id === e.id && old.solved))
      justSolvedId.value = newlySolved?.id || null
      setTimeout(() => { justSolvedId.value = null }, 600)
    } else {
      shakeGuessBox.value = true
      setTimeout(() => { shakeGuessBox.value = false }, 400)
    }
    applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit that guess.'
  } finally {
    guessing.value = false
  }
}

async function nextGrid() {
  advancing.value = true
  try {
    const fresh = await api.advanceGridBattleGrid(props.roomCode)
    applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not advance to the next grid.'
  } finally {
    advancing.value = false
  }
}

function leave() {
  clearInterval(pollTimer)
  emit('leave')
}
</script>
