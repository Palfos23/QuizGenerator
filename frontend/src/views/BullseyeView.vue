<template>
  <GameAccessGate game="bullseye">
  <div>
    <template v-if="stage === 'landing'">
      <h1>Bullseye</h1>
      <p class="page-subtitle">
        Each round shows a target number, like <em>"13 goals in the Premier League 2024/25"</em>.
        Take turns naming a player you think is close to it - whoever's answer is
        farthest off is eliminated. Play continues until one player remains.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="savedPassAndPlay" class="banner" style="background:rgba(242,183,5,0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>You have a game in progress.</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" @click="resumePassAndPlay">Resume</button>
          <button class="btn btn-secondary btn-sm" @click="dismissPassAndPlay">Dismiss</button>
        </div>
      </div>

      <BullseyePreview />

      <div class="field">
        <label>Players</label>
        <select v-model.number="numPlayers">
          <option v-for="n in [2,3,4,5,6,7,8]" :key="n" :value="n">{{ n }}</option>
        </select>
      </div>
      <p class="page-subtitle" style="margin-top:-8px;">
        {{ numPlayers }} players means {{ numPlayers - 1 }} round{{ numPlayers - 1 > 1 ? 's' : '' }} - one elimination per round.
      </p>

      <button class="btn btn-primary" @click="goToSetup">Create game</button>
    </template>

    <template v-else-if="stage === 'setup'">
      <h1>Who's playing?</h1>
      <p class="page-subtitle" v-if="duplicateNames">Two players can't have the same name.</p>

      <div v-for="(p, i) in setupPlayers" :key="i" class="field">
        <input type="text" v-model="p.name" :placeholder="`Player ${i + 1}`" />
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'landing'">← Back</button>
        <button class="btn btn-primary" :disabled="!allNamed || duplicateNames || checkingPool" @click="goToGame">
          Next →
        </button>
      </div>
      <LoadingState v-if="checkingPool" full message="Shuffling your questions…" subtitle="First round starts in just a moment." />
    </template>

    <BullseyeGame
      v-else-if="stage === 'game'"
      :players="setupPlayers"
      @game-over="onGameOver"
    />

    <template v-else-if="stage === 'done'">
      <div class="modal-backdrop">
        <div class="completion-popup podium-host">
          <h2 style="margin-top:0;">Game over!</h2>

          <div class="podium-row">
            <div v-for="(entry, i) in sortedScores.slice(0, 3)" :key="entry[0]" class="podium-block" :class="`rank-${i + 1}`">
              <div class="podium-rank-number">{{ i + 1 }}</div>
              <div class="podium-name">{{ entry[0] }}</div>
              <div class="podium-score">{{ entry[1] }}</div>
            </div>
          </div>

          <div v-if="sortedScores.length > 3" style="max-width:420px; margin:0 auto;">
            <div v-for="(entry, i) in sortedScores.slice(3)" :key="entry[0]" class="podium-rest-row">
              <span>{{ i + 4 }}. {{ entry[0] }}</span>
              <span style="color:var(--text-dim);">{{ entry[1] }}</span>
            </div>
          </div>

          <button class="btn btn-primary" style="margin-top:16px; width:100%;" @click="resetGame">Play again</button>
        </div>
      </div>
    </template>
  </div>
  </GameAccessGate>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import passAndPlayState from '../services/passAndPlayState'
import navTrigger from '../services/navTrigger'
import BullseyeGame from '../components/BullseyeGame.vue'
import LoadingState from '../components/LoadingState.vue'
import GameAccessGate from '../components/GameAccessGate.vue'
import BullseyePreview from '../components/previews/BullseyePreview.vue'

const colorOptions = [
  { hex: '#4f46e5', name: 'Indigo' },
  { hex: '#7C7CFC', name: 'Light indigo' },
  { hex: '#F22C05', name: 'Red' },
  { hex: '#F2BB05', name: 'Yellow' },
  { hex: '#032E8A', name: 'Blue' },
  { hex: '#05D6F2', name: 'Light blue' },
  { hex: '#f43f5e', name: 'Pink' },
  { hex: '#5D038A', name: 'Purple' }
]

const stage = ref('landing')
const error = ref('')
const numPlayers = ref(2)
const setupPlayers = reactive([])
const finalScores = ref([])
const checkingPool = ref(false)

const savedPassAndPlay = ref(null)
onMounted(() => {
  savedPassAndPlay.value = passAndPlayState.load('bullseye')
})

function resumePassAndPlay() {
  const saved = savedPassAndPlay.value
  setupPlayers.length = 0
  saved.players.forEach(p => setupPlayers.push(p))
  stage.value = 'game'
}

function dismissPassAndPlay() {
  passAndPlayState.clear('bullseye')
  passAndPlayState.clear('bullseye-progress')
  savedPassAndPlay.value = null
}

function rebuildSetupPlayers() {
  setupPlayers.length = 0
  const shuffled = [...colorOptions].sort(() => Math.random() - 0.5)
  for (let i = 0; i < numPlayers.value; i++) {
    setupPlayers.push({ name: '', color: shuffled[i % shuffled.length].hex })
  }
}

function goToSetup() {
  rebuildSetupPlayers()
  stage.value = 'setup'
}

const allNamed = computed(() => setupPlayers.every(p => p.name.trim().length > 0))
const duplicateNames = computed(() => {
  const names = setupPlayers.map(p => p.name.trim().toLowerCase()).filter(n => n.length > 0)
  return names.some((n, i) => names.indexOf(n) !== i)
})

async function goToGame() {
  error.value = ''
  const roundsNeeded = setupPlayers.length - 1
  let poolSize = 0
  let accessError = ''
  checkingPool.value = true
  try {
    poolSize = (await api.getBattleEligibleBullseyeQuestions()).length
  } catch (e) {
    accessError = e.response?.data?.message || ''
  } finally {
    checkingPool.value = false
  }
  if (accessError) {
    error.value = accessError
    stage.value = 'landing'
    return
  }
  if (poolSize < roundsNeeded) {
    error.value = `Only found ${poolSize} question(s) - need at least ${roundsNeeded} for ${setupPlayers.length} players. Ask an admin to add more Bullseye questions.`
    stage.value = 'landing'
    return
  }
  passAndPlayState.save('bullseye', { players: [...setupPlayers] })
  savedPassAndPlay.value = passAndPlayState.load('bullseye')
  stage.value = 'game'
}

const sortedScores = computed(() => [...finalScores.value].sort((a, b) => b[1] - a[1]))

function onGameOver(scores) {
  passAndPlayState.clear('bullseye')
  passAndPlayState.clear('bullseye-progress')
  savedPassAndPlay.value = null
  finalScores.value = scores
  stage.value = 'done'
}

function resetGame() {
  passAndPlayState.clear('bullseye')
  passAndPlayState.clear('bullseye-progress')
  savedPassAndPlay.value = null
  finalScores.value = []
  stage.value = 'landing'
}

// Clicking the "Bullseye" nav tab while already on this page doesn't trigger
// any navigation event on its own, so it needs its own trigger to jump back to
// the very first screen - same as every other pass-and-play game.
watch(() => navTrigger.state.bullseye, () => {
  stage.value = 'landing'
})
</script>
