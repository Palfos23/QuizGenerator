<template>
  <GameAccessGate game="tension">
  <div>
    <div v-if="onlineRoom && (stage === 'onlineLobby' || stage === 'onlineGame')" style="display:flex; justify-content:flex-end; margin-bottom:8px;">
      <button class="btn btn-secondary btn-sm" @click="openPlayersModal">{{ $t('Administrer spillere') }}</button>
    </div>

    <template v-if="stage === 'modeChoice'">
      <h1>Tension</h1>
      <p class="page-subtitle">
        {{ $t('Gjett så nær posisjon 10 på listen som du tør - går du for langt inn i «tension»-sonen, koster det deg.') }}
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="savedRoomCode" class="banner" style="background:rgb(var(--gold-rgb) / 0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>{{ $t('Du har et spill i gang i rom') }} <strong>{{ savedRoomCode }}</strong>.</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" :disabled="rejoining" @click="rejoinSavedRoom">
            {{ rejoining ? $t('Går inn igjen…') : $t('Gå inn igjen') }}
          </button>
          <button class="btn btn-secondary btn-sm" @click="dismissSavedRoom">{{ $t('Lukk§TensionViewT') }}</button>
        </div>
      </div>

      <div v-if="savedPassAndPlay" class="banner" style="background:rgb(var(--gold-rgb) / 0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>{{ $t('Du har et spill der enheten sendes rundt, i gang.') }}</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" @click="resumePassAndPlay">{{ $t('Fortsett§TensionViewT') }}</button>
          <button class="btn btn-secondary btn-sm" @click="dismissPassAndPlay">{{ $t('Lukk§TensionViewT') }}</button>
        </div>
      </div>

      <div class="game-landing-hero">
        <TensionPreview />

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
            {{ $t('Si at spørsmålet er') }} <em>{{ $t('«Nevn en av de 10 mest søkte matingrediensene på Google i 2023»') }}</em>
            {{ $t('(et oppdiktet eksempel, bare for å illustrere). Det finnes en rangert liste med 10 riktige svar som ingen kan se ennå:') }}
          </p>
          <ul style="margin:0 0 14px; padding-left:20px; line-height:1.7;">
            <li><strong>{{ $t('Plass 1: Salt') }}</strong> {{ $t('- det mest åpenbare gjettet. Trygt, men bare verdt') }} <strong>{{ $t('1 poeng') }}</strong>.</li>
            <li><strong>{{ $t('Plass 5: Hvitløk') }}</strong> {{ $t('- litt mindre åpenbart. Verdt') }} <strong>{{ $t('5 poeng') }}</strong>.</li>
            <li><strong>{{ $t('Plass 10: Basilikum') }}</strong> {{ $t('- det mest risikable trygge svaret. Verdt') }} <strong>{{ $t('10 poeng') }}</strong> {{ $t('- jo nærmere 10, jo mer belønnes du for å teste flaksen.') }}</li>
          </ul>
          <p>
            {{ $t('Men rett etter listen ligger en felle - et «tension»-svar som') }} <strong>Oregano</strong>{{ $t('. Det virker like sannsynlig som Basilikum, men kom ikke med blant de 10 beste. Gjetter du det, mister du') }} <strong style="color:var(--coral);">{{ $t('5 poeng') }}</strong>{{ $t('. Gjetter du noe helt urelatert, som') }} <em>{{ $t('Sjokolade') }}</em>{{ $t(', mister du') }} <strong style="color:var(--coral);">{{ $t('3 poeng') }}</strong>.
          </p>
          <p style="margin-bottom:0;">
            {{ $t('Spillerne bytter på å gjette ett svar hver, og ingen kan gjenta et svar noen andre allerede har brukt den runden. Når alle har svart, avsløres hele listen - hvem som gjettet hva, og hvor mye det ga.') }}
          </p>
        </div>
      </details>
    </template>

    <template v-else-if="stage === 'landing'">
      <h1>Tension</h1>
      <p class="page-subtitle">
        {{ $t('En selskapsquiz der enheten sendes rundt. Gjett så nær posisjon 10 på listen som du tør - går du for langt inn i «tension»-sonen, koster det deg.') }}
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>{{ $t('Spillere') }}
            <select v-model.number="numPlayers">
              <option v-for="n in 8" :key="n" :value="n">{{ n }}</option>
            </select>
          </label>
        </div>
        <div style="flex:1; min-width:160px;">
          <label>{{ $t('Spørsmål§TensionViewT') }}
            <select v-model.number="numQuestions">
              <option v-for="n in 10" :key="n" :value="n">{{ n }}</option>
            </select>
          </label>
        </div>
        <div style="flex:1; min-width:160px;">
          <label>{{ $t('Kategori') }}
            <select v-model="category">
              <option value="">{{ $t('Alle kategorier') }}</option>
              <option v-for="c in mainCategories" :key="c" :value="c">{{ c }}</option>
            </select>
          </label>
        </div>
      </div>

      <div v-if="!category && mainCategories.length" class="field">
        <label style="text-transform:none; font-weight:400; color:var(--text-dim); font-size:0.85rem;">
          {{ $t('Utelat kategorier du helst ikke vil ha spørsmål fra') }}
          <span class="picker-hint" v-if="excludeCategories.length">{{ excludeCategories.length }} {{ $t('utelatt') }}</span>
        </label>
        <div style="display:flex; gap:8px; flex-wrap:wrap; margin-top:6px;">
          <button
            v-for="c in mainCategories"
            :key="c"
            type="button"
            class="team-chip"
            :class="{ active: excludeCategories.includes(c) }"
            @click="toggleExcludeCategory(excludeCategories, c)"
          >{{ c }}</button>
        </div>
      </div>

      <button class="btn btn-secondary" @click="stage = 'modeChoice'">{{ $t('← Tilbake') }}</button>
      <button class="btn btn-primary" style="margin-left:10px;" @click="goToSetup">{{ $t('Opprett spill') }}</button>
    </template>

    <template v-else-if="stage === 'onlineChoice'">
      <h1>{{ $t('Spill på nett') }}</h1>
      <p class="page-subtitle">{{ $t('Samme spill, ulike enheter - alle svarer fra sin egen telefon i stedet for å sende én rundt.') }}</p>
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

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>{{ $t('Spørsmål§TensionViewT') }}
            <select v-model.number="onlineNumQuestions">
              <option v-for="n in 10" :key="n" :value="n">{{ n }}</option>
            </select>
          </label>
        </div>
        <div style="flex:1; min-width:160px;">
          <label>{{ $t('Kategori') }}
            <select v-model="onlineCategory">
              <option value="">{{ $t('Alle kategorier') }}</option>
              <option v-for="c in mainCategories" :key="c" :value="c">{{ c }}</option>
            </select>
          </label>
        </div>
      </div>

      <div v-if="!onlineCategory && mainCategories.length" class="field">
        <label style="text-transform:none; font-weight:400; color:var(--text-dim); font-size:0.85rem;">
          {{ $t('Utelat kategorier du helst ikke vil ha spørsmål fra') }}
          <span class="picker-hint" v-if="onlineExcludeCategories.length">{{ onlineExcludeCategories.length }} {{ $t('utelatt') }}</span>
        </label>
        <div style="display:flex; gap:8px; flex-wrap:wrap; margin-top:6px;">
          <button
            v-for="c in mainCategories"
            :key="c"
            type="button"
            class="team-chip"
            :class="{ active: onlineExcludeCategories.includes(c) }"
            @click="toggleExcludeCategory(onlineExcludeCategories, c)"
          >{{ c }}</button>
        </div>
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'onlineChoice'">{{ $t('← Tilbake') }}</button>
        <button class="btn btn-primary" :disabled="creatingRoom || !onlineDisplayName.trim()" @click="createOnlineRoom">
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
          <span v-if="p.connected === false && p.id !== onlineRoom?.yourParticipantId" class="tag offline">{{ $t('Frakoblet§TensionViewT') }}</span>
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

    <OnlineTensionGame
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
        <button class="btn btn-primary" :disabled="!allNamed || duplicateNames || starting" @click="startGame">
          {{ starting ? $t('Laster…') : $t('Start spillet') }}
        </button>
      </div>

      <LoadingState v-if="starting" full :message="$t('Stokker spørsmålene dine…')" :subtitle="$t('Første runde starter om et øyeblikk.')" />
    </template>

    <TensionGame
      v-else-if="stage === 'game'"
      :category="category"
      :exclude-categories="excludeCategories"
      :round-count="numQuestions"
      :players="setupPlayers"
      @game-over="onGameOver"
    />

    <template v-else-if="stage === 'done'">
      <h1 style="text-align:center;">{{ $t('Spillet er over!') }}</h1>

      <div class="podium-row">
        <div v-for="(entry, i) in sortedScores.slice(0, 3)" :key="entry[0]" class="podium-block" :class="`rank-${i + 1}`">
          <div class="podium-rank-number">{{ i + 1 }}</div>
          <div class="podium-name">{{ entry[0] }}</div>
          <div class="podium-score" :style="{ color: entry[1] > 0 ? 'var(--teal)' : entry[1] < 0 ? 'var(--coral)' : 'var(--text-dim)' }">{{ entry[1] }}</div>
        </div>
      </div>

      <div v-if="sortedScores.length > 3" style="max-width:420px; margin:0 auto;">
        <div v-for="(entry, i) in sortedScores.slice(3)" :key="entry[0]" class="podium-rest-row">
          <span>{{ i + 4 }}. {{ entry[0] }}</span>
          <span :style="{ color: entry[1] > 0 ? 'var(--teal)' : entry[1] < 0 ? 'var(--coral)' : 'var(--text-dim)' }">{{ entry[1] }}</span>
        </div>
      </div>

      <div style="text-align:center; margin-top:20px;">
        <template v-if="lastGameWasOnline && onlineRoom">
          <p class="page-subtitle" style="margin:0 0 12px;">
            {{ isHost ? $t('Rom {0} er fortsatt åpent - spill igjen med samme gruppe, eller forlat det.', [onlineRoom.roomCode]) : $t('Venter på at verten skal starte en ny runde, eller forlat rommet.') }}
          </p>
          <div style="display:flex; gap:10px; justify-content:center; flex-wrap:wrap;">
            <button v-if="isHost" class="btn btn-primary" :disabled="restartingRoom" @click="restartOnlineRoom">
              {{ restartingRoom ? $t('Starter på nytt…') : $t('Spill igjen') }}
            </button>
            <button class="btn btn-secondary" @click="leaveLobby">{{ $t('← Forlat') }}</button>
          </div>
        </template>
        <button v-else class="btn btn-primary" @click="resetGame">{{ $t('Spill igjen') }}</button>
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
import TensionGame from '../components/TensionGame.vue'
import OnlineTensionGame from '../components/OnlineTensionGame.vue'
import LoadingState from '../components/LoadingState.vue'
import GameAccessGate from '../components/GameAccessGate.vue'
import RoomPlayersModal from '../components/RoomPlayersModal.vue'
import TensionPreview from '../components/previews/TensionPreview.vue'

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
const lastGameWasOnline = ref(false)
const error = ref('')
const numPlayers = ref(2)
const numQuestions = ref(5)
const category = ref('')
const excludeCategories = ref([])
const mainCategories = ref([])

function toggleExcludeCategory(list, name) {
  const idx = list.indexOf(name)
  if (idx === -1) list.push(name)
  else list.splice(idx, 1)
}

const setupPlayers = reactive([])
const starting = ref(false)
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
    // Just a availability check - the actual per-round questions are now
    // fetched one round at a time (with a choice of a few), not all upfront.
    const sample = await api.fetchTensionQuestions(numQuestions.value, category.value, excludeCategories.value)
    if (sample.length < numQuestions.value) {
      error.value = _t('Fant bare {0} spørsmål i denne kategorien - trenger minst {1}. Prøv en annen kategori eller be en administrator legge til flere.', [sample.length, numQuestions.value])
      stage.value = 'landing'
      return
    }
    passAndPlayState.save('tension', {
      category: category.value,
      excludeCategories: excludeCategories.value,
      roundCount: numQuestions.value,
      players: [...setupPlayers]
    })
    savedPassAndPlay.value = passAndPlayState.load('tension')
    stage.value = 'game'
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke laste spørsmål.')
    stage.value = 'landing'
  } finally {
    starting.value = false
  }
}

const sortedScores = computed(() => [...finalScores.value].sort((a, b) => b[1] - a[1]))

function onGameOver(scores) {
  passAndPlayState.clear('tension')
  passAndPlayState.clear('tension-progress')
  savedPassAndPlay.value = null
  lastGameWasOnline.value = false
  api.recordGamePlayed('TENSION')
  finalScores.value = scores
  stage.value = 'done'
}

function resetGame() {
  passAndPlayState.clear('tension')
  passAndPlayState.clear('tension-progress')
  savedPassAndPlay.value = null
  finalScores.value = []
  stage.value = lastGameWasOnline.value ? 'modeChoice' : 'landing'
}

// --- Online multiplayer ---
const onlineNumQuestions = ref(5)
const onlineDisplayName = ref(auth.state.displayName || '')
const onlineCategory = ref('')
const onlineExcludeCategories = ref([])
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
      gameType: 'TENSION',
      displayName: onlineDisplayName.value.trim(),
      color: pickUnusedColor(),
      tensionNumQuestions: onlineNumQuestions.value,
      tensionCategory: onlineCategory.value || null,
      tensionExcludeCategories: onlineExcludeCategories.value
    })
    activeRoom.save(onlineRoom.value.roomCode, 'TENSION')
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
    activeRoom.save(onlineRoom.value.roomCode, 'TENSION')
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
  // Arrived via /join?code=... (JoinGuestView.vue) - go straight into that
  // room instead of making them retype the code they already entered once.
  if (route.query.code) {
    stage.value = 'onlineJoin'
    joinOnlineRoom(String(route.query.code))
    return
  }
  savedRoomCode.value = activeRoom.get('TENSION') || ''
})

const savedPassAndPlay = ref(null)
onMounted(() => {
  savedPassAndPlay.value = passAndPlayState.load('tension')
})

function resumePassAndPlay() {
  const saved = savedPassAndPlay.value
  category.value = saved.category
  excludeCategories.value = saved.excludeCategories || []
  numQuestions.value = saved.roundCount
  setupPlayers.length = 0
  saved.players.forEach(p => setupPlayers.push(p))
  lastGameWasOnline.value = false
  stage.value = 'game'
}

function dismissPassAndPlay() {
  passAndPlayState.clear('tension')
  passAndPlayState.clear('tension-progress')
  savedPassAndPlay.value = null
}

// Clicking the "Tension" nav tab while already on this page doesn't trigger any
// navigation event on its own, so it needs its own trigger to jump back to the
// very first screen - mid-game state (pass-and-play or online) is simply left
// behind, same as if the tab had been closed and reopened.
watch(() => navTrigger.state.tension, () => {
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
