<template>
  <div ref="rootEl" class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av Grid Battle: to spillere bytter på å finne spillerne bak klubb og draktnummer, og en feil koster et liv.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>
    <div class="pv-title">{{ $t('Draktnummer · Premier League 2024/25') }}</div>

    <div class="pv-players">
      <div v-for="p in PLAYERS" :key="p" class="pv-player" :class="{ active: turn === p }">
        <strong>{{ p }}</strong>
        <LivesHearts :max="3" :used="lost[p]" />
        <span class="pv-player-score">{{ found[p] }}</span>
      </div>
    </div>

    <div class="grid-tiles grid-board pv-grid">
      <div
        v-for="t in TILES"
        :key="t.id"
        class="grid-tile"
        :class="{ correct: solved.includes(t.id), 'just-solved': justSolved === t.id, shake: shaking === t.id }"
      >
        <span v-if="solved.includes(t.id)" class="grid-tile-status correct">✓</span>
        <svg class="pv-crest" viewBox="0 0 40 46" aria-hidden="true">
          <path d="M20 2 L37 8 V24 C37 35 29 42 20 44 C11 42 3 35 3 24 V8 Z" :fill="t.color" stroke="rgb(255 255 255 / 0.85)" stroke-width="2" />
          <circle cx="20" cy="22" r="7" fill="rgb(255 255 255 / 0.8)" />
        </svg>
        <div class="grid-tile-hint" :style="{ background: 'var(--gold)' }">#{{ t.number }}</div>
        <div class="grid-tile-name">{{ solved.includes(t.id) ? t.name : '?' }}</div>
      </div>
    </div>

    <p class="game-preview-caption">
      {{ $t('Bytt på å nevne spillere som passer.') }} <span class="good">{{ $t('Riktig§GridBattlePreviewT') }}</span> {{ $t('gir et poeng,') }}
      <span class="bad">{{ $t('feil') }}</span> {{ $t('koster et liv.') }}
    </p>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { usePreviewLoop } from '../../composables/usePreviewLoop'
import LivesHearts from '../LivesHearts.vue'

// The film, on the real board: Jo and Sam take turns finding the player behind each club crest and
// shirt number; one wrong guess shakes the tile and costs a life.
const rootEl = ref(null)
const PLAYERS = ['Jo', 'Sam']
const TILES = [
  { id: 1, number: 9, name: 'Haaland', color: '#6cabdd' },
  { id: 2, number: 7, name: 'Saka', color: '#ef0107' },
  { id: 3, number: 11, name: 'Salah', color: '#c8102e' },
  { id: 4, number: 20, name: 'Palmer', color: '#034694' },
  { id: 5, number: 8, name: 'Ødegaard', color: '#ef0107' },
  { id: 6, number: 41, name: 'Rice', color: '#ef0107' }
]

const solved = ref([])
const justSolved = ref(null)
const shaking = ref(null)
const turn = ref('Jo')
const lost = reactive({ Jo: 0, Sam: 0 })
const found = reactive({ Jo: 0, Sam: 0 })

function reset() {
  solved.value = []; justSolved.value = null; shaking.value = null; turn.value = 'Jo'
  lost.Jo = 0; lost.Sam = 0; found.Jo = 0; found.Sam = 0
}
function next() { turn.value = turn.value === 'Jo' ? 'Sam' : 'Jo' }
function right(id) {
  solved.value = [...solved.value, id]; justSolved.value = id; found[turn.value]++; next()
}

usePreviewLoop(rootEl, {
  async play(sleep) {
    reset(); await sleep(1000)
    right(1); await sleep(1100)
    right(3); await sleep(1100)
    shaking.value = 2; lost[turn.value]++; await sleep(500); shaking.value = null; next(); await sleep(700)
    right(4); await sleep(1100)
    right(2); await sleep(3000)
  },
  finalFrame() { reset(); solved.value = [1, 3, 4, 2]; found.Jo = 2; found.Sam = 2; lost.Jo = 1 }
})
</script>

<style scoped>
.pv-grid { grid-template-columns: repeat(3, 1fr); max-width: 360px; margin: 10px auto 0; }
.pv-crest { position: absolute; inset: 22% 30% 30%; width: 40%; height: 48%; }
.grid-tile.shake { animation: pv-shake 0.45s ease; }
</style>
