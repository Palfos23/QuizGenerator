<template>
  <div>
    <LoadingState v-if="loadingChoices" message="Loading grid choices…" full />

    <div v-else-if="roundChoices.length" class="tension-choice-overlay">
      <div style="color:var(--gold); text-transform:uppercase; letter-spacing:0.5px; font-size:1rem; margin-bottom:6px;">
        Grid {{ currentGridIndex + 1 }} / {{ totalGrids }}
      </div>
      <h2 style="margin:0 0 24px;">{{ pickerName }}, choose a grid</h2>
      <div class="tension-choice-grid">
        <button v-for="g in roundChoices" :key="g.id" class="tension-choice-card" @click="chooseGrid(g)">
          <strong>{{ g.title }}</strong>
          <div style="color:var(--text-dim); font-size:0.85rem; margin-top:4px; font-weight:400;">{{ sportLabel(g.sport) }} · {{ g.entryCount }} to find</div>
        </button>
      </div>
    </div>

    <template v-else>
    <div class="grid-status-bar">
      <div class="grid-progress" style="text-align:center; width:100%;">Grid {{ currentGridIndex + 1 }} / {{ totalGrids }}: {{ gridState?.title }}</div>
      <div v-if="gridState" style="color:var(--text-dim); font-size:0.85rem; text-align:center; width:100%;">{{ gridState.theme }}</div>
    </div>

    <LoadingState v-if="loading" message="Loading grid…" full />

    <template v-else-if="gridState">
      <div class="mp-player-row">
        <div
          v-for="p in players"
          :key="p.name"
          class="mp-player-card"
          :class="{ 'active-turn': p.name === currentPlayerName && !gridComplete, eliminated: eliminatedPlayers.includes(p.name) }"
          :style="{ borderColor: p.color }"
        >
          <strong>{{ p.name }}</strong>
          <LivesHearts :max="gridState.maxStrikes" :used="livesUsed[p.name] || 0" style="margin-top:4px;" />
          <div style="font-size:0.8rem; color:var(--text-dim); margin-top:4px;">Total: {{ scores[p.name] || 0 }}</div>
        </div>
      </div>

      <div v-if="!gridComplete" class="guess-box-wrap no-print" :class="{ 'hide-on-scroll': hideSearchBox }">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <p style="text-align:center; margin:0 0 8px; color:var(--gold); font-weight:600;">{{ currentPlayerName }}'s turn</p>
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

      <div v-if="gridComplete" class="modal-backdrop">
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
                <div v-if="gridState.revealMode === 'DESCRIPTION'" class="grid-tile-description">
                  {{ e.revealedDescription || '?' }}
                </div>
                <img
                  v-else-if="tileImage(e)"
                  :src="tileImage(e)"
                  alt=""
                  class="grid-tile-logo"
                  :class="{ 'is-photo': !!e.athletePhotoUrl }"
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

          <button class="btn btn-primary" style="margin-top:12px; width:100%;" @click="nextGrid">
            {{ currentGridIndex + 1 < totalGrids ? 'Next grid' : 'Finish game' }}
          </button>
        </div>
      </div>

      <div class="grid-tiles">
        <div
          v-for="e in gridState.entries"
          :key="e.id"
          class="grid-tile"
          :class="{ correct: e.guessedByUser, 'revealed-only': e.solved && !e.guessedByUser, 'just-solved': e.id === justSolvedId }"
        >
          <span v-if="e.guessedByUser" class="grid-tile-status correct">✓</span>
          <span v-else-if="e.solved" class="grid-tile-status wrong">✕</span>
          <div v-if="gridState.revealMode === 'DESCRIPTION'" class="grid-tile-description">
            {{ e.revealedDescription || '?' }}
          </div>
          <img
            v-else-if="tileImage(e)"
            :src="tileImage(e)"
            alt=""
            class="grid-tile-logo"
            :class="{ 'is-photo': !!e.athletePhotoUrl }"
            @error="$event.target.style.display = 'none'"
          />
          <div
            v-if="e.hintValue != null || e.hintLabel"
            class="grid-tile-hint"
            :style="{ background: e.hintColor || 'var(--gold)', color: readableTextColor(e.hintColor) }"
          >{{ e.hintValue != null ? formatHint(e.hintLabel, e.hintValue) : e.hintLabel }}</div>
          <div class="grid-tile-name">{{ e.solved ? e.athleteName : '?' }}</div>
        </div>
      </div>
    </template>

    <div v-else class="empty-state">
      <p>Couldn't load this grid.</p>
      <button class="btn btn-primary" @click="proceedToCurrentRound">Try again</button>
    </div>
    </template>

    <div v-if="resultOverlay" class="grid-result-overlay" :class="resultOverlay.correct ? 'correct' : 'wrong'">
      <div class="grid-result-text">{{ resultOverlay.correct ? 'Correct' : 'Wrong' }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import passAndPlayState from '../services/passAndPlayState'
import { readableTextColor, formatHint, sportLabel } from '../constants'
import ConfirmModal from './ConfirmModal.vue'
import LivesHearts from './LivesHearts.vue'
import LoadingState from './LoadingState.vue'
import { useHideOnScroll } from '../composables/useHideOnScroll'

const props = defineProps({
  mode: { type: String, default: 'manual' }, // 'manual' | 'random'
  grids: { type: Array, default: () => [] }, // [{ id, title, ... }] - fixed list, mode === 'manual' only
  numGrids: { type: Number, default: 0 }, // total rounds - mode === 'random' only
  players: { type: Array, required: true } // [{ name, color }]
})
const emit = defineEmits(['gameOver'])
const { hidden: hideSearchBox } = useHideOnScroll()

const totalGrids = computed(() => props.mode === 'random' ? props.numGrids : props.grids.length)
// This round's starting player rotates by seat, same convention as Tension's
// rotatedPlayers[0] - and since they're also the one who picks in "random"
// mode, this doubles as the picker's name before a grid is even chosen.
const pickerName = computed(() => props.players[currentGridIndex.value % props.players.length]?.name)
const currentGridId = computed(() => props.mode === 'manual'
  ? props.grids[currentGridIndex.value]?.id
  : chosenGrids.value[currentGridIndex.value]?.id)

const currentGridIndex = ref(0)
const chosenGrids = ref([]) // grids actually picked so far ('random' mode only), index-aligned with currentGridIndex
const roundChoices = ref([]) // this round's 3 candidate grids, before a pick is made ('random' mode only)
const loadingChoices = ref(false)
const gridState = ref(null)
const loading = ref(true)
const revealedEntryIds = ref([])
const livesUsed = ref({})
const eliminatedPlayers = ref([])
const currentPlayerIdx = ref(0)
const scores = ref(Object.fromEntries(props.players.map(p => [p.name, 0])))
const scoresAtGridStart = ref(Object.fromEntries(props.players.map(p => [p.name, 0])))

const leaderboardForGrid = computed(() => {
  return [...props.players]
    .map(p => ({
      name: p.name,
      total: scores.value[p.name] || 0,
      roundDelta: (scores.value[p.name] || 0) - (scoresAtGridStart.value[p.name] || 0)
    }))
    .sort((a, b) => b.total - a.total)
})
// The board at the buzzer for the results modal - every entry with its real
// answer (revealRemaining() has merged the unfound ones in by this point),
// flagged by whether a player actually guessed it.
const recapTiles = computed(() => {
  if (!gridState.value) return []
  return gridState.value.entries.map(e => ({ ...e, wasSolved: !!e.guessedByUser }))
})
const recapSolvedCount = computed(() => recapTiles.value.filter(e => e.wasSolved).length)
const gridComplete = ref(false)
const searchTerm = ref('')
const searchResults = ref([])
const guessing = ref(false)
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

const currentPlayerName = computed(() => props.players[currentPlayerIdx.value]?.name)

function tileImage(entry) {
  if (entry.athletePhotoUrl) return entry.athletePhotoUrl
  return entry.logoUrl
}

async function loadGrid(gridId) {
  loading.value = true
  revealedEntryIds.value = []
  eliminatedPlayers.value = []
  gridComplete.value = false
  scoresAtGridStart.value = { ...scores.value }
  currentPlayerIdx.value = currentGridIndex.value % props.players.length // rotate who starts, like Tension
  try {
    gridState.value = await api.getMultiplayerGridStart(gridId)
    livesUsed.value = Object.fromEntries(props.players.map(p => [p.name, 0]))
  } catch (e) {
    toast.show(e.response?.data?.message || 'Could not load this grid - please try again.', 'error')
  } finally {
    loading.value = false
  }
}

async function loadRoundChoices() {
  loadingChoices.value = true
  try {
    roundChoices.value = await api.fetchGridBattleRoundChoices(3, chosenGrids.value.map(g => g.id))
  } catch (e) {
    toast.show(e.response?.data?.message || 'Could not load the next round - please try again.', 'error')
  } finally {
    loadingChoices.value = false
  }
}

function chooseGrid(g) {
  chosenGrids.value = [...chosenGrids.value, g]
  roundChoices.value = []
  loadGrid(g.id)
}

// Manual mode already knows every grid up front. Random mode picks one round
// at a time - resume straight into an already-chosen round (e.g. after a
// refresh) rather than re-offering a choice, since a choice already made is a
// commitment, not something to redo; otherwise offer this round's 3 choices.
function proceedToCurrentRound() {
  if (props.mode === 'manual') {
    return loadGrid(props.grids[currentGridIndex.value].id)
  } else if (chosenGrids.value.length > currentGridIndex.value) {
    return loadGrid(chosenGrids.value[currentGridIndex.value].id)
  } else {
    return loadRoundChoices()
  }
}

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
      const results = await api.searchGridCandidates(currentGridId.value, val)
      searchResults.value = trimmed.length < 3
        ? results.filter(a => a.name.toLowerCase() === trimmed.toLowerCase())
        : results
    } catch (e) {
      // autocomplete is a convenience - fail quietly
    }
  }, 200)
})

async function submitGuess(athlete) {
  guessing.value = true
  searchTerm.value = ''
  searchResults.value = []
  const player = currentPlayerName.value
  try {
    const result = await api.submitMultiplayerGridGuess(
      currentGridId.value, athlete.id, revealedEntryIds.value
    )
    if (result.correct) {
      revealedEntryIds.value.push(result.entry.id)
      const idx = gridState.value.entries.findIndex(e => e.id === result.entry.id)
      if (idx !== -1) gridState.value.entries[idx] = result.entry
      justSolvedId.value = result.entry.id
      setTimeout(() => { justSolvedId.value = null }, 600)
      scores.value[player] = (scores.value[player] || 0) + 1
      showResultOverlay(true)
      if (result.allSolved) {
        gridComplete.value = true
        return
      }
    } else {
      livesUsed.value[player] = (livesUsed.value[player] || 0) + 1
      shakeGuessBox.value = true
      setTimeout(() => { shakeGuessBox.value = false }, 400)
      showResultOverlay(false)
      if (livesUsed.value[player] >= gridState.value.maxStrikes) {
        eliminatedPlayers.value.push(player)
      }
      if (eliminatedPlayers.value.length >= props.players.length) {
        gridComplete.value = true
        await revealRemaining()
        return
      }
    }
    advanceTurn()
  } finally {
    guessing.value = false
  }
}

const showSkipConfirm = ref(false)
function confirmSkipTurn() {
  showSkipConfirm.value = false
  skipTurn()
}

function skipTurn() {
  const player = currentPlayerName.value
  searchTerm.value = ''
  searchResults.value = []
  // Same cost as a wrong guess - passing isn't a free way to stall for time
  // to think without the same strike a bad guess would cost.
  livesUsed.value[player] = (livesUsed.value[player] || 0) + 1
  shakeGuessBox.value = true
  setTimeout(() => { shakeGuessBox.value = false }, 400)
  if (livesUsed.value[player] >= gridState.value.maxStrikes) {
    eliminatedPlayers.value.push(player)
  }
  if (eliminatedPlayers.value.length >= props.players.length) {
    gridComplete.value = true
    revealRemaining()
    return
  }
  advanceTurn()
}

function advanceTurn() {
  const n = props.players.length
  for (let step = 1; step <= n; step++) {
    const candidateIdx = (currentPlayerIdx.value + step) % n
    if (!eliminatedPlayers.value.includes(props.players[candidateIdx].name)) {
      currentPlayerIdx.value = candidateIdx
      return
    }
  }
}

async function revealRemaining() {
  try {
    const revealed = await api.revealAllGridEntries(currentGridId.value)
    gridState.value.entries = gridState.value.entries.map(e => {
      if (e.solved) return e // already correctly guessed - keep as-is
      const match = revealed.find(r => r.id === e.id)
      return match || e
    })
  } catch (e) {
    // if this fails, tiles just stay hidden - not worth blocking the game-over flow over
  }
}

function nextGrid() {
  if (currentGridIndex.value + 1 < totalGrids.value) {
    currentGridIndex.value += 1
  } else {
    emit('gameOver', props.players.map(p => [p.name, scores.value[p.name] || 0]))
  }
}

function progressIdentity() {
  return {
    mode: props.mode,
    gridIds: props.mode === 'manual' ? props.grids.map(g => g.id) : null,
    numGrids: props.mode === 'random' ? props.numGrids : null,
    playerNames: props.players.map(p => p.name)
  }
}

function identityMatches(saved) {
  const current = progressIdentity()
  return saved.mode === current.mode
      && JSON.stringify(saved.gridIds) === JSON.stringify(current.gridIds)
      && saved.numGrids === current.numGrids
      && JSON.stringify(saved.playerNames) === JSON.stringify(current.playerNames)
}

function saveProgress() {
  passAndPlayState.save('grid-battle-progress', {
    ...progressIdentity(),
    currentGridIndex: currentGridIndex.value,
    revealedEntryIds: revealedEntryIds.value,
    livesUsed: livesUsed.value,
    eliminatedPlayers: eliminatedPlayers.value,
    currentPlayerIdx: currentPlayerIdx.value,
    scores: scores.value,
    gridComplete: gridComplete.value,
    // The entries array already safely mixes solved (full name/photo, from the
    // server's guess response) and unsolved (hint only, name withheld) tiles -
    // exactly what this browser legitimately saw. Saving it directly means
    // restoring a solved tile never needs to reconstruct its revealed data
    // from scratch, which isn't otherwise derivable from just its ID.
    entries: gridState.value?.entries,
    // 'random' mode only - the grids actually picked so far, so a resume
    // doesn't have to re-offer (and potentially re-roll) a choice already made.
    chosenGrids: chosenGrids.value
  })
}

async function initGame() {
  const saved = passAndPlayState.load('grid-battle-progress')
  if (saved && identityMatches(saved)) {
    currentGridIndex.value = saved.currentGridIndex
    chosenGrids.value = saved.chosenGrids || []
    await proceedToCurrentRound()
    revealedEntryIds.value = saved.revealedEntryIds
    livesUsed.value = saved.livesUsed
    eliminatedPlayers.value = saved.eliminatedPlayers
    currentPlayerIdx.value = saved.currentPlayerIdx
    scores.value = saved.scores
    gridComplete.value = saved.gridComplete
    // proceedToCurrentRound() just fetched this same grid fresh (all-unsolved),
    // if it was already chosen - replace its entries with the saved mix of
    // solved/unsolved tiles instead.
    if (gridState.value && saved.entries) {
      gridState.value.entries = saved.entries
    }
  } else {
    await proceedToCurrentRound()
  }
}

watch([revealedEntryIds, livesUsed, eliminatedPlayers, currentPlayerIdx, scores, gridComplete, currentGridIndex, chosenGrids], saveProgress, { deep: true })
// Make sure the buzzer board is fully revealed for the results modal - covers
// the restore-into-a-finished-grid path, where revealRemaining() never ran.
watch(gridComplete, (val) => {
  if (val && gridState.value?.entries.some(e => !e.solved)) {
    revealRemaining()
  }
})

onMounted(initGame)
watch(currentGridIndex, proceedToCurrentRound)
</script>
