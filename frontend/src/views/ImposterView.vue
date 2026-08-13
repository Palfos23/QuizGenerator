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

      <div class="podium-row">
        <div v-for="(entry, i) in finalScores.slice(0, 3)" :key="entry[0]" class="podium-block" :class="`rank-${i + 1}`">
          <div class="podium-rank-number">{{ i + 1 }}</div>
          <div class="podium-name">{{ entry[0] }}</div>
          <div class="podium-score">{{ entry[1] }} imposter hit{{ entry[1] !== 1 ? 's' : '' }}</div>
        </div>
      </div>

      <div v-if="finalScores.length > 3" style="max-width:420px; margin:0 auto;">
        <div v-for="(entry, i) in finalScores.slice(3)" :key="entry[0]" class="podium-rest-row">
          <span>{{ i + 4 }}. {{ entry[0] }}</span>
          <span style="color:var(--text-dim);">{{ entry[1] }}</span>
        </div>
      </div>

      <div style="text-align:center; margin-top:20px;">
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

const allNamed = computed(() => playerNames.value.every(n => n.trim().length > 0))

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

function onGameOver({ scores }) {
  passAndPlayState.clear('imposter')
  savedPassAndPlay.value = null
  finalScores.value = [...scores].sort((a, b) => a[1] - b[1]) // fewest imposter hits wins
  stage.value = 'done'
}

function resetToStart() {
  gameGridIds.value = []
  chosenBoards.value = []
  playerNames.value = []
  finalScores.value = []
  stage.value = 'landing'
}
</script>
