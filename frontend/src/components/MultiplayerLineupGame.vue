<template>
  <div>
    <div class="grid-status-bar">
      <div class="grid-progress" style="text-align:center; width:100%;">
        Board {{ currentLineupIndex + 1 }} / {{ lineups.length }}: {{ lineupState?.title }}
      </div>
    </div>

    <div v-if="lineupState && (lineupState.teamName || lineupState.opponentName)" class="pitch-scoreline">
      <div class="pitch-scoreline-team">
        <img v-if="lineupState.teamCrestUrl" :src="lineupState.teamCrestUrl" alt="" class="pitch-scoreline-crest" />
        <span>{{ lineupState.teamName }}</span>
      </div>
      <div v-if="lineupState.scoreFor != null && lineupState.scoreAgainst != null" class="pitch-scoreline-score">
        <span>{{ lineupState.scoreFor }}</span><span class="dash">-</span><span>{{ lineupState.scoreAgainst }}</span>
      </div>
      <div v-else class="pitch-scoreline-vs">vs</div>
      <div class="pitch-scoreline-team away">
        <img v-if="lineupState.opponentCrestUrl" :src="lineupState.opponentCrestUrl" alt="" class="pitch-scoreline-crest" />
        <span>{{ lineupState.opponentName }}</span>
      </div>
    </div>

    <div v-if="loading" style="color:var(--text-dim);">Loading board…</div>

    <template v-else-if="lineupState">
      <div class="mp-player-row">
        <div
          v-for="p in players"
          :key="p.name"
          class="mp-player-card"
          :class="{ 'active-turn': p.name === currentPlayerName && !lineupComplete, eliminated: eliminatedPlayers.includes(p.name) }"
          :style="{ borderColor: p.color }"
        >
          <strong>{{ p.name }}</strong>
          <div class="lives-hearts" style="margin-top:4px;">
            <span
              v-for="i in lineupState.maxStrikes"
              :key="i"
              class="life-heart"
              :class="{ lost: i > lineupState.maxStrikes - (livesUsed[p.name] || 0) }"
            ></span>
          </div>
          <div style="font-size:0.8rem; color:var(--text-dim); margin-top:4px;">Total: {{ scores[p.name] || 0 }}</div>
        </div>
      </div>

      <div v-if="!lineupComplete" class="guess-box-wrap no-print">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <p style="text-align:center; margin:0 0 8px; color:var(--gold); font-weight:600;">{{ currentPlayerName }}'s turn</p>
          <input
            type="text"
            v-model="searchTerm"
            placeholder="Search for a player…"
            aria-label="Search for a player"
            autocomplete="off"
            @keydown.esc="searchTerm = ''"
          />
          <div v-if="searchResults.length" class="guess-results">
            <button v-for="a in searchResults" :key="a.id" class="guess-result-row" :disabled="guessing" @click="submitGuess(a)">
              {{ a.name }}
            </button>
          </div>
          <button class="btn btn-secondary btn-sm" style="margin-top:8px; width:100%;" :disabled="guessing" @click="skipTurn">
            Pass turn (costs a life)
          </button>
        </div>
      </div>

      <div v-if="lineupComplete" class="modal-backdrop">
        <div class="completion-popup">
          <h2 style="margin-top:0;">Board complete!</h2>
          <div class="score-square-grid">
            <div v-for="(p, i) in leaderboardForLineup" :key="p.name" class="score-square" :class="{ leader: i === 0 }">
              <div class="score-square-name">{{ p.name }}</div>
              <div class="score-square-number">{{ p.total }}</div>
              <div v-if="p.roundDelta > 0" class="score-square-delta">+{{ p.roundDelta }}</div>
            </div>
          </div>

          <button class="btn btn-primary" style="margin-top:12px; width:100%;" @click="nextLineup">
            {{ currentLineupIndex + 1 < lineups.length ? 'Next board' : 'Finish game' }}
          </button>
        </div>
      </div>

      <div class="pitch">
        <PitchMarkings />
        <div v-for="(row, ri) in rows" :key="ri" class="pitch-row" :class="`pitch-row--${row.kind}`">
          <div v-for="slot in row.items" :key="slot.id ?? slot.slotIndex" class="pitch-slot">
            <div class="pitch-shirt" :class="{ solved: slot.solved, goalkeeper: slot.slotIndex === 0 }" :style="shirtStyle(slot)">
              <template v-if="!slot.solved">
                <span class="pitch-shirt-sleeve left"></span>
                <span class="pitch-shirt-sleeve right"></span>
                <span class="pitch-shirt-collar"></span>
              </template>
              <img v-if="slot.solved && slot.athletePhotoUrl" :src="slot.athletePhotoUrl" alt="" class="pitch-slot-photo" />
              <template v-else>{{ slot.shirtNumber }}</template>
              <span v-if="slot.captain" class="pitch-shirt-captain">C</span>
            </div>
            <div class="pitch-slot-name">{{ slot.solved ? slot.athleteName : '?' }}</div>
          </div>
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
import { displayRowsFor } from '../services/formations'
import { readableTextColor } from '../constants'
import PitchMarkings from './PitchMarkings.vue'

const DEFAULT_KIT_COLOR = '#d92332'
const DEFAULT_GK_KIT_COLOR = '#f2c230'

const props = defineProps({
  lineups: { type: Array, required: true }, // [{ id, title, ... }]
  players: { type: Array, required: true } // [{ name, color }]
})
const emit = defineEmits(['gameOver'])

const currentLineupIndex = ref(0)
const lineupState = ref(null)
const loading = ref(true)
const guessedSlotIds = ref([])
const solvedById = ref({}) // slot id -> solved LineupSlotDto from the server
const revealedNames = ref({}) // slot id -> name, once a board's given up on (everyone eliminated)
const livesUsed = ref({})
const eliminatedPlayers = ref([])
const currentPlayerIdx = ref(0)
const scores = ref(Object.fromEntries(props.players.map(p => [p.name, 0])))
const scoresAtLineupStart = ref(Object.fromEntries(props.players.map(p => [p.name, 0])))
const lineupComplete = ref(false)
const searchTerm = ref('')
const searchResults = ref([])
const guessing = ref(false)
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

const leaderboardForLineup = computed(() => {
  return [...props.players]
    .map(p => ({
      name: p.name,
      total: scores.value[p.name] || 0,
      roundDelta: (scores.value[p.name] || 0) - (scoresAtLineupStart.value[p.name] || 0)
    }))
    .sort((a, b) => b.total - a.total)
})

const rows = computed(() => {
  if (!lineupState.value) return []
  const merged = lineupState.value.slots.map(s => {
    if (solvedById.value[s.id]) return solvedById.value[s.id]
    if (revealedNames.value[s.id]) return { ...s, solved: true, athleteName: revealedNames.value[s.id] }
    return s
  })
  return displayRowsFor(lineupState.value.formation, merged)
})

function shirtStyle(slot) {
  const color = slot.slotIndex === 0
    ? (lineupState.value?.goalkeeperKitColor || DEFAULT_GK_KIT_COLOR)
    : (lineupState.value?.kitColor || DEFAULT_KIT_COLOR)
  return { '--kit-color': color, '--kit-text': readableTextColor(color) }
}

async function loadLineup() {
  loading.value = true
  guessedSlotIds.value = []
  solvedById.value = {}
  revealedNames.value = {}
  eliminatedPlayers.value = []
  lineupComplete.value = false
  scoresAtLineupStart.value = { ...scores.value }
  currentPlayerIdx.value = currentLineupIndex.value % props.players.length // rotate who starts, like Grid Battle
  try {
    lineupState.value = await api.getLineupPlayState(props.lineups[currentLineupIndex.value].id)
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
      const results = await api.searchLineupCandidates(props.lineups[currentLineupIndex.value].id, val)
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
    const result = await api.submitLineupGuess(
      props.lineups[currentLineupIndex.value].id, athlete.id, guessedSlotIds.value
    )
    if (result.correct) {
      guessedSlotIds.value.push(result.slot.id)
      solvedById.value[result.slot.id] = result.slot
      scores.value[player] = (scores.value[player] || 0) + 1
      showResultOverlay(true)
      if (result.allSolved) {
        lineupComplete.value = true
        return
      }
    } else {
      livesUsed.value[player] = (livesUsed.value[player] || 0) + 1
      shakeGuessBox.value = true
      setTimeout(() => { shakeGuessBox.value = false }, 400)
      showResultOverlay(false)
      if (livesUsed.value[player] >= lineupState.value.maxStrikes) {
        eliminatedPlayers.value.push(player)
      }
      if (eliminatedPlayers.value.length >= props.players.length) {
        lineupComplete.value = true
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
  if (livesUsed.value[player] >= lineupState.value.maxStrikes) {
    eliminatedPlayers.value.push(player)
  }
  if (eliminatedPlayers.value.length >= props.players.length) {
    lineupComplete.value = true
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
    revealedNames.value = await api.revealLineup(props.lineups[currentLineupIndex.value].id)
  } catch (e) {
    // if this fails, shirts just stay hidden - not worth blocking the game-over flow over
  }
}

function nextLineup() {
  if (currentLineupIndex.value + 1 < props.lineups.length) {
    currentLineupIndex.value += 1
    loadLineup()
  } else {
    emit('gameOver', props.players.map(p => [p.name, scores.value[p.name] || 0]))
  }
}

function progressIdentity() {
  return {
    lineupIds: props.lineups.map(l => l.id),
    playerNames: props.players.map(p => p.name)
  }
}

function identityMatches(saved) {
  const current = progressIdentity()
  return JSON.stringify(saved.lineupIds) === JSON.stringify(current.lineupIds)
      && JSON.stringify(saved.playerNames) === JSON.stringify(current.playerNames)
}

function saveProgress() {
  passAndPlayState.save('starting-xi-battle-progress', {
    ...progressIdentity(),
    currentLineupIndex: currentLineupIndex.value,
    guessedSlotIds: guessedSlotIds.value,
    solvedById: solvedById.value,
    revealedNames: revealedNames.value,
    livesUsed: livesUsed.value,
    eliminatedPlayers: eliminatedPlayers.value,
    currentPlayerIdx: currentPlayerIdx.value,
    scores: scores.value,
    lineupComplete: lineupComplete.value
  })
}

async function initGame() {
  const saved = passAndPlayState.load('starting-xi-battle-progress')
  if (saved && identityMatches(saved)) {
    currentLineupIndex.value = saved.currentLineupIndex
    await loadLineup()
    guessedSlotIds.value = saved.guessedSlotIds
    solvedById.value = saved.solvedById || {}
    revealedNames.value = saved.revealedNames || {}
    livesUsed.value = saved.livesUsed
    eliminatedPlayers.value = saved.eliminatedPlayers
    currentPlayerIdx.value = saved.currentPlayerIdx
    scores.value = saved.scores
    lineupComplete.value = saved.lineupComplete
  } else {
    await loadLineup()
  }
}

watch(
  [guessedSlotIds, solvedById, revealedNames, livesUsed, eliminatedPlayers, currentPlayerIdx, scores, lineupComplete, currentLineupIndex],
  saveProgress,
  { deep: true }
)

onMounted(initGame)
</script>
