<template>
  <div ref="rootEl" class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av et Imposter-brett: ruter snus én og én, de fleste hører til temaet, men to er imposters.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>
    <div class="pv-title">{{ $t('Toppscorere i Premier League 2023/24') }}</div>

    <div class="pv-players">
      <div v-for="p in PLAYERS" :key="p" class="pv-player" :class="{ active: turn === p }">
        <strong>{{ p }}</strong>
        <span class="pv-player-score" :class="{ bad: hits[p] > 0 }">{{ hits[p] }} ✕</span>
      </div>
    </div>

    <div class="grid-tiles pv-imposter-grid">
      <div
        v-for="t in TILES"
        :key="t.name"
        class="grid-tile"
        :class="{ correct: isOpen(t) && !t.imposter, 'revealed-only': isOpen(t) && t.imposter, 'just-solved': justOpened === t.name }"
      >
        <div class="grid-tile-name-fill">{{ t.name }}</div>
        <span v-if="isOpen(t)" class="grid-tile-status" :class="t.imposter ? 'wrong' : 'correct'">{{ t.imposter ? '✕' : '✓' }}</span>
      </div>
    </div>

    <p class="game-preview-caption">
      {{ $t('Snu ruter etter tur – de fleste hører til temaet, noen få gjør det ikke.') }}
      <strong>{{ $t('Færrest') }} <span class="bad">imposter</span>{{ $t('-treff vinner.') }}</strong>
    </p>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { usePreviewLoop } from '../../composables/usePreviewLoop'

// The film, on the real tiles: Jo and Sam take turns flipping; Watkins and Isak fit the theme,
// Maguire is an imposter.
const rootEl = ref(null)
const PLAYERS = ['Jo', 'Sam']
const TILES = [
  { name: 'Haaland' }, { name: 'Watkins' }, { name: 'Maguire', imposter: true },
  { name: 'Palmer' }, { name: 'Isak' }, { name: 'Rodri', imposter: true }
]

const opened = ref([])
const justOpened = ref(null)
const turn = ref('Jo')
const hits = reactive({ Jo: 0, Sam: 0 })
const isOpen = t => opened.value.includes(t.name)

function reset() { opened.value = []; justOpened.value = null; turn.value = 'Jo'; hits.Jo = 0; hits.Sam = 0 }
function flip(name) {
  const tile = TILES.find(t => t.name === name)
  opened.value = [...opened.value, name]; justOpened.value = name
  if (tile.imposter) hits[turn.value]++
  turn.value = turn.value === 'Jo' ? 'Sam' : 'Jo'
}

usePreviewLoop(rootEl, {
  async play(sleep) {
    reset(); await sleep(1000)
    flip('Watkins'); await sleep(1200)
    flip('Isak'); await sleep(1200)
    flip('Maguire'); await sleep(3200)
  },
  finalFrame() { reset(); flip('Watkins'); flip('Isak'); flip('Maguire') }
})
</script>

<style scoped>
.pv-imposter-grid { grid-template-columns: repeat(3, 1fr); max-width: 360px; margin: 10px auto 0; }
.pv-imposter-grid .grid-tile { aspect-ratio: 3 / 2; }
.pv-imposter-grid .grid-tile-name-fill { font-size: 0.85rem; }
</style>
