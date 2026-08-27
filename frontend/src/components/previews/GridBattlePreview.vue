<template>
  <div class="game-preview" role="img" aria-label="Preview of Grid Battle: a grid of hidden tiles, some solved with a player photo and name, each with a small hint.">
    <span class="game-preview-tag">Preview</span>
    <div class="gb-head">
      <span class="gb-theme">Grid · “Spurs top scorers, 2023/24”</span>
      <span class="gb-lives">
        <span v-for="n in 5" :key="n" class="gb-heart" :class="{ lost: n > 3 }">♥</span>
      </span>
    </div>

    <div class="gb-grid">
      <div v-for="t in tiles" :key="t.id" class="gb-tile" :class="{ solved: t.name }">
        <template v-if="t.name">
          <span class="gb-av">{{ t.initials }}</span>
          <span class="gb-name">{{ t.name }}</span>
          <span class="gb-check">✓</span>
        </template>
        <template v-else>
          <span class="gb-hint">{{ t.hint }}</span>
          <span class="gb-q">?</span>
        </template>
      </div>
    </div>

    <p class="game-preview-caption">
      Take turns naming players that fit. <span class="good">Right</span> flips the tile and scores;
      <span class="bad">wrong</span> costs a life. Highest total across 2–4 grids wins.
    </p>
  </div>
</template>

<script setup>
const tiles = [
  { id: 1, name: 'Son', initials: 'SH', hint: '' },
  { id: 2, hint: 'FW · 14' },
  { id: 3, name: 'Kane', initials: 'HK', hint: '' },
  { id: 4, hint: 'MF · 9' },
  { id: 5, hint: 'FW · 12' },
  { id: 6, name: 'Richarlison', initials: 'RA', hint: '' },
  { id: 7, hint: 'MF · 7' },
  { id: 8, hint: 'DF · 3' },
  { id: 9, hint: 'FW · 6' }
]
</script>

<style scoped>
.gb-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.gb-theme { font-size: 0.8rem; color: var(--text-dim); }
.gb-lives { display: inline-flex; gap: 3px; }
.gb-heart { color: var(--coral); font-size: 0.8rem; }
.gb-heart.lost { color: rgba(255, 77, 109, 0.3); }

.gb-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 96px));
  gap: 6px;
}
.gb-tile {
  position: relative;
  aspect-ratio: 1 / 1;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.03);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  overflow: hidden;
}
.gb-tile.solved {
  border-color: var(--teal);
  background: rgba(61, 220, 151, 0.1);
}
.gb-av {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--violet), var(--teal));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.62rem;
  font-weight: 800;
  color: #06131f;
}
.gb-name { font-size: 0.66rem; font-weight: 700; color: var(--text); }
.gb-check {
  position: absolute;
  top: 4px;
  right: 5px;
  font-size: 0.6rem;
  color: var(--teal);
  font-weight: 800;
}
.gb-hint {
  font-family: var(--font-mono);
  font-size: 0.6rem;
  font-weight: 700;
  color: #241c00;
  background: var(--gold);
  padding: 1px 5px;
  border-radius: 4px;
}
.gb-q { font-size: 1rem; color: var(--text-dim); font-weight: 700; }

@media (max-width: 560px) {
  .gb-grid { grid-template-columns: repeat(3, minmax(0, 84px)); }
  .gb-name { font-size: 0.58rem; }
  .gb-av { width: 26px; height: 26px; font-size: 0.56rem; }
}
</style>
