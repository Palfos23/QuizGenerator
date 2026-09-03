<template>
  <div>
    <LoadingState v-if="loadingChoices" message="Loading board choices…" full />

    <div v-else-if="roundChoices.length" class="tension-choice-overlay">
      <div style="color:var(--gold); text-transform:uppercase; letter-spacing:0.5px; font-size:1rem; margin-bottom:6px;">
        Board {{ currentLineupIndex + 1 }} / {{ totalLineups }}
      </div>
      <h2 style="margin:0 0 24px;"><strong style="color:var(--gold);">{{ pickerName }}</strong>, choose a board</h2>
      <div class="tension-choice-grid">
        <button v-for="l in roundChoices" :key="l.id" class="tension-choice-card" @click="chooseLineup(l)">
          <strong>{{ l.title }}</strong>
          <div style="color:var(--text-dim); font-size:0.85rem; margin-top:4px; font-weight:400;">{{ l.teamName }} vs {{ l.opponentName }} · {{ l.formation }}</div>
        </button>
      </div>
    </div>

    <template v-else>
    <div class="grid-status-bar">
      <div class="grid-progress" style="text-align:center; width:100%;">
        Board {{ currentLineupIndex + 1 }} / {{ totalLineups }}: {{ lineupState?.title }}
      </div>
      <div v-if="lastUpdatedLabel" style="color:var(--text-dim); font-size:0.75rem; text-align:center; width:100%;">{{ lastUpdatedLabel }}</div>
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

    <LoadingState v-if="loading" message="Loading board…" full />

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
          <LivesHearts :max="lineupState.maxStrikes" :used="livesUsed[p.name] || 0" style="margin-top:4px;" />
          <div style="font-size:0.8rem; color:var(--text-dim); margin-top:4px;">Total: {{ scores[p.name] || 0 }}</div>
        </div>
      </div>

      <div v-if="!lineupComplete" class="guess-box-wrap no-print">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <div class="guess-box-header">
            <p style="margin:0; color:var(--gold); font-weight:600;">{{ currentPlayerName }}'s turn</p>
            <button
              type="button"
              class="btn btn-danger btn-sm no-print"
              :disabled="guessing"
              @click="showSkipConfirm = true"
            >Pass turn</button>
          </div>
          <input
            type="text"
            v-model="searchTerm"
            placeholder="Search for a player…"
            aria-label="Search for a player"
            autocomplete="off"
            autocorrect="off"
            autocapitalize="off"
            spellcheck="false"
            @keydown.esc="searchTerm = ''"
          />
          <div v-if="searchResults.length" class="guess-results">
            <button v-for="a in searchResults" :key="a.id" class="guess-result-row" :disabled="guessing" @click="submitGuess(a)">
              {{ a.name }}
            </button>
          </div>
        </div>
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

          <template v-if="recapSlots.length && recapFoundCount < recapSlots.length">
            <div class="grid-recap-label">
              {{ recapFoundCount }} / {{ recapSlots.length }} found — <span style="color:var(--coral);">red</span> shirts went unguessed
            </div>
            <PitchRecap
              :rows="recapRows"
              :kit-color="lineupState.kitColor || DEFAULT_KIT_COLOR"
              :goalkeeper-kit-color="lineupState.goalkeeperKitColor || DEFAULT_GK_KIT_COLOR"
            />
          </template>

          <button class="btn btn-primary" style="margin-top:12px; width:100%;" @click="nextLineup">
            {{ currentLineupIndex + 1 < totalLineups ? 'Next board' : 'Finish game' }}
          </button>
        </div>
      </div>

      <div class="pitch">
        <PitchMarkings />
        <div v-for="(row, ri) in rows" :key="ri" class="pitch-row" :class="`pitch-row--${row.kind}`">
          <div v-for="slot in row.items" :key="slot.id ?? slot.slotIndex" class="pitch-slot">
            <div
              class="pitch-shirt"
              :class="{ solved: slot.guessedByUser, 'revealed-only': slot.solved && !slot.guessedByUser, goalkeeper: slot.slotIndex === 0 }"
              :style="shirtStyle(slot)"
            >
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

    <div v-else class="empty-state">
      <p>Couldn't load this board.</p>
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
import { displayRowsFor } from '../services/formations'
import { readableTextColor, formatLastUpdated } from '../constants'
import PitchMarkings from './PitchMarkings.vue'
import PitchRecap from './PitchRecap.vue'
import ConfirmModal from './ConfirmModal.vue'
import LivesHearts from './LivesHearts.vue'
import LoadingState from './LoadingState.vue'

const DEFAULT_KIT_COLOR = '#d92332'
const DEFAULT_GK_KIT_COLOR = '#f2c230'

const props = defineProps({
  mode: { type: String, default: 'manual' }, // 'manual' | 'random'
  lineups: { type: Array, default: () => [] }, // [{ id, title, ... }] - fixed list, mode === 'manual' only
  numLineups: { type: Number, default: 0 }, // total rounds - mode === 'random' only
  players: { type: Array, required: true } // [{ name, color }]
})
const emit = defineEmits(['gameOver'])

const totalLineups = computed(() => props.mode === 'random' ? props.numLineups : props.lineups.length)
// This round's starting player rotates by seat, same convention as Tension's
// rotatedPlayers[0] - and since they're also the one who picks in "random"
// mode, this doubles as the picker's name before a board is even chosen.
const pickerName = computed(() => props.players[currentLineupIndex.value % props.players.length]?.name)
const currentLineupId = computed(() => props.mode === 'manual'
  ? props.lineups[currentLineupIndex.value]?.id
  : chosenLineups.value[currentLineupIndex.value]?.id)

const currentLineupIndex = ref(0)
const chosenLineups = ref([]) // boards actually picked so far ('random' mode only), index-aligned with currentLineupIndex
const roundChoices = ref([]) // this round's 3 candidate boards, before a pick is made ('random' mode only)
const loadingChoices = ref(false)
const lineupState = ref(null)
const lastUpdatedLabel = computed(() => formatLastUpdated(lineupState.value?.updatedAt))
const loading = ref(true)
const guessedSlotIds = ref([])
const solvedById = ref({}) // slot id -> solved LineupSlotDto from the server
const revealedSlots = ref({}) // slot id -> fully-revealed LineupSlotDto (name + photo), once the board's given up on (everyone eliminated)
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
    if (revealedSlots.value[s.id]) return { ...s, ...revealedSlots.value[s.id], solved: true }
    return s
  })
  return displayRowsFor(lineupState.value.formation, merged)
})

// Board recap for the results modal: every slot revealed, flagged by whether a
// player actually guessed it. Only shown once revealRemaining() has run - i.e.
// the board ended with the XI incomplete.
const recapRows = computed(() => {
  if (!lineupState.value || !Object.keys(revealedSlots.value).length) return []
  const merged = lineupState.value.slots.map(s => ({
    ...s,
    ...(revealedSlots.value[s.id] || {}),
    wasFound: guessedSlotIds.value.includes(s.id)
  }))
  return displayRowsFor(lineupState.value.formation, merged)
})
const recapSlots = computed(() => recapRows.value.flatMap(r => r.items))
const recapFoundCount = computed(() => recapSlots.value.filter(s => s.wasFound).length)

function shirtStyle(slot) {
  const color = slot.slotIndex === 0
    ? (lineupState.value?.goalkeeperKitColor || DEFAULT_GK_KIT_COLOR)
    : (lineupState.value?.kitColor || DEFAULT_KIT_COLOR)
  return { '--kit-color': color, '--kit-text': readableTextColor(color) }
}

async function loadLineup(lineupId) {
  loading.value = true
  guessedSlotIds.value = []
  solvedById.value = {}
  revealedSlots.value = {}
  eliminatedPlayers.value = []
  lineupComplete.value = false
  scoresAtLineupStart.value = { ...scores.value }
  currentPlayerIdx.value = currentLineupIndex.value % props.players.length // rotate who starts, like Grid Battle
  try {
    lineupState.value = await api.getMultiplayerLineupStart(lineupId)
    livesUsed.value = Object.fromEntries(props.players.map(p => [p.name, 0]))
  } catch (e) {
    toast.show(e.response?.data?.message || 'Could not load this board - please try again.', 'error')
  } finally {
    loading.value = false
  }
}

async function loadRoundChoices() {
  loadingChoices.value = true
  try {
    roundChoices.value = await api.fetchLineupBattleRoundChoices(3, chosenLineups.value.map(l => l.id))
  } catch (e) {
    toast.show(e.response?.data?.message || 'Could not load the next round - please try again.', 'error')
  } finally {
    loadingChoices.value = false
  }
}

function chooseLineup(l) {
  chosenLineups.value = [...chosenLineups.value, l]
  roundChoices.value = []
  loadLineup(l.id)
}

// Manual mode already knows every board up front. Random mode picks one round
// at a time - resume straight into an already-chosen round rather than
// re-offering a choice, since a choice already made is a commitment; otherwise
// offer this round's 3 choices.
function proceedToCurrentRound() {
  if (props.mode === 'manual') {
    return loadLineup(props.lineups[currentLineupIndex.value].id)
  } else if (chosenLineups.value.length > currentLineupIndex.value) {
    return loadLineup(chosenLineups.value[currentLineupIndex.value].id)
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
      const results = await api.searchLineupCandidates(currentLineupId.value, val)
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
    const result = await api.submitMultiplayerLineupGuess(
      currentLineupId.value, athlete.id, guessedSlotIds.value
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
    const all = await api.revealAllLineupSlots(currentLineupId.value)
    revealedSlots.value = Object.fromEntries(all.map(s => [s.id, s]))
  } catch (e) {
    // if this fails, shirts just stay hidden - not worth blocking the game-over flow over
  }
}

function nextLineup() {
  if (currentLineupIndex.value + 1 < totalLineups.value) {
    currentLineupIndex.value += 1
  } else {
    emit('gameOver', props.players.map(p => [p.name, scores.value[p.name] || 0]))
  }
}

function progressIdentity() {
  return {
    mode: props.mode,
    lineupIds: props.mode === 'manual' ? props.lineups.map(l => l.id) : null,
    numLineups: props.mode === 'random' ? props.numLineups : null,
    playerNames: props.players.map(p => p.name)
  }
}

function identityMatches(saved) {
  const current = progressIdentity()
  return saved.mode === current.mode
      && JSON.stringify(saved.lineupIds) === JSON.stringify(current.lineupIds)
      && saved.numLineups === current.numLineups
      && JSON.stringify(saved.playerNames) === JSON.stringify(current.playerNames)
}

function saveProgress() {
  passAndPlayState.save('starting-xi-battle-progress', {
    ...progressIdentity(),
    currentLineupIndex: currentLineupIndex.value,
    guessedSlotIds: guessedSlotIds.value,
    solvedById: solvedById.value,
    revealedSlots: revealedSlots.value,
    livesUsed: livesUsed.value,
    eliminatedPlayers: eliminatedPlayers.value,
    currentPlayerIdx: currentPlayerIdx.value,
    scores: scores.value,
    lineupComplete: lineupComplete.value,
    // 'random' mode only - the boards actually picked so far, so a resume
    // doesn't have to re-offer (and potentially re-roll) a choice already made.
    chosenLineups: chosenLineups.value
  })
}

async function initGame() {
  const saved = passAndPlayState.load('starting-xi-battle-progress')
  if (saved && identityMatches(saved)) {
    currentLineupIndex.value = saved.currentLineupIndex
    chosenLineups.value = saved.chosenLineups || []
    await proceedToCurrentRound()
    guessedSlotIds.value = saved.guessedSlotIds
    solvedById.value = saved.solvedById || {}
    revealedSlots.value = saved.revealedSlots || {}
    livesUsed.value = saved.livesUsed
    eliminatedPlayers.value = saved.eliminatedPlayers
    currentPlayerIdx.value = saved.currentPlayerIdx
    scores.value = saved.scores
    lineupComplete.value = saved.lineupComplete
  } else {
    await proceedToCurrentRound()
  }
}

watch(
  [guessedSlotIds, solvedById, revealedSlots, livesUsed, eliminatedPlayers, currentPlayerIdx, scores, lineupComplete, currentLineupIndex, chosenLineups],
  saveProgress,
  { deep: true }
)

onMounted(initGame)
watch(currentLineupIndex, proceedToCurrentRound)

// Safety net for the results-modal pitch recap: if the board is finished but the
// full reveal never landed (e.g. restored into a completed round), fetch it now.
watch(lineupComplete, (done) => {
  if (done && lineupState.value?.slots.some(s => !guessedSlotIds.value.includes(s.id))
      && !Object.keys(revealedSlots.value).length) {
    revealRemaining()
  }
})
</script>
