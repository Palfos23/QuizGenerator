<template>
  <GameAccessGate game="bullseye">
  <div>
    <div v-if="onlineRoom && (stage === 'onlineLobby' || stage === 'onlineGame')" style="display:flex; justify-content:flex-end; margin-bottom:8px;">
      <button class="btn btn-secondary btn-sm" @click="openPlayersModal">Administrer spillere</button>
    </div>

    <template v-if="stage === 'modeChoice'">
      <h1>Bullseye</h1>
      <p class="page-subtitle">
        Hver runde viser et måltall, for eksempel <em>«13 mål i Premier League 2024/25»</em>. Bytt på å nevne en spiller du tror er nær det - den som svarer lengst unna blir slått ut. Spillet fortsetter til én spiller er igjen.
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
        <BullseyePreview />

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
    </template>

    <template v-else-if="stage === 'landing'">
      <h1>Bullseye</h1>
      <p class="page-subtitle">
        En selskapsquiz der enheten sendes rundt. Hver runde viser et måltall - bytt på å nevne noen nær det, den lengst unna blir slått ut, til én spiller er igjen.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Spillere
          <select v-model.number="numPlayers">
            <option v-for="n in [2,3,4,5,6,7,8]" :key="n" :value="n">{{ n }}</option>
          </select>
        </label>
      </div>
      <p class="page-subtitle" style="margin-top:-8px;">
        {{ numPlayers }} spillere gir {{ numPlayers - 1 }} runde{{ numPlayers - 1 > 1 ? 'r' : '' }} - én utslåing per runde.
      </p>

      <div v-if="bullseyeCategories.length" class="field">
        <label style="text-transform:none; font-weight:400; color:var(--text-dim); font-size:0.85rem;">
          Utelat kategorier du helst ikke vil ha spørsmål fra
          <span class="picker-hint" v-if="excludeCategories.length">{{ excludeCategories.length }} utelatt</span>
        </label>
        <div style="display:flex; gap:8px; flex-wrap:wrap; margin-top:6px;">
          <button
            v-for="c in bullseyeCategories"
            :key="c"
            type="button"
            class="team-chip"
            :class="{ active: excludeCategories.includes(c) }"
            @click="toggleExcludeCategory(excludeCategories, c)"
          >{{ c }}</button>
        </div>
      </div>

      <div class="field">
        <label>Hvordan skal spørsmålene velges?</label>
        <div class="language-row">
          <button class="language-btn" :class="{ active: bullseyeMode === 'random' }" @click="bullseyeMode = 'random'">Tilfeldig</button>
          <button class="language-btn" :class="{ active: bullseyeMode === 'manual' }" @click="bullseyeMode = 'manual'">Velg selv</button>
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
      <p class="page-subtitle">
        Antall runder settes når alle har blitt med og du starter spillet - én utslåing per runde, så det ender alltid med nøyaktig én spiller igjen.
      </p>

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
        {{ (onlineRoom?.participants || []).length }} har blitt med ·
        {{ (onlineRoom?.participants || []).length >= 2 ? (onlineRoom.participants.length - 1) + ' runde(r) hvis du starter nå' : 'minst 2 trengs for å starte' }}
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

    <OnlineBullseyeGame
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
        <button class="btn btn-primary" :disabled="!allNamed || duplicateNames || checkingPool" @click="goToQuestionChoice">
          Neste →
        </button>
      </div>
      <LoadingState v-if="checkingPool" full message="Stokker spørsmålene dine…" subtitle="Første runde starter om et øyeblikk." />
    </template>

    <template v-else-if="stage === 'pickQuestions'">
      <h1>Velg {{ roundsNeeded }} spørsmål</h1>
      <p class="page-subtitle">{{ chosenQuestions.length }} / {{ roundsNeeded }} valgt</p>

      <LoadingState v-if="loadingQuestions" full />
      <div v-else-if="!availableQuestions.length" class="empty-state">Ingen spørsmål tilgjengelige ennå.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="q in availableQuestions" :key="q.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ q.title }}</div>
            <div class="saved-quiz-meta">
              {{ sportLabel(q.sport) }} · {{ q.entryCount }} svar · «{{ q.groupDigits === false ? q.targetValue : formatNumber(q.targetValue) }} {{ q.statLabel }}"
            </div>
          </div>
          <button
            class="btn btn-sm"
            :class="isChosenQuestion(q) ? 'btn-primary' : 'btn-secondary'"
            :disabled="!isChosenQuestion(q) && chosenQuestions.length >= roundsNeeded"
            @click="toggleChosenQuestion(q)"
          >{{ isChosenQuestion(q) ? 'Valgt ✓' : '+ Velg' }}</button>
        </div>
      </div>

      <div class="board-picker-actions">
        <button class="btn btn-secondary" @click="stage = 'setup'">← Tilbake</button>
        <button
          class="btn btn-primary"
          :class="{ 'btn-ready-pulse': chosenQuestions.length === roundsNeeded }"
          :disabled="chosenQuestions.length !== roundsNeeded"
          @click="startManualGame"
        >{{ chosenQuestions.length === roundsNeeded ? `Start spillet ✓ (${chosenQuestions.length}/${roundsNeeded})` : `Start spillet (${chosenQuestions.length}/${roundsNeeded})` }}</button>
      </div>
    </template>

    <BullseyeGame
      v-else-if="stage === 'game'"
      :players="setupPlayers"
      :exclude-categories="excludeCategories"
      :mode="gameMode"
      :questions="gameQuestions"
      @game-over="onGameOver"
    />

    <template v-else-if="stage === 'done'">
      <div class="modal-backdrop">
        <div class="completion-popup podium-host">
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
import { formatNumber, sportLabel } from '../constants'
import { createRoomChannel, createStaleGuard } from '../composables/useRoomChannel'
import InviteLinkButton from '../components/InviteLinkButton.vue'
import auth from '../services/auth'
import activeRoom from '../services/activeRoom'
import passAndPlayState from '../services/passAndPlayState'
import navTrigger from '../services/navTrigger'
import BullseyeGame from '../components/BullseyeGame.vue'
import OnlineBullseyeGame from '../components/OnlineBullseyeGame.vue'
import LoadingState from '../components/LoadingState.vue'
import GameAccessGate from '../components/GameAccessGate.vue'
import RoomPlayersModal from '../components/RoomPlayersModal.vue'
import BullseyePreview from '../components/previews/BullseyePreview.vue'

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

const stage = ref('modeChoice')
const error = ref('')
const numPlayers = ref(2)
const setupPlayers = reactive([])
const finalScores = ref([])
const checkingPool = ref(false)
const bullseyeCategories = ref([])
const excludeCategories = ref([])
const bullseyeMode = ref('random') // 'random' | 'manual'
const availableQuestions = ref([])
const loadingQuestions = ref(false)
const chosenQuestions = ref([])
const gameMode = ref('random')
const gameQuestions = ref([])
const roundsNeeded = computed(() => setupPlayers.length - 1)

function toggleExcludeCategory(list, name) {
  const idx = list.indexOf(name)
  if (idx === -1) list.push(name)
  else list.splice(idx, 1)
}

const savedPassAndPlay = ref(null)
onMounted(() => {
  savedPassAndPlay.value = passAndPlayState.load('bullseye')
  api.fetchBullseyeCategories().then(cats => { bullseyeCategories.value = cats }).catch(() => {})
})

function resumePassAndPlay() {
  const saved = savedPassAndPlay.value
  setupPlayers.length = 0
  saved.players.forEach(p => setupPlayers.push(p))
  excludeCategories.value = saved.excludeCategories || []
  gameMode.value = saved.mode || 'random'
  gameQuestions.value = saved.gameQuestions || []
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

async function goToQuestionChoice() {
  error.value = ''
  if (bullseyeMode.value === 'random') {
    // Questions are picked one round at a time as the game is played (see
    // BullseyeGame's own round-choice screen), not resolved up front - this
    // just checks there's actually enough of a pool to draw from at all.
    let poolSize = 0
    let accessError = ''
    checkingPool.value = true
    try {
      poolSize = (await api.getBattleEligibleBullseyeQuestions(excludeCategories.value)).length
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
    if (poolSize < roundsNeeded.value) {
      const reason = excludeCategories.value.length
        ? 'Prøv å utelate færre kategorier, eller be en administrator legge til flere Bullseye-spørsmål.'
        : 'Be en administrator legge til flere Bullseye-spørsmål.'
      error.value = `Fant bare ${poolSize} spørsmål - trenger minst ${roundsNeeded.value} for ${setupPlayers.length} spillere. ${reason}`
      stage.value = 'landing'
      return
    }
    gameMode.value = 'random'
    gameQuestions.value = []
    passAndPlayState.save('bullseye', { mode: 'random', players: [...setupPlayers], excludeCategories: [...excludeCategories.value] })
    savedPassAndPlay.value = passAndPlayState.load('bullseye')
    stage.value = 'game'
  } else {
    chosenQuestions.value = []
    stage.value = 'pickQuestions'
    await loadAvailableQuestions()
  }
}

async function loadAvailableQuestions() {
  loadingQuestions.value = true
  try {
    availableQuestions.value = await api.getBattleEligibleBullseyeQuestions(excludeCategories.value)
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke laste spørsmål.'
  } finally {
    loadingQuestions.value = false
  }
}

function isChosenQuestion(q) {
  return chosenQuestions.value.some(c => c.id === q.id)
}
function toggleChosenQuestion(q) {
  if (isChosenQuestion(q)) {
    chosenQuestions.value = chosenQuestions.value.filter(c => c.id !== q.id)
  } else if (chosenQuestions.value.length < roundsNeeded.value) {
    chosenQuestions.value.push(q)
  }
}

function startManualGame() {
  gameMode.value = 'manual'
  gameQuestions.value = chosenQuestions.value
  passAndPlayState.save('bullseye', {
    mode: 'manual', gameQuestions: gameQuestions.value,
    players: [...setupPlayers], excludeCategories: [...excludeCategories.value]
  })
  savedPassAndPlay.value = passAndPlayState.load('bullseye')
  stage.value = 'game'
}

const sortedScores = computed(() => [...finalScores.value].sort((a, b) => b[1] - a[1]))

const lastGameWasOnline = ref(false)
const restartingRoom = ref(false)

function onGameOver(scores) {
  lastGameWasOnline.value = false
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
    activeRoom.save(onlineRoom.value.roomCode, 'BULLSEYE')
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

// Server already records the completed game (see BullseyeOnlineService.nextRound),
// so unlike onGameOver above, this doesn't also call api.recordGamePlayed - that
// would double-count it.
function onOnlineGameOver(scores) {
  lastGameWasOnline.value = true
  activeRoom.clear()
  finalScores.value = scores
  // Deliberately NOT resetOnline() here - the room (and onlineRoom itself)
  // stays around so "Play again" below can reuse the same code/lobby/
  // participants instead of everyone having to leave and re-share a new
  // one. The game's own state channel already stopped itself on finishing;
  // re-subscribing to the LOBBY channel here is what lets every player -
  // not just whoever clicks "Play again" - see the restart happen live and
  // get dropped back into the lobby automatically (see applyLobbyUpdate).
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
