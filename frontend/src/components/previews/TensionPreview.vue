<template>
  <div ref="rootEl" class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av Tension: tre svar på en topp 10-liste avsløres, et svar utenfor listen koster poeng, og stillingen oppdateres.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>
    <div class="pv-title">{{ $t('Topp 10 mest søkte ingredienser i oppskrifter') }}</div>

    <div v-if="!showStandings" class="pv-rows">
      <div v-for="(a, i) in ANSWERS" :key="a.player" class="pv-row" :class="[a.kind, { in: shown > i }]">
        <span class="pv-row-rank">{{ a.rank || '–' }}</span>
        <span class="pv-row-main"><strong>{{ a.player }}</strong><span>{{ a.answer }}</span></span>
        <span class="pv-chip" :class="a.kind">{{ a.label }}</span>
      </div>
    </div>

    <div v-else class="pv-standings">
      <div class="tension-standings-head" aria-hidden="true">
        <span>{{ $t('Spiller') }}</span><span>{{ $t('Runden') }}</span><span>{{ $t('Totalt') }}</span>
      </div>
      <div class="tension-standings-list">
        <div v-for="(s, i) in STANDINGS" :key="s.name" class="tension-standing-row" :class="{ leader: i === 0 }">
          <span class="tension-standing-rank">{{ i + 1 }}</span>
          <span class="tension-standing-name">{{ s.name }}<span v-if="s.up" class="tension-standing-arrow up">▲{{ s.up }}</span></span>
          <span class="tension-standing-round"><span class="tension-round-chip" :class="s.round > 0 ? 'plus' : 'minus'">{{ s.round > 0 ? '+' : '' }}{{ s.round }}</span></span>
          <span class="tension-standing-total" :class="{ negative: s.total < 0 }">{{ s.total }}</span>
        </div>
      </div>
    </div>

    <p class="game-preview-caption">
      {{ $t('Gjett så nær') }} <strong>{{ $t('plass 10') }}</strong> {{ $t('som du tør – jo lenger ned, jo flere poeng. Et svar utenfor listen er') }} <span class="bad">tension</span> {{ $t('og koster deg.') }}
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { usePreviewLoop } from '../../composables/usePreviewLoop'
import { t as _t } from '../../i18n'

// The film: three answers are revealed (a safe one, a brave one near place 10, and one past the
// list), then the standings after the round, in the real standings' style.
const rootEl = ref(null)
const shown = ref(0)
const showStandings = ref(false)

const ANSWERS = [
  { player: 'Jo', answer: 'Salt', rank: 1, kind: 'safe', label: _t('plass 1 · +1') },
  { player: 'Sam', answer: _t('Basilikum'), rank: 9, kind: 'hit', label: _t('plass 9 · +9') },
  { player: 'Alex', answer: 'Oregano', rank: null, kind: 'out', label: _t('tension · −5') }
]
const STANDINGS = [
  { name: 'Sam', round: 9, total: 14, up: 1 },
  { name: 'Jo', round: 1, total: 11 },
  { name: 'Alex', round: -5, total: 2 }
]

usePreviewLoop(rootEl, {
  async play(sleep) {
    shown.value = 0; showStandings.value = false; await sleep(900)
    for (let i = 1; i <= ANSWERS.length; i++) { shown.value = i; await sleep(1200) }
    await sleep(900)
    showStandings.value = true; await sleep(3400)
  },
  finalFrame() { shown.value = ANSWERS.length; showStandings.value = true }
})
</script>
