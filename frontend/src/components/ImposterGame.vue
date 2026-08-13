<template>
  <div>
    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <template v-else-if="playState">
      <div class="fiveoo-header">
        <div class="grid-progress" style="text-align:center;">Board {{ currentGridIndex + 1 }} / {{ gridIds.length }}</div>
        <h2>{{ playState.title }}</h2>
        <p v-if="playState.description" class="fiveoo-description">{{ playState.description }}</p>
        <p class="fiveoo-rules-reminder">Fewest imposter hits wins - flip a tile on your turn</p>
      </div>

      <div class="mp-player-row">
        <div
          v-for="p in players"
          :key="p"
          class="mp-player-card"
          :class="{ 'active-turn': p === currentPlayer }"
        >
          <strong>{{ p }}</strong>
          <div style="font-size:1.8rem; font-weight:700; margin-top:4px;" :style="{ color: scores[p] === 0 ? 'var(--teal)' : 'var(--text)' }">
            {{ scores[p] }}
          </div>
        </div>
      </div>

      <p style="text-align:center; margin:0 0 12px; color:var(--gold); font-weight:600;">{{ currentPlayer }}'s turn - pick a tile</p>

      <div class="grid-tiles">
        <div
          v-for="t in playState.tiles"
          :key="t.id"
          class="grid-tile"
          :class="tileClass(t)"
          @click="flipTile(t)"
          style="cursor:pointer;"
        >
          <div v-if="playState.displayMode === 'NAME_ONLY'" class="grid-tile-name-fill">{{ t.athleteName }}</div>
          <template v-else>
            <img
              v-if="tileImage(t)"
              :src="tileImage(t)"
              alt=""
              class="grid-tile-logo"
              :class="{ 'is-photo': !!t.photoUrl && playState.displayMode !== 'NAME_AND_LOGO' }"
              @error="$event.target.style.display = 'none'"
            />
            <div v-if="playState.displayMode !== 'PHOTO_ONLY'" class="grid-tile-name">{{ t.athleteName }}</div>
          </template>
          <span v-if="flippedTiles[t.id]" class="grid-tile-status" :class="flippedTiles[t.id].imposter ? 'wrong' : 'correct'">
            {{ flippedTiles[t.id].imposter ? '✕' : '✓' }}
          </span>
        </div>
      </div>
    </template>

    <div v-if="flipOverlay" class="imposter-flip-overlay" :class="flipOverlay.imposter ? 'imposter-hit' : 'imposter-fit'">
      <div class="imposter-overlay-text">{{ flipOverlay.imposter ? 'IMPOSTER!' : 'Correct' }}</div>
    </div>

    <div v-if="boardRevealModal" class="modal-backdrop">
      <div class="modal">
        <h2 style="margin-top:0;">The imposters on this board</h2>

        <div style="margin-bottom:16px;">
          <div
            v-for="([name, points], i) in sortedScoresForModal"
            :key="name"
            style="display:flex; justify-content:space-between; padding:3px 8px; border-radius:6px;"
            :class="{ 'tension-winner-row': i === 0 }"
          >
            <span>{{ name }}</span>
            <span>{{ points }}</span>
          </div>
        </div>

        <div v-if="!boardRevealModal.reveal.length" style="color:var(--text-dim);">No imposters on this board.</div>
        <div v-for="(r, i) in boardRevealModal.reveal" :key="i" style="padding:6px 0; border-bottom:1px solid var(--border);">
          <strong style="color:var(--coral);">{{ r.imposterName }}</strong>
          <span v-if="r.replacedName"> replaced <strong style="color:var(--text);">{{ r.replacedName }}</strong></span>
          <span v-if="r.flippedByPlayer" style="display:block; color:var(--text-dim); font-size:0.82rem;">Found by {{ r.flippedByPlayer }}</span>
        </div>
        <button class="btn btn-primary" style="margin-top:16px; width:100%;" @click="continueAfterReveal">
          {{ boardRevealModal.isLastBoard ? 'See final results' : 'Next board →' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import passAndPlayState from '../services/passAndPlayState'

const props = defineProps({
  gridIds: { type: Array, required: true },
  players: { type: Array, required: true }
})
const emit = defineEmits(['game-over'])

const loading = ref(true)
const error = ref('')
const playState = ref(null)
const flippedTiles = reactive({}) // tileId -> { imposter: bool } - across the whole session, not just the current board
const currentGridIndex = ref(0)
const currentPlayerIdx = ref(0)
const scores = reactive({}) // accumulates across every board in this session
const accumulatedReveal = ref([]) // reveal entries from every completed board so far
const boardRevealModal = ref(null) // { reveal, isLastBoard } shown between boards, or null when hidden
const tileOrders = reactive({}) // gridId -> array of tile ids in the shuffled order for this session
const flipOverlay = ref(null)
const flipping = ref(false)
let overlayTimeout = null
let restoring = false // true while applying saved progress, so that initial restore doesn't itself get saved as a redundant write

function shuffledIds(ids) {
  // Fisher-Yates - unbiased, and doesn't mutate the input array
  const arr = [...ids]
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr
}

const currentPlayer = computed(() => props.players[currentPlayerIdx.value])

const sortedScoresForModal = computed(() => {
  return Object.entries(scores).sort((a, b) => a[1] - b[1]) // fewest imposter hits wins
})

onMounted(async () => {
  const saved = passAndPlayState.load('imposter')
  if (saved && saved.gridIds && JSON.stringify(saved.gridIds) === JSON.stringify(props.gridIds)
      && JSON.stringify(saved.players) === JSON.stringify(props.players)) {
    restoring = true
    currentGridIndex.value = saved.currentGridIndex ?? 0
    currentPlayerIdx.value = saved.currentPlayerIdx ?? 0
    Object.assign(flippedTiles, saved.flippedTiles || {})
    for (const p of props.players) scores[p] = saved.scores?.[p] ?? 0
    accumulatedReveal.value = saved.accumulatedReveal || []
    Object.assign(tileOrders, saved.tileOrders || {})
  } else {
    for (const p of props.players) scores[p] = 0
  }
  await loadPlayState()
  restoring = false
})

async function loadPlayState() {
  loading.value = true
  error.value = ''
  try {
    const gridId = props.gridIds[currentGridIndex.value]
    const state = await api.getImposterPlayState(gridId)

    let order = tileOrders[gridId]
    if (!order) {
      order = shuffledIds(state.tiles.map(t => t.id))
      tileOrders[gridId] = order
    }
    const tilesById = new Map(state.tiles.map(t => [t.id, t]))
    state.tiles = order.map(id => tilesById.get(id)).filter(Boolean)

    playState.value = state
  } catch (e) {
    error.value = 'Could not load this board.'
  } finally {
    loading.value = false
  }
}

function tileImage(t) {
  const flipped = flippedTiles[t.id]
  if (flipped && flipped.revealPhotoUrl) return flipped.revealPhotoUrl
  if (playState.value.displayMode === 'NAME_AND_LOGO') return t.logoUrl
  return t.photoUrl
}

function tileClass(t) {
  const flipped = flippedTiles[t.id]
  if (!flipped) return {}
  return { correct: !flipped.imposter, 'revealed-only': flipped.imposter }
}

function saveProgress() {
  if (restoring) return
  passAndPlayState.save('imposter', {
    gridIds: props.gridIds,
    players: props.players,
    currentGridIndex: currentGridIndex.value,
    currentPlayerIdx: currentPlayerIdx.value,
    flippedTiles: { ...flippedTiles },
    scores: { ...scores },
    accumulatedReveal: accumulatedReveal.value,
    tileOrders: { ...tileOrders }
  })
}
watch([currentGridIndex, currentPlayerIdx, flippedTiles, scores, accumulatedReveal, tileOrders], saveProgress, { deep: true })

async function flipTile(t) {
  if (flipping.value || flippedTiles[t.id]) return
  flipping.value = true
  try {
    const result = await api.flipImposterTile(props.gridIds[currentGridIndex.value], t.id)
    flippedTiles[t.id] = { imposter: result.imposter, revealPhotoUrl: result.revealPhotoUrl, player: currentPlayer.value }
    if (result.imposter) scores[currentPlayer.value]++
    showFlipOverlay(result.imposter)

    const allFlipped = playState.value.tiles.every(tile => flippedTiles[tile.id])
    const fitsFoundSoFar = playState.value.tiles.filter(tile => flippedTiles[tile.id] && !flippedTiles[tile.id].imposter).length
    const totalFitsOnBoard = playState.value.tiles.length - playState.value.imposterCount
    const onlyImpostersRemain = fitsFoundSoFar === totalFitsOnBoard

    if (allFlipped || onlyImpostersRemain) {
      await finishBoard()
      return
    }

    currentPlayerIdx.value = (currentPlayerIdx.value + 1) % props.players.length
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not flip that tile.'
  } finally {
    flipping.value = false
  }
}

function showFlipOverlay(imposter) {
  clearTimeout(overlayTimeout)
  flipOverlay.value = null
  requestAnimationFrame(() => {
    flipOverlay.value = { imposter }
    overlayTimeout = setTimeout(() => { flipOverlay.value = null }, 1200)
  })
}

async function finishBoard() {
  let revealList = []
  try {
    revealList = await api.getImposterReveal(props.gridIds[currentGridIndex.value])
    revealList = revealList.map(r => ({ ...r, flippedByPlayer: flippedTiles[r.tileId]?.player || null }))
  } catch (e) {
    // reveal failing for one board shouldn't block the rest of the session
  }
  accumulatedReveal.value = [...accumulatedReveal.value, ...revealList]

  setTimeout(() => {
    boardRevealModal.value = {
      reveal: revealList,
      isLastBoard: currentGridIndex.value + 1 >= props.gridIds.length
    }
  }, 1300) // let the last flip's overlay finish playing first
}

function continueAfterReveal() {
  const wasLastBoard = boardRevealModal.value.isLastBoard
  boardRevealModal.value = null
  if (wasLastBoard) {
    passAndPlayState.clear('imposter')
    emit('game-over', { scores: Object.entries(scores), revealList: accumulatedReveal.value })
  } else {
    currentGridIndex.value += 1
    // rotate who starts the next board, like Grid Battle and Tension do
    currentPlayerIdx.value = currentGridIndex.value % props.players.length
    loadPlayState()
  }
}
</script>
