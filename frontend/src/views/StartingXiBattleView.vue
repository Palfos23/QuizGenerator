<template>
  <div>
    <template v-if="stage === 'landing'">
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

      <div style="display:flex; gap:12px; flex-wrap:wrap; margin-top:20px;">
        <button class="btn btn-primary" @click="stage = 'create'">+ Create a room</button>
        <button class="btn btn-secondary" @click="stage = 'join'">Join with a code</button>
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

    <template v-else-if="stage === 'create'">
      <h1>Create a room</h1>
      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Your name <span class="picker-hint">shown to other players</span></label>
        <input type="text" v-model="displayName" placeholder="Your name" />
      </div>

      <div class="field">
        <label>Boards <span class="picker-hint">2-4</span></label>
        <select v-model.number="numLineups">
          <option v-for="n in [2,3,4]" :key="n" :value="n">{{ n }}</option>
        </select>
      </div>

      <div class="field">
        <label>How should the boards be picked?</label>
        <div class="language-row">
          <button class="language-btn" :class="{ active: lineupMode === 'random' }" @click="lineupMode = 'random'">Random</button>
          <button class="language-btn" :class="{ active: lineupMode === 'manual' }" @click="selectManual">Pick my own</button>
        </div>
      </div>

      <div v-if="lineupMode === 'manual'" class="field">
        <label>Pick exactly {{ numLineups }} board(s) <span class="picker-hint">{{ chosenLineups.length }}/{{ numLineups }} chosen</span></label>
        <div v-if="loadingLineups" style="color:var(--text-dim);">Loading…</div>
        <div v-else class="saved-quiz-list">
          <div v-for="l in availableLineups" :key="l.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ l.title }}</div>
              <div class="saved-quiz-meta">{{ l.teamName }} vs {{ l.opponentName }} · {{ l.formation }}</div>
            </div>
            <button
              class="btn btn-sm"
              :class="chosenLineups.includes(l.id) ? 'btn-primary' : 'btn-secondary'"
              @click="toggleLineup(l.id)"
            >{{ chosenLineups.includes(l.id) ? 'Chosen ✓' : '+ Choose' }}</button>
          </div>
        </div>
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'landing'">← Back</button>
        <button class="btn btn-primary" :disabled="creatingRoom || !displayName.trim() || (lineupMode === 'manual' && chosenLineups.length !== numLineups)" @click="createRoom">
          {{ creatingRoom ? 'Creating…' : 'Create room' }}
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'join'">
      <h1>Join a room</h1>
      <div v-if="error" class="banner error">{{ error }}</div>
      <div class="field">
        <label>Your name <span class="picker-hint">shown to other players</span></label>
        <input type="text" v-model="displayName" placeholder="Your name" />
      </div>
      <div class="field">
        <label>Room code</label>
        <input type="text" v-model="joinCode" placeholder="e.g. ABCDE" style="text-transform:uppercase; letter-spacing:0.1em; font-size:1.2rem; text-align:center;" maxlength="5" />
      </div>
      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'landing'">← Back</button>
        <button class="btn btn-primary" :disabled="!joinCode.trim() || !displayName.trim() || joiningRoom" @click="joinRoom()">
          {{ joiningRoom ? 'Joining…' : 'Join' }}
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'lobby'">
      <h1>Room {{ room?.roomCode }}</h1>
      <p class="page-subtitle">Share this code with your friends. Everyone needs to join before the host starts.</p>
      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="saved-quiz-list" style="max-width:420px;">
        <div v-for="p in room?.participants || []" :key="p.id" class="saved-quiz-row">
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
        {{ (room?.participants || []).length }} joined · need at least 2 to start
      </p>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="leaveLobby">← Leave</button>
        <button v-if="isHost" class="btn btn-primary" :disabled="(room?.participants || []).length < 2 || startingRoom" @click="startRoomGame">
          {{ startingRoom ? 'Starting…' : 'Start game' }}
        </button>
        <span v-else style="color:var(--text-dim); align-self:center;">Waiting for the host to start…</span>
      </div>
    </template>

    <OnlineStartingXiBattleGame
      v-else-if="stage === 'game'"
      :room-code="room?.roomCode"
      :your-participant-id="room?.yourParticipantId"
      :is-host="isHost"
      @game-over="onGameOver"
      @leave="leaveGame"
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
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import auth from '../services/auth'
import activeRoom from '../services/activeRoom'
import navTrigger from '../services/navTrigger'
import OnlineStartingXiBattleGame from '../components/OnlineStartingXiBattleGame.vue'

const colorOptions = ['#4f46e5', '#F22C05', '#F2BB05', '#032E8A', '#05D6F2', '#f43f5e', '#5D038A', '#22c55e']

const stage = ref('landing')
const error = ref('')
const displayName = ref(auth.state.displayName || '')
const numLineups = ref(2)
const lineupMode = ref('random')
const chosenLineups = ref([])
const availableLineups = ref([])
const loadingLineups = ref(false)
const joinCode = ref('')
const room = ref(null)
const creatingRoom = ref(false)
const joiningRoom = ref(false)
const startingRoom = ref(false)
const finalScores = ref([])
let lobbyPollTimer = null

const isHost = computed(() => !!room.value?.host)
const sortedScores = computed(() => [...finalScores.value].sort((a, b) => b[1] - a[1]))

function pickUnusedColor(existingColors = []) {
  const available = colorOptions.filter(c => !existingColors.includes(c))
  const pool = available.length ? available : colorOptions
  return pool[Math.floor(Math.random() * pool.length)]
}

async function loadAvailableLineups() {
  loadingLineups.value = true
  try {
    availableLineups.value = await api.listLineups()
  } catch (e) {
    error.value = 'Could not load Starting XI boards.'
  } finally {
    loadingLineups.value = false
  }
}

function selectManual() {
  lineupMode.value = 'manual'
  chosenLineups.value = []
  if (!availableLineups.value.length) loadAvailableLineups()
}

function toggleLineup(id) {
  if (chosenLineups.value.includes(id)) {
    chosenLineups.value = chosenLineups.value.filter(l => l !== id)
  } else if (chosenLineups.value.length < numLineups.value) {
    chosenLineups.value.push(id)
  }
}

async function createRoom() {
  error.value = ''
  creatingRoom.value = true
  try {
    const payload = {
      gameType: 'STARTING_XI_BATTLE',
      displayName: displayName.value.trim(),
      color: pickUnusedColor()
    }
    if (lineupMode.value === 'manual') {
      payload.lineupIds = chosenLineups.value
    } else {
      payload.randomLineupCount = numLineups.value
    }
    room.value = await api.createRoom(payload)
    activeRoom.save(room.value.roomCode, 'STARTING_XI_BATTLE')
    stage.value = 'lobby'
    startLobbyPolling()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not create the room.'
  } finally {
    creatingRoom.value = false
  }
}

async function joinRoom(codeOverride) {
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
    room.value = await api.joinRoom(code, {
      displayName: displayName.value.trim(),
      color: pickUnusedColor(existingColors)
    })
    activeRoom.save(room.value.roomCode, 'STARTING_XI_BATTLE')
    if (room.value.status === 'IN_PROGRESS') {
      stage.value = 'game'
    } else {
      stage.value = 'lobby'
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
    if (!room.value) return
    try {
      const updated = await api.getRoom(room.value.roomCode)
      room.value = updated
      if (updated.status === 'IN_PROGRESS') {
        clearInterval(lobbyPollTimer)
        stage.value = 'game'
      }
    } catch (e) {
      // a transient poll failure isn't worth surfacing - it'll succeed next tick
    }
  }, 2000)
}

async function startRoomGame() {
  error.value = ''
  startingRoom.value = true
  try {
    await api.startRoom(room.value.roomCode)
    clearInterval(lobbyPollTimer)
    stage.value = 'game'
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not start the game.'
  } finally {
    startingRoom.value = false
  }
}

function leaveLobby() {
  clearInterval(lobbyPollTimer)
  activeRoom.clear()
  resetRoomState()
  stage.value = 'landing'
}

function leaveGame() {
  activeRoom.clear()
  resetRoomState()
  stage.value = 'landing'
}

function resetRoomState() {
  room.value = null
  chosenLineups.value = []
  lineupMode.value = 'random'
  joinCode.value = ''
}

function onGameOver(scores) {
  activeRoom.clear()
  finalScores.value = scores
  resetRoomState()
  stage.value = 'done'
}

function resetGame() {
  finalScores.value = []
  stage.value = 'landing'
}

const savedRoomCode = ref('')
const rejoining = ref(false)

onMounted(() => {
  savedRoomCode.value = activeRoom.get('STARTING_XI_BATTLE') || ''
})

watch(() => navTrigger.state.startingXiBattle, () => {
  clearInterval(lobbyPollTimer)
  stage.value = 'landing'
})

function dismissSavedRoom() {
  activeRoom.clear()
  savedRoomCode.value = ''
}

async function rejoinSavedRoom() {
  rejoining.value = true
  const code = savedRoomCode.value
  savedRoomCode.value = ''
  await joinRoom(code)
  rejoining.value = false
}
</script>
