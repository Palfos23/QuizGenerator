<template>
  <div ref="rootEl" class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av 501: to spillere teller ned fra 501 og prøver å lande mellom 0 og −10.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>
    <div class="pv-title">{{ $t('Premier League-mål, hele karrieren') }}</div>

    <div class="fiveoo-players">
      <FiveOhOnePlayerCard
        v-for="p in PLAYERS"
        :key="p"
        :name="p"
        :total="totals[p]"
        :active="turn === p && !done"
        :turn-label="$t('Tur')"
        :in-window="done && p === 'Jo'"
        :throws="throws.filter(t => t.player === p)"
      />
    </div>

    <FiveOhOneThrowList :throws="throws" :show-history="false" />

    <p class="game-preview-caption">
      {{ $t('Nevn en spiller og trekk fra tallet. Land mellom') }} <strong>{{ $t('0 og −10') }}</strong> {{ $t('for å vinne – over 180 gir') }} <span class="bad">0</span>.
    </p>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { usePreviewLoop } from '../../composables/usePreviewLoop'
import FiveOhOnePlayerCard from '../FiveOhOnePlayerCard.vue'
import FiveOhOneThrowList from '../FiveOhOneThrowList.vue'

// The film, on the real 501 cards: Jo and Sam take turns, Sam's Kane (213, over 180) scores
// nothing, and Jo checks out on Lampard: 501 − 175 − 150 − 177 = −1.
const rootEl = ref(null)
const PLAYERS = ['Jo', 'Sam']
const FILM = [
  { player: 'Jo', name: 'Thierry Henry', rawValue: 175, score: 175 },
  { player: 'Sam', name: 'Harry Kane', rawValue: 213, score: 0 },
  { player: 'Jo', name: 'Michael Owen', rawValue: 150, score: 150 },
  { player: 'Sam', name: 'Peter Crouch', rawValue: 108, score: 108 },
  { player: 'Jo', name: 'Frank Lampard', rawValue: 177, score: 177 }
]

const totals = reactive({ Jo: 501, Sam: 501 })
const throws = ref([])
const turn = ref('Jo')
const done = ref(false)

function reset() {
  totals.Jo = 501; totals.Sam = 501; throws.value = []; turn.value = 'Jo'; done.value = false
}
function apply(t) {
  totals[t.player] -= t.score
  throws.value = [...throws.value, { ...t, bust: false, resultingTotal: totals[t.player] }]
  turn.value = t.player === 'Jo' ? 'Sam' : 'Jo'
}

usePreviewLoop(rootEl, {
  async play(sleep) {
    reset(); await sleep(900)
    for (const t of FILM) { apply(t); await sleep(1200) }
    done.value = true
    await sleep(3000)
  },
  finalFrame() { reset(); FILM.forEach(apply); done.value = true }
})
</script>
