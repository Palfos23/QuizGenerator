<template>
  <div>
    <div class="grid-status-bar">
      <div class="grid-progress" style="text-align:center; width:100%;">Grid {{ (state?.currentGridIndex ?? 0) + 1 }} / {{ state?.totalGrids ?? '?' }}: {{ state?.gridTitle }}</div>
      <div v-if="state" style="color:var(--text-dim); font-size:0.85rem; text-align:center; width:100%;">{{ state.gridTheme }}</div>
    </div>

    <LoadingState v-if="loading" full message="Loading the grid…" />
    <div v-if="error" class="banner error">{{ error }}</div>

    <template v-if="state && !state.finished">
      <div v-if="state.awaitingGridChoice" class="tension-choice-overlay">
        <template v-if="isPicker">
          <div style="color:var(--gold); text-transform:uppercase; letter-spacing:0.5px; font-size:1rem; margin-bottom:6px;">
            Grid {{ state.currentGridIndex + 1 }} / {{ state.totalGrids }}
          </div>
          <h2 style="margin:0 0 24px;">Choose a grid</h2>
          <div class="tension-choice-grid">
            <button
              v-for="g in state.gridChoices"
              :key="g.id"
              class="tension-choice-card"
              :disabled="choosing"
              @click="chooseGrid(g)"
            >
              <strong>{{ g.title }}</strong>
              <div style="color:var(--text-dim); font-size:0.85rem; margin-top:4px; font-weight:400;">{{ sportLabel(g.sport) }} · {{ g.entryCount }} to find</div>
            </button>
          </div>
        </template>
        <div v-else class="banner" style="text-align:center; background:rgba(255,255,255,0.03);">
          Waiting for {{ pickerName }} to choose a grid…
        </div>
      </div>

      <template v-else>
      <div class="mp-player-row">
        <div
          v-for="p in state.players"
          :key="p.participantId"
          class="mp-player-card"
          :class="{ 'active-turn': p.participantId === state.currentTurnParticipantId && !state.gridComplete, eliminated: p.eliminatedThisGrid }"
          :style="{ borderColor: p.color }"
        >
          <strong>{{ p.name }}</strong>
          <LivesHearts :max="state.maxStrikes" :used="p.livesUsed" style="margin-top:4px;" />
          <div style="font-size:0.8rem; color:var(--text-dim); margin-top:4px;">Total: {{ p.totalScore }}</div>
        </div>
      </div>

      <div v-if="!state.gridComplete && isYourTurn" class="guess-box-wrap no-print" :class="{ 'hide-on-scroll': hideSearchBox }">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <p style="text-align:center; margin:0 0 8px; color:var(--gold); font-weight:600;">Your turn</p>
          <input
            type="text"
            v-model="searchTerm"
            placeholder="Search for an answer…"
            aria-label="Search for an answer"
            autocomplete="off"
            autocorrect="off"
            autocapitalize="off"
            spellcheck="false"
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
              {{ a.name }}
            </button>
          </div>
        </div>
        <button
          class="btn btn-danger pass-turn-btn"
          :disabled="guessing"
          @click="showSkipConfirm = true"
        >Pass turn (costs a life)</button>
      </div>

      <ConfirmModal
        v-if="showSkipConfirm"
        title="Pass your turn?"
        message="You'll lose a life, same as a wrong guess."
        confirm-text="Pass turn"
        cancel-text="Keep guessing"
        @confirm="confirmSkipTurn"
        @cancel="showSkipConfirm = false"
      />

      <div v-if="!state.gridComplete && !isYourTurn" class="banner" style="text-align:center; background:rgba(255,255,255,0.03);">
        Waiting for {{ currentTurnName }}'s turn…
      </div>

      <div v-if="state.gridComplete" class="modal-backdrop">
        <div class="completion-popup">
          <h2 style="margin-top:0;">Grid complete!</h2>
          <div class="score-square-grid">
            <div v-for="(p, i) in leaderboardForGrid" :key="p.name" class="score-square" :class="{ leader: i === 0 }">
              <div class="score-square-name">{{ p.name }}</div>
              <div class="score-square-number">{{ p.total }}</div>
              <div v-if="p.roundDelta > 0" class="score-square-delta">+{{ p.roundDelta }}</div>
            </div>
          </div>

          <template v-if="recapTiles.length && recapSolvedCount < recapTiles.length">
            <div class="grid-recap-label">
              {{ recapSolvedCount }} / {{ recapTiles.length }} found — <span style="color:var(--coral);">red</span> tiles went unguessed
            </div>
            <div class="grid-tiles-recap">
              <div
                v-for="e in recapTiles"
                :key="e.id"
                class="grid-tile"
                :class="e.wasSolved ? 'correct' : 'revealed-only'"
              >
                <span v-if="e.wasSolved" class="grid-tile-status correct">✓</span>
                <span v-else class="grid-tile-status wrong">✕</span>
                <div v-if="state.revealMode === 'DESCRIPTION'" class="grid-tile-description">
                  {{ e.revealedDescription || '?' }}
                </div>
                <img
                  v-else-if="tileImage(e)"
                  :src="tileImage(e)"
                  alt=""
                  class="grid-tile-logo"
                  :class="{ 'is-photo': !!e.athletePhotoUrl, 'is-fit': state.fitImages && !!e.athletePhotoUrl }"
                  @error="$event.target.style.display = 'none'"
                />
                <div
                  v-if="e.hintValue != null || e.hintLabel"
                  class="grid-tile-hint"
                  :style="{ background: e.hintColor || 'var(--gold)', color: readableTextColor(e.hintColor) }"
                >{{ e.hintValue != null ? formatHint(e.hintLabel, e.hintValue) : e.hintLabel }}</div>
                <div class="grid-tile-name">{{ e.athleteName || '?' }}</div>
              </div>
            </div>
          </template>

          <button v-if="isHost" class="btn btn-primary" style="margin-top:12px; width:100%;" :disabled="advancing" @click="nextGrid">
            {{ advancing ? 'Loading…' : (state.currentGridIndex + 1 < state.totalGrids ? 'Next grid' : 'Finish game') }}
          </button>
          <div v-else style="margin-top:8px; color:var(--text-dim);">Waiting for the host to continue…</div>
        </div>
      </div>

      <div class="grid-tiles">
        <div
          v-for="e in state.entries"
          :key="e.id"
          class="grid-tile"
          :class="{ correct: e.solved, 'revealed-only': !e.solved && e.athleteName, 'just-solved': e.id === justSolvedId }"
        >
          <span v-if="e.solved" class="grid-tile-status correct">✓</span>
          <span v-else-if="e.athleteName" class="grid-tile-status wrong">✕</span>
          <div v-if="state.revealMode === 'DESCRIPTION'" class="grid-tile-description">
            {{ e.revealedDescription || '?' }}
          </div>
          <img
            v-else-if="tileImage(e)"
            :src="tileImage(e)"
            alt=""
            class="grid-tile-logo"
            :class="{ 'is-photo': e.athletePhotoUrl, 'is-fit': state.fitImages && e.athletePhotoUrl }"
            @error="$event.target.style.display = 'none'"
          />
          <div
            v-if="e.hintValue != null || e.hintLabel"
            class="grid-tile-hint"
            :style="{ background: e.hintColor || 'var(--gold)', color: readableTextColor(e.hintColor) }"
          >{{ e.hintValue != null ? formatHint(e.hintLabel, e.hintValue) : e.hintLabel }}</div>
          <div class="grid-tile-name">{{ e.athleteName || '?' }}</div>
        </div>
      </div>
      </template>
    </template>

    <div style="display:flex; align-items:center; gap:12px; margin-top:20px; flex-wrap:wrap;">
      <button class="btn btn-secondary btn-sm no-print" @click="leave">← Leave game</button>
      <span class="tag no-print" style="background:rgba(255,255,255,0.06); color:var(--text-dim);">Room: {{ roomCode }}</span>
    </div>

    <div v-if="resultOverlay" class="grid-result-overlay" :class="resultOverlay.correct ? 'correct' : 'wrong'">
      <div class="grid-result-text">{{ resultOverlay.correct ? 'Correct' : 'Wrong' }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import api from '../services/api'
import { readableTextColor, formatHint, sportLabel } from '../constants'
import ConfirmModal from './ConfirmModal.vue'
import LivesHearts from './LivesHearts.vue'
import LoadingState from './LoadingState.vue'
import { useHideOnScroll } from '../composables/useHideOnScroll'
import { usePolling } from '../composables/usePolling'

const props = defineProps({
  roomCode: { type: String, required: true },
  yourParticipantId: { type: [Number, String], required: true },
  isHost: { type: Boolean, default: false }
})
const emit = defineEmits(['gameOver', 'leave'])
const { hidden: hideSearchBox } = useHideOnScroll()

const state = ref(null)
const scoresAtGridStart = ref({})
// Full tile data (photo/description/hint/name) for every entry, fetched once the
// grid completes so the results modal can show the board recap - which answers
// were found and which went unguessed.
const revealedEntries = ref([])

const leaderboardForGrid = computed(() => {
  if (!state.value) return []
  return [...state.value.players]
    .map(p => ({
      name: p.name,
      total: p.totalScore,
      roundDelta: p.totalScore - (scoresAtGridStart.value[p.name] || 0)
    }))
    .sort((a, b) => b.total - a.total)
})
// The board as it stood at the buzzer: every entry with its real answer, flagged
// by whether a player actually guessed it. Sorted the same way the live board is
// (state.entries), so the recap reads top-to-bottom identically.
const recapTiles = computed(() => {
  if (!state.value || !revealedEntries.value.length) return []
  const solvedIds = new Set(state.value.entries.filter(e => e.solved).map(e => e.id))
  const byId = new Map(revealedEntries.value.map(e => [e.id, e]))
  return state.value.entries
    .map(e => {
      const full = byId.get(e.id)
      return full ? { ...full, wasSolved: solvedIds.has(e.id) } : null
    })
    .filter(Boolean)
})
const recapSolvedCount = computed(() => recapTiles.value.filter(e => e.wasSolved).length)
let lastGridIndexSeen = null
const loading = ref(true)
const error = ref('')
const searchTerm = ref('')
const searchResults = ref([])
const guessing = ref(false)
const advancing = ref(false)
const justSolvedId = ref(null)
const shakeGuessBox = ref(false)

const resultOverlay = ref(null) // { correct } or null when hidden
let resultOverlayTimeout = null
function showResultOverlay(correct) {
  clearTimeout(resultOverlayTimeout)
  resultOverlay.value = null
  requestAnimationFrame(() => {
    resultOverlay.value = { correct }
    resultOverlayTimeout = setTimeout(() => { resultOverlay.value = null }, 1200)
  })
}

const isYourTurn = computed(() => !!state.value && state.value.currentTurnParticipantId === props.yourParticipantId)
const currentTurnName = computed(() =>
  state.value?.players.find(p => p.participantId === state.value.currentTurnParticipantId)?.name || '…'
)
const isPicker = computed(() => !!state.value && state.value.pickerParticipantId === props.yourParticipantId)
const pickerName = computed(() =>
  state.value?.players.find(p => p.participantId === state.value.pickerParticipantId)?.name || '…'
)
const choosing = ref(false)

function tileImage(e) {
  return e.athletePhotoUrl || e.logoUrl
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
  if (fresh.currentGridIndex !== lastGridIndexSeen) {
    scoresAtGridStart.value = Object.fromEntries(fresh.players.map(p => [p.name, p.totalScore]))
    lastGridIndexSeen = fresh.currentGridIndex
    revealedEntries.value = []
  }
  state.value = fresh
  if (fresh.gridComplete && !revealedEntries.value.length) {
    api.revealAllGridEntries(fresh.currentGridId).then(list => { revealedEntries.value = list }).catch(() => {})
  }
  if (fresh.finished) {
    stopPolling()
    const scores = fresh.players.map(p => [p.name, p.totalScore])
    emit('gameOver', scores)
  }
}

const { stop: stopPolling } = usePolling(poll, 1200)

let searchDebounce = null
watch(searchTerm, (val) => {
  clearTimeout(searchDebounce)
  const trimmed = (val || '').trim()
  if (!trimmed) {
    searchResults.value = []
    return
  }
  searchDebounce = setTimeout(async () => {
    try {
      const results = await api.searchGridCandidates(state.value.currentGridId, val)
      searchResults.value = trimmed.length < 3
        ? results.filter(a => a.name.toLowerCase() === trimmed.toLowerCase())
        : results
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
      showResultOverlay(true)
    } else {
      shakeGuessBox.value = true
      setTimeout(() => { shakeGuessBox.value = false }, 400)
      showResultOverlay(false)
    }
    applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit that guess.'
  } finally {
    guessing.value = false
  }
}

async function chooseGrid(g) {
  choosing.value = true
  try {
    const fresh = await api.chooseGridBattleGrid(props.roomCode, g.id)
    applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not choose that grid.'
  } finally {
    choosing.value = false
  }
}

const showSkipConfirm = ref(false)
function confirmSkipTurn() {
  showSkipConfirm.value = false
  skipTurn()
}

async function skipTurn() {
  guessing.value = true
  searchTerm.value = ''
  searchResults.value = []
  try {
    const fresh = await api.skipGridBattleTurn(props.roomCode)
    applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not skip your turn.'
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
  stopPolling()
  emit('leave')
}
</script>
