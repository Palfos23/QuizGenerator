<template>
  <div>
    <template v-if="stage === 'landing'">
      <h1>Imposter</h1>
      <p class="page-subtitle">
        Most of these tiles genuinely fit the theme - a few are deliberate imposters. Flip tiles as a
        group and try to avoid them. Fewest imposter hits wins.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="savedPassAndPlay" class="banner" style="background:rgba(242,183,5,0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>You have a game in progress.</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" @click="resumePassAndPlay">Resume</button>
          <button class="btn btn-secondary btn-sm" @click="dismissPassAndPlay">Dismiss</button>
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>Players</label>
          <select v-model.number="numPlayers">
            <option v-for="n in [2,3,4,5]" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
        <div style="flex:1; min-width:160px;">
          <label>Boards</label>
          <select v-model.number="numBoards">
            <option v-for="n in [1,2,3,4]" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
      </div>

      <button class="btn btn-primary" style="margin-top:10px;" @click="goToSetup">Next →</button>
    </template>

    <template v-else-if="stage === 'setup'">
      <h1>Name the players</h1>
      <p class="page-subtitle">{{ numPlayers }} players, passing the device between turns.</p>
      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-for="(name, i) in playerNames" :key="i" class="field">
        <label>Player {{ i + 1 }}</label>
        <input type="text" v-model="playerNames[i]" :placeholder="`Player ${i + 1}`" />
      </div>

      <div style="margin-top:20px; display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'landing'">← Back</button>
        <button class="btn btn-primary" :disabled="!allNamed" @click="goToBoardChoice">Next →</button>
      </div>
    </template>

    <template v-else-if="stage === 'pickBoards'">
      <h1>Choose {{ numBoards }} board{{ numBoards > 1 ? 's' : '' }}</h1>
      <p class="page-subtitle">{{ chosenBoards.length }} / {{ numBoards }} selected</p>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
      <div v-else-if="!allBoards.length" class="empty-state friendly">No boards yet - ask an admin to add one.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="g in allBoards" :key="g.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ g.title }}</div>
            <div class="saved-quiz-meta">{{ g.tileCount }} tiles, {{ g.imposterCount }} imposter<span v-if="g.imposterCount !== 1">s</span><span v-if="g.description"> · {{ g.description }}</span></div>
          </div>
          <button
            class="btn btn-sm"
            :class="isChosen(g) ? 'btn-primary' : 'btn-secondary'"
            :disabled="!isChosen(g) && chosenBoards.length >= numBoards"
            @click="toggleChosen(g)"
          >{{ isChosen(g) ? 'Selected ✓' : '+ Select' }}</button>
        </div>
      </div>

      <div style="margin-top:20px; display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'setup'">← Back</button>
        <button class="btn btn-primary" :disabled="chosenBoards.length !== numBoards" @click="startGame">
          Start game ({{ chosenBoards.length }}/{{ numBoards }})
        </button>
      </div>
    </template>

    <ImposterGame
      v-else-if="stage === 'game'"
      :grid-ids="gameGridIds"
      :players="playerNames"
      @game-over="onGameOver"
    />

    <template v-else-if="stage === 'done'">
      <h1 style="text-align:center;">Game over!</h1>
      <h2 style="text-align:center; color:var(--gold);">🏆 {{ winner }}</h2>

      <table class="table" style="max-width:480px; margin:20px auto; table-layout:fixed; min-width:0;">
        <thead>
          <tr><th>Player</th><th style="text-align:right;">Imposter hits</th></tr>
        </thead>
        <tbody>
          <tr v-for="([name, points], i) in finalScores" :key="name" :class="{ 'tension-winner-row': i === 0 }">
            <td>{{ name }}</td>
            <td style="text-align:right;">{{ points }}</td>
          </tr>
        </tbody>
      </table>

      <div v-if="reveal.length" style="max-width:480px; margin:0 auto 20px;">
        <h3 style="text-align:center; margin-bottom:10px;">The imposters</h3>
        <div v-for="(r, i) in reveal" :key="i" style="text-align:center; color:var(--text-dim); padding:4px 0;">
          <strong style="color:var(--coral);">{{ r.imposterName }}</strong>
          <span v-if="r.replacedName"> replaced <strong style="color:var(--text);">{{ r.replacedName }}</strong></span>
        </div>
      </div>

      <div style="text-align:center;">
        <button class="btn btn-primary" @click="resetToStart">Play again</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import passAndPlayState from '../services/passAndPlayState'
import ImposterGame from '../components/ImposterGame.vue'

const stage = ref('landing')
const error = ref('')
const loading = ref(true)
const allBoards = ref([])
const numPlayers = ref(2)
const numBoards = ref(1)
const playerNames = ref([])
const chosenBoards = ref([])
const gameGridIds = ref([])
const finalScores = ref([])
const reveal = ref([])

const allNamed = computed(() => playerNames.value.every(n => n.trim().length > 0))
const winner = computed(() => finalScores.value[0]?.[0] ?? null)

onMounted(loadBoards)

const savedPassAndPlay = ref(null)
onMounted(() => {
  savedPassAndPlay.value = passAndPlayState.load('imposter')
})

function resumePassAndPlay() {
  const saved = savedPassAndPlay.value
  gameGridIds.value = saved.gridIds
  playerNames.value = saved.players
  stage.value = 'game'
}

function dismissPassAndPlay() {
  passAndPlayState.clear('imposter')
  savedPassAndPlay.value = null
}

async function loadBoards() {
  loading.value = true
  error.value = ''
  try {
    allBoards.value = await api.listImposterGrids()
  } catch (e) {
    error.value = 'Could not load boards.'
  } finally {
    loading.value = false
  }
}

function goToSetup() {
  playerNames.value = Array.from({ length: numPlayers.value }, (_, i) => playerNames.value[i] || '')
  error.value = ''
  stage.value = 'setup'
}

function goToBoardChoice() {
  playerNames.value = playerNames.value.map(n => n.trim())
  chosenBoards.value = []
  error.value = ''
  stage.value = 'pickBoards'
}

function isChosen(g) {
  return chosenBoards.value.some(c => c.id === g.id)
}

function toggleChosen(g) {
  if (isChosen(g)) {
    chosenBoards.value = chosenBoards.value.filter(c => c.id !== g.id)
  } else if (chosenBoards.value.length < numBoards.value) {
    chosenBoards.value.push(g)
  }
}

function startGame() {
  gameGridIds.value = chosenBoards.value.map(b => b.id)
  passAndPlayState.save('imposter', { gridIds: gameGridIds.value, players: playerNames.value })
  savedPassAndPlay.value = passAndPlayState.load('imposter')
  stage.value = 'game'
}

function onGameOver({ scores, revealList }) {
  passAndPlayState.clear('imposter')
  savedPassAndPlay.value = null
  finalScores.value = [...scores].sort((a, b) => a[1] - b[1]) // fewest imposter hits wins
  reveal.value = revealList
  stage.value = 'done'
}

function resetToStart() {
  gameGridIds.value = []
  chosenBoards.value = []
  playerNames.value = []
  finalScores.value = []
  reveal.value = []
  stage.value = 'landing'
}
</script>
