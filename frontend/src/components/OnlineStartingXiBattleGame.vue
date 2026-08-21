<template>
  <div>
    <div class="grid-status-bar">
      <div class="grid-progress" style="text-align:center; width:100%;">
        Board {{ (state?.currentLineupIndex ?? 0) + 1 }} / {{ state?.totalLineups ?? '?' }}: {{ state?.lineupTitle }}
      </div>
    </div>

    <div v-if="state && (state.teamName || state.opponentName)" class="pitch-scoreline">
      <div class="pitch-scoreline-team">
        <img v-if="state.teamCrestUrl" :src="state.teamCrestUrl" alt="" class="pitch-scoreline-crest" />
        <span>{{ state.teamName }}</span>
      </div>
      <div v-if="state.scoreFor != null && state.scoreAgainst != null" class="pitch-scoreline-score">
        <span>{{ state.scoreFor }}</span><span class="dash">-</span><span>{{ state.scoreAgainst }}</span>
      </div>
      <div v-else class="pitch-scoreline-vs">vs</div>
      <div class="pitch-scoreline-team away">
        <img v-if="state.opponentCrestUrl" :src="state.opponentCrestUrl" alt="" class="pitch-scoreline-crest" />
        <span>{{ state.opponentName }}</span>
      </div>
    </div>

    <LoadingState v-if="loading" full message="Loading the lineup…" />
    <div v-if="error" class="banner error">{{ error }}</div>

    <template v-if="state && !state.finished">
      <div v-if="state.awaitingLineupChoice" class="tension-choice-overlay">
        <template v-if="isPicker">
          <div style="color:var(--gold); text-transform:uppercase; letter-spacing:0.5px; font-size:1rem; margin-bottom:6px;">
            Board {{ state.currentLineupIndex + 1 }} / {{ state.totalLineups }}
          </div>
          <h2 style="margin:0 0 24px;">Choose a board</h2>
          <div class="tension-choice-grid">
            <button
              v-for="l in state.lineupChoices"
              :key="l.id"
              class="tension-choice-card"
              :disabled="choosing"
              @click="chooseLineup(l)"
            >
              <strong>{{ l.title }}</strong>
              <div style="color:var(--text-dim); font-size:0.85rem; margin-top:4px; font-weight:400;">{{ l.teamName }} vs {{ l.opponentName }} · {{ l.formation }}</div>
            </button>
          </div>
        </template>
        <div v-else class="banner" style="text-align:center; background:rgba(255,255,255,0.03);">
          Waiting for {{ pickerName }} to choose a board…
        </div>
      </div>

      <template v-else>
      <div class="mp-player-row">
        <div
          v-for="p in state.players"
          :key="p.participantId"
          class="mp-player-card"
          :class="{ 'active-turn': p.participantId === state.currentTurnParticipantId && !state.lineupComplete, eliminated: p.eliminatedThisLineup }"
          :style="{ borderColor: p.color }"
        >
          <strong>{{ p.name }}</strong>
          <div class="lives-hearts" style="margin-top:4px;">
            <span v-for="i in state.maxStrikes" :key="i" class="life-heart" :class="{ lost: i > state.maxStrikes - p.livesUsed }"></span>
          </div>
          <div style="font-size:0.8rem; color:var(--text-dim); margin-top:4px;">Total: {{ p.totalScore }}</div>
        </div>
      </div>

      <div v-if="!state.lineupComplete && isYourTurn" class="guess-box-wrap no-print" :class="{ 'hide-on-scroll': hideSearchBox }">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <p style="text-align:center; margin:0 0 8px; color:var(--gold); font-weight:600;">Your turn</p>
          <input
            type="text"
            v-model="searchTerm"
            placeholder="Search for a player…"
            aria-label="Search for a player"
            autocomplete="off"
            @keydown.esc="searchTerm = ''"
          />
          <div v-if="searchResults.length" class="guess-results">
            <button v-for="a in searchResults" :key="a.id" class="guess-result-row" :disabled="guessing" @click="submitGuess(a)">
              {{ a.name }}
            </button>
          </div>
        </div>
        <button
          class="btn btn-danger pass-turn-btn"
          :disabled="guessing"
          @click="showSkipConfirm = true"
        >Pass turn (costs a life)</button>
      </div>

      <ConfirmModal
        v-if="showSkipConfirm"
        title="Pass your turn?"
        message="You'll lose a life, same as a wrong guess."
        confirm-text="Pass turn"
        cancel-text="Keep guessing"
        @confirm="confirmSkipTurn"
        @cancel="showSkipConfirm = false"
      />

      <div v-if="!state.lineupComplete && !isYourTurn" class="banner" style="text-align:center; background:rgba(255,255,255,0.03);">
        Waiting for {{ currentTurnName }}'s turn…
      </div>

      <div v-if="state.lineupComplete" class="modal-backdrop">
        <div class="completion-popup">
          <h2 style="margin-top:0;">Board complete!</h2>
          <div class="score-square-grid">
            <div v-for="(p, i) in leaderboardForLineup" :key="p.name" class="score-square" :class="{ leader: i === 0 }">
              <div class="score-square-name">{{ p.name }}</div>
              <div class="score-square-number">{{ p.total }}</div>
              <div v-if="p.roundDelta > 0" class="score-square-delta">+{{ p.roundDelta }}</div>
            </div>
          </div>

          <div v-if="unsolvedEntries.length" style="text-align:left; margin-top:4px;">
            <div style="color:var(--text-dim); font-size:0.82rem; margin-bottom:8px;">Not found:</div>
            <div v-for="e in unsolvedEntries" :key="e.id" class="imposter-reveal-entry" style="background:rgba(255,255,255,0.03); border-color:var(--border);">
              <span style="font-weight:700;">{{ e.name }}</span>
            </div>
          </div>

          <button v-if="isHost" class="btn btn-primary" style="margin-top:12px; width:100%;" :disabled="advancing" @click="nextLineup">
            {{ advancing ? 'Loading…' : (state.currentLineupIndex + 1 < state.totalLineups ? 'Next board' : 'Finish game') }}
          </button>
          <div v-else style="margin-top:8px; color:var(--text-dim);">Waiting for the host to continue…</div>
        </div>
      </div>

      <div class="pitch">
        <PitchMarkings />
        <div v-for="(row, ri) in rows" :key="ri" class="pitch-row" :class="`pitch-row--${row.kind}`">
          <div v-for="slot in row.items" :key="slot.id ?? slot.slotIndex" class="pitch-slot">
            <div class="pitch-shirt" :class="{ solved: slot.solved, goalkeeper: slot.slotIndex === 0 }" :style="shirtStyle(slot)">
              <template v-if="!slot.solved">
                <span class="pitch-shirt-sleeve left"></span>
                <span class="pitch-shirt-sleeve right"></span>
                <span class="pitch-shirt-collar"></span>
              </template>
              <img v-if="slot.solved && slot.athletePhotoUrl" :src="slot.athletePhotoUrl" alt="" class="pitch-slot-photo" />
              <template v-else>{{ slot.shirtNumber }}</template>
              <span v-if="slot.captain" class="pitch-shirt-captain">C</span>
            </div>
            <div class="pitch-slot-name">
              {{ slot.solved ? slot.athleteName : '?' }}
              <template v-if="slot.solved && slot.solvedByName"> <span style="color:var(--text-dim);">({{ slot.solvedByName }})</span></template>
            </div>
          </div>
        </div>
      </div>
      </template>
    </template>

    <div style="display:flex; align-items:center; gap:12px; margin-top:20px; flex-wrap:wrap;">
      <button class="btn btn-secondary btn-sm no-print" @click="leave">← Leave game</button>
      <span class="tag no-print" style="background:rgba(255,255,255,0.06); color:var(--text-dim);">Room: {{ roomCode }}</span>
    </div>

    <div v-if="resultOverlay" class="grid-result-overlay" :class="resultOverlay.correct ? 'correct' : 'wrong'">
      <div class="grid-result-text">{{ resultOverlay.correct ? 'Correct' : 'Wrong' }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import api from '../services/api'
import { displayRowsFor } from '../services/formations'
import { readableTextColor } from '../constants'
import PitchMarkings from './PitchMarkings.vue'
import ConfirmModal from './ConfirmModal.vue'
import LoadingState from './LoadingState.vue'
import { useHideOnScroll } from '../composables/useHideOnScroll'
import { usePolling } from '../composables/usePolling'

const DEFAULT_KIT_COLOR = '#d92332'
const DEFAULT_GK_KIT_COLOR = '#f2c230'

const props = defineProps({
  roomCode: { type: String, required: true },
  yourParticipantId: { type: [Number, String], required: true },
  isHost: { type: Boolean, default: false }
})
const emit = defineEmits(['gameOver', 'leave'])
const { hidden: hideSearchBox } = useHideOnScroll()

const state = ref(null)
const scoresAtLineupStart = ref({})
let lastLineupIndexSeen = null
const loading = ref(true)
const error = ref('')
const searchTerm = ref('')
const searchResults = ref([])
const guessing = ref(false)
const advancing = ref(false)
const shakeGuessBox = ref(false)

const resultOverlay = ref(null)
let resultOverlayTimeout = null
function showResultOverlay(correct) {
  clearTimeout(resultOverlayTimeout)
  resultOverlay.value = null
  requestAnimationFrame(() => {
    resultOverlay.value = { correct }
    resultOverlayTimeout = setTimeout(() => { resultOverlay.value = null }, 1200)
  })
}

const isYourTurn = computed(() => !!state.value && state.value.currentTurnParticipantId === props.yourParticipantId)
const currentTurnName = computed(() =>
  state.value?.players.find(p => p.participantId === state.value.currentTurnParticipantId)?.name || '…'
)
const isPicker = computed(() => !!state.value && state.value.pickerParticipantId === props.yourParticipantId)
const pickerName = computed(() =>
  state.value?.players.find(p => p.participantId === state.value.pickerParticipantId)?.name || '…'
)
const choosing = ref(false)
const rows = computed(() => state.value ? displayRowsFor(state.value.formation, state.value.slots) : [])
function shirtStyle(slot) {
  const color = slot.slotIndex === 0
    ? (state.value?.goalkeeperKitColor || DEFAULT_GK_KIT_COLOR)
    : (state.value?.kitColor || DEFAULT_KIT_COLOR)
  return { '--kit-color': color, '--kit-text': readableTextColor(color) }
}
const leaderboardForLineup = computed(() => {
  if (!state.value) return []
  return [...state.value.players]
    .map(p => ({ name: p.name, total: p.totalScore, roundDelta: p.totalScore - (scoresAtLineupStart.value[p.name] || 0) }))
    .sort((a, b) => b.total - a.total)
})
const revealMap = ref({}) // slot id -> athleteName, fetched once the board completes
// Only non-empty when the board ended by everyone running out of lives -
// a full solve never leaves anything unsolved to report here.
const unsolvedEntries = computed(() => {
  if (!state.value) return []
  return state.value.slots
    .filter(s => !s.solved)
    .map(s => ({ id: s.id, name: revealMap.value[s.id] }))
    .filter(s => s.name)
})

async function poll() {
  try {
    const fresh = await api.getLineupBattleState(props.roomCode)
    applyState(fresh)
  } catch (e) {
    error.value = 'Lost connection to the room - retrying…'
  } finally {
    loading.value = false
  }
}

function applyState(fresh) {
  error.value = ''
  if (fresh.currentLineupIndex !== lastLineupIndexSeen) {
    scoresAtLineupStart.value = Object.fromEntries((fresh.players || []).map(p => [p.name, p.totalScore]))
    lastLineupIndexSeen = fresh.currentLineupIndex
    revealMap.value = {}
  }
  state.value = fresh
  if (fresh.lineupComplete && Object.keys(revealMap.value).length === 0) {
    api.getMultiplayerLineupReveal(fresh.currentLineupId).then(map => { revealMap.value = map }).catch(() => {})
  }
  if (fresh.finished) {
    stopPolling()
    const scores = fresh.players.map(p => [p.name, p.totalScore])
    emit('gameOver', scores)
  }
}

const { stop: stopPolling } = usePolling(poll, 1200)

let searchDebounce = null
watch(searchTerm, (val) => {
  clearTimeout(searchDebounce)
  const trimmed = (val || '').trim()
  if (!trimmed) {
    searchResults.value = []
    return
  }
  searchDebounce = setTimeout(async () => {
    try {
      const results = await api.searchLineupCandidates(state.value.currentLineupId, val)
      searchResults.value = trimmed.length < 3
        ? results.filter(a => a.name.toLowerCase() === trimmed.toLowerCase())
        : results
    } catch (e) {
      // autocomplete failing isn't worth surfacing
    }
  }, 250)
})

async function submitGuess(athlete) {
  guessing.value = true
  searchTerm.value = ''
  searchResults.value = []
  try {
    const before = state.value.slots.filter(s => s.solved).length
    const fresh = await api.submitLineupBattleGuess(props.roomCode, athlete.id)
    const after = fresh.slots.filter(s => s.solved).length
    if (after > before) {
      showResultOverlay(true)
    } else {
      shakeGuessBox.value = true
      setTimeout(() => { shakeGuessBox.value = false }, 400)
      showResultOverlay(false)
    }
    applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit that guess.'
  } finally {
    guessing.value = false
  }
}

async function chooseLineup(l) {
  choosing.value = true
  try {
    const fresh = await api.chooseLineupBattleLineup(props.roomCode, l.id)
    applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not choose that board.'
  } finally {
    choosing.value = false
  }
}

const showSkipConfirm = ref(false)
function confirmSkipTurn() {
  showSkipConfirm.value = false
  skipTurn()
}

async function skipTurn() {
  guessing.value = true
  searchTerm.value = ''
  searchResults.value = []
  try {
    const fresh = await api.skipLineupBattleTurn(props.roomCode)
    applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not skip your turn.'
  } finally {
    guessing.value = false
  }
}

async function nextLineup() {
  advancing.value = true
  try {
    const fresh = await api.advanceLineupBattleLineup(props.roomCode)
    applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not advance to the next board.'
  } finally {
    advancing.value = false
  }
}

function leave() {
  stopPolling()
  emit('leave')
}
</script>
