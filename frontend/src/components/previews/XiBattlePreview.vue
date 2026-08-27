<template>
  <div class="game-preview" role="img" aria-label="Preview of XI Battle: a football pitch with a starting lineup, some shirts revealed and some still hidden.">
    <span class="game-preview-tag">Preview</span>
    <div class="xp-head">
      <span class="xp-match">Arsenal XI · 2014 Community Shield</span>
      <span class="xp-lives">
        <span v-for="n in 5" :key="n" class="xp-heart" :class="{ lost: n > 4 }">♥</span>
      </span>
    </div>

    <div class="xp-pitch">
      <div v-for="(row, ri) in rows" :key="ri" class="xp-row">
        <div
          v-for="(slot, si) in row"
          :key="si"
          class="xp-shirt"
          :class="{ found: slot.name, gk: slot.gk }"
        >
          <span class="xp-num">{{ slot.name ? '' : slot.num }}</span>
          <span v-if="slot.name" class="xp-tick">✓</span>
          <span v-if="slot.name" class="xp-pname">{{ slot.name }}</span>
        </div>
      </div>
    </div>

    <p class="game-preview-caption">
      Guess who started a real match. <span class="good">Right</span> reveals the shirt and scores;
      <span class="bad">wrong</span> costs a life. Highest total across 2–4 boards wins.
    </p>
  </div>
</template>

<script setup>
// Top-to-bottom as displayed: attack at the top, keeper at the back.
const rows = [
  [{ num: 7, name: 'Sánchez' }, { num: 11 }, { num: 14, name: 'Walcott' }],
  [{ num: 16 }, { num: 8, name: 'Arteta' }, { num: 35 }],
  [{ num: 3 }, { num: 6, name: 'Koscielny' }, { num: 5 }, { num: 24 }],
  [{ num: 1, name: 'Szczęsny', gk: true }]
]
</script>

<style scoped>
.xp-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.xp-match { font-size: 0.8rem; color: var(--text-dim); }
.xp-lives { display: inline-flex; gap: 3px; }
.xp-heart { color: var(--coral); font-size: 0.8rem; }
.xp-heart.lost { color: rgba(255, 77, 109, 0.3); }

.xp-pitch {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px 8px;
  border-radius: var(--radius-md);
  background:
    repeating-linear-gradient(0deg, rgba(255,255,255,0.03) 0 22px, transparent 22px 44px),
    linear-gradient(180deg, #123f22, #0e3319);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.xp-row {
  display: flex;
  justify-content: space-evenly;
}
.xp-shirt {
  position: relative;
  width: 30px;
  height: 32px;
  border-radius: 7px 7px 5px 5px;
  background: rgba(15, 23, 20, 0.5);
  border: 2px solid rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.68rem;
  font-weight: 800;
  color: var(--text);
}
.xp-shirt.gk { border-color: var(--gold); color: var(--gold); }
.xp-shirt.found {
  background: var(--teal);
  border-color: var(--teal);
  color: #05261a;
}
.xp-tick { font-size: 0.7rem; }
.xp-pname {
  position: absolute;
  left: 50%;
  bottom: -13px;
  transform: translateX(-50%);
  font-size: 0.56rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  white-space: nowrap;
}
.xp-row:first-child .xp-pname { bottom: auto; top: -13px; }
</style>
