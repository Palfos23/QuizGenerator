<template>
  <div class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av XI Battle: en fotballbane med en startellever, noen drakter avslørt og noen fortsatt skjult.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>
    <div class="xp-head">
      <span class="xp-match">{{ $t('Arsenal XI · Community Shield 2014') }}</span>
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
      {{ $t('Gjett hvem som startet en ekte kamp.') }} <span class="good">{{ $t('Riktig§XiBattlePreviewT') }}</span> {{ $t('avslører drakten og gir poeng;') }}
      <span class="bad">{{ $t('feil') }}</span> {{ $t('koster et liv. Høyest totalsum på 2–4 brett vinner.') }}
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
  max-width: 440px;
  margin: 0 auto 12px;
}
.xp-match { font-size: 0.8rem; color: var(--text-dim); }
.xp-lives { display: inline-flex; gap: 3px; }
.xp-heart { color: var(--coral); font-size: 0.8rem; }
.xp-heart.lost { color: rgba(179, 38, 30, 0.3); }

.xp-pitch {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 440px;
  margin: 0 auto;
  padding: 16px 12px;
  border-radius: 2px;
  background:
    repeating-linear-gradient(0deg, rgba(27, 26, 23, 0.07) 0 22px, transparent 22px 44px),
    linear-gradient(180deg, #123f22, #0e3319);
  border: 1px solid rgba(27, 26, 23, 0.08);
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
  background: rgba(27, 26, 23, 0.5);
  border: 2px solid rgba(27, 26, 23, 0.25);
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
  color: #fbf7ef;
}
.xp-tick { font-size: 0.7rem; }
.xp-pname {
  position: absolute;
  left: 50%;
  bottom: -13px;
  transform: translateX(-50%);
  font-size: 0.56rem;
  font-weight: 700;
  color: rgba(27, 26, 23, 0.9);
  white-space: nowrap;
}
.xp-row:first-child .xp-pname { bottom: auto; top: -13px; }
</style>
