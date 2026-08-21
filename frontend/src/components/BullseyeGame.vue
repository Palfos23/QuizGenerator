<template>
  <div>
    <LoadingState v-if="loadingChoices" message="Loading question choices…" full />

    <div v-else-if="roundChoices.length" class="tension-choice-overlay">
      <div style="color:var(--gold); text-transform:uppercase; letter-spacing:0.5px; font-size:1rem; margin-bottom:6px;">
        Round {{ roundIndex + 1 }} / {{ totalRounds }}
      </div>
      <h2 style="margin:0 0 24px;">{{ rotatedActivePlayers[0]?.name }}, choose a question</h2>
      <div class="tension-choice-grid">
        <button v-for="q in roundChoices" :key="q.id" class="tension-choice-card" @click="chooseQuestion(q)">
          <strong>{{ q.targetValue }} {{ q.statLabel }}</strong>
          <div style="color:var(--text-dim); font-size:0.85rem; margin-top:4px; font-weight:400;">
            {{ sportLabel(q.sport) }} · {{ q.entryCount }} possible answers
          </div>
        </button>
      </div>
    </div>

    <LoadingState v-else-if="loading" message="Loading the question…" full />

    <div v-else-if="!roundState" class="empty-state">
      <p>Couldn't load this round.</p>
      <button class="btn btn-primary" @click="loadRoundChoices">Try again</button>
    </div>

    <template v-else>
      <div class="grid-status-bar">
        <div class="grid-progress" style="text-align:center; width:100%;">Round {{ roundIndex + 1 }} / {{ totalRounds }}</div>
        <div style="color:var(--text-dim); font-size:0.85rem; text-align:center; width:100%;">{{ sportLabel(roundState.sport) }}</div>
      </div>

      <h1 style="text-align:center; margin:6px 0 20px;">{{ roundState.targetValue }} {{ roundState.statLabel }}</h1>

      <div class="mp-player-row">
        <div
          v-for="p in activePlayers"
          :key="p.name"
          class="mp-player-card"
          :class="{ 'active-turn': p.name === currentTurnPlayerName && !revealed, 'bullseye-just-eliminated': justEliminatedName === p.name }"
          :style="{ borderColor: p.color }"
        >
          <strong>{{ p.name }}</strong>
          <div class="tension-player-answer">{{ answerTextFor(p.name) || '— waiting —' }}</div>
        </div>
      </div>

      <div v-if="!revealed" style="text-align:center; margin:8px 0 0;">
        <div v-if="countdown !== null" style="font-size:1.4rem; font-weight:700;">
          Revealing in {{ countdown }}…
        </div>
        <p v-else style="color:var(--gold); font-weight:600;">
          {{ currentTurnPlayerName }}'s turn - pick a name
        </p>
      </div>

      <div v-if="revealed" class="tension-answers-panel" style="margin-top:16px;">
        <h3 style="text-align:center; margin-top:0;">Answers</h3>

        <div v-if="bullseyeAnswers.length" class="bullseye-truth-callout">
          <div class="bullseye-truth-label">{{ bullseyeAnswers[0].distance === 0 ? '🎯 Bullseye' : 'Closest possible answer' }}</div>
          <div class="bullseye-truth-names">
            {{ bullseyeAnswers.map(e => `${e.athleteName} (${e.statValue})`).join(', ') }}
          </div>
        </div>

        <table class="table scoreboard-table">
          <thead>
            <tr>
              <th style="width:8%;">#</th>
              <th style="width:27%;">Player</th>
              <th style="width:30%;">Answer</th>
              <th style="width:15%;">Value</th>
              <th style="width:20%;">Distance</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(a, idx) in rankedAnswers"
              :key="a.player"
              :class="{
                'bullseye-row-revealed': revealIndex > idx,
                'bullseye-row-eliminated': revealIndex > idx && a.player === eliminatedThisRound?.player
              }"
            >
              <td>{{ idx + 1 }}</td>
              <td>{{ revealIndex > idx ? a.player : '???' }}</td>
              <td>{{ revealIndex > idx ? a.name : '???' }}</td>
              <td>{{ revealIndex > idx ? a.statValue : '' }}</td>
              <td>{{ revealIndex > idx ? a.distance : '' }}</td>
            </tr>
          </tbody>
        </table>

        <div style="text-align:center; margin-top:16px;">
          <button v-if="revealIndex < rankedAnswers.length" class="btn btn-secondary" @click="skipReveal">
            Skip reveal
          </button>
          <button v-else-if="readyForNextRound" class="btn btn-primary" @click="nextRound">
            {{ activePlayers.length <= 2 ? 'Finish game' : 'Next round' }}
          </button>
        </div>
      </div>
    </template>

    <BullseyeAnswerModal
      v-if="roundState && !allAnswered && !revealed"
      :current-player="currentTurnPlayerName"
      :target-value="roundState.targetValue"
      :stat-label="roundState.statLabel"
      :entries="roundState.entries"
      :answered-players="roundAnswers.map(a => a.player)"
      :all-players="rotatedActivePlayers.map(p => p.name)"
      :used-names="roundAnswers.map(a => a.name)"
      @submit="submitAnswer"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import passAndPlayState from '../services/passAndPlayState'
import { sportLabel } from '../constants'
import BullseyeAnswerModal from './BullseyeAnswerModal.vue'
import LoadingState from './LoadingState.vue'

const props = defineProps({
  players: { type: Array, required: true } // [{ name, color }]
})
const emit = defineEmits(['gameOver'])

const activePlayers = ref([...props.players])
const roundIndex = ref(0)
const totalRounds = computed(() => props.players.length - 1)
const chosenQuestions = ref([]) // [{id}] used so far this game, for excludeIds
const roundChoices = ref([])
const loadingChoices = ref(false)
const loading = ref(true)
const roundState = ref(null)
const roundAnswers = ref([]) // [{ player, name }] push order = submission order
const currentTurnIdx = ref(0)
const revealed = ref(false)
const revealIndex = ref(0)
const countdown = ref(null)
const eliminationOrder = ref([]) // player names, in elimination order across the whole game
const justEliminatedName = ref(null)
const readyForNextRound = ref(false)

const rotatedActivePlayers = computed(() => {
  const shift = roundIndex.value % activePlayers.value.length
  return [...activePlayers.value.slice(shift), ...activePlayers.value.slice(0, shift)]
})

const allAnswered = computed(() => roundAnswers.value.length === rotatedActivePlayers.value.length)
const currentTurnPlayerName = computed(() => rotatedActivePlayers.value[currentTurnIdx.value]?.name || '')

function answerTextFor(name) {
  return roundAnswers.value.find(a => a.player === name)?.name
}

function submitAnswer(name) {
  roundAnswers.value.push({ player: currentTurnPlayerName.value, name })
  currentTurnIdx.value = currentTurnIdx.value + 1 < rotatedActivePlayers.value.length ? currentTurnIdx.value + 1 : 0
}

// Case-insensitive exact-name match against the round's authored answer key -
// an unmatched name (including anything typed that isn't in the pool at all,
// or a subject only present because the question is in "auto pool" mode, with
// no authored value of their own) resolves to a stat value of 0, same as the
// rest of the app's free-text guess handling. Closest-first so the reveal
// builds toward who's eliminated.
const rankedAnswers = computed(() => {
  if (!roundState.value) return []
  return roundAnswers.value.map((a, i) => {
    const matched = roundState.value.entries.find(e => e.athleteName.toLowerCase() === a.name.trim().toLowerCase())
    const statValue = matched && matched.statValue !== null ? matched.statValue : 0
    return { ...a, submissionIndex: i, statValue, distance: Math.abs(statValue - roundState.value.targetValue) }
  }).sort((x, y) => x.distance - y.distance || x.submissionIndex - y.submissionIndex)
})

// Farthest off is eliminated; ties broken by whoever answered earliest this
// round - keeps exactly one elimination per round.
const eliminatedThisRound = computed(() => {
  if (!rankedAnswers.value.length) return null
  return [...rankedAnswers.value].sort((a, b) => b.distance - a.distance || a.submissionIndex - b.submissionIndex)[0]
})

// The actual best possible answer(s) in the whole pool - not just among what
// players guessed - so the reveal shows what the "bullseye" really was, not
// only how close each player's own pick landed. Ties (e.g. two athletes both
// on exactly the target) are all shown together.
const bullseyeAnswers = computed(() => {
  if (!roundState.value) return []
  const valued = roundState.value.entries.filter(e => e.statValue !== null)
  if (!valued.length) return []
  const withDistance = valued.map(e => ({ ...e, distance: Math.abs(e.statValue - roundState.value.targetValue) }))
  const minDistance = Math.min(...withDistance.map(e => e.distance))
  return withDistance.filter(e => e.distance === minDistance)
})

let countdownTimer = null
watch([roundAnswers, revealed], () => {
  if (allAnswered.value && !revealed.value) {
    countdown.value = 3
    clearInterval(countdownTimer)
    countdownTimer = setInterval(() => {
      if (countdown.value > 1) {
        countdown.value -= 1
      } else {
        clearInterval(countdownTimer)
        countdown.value = null
        reveal()
      }
    }, 1000)
  }
}, { deep: true })

function reveal() {
  revealed.value = true
  revealIndex.value = 0
  scheduleReveal()
}

let revealTimer = null
function scheduleReveal() {
  clearTimeout(revealTimer)
  if (revealIndex.value < rankedAnswers.value.length) {
    revealTimer = setTimeout(() => {
      revealIndex.value += 1
      if (revealIndex.value >= rankedAnswers.value.length) {
        finishReveal()
      } else {
        scheduleReveal()
      }
    }, 1300)
  } else {
    finishReveal()
  }
}

function skipReveal() {
  clearTimeout(revealTimer)
  revealIndex.value = rankedAnswers.value.length
  finishReveal()
}

function finishReveal() {
  justEliminatedName.value = eliminatedThisRound.value?.player || null
  readyForNextRound.value = true
}

function buildFinalScores() {
  const n = props.players.length
  return props.players.map(p => {
    const round = eliminationOrder.value.indexOf(p.name) + 1 // 0 = never eliminated = winner
    return [p.name, round === 0 ? n : n - round]
  })
}

function nextRound() {
  const eliminatedName = eliminatedThisRound.value?.player
  if (eliminatedName) {
    eliminationOrder.value = [...eliminationOrder.value, eliminatedName]
    activePlayers.value = activePlayers.value.filter(p => p.name !== eliminatedName)
  }
  justEliminatedName.value = null

  if (activePlayers.value.length <= 1) {
    emit('gameOver', buildFinalScores())
    return
  }

  roundIndex.value += 1
  roundAnswers.value = []
  revealed.value = false
  revealIndex.value = 0
  currentTurnIdx.value = 0
  readyForNextRound.value = false
  roundState.value = null
  loadRoundChoices()
}

async function loadRoundChoices() {
  loadingChoices.value = true
  try {
    roundChoices.value = await api.fetchBullseyeBattleRoundChoices(3, chosenQuestions.value.map(q => q.id))
  } catch (e) {
    toast.show(e.response?.data?.message || 'Could not load the next round - please try again.', 'error')
  } finally {
    loadingChoices.value = false
  }
}

function chooseQuestion(q) {
  chosenQuestions.value = [...chosenQuestions.value, q]
  roundChoices.value = []
  loadRoundState(q.id)
}

async function loadRoundState(id) {
  loading.value = true
  try {
    roundState.value = await api.getMultiplayerBullseyeStart(id)
  } catch (e) {
    toast.show(e.response?.data?.message || 'Could not load this question - please try again.', 'error')
  } finally {
    loading.value = false
  }
}

function progressIdentity() {
  return { playerNames: props.players.map(p => p.name) }
}

function identityMatches(saved) {
  return JSON.stringify(saved.playerNames) === JSON.stringify(progressIdentity().playerNames)
}

function saveProgress() {
  passAndPlayState.save('bullseye-progress', {
    ...progressIdentity(),
    activePlayerNames: activePlayers.value.map(p => p.name),
    roundIndex: roundIndex.value,
    eliminationOrder: eliminationOrder.value,
    chosenQuestionIds: chosenQuestions.value.map(q => q.id)
  })
}

function initGame() {
  // Resuming mid-round isn't reconstructed - who's answered what so far is
  // dropped and the current round just restarts with a fresh choice-of-3,
  // same simplification TensionGame already makes for the same reason.
  // Everything from every round before this one - who's been eliminated,
  // and which questions were already used - carries over exactly.
  const saved = passAndPlayState.load('bullseye-progress')
  if (saved && identityMatches(saved)) {
    activePlayers.value = saved.activePlayerNames.map(name => props.players.find(p => p.name === name)).filter(Boolean)
    roundIndex.value = saved.roundIndex
    eliminationOrder.value = saved.eliminationOrder || []
    chosenQuestions.value = (saved.chosenQuestionIds || []).map(id => ({ id }))
  }
  loadRoundChoices()
}

watch([roundIndex, activePlayers, eliminationOrder, chosenQuestions], saveProgress, { deep: true })

onMounted(initGame)
</script>
