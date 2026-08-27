<template>
  <div class="game-preview" role="img" aria-label="Preview of an Imposter board: a grid of tiles where a couple are revealed as imposters.">
    <span class="game-preview-tag">Preview</span>
    <div class="ip-theme">Theme · “Premier League top scorers, 2023/24”</div>
    <div class="ip-grid">
      <div v-for="t in tiles" :key="t.name" class="ip-tile" :class="t.state">
        <span class="ip-name">{{ t.name }}</span>
        <span v-if="t.state === 'fit'" class="ip-badge good">✓ fits</span>
        <span v-else-if="t.state === 'imposter'" class="ip-badge bad">✕ imposter</span>
      </div>
    </div>
    <p class="game-preview-caption">
      Flip tiles as a group — most belong to the theme, a few don’t.
      <strong>Fewest <span class="bad">imposter</span> hits wins.</strong>
    </p>
  </div>
</template>

<script setup>
const tiles = [
  { name: 'Haaland', state: 'plain' },
  { name: 'Watkins', state: 'fit' },
  { name: 'Isak', state: 'plain' },
  { name: 'Saka', state: 'plain' },
  { name: 'Foden', state: 'imposter' },
  { name: 'Palmer', state: 'plain' },
  { name: 'Solanke', state: 'plain' },
  { name: 'Wilson', state: 'imposter' }
]
</script>

<style scoped>
.ip-theme {
  font-size: 0.78rem;
  color: var(--text-dim);
  margin-bottom: 12px;
}
.ip-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 7px;
}
.ip-tile {
  position: relative;
  height: 58px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.03);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
}
.ip-name {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--text-dim);
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}
.ip-tile.fit {
  border-color: var(--teal);
  background: rgba(61, 220, 151, 0.12);
}
.ip-tile.imposter {
  border-color: var(--coral);
  background: rgba(255, 77, 109, 0.14);
}
.ip-tile.fit .ip-name,
.ip-tile.imposter .ip-name {
  color: var(--text);
  transform: translateY(-6px);
}
.ip-badge {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 6px;
  text-align: center;
  font-size: 0.54rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}
.ip-badge.good { color: var(--teal); }
.ip-badge.bad { color: var(--coral); }

@media (max-width: 520px) {
  .ip-grid { grid-template-columns: repeat(4, 1fr); }
  .ip-name { font-size: 0.62rem; }
  .ip-tile { height: 50px; }
}
</style>
