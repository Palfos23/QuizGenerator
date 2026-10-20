<template>
  <GameAccessGate game="flashback">
  <div>
    <div v-if="onlineRoom && (stage === 'onlineLobby' || stage === 'onlineGame')" style="display:flex; justify-content:flex-end; margin-bottom:8px;">
      <button class="btn btn-secondary btn-sm" @click="openPlayersModal">Administrer spillere</button>
    </div>

    <template v-if="stage === 'modeChoice'">
      <h1>Flashback</h1>
      <p class="page-subtitle">
        Gjett det nøyaktige året ut fra et hint - det er vanskelig først, og blir lettere for hver runde ingen treffer.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="savedRoomCode" class="banner" style="background:rgba(216, 67, 43, 0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>Du har et spill i gang i rom <strong>{{ savedRoomCode }}</strong>.</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" :disabled="rejoining" @click="rejoinSavedRoom">
            {{ rejoining ? 'Går inn igjen…' : 'Gå inn igjen' }}
          </button>
          <button class="btn btn-secondary btn-sm" @click="dismissSavedRoom">Lukk</button>
        </div>
      </div>

      <div v-if="savedPassAndPlay" class="banner" style="background:rgba(216, 67, 43, 0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>Du har et spill der enheten sendes rundt, i gang.</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" @click="resumePassAndPlay">Fortsett</button>
          <button class="btn btn-secondary btn-sm" @click="dismissPassAndPlay">Lukk</button>
        </div>
      </div>

      <div class="game-landing-hero">
        <FlashbackPreview />

        <div class="mode-choice-row">
          <button class="mode-choice-card" @click="stage = 'landing'">
            <h3>Samme enhet</h3>
            <p>Send telefonen rundt - alle tar turen sin på én skjerm.</p>
          </button>
          <button class="mode-choice-card" @click="stage = 'onlineChoice'">
            <h3>Spill på nett</h3>
            <p>Alle spiller fra sin egen enhet med en felles romkode.</p>
          </button>
        </div>
      </div>

      <details class="advanced-disclosure" style="margin-top:24px;">
        <summary>Usikker på hvordan det fungerer? Se et eksempel</summary>
        <div style="margin-top:16px; padding:16px 20px; border:1px solid var(--border); border-radius: 2px; background:rgba(27, 26, 23, 0.06);">
          <p style="margin-top:0;">
            Hver runde skjuler et ekte år bak opptil 5 hint, vist ett om gangen - vanskeligste først, letteste sist. Etter det første hintet bytter spillerne på å gjette ett år hver. Du må treffe det <strong>nøyaktig</strong> for å få poeng - og jo tidligere du treffer, jo mer er det verdt: med 4 hint er riktig gjetning på hint 1 verdt 4 poeng, hint 2 verdt 3, og så videre ned til 1 poeng på det siste hintet.
          </p>
          <ul style="margin:0 0 14px; padding-left:20px; line-height:1.7;">
            <li>Ingen riktig? Neste hint avsløres, og gjettingen fortsetter - år som allerede er prøvd forblir synlige for alle.</li>
            <li>Fortsatt ingen på det siste hintet? Den som er <strong>nærmest i den siste gjetningsrunden</strong> får poenget for det siste hintet i stedet - ved likt får alle på delt plass full poengsum.</li>
          </ul>
          <p style="margin-bottom:0;">
            Høyest totalsum over alle rundene vinner spillet.
          </p>
        </div>
      </details>
    </template>

    <template v-else-if="stage === 'landing'">
      <h1>Flashback</h1>
      <p class="page-subtitle">
        En selskapsquiz der enheten sendes rundt. Gjett det nøyaktige året ut fra et hint - det er vanskelig først, og blir lettere for hver runde ingen treffer.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>Spillere
            <select v-model.number="numPlayers">
              <option v-for="n in playerCountOptions" :key="n" :value="n">{{ n }}</option>
            </select>
          </label>
        </div>
        <div style="flex:1; min-width:160px;">
          <label>Runder
            <select v-model.number="numRounds">
              <option v-for="n in 10" :key="n" :value="n">{{ n }}</option>
            </select>
          </label>
        </div>
      </div>

      <button class="btn btn-secondary" @click="stage = 'modeChoice'">← Tilbake</button>
      <button class="btn btn-primary" style="margin-left:10px;" @click="goToSetup">Opprett spill</button>
    </template>

    <template v-else-if="stage === 'onlineChoice'">
      <h1>Spill på nett</h1>
      <p class="page-subtitle">Samme spill, ulike enheter - del en romkode med venner i stedet for å sende én telefon rundt.</p>
      <div v-if="error" class="banner error">{{ error }}</div>
      <div style="display:flex; gap:12px; flex-wrap:wrap;">
        <button class="btn btn-primary" @click="stage = 'onlineCreate'">+ Opprett et rom</button>
        <button class="btn btn-secondary" @click="stage = 'onlineJoin'">Bli med med kode</button>
      </div>
      <button class="btn btn-secondary" style="margin-top:16px;" @click="stage = 'modeChoice'">← Tilbake</button>
    </template>

    <template v-else-if="stage === 'onlineCreate'">
      <h1>Opprett et rom</h1>
      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Navnet ditt <span class="picker-hint">vises for andre spillere</span><input type="text" v-model="onlineDisplayName" placeholder="Navnet ditt" /></label>
      </div>
      <div class="field" style="max-width:200px;">
        <label>Runder
          <select v-model.number="onlineNumRounds">
            <option v-for="n in 10" :key="n" :value="n">{{ n }}</option>
          </select>
        </label>
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'onlineChoice'">← Tilbake</button>
        <button class="btn btn-primary" :disabled="creatingRoom || !onlineDisplayName.trim()" @click="createOnlineRoom">
          {{ creatingRoom ? 'Oppretter…' : 'Opprett rom' }}
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'onlineJoin'">
      <h1>Bli med i et rom</h1>
      <div v-if="error" class="banner error">{{ error }}</div>
      <div class="field">
        <label>Navnet ditt <span class="picker-hint">vises for andre spillere</span><input type="text" v-model="onlineDisplayName" placeholder="Navnet ditt" /></label>
      </div>
      <div class="field">
        <label>Romkode<input type="text" v-model="joinCode" placeholder="f.eks. ABCDE" style="text-transform:uppercase; letter-spacing:0.1em; font-size:1.2rem; text-align:center;" maxlength="5" /></label>
      </div>
      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'onlineChoice'">← Tilbake</button>
        <button class="btn btn-primary" :disabled="!joinCode.trim() || !onlineDisplayName.trim() || joiningRoom" @click="joinOnlineRoom()">
          {{ joiningRoom ? 'Blir med…' : 'Bli med' }}
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'onlineLobby'">
      <h1>Rom {{ onlineRoom?.roomCode }}</h1>
      <p class="page-subtitle">Del denne koden med vennene dine. Alle må bli med før verten starter.</p>
      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="saved-quiz-list" style="max-width:420px;">
        <div v-for="p in onlineRoom?.participants || []" :key="p.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              <span style="display:inline-block; width:10px; height:10px; border-radius:50%; margin-right:6px;" :style="{ background: p.color }"></span>
              {{ p.displayName }}
            </div>
          </div>
          <span v-if="p.connected === false && p.id !== onlineRoom?.yourParticipantId" class="tag offline">Frakoblet</span>
            <span v-else class="tag" :style="{ background: 'rgba(31, 122, 80, 0.15)', color: 'var(--teal)' }">I rommet</span>
        </div>
      </div>

      <p style="color:var(--text-dim); font-size:0.9rem; margin-top:16px;">
        {{ (onlineRoom?.participants || []).length }} har blitt med · minst 2 trengs for å starte
      </p>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="leaveLobby">← Forlat</button>
        <InviteLinkButton v-if="onlineRoom" :room-code="onlineRoom.roomCode" />
        <button
          v-if="isHost"
          class="btn btn-primary"
          :disabled="(onlineRoom?.participants || []).length < 2 || startingRoom"
          @click="startOnlineRoom"
        >{{ startingRoom ? 'Starter…' : 'Start spillet' }}</button>
        <span v-else style="color:var(--text-dim); align-self:center;">Venter på at verten skal starte…</span>
      </div>
    </template>

    <OnlineFlashbackGame
      v-else-if="stage === 'onlineGame'"
      :room-code="onlineRoom?.roomCode"
      :your-participant-id="onlineRoom?.yourParticipantId"
      :is-host="isHost"
      @game-over="onOnlineGameOver"
      @leave="leaveGame"
    />

    <template v-else-if="stage === 'setup'">
      <h1>Hvem spiller?</h1>
      <p class="page-subtitle" v-if="duplicateNames">To spillere kan ikke ha samme navn.</p>

      <div v-for="(p, i) in setupPlayers" :key="i" class="field">
        <input type="text" v-model="p.name" :placeholder="`Spiller ${i + 1}`" />
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'landing'">← Tilbake</button>
        <button class="btn btn-primary" :disabled="!allNamed || duplicateNames" @click="startGame">
          Start spillet
        </button>
      </div>
    </template>

    <FlashbackGame
      v-else-if="stage === 'game'"
      :round-count="numRounds"
      :players="setupPlayers"
      @game-over="onGameOver"
    />

    <template v-else-if="stage === 'done'">
      <div class="modal-backdrop">
        <div class="completion-popup" :class="{ 'podium-host': setupPlayers.length > 2 }">
          <h2 style="margin-top:0;">Spillet er over!</h2>

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

          <template v-if="lastGameWasOnline && onlineRoom">
            <p class="page-subtitle" style="margin:16px 0 0;">
              {{ isHost ? `Rom ${onlineRoom.roomCode} er fortsatt åpent - spill igjen med samme gruppe, eller forlat det.` : 'Venter på at verten skal starte en ny runde, eller forlat rommet.' }}
            </p>
            <div style="display:flex; gap:10px; justify-content:center; flex-wrap:wrap; margin-top:12px;">
              <button v-if="isHost" class="btn btn-primary" :disabled="restartingRoom" @click="restartOnlineRoom">
                {{ restartingRoom ? 'Starter på nytt…' : 'Spill igjen' }}
              </button>
              <button class="btn btn-secondary" @click="leaveLobby">← Forlat</button>
            </div>
          </template>
          <button v-else class="btn btn-primary" style="margin-top:16px; width:100%;" @click="resetGame">Spill igjen</button>
        </div>
      </div>
    </template>

    <RoomPlayersModal
      v-if="showPlayersModal && onlineRoom"
      :room-code="onlineRoom.roomCode"
      :participants="playersModalParticipants"
      :your-participant-id="onlineRoom.yourParticipantId"
      :is-host="isHost"
      @close="showPlayersModal = false"
      @changed="onPlayersModalChanged"
    />
  </div>
  </GameAccessGate>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import { createRoomChannel, createStaleGuard } from '../composables/useRoomChannel'
import InviteLinkButton from '../components/InviteLinkButton.vue'
import auth from '../services/auth'
import activeRoom from '../services/activeRoom'
import passAndPlayState from '../services/passAndPlayState'
import navTrigger from '../services/navTrigger'
import FlashbackGame from '../components/FlashbackGame.vue'
import OnlineFlashbackGame from '../components/OnlineFlashbackGame.vue'
import GameAccessGate from '../components/GameAccessGate.vue'
import RoomPlayersModal from '../components/RoomPlayersModal.vue'
import FlashbackPreview from '../components/previews/FlashbackPreview.vue'

const colorOptions = [
  { hex: '#4f46e5', name: 'Indigo' },
  { hex: '#7C7CFC', name: 'Lys indigo' },
  { hex: '#F22C05', name: 'Rød' },
  { hex: '#F2BB05', name: 'Gul' },
  { hex: '#032E8A', name: 'Blå' },
  { hex: '#05D6F2', name: 'Lyseblå' },
  { hex: '#f43f5e', name: 'Rosa' },
  { hex: '#5D038A', name: 'Lilla' }
]
const playerCountOptions = [2, 3, 4, 5, 6, 7, 8] // min 2 - closest-guess tiebreak needs at least a real opponent

const stage = ref('modeChoice')
const error = ref('')
const numPlayers = ref(2)
const numRounds = ref(5)

const setupPlayers = reactive([])
const finalScores = ref([])

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

function startGame() {
  passAndPlayState.save('flashback', {
    roundCount: numRounds.value,
    players: [...setupPlayers]
  })
  savedPassAndPlay.value = passAndPlayState.load('flashback')
  stage.value = 'game'
}

const sortedScores = computed(() => [...finalScores.value].sort((a, b) => b[1] - a[1]))

const lastGameWasOnline = ref(false)
const restartingRoom = ref(false)

function onGameOver(scores) {
  lastGameWasOnline.value = false
  api.recordGamePlayed('FLASHBACK')
  passAndPlayState.clear('flashback')
  passAndPlayState.clear('flashback-progress')
  savedPassAndPlay.value = null
  finalScores.value = scores
  stage.value = 'done'
}

function resetGame() {
  passAndPlayState.clear('flashback')
  passAndPlayState.clear('flashback-progress')
  savedPassAndPlay.value = null
  finalScores.value = []
  stage.value = 'modeChoice'
}

const savedPassAndPlay = ref(null)
onMounted(() => {
  savedPassAndPlay.value = passAndPlayState.load('flashback')
})

function resumePassAndPlay() {
  const saved = savedPassAndPlay.value
  numRounds.value = saved.roundCount
  setupPlayers.length = 0
  saved.players.forEach(p => setupPlayers.push(p))
  stage.value = 'game'
}

function dismissPassAndPlay() {
  passAndPlayState.clear('flashback')
  passAndPlayState.clear('flashback-progress')
  savedPassAndPlay.value = null
}

// --- Online multiplayer ---
const onlineNumRounds = ref(5)
const onlineDisplayName = ref(auth.state.displayName || '')
const joinCode = ref('')
const onlineRoom = ref(null)
const creatingRoom = ref(false)
const joiningRoom = ref(false)
const startingRoom = ref(false)
let lobbyChannel = null

const isHost = computed(() => !!onlineRoom.value?.host)

// "Manage players" (kick a stuck participant / claim host) - fetches a fresh
// room snapshot on open rather than trusting whatever onlineRoom already has,
// since that can be stale once a game's in progress (the lobby channel stops
// polling by then - see stopLobbyChannel below) and connected/isHost are
// exactly the fields that matter most here.
const showPlayersModal = ref(false)
const playersModalParticipants = ref([])
async function openPlayersModal() {
  if (!onlineRoom.value) return
  try {
    const fresh = await api.getRoom(onlineRoom.value.roomCode)
    playersModalParticipants.value = fresh.participants
    showPlayersModal.value = true
  } catch (e) {
    error.value = 'Kunne ikke laste spillerlisten.'
  }
}
function onPlayersModalChanged(dto) {
  playersModalParticipants.value = dto.participants
  onlineRoom.value = onlineRoom.value ? { ...onlineRoom.value, ...dto } : dto
}

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
      gameType: 'FLASHBACK',
      displayName: onlineDisplayName.value.trim(),
      color: pickUnusedColor(),
      flashbackNumRounds: onlineNumRounds.value
    })
    activeRoom.save(onlineRoom.value.roomCode, 'FLASHBACK')
    stage.value = 'onlineLobby'
    startLobbyChannel()
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke opprette rommet.'
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
    activeRoom.save(onlineRoom.value.roomCode, 'FLASHBACK')
    if (onlineRoom.value.status === 'IN_PROGRESS') {
      stage.value = 'onlineGame'
    } else {
      stage.value = 'onlineLobby'
      startLobbyChannel()
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke bli med i rommet - sjekk koden og prøv igjen.'
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
const lobbyStaleGuard = createStaleGuard()

function applyLobbyUpdate(updated) {
  lobbyStaleGuard.markApplied()
  const mine = onlineRoom.value
  onlineRoom.value = mine
    ? { ...updated, host: mine.host, yourParticipantId: mine.yourParticipantId }
    : updated
  if (updated.status === 'IN_PROGRESS') {
    stopLobbyChannel()
    stage.value = 'onlineGame'
  } else if (updated.status === 'WAITING' && stage.value === 'done') {
    // The host just restarted (see restartOnlineRoom) - every other player
    // sitting on the "Game over" screen, still subscribed to this same
    // lobby channel, gets dropped back into the lobby right along with them.
    stage.value = 'onlineLobby'
  }
}

async function pollLobby() {
  if (!onlineRoom.value) return
  const stillFresh = lobbyStaleGuard.begin()
  try {
    const updated = await api.getRoom(onlineRoom.value.roomCode)
    if (stillFresh()) applyLobbyUpdate(updated)
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
    error.value = e.response?.data?.message || 'Kunne ikke starte spillet.'
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

// Server already records the completed game (see FlashbackOnlineService.nextRound),
// so unlike onGameOver above, this doesn't also call api.recordGamePlayed - that
// would double-count it.
function onOnlineGameOver(scores) {
  lastGameWasOnline.value = true
  activeRoom.clear()
  finalScores.value = scores
  // Deliberately NOT resetOnline() here - the room stays around so "Play
  // again" can reuse the same code/lobby/participants, and re-subscribing to
  // the lobby channel is what lets every player (not just whoever clicks it)
  // see a restart happen live - see applyLobbyUpdate.
  startLobbyChannel()
  stage.value = 'done'
}

async function restartOnlineRoom() {
  if (!onlineRoom.value) return
  error.value = ''
  restartingRoom.value = true
  try {
    const dto = await api.restartRoom(onlineRoom.value.roomCode)
    applyLobbyUpdate(dto)
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke starte spillet på nytt.'
  } finally {
    restartingRoom.value = false
  }
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
  savedRoomCode.value = activeRoom.get('FLASHBACK') || ''
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

// Clicking the "Flashback" nav tab while already on this page doesn't trigger
// any navigation event on its own - same reasoning as MultiplayerGridView's
// equivalent watcher.
watch(() => navTrigger.state.flashback, () => {
  stopLobbyChannel()
  stage.value = 'modeChoice'
})
</script>
