<template>
  <div>
    <div class="fiveoo-header">
      <div class="grid-progress" style="text-align:center;">Board {{ (state?.currentGridIndex ?? 0) + 1 }} / {{ state?.totalGrids ?? '?' }}</div>
      <h2>{{ state?.gridTitle }}</h2>
      <p v-if="state?.gridDescription" class="fiveoo-description">{{ state.gridDescription }}</p>
      <p class="fiveoo-rules-reminder">Fewest imposter hits wins - flip a tile on your turn</p>
    </div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
    <div v-if="error" class="banner error">{{ error }}</div>

    <template v-if="state && !state.finished">
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
          <div v-if="state.displayMode === 'NAME_ONLY'" class="grid-tile-name-fill">{{ t.athleteName }}</div>
          <template v-else>
            <img
              v-if="tileImage(t)"
              :src="tileImage(t)"
              alt=""
              class="grid-tile-logo"
              :class="{ 'is-photo': !!t.photoUrl && state.displayMode !== 'NAME_AND_LOGO' }"
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
import { computed, onMounted, onUnmounted, ref } from 'vue'
import api from '../services/api'

const props = defineProps({
  roomCode: { type: String, required: true },
  yourParticipantId: { type: [Number, String], required: true },
  isHost: { type: Boolean, default: false }
})
const emit = defineEmits(['gameOver', 'leave'])

const state = ref(null)
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

let pollTimer = null

const isYourTurn = computed(() => !!state.value && state.value.currentTurnParticipantId === props.yourParticipantId)
const currentTurnName = computed(() =>
  state.value?.players.find(p => p.participantId === state.value.currentTurnParticipantId)?.name || '…'
)
const sortedPlayers = computed(() => {
  if (!state.value) return []
  return [...state.value.players].sort((a, b) => a.totalScore - b.totalScore) // fewest imposter hits wins
})

function tileImage(t) {
  if (state.value.displayMode === 'NAME_AND_LOGO') return t.logoUrl
  return t.photoUrl
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
    clearInterval(pollTimer)
    const scores = fresh.players.map(p => [p.name, p.totalScore])
    emit('gameOver', scores)
  }
}

onMounted(() => {
  poll()
  pollTimer = setInterval(poll, 1200)
})
onUnmounted(() => clearInterval(pollTimer))

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
  clearInterval(pollTimer)
  emit('leave')
}
</script>
