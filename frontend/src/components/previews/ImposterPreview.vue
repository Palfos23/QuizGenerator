<template>
  <div class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av et Imposter-brett: et rutenett der et par ruter er avslørt som imposters.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>
    <div class="ip-theme">{{ $t('Tema · «Toppscorere i Premier League, 2023/24»') }}</div>
    <div class="ip-grid">
      <div v-for="t in tiles" :key="t.name" class="ip-tile" :class="t.state">
        <span class="ip-name">{{ t.name }}</span>
        <span v-if="t.state === 'fit'" class="ip-badge good">{{ $t('✓ passer') }}</span>
        <span v-else-if="t.state === 'imposter'" class="ip-badge bad">✕ imposter</span>
      </div>
    </div>
    <p class="game-preview-caption">
      {{ $t('Snu ruter sammen — de fleste hører til temaet, noen få gjør det ikke.') }}
      <strong>{{ $t('Færrest') }} <span class="bad">imposter</span> {{ $t('-treff vinner.') }}</strong>
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
  border-radius: 2px;
  border: 1px solid var(--border);
  background: rgb(var(--fg-rgb) / 0.07);
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
  background: rgb(var(--teal-rgb) / 0.12);
}
.ip-tile.imposter {
  border-color: var(--coral);
  background: rgb(var(--coral-rgb) / 0.14);
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
