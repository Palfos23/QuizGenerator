<template>
  <div ref="rootEl" class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av Bullseye: tre gjetninger plasseres på en tallinje rundt måltallet, og den lengst unna er ute.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>
    <div class="pv-title">{{ $t('1837 – året jeg ble president. Hvem er jeg?') }}</div>

    <BullseyeReveal
      :target="1837"
      :results="RESULTS"
      :reveal-count="shown"
      :truth="TRUTH"
      :show-truth="shown >= RESULTS.length"
    />

    <p class="game-preview-caption">
      {{ $t('Alle nevner noen nær måltallet.') }} <span class="bad">{{ $t('Lengst unna er ute') }}</span> – <strong>{{ $t('siste spiller igjen vinner.') }}</strong>
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { usePreviewLoop } from '../../composables/usePreviewLoop'
import BullseyeReveal from '../BullseyeReveal.vue'

// The film, on the real reveal: three guesses land on the number line one by one (closest first),
// the furthest is knocked out, and the answer key opens.
const rootEl = ref(null)
const shown = ref(0)

const RESULTS = [
  { key: 'Jo', player: 'Jo', guess: 'Andrew Jackson', statValue: 1829, distance: 8, eliminated: false },
  { key: 'Sam', player: 'Sam', guess: 'James Buchanan', statValue: 1857, distance: 20, eliminated: false },
  { key: 'Alex', player: 'Alex', guess: 'Abraham Lincoln', statValue: 1861, distance: 24, eliminated: true }
]
const TRUTH = [{ key: 'mvb', name: 'Martin Van Buren', statValue: 1837, distance: 0, foundBy: null }]

usePreviewLoop(rootEl, {
  async play(sleep) {
    shown.value = 0; await sleep(1000)
    for (let i = 1; i <= RESULTS.length; i++) { shown.value = i; await sleep(1300) }
    await sleep(3000)
  },
  finalFrame() { shown.value = RESULTS.length }
})
</script>
