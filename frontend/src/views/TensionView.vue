<template>
  <div>
    <template v-if="stage === 'landing'">
      <h1>Tension</h1>
      <p class="page-subtitle">
        A pass-the-device party quiz. Guess as close to position 10 on the list as you dare -
        go too far past it into "tension" territory and it costs you.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>Players</label>
          <select v-model.number="numPlayers">
            <option v-for="n in 8" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
        <div style="flex:1; min-width:160px;">
          <label>Questions</label>
          <select v-model.number="numQuestions">
            <option v-for="n in 10" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
        <div style="flex:1; min-width:160px;">
          <label>Category</label>
          <select v-model="category">
            <option value="">All categories</option>
            <option v-for="c in mainCategories" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>
      </div>

      <button class="btn btn-primary" @click="goToSetup">Create game</button>
    </template>

    <template v-else-if="stage === 'setup'">
      <h1>Who's playing?</h1>
      <p class="page-subtitle" v-if="duplicateNames">Two players can't have the same name.</p>

      <div v-for="(p, i) in setupPlayers" :key="i" class="field" style="display:flex; gap:10px; align-items:center;">
        <input type="text" v-model="p.name" :placeholder="`Player ${i + 1}`" style="flex:1;" />
        <button
          class="color-swatch-btn"
          :style="{ background: p.color }"
          @click="openColorPicker(i)"
          title="Pick a color"
        ></button>
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'landing'">← Back</button>
        <button class="btn btn-primary" :disabled="!allNamed || duplicateNames || starting" @click="startGame">
          {{ starting ? 'Loading…' : 'Start game' }}
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

    <TensionGame
      v-else-if="stage === 'game'"
      :questions="questions"
      :players="setupPlayers"
      @game-over="onGameOver"
    />

    <template v-else-if="stage === 'done'">
      <h1 style="text-align:center;">Game over!</h1>
      <h2 v-if="winner" style="text-align:center; color:var(--gold);">🏆 {{ winner }}</h2>

      <table class="table" style="max-width:480px; margin:20px auto;">
        <thead>
          <tr><th>#</th><th>Player</th><th style="text-align:right;">Score</th></tr>
        </thead>
        <tbody>
          <tr v-for="([name, score], i) in sortedScores" :key="name" :class="{ 'tension-winner-row': i === 0 }">
            <td>{{ i + 1 }}</td>
            <td>{{ name }}</td>
            <td style="text-align:right;" :style="{ color: score > 0 ? 'var(--teal)' : score < 0 ? 'var(--coral)' : 'var(--text)' }">{{ score }}</td>
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
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import TensionGame from '../components/TensionGame.vue'

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
const numQuestions = ref(5)
const category = ref('')
const mainCategories = ref([])

const setupPlayers = reactive([])
const colorPickerIndex = ref(null)
const starting = ref(false)
const questions = ref([])
const finalScores = ref({})

onMounted(async () => {
  try {
    mainCategories.value = await api.fetchTensionMainCategories()
  } catch (e) {
    // category list is a nice-to-have for the dropdown - fail quietly
  }
})

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

async function startGame() {
  starting.value = true
  error.value = ''
  try {
    questions.value = await api.fetchTensionQuestions(numQuestions.value, category.value)
    if (!questions.value.length) {
      error.value = 'No tension questions available yet - ask an admin to add some.'
      stage.value = 'landing'
      return
    }
    stage.value = 'game'
  } catch (e) {
    error.value = 'Could not load questions.'
    stage.value = 'landing'
  } finally {
    starting.value = false
  }
}

const sortedScores = computed(() => Object.entries(finalScores.value).sort((a, b) => b[1] - a[1]))
const winner = computed(() => sortedScores.value[0]?.[0] ?? null)

function onGameOver(scores) {
  finalScores.value = scores
  stage.value = 'done'
}

function resetGame() {
  questions.value = []
  finalScores.value = {}
  stage.value = 'landing'
}
</script>
