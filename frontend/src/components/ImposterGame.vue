<template>
  <div>
    <div v-if="error" class="banner error">{{ error }}</div>

    <LoadingState v-if="loadingChoices" message="Loading board choices…" full />

    <div v-else-if="roundChoices.length" class="tension-choice-overlay">
      <div style="color:var(--gold); text-transform:uppercase; letter-spacing:0.5px; font-size:1rem; margin-bottom:6px;">
        Board {{ currentGridIndex + 1 }} / {{ totalBoards }}
      </div>
      <h2 style="margin:0 0 24px;">{{ pickerName }}, choose a board</h2>
      <div class="tension-choice-grid">
        <button v-for="g in roundChoices" :key="g.id" class="tension-choice-card" @click="chooseBoard(g)">
          <strong>{{ g.title }}</strong>
          <div style="color:var(--text-dim); font-size:0.85rem; margin-top:4px; font-weight:400;">
            {{ g.tileCount }} tiles, {{ g.imposterCount }} imposter<span v-if="g.imposterCount !== 1">s</span><span v-if="g.description"> · {{ g.description }}</span>
          </div>
        </button>
      </div>
    </div>

    <LoadingState v-else-if="loading" full message="Loading the board…" />

    <template v-else-if="playState">
      <div class="fiveoo-header">
        <div class="grid-progress" style="text-align:center;">Board {{ currentGridIndex + 1 }} / {{ totalBoards }}</div>
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
          :style="{ cursor: boardFinished ? 'default' : 'pointer' }"
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
            <div v-if="playState.displayMode !== 'PHOTO_ONLY' || flippedTiles[t.id]" class="grid-tile-name">{{ t.athleteName }}</div>
          </template>
          <span v-if="flippedTiles[t.id]" class="grid-tile-status" :class="flippedTiles[t.id].imposter ? 'wrong' : 'correct'">
            {{ flippedTiles[t.id].imposter ? '✕' : '✓' }}
          </span>
        </div>
      </div>
    </template>

    <div v-else class="empty-state">
      <p>Couldn't load this board.</p>
      <button class="btn btn-primary" @click="proceedToCurrentRound">Try again</button>
    </div>

    <div v-if="flipOverlay" class="imposter-flip-overlay" :class="flipOverlay.imposter ? 'imposter-hit' : 'imposter-fit'">
      <div class="imposter-overlay-text">{{ flipOverlay.imposter ? 'IMPOSTER!' : 'Correct' }}</div>
    </div>

    <div v-if="boardRevealModal" class="modal-backdrop">
      <div class="modal">
        <h2 style="margin-top:0;">Board complete!</h2>

        <div class="score-square-grid">
          <div v-for="([name, points], i) in sortedScoresForModal" :key="name" class="score-square" :class="{ leader: i === 0 }">
            <div class="score-square-name">{{ name }}</div>
            <div class="score-square-number">{{ points }}</div>
          </div>
        </div>

        <div v-if="!boardRevealModal.reveal.length" style="color:var(--text-dim);">No imposters on this board.</div>
        <div v-for="(r, i) in boardRevealModal.reveal" :key="i" class="imposter-reveal-entry">
          <div>
            <span class="imposter-reveal-entry-name">{{ r.imposterName }}</span>
            <span v-if="r.replacedName" class="imposter-reveal-entry-replaced">replaced {{ r.replacedName }}</span>
          </div>
          <span v-if="r.flippedByPlayer" class="imposter-reveal-entry-foundby">Found by {{ r.flippedByPlayer }}</span>
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
import LoadingState from './LoadingState.vue'

const props = defineProps({
  mode: { type: String, default: 'manual' }, // 'manual' | 'random'
  gridIds: { type: Array, default: () => [] }, // fixed list, mode === 'manual' only
  numBoards: { type: Number, default: 0 }, // total rounds, mode === 'random' only
  players: { type: Array, required: true }
})
const emit = defineEmits(['game-over'])

const totalBoards = computed(() => props.mode === 'random' ? props.numBoards : props.gridIds.length)
// This round's starting player rotates by seat, same convention as Tension's
// rotatedPlayers[0] - and since they're also the one who picks in "random"
// mode, this doubles as the picker's name before a board is even chosen.
const pickerName = computed(() => props.players[currentGridIndex.value % props.players.length])
const currentGridId = computed(() => props.mode === 'manual'
  ? props.gridIds[currentGridIndex.value]
  : chosenGridIds.value[currentGridIndex.value])

const chosenGridIds = ref([]) // board ids actually picked so far ('random' mode only), index-aligned with currentGridIndex
const roundChoices = ref([]) // this round's 3 candidate boards, before a pick is made ('random' mode only)
const loadingChoices = ref(false)
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
const boardFinished = ref(false) // true the instant the board's outcome is decided, even before the reveal modal visually appears
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

function identityMatches(saved) {
  if (!saved) return false
  if (props.mode === 'manual') {
    return saved.mode !== 'random'
      && JSON.stringify(saved.gridIds) === JSON.stringify(props.gridIds)
      && JSON.stringify(saved.players) === JSON.stringify(props.players)
  }
  return saved.mode === 'random'
    && saved.numBoards === props.numBoards
    && JSON.stringify(saved.players) === JSON.stringify(props.players)
}

onMounted(async () => {
  const saved = passAndPlayState.load('imposter')
  if (identityMatches(saved)) {
    restoring = true
    currentGridIndex.value = saved.currentGridIndex ?? 0
    currentPlayerIdx.value = saved.currentPlayerIdx ?? 0
    Object.assign(flippedTiles, saved.flippedTiles || {})
    for (const p of props.players) scores[p] = saved.scores?.[p] ?? 0
    accumulatedReveal.value = saved.accumulatedReveal || []
    Object.assign(tileOrders, saved.tileOrders || {})
    chosenGridIds.value = saved.chosenGridIds || []
  } else {
    for (const p of props.players) scores[p] = 0
  }
  await proceedToCurrentRound()
  restoring = false
})

async function loadRoundChoices() {
  loadingChoices.value = true
  error.value = ''
  try {
    roundChoices.value = await api.fetchImposterBattleRoundChoices(3, chosenGridIds.value)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not load the next round.'
    loading.value = false // loadPlayState never ran to clear this - avoid a stuck spinner
  } finally {
    loadingChoices.value = false
  }
}

function chooseBoard(g) {
  chosenGridIds.value = [...chosenGridIds.value, g.id]
  roundChoices.value = []
  loadPlayState(g.id)
}

// Manual mode already knows every board up front. Random mode picks one
// round at a time - resume straight into an already-chosen round rather than
// re-offering a choice, since a choice already made is a commitment;
// otherwise offer this round's 3 choices.
function proceedToCurrentRound() {
  if (props.mode === 'manual') {
    return loadPlayState(props.gridIds[currentGridIndex.value])
  } else if (chosenGridIds.value.length > currentGridIndex.value) {
    return loadPlayState(chosenGridIds.value[currentGridIndex.value])
  } else {
    return loadRoundChoices()
  }
}

async function loadPlayState(gridId) {
  loading.value = true
  error.value = ''
  boardFinished.value = false
  try {
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
    mode: props.mode,
    gridIds: props.mode === 'manual' ? props.gridIds : undefined,
    numBoards: props.mode === 'random' ? props.numBoards : undefined,
    players: props.players,
    currentGridIndex: currentGridIndex.value,
    currentPlayerIdx: currentPlayerIdx.value,
    flippedTiles: { ...flippedTiles },
    scores: { ...scores },
    accumulatedReveal: accumulatedReveal.value,
    tileOrders: { ...tileOrders },
    chosenGridIds: chosenGridIds.value
  })
}
watch([currentGridIndex, currentPlayerIdx, flippedTiles, scores, accumulatedReveal, tileOrders, chosenGridIds], saveProgress, { deep: true })

async function flipTile(t) {
  if (flipping.value || flippedTiles[t.id] || boardFinished.value) return
  flipping.value = true
  try {
    const result = await api.flipImposterTile(currentGridId.value, t.id)
    flippedTiles[t.id] = { imposter: result.imposter, revealPhotoUrl: result.revealPhotoUrl, player: currentPlayer.value }
    if (result.imposter) scores[currentPlayer.value]++
    showFlipOverlay(result.imposter)

    const allFlipped = playState.value.tiles.every(tile => flippedTiles[tile.id])
    const fitsFoundSoFar = playState.value.tiles.filter(tile => flippedTiles[tile.id] && !flippedTiles[tile.id].imposter).length
    const totalFitsOnBoard = playState.value.tiles.length - playState.value.imposterCount
    const onlyImpostersRemain = fitsFoundSoFar === totalFitsOnBoard

    if (allFlipped || onlyImpostersRemain) {
      boardFinished.value = true // block further flips immediately, well before the reveal modal visually appears
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
    revealList = await api.getImposterReveal(currentGridId.value)
    revealList = revealList.map(r => ({ ...r, flippedByPlayer: flippedTiles[r.tileId]?.player || null }))
  } catch (e) {
    // reveal failing for one board shouldn't block the rest of the session
  }
  accumulatedReveal.value = [...accumulatedReveal.value, ...revealList]

  setTimeout(() => {
    boardRevealModal.value = {
      reveal: revealList,
      isLastBoard: currentGridIndex.value + 1 >= totalBoards.value
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
    proceedToCurrentRound()
  }
}
</script>
