<template>
  <div>
    <div class="grid-status-bar">
      <div class="grid-progress">Question {{ currentQuestionIndex + 1 }} / {{ questions.length }}</div>
      <div style="color:var(--text-dim); font-size:0.85rem;">Tension answers: {{ question.tensionAnswers.length }}</div>
    </div>

    <h1 style="text-align:center; margin:6px 0 24px;">{{ question.title }}</h1>

    <div class="tension-layout">
      <div class="tension-player-col">
        <div
          v-for="player in leftPlayers"
          :key="player"
          class="tension-player-card"
          :style="{ borderColor: colorOf(player) }"
        >
          <div>
            <strong>{{ player }}</strong>
            <div class="tension-player-answer">{{ answerTextFor(player) || '— waiting —' }}</div>
          </div>
          <div style="text-align:right;">
            <div
              v-if="showRoundScoreFor(player)"
              class="tension-round-score"
              :class="{ positive: (scoreFor(player) ?? 0) > 0, negative: (scoreFor(player) ?? 0) < 0 }"
            >{{ formatScore(scoreFor(player)) }}</div>
            <div style="font-size:0.8rem; color:var(--text-dim);">Total: {{ scores[player] }}</div>
          </div>
        </div>
      </div>

      <div class="tension-answers-panel">
        <h3 style="text-align:center; margin-top:0;">Answers</h3>
        <table class="table">
          <thead>
            <tr>
              <th>#</th>
              <th>Answer</th>
              <th>Player</th>
              <th>Score</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(ans, idx) in allAnswersList" :key="ans.text" :class="{ 'tension-row-revealed': revealIndex > idx, 'tension-row-trap': revealIndex > idx && ans.tension }">
              <td>{{ ans.rank }}</td>
              <td>{{ revealIndex > idx ? ans.text : '???' }}</td>
              <td>{{ revealIndex > idx ? guessedByFor(ans).join(', ') : '' }}</td>
              <td>{{ revealIndex > idx ? guessedScoresFor(ans) : '' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="tension-player-col">
        <div
          v-for="player in rightPlayers"
          :key="player"
          class="tension-player-card"
          :style="{ borderColor: colorOf(player) }"
        >
          <div>
            <strong>{{ player }}</strong>
            <div class="tension-player-answer">{{ answerTextFor(player) || '— waiting —' }}</div>
          </div>
          <div style="text-align:right;">
            <div
              v-if="showRoundScoreFor(player)"
              class="tension-round-score"
              :class="{ positive: (scoreFor(player) ?? 0) > 0, negative: (scoreFor(player) ?? 0) < 0 }"
            >{{ formatScore(scoreFor(player)) }}</div>
            <div style="font-size:0.8rem; color:var(--text-dim);">Total: {{ scores[player] }}</div>
          </div>
        </div>
      </div>
    </div>

    <div style="text-align:center; margin-top:24px;">
      <div v-if="countdown !== null && !revealed" style="font-size:1.4rem; font-weight:700;">
        Answers in {{ countdown }}…
      </div>

      <button v-if="revealed && revealIndex < allAnswersList.length" class="btn btn-secondary" @click="revealIndex = allAnswersList.length">
        Skip reveal
      </button>

      <button v-if="revealed && revealIndex >= allAnswersList.length" class="btn btn-primary" @click="nextQuestion">
        {{ currentQuestionIndex + 1 < questions.length ? 'Next question' : 'Finish game' }}
      </button>
    </div>

    <div v-if="showIntro" class="tension-intro-overlay">
      <div style="color:var(--gold); text-transform:uppercase; letter-spacing:0.5px; font-size:1rem; margin-bottom:10px;">
        Question {{ currentQuestionIndex + 1 }} / {{ questions.length }}
      </div>
      <h1 style="max-width:80%; margin:0 auto;">{{ question.title }}</h1>
      <p style="margin-top:20px; font-size:1.1rem;">First up: <strong>{{ rotatedPlayers[0] }}</strong></p>
      <p style="margin-top:20px; color:var(--text-dim);">Starting in {{ introCountdown }}s…</p>
    </div>

    <TensionAnswerModal
      v-if="!revealed && !allAnswered && !showIntro"
      :current-player="rotatedPlayers[currentPlayerIdx]"
      :question-title="question.title"
      :tension-count="question.tensionAnswers.length"
      :category="question.answersCategory"
      :answered-players="roundAnswers.map(a => a.player)"
      :all-players="rotatedPlayers"
      :used-answers="roundAnswers.map(a => a.answer)"
      @submit="submitAnswer"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import TensionAnswerModal from './TensionAnswerModal.vue'

const props = defineProps({
  questions: { type: Array, required: true },
  players: { type: Array, required: true } // [{ name, color }]
})
const emit = defineEmits(['gameOver'])

const currentQuestionIndex = ref(0)
const roundAnswers = ref([]) // [{ player, answer, score?, rank?, tension? }]
const currentPlayerIdx = ref(0)
const revealed = ref(false)
const scores = ref(Object.fromEntries(props.players.map(p => [p.name, 0])))
const pendingScores = ref({})
const revealIndex = ref(0)
const countdown = ref(null)
const showIntro = ref(true)
const introCountdown = ref(4)

const playerNames = props.players.map(p => p.name)
const question = computed(() => props.questions[currentQuestionIndex.value])

function colorOf(name) {
  return props.players.find(p => p.name === name)?.color || 'var(--border)'
}

const rotatedPlayers = computed(() => {
  const shift = currentQuestionIndex.value % playerNames.length
  return [...playerNames.slice(shift), ...playerNames.slice(0, shift)]
})

const leftPlayers = computed(() => playerNames.slice(0, Math.ceil(playerNames.length / 2)))
const rightPlayers = computed(() => playerNames.slice(Math.ceil(playerNames.length / 2)))

// Combined reveal order: all safe answers (rank ascending) first, then all tension
// answers (rank ascending) - matches the original's reveal sequencing.
const allAnswersList = computed(() => [
  ...question.value.safeAnswers.map(a => ({ text: a.text, rank: a.rank, tension: false })),
  ...question.value.tensionAnswers.map(a => ({ text: a.text, rank: a.rank, tension: true }))
])

const allAnswered = computed(() => roundAnswers.value.length === rotatedPlayers.value.length)

function answerTextFor(player) {
  return roundAnswers.value.find(a => a.player === player)?.answer
}
function scoreFor(player) {
  return roundAnswers.value.find(a => a.player === player)?.score
}
function showRoundScoreFor(player) {
  const pa = roundAnswers.value.find(a => a.player === player)
  if (!revealed.value || !pa) return false
  const answerInList = pa.rank !== undefined
  if (!answerInList) return revealIndex.value >= allAnswersList.value.length
  return allAnswersList.value.slice(0, revealIndex.value).some(a => a.text.toLowerCase() === pa.answer.toLowerCase())
}
function formatScore(score) {
  if (score === undefined) return ''
  return score > 0 ? `+${score}` : String(score)
}
function guessedByFor(ans) {
  return roundAnswers.value.filter(a => a.answer.toLowerCase() === ans.text.toLowerCase()).map(a => a.player)
}
function guessedScoresFor(ans) {
  return guessedByFor(ans)
    .map(name => roundAnswers.value.find(a => a.player === name))
    .map(a => formatScore(a?.score))
    .join(', ')
}

function submitAnswer(answerText) {
  roundAnswers.value.push({ player: rotatedPlayers.value[currentPlayerIdx.value], answer: answerText })
  currentPlayerIdx.value = currentPlayerIdx.value + 1 < rotatedPlayers.value.length ? currentPlayerIdx.value + 1 : 0
}

let countdownTimer = null
watch([roundAnswers, revealed], () => {
  if (roundAnswers.value.length === rotatedPlayers.value.length && !revealed.value) {
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
  const scored = roundAnswers.value.map(a => {
    const found = allAnswersList.value.find(ans => ans.text.toLowerCase() === a.answer.toLowerCase())
    let score = -3
    let rank
    if (found) {
      rank = found.rank
      score = found.tension ? -5 : rank
    }
    return { ...a, score, rank }
  })
  const totals = { ...scores.value }
  scored.forEach(a => { totals[a.player] += a.score })
  pendingScores.value = totals
  roundAnswers.value = scored
  revealed.value = true
  revealIndex.value = 0
}

let revealTimer = null
watch(revealIndex, () => {
  clearTimeout(revealTimer)
  if (!revealed.value) return
  if (revealIndex.value < allAnswersList.value.length) {
    revealTimer = setTimeout(() => { revealIndex.value += 1 }, 1100)
  } else {
    scores.value = pendingScores.value
  }
})
watch(revealed, (val) => {
  if (val) revealIndex.value = 0
})

function startIntro() {
  showIntro.value = true
  introCountdown.value = 4
  const timer = setInterval(() => {
    if (introCountdown.value > 1) {
      introCountdown.value -= 1
    } else {
      clearInterval(timer)
      showIntro.value = false
    }
  }, 1000)
}

onMounted(startIntro)
watch(currentQuestionIndex, startIntro)

function nextQuestion() {
  if (currentQuestionIndex.value + 1 < props.questions.length) {
    currentQuestionIndex.value += 1
    roundAnswers.value = []
    revealed.value = false
    currentPlayerIdx.value = 0
    revealIndex.value = 0
    countdown.value = null
  } else {
    emit('gameOver', scores.value)
  }
}
</script>
