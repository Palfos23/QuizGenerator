<template>
  <div>
    <template v-if="stage === 'modeChoice'">
      <h1>Imposter</h1>
      <p class="page-subtitle">
        Most of these tiles genuinely fit the theme - a few are deliberate imposters. Flip tiles as a
        group and try to avoid them. Fewest imposter hits wins.
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

      <div style="display:flex; gap:16px; flex-wrap:wrap; margin-top:20px;">
        <button class="dashboard-feature-card" style="flex:1; min-width:220px; text-align:center; cursor:pointer; border:1px solid var(--border);" @click="stage = 'landing'">
          <h3>Same device</h3>
          <p>Pass the phone around - everyone takes their turn on one screen.</p>
        </button>
        <button class="dashboard-feature-card" style="flex:1; min-width:220px; text-align:center; cursor:pointer; border:1px solid var(--border);" @click="stage = 'onlineChoice'">
          <h3>Play online</h3>
          <p>Everyone plays from their own device with a shared room code.</p>
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'landing'">
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

      <button class="btn btn-secondary" @click="stage = 'modeChoice'">← Back</button>
      <button class="btn btn-primary" style="margin-left:10px;" @click="goToSetup">Next →</button>
    </template>

    <template v-else-if="stage === 'onlineChoice'">
      <h1>Play online</h1>
      <p class="page-subtitle">Same game, different devices - share a room code with 1-4 friends instead of passing one phone around.</p>
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

      <div class="field">
        <label>Boards <span class="picker-hint">2-4</span></label>
        <select v-model.number="onlineNumBoards">
          <option v-for="n in [2,3,4]" :key="n" :value="n">{{ n }}</option>
        </select>
      </div>

      <div class="field">
        <label>How should the boards be picked?</label>
        <div class="language-row">
          <button class="language-btn" :class="{ active: onlineBoardMode === 'random' }" @click="onlineBoardMode = 'random'">Random</button>
          <button class="language-btn" :class="{ active: onlineBoardMode === 'manual' }" @click="selectOnlineManual">Pick my own</button>
        </div>
      </div>

      <div v-if="onlineBoardMode === 'manual'" class="field">
        <label>Pick exactly {{ onlineNumBoards }} board(s) <span class="picker-hint">{{ onlineChosenBoards.length }}/{{ onlineNumBoards }} chosen</span></label>
        <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
        <div v-else class="saved-quiz-list">
          <div v-for="g in allBoards" :key="g.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ g.title }}</div>
              <div class="saved-quiz-meta">{{ g.tileCount }} tiles, {{ g.imposterCount }} imposter<span v-if="g.imposterCount !== 1">s</span></div>
            </div>
            <button
              class="btn btn-sm"
              :class="onlineChosenBoards.includes(g.id) ? 'btn-primary' : 'btn-secondary'"
              @click="toggleOnlineBoard(g.id)"
            >{{ onlineChosenBoards.includes(g.id) ? 'Chosen ✓' : '+ Choose' }}</button>
          </div>
        </div>
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'onlineChoice'">← Back</button>
        <button class="btn btn-primary" :disabled="creatingRoom || !onlineDisplayName.trim() || (onlineBoardMode === 'manual' && onlineChosenBoards.length !== onlineNumBoards)" @click="createOnlineRoom">
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

    <OnlineImposterGame
      v-else-if="stage === 'onlineGame'"
      :room-code="onlineRoom?.roomCode"
      :your-participant-id="onlineRoom?.yourParticipantId"
      :is-host="isHost"
      @game-over="onOnlineGameOver"
      @leave="leaveGame"
    />

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
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import auth from '../services/auth'
import activeRoom from '../services/activeRoom'
import passAndPlayState from '../services/passAndPlayState'
import navTrigger from '../services/navTrigger'
import ImposterGame from '../components/ImposterGame.vue'
import OnlineImposterGame from '../components/OnlineImposterGame.vue'

const stage = ref('modeChoice')
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
  lastGameWasOnline.value = false
  finalScores.value = [...scores].sort((a, b) => a[1] - b[1]) // fewest imposter hits wins
  stage.value = 'done'
}

function resetToStart() {
  gameGridIds.value = []
  chosenBoards.value = []
  playerNames.value = []
  finalScores.value = []
  stage.value = lastGameWasOnline.value ? 'modeChoice' : 'landing'
}

// --- Online multiplayer ---
const onlineNumBoards = ref(2)
const onlineDisplayName = ref(auth.state.displayName || '')
const lastGameWasOnline = ref(false)
const onlineBoardMode = ref('random')
const onlineChosenBoards = ref([])
const joinCode = ref('')
const onlineRoom = ref(null)
const creatingRoom = ref(false)
const joiningRoom = ref(false)
const startingRoom = ref(false)
let lobbyPollTimer = null

const isHost = computed(() => !!onlineRoom.value?.host)

function selectOnlineManual() {
  onlineBoardMode.value = 'manual'
  onlineChosenBoards.value = []
}

function toggleOnlineBoard(id) {
  if (onlineChosenBoards.value.includes(id)) {
    onlineChosenBoards.value = onlineChosenBoards.value.filter(g => g !== id)
  } else if (onlineChosenBoards.value.length < onlineNumBoards.value) {
    onlineChosenBoards.value.push(id)
  }
}

// Colors are auto-assigned (no picker) - just pick one not already visible in this browser tab's memory.
// Colors are auto-assigned (no picker) - picks one not already in use by
// another participant in the room, so two players never end up looking the
// same in the player row. Falls back to a fully random pick only if every
// palette color is somehow already taken.
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
    const payload = {
      gameType: 'IMPOSTER',
      displayName: onlineDisplayName.value.trim(),
      color: pickUnusedColor()
    }
    if (onlineBoardMode.value === 'manual') {
      payload.gridIds = onlineChosenBoards.value
    } else {
      payload.randomGridCount = onlineNumBoards.value
    }
    onlineRoom.value = await api.createRoom(payload)
    activeRoom.save(onlineRoom.value.roomCode, 'IMPOSTER')
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
    activeRoom.save(onlineRoom.value.roomCode, 'IMPOSTER')
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
  onlineChosenBoards.value = []
  onlineBoardMode.value = 'random'
  joinCode.value = ''
}

function onOnlineGameOver(scores) {
  activeRoom.clear()
  lastGameWasOnline.value = true
  finalScores.value = [...scores].sort((a, b) => a[1] - b[1]) // fewest imposter hits wins
  resetOnline()
  stage.value = 'done'
}

const savedRoomCode = ref('')
const rejoining = ref(false)

onMounted(() => {
  savedRoomCode.value = activeRoom.get('IMPOSTER') || ''
})

// Clicking the "Imposter" nav tab while already on this page doesn't trigger
// any navigation event on its own, so it needs its own trigger to jump back to
// the very first screen - mid-game state (pass-and-play or online) is simply
// left behind, same as if the tab had been closed and reopened.
watch(() => navTrigger.state.imposter, () => {
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
