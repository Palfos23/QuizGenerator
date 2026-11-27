<template>
  <div ref="rootEl" class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av Flashback: et mysterieår avslørt gjennom hint, gjettet riktig på det andre.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>
    <div class="pv-title">{{ $t('Hvilket år er det?') }}</div>

    <div class="fp-clues">
      <div v-for="c in clues" :key="c.num" class="fp-clue" :class="{ win: c.winner && step >= 5, in: step >= c.at }">
        <span class="fp-num">{{ c.num }}</span>
        <div class="fp-body">
          <div class="fp-text">{{ c.text }}</div>
          <div class="fp-chips">
            <span v-for="g in c.guesses" :key="g.name" class="fp-chip" :class="{ win: g.win, in: step >= g.at }">
              {{ g.name }}: {{ g.year }}<b v-if="g.win"> 🎯</b>
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="fp-answer" :class="{ in: step >= 6 }">
      <span class="fp-answer-label">{{ $t('Året var') }}</span>
      <span class="fp-answer-val">{{ year }}</span>
      <span class="fp-answer-points" :class="{ in: step >= 7 }">{{ $t('+4 poeng') }}</span>
    </div>

    <p class="game-preview-caption">
      {{ $t('Ingen traff på hint 1, så hint 2 ble vist - gjetter du riktig på et') }} <strong>{{ $t('tidligere') }}</strong> {{ $t('hint, får du flere poeng.') }}
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { t as _t } from '../../i18n'
import { tweenRef, usePreviewLoop } from '../../composables/usePreviewLoop'

// The film: clue 1 brings two wrong guesses, clue 2 arrives, one guess lands on the year, and the answer counts up to it.
const rootEl = ref(null)
const step = ref(0)
const year = ref(1900)

usePreviewLoop(rootEl, {
  async play(sleep) {
    step.value = 0; year.value = 1900; await sleep(900)
    step.value = 1; await sleep(900)
    step.value = 2; await sleep(650)
    step.value = 3; await sleep(1200)
    step.value = 4; await sleep(1100)
    step.value = 5; await sleep(900)
    step.value = 6; await tweenRef(year, 1969, 1100)
    step.value = 7; await sleep(3200)
  },
  finalFrame() { step.value = 7; year.value = 1969 }
})

const clues = [
  {
    num: 1,
    at: 1,
    text: _t('Et berømt «ett lite skritt» ble tatt dette året'),
    winner: false,
    guesses: [
      { name: 'Jo', year: 1958, win: false, at: 2 },
      { name: 'Sam', year: 1972, win: false, at: 3 }
    ]
  },
  {
    num: 2,
    at: 4,
    text: _t('Kappløpet om verdensrommet mellom USA og Sovjetunionen var i full gang'),
    winner: true,
    guesses: [
      { name: 'Jo', year: 1969, win: true, at: 5 }
    ]
  }
]
</script>

<style scoped>
.fp-clues {
  display: flex;
  flex-direction: column;
  gap: 7px;
  max-width: 520px;
  margin: 0 auto;
}
.fp-clue {
  opacity: 0;
  translate: 0 10px;
  transition: opacity 420ms var(--ease-out), translate 420ms var(--ease-out), border-color 300ms var(--ease-out), background 300ms var(--ease-out);
  display: flex;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  background: rgb(var(--fg-rgb) / 0.025);
  border: 1px solid var(--border);
}
.fp-clue.in { opacity: 1; translate: 0 0; }
.fp-chip { opacity: 0; scale: 0.85; transition: opacity 280ms var(--ease-out), scale 280ms var(--ease-out), background 250ms var(--ease-out); }
.fp-chip.in { opacity: 1; scale: 1; }
.fp-answer { opacity: 0; translate: 0 8px; transition: opacity 360ms var(--ease-out), translate 360ms var(--ease-out); }
.fp-answer.in { opacity: 1; translate: 0 0; }
.fp-answer-val { font-variant-numeric: tabular-nums; }
.fp-answer-points { opacity: 0; scale: 0.8; transition: opacity 260ms var(--ease-out), scale 260ms var(--ease-out); }
.fp-answer-points.in { opacity: 1; scale: 1; }
.fp-clue.win {
  border-color: var(--gold);
  background: rgb(var(--gold-rgb) / 0.08);
}
.fp-num {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgb(var(--fg-rgb) / 0.06);
  color: var(--text-dim);
  font-size: 0.72rem;
  font-weight: 700;
}
.fp-clue.win .fp-num {
  background: var(--gold);
  color: var(--ink-raised);
}
.fp-body {
  min-width: 0;
  flex: 1;
}
.fp-text {
  font-size: 0.78rem;
  font-style: italic;
  color: var(--text);
  margin-bottom: 6px;
}
.fp-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.fp-chip {
  font-size: 0.68rem;
  font-weight: 600;
  color: var(--text-dim);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 2px 9px;
}
.fp-chip.win {
  color: var(--gold);
  border-color: rgb(var(--gold-rgb) / 0.5);
  background: rgb(var(--gold-rgb) / 0.12);
}
.fp-answer {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}
.fp-answer-label {
  font-size: 0.72rem;
  color: var(--text-dim);
}
.fp-answer-val {
  font-family: var(--font-display);
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--gold);
}
.fp-answer-points {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--teal);
}
</style>
