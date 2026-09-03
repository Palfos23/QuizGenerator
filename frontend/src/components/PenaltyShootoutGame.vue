<template>
  <div>
    <LoadingState v-if="loading" message="Loading the shootout…" full />
    <div v-if="error" class="banner error">{{ error }}</div>

    <template v-else-if="shootout">
      <!-- Match info first (everything the admin can set for it - title,
           context, teams/crests/score, match date - mirrors how
           StartingXiPlayView shows a Lineup board's own title+competition+
           scoreline), then lives, then the guess box. See
           PenaltyShootoutBoard's showScoreline prop, switched off below so
           it doesn't render this same header a second time above the kicks. -->
      <div v-if="shootout.title || shootout.teamName || shootout.opponentName" style="text-align:center; margin-bottom:4px;">
        <h1 v-if="shootout.title" style="margin:0 0 6px;">{{ shootout.title }}</h1>
        <p v-if="shootout.competition" class="page-subtitle" style="margin:0;">{{ shootout.competition }}</p>
        <p v-if="matchDateLabel" class="page-subtitle" style="margin-top:2px; font-size:0.82rem;">{{ matchDateLabel }}</p>
      </div>

      <div class="pitch-scoreline" v-if="shootout.teamName || shootout.opponentName">
        <div class="pitch-scoreline-team">
          <img v-if="shootout.teamCrestUrl" :src="shootout.teamCrestUrl" alt="" class="pitch-scoreline-crest" />
          <span>{{ shootout.teamName }}</span>
        </div>
        <div v-if="shootout.teamPensScored != null && shootout.opponentPensScored != null" class="pitch-scoreline-score">
          <span>{{ shootout.teamPensScored }}</span><span class="dash">-</span><span>{{ shootout.opponentPensScored }}</span>
        </div>
        <div v-else class="pitch-scoreline-vs">vs</div>
        <div class="pitch-scoreline-team away">
          <img v-if="shootout.opponentCrestUrl" :src="shootout.opponentCrestUrl" alt="" class="pitch-scoreline-crest" />
          <span>{{ shootout.opponentName }}</span>
        </div>
      </div>

      <!-- Solo play (1 player) skips the player row entirely - there's no one
           else to distinguish "whose turn" from, so the badge/highlight
           treatment would just be visual noise. Pass-and-play (2+) gets the
           same active-turn treatment every other battle mode in the app uses. -->
      <div v-if="players.length > 1" class="mp-player-row">
        <div
          v-for="p in players"
          :key="p.name"
          class="mp-player-card"
          :class="{ 'active-turn': p.name === currentPlayerName && !shootoutComplete, eliminated: eliminatedPlayers.includes(p.name) }"
          :style="{ borderColor: p.color }"
        >
          <strong>{{ p.name }}</strong>
          <LivesHearts :max="shootout.maxStrikes" :used="livesUsed[p.name] || 0" style="margin-top:4px;" />
          <div style="font-size:0.8rem; color:var(--text-dim); margin-top:4px;">Found: {{ scores[p.name] || 0 }}</div>
        </div>
      </div>
      <div v-else class="mp-player-row">
        <div class="mp-player-card" style="border-color:var(--gold);">
          <LivesHearts :max="shootout.maxStrikes" :used="livesUsed[players[0]?.name] || 0" />
          <div style="font-size:0.8rem; color:var(--text-dim); margin-top:4px;">Found: {{ scores[players[0]?.name] || 0 }}</div>
        </div>
      </div>

      <div v-if="!shootoutComplete" class="guess-box-wrap no-print">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <div class="guess-box-header">
            <p style="margin:0; color:var(--gold); font-weight:600;">
              {{ players.length > 1 ? `${currentPlayerName}'s turn` : 'Who took this kick?' }}
            </p>
            <button
              v-if="players.length > 1"
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

      <PenaltyShootoutBoard
        :kicks="shootout.kicks"
        :just-solved-id="justSolvedId"
        :show-scoreline="false"
      />

      <div v-if="shootoutComplete" class="modal-backdrop">
        <div class="completion-popup">
          <h2 style="margin-top:0;">{{ allKicksSolved ? 'Shootout solved!' : 'Shootout over' }}</h2>
          <div class="score-square-grid">
            <div v-for="(p, i) in leaderboard" :key="p.name" class="score-square" :class="{ leader: i === 0 }">
              <div class="score-square-name">{{ p.name }}</div>
              <div class="score-square-number">{{ p.total }}</div>
            </div>
          </div>

          <template v-if="recapKicks.length && recapFoundCount < recapKicks.length">
            <div class="grid-recap-label">
              {{ recapFoundCount }} / {{ recapKicks.length }} found — <span style="color:var(--coral);">red</span> kicks went unfound
            </div>
            <div class="pen-kick-recap">
              <PenaltyShootoutBoard :kicks="recapKicks" :show-scoreline="false" />
            </div>
          </template>

          <button class="btn btn-primary" style="margin-top:16px; width:100%;" @click="finish">
            {{ players.length > 1 ? 'See final result' : 'Done' }}
          </button>
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
import toast from '../services/toast'
import ConfirmModal from './ConfirmModal.vue'
import LivesHearts from './LivesHearts.vue'
import LoadingState from './LoadingState.vue'
import PenaltyShootoutBoard from './PenaltyShootoutBoard.vue'

// Stateless server-side (see PenaltyShootoutPlayService's class comment) -
// this component owns all progress itself, the same way
// MultiplayerGridGame.vue does for its pass-and-play flavor. The one
// simplification relative to Grid/Lineup Battle: a "game" here is always
// exactly one shootout, not 2-4 played back to back - "a specified penalty
// shootout" (singular) is already a complete, satisfying unit of play on its
// own, the same way a single Starting XI board is.
const props = defineProps({
  shootoutId: { type: [Number, String], required: true },
  players: { type: Array, required: true } // [{ name, color }] - length 1 = solo
})
const emit = defineEmits(['gameOver'])

const loading = ref(true)
const error = ref('')
const shootout = ref(null)
const revealedKickIds = ref([])
const livesUsed = ref({})
const eliminatedPlayers = ref([])
const currentPlayerIdx = ref(0)
const scores = ref(Object.fromEntries(props.players.map(p => [p.name, 0])))
const shootoutComplete = ref(false)
const allKicksSolved = ref(false)
const searchTerm = ref('')
const searchResults = ref([])
const guessing = ref(false)
const justSolvedId = ref(null)
const shakeGuessBox = ref(false)
const showSkipConfirm = ref(false)

const resultOverlay = ref(null)
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

// No existing date formatter fits here (formatLastUpdated in constants.js is
// "X hours ago" style, for a board's own updatedAt - matchDate is the
// historical date the real shootout happened, e.g. "9 July 2006") - not
// shown anywhere in Lineup's equivalent screens today, but the request here
// was specifically "all of the match info", so it's included.
const matchDateLabel = computed(() => {
  if (!shootout.value?.matchDate) return ''
  return new Date(shootout.value.matchDate).toLocaleDateString(undefined, { day: 'numeric', month: 'long', year: 'numeric' })
})
const leaderboard = computed(() =>
  [...props.players]
    .map(p => ({ name: p.name, total: scores.value[p.name] || 0 }))
    .sort((a, b) => b.total - a.total)
)

// The board at the buzzer for the results modal - every kick with its real
// answer (revealRemaining() has merged the unfound ones in by the time the
// game is over), so the modal can show which ones nobody actually guessed
// rather than the player having to dismiss it and scroll down to the board.
const recapKicks = computed(() => shootout.value?.kicks || [])
const recapFoundCount = computed(() => recapKicks.value.filter(k => k.guessedByUser).length)

onMounted(loadShootout)

async function loadShootout() {
  loading.value = true
  error.value = ''
  try {
    shootout.value = await api.getPenaltyShootoutStart(props.shootoutId)
    livesUsed.value = Object.fromEntries(props.players.map(p => [p.name, 0]))
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not load this shootout - please try again.'
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
      const results = await api.searchPenaltyShootoutCandidates(props.shootoutId, val)
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
  const player = currentPlayerName.value
  try {
    const result = await api.submitPenaltyShootoutGuess(props.shootoutId, athlete.id, revealedKickIds.value)
    if (result.correct) {
      revealedKickIds.value.push(result.kick.id)
      const idx = shootout.value.kicks.findIndex(k => k.id === result.kick.id)
      if (idx !== -1) shootout.value.kicks[idx] = result.kick
      justSolvedId.value = result.kick.id
      setTimeout(() => { justSolvedId.value = null }, 600)
      scores.value[player] = (scores.value[player] || 0) + 1
      showResultOverlay(true)
      if (result.allSolved) {
        allKicksSolved.value = true
        shootoutComplete.value = true
        return
      }
    } else {
      livesUsed.value[player] = (livesUsed.value[player] || 0) + 1
      shakeGuessBox.value = true
      setTimeout(() => { shakeGuessBox.value = false }, 400)
      showResultOverlay(false)
      if (livesUsed.value[player] >= shootout.value.maxStrikes) {
        eliminatedPlayers.value.push(player)
      }
      if (eliminatedPlayers.value.length >= props.players.length) {
        shootoutComplete.value = true
        await revealRemaining()
        return
      }
    }
    advanceTurn()
  } finally {
    guessing.value = false
  }
}

function confirmSkipTurn() {
  showSkipConfirm.value = false
  skipTurn()
}

async function skipTurn() {
  const player = currentPlayerName.value
  searchTerm.value = ''
  searchResults.value = []
  // Same cost as a wrong guess - passing isn't a free way to stall for time
  // to think without the same strike a bad guess would cost.
  livesUsed.value[player] = (livesUsed.value[player] || 0) + 1
  shakeGuessBox.value = true
  setTimeout(() => { shakeGuessBox.value = false }, 400)
  if (livesUsed.value[player] >= shootout.value.maxStrikes) {
    eliminatedPlayers.value.push(player)
  }
  if (eliminatedPlayers.value.length >= props.players.length) {
    shootoutComplete.value = true
    await revealRemaining()
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
    const revealed = await api.revealAllPenaltyShootoutKicks(props.shootoutId)
    shootout.value.kicks = shootout.value.kicks.map(k => {
      if (k.solved) return k // already correctly guessed - keep as-is
      const match = revealed.find(r => r.id === k.id)
      return match || k
    })
  } catch (e) {
    toast.show('Could not reveal the remaining kicks.', 'error')
  }
}

function finish() {
  emit('gameOver', props.players.map(p => [p.name, scores.value[p.name] || 0]))
}
</script>
