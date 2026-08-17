<template>
  <div>
    <template v-if="stage === 'modeChoice'">
      <h1>Starting XI Battle</h1>
      <p class="page-subtitle">
        Take turns guessing - get it right and the shirt reveals (and you score a point),
        get it wrong and you lose a life. Play continues until a board is fully solved or everyone's out of lives.
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

      <details class="advanced-disclosure" style="margin-top:24px;">
        <summary>Not sure how it works? See an example</summary>
        <div style="margin-top:16px; padding:16px 20px; border:1px solid var(--border); border-radius:var(--radius-md); background:rgba(255,255,255,0.02);">
          <p style="margin-top:0;">
            The board is one team's starting lineup from a real match, e.g. <em>Arsenal's XI in the 2014 Community Shield</em>.
            On your turn, search for a player you think started that match and submit a guess.
          </p>
          <ul style="margin:0 0 14px; padding-left:20px; line-height:1.7;">
            <li><strong style="color:var(--teal);">Guess right</strong> - that shirt flips over to reveal the player, and you score a point.</li>
            <li><strong style="color:var(--coral);">Guess wrong</strong> - you lose one of your lives (5 by default).</li>
          </ul>
          <p style="margin-bottom:0;">
            Either way, the turn passes to the next player. A full game is 2-4 boards played back to back,
            with everyone's points added up across all of them - whoever has the highest total at the end wins.
          </p>
        </div>
      </details>
    </template>

    <template v-else-if="stage === 'landing'">
      <h1>Starting XI Battle</h1>
      <p class="page-subtitle">
        A pass-the-device multiplayer version of Starting XI. Take turns guessing -
        get it right and the shirt reveals (and you score a point), get it wrong and you lose a life.
        Play continues until a board is fully solved or everyone's out of lives.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>Players</label>
          <select v-model.number="numPlayers">
            <option v-for="n in [2,3,4,5]" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
        <div style="flex:1; min-width:160px;">
          <label>Boards</label>
          <select v-model.number="numLineups">
            <option v-for="n in [2,3,4]" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
      </div>

      <div class="field">
        <label>How should the boards be picked?</label>
        <div class="language-row">
          <button class="language-btn" :class="{ active: lineupMode === 'random' }" @click="lineupMode = 'random'">Random</button>
          <button class="language-btn" :class="{ active: lineupMode === 'manual' }" @click="lineupMode = 'manual'">Pick my own</button>
        </div>
      </div>

      <button class="btn btn-secondary" @click="stage = 'modeChoice'">← Back</button>
      <button class="btn btn-primary" style="margin-left:10px;" @click="goToSetup">Create game</button>
    </template>

    <template v-else-if="stage === 'onlineChoice'">
      <h1>Play online</h1>
      <p class="page-subtitle">Same game, different devices - share a room code with 1-3 friends instead of passing one phone around.</p>
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
        <select v-model.number="onlineNumLineups">
          <option v-for="n in [2,3,4]" :key="n" :value="n">{{ n }}</option>
        </select>
      </div>

      <div class="field">
        <label>How should the boards be picked?</label>
        <div class="language-row">
          <button class="language-btn" :class="{ active: onlineLineupMode === 'random' }" @click="onlineLineupMode = 'random'">Random</button>
          <button class="language-btn" :class="{ active: onlineLineupMode === 'manual' }" @click="selectOnlineManual">Pick my own</button>
        </div>
      </div>

      <div v-if="onlineLineupMode === 'manual'" class="field">
        <label>Pick exactly {{ onlineNumLineups }} board(s) <span class="picker-hint">{{ onlineChosenLineups.length }}/{{ onlineNumLineups }} chosen</span></label>
        <div v-if="loadingLineups" style="color:var(--text-dim);">Loading…</div>
        <div v-else class="saved-quiz-list">
          <div v-for="l in availableLineups" :key="l.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ l.title }}</div>
              <div class="saved-quiz-meta">{{ l.teamName }} vs {{ l.opponentName }} · {{ l.formation }}</div>
            </div>
            <button
              class="btn btn-sm"
              :class="onlineChosenLineups.includes(l.id) ? 'btn-primary' : 'btn-secondary'"
              @click="toggleOnlineLineup(l.id)"
            >{{ onlineChosenLineups.includes(l.id) ? 'Chosen ✓' : '+ Choose' }}</button>
          </div>
        </div>
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'onlineChoice'">← Back</button>
        <button class="btn btn-primary" :disabled="creatingRoom || !onlineDisplayName.trim() || (onlineLineupMode === 'manual' && onlineChosenLineups.length !== onlineNumLineups)" @click="createOnlineRoom">
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
        <button v-if="isHost" class="btn btn-primary" :disabled="(onlineRoom?.participants || []).length < 2 || startingRoom" @click="startOnlineRoom">
          {{ startingRoom ? 'Starting…' : 'Start game' }}
        </button>
        <span v-else style="color:var(--text-dim); align-self:center;">Waiting for the host to start…</span>
      </div>
    </template>

    <OnlineStartingXiBattleGame
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
        <button class="btn btn-primary" :disabled="!allNamed || duplicateNames" @click="goToLineupChoice">
          Next →
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'pickLineups'">
      <h1>Choose {{ numLineups }} board{{ numLineups > 1 ? 's' : '' }}</h1>
      <p class="page-subtitle">{{ chosenLineups.length }} / {{ numLineups }} selected</p>

      <div v-if="loadingLineups" style="color:var(--text-dim);">Loading…</div>
      <div v-else-if="!availableLineups.length" class="empty-state">No boards available yet.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="l in availableLineups" :key="l.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ l.title }}</div>
            <div class="saved-quiz-meta">{{ l.teamName }} vs {{ l.opponentName }} · {{ l.formation }}</div>
          </div>
          <button
            class="btn btn-sm"
            :class="isChosen(l) ? 'btn-primary' : 'btn-secondary'"
            :disabled="!isChosen(l) && chosenLineups.length >= numLineups"
            @click="toggleChosen(l)"
          >{{ isChosen(l) ? 'Selected ✓' : '+ Select' }}</button>
        </div>
      </div>

      <div style="display:flex; gap:12px; margin-top:20px;">
        <button class="btn btn-secondary" @click="stage = 'setup'">← Back</button>
        <button class="btn btn-primary" :disabled="chosenLineups.length !== numLineups" @click="startGame">
          Start game ({{ chosenLineups.length }}/{{ numLineups }})
        </button>
      </div>
    </template>

    <MultiplayerLineupGame
      v-else-if="stage === 'game'"
      :lineups="gameLineups"
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
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import auth from '../services/auth'
import activeRoom from '../services/activeRoom'
import passAndPlayState from '../services/passAndPlayState'
import navTrigger from '../services/navTrigger'
import MultiplayerLineupGame from '../components/MultiplayerLineupGame.vue'
import OnlineStartingXiBattleGame from '../components/OnlineStartingXiBattleGame.vue'

const colorOptions = ['#4f46e5', '#F22C05', '#F2BB05', '#032E8A', '#05D6F2', '#f43f5e', '#5D038A', '#22c55e']

const stage = ref('modeChoice')
const error = ref('')
const numPlayers = ref(2)
const numLineups = ref(2)
const lineupMode = ref('random')

const setupPlayers = reactive([])

const savedPassAndPlay = ref(null)
onMounted(() => {
  savedPassAndPlay.value = passAndPlayState.load('starting-xi-battle')
})

function resumePassAndPlay() {
  const saved = savedPassAndPlay.value
  gameLineups.value = saved.gameLineups
  setupPlayers.length = 0
  saved.players.forEach(p => setupPlayers.push(p))
  lastGameWasOnline.value = false
  stage.value = 'game'
}

function dismissPassAndPlay() {
  passAndPlayState.clear('starting-xi-battle')
  passAndPlayState.clear('starting-xi-battle-progress')
  savedPassAndPlay.value = null
}

const availableLineups = ref([])
const loadingLineups = ref(false)
const chosenLineups = ref([])
const gameLineups = ref([])
const finalScores = ref([])

function rebuildSetupPlayers() {
  setupPlayers.length = 0
  const shuffled = [...colorOptions].sort(() => Math.random() - 0.5)
  for (let i = 0; i < numPlayers.value; i++) {
    setupPlayers.push({ name: '', color: shuffled[i % shuffled.length] })
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

// The plain /lineups list includes boards an admin has excluded from battle
// (they still need to stay solo-playable) - battle picking, random or manual,
// should never surface those, same reasoning as Grid Battle's dedicated
// getBattleEligibleGrids() endpoint.
async function loadAvailableLineups() {
  loadingLineups.value = true
  try {
    const all = await api.listLineups()
    availableLineups.value = all.filter(l => !l.excludedFromBattle)
  } catch (e) {
    error.value = 'Could not load Starting XI boards.'
  } finally {
    loadingLineups.value = false
  }
}

async function goToLineupChoice() {
  error.value = ''
  if (lineupMode.value === 'random') {
    await pickRandomLineups()
    if (gameLineups.value.length < numLineups.value) {
      error.value = `Only found ${gameLineups.value.length} board(s) - need at least ${numLineups.value}. Try picking your own, or ask an admin to add more boards.`
      stage.value = 'landing'
      return
    }
    passAndPlayState.save('starting-xi-battle', { gameLineups: gameLineups.value, players: [...setupPlayers] })
    savedPassAndPlay.value = passAndPlayState.load('starting-xi-battle')
    stage.value = 'game'
  } else {
    chosenLineups.value = []
    stage.value = 'pickLineups'
    await loadAvailableLineups()
  }
}

async function pickRandomLineups() {
  try {
    const all = await api.listLineups()
    const pool = all.filter(l => !l.excludedFromBattle)
    const shuffled = [...pool].sort(() => Math.random() - 0.5)
    gameLineups.value = shuffled.slice(0, numLineups.value)
  } catch (e) {
    gameLineups.value = []
  }
}

function isChosen(l) {
  return chosenLineups.value.some(c => c.id === l.id)
}
function toggleChosen(l) {
  if (isChosen(l)) {
    chosenLineups.value = chosenLineups.value.filter(c => c.id !== l.id)
  } else if (chosenLineups.value.length < numLineups.value) {
    chosenLineups.value.push(l)
  }
}

function startGame() {
  gameLineups.value = chosenLineups.value
  passAndPlayState.save('starting-xi-battle', { gameLineups: gameLineups.value, players: [...setupPlayers] })
  savedPassAndPlay.value = passAndPlayState.load('starting-xi-battle')
  stage.value = 'game'
}

const sortedScores = computed(() => [...finalScores.value].sort((a, b) => b[1] - a[1]))

function onGameOver(scores) {
  passAndPlayState.clear('starting-xi-battle')
  passAndPlayState.clear('starting-xi-battle-progress')
  savedPassAndPlay.value = null
  lastGameWasOnline.value = false
  finalScores.value = scores
  stage.value = 'done'
}

function resetGame() {
  passAndPlayState.clear('starting-xi-battle')
  passAndPlayState.clear('starting-xi-battle-progress')
  savedPassAndPlay.value = null
  gameLineups.value = []
  finalScores.value = []
  stage.value = lastGameWasOnline.value ? 'modeChoice' : 'landing'
}

// --- Online multiplayer ---
const onlineNumLineups = ref(2)
const onlineDisplayName = ref(auth.state.displayName || '')
const lastGameWasOnline = ref(false)
const onlineLineupMode = ref('random')
const onlineChosenLineups = ref([])
const joinCode = ref('')
const onlineRoom = ref(null)
const creatingRoom = ref(false)
const joiningRoom = ref(false)
const startingRoom = ref(false)
let lobbyPollTimer = null

const isHost = computed(() => !!onlineRoom.value?.host)

function selectOnlineManual() {
  onlineLineupMode.value = 'manual'
  onlineChosenLineups.value = []
  if (!availableLineups.value.length) loadAvailableLineups()
}

function toggleOnlineLineup(id) {
  if (onlineChosenLineups.value.includes(id)) {
    onlineChosenLineups.value = onlineChosenLineups.value.filter(l => l !== id)
  } else if (onlineChosenLineups.value.length < onlineNumLineups.value) {
    onlineChosenLineups.value.push(id)
  }
}

function pickUnusedColor(existingColors = []) {
  const available = colorOptions.filter(c => !existingColors.includes(c))
  const pool = available.length ? available : colorOptions
  return pool[Math.floor(Math.random() * pool.length)]
}

async function createOnlineRoom() {
  error.value = ''
  creatingRoom.value = true
  try {
    const payload = {
      gameType: 'STARTING_XI_BATTLE',
      displayName: onlineDisplayName.value.trim(),
      color: pickUnusedColor()
    }
    if (onlineLineupMode.value === 'manual') {
      payload.lineupIds = onlineChosenLineups.value
    } else {
      payload.randomLineupCount = onlineNumLineups.value
    }
    onlineRoom.value = await api.createRoom(payload)
    activeRoom.save(onlineRoom.value.roomCode, 'STARTING_XI_BATTLE')
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
      // pre-check failing isn't fatal - joining below surfaces the real error
    }
    onlineRoom.value = await api.joinRoom(code, {
      displayName: onlineDisplayName.value.trim(),
      color: pickUnusedColor(existingColors)
    })
    activeRoom.save(onlineRoom.value.roomCode, 'STARTING_XI_BATTLE')
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
  onlineChosenLineups.value = []
  onlineLineupMode.value = 'random'
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
  savedRoomCode.value = activeRoom.get('STARTING_XI_BATTLE') || ''
})

// Clicking the "Starting XI" nav tab while already on this page doesn't
// trigger any navigation event on its own, so it needs its own trigger to
// jump back to the very first screen - mid-game state (pass-and-play or
// online) is simply left behind, same as if the tab had been closed and
// reopened.
watch(() => navTrigger.state.startingXiBattle, () => {
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
