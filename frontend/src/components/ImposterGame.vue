<template>
  <div>
    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <template v-else-if="playState">
      <div class="fiveoo-header">
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
          <img
            v-if="tileImage(t)"
            :src="tileImage(t)"
            alt=""
            class="grid-tile-logo"
            :class="{ 'is-photo': !!t.photoUrl && playState.displayMode !== 'NAME_AND_LOGO' }"
            @error="$event.target.style.display = 'none'"
          />
          <span v-if="flippedTiles[t.id]" class="grid-tile-status" :class="flippedTiles[t.id].imposter ? 'wrong' : 'correct'">
            {{ flippedTiles[t.id].imposter ? '✕' : '✓' }}
          </span>
          <div v-if="playState.displayMode !== 'PHOTO_ONLY'" class="grid-tile-name">{{ t.athleteName }}</div>
        </div>
      </div>
    </template>

    <div v-if="flipOverlay" class="imposter-flip-overlay" :class="flipOverlay.imposter ? 'imposter-hit' : 'imposter-fit'">
      <div class="imposter-overlay-text">{{ flipOverlay.imposter ? 'IMPOSTER!' : 'Correct' }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import passAndPlayState from '../services/passAndPlayState'

const props = defineProps({
  gridId: { type: [Number, String], required: true },
  players: { type: Array, required: true }
})
const emit = defineEmits(['game-over'])

const loading = ref(true)
const error = ref('')
const playState = ref(null)
const flippedTiles = reactive({}) // tileId -> { imposter: bool }
const currentPlayerIdx = ref(0)
const scores = reactive({})
const flipOverlay = ref(null)
const flipping = ref(false)
let overlayTimeout = null

const currentPlayer = computed(() => props.players[currentPlayerIdx.value])

onMounted(async () => {
  for (const p of props.players) scores[p] = 0
  await loadPlayState()
})

async function loadPlayState() {
  loading.value = true
  error.value = ''
  try {
    playState.value = await api.getImposterPlayState(props.gridId)
  } catch (e) {
    error.value = 'Could not load this board.'
  } finally {
    loading.value = false
  }
}

function tileImage(t) {
  if (playState.value.displayMode === 'NAME_AND_LOGO') return t.logoUrl
  return t.photoUrl
}

function tileClass(t) {
  const flipped = flippedTiles[t.id]
  if (!flipped) return {}
  return { correct: !flipped.imposter, 'revealed-only': flipped.imposter }
}

async function flipTile(t) {
  if (flipping.value || flippedTiles[t.id]) return
  flipping.value = true
  try {
    const result = await api.flipImposterTile(props.gridId, t.id)
    flippedTiles[t.id] = { imposter: result.imposter }
    if (result.imposter) scores[currentPlayer.value]++
    showFlipOverlay(result.imposter)

    const allFlipped = playState.value.tiles.every(tile => flippedTiles[tile.id])
    if (allFlipped) {
      await finishGame()
      return
    }

    currentPlayerIdx.value = (currentPlayerIdx.value + 1) % props.players.length
    passAndPlayState.save('imposter', { gridId: props.gridId, players: props.players })
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

async function finishGame() {
  let revealList = []
  try {
    revealList = await api.getImposterReveal(props.gridId)
  } catch (e) {
    // reveal failing shouldn't block showing final scores
  }
  setTimeout(() => {
    emit('game-over', { scores: Object.entries(scores), revealList })
  }, 1300) // let the last flip's overlay finish playing first
}
</script>
