<template>
  <GameAccessGate game="flashback">
  <div>
    <template v-if="stage === 'landing'">
      <h1>Flashback</h1>
      <p class="page-subtitle">
        A pass-the-device party quiz. Guess the exact year from a clue - it's hard at first,
        then gets easier each round nobody nails it.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="savedPassAndPlay" class="banner" style="background:rgba(242,183,5,0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>You have a pass-the-device game in progress.</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" @click="resumePassAndPlay">Resume</button>
          <button class="btn btn-secondary btn-sm" @click="dismissPassAndPlay">Dismiss</button>
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>Players</label>
          <select v-model.number="numPlayers">
            <option v-for="n in playerCountOptions" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
        <div style="flex:1; min-width:160px;">
          <label>Rounds</label>
          <select v-model.number="numRounds">
            <option v-for="n in 10" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
      </div>

      <button class="btn btn-primary" @click="goToSetup">Create game</button>

      <details class="advanced-disclosure" style="margin-top:24px;">
        <summary>Not sure how it works? See an example</summary>
        <div style="margin-top:16px; padding:16px 20px; border:1px solid var(--border); border-radius:var(--radius-md); background:rgba(255,255,255,0.02);">
          <p style="margin-top:0;">
            Each round hides a real year behind up to 5 clues, shown one at a time - hardest first,
            easiest last. After the first clue, players take turns guessing a year each. You have
            to hit it <strong>exactly</strong> to score - and the earlier you nail it, the more
            it's worth: with 4 clues, guessing right on clue 1 is worth 4 points, clue 2 is worth
            3, and so on down to 1 point on the last clue.
          </p>
          <ul style="margin:0 0 14px; padding-left:20px; line-height:1.7;">
            <li>Nobody right? The next clue reveals, and guessing continues - already-tried years stay visible to everyone.</li>
            <li>Still nobody by the last clue? Whoever's <strong>closest on that final round of guesses</strong> scores the last clue's point instead - a tie means everyone tied scores in full.</li>
          </ul>
          <p style="margin-bottom:0;">
            Highest total across every round wins the game.
          </p>
        </div>
      </details>
    </template>

    <template v-else-if="stage === 'setup'">
      <h1>Who's playing?</h1>
      <p class="page-subtitle" v-if="duplicateNames">Two players can't have the same name.</p>

      <div v-for="(p, i) in setupPlayers" :key="i" class="field">
        <input type="text" v-model="p.name" :placeholder="`Player ${i + 1}`" />
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'landing'">← Back</button>
        <button class="btn btn-primary" :disabled="!allNamed || duplicateNames" @click="startGame">
          Start game
        </button>
      </div>
    </template>

    <FlashbackGame
      v-else-if="stage === 'game'"
      :round-count="numRounds"
      :players="setupPlayers"
      @game-over="onGameOver"
    />

    <template v-else-if="stage === 'done'">
      <div class="modal-backdrop">
        <div class="completion-popup" :class="{ 'podium-host': setupPlayers.length > 2 }">
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
import FlashbackGame from '../components/FlashbackGame.vue'
import GameAccessGate from '../components/GameAccessGate.vue'

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
const playerCountOptions = [2, 3, 4, 5, 6, 7, 8] // min 2 - closest-guess tiebreak needs at least a real opponent

const stage = ref('landing')
const error = ref('')
const numPlayers = ref(2)
const numRounds = ref(5)

const setupPlayers = reactive([])
const finalScores = ref([])

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

function startGame() {
  passAndPlayState.save('flashback', {
    roundCount: numRounds.value,
    players: [...setupPlayers]
  })
  savedPassAndPlay.value = passAndPlayState.load('flashback')
  stage.value = 'game'
}

const sortedScores = computed(() => [...finalScores.value].sort((a, b) => b[1] - a[1]))

function onGameOver(scores) {
  api.recordGamePlayed('FLASHBACK')
  passAndPlayState.clear('flashback')
  passAndPlayState.clear('flashback-progress')
  savedPassAndPlay.value = null
  finalScores.value = scores
  stage.value = 'done'
}

function resetGame() {
  passAndPlayState.clear('flashback')
  passAndPlayState.clear('flashback-progress')
  savedPassAndPlay.value = null
  finalScores.value = []
  stage.value = 'landing'
}

const savedPassAndPlay = ref(null)
onMounted(() => {
  savedPassAndPlay.value = passAndPlayState.load('flashback')
})

function resumePassAndPlay() {
  const saved = savedPassAndPlay.value
  numRounds.value = saved.roundCount
  setupPlayers.length = 0
  saved.players.forEach(p => setupPlayers.push(p))
  stage.value = 'game'
}

function dismissPassAndPlay() {
  passAndPlayState.clear('flashback')
  passAndPlayState.clear('flashback-progress')
  savedPassAndPlay.value = null
}

// Clicking the "Flashback" nav tab while already on this page doesn't trigger
// any navigation event on its own - same reasoning as MultiplayerGridView's
// equivalent watcher.
watch(() => navTrigger.state.flashback, () => {
  stage.value = 'landing'
})
</script>
