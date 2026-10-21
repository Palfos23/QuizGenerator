<template>
  <GameAccessGate game="grid-battle">
  <div>
    <div v-if="onlineRoom && (stage === 'onlineLobby' || stage === 'onlineGame')" style="display:flex; justify-content:flex-end; margin-bottom:8px;">
      <button class="btn btn-secondary btn-sm" @click="openPlayersModal">{{ $t('Administrer spillere') }}</button>
    </div>

    <template v-if="stage === 'modeChoice'">
      <h1>Grid Battle</h1>
      <p class="page-subtitle">
        {{ $t('Bytt på å gjette - gjetter du riktig, avsløres ruten (og du får ett poeng), gjetter du feil, mister du et liv. Spillet fortsetter til et grid er helt løst eller alle er tomme for liv.') }}
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="savedRoomCode" class="banner" style="background:rgb(var(--gold-rgb) / 0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>{{ $t('Du har et spill i gang i rom') }} <strong>{{ savedRoomCode }}</strong>.</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" :disabled="rejoining" @click="rejoinSavedRoom">
            {{ rejoining ? $t('Går inn igjen…') : $t('Gå inn igjen') }}
          </button>
          <button class="btn btn-secondary btn-sm" @click="dismissSavedRoom">{{ $t('Lukk§MultiplayerGridViewT') }}</button>
        </div>
      </div>

      <div v-if="savedPassAndPlay" class="banner" style="background:rgb(var(--gold-rgb) / 0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>{{ $t('Du har et spill der enheten sendes rundt, i gang.') }}</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" @click="resumePassAndPlay">{{ $t('Fortsett§MultiplayerGridViewT') }}</button>
          <button class="btn btn-secondary btn-sm" @click="dismissPassAndPlay">{{ $t('Lukk§MultiplayerGridViewT') }}</button>
        </div>
      </div>

      <div class="game-landing-hero">
        <GridBattlePreview />

        <div class="mode-choice-row">
          <button class="mode-choice-card" @click="stage = 'landing'">
            <h3>{{ $t('Samme enhet') }}</h3>
            <p>{{ $t('Send telefonen rundt - alle tar turen sin på én skjerm.') }}</p>
          </button>
          <button class="mode-choice-card" @click="stage = 'onlineChoice'">
            <h3>{{ $t('Spill på nett') }}</h3>
            <p>{{ $t('Alle spiller fra sin egen enhet med en felles romkode.') }}</p>
          </button>
        </div>
      </div>

      <details class="advanced-disclosure" style="margin-top:24px;">
        <summary>{{ $t('Usikker på hvordan det fungerer? Se et eksempel') }}</summary>

        <div style="margin-top:16px; padding:16px 20px; border:1px solid var(--border); border-radius: 2px; background:rgb(var(--fg-rgb) / 0.06);">
          <p style="margin-top:0;">
            {{ $t('La oss si at gridets tema er') }} <em>{{ $t('«Toppscorere for en fotballklubb forrige sesong.»') }}</em> {{ $t('Det er et brett med skjulte ruter, der hver viser et lite hint før den er løst - som') }} <strong>{{ $t('«FW | 14»') }}</strong> {{ $t('for en spiss med 14 mål.') }}
          </p>
          <p>
            {{ $t('Når det er din tur, søker du etter spilleren du tror passer til en av de skjulte rutene, og sender inn en gjetning.') }}
          </p>
          <ul style="margin:0 0 14px; padding-left:20px; line-height:1.7;">
            <li><strong style="color:var(--teal);">{{ $t('Gjett riktig') }}</strong> {{ $t('- ruten snus og avslører bildet og navnet, og du får ett poeng.') }}</li>
            <li><strong style="color:var(--coral);">{{ $t('Gjett feil') }}</strong> {{ $t('- du mister ett av livene dine (5 som standard).') }}</li>
          </ul>
          <p>
            {{ $t('Uansett går turen videre til neste spiller. Hver spiller har sine egne liv, så det å bli slått ut avslutter ikke spillet for alle andre - brettet fortsetter til det enten er helt løst, eller alle spillere er tomme for liv.') }}
          </p>
          <p style="margin-bottom:0;">
            {{ $t('Et helt spill er 2-4 grids spilt etter hverandre, der alles poeng legges sammen på tvers av alle - den som har høyest sum til slutt vinner.') }}
          </p>
        </div>
      </details>
    </template>

    <template v-else-if="stage === 'landing'">
      <h1>Grid Battle</h1>
      <p class="page-subtitle">
        {{ $t('En flerspillerversjon av Ukens grid der enheten sendes rundt. Bytt på å gjette - gjetter du riktig, avsløres ruten (og du får ett poeng), gjetter du feil, mister du et liv. Spillet fortsetter til et grid er helt løst eller alle er tomme for liv.') }}
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>{{ $t('Spillere') }}
            <select v-model.number="numPlayers">
              <option v-for="n in [2,3,4,5]" :key="n" :value="n">{{ n }}</option>
            </select>
          </label>
        </div>
        <div style="flex:1; min-width:160px;">
          <label>Grids
            <select v-model.number="numGrids">
              <option v-for="n in [2,3,4]" :key="n" :value="n">{{ n }}</option>
            </select>
          </label>
        </div>
      </div>

      <div class="field">
        <label>{{ $t('Hvordan skal gridene velges?') }}</label>
        <div class="language-row">
          <button class="language-btn" :class="{ active: gridMode === 'random' }" @click="gridMode = 'random'">{{ $t('Tilfeldig') }}</button>
          <button class="language-btn" :class="{ active: gridMode === 'manual' }" @click="gridMode = 'manual'">{{ $t('Velg selv') }}</button>
        </div>
      </div>

      <button class="btn btn-secondary" @click="stage = 'modeChoice'">{{ $t('← Tilbake') }}</button>
      <button class="btn btn-primary" style="margin-left:10px;" @click="goToSetup">{{ $t('Opprett spill') }}</button>
    </template>

    <template v-else-if="stage === 'onlineChoice'">
      <h1>{{ $t('Spill på nett') }}</h1>
      <p class="page-subtitle">{{ $t('Samme spill, ulike enheter - del en romkode med 1-3 venner i stedet for å sende én telefon rundt.') }}</p>
      <div v-if="error" class="banner error">{{ error }}</div>
      <div style="display:flex; gap:12px; flex-wrap:wrap;">
        <button class="btn btn-primary" @click="stage = 'onlineCreate'">{{ $t('+ Opprett et rom') }}</button>
        <button class="btn btn-secondary" @click="stage = 'onlineJoin'">{{ $t('Bli med med kode') }}</button>
      </div>
      <button class="btn btn-secondary" style="margin-top:16px;" @click="stage = 'modeChoice'">{{ $t('← Tilbake') }}</button>
    </template>

    <template v-else-if="stage === 'onlineCreate'">
      <h1>{{ $t('Opprett et rom') }}</h1>
      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>{{ $t('Navnet ditt') }} <span class="picker-hint">{{ $t('vises for andre spillere') }}</span><input type="text" v-model="onlineDisplayName" :placeholder="$t('Navnet ditt')" /></label>
      </div>

      <div class="field">
        <label>Grids <span class="picker-hint">2-4</span>
          <select v-model.number="onlineNumGrids">
            <option v-for="n in [2,3,4]" :key="n" :value="n">{{ n }}</option>
          </select>
        </label>
      </div>

      <div class="field">
        <label>{{ $t('Hvordan skal gridene velges?') }}</label>
        <div class="language-row">
          <button class="language-btn" :class="{ active: onlineGridMode === 'random' }" @click="onlineGridMode = 'random'">{{ $t('Tilfeldig') }}</button>
          <button class="language-btn" :class="{ active: onlineGridMode === 'manual' }" @click="selectOnlineManual">{{ $t('Velg selv') }}</button>
        </div>
      </div>

      <div v-if="onlineGridMode === 'manual'" class="field">
        <label>{{ $t('Velg nøyaktig') }} {{ onlineNumGrids }} {{ $t('grid') }} <span class="picker-hint">{{ onlineChosenGrids.length }}/{{ onlineNumGrids }} {{ $t('valgt§MultiplayerGridViewT') }}</span></label>
        <LoadingState v-if="loadingGrids" full />
        <div v-else class="saved-quiz-list">
          <div v-for="g in availableGrids" :key="g.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">
                {{ g.title }}
              </div>
              <div class="saved-quiz-meta">{{ sportLabel(g.sport) }} · {{ g.entryCount }} {{ $t('å finne') }}</div>
            </div>
            <button
              class="btn btn-sm"
              :class="onlineChosenGrids.includes(g.id) ? 'btn-primary' : 'btn-secondary'"
              @click="toggleOnlineGrid(g.id)"
            >{{ onlineChosenGrids.includes(g.id) ? $t('Valgt ✓§MultiplayerGridViewM') : $t('+ Velg§MultiplayerGridViewM') }}</button>
          </div>
        </div>
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'onlineChoice'">{{ $t('← Tilbake') }}</button>
        <button class="btn btn-primary" :disabled="creatingRoom || !onlineDisplayName.trim() || (onlineGridMode === 'manual' && onlineChosenGrids.length !== onlineNumGrids)" @click="createOnlineRoom">
          {{ creatingRoom ? $t('Oppretter…') : $t('Opprett rom') }}
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'onlineJoin'">
      <h1>{{ $t('Bli med i et rom') }}</h1>
      <div v-if="error" class="banner error">{{ error }}</div>
      <div class="field">
        <label>{{ $t('Navnet ditt') }} <span class="picker-hint">{{ $t('vises for andre spillere') }}</span><input type="text" v-model="onlineDisplayName" :placeholder="$t('Navnet ditt')" /></label>
      </div>
      <div class="field">
        <label>{{ $t('Romkode') }}<input type="text" v-model="joinCode" :placeholder="$t('f.eks. ABCDE')" style="text-transform:uppercase; letter-spacing:0.1em; font-size:1.2rem; text-align:center;" maxlength="5" /></label>
      </div>
      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'onlineChoice'">{{ $t('← Tilbake') }}</button>
        <button class="btn btn-primary" :disabled="!joinCode.trim() || !onlineDisplayName.trim() || joiningRoom" @click="joinOnlineRoom()">
          {{ joiningRoom ? $t('Blir med…') : $t('Bli med') }}
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'onlineLobby'">
      <h1>{{ $t('Rom') }} {{ onlineRoom?.roomCode }}</h1>
      <p class="page-subtitle">{{ $t('Del denne koden med vennene dine. Alle må bli med før verten starter.') }}</p>
      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="saved-quiz-list" style="max-width:420px;">
        <div v-for="p in onlineRoom?.participants || []" :key="p.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              <span style="display:inline-block; width:10px; height:10px; border-radius:50%; margin-right:6px;" :style="{ background: p.color }"></span>
              {{ p.displayName }}
            </div>
          </div>
          <span v-if="p.connected === false && p.id !== onlineRoom?.yourParticipantId" class="tag offline">{{ $t('Frakoblet§MultiplayerGridViewT') }}</span>
            <span v-else class="tag" :style="{ background: 'rgb(var(--teal-rgb) / 0.15)', color: 'var(--teal)' }">{{ $t('I rommet') }}</span>
        </div>
      </div>

      <p style="color:var(--text-dim); font-size:0.9rem; margin-top:16px;">
        {{ (onlineRoom?.participants || []).length }} {{ $t('har blitt med · minst 2 trengs for å starte') }}
      </p>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="leaveLobby">{{ $t('← Forlat') }}</button>
        <InviteLinkButton v-if="onlineRoom" :room-code="onlineRoom.roomCode" />
        <button
          v-if="isHost"
          class="btn btn-primary"
          :disabled="(onlineRoom?.participants || []).length < 2 || startingRoom"
          @click="startOnlineRoom"
        >{{ startingRoom ? $t('Starter…') : $t('Start spillet') }}</button>
        <span v-else style="color:var(--text-dim); align-self:center;">{{ $t('Venter på at verten skal starte…') }}</span>
      </div>
    </template>

    <OnlineGridBattleGame
      v-else-if="stage === 'onlineGame'"
      :room-code="onlineRoom?.roomCode"
      :your-participant-id="onlineRoom?.yourParticipantId"
      :is-host="isHost"
      @game-over="onOnlineGameOver"
      @leave="leaveGame"
    />

    <template v-else-if="stage === 'setup'">
      <h1>{{ $t('Hvem spiller?') }}</h1>
      <p class="page-subtitle" v-if="duplicateNames">{{ $t('To spillere kan ikke ha samme navn.') }}</p>

      <div v-for="(p, i) in setupPlayers" :key="i" class="field">
        <input type="text" v-model="p.name" :placeholder="$t('Spiller {0}', [i + 1])" />
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'landing'">{{ $t('← Tilbake') }}</button>
        <button class="btn btn-primary" :disabled="!allNamed || duplicateNames || checkingPool" @click="goToGridChoice">
          {{ $t('Neste →') }}
        </button>
      </div>
      <LoadingState v-if="checkingPool" full :message="$t('Stokker gridene dine…')" :subtitle="$t('Første runde starter om et øyeblikk.')" />
    </template>

    <template v-else-if="stage === 'pickGrids'">
      <h1>{{ $t('Velg') }} {{ numGrids }} {{ $t('grid') }}</h1>
      <p class="page-subtitle">{{ chosenGrids.length }} / {{ numGrids }} {{ $t('valgt§MultiplayerGridViewT') }}</p>

      <LoadingState v-if="loadingGrids" full />
      <div v-else-if="!availableGrids.length" class="empty-state">{{ $t('Ingen grids tilgjengelige ennå.') }}</div>

      <div v-else class="saved-quiz-list">
        <div v-for="g in availableGrids" :key="g.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">
              {{ g.title }}
            </div>
            <div class="saved-quiz-meta">{{ sportLabel(g.sport) }} · {{ g.entryCount }} {{ $t('å finne') }}</div>
          </div>
          <button
            class="btn btn-sm"
            :class="isChosen(g) ? 'btn-primary' : 'btn-secondary'"
            :disabled="!isChosen(g) && chosenGrids.length >= numGrids"
            @click="toggleChosen(g)"
          >{{ isChosen(g) ? $t('Valgt ✓§MultiplayerGridViewM') : $t('+ Velg§MultiplayerGridViewM') }}</button>
        </div>
      </div>

      <div class="board-picker-actions">
        <button class="btn btn-secondary" @click="stage = 'setup'">{{ $t('← Tilbake') }}</button>
        <button
          class="btn btn-primary"
          :class="{ 'btn-ready-pulse': chosenGrids.length === numGrids }"
          :disabled="chosenGrids.length !== numGrids"
          @click="startGame"
        >{{ chosenGrids.length === numGrids ? $t('Start spillet ✓ ({0}/{1})', [chosenGrids.length, numGrids]) : $t('Start spillet ({0}/{1})', [chosenGrids.length, numGrids]) }}</button>
      </div>
    </template>

    <MultiplayerGridGame
      v-else-if="stage === 'game'"
      :mode="gameMode"
      :grids="gameGrids"
      :num-grids="gameNumGrids"
      :players="setupPlayers"
      @game-over="onGameOver"
    />

    <template v-else-if="stage === 'done'">
      <div class="modal-backdrop">
        <div class="completion-popup podium-host">
          <h2 style="margin-top:0;">{{ $t('Spillet er over!') }}</h2>

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
              {{ isHost ? $t('Rom {0} er fortsatt åpent - spill igjen med samme gruppe, eller forlat det.', [onlineRoom.roomCode]) : $t('Venter på at verten skal starte en ny runde, eller forlat rommet.') }}
            </p>
            <div style="display:flex; gap:10px; justify-content:center; flex-wrap:wrap; margin-top:12px;">
              <button v-if="isHost" class="btn btn-primary" :disabled="restartingRoom" @click="restartOnlineRoom">
                {{ restartingRoom ? $t('Starter på nytt…') : $t('Spill igjen') }}
              </button>
              <button class="btn btn-secondary" @click="leaveLobby">{{ $t('← Forlat') }}</button>
            </div>
          </template>
          <button v-else class="btn btn-primary" style="margin-top:16px; width:100%;" @click="resetGame">{{ $t('Spill igjen') }}</button>
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
import { t as _t } from '../i18n'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import { createRoomChannel, createStaleGuard } from '../composables/useRoomChannel'
import InviteLinkButton from '../components/InviteLinkButton.vue'
import auth from '../services/auth'
import activeRoom from '../services/activeRoom'
import passAndPlayState from '../services/passAndPlayState'
import navTrigger from '../services/navTrigger'
import { sportLabel } from '../constants'
import MultiplayerGridGame from '../components/MultiplayerGridGame.vue'
import OnlineGridBattleGame from '../components/OnlineGridBattleGame.vue'
import LoadingState from '../components/LoadingState.vue'
import GameAccessGate from '../components/GameAccessGate.vue'
import RoomPlayersModal from '../components/RoomPlayersModal.vue'
import GridBattlePreview from '../components/previews/GridBattlePreview.vue'

const colorOptions = [
  { hex: '#4f46e5', name: 'Indigo' },
  { hex: '#7C7CFC', name: _t('Lys indigo') },
  { hex: '#F22C05', name: _t('Rød') },
  { hex: '#F2BB05', name: _t('Gul') },
  { hex: '#032E8A', name: _t('Blå') },
  { hex: '#05D6F2', name: _t('Lyseblå') },
  { hex: '#f43f5e', name: _t('Rosa') },
  { hex: '#5D038A', name: _t('Lilla') }
]

const stage = ref('modeChoice')
const error = ref('')
const numPlayers = ref(2)
const numGrids = ref(2)
const gridMode = ref('random')

const setupPlayers = reactive([])

const savedPassAndPlay = ref(null)
onMounted(() => {
  savedPassAndPlay.value = passAndPlayState.load('grid-battle')
})

function resumePassAndPlay() {
  const saved = savedPassAndPlay.value
  gameMode.value = saved.mode || 'manual'
  if (gameMode.value === 'random') {
    gameNumGrids.value = saved.numGrids
    gameGrids.value = []
  } else {
    gameGrids.value = saved.gameGrids
  }
  setupPlayers.length = 0
  saved.players.forEach(p => setupPlayers.push(p))
  lastGameWasOnline.value = false
  stage.value = 'game'
}

function dismissPassAndPlay() {
  passAndPlayState.clear('grid-battle')
  passAndPlayState.clear('grid-battle-progress')
  savedPassAndPlay.value = null
}

const availableGrids = ref([])
const loadingGrids = ref(false)
const chosenGrids = ref([])
const gameGrids = ref([])
const gameMode = ref('manual')
const gameNumGrids = ref(2)
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

async function loadAvailableGrids() {
  loadingGrids.value = true
  try {
    availableGrids.value = await api.getBattleEligibleGrids()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke laste grids.')
  } finally {
    loadingGrids.value = false
  }
}

const checkingPool = ref(false)

async function goToGridChoice() {
  error.value = ''
  if (gridMode.value === 'random') {
    // Grids are picked one round at a time as the game is played (see
    // MultiplayerGridGame's round-choice screen), not resolved up front - this
    // just checks there's actually enough of a pool to draw from at all.
    let poolSize = 0
    let accessError = ''
    checkingPool.value = true
    try {
      poolSize = (await api.getBattleEligibleGrids()).length
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
    if (poolSize < numGrids.value) {
      error.value = _t('Fant bare {0} grid - trenger minst {1}. Prøv å velge selv, eller be en administrator legge til flere grids.', [poolSize, numGrids.value])
      stage.value = 'landing'
      return
    }
    gameMode.value = 'random'
    gameNumGrids.value = numGrids.value
    gameGrids.value = []
    passAndPlayState.save('grid-battle', { mode: 'random', numGrids: numGrids.value, players: [...setupPlayers] })
    savedPassAndPlay.value = passAndPlayState.load('grid-battle')
    stage.value = 'game'
  } else {
    chosenGrids.value = []
    stage.value = 'pickGrids'
    await loadAvailableGrids()
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
  gameMode.value = 'manual'
  gameGrids.value = chosenGrids.value
  passAndPlayState.save('grid-battle', { mode: 'manual', gameGrids: gameGrids.value, players: [...setupPlayers] })
  savedPassAndPlay.value = passAndPlayState.load('grid-battle')
  stage.value = 'game'
}

const sortedScores = computed(() => [...finalScores.value].sort((a, b) => b[1] - a[1]))

function onGameOver(scores) {
  api.recordGamePlayed('GRID_BATTLE')
  passAndPlayState.clear('grid-battle')
  passAndPlayState.clear('grid-battle-progress')
  savedPassAndPlay.value = null
  lastGameWasOnline.value = false
  finalScores.value = scores
  stage.value = 'done'
}

function resetGame() {
  passAndPlayState.clear('grid-battle')
  passAndPlayState.clear('grid-battle-progress')
  savedPassAndPlay.value = null
  gameGrids.value = []
  finalScores.value = []
  stage.value = lastGameWasOnline.value ? 'modeChoice' : 'landing'
}

// --- Online multiplayer ---
const onlineNumGrids = ref(2)
const onlineDisplayName = ref(auth.state.displayName || '')
const lastGameWasOnline = ref(false)
const onlineGridMode = ref('random')
const onlineChosenGrids = ref([])
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
    error.value = _t('Kunne ikke laste spillerlisten.')
  }
}
function onPlayersModalChanged(dto) {
  playersModalParticipants.value = dto.participants
  onlineRoom.value = onlineRoom.value ? { ...onlineRoom.value, ...dto } : dto
}

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
      gameType: 'GRID_BATTLE',
      displayName: onlineDisplayName.value.trim(),
      color: pickUnusedColor()
    }
    if (onlineGridMode.value === 'manual') {
      payload.gridIds = onlineChosenGrids.value
    } else {
      payload.randomGridCount = onlineNumGrids.value
    }
    onlineRoom.value = await api.createRoom(payload)
    activeRoom.save(onlineRoom.value.roomCode, 'GRID_BATTLE')
    stage.value = 'onlineLobby'
    startLobbyChannel()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke opprette rommet.')
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
    activeRoom.save(onlineRoom.value.roomCode, 'GRID_BATTLE')
    if (onlineRoom.value.status === 'IN_PROGRESS') {
      stage.value = 'onlineGame'
    } else {
      stage.value = 'onlineLobby'
      startLobbyChannel()
    }
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke bli med i rommet - sjekk koden og prøv igjen.')
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
    error.value = e.response?.data?.message || _t('Kunne ikke starte spillet.')
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
  onlineChosenGrids.value = []
  onlineGridMode.value = 'random'
  joinCode.value = ''
}

function onOnlineGameOver(scores) {
  activeRoom.clear()
  lastGameWasOnline.value = true
  finalScores.value = scores
  // Deliberately NOT resetOnline() here - the room stays around so "Play
  // again" can reuse the same code/lobby/participants, and re-subscribing to
  // the lobby channel is what lets every player (not just whoever clicks it)
  // see a restart happen live - see applyLobbyUpdate.
  startLobbyChannel()
  stage.value = 'done'
}

const restartingRoom = ref(false)
async function restartOnlineRoom() {
  if (!onlineRoom.value) return
  error.value = ''
  restartingRoom.value = true
  try {
    const dto = await api.restartRoom(onlineRoom.value.roomCode)
    applyLobbyUpdate(dto)
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke starte spillet på nytt.')
  } finally {
    restartingRoom.value = false
  }
}

const savedRoomCode = ref('')
const rejoining = ref(false)
const route = useRoute()

onMounted(() => {
  // Arrived via /join?code=... (JoinGuestView.vue, mostly for guests, but works
  // for a signed-in visitor too) - go straight into that room instead of
  // making them retype the code they already entered once.
  if (route.query.code) {
    stage.value = 'onlineJoin'
    joinOnlineRoom(String(route.query.code))
    return
  }
  savedRoomCode.value = activeRoom.get('GRID_BATTLE') || ''
})

// Clicking the "Grid Battle" nav tab while already on this page doesn't trigger
// any navigation event on its own, so it needs its own trigger to jump back to
// the very first screen - mid-game state (pass-and-play or online) is simply
// left behind, same as if the tab had been closed and reopened.
watch(() => navTrigger.state.gridBattle, () => {
  stopLobbyChannel()
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
