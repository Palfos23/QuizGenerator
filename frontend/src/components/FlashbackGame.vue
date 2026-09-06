<template>
  <div>
    <LoadingState v-if="loading" message="Loading the round…" full />

    <div v-else-if="!roundState" class="empty-state">
      <p>Couldn't load this round.</p>
      <button class="btn btn-primary" @click="startRound">Try again</button>
    </div>

    <template v-else>
      <div class="grid-status-bar">
        <div class="grid-progress" style="text-align:center; width:100%;">Round {{ roundIndex + 1 }} / {{ totalRounds }}</div>
      </div>

      <h1 style="text-align:center; margin:6px 0 20px;">Round {{ roundIndex + 1 }}</h1>

      <div class="mp-player-row">
        <div
          v-for="p in activePlayers"
          :key="p.name"
          class="mp-player-card"
          :class="{ 'active-turn': p.name === currentTurnPlayerName && !revealed }"
          :style="{ borderColor: p.color }"
        >
          <strong>{{ p.name }}</strong>
          <div style="font-size:0.8rem; color:var(--text-dim); margin-top:4px;">Score: {{ scores[p.name] || 0 }}</div>
        </div>
      </div>

      <div v-if="!revealed" class="guess-box-wrap no-print">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <div class="guess-box-header">
            <p style="margin:0; color:var(--gold); font-weight:600;">{{ currentTurnPlayerName }}'s turn</p>
          </div>
          <form @submit.prevent="submitCurrentGuess">
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
            That year's already been guessed this round.
          </p>
          <button
            type="button"
            class="btn btn-primary"
            style="margin-top:10px; width:100%;"
            :disabled="guessValue === null || guessValue === ''"
            @click="submitCurrentGuess"
          >Guess</button>
        </div>
      </div>

      <!-- Hints revealed so far, and who's guessed what on each - visible
           throughout the round, not just at the end. -->
      <div class="tension-reveal-list" style="max-width:560px; margin:20px auto 0;">
        <div v-for="(hint, hIdx) in visibleHints" :key="hIdx" class="tension-reveal-row is-revealed">
          <div class="tension-reveal-rank">{{ hIdx + 1 }}</div>
          <div class="tension-reveal-main">
            <div class="tension-reveal-answer" style="font-weight:400; font-style:italic;">{{ hint }}</div>
          </div>
          <div class="tension-reveal-guessers">
            <span v-if="!guessesForHint(hIdx).length" class="tension-reveal-nobody">Nobody guessed</span>
            <span
              v-for="g in guessesForHint(hIdx)"
              :key="g.player"
              class="tension-reveal-chip"
              :class="{ 'flashback-chip-winner': revealed && roundWinners.includes(g.player) }"
              :style="{ borderColor: colorOf(g.player) }"
            >{{ g.player }}: {{ g.year }}</span>
          </div>
        </div>
      </div>

      <div v-if="revealed" class="modal-backdrop">
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
            <div style="font-weight:700; font-size:1.4rem; margin-top:4px;">{{ roundState.year }}</div>
          </div>

          <button class="btn btn-primary" style="margin-top:16px; width:100%;" @click="nextRound">
            {{ roundIndex + 1 < totalRounds ? 'Next round' : 'Finish game' }}
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import passAndPlayState from '../services/passAndPlayState'
import LoadingState from './LoadingState.vue'

const props = defineProps({
  roundCount: { type: Number, required: true },
  players: { type: Array, required: true } // [{ name, color }]
})
const emit = defineEmits(['gameOver'])

const roundIndex = ref(0)
const totalRounds = computed(() => props.roundCount)
const chosenQuestions = ref([]) // years actually played so far, index-aligned with roundIndex - excluded from future picks
const loading = ref(false)
const roundState = ref(null) // the currently-playing year: { id, title, year, hints } - title is admin-only, never shown (see template)

const activePlayers = ref([...props.players]) // no elimination in this game - just here for the shared .mp-player-row markup
const scores = ref(Object.fromEntries(props.players.map(p => [p.name, 0])))

const roundGuesses = ref([]) // [{ player, year, hintIndex }], push order = submission order, cleared each round
const hintIndex = ref(0) // how many hints are visible so far, minus 1
const currentTurnIdx = ref(0) // continuous across the whole round - does NOT reset when a new hint reveals
const revealed = ref(false)
const wasExactMatch = ref(false) // true if roundWinners scored by guessing exactly, false if by closest-guess fallback
const roundWinners = ref([]) // player name(s) who scored this round's point - can be more than one on a tie

const guessValue = ref(null)
const duplicateGuessError = ref(false)
const shakeGuessBox = ref(false)

// Who starts guessing rotates by round, same convention as Tension/Bullseye.
const rotatedActivePlayers = computed(() => {
  const shift = roundIndex.value % activePlayers.value.length
  return [...activePlayers.value.slice(shift), ...activePlayers.value.slice(0, shift)]
})
const currentTurnPlayerName = computed(() => rotatedActivePlayers.value[currentTurnIdx.value]?.name || '')

const visibleHints = computed(() => roundState.value ? roundState.value.hints.slice(0, hintIndex.value + 1) : [])

function guessesForHint(hIdx) {
  return roundGuesses.value.filter(g => g.hintIndex === hIdx)
}

function colorOf(name) {
  return props.players.find(p => p.name === name)?.color || 'var(--border)'
}

const resultLabel = computed(() => {
  const names = roundWinners.value.join(' and ')
  if (wasExactMatch.value) {
    return roundWinners.value.length > 1 ? `🎯 ${names} both nailed it!` : `🎯 ${names} nailed it!`
  }
  if (roundWinners.value.length > 1) return `Nobody got it exactly - ${names} tied closest`
  if (roundWinners.value.length === 1) return `Nobody got it exactly - ${names} was closest`
  return 'The year was'
})

const leaderboardForRound = computed(() =>
  [...props.players]
    .map(p => ({
      name: p.name,
      total: scores.value[p.name] || 0,
      roundDelta: roundWinners.value.includes(p.name) ? 1 : 0
    }))
    .sort((a, b) => b.total - a.total)
)

// A guess is only blocked as a duplicate if it repeats a year from an
// earlier, already-resolved hint - not the hint currently in progress. Until
// everyone's had their turn on THIS hint, nothing's proven wrong yet (see
// advanceTurn), so two players independently guessing the same still-live
// year is a legitimate tie in the making, not a wasted repeat.
//
// Deliberately gives no feedback at all on whether THIS guess was right -
// only advanceTurn(), once everyone's answered, reveals anything. Otherwise
// the first correct guess would tip off every other player still waiting
// their turn on this same hint, which is exactly the outcome the "does
// everyone get a fair shot at this hint" design is trying to avoid.
function submitCurrentGuess() {
  if (guessValue.value === null || guessValue.value === '') return
  const year = Math.trunc(guessValue.value)
  duplicateGuessError.value = roundGuesses.value.some(g => g.year === year && g.hintIndex < hintIndex.value)
  if (duplicateGuessError.value) {
    shakeGuessBox.value = true
    setTimeout(() => { shakeGuessBox.value = false }, 400)
    return
  }

  const player = currentTurnPlayerName.value
  roundGuesses.value.push({ player, year, hintIndex: hintIndex.value })
  guessValue.value = ''
  advanceTurn()
}

// Never resolves mid-hint - every active player gets a guess on the current
// hint first, so an exact match doesn't cut the round short before someone
// else gets a chance to tie it (see the user-facing question this answers).
// An exact match is just the distance-0 case of the same "closest guess"
// resolution the last hint already needed, unified into one pass here -
// any player(s) tied at the minimum distance split the round's point,
// whether that minimum is 0 (nailed it) or not (closest, hints exhausted).
function advanceTurn() {
  const n = rotatedActivePlayers.value.length
  currentTurnIdx.value = currentTurnIdx.value + 1 < n ? currentTurnIdx.value + 1 : 0

  const thisHintGuesses = guessesForHint(hintIndex.value)
  if (thisHintGuesses.length < n) return // still more players to guess on this hint

  const withDistance = thisHintGuesses.map(g => ({ ...g, distance: Math.abs(g.year - roundState.value.year) }))
  const minDistance = Math.min(...withDistance.map(g => g.distance))
  const winners = withDistance.filter(g => g.distance === minDistance).map(g => g.player)

  if (minDistance > 0) {
    const hints = roundState.value.hints
    if (hintIndex.value < hints.length - 1) {
      hintIndex.value += 1
      return
    }
  }

  // Either someone nailed it, or the last hint's just been exhausted with
  // nobody exact - either way the round ends here, split among any tie.
  winners.forEach(name => { scores.value[name] = (scores.value[name] || 0) + 1 })
  roundWinners.value = winners
  wasExactMatch.value = minDistance === 0
  revealed.value = true
}

// Always a random pick, never a player choice - one fetch gets both "is
// there a year left to play" and the year itself in one round-trip, mirroring
// GamePlayEventController's "count" param but always asking for exactly 1.
async function startRound() {
  loading.value = true
  roundState.value = null
  try {
    const picks = await api.fetchFlashbackRoundChoices(1, chosenQuestions.value.map(q => q.id))
    if (!picks.length) {
      toast.show('No more years left to play - ask an admin to add more.', 'error')
      return
    }
    const q = picks[0]
    chosenQuestions.value = [...chosenQuestions.value, q]
    roundState.value = q
    roundGuesses.value = []
    hintIndex.value = 0
    currentTurnIdx.value = 0
    revealed.value = false
    wasExactMatch.value = false
    roundWinners.value = []
    guessValue.value = null
    duplicateGuessError.value = false
  } catch (e) {
    toast.show(e.response?.data?.message || 'Could not load the next round - please try again.', 'error')
  } finally {
    loading.value = false
  }
}

function nextRound() {
  if (roundIndex.value + 1 < props.roundCount) {
    roundIndex.value += 1
    startRound()
  } else {
    emit('gameOver', props.players.map(p => [p.name, scores.value[p.name] || 0]))
  }
}

function progressIdentity() {
  return {
    roundCount: props.roundCount,
    playerNames: props.players.map(p => p.name)
  }
}

function identityMatches(saved) {
  const current = progressIdentity()
  return saved.roundCount === current.roundCount
      && JSON.stringify(saved.playerNames) === JSON.stringify(current.playerNames)
}

function saveProgress() {
  passAndPlayState.save('flashback-progress', {
    ...progressIdentity(),
    roundIndex: roundIndex.value,
    scores: scores.value,
    chosenQuestionIds: chosenQuestions.value.map(q => q.id)
  })
}

function initGame() {
  // Resuming mid-round isn't reconstructed - who's guessed what so far is
  // dropped and the current round just restarts with a fresh random year,
  // same simplification Tension/Bullseye already make for the same reason.
  // Everything from every round before this one - scores, and which years
  // were already used - carries over exactly.
  const saved = passAndPlayState.load('flashback-progress')
  if (saved && identityMatches(saved)) {
    roundIndex.value = saved.roundIndex
    scores.value = saved.scores
    chosenQuestions.value = (saved.chosenQuestionIds || []).map(id => ({ id }))
  }
  startRound()
}

watch([roundIndex, scores, chosenQuestions], saveProgress, { deep: true })

onMounted(initGame)
</script>
