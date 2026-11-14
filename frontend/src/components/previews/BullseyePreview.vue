<template>
  <div ref="rootEl" class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av Bullseye: et måltall, der hver spillers gjetning rangeres etter hvor langt unna den er.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>

    <div class="bp-target">
      <span class="bp-rings"></span>
      <span class="bp-val">13</span>
      <span class="bp-cap">{{ $t('mål · Premier League 2024/25') }}</span>
    </div>

    <div class="bp-guesses">
      <div v-for="(g, i) in guesses" :key="g.name" class="bp-guess" :class="[rowState(g), { in: shown > i }]">
        <span class="bp-who">{{ g.name }}</span>
        <span class="bp-pick">{{ g.pick }} <i>· {{ g.stat }}</i></span>
        <span class="bp-off">{{ $t('bom med') }} {{ g.off }}</span>
        <span class="bp-flag">{{ flag(g) }}</span>
      </div>
    </div>

    <p class="game-preview-caption">
      {{ $t('Bytt på å nevne noen nær målet. Hver runde blir den') }} <span class="bad">{{ $t('lengst unna') }}</span> {{ $t('slått ut —') }} <strong>{{ $t('siste spiller igjen vinner.') }}</strong>
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { usePreviewLoop } from '../../composables/usePreviewLoop'
import { t as _t } from '../../i18n'

// The film: three guesses land, each is measured against the target, and the one furthest away is knocked out.
const rootEl = ref(null)
const shown = ref(0)
const judged = ref(false)
const knocked = ref(false)

function rowState(g) {
  if (!judged.value) return 'pending'
  if (g.state === 'out') return knocked.value ? 'out' : 'safe'
  return g.state
}
function flag(g) {
  if (!judged.value) return ''
  if (g.state === 'out' && knocked.value) return _t('ute§BullseyePreviewM')
  return g.state === 'best' ? _t('nærmest') : _t('trygg')
}

usePreviewLoop(rootEl, {
  async play(sleep) {
    shown.value = 0; judged.value = false; knocked.value = false; await sleep(900)
    for (let i = 1; i <= 3; i++) { shown.value = i; await sleep(700) }
    await sleep(500)
    judged.value = true; await sleep(1500)
    knocked.value = true; await sleep(3200)
  },
  finalFrame() { shown.value = 3; judged.value = true; knocked.value = true }
})

const guesses = [
  { name: 'Jo', pick: 'Palmer', stat: '15 g', off: 2, state: 'best' },
  { name: 'Sam', pick: 'Isak', stat: '21 g', off: 8, state: 'safe' },
  { name: 'Alex', pick: 'Haaland', stat: '27 g', off: 14, state: 'out' }
]
</script>

<style scoped>
.bp-target {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 0 16px;
}
.bp-rings {
  position: absolute;
  top: -2px;
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background:
    radial-gradient(circle,
      rgb(var(--coral-rgb) / 0.6) 0 15%,
      transparent 15% 22%,
      rgb(var(--gold-rgb) / 0.4) 22% 42%,
      transparent 42% 48%,
      rgb(var(--fg-rgb) / 0.16) 48% 72%,
      transparent 72%);
}
.bp-val {
  position: relative;
  font-family: var(--font-display);
  font-size: 2.1rem;
  font-weight: 700;
  color: var(--gold);
  line-height: 1;
}
.bp-cap {
  position: relative;
  margin-top: 8px;
  font-size: 0.72rem;
  color: var(--text-dim);
}
.bp-guesses {
  display: flex;
  flex-direction: column;
  gap: 5px;
  max-width: 500px;
  margin: 0 auto;
}
.bp-guess {
  opacity: 0;
  translate: 0 8px;
  transition: opacity 360ms var(--ease-out), translate 360ms var(--ease-out), background 300ms var(--ease-out), border-color 300ms var(--ease-out);
  display: grid;
  grid-template-columns: 44px 1fr auto auto;
  align-items: center;
  gap: 10px;
  padding: 7px 12px;
  border-radius: var(--radius-sm);
  background: rgb(var(--fg-rgb) / 0.025);
  border: 1px solid var(--border);
  font-size: 0.76rem;
}
.bp-guess.in { opacity: 1; translate: 0 0; }
.bp-guess.in.out { opacity: 0.55; }
.bp-off { transition: opacity 300ms var(--ease-out); }
.bp-guess.pending .bp-off { opacity: 0; }
.bp-rings::after { content: ''; position: absolute; inset: 0; border-radius: 50%; border: 2px solid rgb(var(--coral-rgb) / 0.6); animation: pv-ping 2.4s var(--ease-out) infinite; }
@media (prefers-reduced-motion: reduce) { .bp-rings::after { animation: none; opacity: 0; } }
.bp-who { font-weight: 700; color: var(--text); }
.bp-pick { color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bp-pick i { font-style: normal; color: var(--text-dim); font-size: 0.7rem; }
.bp-off { font-size: 0.68rem; color: var(--text-dim); white-space: nowrap; }
.bp-flag {
  font-size: 0.58rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  min-width: 54px;
  text-align: right;
}
.bp-guess.best { border-color: var(--teal); background: rgb(var(--teal-rgb) / 0.09); }
.bp-guess.best .bp-flag { color: var(--teal); }
.bp-guess.safe .bp-flag { color: var(--text-dim); }
.bp-guess.out { border-color: rgb(var(--coral-rgb) / 0.4); }
.bp-guess.out .bp-pick,
.bp-guess.out .bp-who { text-decoration: line-through; }
.bp-guess.out .bp-flag { color: var(--coral); }
</style>
