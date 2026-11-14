<template>
  <div ref="rootEl" class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av Tension: en skjult rangert liste med ti svar, med et fellesvar rett etter slutten.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>
    <div class="tp-q">{{ $t('«Nevn en av de 10 mest søkte ingrediensene i oppskrifter»') }}</div>

    <div class="tp-track">
      <div class="tp-fill" :style="{ inset: `0 ${fillRight}% 0 0` }"></div>
      <div class="tp-danger" :class="{ on: stage >= 3 }">
        <span>{{ $t('Tension-sone') }}</span>
      </div>

      <div class="tp-pin safe" :class="{ on: stage >= 1 }" style="left: 8%;">
        <span class="tp-dot"></span>
        <span class="tp-label">Salt<b>{{ $t('plass 1 · +1') }}</b></span>
      </div>
      <div class="tp-pin safe hi" :class="{ on: stage >= 2 }" style="left: 60%;">
        <span class="tp-dot"></span>
        <span class="tp-label">{{ $t('Basilikum') }}<b>{{ $t('plass 10 · +10') }}</b></span>
      </div>
      <div class="tp-pin trap" :class="{ on: stage >= 4 }" style="left: 82%;">
        <span class="tp-dot"></span>
        <span class="tp-label">Oregano<b class="bad">{{ $t('felle · −5') }}</b></span>
      </div>
    </div>

    <div class="tp-scale"><span>{{ $t('trygt · lav belønning') }}</span><span>{{ $t('risikabelt · høy belønning') }}</span><span>{{ $t('for langt') }}</span></div>

    <p class="game-preview-caption">
      {{ $t('Gjett så nær') }} <strong>{{ $t('plass 10') }}</strong> {{ $t('som du tør — jo nærmere du går, jo flere poeng. Går du for langt inn i') }} <span class="bad">tension</span> {{ $t('-sonen, koster det deg.') }}
    </p>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { usePreviewLoop } from '../../composables/usePreviewLoop'

// The film: a safe answer lands early, the bar fills toward place 10, the danger zone sweeps in, and the trap drops.
const rootEl = ref(null)
const stage = ref(0)
const fillRight = computed(() => [100, 92, 40, 32, 32][stage.value])

usePreviewLoop(rootEl, {
  async play(sleep) {
    stage.value = 0; await sleep(1000)
    stage.value = 1; await sleep(1000)
    stage.value = 2; await sleep(1700)
    stage.value = 3; await sleep(1000)
    stage.value = 4; await sleep(3400)
    stage.value = 0; await sleep(700)
  },
  finalFrame() { stage.value = 4 }
})
</script>

<style scoped>
.tp-q {
  font-size: 0.82rem;
  color: var(--text);
  font-style: italic;
  margin-bottom: 22px;
}
.tp-track {
  position: relative;
  height: 12px;
  border-radius: 999px;
  background: rgb(var(--fg-rgb) / 0.06);
  /* Bottom margin has to clear the "hi"/"trap" pin labels below, which are
     ~2 lines tall and absolutely positioned starting 20px under the track -
     too little room here and .tp-scale (the safe/risky/overshoot captions,
     right below in normal flow) collides with and garbles into them,
     especially on narrow screens where there's less width for the text to
     dodge each other horizontally. */
  margin: 40px 0 46px;
}
.tp-fill {
  position: absolute;
  inset: 0 32% 0 0;
  transition: inset 1.1s var(--ease-in-out);
  border-radius: 999px;
  background: rgb(var(--teal-rgb) / 0.75);
}
.tp-danger {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 68%;
  right: 0;
  border-radius: 0 999px 999px 0;
  background: repeating-linear-gradient(-45deg, rgb(var(--coral-rgb) / 0.3) 0 6px, rgb(var(--coral-rgb) / 0.12) 6px 12px);
}
.tp-danger { clip-path: inset(-30px 100% -30px 0); transition: clip-path 800ms var(--ease-in-out); }
.tp-danger.on { clip-path: inset(-30px 0 -30px 0); }
.tp-danger span {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.58rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--coral);
  white-space: nowrap;
}
.tp-pin {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  opacity: 0;
  scale: 0.5;
  transition: opacity 300ms var(--ease-in), scale 300ms var(--ease-in);
}
.tp-pin.on { opacity: 1; scale: 1; transition: opacity 320ms var(--ease-out), scale 320ms var(--ease-out); }
.tp-pin.trap.on .tp-dot { animation: pv-shake 520ms 260ms both; }
.tp-dot {
  display: block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 3px solid var(--teal);
  background: var(--ink-raised);
}
.tp-pin.trap .tp-dot { border-color: var(--coral); }
.tp-label {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 18px;
  font-size: 0.66rem;
  font-weight: 700;
  color: var(--text);
  text-align: center;
  white-space: nowrap;
  line-height: 1.35;
}
.tp-label b {
  display: block;
  font-weight: 500;
  font-size: 0.6rem;
  color: var(--text-dim);
}
.tp-label b.bad { color: var(--coral); }
.tp-pin.hi .tp-label,
.tp-pin.trap .tp-label {
  bottom: auto;
  top: 20px;
}
.tp-scale {
  display: flex;
  justify-content: space-between;
  font-size: 0.58rem;
  color: var(--text-dim);
  margin-top: 4px;
}
</style>
