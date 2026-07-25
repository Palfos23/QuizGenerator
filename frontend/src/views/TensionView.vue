<template>
  <div>
    <template v-if="stage === 'modeChoice'">
      <h1>Tension</h1>
      <p class="page-subtitle">
        Guess as close to position 10 on the list as you dare -
        go too far past it into "tension" territory and it costs you.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="savedRoomCode" class="banner" style="background:rgba(242,183,5,0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>You have a game in progress in room <strong>{{ savedRoomCode }}</strong>.</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" :disabled="rejoining" @click="rejoinSavedRoom">
            {{ rejoining ? 'Rejoining…' : 'Rejoin' }}
          </button>
          <button class="btn btn-secondary btn-sm" @click="dismissSavedRoom">Dismiss</button>
        </div>
      </div>

      <div style="display:flex; gap:16px; flex-wrap:wrap; margin-top:20px;">
        <button class="dashboard-feature-card" style="flex:1; min-width:220px; text-align:center; cursor:pointer; border:1px solid var(--border);" @click="stage = 'landing'">
          <h3>📱 Same device</h3>
          <p>Pass the phone around - everyone takes their turn on one screen.</p>
        </button>
        <button class="dashboard-feature-card" style="flex:1; min-width:220px; text-align:center; cursor:pointer; border:1px solid var(--border);" @click="stage = 'onlineChoice'">
          <h3>🌐 Play online</h3>
          <p>Everyone plays from their own device with a shared room code.</p>
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'landing'">
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

      <button class="btn btn-secondary" @click="stage = 'modeChoice'">← Back</button>
      <button class="btn btn-primary" style="margin-left:10px;" @click="goToSetup">Create game</button>
    </template>

    <template v-else-if="stage === 'onlineChoice'">
      <h1>Play online</h1>
      <p class="page-subtitle">Same game, different devices - everyone answers from their own phone instead of passing one around.</p>
      <div v-if="error" class="banner error">{{ error }}</div>
      <div style="display:flex; gap:12px; flex-wrap:wrap;">
        <button class="btn btn-primary" @click="stage = 'onlineCreate'">+ Create a room</button>
        <button class="btn btn-secondary" @click="stage = 'onlineJoin'">Join with a code</button>
      </div>
      <button class="btn btn-secondary" style="margin-top:16px;" @click="stage = 'modeChoice'">← Back</button>
    </template>

    <template v-else-if="stage === 'onlineCreate'">
      <h1>Create a room</h1>
      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Your name <span class="picker-hint">shown to other players</span></label>
        <input type="text" v-model="onlineDisplayName" placeholder="Your name" />
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>Questions</label>
          <select v-model.number="onlineNumQuestions">
            <option v-for="n in 10" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
        <div style="flex:1; min-width:160px;">
          <label>Category</label>
          <select v-model="onlineCategory">
            <option value="">All categories</option>
            <option v-for="c in mainCategories" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'onlineChoice'">← Back</button>
        <button class="btn btn-primary" :disabled="creatingRoom || !onlineDisplayName.trim()" @click="createOnlineRoom">
          {{ creatingRoom ? 'Creating…' : 'Create room' }}
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'onlineJoin'">
      <h1>Join a room</h1>
      <div v-if="error" class="banner error">{{ error }}</div>
      <div class="field">
        <label>Your name <span class="picker-hint">shown to other players</span></label>
        <input type="text" v-model="onlineDisplayName" placeholder="Your name" />
      </div>
      <div class="field">
        <label>Room code</label>
        <input type="text" v-model="joinCode" placeholder="e.g. ABCDE" style="text-transform:uppercase; letter-spacing:0.1em; font-size:1.2rem; text-align:center;" maxlength="5" />
      </div>
      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'onlineChoice'">← Back</button>
        <button class="btn btn-primary" :disabled="!joinCode.trim() || !onlineDisplayName.trim() || joiningRoom" @click="joinOnlineRoom()">
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

    <OnlineTensionGame
      v-else-if="stage === 'onlineGame'"
      :room-code="onlineRoom?.roomCode"
      :your-participant-id="onlineRoom?.yourParticipantId"
      :is-host="isHost"
      @game-over="onOnlineGameOver"
      @leave="leaveGame"
    />

    <template v-else-if="stage === 'setup'">
      <h1>Who's playing?</h1>
      <p class="page-subtitle" v-if="duplicateNames">Two players can't have the same name.</p>

      <div v-for="(p, i) in setupPlayers" :key="i" class="field">
        <input type="text" v-model="p.name" :placeholder="`Player ${i + 1}`" />
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'landing'">← Back</button>
        <button class="btn btn-primary" :disabled="!allNamed || duplicateNames || starting" @click="startGame">
          {{ starting ? 'Loading…' : 'Start game' }}
        </button>
      </div>

      <div v-if="starting" class="tension-intro-overlay">
        <span class="spinner" style="width:32px; height:32px; border-width:4px; margin-bottom:20px;"></span>
        <h1>Shuffling your questions…</h1>
        <p style="margin-top:12px; color:var(--text-dim);">First round starts in just a moment.</p>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import auth from '../services/auth'
import activeRoom from '../services/activeRoom'
import navTrigger from '../services/navTrigger'
import TensionGame from '../components/TensionGame.vue'
import OnlineTensionGame from '../components/OnlineTensionGame.vue'

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

const stage = ref('modeChoice')
const lastGameWasOnline = ref(false)
const error = ref('')
const numPlayers = ref(2)
const numQuestions = ref(5)
const category = ref('')
const mainCategories = ref([])

const setupPlayers = reactive([])
const starting = ref(false)
const questions = ref([])
const finalScores = ref([])

onMounted(async () => {
  try {
    mainCategories.value = await api.fetchTensionMainCategories()
  } catch (e) {
    // category list is a nice-to-have for the dropdown - fail quietly
  }
})

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

const sortedScores = computed(() => [...finalScores.value].sort((a, b) => b[1] - a[1]))
const winner = computed(() => sortedScores.value[0]?.[0] ?? null)

function onGameOver(scores) {
  lastGameWasOnline.value = false
  finalScores.value = scores
  stage.value = 'done'
}

function resetGame() {
  questions.value = []
  finalScores.value = []
  stage.value = lastGameWasOnline.value ? 'modeChoice' : 'landing'
}

// --- Online multiplayer ---
const onlineNumQuestions = ref(5)
const onlineDisplayName = ref(auth.state.displayName || '')
const onlineCategory = ref('')
const joinCode = ref('')
const onlineRoom = ref(null)
const creatingRoom = ref(false)
const joiningRoom = ref(false)
const startingRoom = ref(false)
let lobbyPollTimer = null

const isHost = computed(() => !!onlineRoom.value?.host)

function randomOnlineColor() {
  const palette = ['#4f46e5', '#F22C05', '#F2BB05', '#032E8A', '#05D6F2', '#f43f5e', '#5D038A', '#22c55e']
  return palette[Math.floor(Math.random() * palette.length)]
}

async function createOnlineRoom() {
  error.value = ''
  creatingRoom.value = true
  try {
    onlineRoom.value = await api.createRoom({
      gameType: 'TENSION',
      displayName: onlineDisplayName.value.trim(),
      color: randomOnlineColor(),
      tensionNumQuestions: onlineNumQuestions.value,
      tensionCategory: onlineCategory.value || null
    })
    activeRoom.save(onlineRoom.value.roomCode, 'TENSION')
    stage.value = 'onlineLobby'
    startLobbyPolling()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not create the room.'
  } finally {
    creatingRoom.value = false
  }
}

async function joinOnlineRoom(codeOverride) {
  error.value = ''
  joiningRoom.value = true
  try {
    const code = (codeOverride || joinCode.value).trim().toUpperCase()
    onlineRoom.value = await api.joinRoom(code, {
      displayName: onlineDisplayName.value.trim(),
      color: randomOnlineColor()
    })
    activeRoom.save(onlineRoom.value.roomCode, 'TENSION')
    if (onlineRoom.value.status === 'IN_PROGRESS') {
      stage.value = 'onlineGame'
    } else {
      stage.value = 'onlineLobby'
      startLobbyPolling()
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not join that room - check the code and try again.'
    activeRoom.clear()
    savedRoomCode.value = ''
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
  activeRoom.clear()
  resetOnline()
  stage.value = 'modeChoice'
}

function leaveGame() {
  activeRoom.clear()
  resetOnline()
  stage.value = 'modeChoice'
}

function resetOnline() {
  onlineRoom.value = null
  joinCode.value = ''
}

function onOnlineGameOver(scores) {
  activeRoom.clear()
  lastGameWasOnline.value = true
  finalScores.value = scores
  resetOnline()
  stage.value = 'done'
}

const savedRoomCode = ref('')
const rejoining = ref(false)

onMounted(() => {
  savedRoomCode.value = activeRoom.get('TENSION') || ''
})

// Clicking the "Tension" nav tab while already on this page doesn't trigger any
// navigation event on its own, so it needs its own trigger to jump back to the
// very first screen - mid-game state (pass-and-play or online) is simply left
// behind, same as if the tab had been closed and reopened.
watch(() => navTrigger.state.tension, () => {
  clearInterval(lobbyPollTimer)
  stage.value = 'modeChoice'
})

function dismissSavedRoom() {
  activeRoom.clear()
  savedRoomCode.value = ''
}

async function rejoinSavedRoom() {
  rejoining.value = true
  const code = savedRoomCode.value
  savedRoomCode.value = ''
  await joinOnlineRoom(code)
  rejoining.value = false
}
</script>
