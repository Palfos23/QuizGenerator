<template>
  <div>
    <template v-if="stage === 'category'">
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
      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
      <div v-else-if="!grids.length" class="empty-state friendly">No boards yet - ask an admin to add one.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="g in grids" :key="g.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ g.title }}</div>
            <div class="saved-quiz-meta">{{ g.tileCount }} tiles, {{ g.imposterCount }} imposter<span v-if="g.imposterCount !== 1">s</span><span v-if="g.description"> · {{ g.description }}</span></div>
          </div>
          <button class="btn btn-primary btn-sm" @click="chooseGrid(g.id)">Play</button>
        </div>
      </div>
    </template>

    <template v-else-if="stage === 'setup'">
      <h1>Name the players</h1>
      <p class="page-subtitle">2 to 5 players, passing the device between turns.</p>
      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-for="(name, i) in playerNames" :key="i" class="field">
        <label>Player {{ i + 1 }}</label>
        <div style="display:flex; gap:8px;">
          <input type="text" v-model="playerNames[i]" :placeholder="`Player ${i + 1}`" style="flex:1;" />
          <button v-if="playerNames.length > 2" class="chip-remove-btn" @click="playerNames.splice(i, 1)">✕</button>
        </div>
      </div>
      <button v-if="playerNames.length < 5" class="btn btn-secondary btn-sm" @click="playerNames.push('')">+ Add player</button>

      <div style="margin-top:20px;">
        <button class="btn btn-secondary" @click="stage = 'category'">← Back</button>
        <button class="btn btn-primary" style="margin-left:10px;" :disabled="!allNamed" @click="startGame">Start game</button>
      </div>
    </template>

    <ImposterGame
      v-else-if="stage === 'game'"
      :grid-id="chosenGridId"
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

const stage = ref('category')
const error = ref('')
const loading = ref(true)
const grids = ref([])
const chosenGridId = ref(null)
const playerNames = ref(['', ''])
const finalScores = ref([])
const reveal = ref([])

const allNamed = computed(() => playerNames.value.every(n => n.trim().length > 0))
const winner = computed(() => finalScores.value[0]?.[0] ?? null)

onMounted(loadGrids)

const savedPassAndPlay = ref(null)
onMounted(() => {
  savedPassAndPlay.value = passAndPlayState.load('imposter')
})

function resumePassAndPlay() {
  const saved = savedPassAndPlay.value
  chosenGridId.value = saved.gridId
  playerNames.value = saved.players
  stage.value = 'game'
}

function dismissPassAndPlay() {
  passAndPlayState.clear('imposter')
  savedPassAndPlay.value = null
}

async function loadGrids() {
  loading.value = true
  error.value = ''
  try {
    grids.value = await api.listImposterGrids()
  } catch (e) {
    error.value = 'Could not load boards.'
  } finally {
    loading.value = false
  }
}

function chooseGrid(id) {
  chosenGridId.value = id
  stage.value = 'setup'
}

function startGame() {
  playerNames.value = playerNames.value.map(n => n.trim())
  passAndPlayState.save('imposter', { gridId: chosenGridId.value, players: playerNames.value })
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
  chosenGridId.value = null
  playerNames.value = ['', '']
  finalScores.value = []
  reveal.value = []
  stage.value = 'category'
}
</script>
