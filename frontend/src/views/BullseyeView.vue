<template>
  <GameAccessGate game="bullseye">
  <div>
    <template v-if="stage === 'modeChoice'">
      <h1>Bullseye</h1>
      <p class="page-subtitle">
        Each round shows a target number, like <em>"13 goals in the Premier League 2024/25"</em>.
        Take turns naming a player you think is close to it - whoever's answer is
        farthest off is eliminated. Play continues until one player remains.
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

      <div v-if="savedPassAndPlay" class="banner" style="background:rgba(242,183,5,0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>You have a pass-the-device game in progress.</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" @click="resumePassAndPlay">Resume</button>
          <button class="btn btn-secondary btn-sm" @click="dismissPassAndPlay">Dismiss</button>
        </div>
      </div>

      <BullseyePreview />

      <div class="mode-choice-row">
        <button class="mode-choice-card" @click="stage = 'landing'">
          <h3>Same device</h3>
          <p>Pass the phone around - everyone takes their turn on one screen.</p>
        </button>
        <button class="mode-choice-card" @click="stage = 'onlineChoice'">
          <h3>Play online</h3>
          <p>Everyone plays from their own device with a shared room code.</p>
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'landing'">
      <h1>Bullseye</h1>
      <p class="page-subtitle">
        A pass-the-device party quiz. Each round shows a target number - take turns naming
        someone close to it, farthest off is eliminated, until one player remains.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Players</label>
        <select v-model.number="numPlayers">
          <option v-for="n in [2,3,4,5,6,7,8]" :key="n" :value="n">{{ n }}</option>
        </select>
      </div>
      <p class="page-subtitle" style="margin-top:-8px;">
        {{ numPlayers }} players means {{ numPlayers - 1 }} round{{ numPlayers - 1 > 1 ? 's' : '' }} - one elimination per round.
      </p>

      <button class="btn btn-secondary" @click="stage = 'modeChoice'">← Back</button>
      <button class="btn btn-primary" style="margin-left:10px;" @click="goToSetup">Create game</button>
    </template>

    <template v-else-if="stage === 'onlineChoice'">
      <h1>Play online</h1>
      <p class="page-subtitle">Same game, different devices - share a room code with friends instead of passing one phone around.</p>
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
      <p class="page-subtitle">
        The round count is set once everyone's joined and you start the game - one elimination
        per round, so it always ends with exactly one player left.
      </p>

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
        {{ (onlineRoom?.participants || []).length }} joined ·
        {{ (onlineRoom?.participants || []).length >= 2 ? (onlineRoom.participants.length - 1) + ' round(s) if you start now' : 'need at least 2 to start' }}
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

    <OnlineBullseyeGame
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
import { useRoute } from 'vue-router'
import api from '../services/api'
import { createRoomChannel } from '../composables/useRoomChannel'
import auth from '../services/auth'
import activeRoom from '../services/activeRoom'
import passAndPlayState from '../services/passAndPlayState'
import navTrigger from '../services/navTrigger'
import BullseyeGame from '../components/BullseyeGame.vue'
import OnlineBullseyeGame from '../components/OnlineBullseyeGame.vue'
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

const stage = ref('modeChoice')
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
  api.recordGamePlayed('BULLSEYE')
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
  stage.value = 'modeChoice'
}

// --- Online multiplayer ---
const onlineDisplayName = ref(auth.state.displayName || '')
const joinCode = ref('')
const onlineRoom = ref(null)
const creatingRoom = ref(false)
const joiningRoom = ref(false)
const startingRoom = ref(false)
let lobbyChannel = null

const isHost = computed(() => !!onlineRoom.value?.host)

// Colors are auto-assigned (no picker) - picks one not already in use by
// another participant in the room, same convention every online game's view
// already uses.
function pickUnusedColor(existingColors = []) {
  const palette = ['#4f46e5', '#F22C05', '#F2BB05', '#032E8A', '#05D6F2', '#f43f5e', '#5D038A', '#22c55e']
  const available = palette.filter(c => !existingColors.includes(c))
  const pool = available.length ? available : palette
  return pool[Math.floor(Math.random() * pool.length)]
}

async function createOnlineRoom() {
  error.value = ''
  creatingRoom.value = true
  try {
    onlineRoom.value = await api.createRoom({
      gameType: 'BULLSEYE',
      displayName: onlineDisplayName.value.trim(),
      color: pickUnusedColor()
    })
    activeRoom.save(onlineRoom.value.roomCode, 'BULLSEYE')
    stage.value = 'onlineLobby'
    startLobbyChannel()
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
    let existingColors = []
    try {
      const existing = await api.getRoom(code)
      existingColors = (existing.participants || []).map(p => p.color)
    } catch (e) {
      // if this pre-check fails, joining below will surface the real error (bad code, etc.)
    }
    onlineRoom.value = await api.joinRoom(code, {
      displayName: onlineDisplayName.value.trim(),
      color: pickUnusedColor(existingColors)
    })
    activeRoom.save(onlineRoom.value.roomCode, 'BULLSEYE')
    if (onlineRoom.value.status === 'IN_PROGRESS') {
      stage.value = 'onlineGame'
    } else {
      stage.value = 'onlineLobby'
      startLobbyChannel()
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not join that room - check the code and try again.'
    activeRoom.clear()
    savedRoomCode.value = ''
  } finally {
    joiningRoom.value = false
  }
}

// Applies a lobby update from either a push or a poll - shared so both do
// exactly the same thing (see createRoomChannel).
// A lobby broadcast is personalized (host/yourParticipantId) from whoever
// triggered it - joining as a guest broadcasts a DTO where isHost is false,
// which would be wrong applied blindly to the host's own tab. Every OTHER
// field (participants, status) is genuinely shared, so only those two need
// preserving from what this tab already knows about itself.
function applyLobbyUpdate(updated) {
  const mine = onlineRoom.value
  onlineRoom.value = mine
    ? { ...updated, host: mine.host, yourParticipantId: mine.yourParticipantId }
    : updated
  if (updated.status === 'IN_PROGRESS') {
    stopLobbyChannel()
    stage.value = 'onlineGame'
  }
}

async function pollLobby() {
  if (!onlineRoom.value) return
  try {
    const updated = await api.getRoom(onlineRoom.value.roomCode)
    applyLobbyUpdate(updated)
  } catch (e) {
    // a transient poll failure isn't worth surfacing - it'll succeed next tick
  }
}

function startLobbyChannel() {
  stopLobbyChannel()
  lobbyChannel = createRoomChannel(`/topic/rooms/${onlineRoom.value.roomCode}/lobby`, { poll: pollLobby, onMessage: applyLobbyUpdate })
  lobbyChannel.start()
}

function stopLobbyChannel() {
  if (lobbyChannel) {
    lobbyChannel.stop()
    lobbyChannel = null
  }
}

async function startOnlineRoom() {
  error.value = ''
  startingRoom.value = true
  try {
    await api.startRoom(onlineRoom.value.roomCode)
    stopLobbyChannel()
    stage.value = 'onlineGame'
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not start the game.'
  } finally {
    startingRoom.value = false
  }
}

function leaveLobby() {
  stopLobbyChannel()
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

// Server already records the completed game (see BullseyeOnlineService.nextRound),
// so unlike onGameOver above, this doesn't also call api.recordGamePlayed - that
// would double-count it.
function onOnlineGameOver(scores) {
  activeRoom.clear()
  finalScores.value = scores
  resetOnline()
  stage.value = 'done'
}

const savedRoomCode = ref('')
const rejoining = ref(false)
const route = useRoute()

onMounted(() => {
  // Arrived via /join?code=... (JoinGuestView.vue) - go straight into that
  // room instead of making them retype the code they already entered once.
  if (route.query.code) {
    stage.value = 'onlineJoin'
    joinOnlineRoom(String(route.query.code))
    return
  }
  savedRoomCode.value = activeRoom.get('BULLSEYE') || ''
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

// Clicking the "Bullseye" nav tab while already on this page doesn't trigger
// any navigation event on its own, so it needs its own trigger to jump back to
// the very first screen - same as every other pass-and-play game.
watch(() => navTrigger.state.bullseye, () => {
  stopLobbyChannel()
  stage.value = 'modeChoice'
})
</script>
