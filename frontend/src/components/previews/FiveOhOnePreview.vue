<template>
  <div ref="rootEl" class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av 501: en poengsum som teller ned fra 501 mot et utsjekkvindu.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>
    <div class="fp-cat">{{ $t('Kategori · «Premier League-kamper»') }}</div>

    <div class="fp-body">
      <div class="fp-board">
        <div class="fp-ring">
          <div class="fp-score">
            <span class="fp-num" :class="{ flash: busted }">{{ score }}</span>
            <span class="fp-lbl">{{ $t('for å sjekke ut') }}</span>
          </div>
        </div>
      </div>

      <div class="fp-throws">
        <div class="fp-throw" :class="{ in: shown >= 1 }">
          <span class="fp-p">Start</span>
          <span class="fp-d"></span>
          <span class="fp-t">501</span>
        </div>
        <div class="fp-throw" :class="{ in: shown >= 2 }">
          <span class="fp-p">M. Salah <i>234</i></span>
          <span class="fp-d">−234</span>
          <span class="fp-t">267</span>
        </div>
        <div class="fp-throw" :class="{ in: shown >= 3 }">
          <span class="fp-p">H. Kane <i>89</i></span>
          <span class="fp-d">−89</span>
          <span class="fp-t">178</span>
        </div>
        <div class="fp-throw" :class="{ in: shown >= 4, bust: shown >= 4 }">
          <span class="fp-p">P. Foden <i>201</i></span>
          <span class="fp-d">over 180</span>
          <span class="fp-t">bust</span>
        </div>
        <div class="fp-throw now" :class="{ in: shown >= 5 }">
          <span class="fp-p">{{ $t('din kasting…') }}</span>
          <span class="fp-d">{{ resolved ? '−170' : '' }}</span>
          <span class="fp-t"><span v-if="!resolved" class="fp-dots">···</span><template v-else>8</template></span>
        </div>
      </div>
    </div>

    <p class="game-preview-caption">
      {{ $t('Start på 501, trekk fra tallet til hvert valg, og land mellom') }} <strong>{{ $t('0 og −10') }}</strong> {{ $t('for å vinne. Går du utenfor vinduet eller bryter dartreglene,') }} <span class="bad">{{ $t('teller ikke kastet') }}</span>.
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { tweenRef, usePreviewLoop } from '../../composables/usePreviewLoop'

// The film: the score counts down from 501 throw by throw, one throw busts, and the last one checks out on 8.
const rootEl = ref(null)
const shown = ref(0)
const score = ref(501)
const busted = ref(false)
const resolved = ref(false)

usePreviewLoop(rootEl, {
  async play(sleep) {
    shown.value = 0; score.value = 501; busted.value = false; resolved.value = false; await sleep(900)
    shown.value = 1; await sleep(700)
    shown.value = 2; await tweenRef(score, 267, 800); await sleep(500)
    shown.value = 3; await tweenRef(score, 178, 700); await sleep(500)
    shown.value = 4; busted.value = true; await sleep(1300)
    busted.value = false
    shown.value = 5; await sleep(1100)
    resolved.value = true; await tweenRef(score, 8, 1100); await sleep(3000)
  },
  finalFrame() { shown.value = 5; score.value = 8; resolved.value = true }
})
</script>

<style scoped>
.fp-cat {
  font-size: 0.8rem;
  color: var(--text-dim);
  margin-bottom: 16px;
}
.fp-body {
  display: flex;
  gap: 22px;
  align-items: center;
  justify-content: center;
}
.fp-board {
  flex-shrink: 0;
}
.fp-ring {
  width: 104px;
  height: 104px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle, transparent 34%, rgb(var(--gold-rgb) / 0.14) 35% 44%, transparent 45%),
    conic-gradient(from 0deg, rgb(var(--fg-rgb) / 0.05) 0 25%, rgb(var(--fg-rgb) / 0.06) 25% 50%, rgb(var(--fg-rgb) / 0.05) 50% 75%, rgb(var(--fg-rgb) / 0.06) 75% 100%);
  border: 1px solid var(--border);
}
.fp-score {
  width: 62px;
  height: 62px;
  border-radius: 50%;
  background: var(--gold-soft);
  border: 1px solid rgb(var(--gold-rgb) / 0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.fp-num {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--gold);
  line-height: 1;
}
.fp-lbl {
  font-size: 0.5rem;
  color: var(--text-dim);
  margin-top: 2px;
}
.fp-throws {
  flex: 0 1 380px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.fp-throw {
  opacity: 0;
  translate: 0 8px;
  transition: opacity 320ms var(--ease-out), translate 320ms var(--ease-out), background 250ms var(--ease-out);
  display: grid;
  grid-template-columns: 1fr auto 46px;
  align-items: center;
  gap: 10px;
  padding: 5px 10px;
  border-radius: var(--radius-sm);
  background: rgb(var(--fg-rgb) / 0.06);
  font-size: 0.74rem;
}
.fp-p { color: var(--text-dim); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fp-p i { font-style: normal; color: var(--text); font-size: 0.68rem; }
.fp-d { font-size: 0.66rem; color: var(--text-dim); }
.fp-t { font-weight: 700; color: var(--text); text-align: right; }
.fp-throw.in { opacity: 1; translate: 0 0; }
.fp-throw.bust.in { animation: pv-shake 520ms 120ms both; }
.fp-num { transition: color 200ms var(--ease-out); font-variant-numeric: tabular-nums; }
.fp-num.flash { color: var(--coral); }
.fp-dots { letter-spacing: 0.1em; animation: dqp-dots 1s steps(1) infinite; }
@keyframes dqp-dots { 50% { opacity: 0.25; } }
.fp-throw.bust { background: rgb(var(--coral-rgb) / 0.1); }
.fp-throw.bust .fp-d, .fp-throw.bust .fp-t { color: var(--coral); }
.fp-throw.now { background: var(--gold-soft); }
.fp-throw.now .fp-t { color: var(--gold); }

@media (max-width: 560px) {
  .fp-body { flex-direction: column; gap: 16px; }
  /* .fp-throws' flex-basis (380px, meant to cap its WIDTH in the row layout
     above) becomes a HEIGHT once flex-direction flips to column - without
     resetting it to auto here, the box claims 380px of vertical space it
     doesn't need, leaving a dead gap before the caption below it. */
  .fp-throws { width: 100%; flex-basis: auto; }
}
</style>
