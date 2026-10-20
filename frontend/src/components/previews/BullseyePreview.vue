<template>
  <div class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av Bullseye: et måltall, der hver spillers gjetning rangeres etter hvor langt unna den er.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>

    <div class="bp-target">
      <span class="bp-rings"></span>
      <span class="bp-val">13</span>
      <span class="bp-cap">{{ $t('mål · Premier League 2024/25') }}</span>
    </div>

    <div class="bp-guesses">
      <div v-for="g in guesses" :key="g.name" class="bp-guess" :class="g.state">
        <span class="bp-who">{{ g.name }}</span>
        <span class="bp-pick">{{ g.pick }} <i>· {{ g.stat }}</i></span>
        <span class="bp-off">{{ $t('bom med') }} {{ g.off }}</span>
        <span class="bp-flag">{{ g.state === 'out' ? $t('ute§BullseyePreviewM') : g.state === 'best' ? $t('nærmest') : $t('trygg') }}</span>
      </div>
    </div>

    <p class="game-preview-caption">
      {{ $t('Bytt på å nevne noen nær målet. Hver runde blir den') }} <span class="bad">{{ $t('lengst unna') }}</span> {{ $t('slått ut —') }} <strong>{{ $t('siste spiller igjen vinner.') }}</strong>
    </p>
  </div>
</template>

<script setup>
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
      rgba(179, 38, 30, 0.6) 0 15%,
      transparent 15% 22%,
      rgba(216, 67, 43, 0.4) 22% 42%,
      transparent 42% 48%,
      rgba(27, 26, 23, 0.16) 48% 72%,
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
  display: grid;
  grid-template-columns: 44px 1fr auto auto;
  align-items: center;
  gap: 10px;
  padding: 7px 12px;
  border-radius: 2px;
  background: rgba(27, 26, 23, 0.025);
  border: 1px solid var(--border);
  font-size: 0.76rem;
}
.bp-who { font-weight: 700; color: var(--text); }
.bp-pick { color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bp-pick i { font-style: normal; color: var(--text-dim); font-size: 0.7rem; }
.bp-off { font-family: var(--font-mono); font-size: 0.68rem; color: var(--text-dim); white-space: nowrap; }
.bp-flag {
  font-size: 0.58rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  min-width: 54px;
  text-align: right;
}
.bp-guess.best { border-color: var(--teal); background: rgba(31, 122, 80, 0.09); }
.bp-guess.best .bp-flag { color: var(--teal); }
.bp-guess.safe .bp-flag { color: var(--text-dim); }
.bp-guess.out { opacity: 0.55; border-color: rgba(179, 38, 30, 0.4); }
.bp-guess.out .bp-pick,
.bp-guess.out .bp-who { text-decoration: line-through; }
.bp-guess.out .bp-flag { color: var(--coral); }
</style>
