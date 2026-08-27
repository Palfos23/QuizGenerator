<template>
  <div class="game-preview" role="img" aria-label="Preview of Bullseye: a target number, with each player's guess ranked by how far off it is.">
    <span class="game-preview-tag">Preview</span>

    <div class="bp-target">
      <span class="bp-rings"></span>
      <span class="bp-val">13</span>
      <span class="bp-cap">goals · Premier League 2024/25</span>
    </div>

    <div class="bp-guesses">
      <div v-for="g in guesses" :key="g.name" class="bp-guess" :class="g.state">
        <span class="bp-who">{{ g.name }}</span>
        <span class="bp-pick">{{ g.pick }} <i>· {{ g.stat }}</i></span>
        <span class="bp-off">off by {{ g.off }}</span>
        <span class="bp-flag">{{ g.state === 'out' ? 'eliminated' : g.state === 'best' ? 'closest' : 'safe' }}</span>
      </div>
    </div>

    <p class="game-preview-caption">
      Take turns naming someone close to the target. Each round the <span class="bad">farthest off</span> is
      knocked out — <strong>last player standing wins.</strong>
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
  padding: 10px 0 18px;
}
.bp-rings {
  position: absolute;
  top: -4px;
  width: 92px;
  height: 92px;
  border-radius: 50%;
  background:
    radial-gradient(circle,
      rgba(255, 77, 109, 0.6) 0 14%,
      transparent 14% 20%,
      rgba(242, 183, 5, 0.4) 20% 38%,
      transparent 38% 44%,
      rgba(255, 255, 255, 0.16) 44% 64%,
      transparent 64%);
}
.bp-val {
  position: relative;
  font-family: var(--font-display);
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--gold);
  line-height: 1;
}
.bp-cap {
  position: relative;
  margin-top: 6px;
  font-size: 0.72rem;
  color: var(--text-dim);
}
.bp-guesses {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.bp-guess {
  display: grid;
  grid-template-columns: 44px 1fr auto auto;
  align-items: center;
  gap: 10px;
  padding: 7px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
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
.bp-guess.best { border-color: var(--teal); background: rgba(61, 220, 151, 0.09); }
.bp-guess.best .bp-flag { color: var(--teal); }
.bp-guess.safe .bp-flag { color: var(--text-dim); }
.bp-guess.out { opacity: 0.55; border-color: rgba(255, 77, 109, 0.4); }
.bp-guess.out .bp-pick,
.bp-guess.out .bp-who { text-decoration: line-through; }
.bp-guess.out .bp-flag { color: var(--coral); }
</style>
