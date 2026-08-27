<template>
  <GameAccessGate game="501">
  <div>
    <template v-if="stage === 'modeChoice'">
      <h1>501</h1>
      <p class="page-subtitle">
        Start at 501, subtract your throw's value, first to checkout between 0 and -10 wins.
        Darts scoring applies - anything over 180, or one of the nine unreachable checkout numbers, scores zero.
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

      <FiveOhOnePreview />

      <div class="mode-choice-row">
        <button class="mode-choice-card" @click="stage = 'category'">
          <h3>Same device</h3>
          <p>Pass the phone around - one throw each, back and forth.</p>
        </button>
        <button class="mode-choice-card" @click="stage = 'onlineChoice'">
          <h3>Play online</h3>
          <p>You and one friend, each on your own device with a shared room code.</p>
        </button>
      </div>

      <details class="advanced-disclosure" style="margin-top:24px;">
        <summary>Not sure how it works? See an example</summary>
        <div style="margin-top:16px; padding:16px 20px; border:1px solid var(--border); border-radius:var(--radius-md); background:rgba(255,255,255,0.02);">
          <p style="margin-top:0;">
            Say the category is "Premier League appearances." You're on 501, and you search for a player -
            say one with <strong>136</strong> appearances. That's under 180 and not one of the unreachable numbers,
            so it scores in full: you're now on <strong>365</strong>.
          </p>
          <p>
            Later in the game you're sitting on <strong>184</strong>. You throw a player with <strong>212</strong>
            appearances - too high to count in darts (max is 180), so it scores <strong>zero</strong> and your total
            doesn't move. Another throw of exactly <strong>172</strong> would also score zero - it's one of the nine
            numbers that can't be hit with three darts, checkout-wise.
          </p>
          <p style="margin-bottom:0;">
            Once you're close, say you're on <strong>8</strong> and you throw a player with <strong>15</strong> appearances -
            that would take you to <strong>-7</strong>, past zero but still within the -10 window, so you land safely
            and you're "in the window." If instead you'd thrown a value that took you below -10, that throw busts -
            your total reverts to what it was before.
          </p>
        </div>
      </details>
    </template>

    <template v-else-if="stage === 'category'">
      <h1>501</h1>
      <p class="page-subtitle">501 is strictly 1v1, passing the device between throws.</p>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
      <div v-else-if="!categories.length" class="empty-state friendly">No categories yet - ask an admin to add one.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="c in categories" :key="c.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ c.title }}</div>
            <div class="saved-quiz-meta">{{ c.entryCount }} entries<span v-if="c.description"> · {{ c.description }}</span></div>
          </div>
          <button class="btn btn-primary btn-sm" @click="chooseCategory(c.id)">Play</button>
        </div>
      </div>

      <button class="btn btn-secondary" style="margin-top:16px;" @click="stage = 'modeChoice'">← Back</button>
    </template>

    <template v-else-if="stage === 'setup'">
      <h1>Name the players</h1>
      <p class="page-subtitle">501 is strictly 1v1.</p>
      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Player 1</label>
        <input type="text" v-model="playerNames[0]" placeholder="Player 1" />
      </div>
      <div class="field">
        <label>Player 2</label>
        <input type="text" v-model="playerNames[1]" placeholder="Player 2" />
      </div>

      <button class="btn btn-secondary" @click="stage = 'category'">← Back</button>
      <button class="btn btn-primary" style="margin-left:10px;" :disabled="!bothNamed" @click="startGame">Start game</button>
    </template>

    <template v-else-if="stage === 'onlineChoice'">
      <h1>Play online</h1>
      <p class="page-subtitle">Same game, different devices - you and one friend, sharing a room code.</p>
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
        <label>Your name <span class="picker-hint">shown to the other player</span></label>
        <input type="text" v-model="onlineDisplayName" placeholder="Your name" />
      </div>

      <div class="field">
        <label>Category</label>
        <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
        <div v-else class="saved-quiz-list">
          <div v-for="c in categories" :key="c.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ c.title }}</div>
              <div class="saved-quiz-meta">{{ c.entryCount }} entries<span v-if="c.description"> · {{ c.description }}</span></div>
            </div>
            <button
              class="btn btn-sm"
              :class="onlineCategoryId === c.id ? 'btn-primary' : 'btn-secondary'"
              @click="onlineCategoryId = c.id"
            >{{ onlineCategoryId === c.id ? 'Chosen ✓' : '+ Choose' }}</button>
          </div>
        </div>
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'onlineChoice'">← Back</button>
        <button class="btn btn-primary" :disabled="creatingRoom || !onlineDisplayName.trim() || !onlineCategoryId" @click="createOnlineRoom">
          {{ creatingRoom ? 'Creating…' : 'Create room' }}
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'onlineJoin'">
      <h1>Join a room</h1>
      <div v-if="error" class="banner error">{{ error }}</div>
      <div class="field">
        <label>Your name <span class="picker-hint">shown to the other player</span></label>
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
      <p class="page-subtitle">Share this code with your friend. Both players need to join before the host starts.</p>
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
        {{ (onlineRoom?.participants || []).length }} joined · need exactly 2 to start
      </p>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="leaveLobby">← Leave</button>
        <button
          v-if="isHost"
          class="btn btn-primary"
          :disabled="(onlineRoom?.participants || []).length !== 2 || startingRoom"
          @click="startOnlineRoom"
        >{{ startingRoom ? 'Starting…' : 'Start game' }}</button>
        <span v-else style="color:var(--text-dim); align-self:center;">Waiting for the host to start…</span>
      </div>
    </template>

    <OnlineFiveOhOneGame
      v-else-if="stage === 'onlineGame'"
      :room-code="onlineRoom?.roomCode"
      :your-participant-id="onlineRoom?.yourParticipantId"
      @game-over="onOnlineGameOver"
      @leave="leaveGame"
    />

    <FiveOhOneGame
      v-else-if="stage === 'game'"
      :category="chosenCategory"
      :players="playerNames"
      @game-over="onGameOver"
    />

  </div>
  </GameAccessGate>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import auth from '../services/auth'
import activeRoom from '../services/activeRoom'
import passAndPlayState from '../services/passAndPlayState'
import navTrigger from '../services/navTrigger'
import FiveOhOneGame from '../components/FiveOhOneGame.vue'
import OnlineFiveOhOneGame from '../components/OnlineFiveOhOneGame.vue'
import GameAccessGate from '../components/GameAccessGate.vue'
import FiveOhOnePreview from '../components/previews/FiveOhOnePreview.vue'

const stage = ref('modeChoice')
const error = ref('')
const loading = ref(true)
const categories = ref([])
const chosenCategory = ref(null)
const playerNames = ref(['', ''])

const bothNamed = computed(() => playerNames.value[0].trim().length > 0 && playerNames.value[1].trim().length > 0)

onMounted(loadCategories)

const savedPassAndPlay = ref(null)
onMounted(() => {
  savedPassAndPlay.value = passAndPlayState.load('501')
})

function resumePassAndPlay() {
  const saved = savedPassAndPlay.value
  chosenCategory.value = saved.category
  playerNames.value = saved.players
  stage.value = 'game'
}

function dismissPassAndPlay() {
  passAndPlayState.clear('501')
  passAndPlayState.clear('501-progress')
  savedPassAndPlay.value = null
}

async function loadCategories() {
  loading.value = true
  error.value = ''
  try {
    categories.value = await api.listFiveOhOneCategories()
  } catch (e) {
    error.value = 'Could not load categories.'
  } finally {
    loading.value = false
  }
}

async function chooseCategory(id) {
  error.value = ''
  try {
    chosenCategory.value = await api.getFiveOhOneCategory(id)
    stage.value = 'setup'
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not load that category.'
  }
}

function startGame() {
  playerNames.value = [playerNames.value[0].trim(), playerNames.value[1].trim()]
  passAndPlayState.save('501', { category: chosenCategory.value, players: playerNames.value })
  savedPassAndPlay.value = passAndPlayState.load('501')
  stage.value = 'game'
}

function onGameOver() {
  passAndPlayState.clear('501')
  passAndPlayState.clear('501-progress')
  savedPassAndPlay.value = null
  lastGameWasOnline.value = false
  resetToStart()
}

function resetToStart() {
  chosenCategory.value = null
  playerNames.value = ['', '']
  stage.value = lastGameWasOnline.value ? 'modeChoice' : 'category'
}

// --- Online multiplayer ---
const onlineDisplayName = ref(auth.state.displayName || '')
const lastGameWasOnline = ref(false)
const onlineCategoryId = ref(null)
const joinCode = ref('')
const onlineRoom = ref(null)
const creatingRoom = ref(false)
const joiningRoom = ref(false)
const startingRoom = ref(false)
let lobbyPollTimer = null

const isHost = computed(() => !!onlineRoom.value?.host)

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
    onlineRoom.value = await api.createRoom({
      gameType: 'FIVE_O_ONE',
      displayName: onlineDisplayName.value.trim(),
      color: pickUnusedColor(),
      fiveOhOneCategoryId: onlineCategoryId.value
    })
    activeRoom.save(onlineRoom.value.roomCode, 'FIVE_O_ONE')
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
    activeRoom.save(onlineRoom.value.roomCode, 'FIVE_O_ONE')
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
  onlineCategoryId.value = null
  joinCode.value = ''
}

function onOnlineGameOver() {
  activeRoom.clear()
  lastGameWasOnline.value = true
  resetOnline()
  resetToStart()
}

const savedRoomCode = ref('')
const rejoining = ref(false)

onMounted(() => {
  savedRoomCode.value = activeRoom.get('FIVE_O_ONE') || ''
})

// Clicking the "501" nav tab while already on this page doesn't trigger any
// navigation event on its own, so it needs its own trigger to jump back to
// the very first screen - mid-game state (pass-and-play or online) is simply
// left behind, same as if the tab had been closed and reopened.
watch(() => navTrigger.state.fiveOhOne, () => {
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
