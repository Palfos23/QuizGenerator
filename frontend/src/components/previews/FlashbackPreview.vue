<template>
  <div class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av Flashback: et mysterieår avslørt gjennom hint, gjettet riktig på det andre.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>

    <div class="fp-clues">
      <div v-for="c in clues" :key="c.num" class="fp-clue" :class="{ win: c.winner }">
        <span class="fp-num">{{ c.num }}</span>
        <div class="fp-body">
          <div class="fp-text">{{ c.text }}</div>
          <div class="fp-chips">
            <span v-for="g in c.guesses" :key="g.name" class="fp-chip" :class="{ win: g.win }">
              {{ g.name }}: {{ g.year }}<b v-if="g.win"> 🎯</b>
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="fp-answer">
      <span class="fp-answer-label">{{ $t('Året var') }}</span>
      <span class="fp-answer-val">1969</span>
      <span class="fp-answer-points">{{ $t('+4 poeng') }}</span>
    </div>

    <p class="game-preview-caption">
      {{ $t('Ingen traff på hint 1, så hint 2 ble vist - gjetter du riktig på et') }} <strong>{{ $t('tidligere') }}</strong> {{ $t('hint, får du flere poeng.') }}
    </p>
  </div>
</template>

<script setup>
import { t as _t } from '../../i18n'
const clues = [
  {
    num: 1,
    text: _t('Et berømt «ett lite skritt» ble tatt dette året'),
    winner: false,
    guesses: [
      { name: 'Jo', year: 1958, win: false },
      { name: 'Sam', year: 1972, win: false }
    ]
  },
  {
    num: 2,
    text: _t('Kappløpet om verdensrommet mellom USA og Sovjetunionen var i full gang'),
    winner: true,
    guesses: [
      { name: 'Jo', year: 1969, win: true }
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
  display: flex;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 2px;
  background: rgba(27, 26, 23, 0.025);
  border: 1px solid var(--border);
}
.fp-clue.win {
  border-color: var(--gold);
  background: rgba(216, 67, 43, 0.08);
}
.fp-num {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(27, 26, 23, 0.06);
  color: var(--text-dim);
  font-size: 0.72rem;
  font-weight: 700;
}
.fp-clue.win .fp-num {
  background: var(--gold);
  color: #fbf7ef;
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
  border-color: rgba(216, 67, 43, 0.5);
  background: rgba(216, 67, 43, 0.12);
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
