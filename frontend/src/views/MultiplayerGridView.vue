<template>
  <div>
    <template v-if="stage === 'landing'">
      <h1>Grid Battle</h1>
      <p class="page-subtitle">
        A pass-the-device multiplayer version of Weekly Grid. Take turns guessing -
        get it right and the tile reveals (and you score a point), get it wrong and you lose a life.
        Play continues until a grid is fully solved or everyone's out of lives.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>Players</label>
          <select v-model.number="numPlayers">
            <option v-for="n in [2,3,4]" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
        <div style="flex:1; min-width:160px;">
          <label>Grids</label>
          <select v-model.number="numGrids">
            <option v-for="n in [2,3,4]" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
      </div>

      <div class="field">
        <label>How should the grids be picked?</label>
        <div class="language-row">
          <button class="language-btn" :class="{ active: gridMode === 'random' }" @click="gridMode = 'random'">🎲 Random</button>
          <button class="language-btn" :class="{ active: gridMode === 'manual' }" @click="gridMode = 'manual'">✋ Pick my own</button>
        </div>
      </div>

      <button class="btn btn-primary" @click="goToSetup">Create game</button>
    </template>

    <template v-else-if="stage === 'setup'">
      <h1>Who's playing?</h1>
      <p class="page-subtitle" v-if="duplicateNames">Two players can't have the same name.</p>

      <div v-for="(p, i) in setupPlayers" :key="i" class="field" style="display:flex; gap:10px; align-items:center;">
        <input type="text" v-model="p.name" :placeholder="`Player ${i + 1}`" style="flex:1;" />
        <button class="color-swatch-btn" :style="{ background: p.color }" @click="openColorPicker(i)" title="Pick a color"></button>
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'landing'">← Back</button>
        <button class="btn btn-primary" :disabled="!allNamed || duplicateNames" @click="goToGridChoice">
          Next →
        </button>
      </div>

      <div v-if="colorPickerIndex !== null" class="modal-backdrop" @click.self="colorPickerIndex = null">
        <div class="modal">
          <h2>Pick a color</h2>
          <div class="color-grid">
            <button
              v-for="c in colorOptions"
              :key="c.hex"
              class="color-swatch-btn"
              :class="{ selected: setupPlayers[colorPickerIndex].color === c.hex }"
              :style="{ background: usedColors.includes(c.hex) && setupPlayers[colorPickerIndex].color !== c.hex ? 'rgba(255,255,255,0.1)' : c.hex, opacity: usedColors.includes(c.hex) && setupPlayers[colorPickerIndex].color !== c.hex ? 0.4 : 1 }"
              :disabled="usedColors.includes(c.hex) && setupPlayers[colorPickerIndex].color !== c.hex"
              :title="c.name"
              @click="pickColor(c.hex)"
            ></button>
          </div>
          <button class="btn btn-secondary" style="margin-top:16px;" @click="colorPickerIndex = null">Close</button>
        </div>
      </div>
    </template>

    <template v-else-if="stage === 'pickGrids'">
      <h1>Choose {{ numGrids }} grid{{ numGrids > 1 ? 's' : '' }}</h1>
      <p class="page-subtitle">{{ chosenGrids.length }} / {{ numGrids }} selected</p>

      <div v-if="loadingGrids" style="color:var(--text-dim);">Loading…</div>
      <div v-else-if="!availableGrids.length" class="empty-state">No grids available yet.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="g in availableGrids" :key="g.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ g.title }}</div>
            <div class="saved-quiz-meta">{{ sportLabel(g.sport) }} · {{ g.entryCount }} athletes</div>
          </div>
          <button
            class="btn btn-sm"
            :class="isChosen(g) ? 'btn-primary' : 'btn-secondary'"
            :disabled="!isChosen(g) && chosenGrids.length >= numGrids"
            @click="toggleChosen(g)"
          >{{ isChosen(g) ? 'Selected ✓' : '+ Select' }}</button>
        </div>
      </div>

      <div style="display:flex; gap:12px; margin-top:20px;">
        <button class="btn btn-secondary" @click="stage = 'setup'">← Back</button>
        <button class="btn btn-primary" :disabled="chosenGrids.length !== numGrids" @click="startGame">
          Start game ({{ chosenGrids.length }}/{{ numGrids }})
        </button>
      </div>
    </template>

    <MultiplayerGridGame
      v-else-if="stage === 'game'"
      :grids="gameGrids"
      :players="setupPlayers"
      @game-over="onGameOver"
    />

    <template v-else-if="stage === 'done'">
      <h1 style="text-align:center;">Game over!</h1>
      <h2 v-if="winner" style="text-align:center; color:var(--gold);">🏆 {{ winner }}</h2>

      <table class="table" style="max-width:480px; margin:20px auto; table-layout:fixed; min-width:0;">
        <thead>
          <tr><th>#</th><th>Player</th><th style="text-align:right;">Score</th></tr>
        </thead>
        <tbody>
          <tr v-for="([name, score], i) in sortedScores" :key="name" :class="{ 'tension-winner-row': i === 0 }">
            <td>{{ i + 1 }}</td>
            <td>{{ name }}</td>
            <td style="text-align:right;">{{ score }}</td>
          </tr>
        </tbody>
      </table>

      <div style="text-align:center;">
        <button class="btn btn-primary" @click="resetGame">Play again</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import api from '../services/api'
import { sportLabel } from '../constants'
import MultiplayerGridGame from '../components/MultiplayerGridGame.vue'

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
const numGrids = ref(2)
const gridMode = ref('random')

const setupPlayers = reactive([])
const colorPickerIndex = ref(null)

const availableGrids = ref([])
const loadingGrids = ref(false)
const chosenGrids = ref([])
const gameGrids = ref([])
const finalScores = ref({})

function rebuildSetupPlayers() {
  setupPlayers.length = 0
  for (let i = 0; i < numPlayers.value; i++) {
    setupPlayers.push({ name: '', color: colorOptions[i % colorOptions.length].hex })
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
const usedColors = computed(() => setupPlayers.map(p => p.color))

function openColorPicker(i) {
  colorPickerIndex.value = i
}
function pickColor(hex) {
  setupPlayers[colorPickerIndex.value].color = hex
  colorPickerIndex.value = null
}

async function goToGridChoice() {
  error.value = ''
  if (gridMode.value === 'random') {
    await pickRandomGrids()
    if (gameGrids.value.length < numGrids.value) {
      error.value = `Only found ${gameGrids.value.length} grid(s) - need at least ${numGrids.value}. Try picking your own, or ask an admin to add more grids.`
      stage.value = 'landing'
      return
    }
    stage.value = 'game'
  } else {
    chosenGrids.value = []
    loadingGrids.value = true
    stage.value = 'pickGrids'
    try {
      const [active, archive] = await Promise.all([api.getActiveGrids(), api.getArchiveGrids()])
      availableGrids.value = [...active, ...archive]
    } catch (e) {
      error.value = 'Could not load grids.'
    } finally {
      loadingGrids.value = false
    }
  }
}

async function pickRandomGrids() {
  try {
    const [active, archive] = await Promise.all([api.getActiveGrids(), api.getArchiveGrids()])
    const pool = [...active, ...archive]
    const shuffled = [...pool].sort(() => Math.random() - 0.5)
    gameGrids.value = shuffled.slice(0, numGrids.value)
  } catch (e) {
    gameGrids.value = []
  }
}

function isChosen(g) {
  return chosenGrids.value.some(c => c.id === g.id)
}
function toggleChosen(g) {
  if (isChosen(g)) {
    chosenGrids.value = chosenGrids.value.filter(c => c.id !== g.id)
  } else if (chosenGrids.value.length < numGrids.value) {
    chosenGrids.value.push(g)
  }
}

function startGame() {
  gameGrids.value = chosenGrids.value
  stage.value = 'game'
}

const sortedScores = computed(() => Object.entries(finalScores.value).sort((a, b) => b[1] - a[1]))
const winner = computed(() => sortedScores.value[0]?.[0] ?? null)

function onGameOver(scores) {
  finalScores.value = scores
  stage.value = 'done'
}

function resetGame() {
  gameGrids.value = []
  finalScores.value = {}
  stage.value = 'landing'
}
</script>
