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
      <button class="btn btn-secondary" style="margin-left:10px;" @click="stage = 'onlineChoice'">🌐 Play online instead</button>
    </template>

    <template v-else-if="stage === 'onlineChoice'">
      <h1>Play online</h1>
      <p class="page-subtitle">Same game, different devices - share a room code with 1-3 friends instead of passing one phone around.</p>
      <div v-if="error" class="banner error">{{ error }}</div>
      <div style="display:flex; gap:12px; flex-wrap:wrap;">
        <button class="btn btn-primary" @click="stage = 'onlineCreate'">+ Create a room</button>
        <button class="btn btn-secondary" @click="stage = 'onlineJoin'">Join with a code</button>
      </div>
      <button class="btn btn-secondary" style="margin-top:16px;" @click="stage = 'landing'">← Back</button>
    </template>

    <template v-else-if="stage === 'onlineCreate'">
      <h1>Create a room</h1>
      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Grids <span class="picker-hint">2-4</span></label>
        <select v-model.number="onlineNumGrids">
          <option v-for="n in [2,3,4]" :key="n" :value="n">{{ n }}</option>
        </select>
      </div>

      <div class="field">
        <label>How should the grids be picked?</label>
        <div class="language-row">
          <button class="language-btn" :class="{ active: onlineGridMode === 'random' }" @click="onlineGridMode = 'random'">🎲 Random</button>
          <button class="language-btn" :class="{ active: onlineGridMode === 'manual' }" @click="selectOnlineManual">✋ Pick my own</button>
        </div>
      </div>

      <div v-if="onlineGridMode === 'manual'" class="field">
        <label>Pick exactly {{ onlineNumGrids }} grid(s) <span class="picker-hint">{{ onlineChosenGrids.length }}/{{ onlineNumGrids }} chosen</span></label>
        <div v-if="loadingGrids" style="color:var(--text-dim);">Loading…</div>
        <div v-else class="saved-quiz-list">
          <div v-for="g in availableGrids" :key="g.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ g.title }}</div>
              <div class="saved-quiz-meta">{{ sportLabel(g.sport) }} · {{ g.entryCount }} athletes</div>
            </div>
            <button
              class="btn btn-sm"
              :class="onlineChosenGrids.includes(g.id) ? 'btn-primary' : 'btn-secondary'"
              @click="toggleOnlineGrid(g.id)"
            >{{ onlineChosenGrids.includes(g.id) ? 'Chosen ✓' : '+ Choose' }}</button>
          </div>
        </div>
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'onlineChoice'">← Back</button>
        <button class="btn btn-primary" :disabled="creatingRoom || (onlineGridMode === 'manual' && onlineChosenGrids.length !== onlineNumGrids)" @click="createOnlineRoom">
          {{ creatingRoom ? 'Creating…' : 'Create room' }}
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'onlineJoin'">
      <h1>Join a room</h1>
      <div v-if="error" class="banner error">{{ error }}</div>
      <div class="field">
        <label>Room code</label>
        <input type="text" v-model="joinCode" placeholder="e.g. ABCDE" style="text-transform:uppercase; letter-spacing:0.1em; font-size:1.2rem; text-align:center;" maxlength="5" />
      </div>
      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'onlineChoice'">← Back</button>
        <button class="btn btn-primary" :disabled="!joinCode.trim() || joiningRoom" @click="joinOnlineRoom">
          {{ joiningRoom ? 'Joining…' : 'Join' }}
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'onlineLobby'">
      <h1>Room {{ onlineRoom?.roomCode }}</h1>
      <p class="page-subtitle">Share this code with your friends. Everyone needs to join before the host starts.</p>
      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="saved-quiz-list" style="max-width:420px;">
        <div v-for="p in onlineRoom?.participants || []" :key="p.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              <span style="display:inline-block; width:10px; height:10px; border-radius:50%; margin-right:6px;" :style="{ background: p.color }"></span>
              {{ p.displayName }}
            </div>
          </div>
          <span class="tag" :style="{ background: 'rgba(61,220,151,0.15)', color: 'var(--teal)' }">In room</span>
        </div>
      </div>

      <p style="color:var(--text-dim); font-size:0.9rem; margin-top:16px;">
        {{ (onlineRoom?.participants || []).length }} joined · need at least 2 to start
      </p>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="leaveLobby">← Leave</button>
        <button
          v-if="isHost"
          class="btn btn-primary"
          :disabled="(onlineRoom?.participants || []).length < 2 || startingRoom"
          @click="startOnlineRoom"
        >{{ startingRoom ? 'Starting…' : 'Start game' }}</button>
        <span v-else style="color:var(--text-dim); align-self:center;">Waiting for the host to start…</span>
      </div>
    </template>

    <OnlineGridBattleGame
      v-else-if="stage === 'onlineGame'"
      :room-code="onlineRoom?.roomCode"
      :your-participant-id="onlineRoom?.yourParticipantId"
      :is-host="isHost"
      @game-over="onOnlineGameOver"
      @leave="stage = 'landing'; resetOnline()"
    />

    <template v-else-if="stage === 'setup'">
      <h1>Who's playing?</h1>
      <p class="page-subtitle" v-if="duplicateNames">Two players can't have the same name.</p>

      <div v-for="(p, i) in setupPlayers" :key="i" class="field" style="display:flex; gap:10px; align-items:center;">
        <input type="text" v-model="p.name" :placeholder="`Player ${i + 1}`" style="flex:1;" />
        <span class="color-swatch-btn" :style="{ background: p.color }" title="Your color"></span>
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'landing'">← Back</button>
        <button class="btn btn-primary" :disabled="!allNamed || duplicateNames" @click="goToGridChoice">
          Next →
        </button>
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
import auth from '../services/auth'
import { sportLabel } from '../constants'
import MultiplayerGridGame from '../components/MultiplayerGridGame.vue'
import OnlineGridBattleGame from '../components/OnlineGridBattleGame.vue'

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

const availableGrids = ref([])
const loadingGrids = ref(false)
const chosenGrids = ref([])
const gameGrids = ref([])
const finalScores = ref({})

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

async function loadAvailableGrids() {
  loadingGrids.value = true
  try {
    const [active, archive] = await Promise.all([api.getActiveGrids(), api.getArchiveGrids()])
    availableGrids.value = [...active, ...archive]
  } catch (e) {
    error.value = 'Could not load grids.'
  } finally {
    loadingGrids.value = false
  }
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
    stage.value = 'pickGrids'
    await loadAvailableGrids()
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

// --- Online multiplayer ---
const onlineNumGrids = ref(2)
const onlineGridMode = ref('random')
const onlineChosenGrids = ref([])
const joinCode = ref('')
const onlineRoom = ref(null)
const creatingRoom = ref(false)
const joiningRoom = ref(false)
const startingRoom = ref(false)
let lobbyPollTimer = null

const isHost = computed(() => !!onlineRoom.value?.host)

function selectOnlineManual() {
  onlineGridMode.value = 'manual'
  onlineChosenGrids.value = []
  if (!availableGrids.value.length) loadAvailableGrids()
}

function toggleOnlineGrid(id) {
  if (onlineChosenGrids.value.includes(id)) {
    onlineChosenGrids.value = onlineChosenGrids.value.filter(g => g !== id)
  } else if (onlineChosenGrids.value.length < onlineNumGrids.value) {
    onlineChosenGrids.value.push(id)
  }
}

// Colors are auto-assigned (no picker) - just pick one not already visible in this browser tab's memory.
function randomOnlineColor() {
  const palette = ['#4f46e5', '#F22C05', '#F2BB05', '#032E8A', '#05D6F2', '#f43f5e', '#5D038A', '#22c55e']
  return palette[Math.floor(Math.random() * palette.length)]
}

async function createOnlineRoom() {
  error.value = ''
  creatingRoom.value = true
  try {
    const payload = {
      gameType: 'GRID_BATTLE',
      displayName: auth.state.displayName,
      color: randomOnlineColor()
    }
    if (onlineGridMode.value === 'manual') {
      payload.gridIds = onlineChosenGrids.value
    } else {
      payload.randomGridCount = onlineNumGrids.value
    }
    onlineRoom.value = await api.createRoom(payload)
    stage.value = 'onlineLobby'
    startLobbyPolling()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not create the room.'
  } finally {
    creatingRoom.value = false
  }
}

async function joinOnlineRoom() {
  error.value = ''
  joiningRoom.value = true
  try {
    onlineRoom.value = await api.joinRoom(joinCode.value.trim().toUpperCase(), {
      displayName: auth.state.displayName,
      color: randomOnlineColor()
    })
    stage.value = 'onlineLobby'
    startLobbyPolling()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not join that room - check the code and try again.'
  } finally {
    joiningRoom.value = false
  }
}

function startLobbyPolling() {
  clearInterval(lobbyPollTimer)
  lobbyPollTimer = setInterval(async () => {
    if (!onlineRoom.value) return
    try {
      const updated = await api.getRoom(onlineRoom.value.roomCode)
      onlineRoom.value = updated
      if (updated.status === 'IN_PROGRESS') {
        clearInterval(lobbyPollTimer)
        stage.value = 'onlineGame'
      }
    } catch (e) {
      // a transient poll failure isn't worth surfacing - it'll succeed next tick
    }
  }, 2000)
}

async function startOnlineRoom() {
  error.value = ''
  startingRoom.value = true
  try {
    await api.startRoom(onlineRoom.value.roomCode)
    clearInterval(lobbyPollTimer)
    stage.value = 'onlineGame'
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not start the game.'
  } finally {
    startingRoom.value = false
  }
}

function leaveLobby() {
  clearInterval(lobbyPollTimer)
  resetOnline()
  stage.value = 'landing'
}

function resetOnline() {
  onlineRoom.value = null
  onlineChosenGrids.value = []
  onlineGridMode.value = 'random'
  joinCode.value = ''
}

function onOnlineGameOver(scores) {
  finalScores.value = scores
  resetOnline()
  stage.value = 'done'
}
</script>
