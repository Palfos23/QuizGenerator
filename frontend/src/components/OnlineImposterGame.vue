<template>
  <div>
    <div class="fiveoo-header">
      <div class="grid-progress" style="text-align:center;">Board {{ (state?.currentGridIndex ?? 0) + 1 }} / {{ state?.totalGrids ?? '?' }}</div>
      <h2>{{ state?.gridTitle }}</h2>
      <p v-if="state?.gridDescription" class="fiveoo-description">{{ state.gridDescription }}</p>
      <p v-if="lastUpdatedLabel" class="fiveoo-description" style="font-size:0.75rem;">{{ lastUpdatedLabel }}</p>
      <p class="fiveoo-rules-reminder">Fewest imposter hits wins - flip a tile on your turn</p>
    </div>

    <LoadingState v-if="loading" full message="Loading the board…" />
    <div v-if="error" class="banner error">{{ error }}</div>

    <template v-if="state && !state.finished">
      <div v-if="state.awaitingGridChoice" class="tension-choice-overlay">
        <template v-if="isPicker">
          <div style="color:var(--gold); text-transform:uppercase; letter-spacing:0.5px; font-size:1rem; margin-bottom:6px;">
            Board {{ state.currentGridIndex + 1 }} / {{ state.totalGrids }}
          </div>
          <h2 style="margin:0 0 24px;">Choose a board</h2>
          <div class="tension-choice-grid">
            <button
              v-for="g in state.gridChoices"
              :key="g.id"
              class="tension-choice-card"
              :disabled="choosing"
              @click="chooseGrid(g)"
            >
              <strong>{{ g.title }}</strong>
              <div style="color:var(--text-dim); font-size:0.85rem; margin-top:4px; font-weight:400;">
                {{ g.tileCount }} tiles, {{ g.imposterCount }} imposter<span v-if="g.imposterCount !== 1">s</span><span v-if="g.description"> · {{ g.description }}</span>
              </div>
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
          :class="{ 'active-turn': p.participantId === state.currentTurnParticipantId && !state.boardComplete }"
          :style="{ borderColor: p.color }"
        >
          <strong>{{ p.name }}</strong>
          <div style="font-size:1.8rem; font-weight:700; margin-top:4px;" :style="{ color: p.totalScore === 0 ? 'var(--teal)' : 'var(--text)' }">
            {{ p.totalScore }}
          </div>
        </div>
      </div>

      <p v-if="!state.boardComplete" style="text-align:center; margin:0 0 12px;" :style="{ color: isYourTurn ? 'var(--gold)' : 'var(--text-dim)' }">
        {{ isYourTurn ? "Your turn - pick a tile" : `Waiting for ${currentTurnName}'s turn…` }}
      </p>

      <div class="grid-tiles">
        <div
          v-for="t in state.tiles"
          :key="t.id"
          class="grid-tile"
          :class="tileClass(t)"
          @click="flipTile(t)"
          :style="{ cursor: (state.boardComplete || !isYourTurn) ? 'default' : 'pointer' }"
        >
          <div v-if="isNameOnlyTile(t)" class="grid-tile-name-fill">{{ t.athleteName }}</div>
          <template v-else>
            <img
              v-if="tileImage(t)"
              :src="tileImage(t)"
              alt=""
              class="grid-tile-logo"
              :class="{
                'is-photo': !!t.photoUrl && state.displayMode !== 'NAME_AND_LOGO',
                'is-fit': state.fitImages && !!t.photoUrl && state.displayMode !== 'NAME_AND_LOGO'
              }"
              @error="$event.target.style.display = 'none'"
            />
            <div v-if="state.displayMode !== 'PHOTO_ONLY' || t.flipped" class="grid-tile-name">{{ t.athleteName }}</div>
          </template>
          <span v-if="t.flipped" class="grid-tile-status" :class="t.imposter ? 'wrong' : 'correct'">
            {{ t.imposter ? '✕' : '✓' }}
          </span>
        </div>
      </div>

      <div v-if="state.boardComplete" class="modal-backdrop">
        <div class="modal">
          <h2 style="margin-top:0;">Board complete!</h2>

          <div class="score-square-grid">
            <div v-for="(p, i) in sortedPlayers" :key="p.participantId" class="score-square" :class="{ leader: i === 0 }">
              <div class="score-square-name">{{ p.name }}</div>
              <div class="score-square-number">{{ p.totalScore }}</div>
            </div>
          </div>

          <div v-if="!revealList.length" style="color:var(--text-dim);">No imposters on this board.</div>
          <div v-for="(r, i) in revealList" :key="i" class="imposter-reveal-entry">
            <div>
              <span class="imposter-reveal-entry-name">{{ r.imposterName }}</span>
              <span v-if="r.replacedName" class="imposter-reveal-entry-replaced">replaced {{ r.replacedName }}</span>
            </div>
            <span v-if="r.flippedByName" class="imposter-reveal-entry-foundby">Found by {{ r.flippedByName }}</span>
          </div>

          <button v-if="isHost" class="btn btn-primary" style="margin-top:16px; width:100%;" :disabled="advancing" @click="nextBoard">
            {{ advancing ? 'Loading…' : (state.currentGridIndex + 1 < state.totalGrids ? 'Next board' : 'See final results') }}
          </button>
          <div v-else style="margin-top:8px; color:var(--text-dim);">Waiting for the host to continue…</div>
        </div>
      </div>
      </template>
    </template>

    <div style="display:flex; align-items:center; gap:12px; margin-top:20px; flex-wrap:wrap;">
      <button class="btn btn-secondary btn-sm no-print" @click="leave">← Leave game</button>
      <span class="tag no-print" style="background:rgba(255,255,255,0.06); color:var(--text-dim);">Room: {{ roomCode }}</span>
    </div>

    <div v-if="flipOverlay" class="imposter-flip-overlay" :class="flipOverlay.imposter ? 'imposter-hit' : 'imposter-fit'">
      <div class="imposter-overlay-text">{{ flipOverlay.imposter ? 'IMPOSTER!' : 'Correct' }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import api from '../services/api'
import LoadingState from './LoadingState.vue'
import { usePolling } from '../composables/usePolling'
import { formatLastUpdated } from '../constants'

const props = defineProps({
  roomCode: { type: String, required: true },
  yourParticipantId: { type: [Number, String], required: true },
  isHost: { type: Boolean, default: false }
})
const emit = defineEmits(['gameOver', 'leave'])

const state = ref(null)
const lastUpdatedLabel = computed(() => formatLastUpdated(state.value?.gridUpdatedAt))
const revealList = ref([])
let lastGridIndexSeen = null
const loading = ref(true)
const error = ref('')
const flipping = ref(false)
const advancing = ref(false)

const flipOverlay = ref(null)
let overlayTimeout = null
function showFlipOverlay(imposter) {
  clearTimeout(overlayTimeout)
  flipOverlay.value = null
  requestAnimationFrame(() => {
    flipOverlay.value = { imposter }
    overlayTimeout = setTimeout(() => { flipOverlay.value = null }, 1200)
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
const sortedPlayers = computed(() => {
  if (!state.value) return []
  return [...state.value.players].sort((a, b) => a.totalScore - b.totalScore) // fewest imposter hits wins
})

function tileImage(t) {
  if (state.value.displayMode === 'NAME_AND_LOGO') return t.logoUrl
  return t.photoUrl
}

// Same reasoning as ImposterGame.vue's isNameOnlyTile - NAME_UNTIL_REVEALED
// withholds the photo until this specific tile has been flipped.
function isNameOnlyTile(t) {
  const mode = state.value.displayMode
  return mode === 'NAME_ONLY' || (mode === 'NAME_UNTIL_REVEALED' && !t.flipped)
}

function tileClass(t) {
  if (!t.flipped) return {}
  return { correct: !t.imposter, 'revealed-only': t.imposter }
}

async function poll() {
  try {
    const fresh = await api.getImposterOnlineState(props.roomCode)
    applyState(fresh)
  } catch (e) {
    error.value = 'Lost connection to the room - retrying…'
  } finally {
    loading.value = false
  }
}

function applyState(fresh) {
  error.value = ''
  if (fresh.currentGridIndex !== lastGridIndexSeen) {
    lastGridIndexSeen = fresh.currentGridIndex
    revealList.value = []
  }
  state.value = fresh
  if (fresh.boardComplete && revealList.value.length === 0) {
    api.getImposterOnlineReveal(props.roomCode).then(list => { revealList.value = list }).catch(() => {})
  }
  if (fresh.finished) {
    stopPolling()
    const scores = fresh.players.map(p => [p.name, p.totalScore])
    emit('gameOver', scores)
  }
}

const { stop: stopPolling } = usePolling(poll, 1200)

async function chooseGrid(g) {
  choosing.value = true
  try {
    const fresh = await api.chooseImposterOnlineGrid(props.roomCode, g.id)
    applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not choose that board.'
  } finally {
    choosing.value = false
  }
}

async function flipTile(t) {
  if (flipping.value || t.flipped || !state.value || state.value.boardComplete || !isYourTurn.value) return
  flipping.value = true
  try {
    const before = state.value.tiles.find(x => x.id === t.id)
    const fresh = await api.flipImposterOnlineTile(props.roomCode, t.id)
    const after = fresh.tiles.find(x => x.id === t.id)
    if (after && after.flipped && !before.flipped) {
      showFlipOverlay(after.imposter)
    }
    applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not flip that tile.'
  } finally {
    flipping.value = false
  }
}

async function nextBoard() {
  advancing.value = true
  try {
    const fresh = await api.advanceImposterOnlineBoard(props.roomCode)
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
