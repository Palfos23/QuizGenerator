<template>
  <div>
    <div class="grid-status-bar">
      <div class="grid-progress" style="text-align:center; width:100%;">Grid {{ currentGridIndex + 1 }} / {{ grids.length }}: {{ gridState?.title }}</div>
      <div v-if="gridState" style="color:var(--text-dim); font-size:0.85rem; text-align:center; width:100%;">{{ gridState.theme }}</div>
    </div>

    <div v-if="loading" style="color:var(--text-dim);">Loading grid…</div>

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
          <div class="lives-hearts" style="margin-top:4px;">
            <span
              v-for="i in gridState.maxStrikes"
              :key="i"
              class="life-heart"
              :class="{ lost: i > gridState.maxStrikes - (livesUsed[p.name] || 0) }"
            ></span>
          </div>
          <div style="font-size:0.8rem; color:var(--text-dim); margin-top:4px;">Total: {{ scores[p.name] || 0 }}</div>
        </div>
      </div>

      <div v-if="!gridComplete" class="guess-box-wrap no-print">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <p style="text-align:center; margin:0 0 8px; color:var(--gold); font-weight:600;">{{ currentPlayerName }}'s turn</p>
          <input
            type="text"
            v-model="searchTerm"
            placeholder="Search for an answer…"
            aria-label="Search for an answer"
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
          <button
            class="btn btn-secondary btn-sm"
            style="margin-top:8px; width:100%;"
            :disabled="guessing"
            @click="skipTurn"
          >Pass turn (costs a life)</button>
        </div>
      </div>

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

          <div v-if="unsolvedEntries.length" style="text-align:left; margin-top:4px;">
            <div style="color:var(--text-dim); font-size:0.82rem; margin-bottom:8px;">Not found:</div>
            <div v-for="e in unsolvedEntries" :key="e.id" class="imposter-reveal-entry" style="background:rgba(255,255,255,0.03); border-color:var(--border);">
              <span style="font-weight:700;">{{ e.name }}</span>
            </div>
          </div>

          <button class="btn btn-primary" style="margin-top:12px; width:100%;" @click="nextGrid">
            {{ currentGridIndex + 1 < grids.length ? 'Next grid' : 'Finish game' }}
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
          <img
            v-if="tileImage(e)"
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

    <div v-if="resultOverlay" class="grid-result-overlay" :class="resultOverlay.correct ? 'correct' : 'wrong'">
      <div class="grid-result-text">{{ resultOverlay.correct ? 'Correct' : 'Wrong' }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import passAndPlayState from '../services/passAndPlayState'
import { readableTextColor, formatHint } from '../constants'

const props = defineProps({
  grids: { type: Array, required: true }, // [{ id, title, ... }]
  players: { type: Array, required: true } // [{ name, color }]
})
const emit = defineEmits(['gameOver'])

const currentGridIndex = ref(0)
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
const unsolvedEntries = computed(() => {
  if (!gridState.value) return []
  return gridState.value.entries
    .filter(e => !revealedEntryIds.value.includes(e.id))
    .map(e => ({ id: e.id, name: revealMap.value[e.id] }))
    .filter(e => e.name)
})
const gridComplete = ref(false)
const revealMap = ref({}) // entryId -> athleteName, fetched once the grid completes
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

async function loadGrid() {
  loading.value = true
  revealedEntryIds.value = []
  revealMap.value = {}
  eliminatedPlayers.value = []
  gridComplete.value = false
  scoresAtGridStart.value = { ...scores.value }
  currentPlayerIdx.value = currentGridIndex.value % props.players.length // rotate who starts, like Tension
  try {
    gridState.value = await api.getMultiplayerGridStart(props.grids[currentGridIndex.value].id)
    livesUsed.value = Object.fromEntries(props.players.map(p => [p.name, 0]))
  } finally {
    loading.value = false
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
      const results = await api.searchGridCandidates(props.grids[currentGridIndex.value].id, val)
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
      props.grids[currentGridIndex.value].id, athlete.id, revealedEntryIds.value
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
    const revealed = await api.revealAllGridEntries(props.grids[currentGridIndex.value].id)
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
  if (currentGridIndex.value + 1 < props.grids.length) {
    currentGridIndex.value += 1
    loadGrid()
  } else {
    emit('gameOver', props.players.map(p => [p.name, scores.value[p.name] || 0]))
  }
}

function progressIdentity() {
  return {
    gridIds: props.grids.map(g => g.id),
    playerNames: props.players.map(p => p.name)
  }
}

function identityMatches(saved) {
  const current = progressIdentity()
  return JSON.stringify(saved.gridIds) === JSON.stringify(current.gridIds)
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
    entries: gridState.value?.entries
  })
}

async function initGame() {
  const saved = passAndPlayState.load('grid-battle-progress')
  if (saved && identityMatches(saved)) {
    currentGridIndex.value = saved.currentGridIndex
    await loadGrid()
    revealedEntryIds.value = saved.revealedEntryIds
    livesUsed.value = saved.livesUsed
    eliminatedPlayers.value = saved.eliminatedPlayers
    currentPlayerIdx.value = saved.currentPlayerIdx
    scores.value = saved.scores
    gridComplete.value = saved.gridComplete
    // loadGrid() just fetched this same grid fresh (all-unsolved) - replace its
    // entries with the saved mix of solved/unsolved tiles instead.
    if (gridState.value && saved.entries) {
      gridState.value.entries = saved.entries
    }
  } else {
    await loadGrid()
  }
}

watch([revealedEntryIds, livesUsed, eliminatedPlayers, currentPlayerIdx, scores, gridComplete, currentGridIndex], saveProgress, { deep: true })
watch(gridComplete, async (val) => {
  if (val) {
    try {
      revealMap.value = await api.getMultiplayerGridReveal(props.grids[currentGridIndex.value].id)
    } catch (e) {
      // reveal failing shouldn't block the rest of the completion modal
    }
  }
})

onMounted(initGame)
</script>
