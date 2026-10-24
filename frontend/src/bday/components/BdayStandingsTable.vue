<template>
  <div class="st" :class="{ 'st--large': large }">
    <div v-if="!players.length" class="empty-state">Ingen spillere ennå.</div>
    <template v-for="(p, i) in players" :key="p.hidden ? 'tile-' + i : p.guestId">
    <!-- a face-down place on the final-reveal board -->
    <component
      :is="canTurn(i) ? 'button' : 'div'"
      v-if="p.hidden"
      class="st-row st-tile"
      :class="{ 'is-next': canTurn(i) }"
      :type="canTurn(i) ? 'button' : undefined"
      @click="canTurn(i) && emit('reveal')"
    >
      <span class="st-rank">{{ p.rank }}</span>
      <span class="st-card" aria-hidden="true">?</span>
      <span class="st-tile-label">{{ canTurn(i) ? 'Trykk for å avsløre plass ' + p.rank : 'Plass ' + p.rank + ' - skjult' }}</span>
    </component>
    <div v-else class="st-row" :class="{ 'is-first': p.rank === 1, 'is-flipping': flipped.includes(p.guestId) }">
      <span class="st-rank">{{ p.rank }}</span>
      <div class="st-main">
        <div class="st-name">{{ p.name }}<span v-if="p.teamName" class="st-team">{{ p.teamName }}</span></div>
        <div v-if="showParts" class="st-parts">
          <span :class="{ 'is-none': !p.quizDone }">Quiz {{ p.quizDone ? p.quizScore : '–' }}</span>
          <span :class="{ 'is-none': p.marioKartTimeMs === null }">Mario Kart {{ p.marioKartTimeMs === null ? '–' : p.marioKartPoints }}</span>
          <span :class="{ 'is-none': !p.teamName }">Musikk {{ p.teamName ? p.musicPoints : '–' }}</span>
          <span :class="{ 'is-none': p.bonusPoints === 0 }">Bonus {{ p.bonusPoints > 0 ? '+' + p.bonusPoints : p.bonusPoints }}</span>
        </div>
      </div>
      <span class="st-total">{{ p.total }}</span>
    </div>
    </template>
  </div>
</template>

<script setup>
// The combined birthday-night board: one row per guest, the total big and the four parts it is made of small underneath.
const props = defineProps({
  players: { type: Array, required: true }, // PlayerStanding[]
  large: { type: Boolean, default: false }, // for the big screen
  showParts: { type: Boolean, default: true }, // false while the final reveal is pending - the points per competition stay private
  turnable: { type: Boolean, default: false }, // the host's screen: the next face-down tile can be clicked to turn it
  flipped: { type: Array, default: () => [] } // guest ids that were just turned, for the flip animation
})
const emit = defineEmits(['reveal'])

// tiles turn from the lowest place up, so only the last face-down row is next
function canTurn(i) {
  return props.turnable && props.players[i]?.hidden && !props.players[i + 1]?.hidden
}
</script>

<style scoped>
.st-tile { width: 100%; text-align: left; background: transparent; color: inherit; font: inherit; border-left: none; border-right: none; border-bottom: none; }
button.st-tile { cursor: pointer; }
.st-card {
  width: 46px; height: 46px; display: grid; place-items: center; border-radius: 3px;
  background: var(--text); color: var(--ink-raised);
  font-family: var(--font-display); font-weight: 700; font-size: 1.5rem;
}
.st-tile-label { flex: 1; color: var(--text-dim); font-size: 0.9rem; }
.st-tile.is-next .st-card { box-shadow: 0 0 0 3px var(--gold); animation: st-pulse 1.4s ease-in-out infinite; }
.st-tile.is-next .st-tile-label { color: var(--text); font-weight: 600; }
.st--large .st-card { width: 60px; height: 60px; font-size: 2rem; }
.st--large .st-tile-label { font-size: 1.1rem; }
.st-row.is-flipping { animation: st-flip 0.7s ease-out; transform-origin: center; }
@keyframes st-flip { 0% { transform: rotateX(90deg); opacity: 0; } 100% { transform: rotateX(0); opacity: 1; } }
@keyframes st-pulse { 50% { box-shadow: 0 0 0 6px transparent; } }
@media (prefers-reduced-motion: reduce) { .st-row.is-flipping, .st-tile.is-next .st-card { animation: none; } }
.st-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 0;
  border-top: 1px solid var(--line);
}
.st-row:first-child { border-top: none; }
.st-row.is-first { background: var(--text); color: var(--ink-raised); margin: 0 -12px; padding: 12px; border-top-color: transparent; }
.st-rank { width: 30px; font-family: var(--font-mono); font-size: 0.9rem; color: var(--text-dim); }
.st-row.is-first .st-rank { color: inherit; opacity: 0.75; }
.st-main { flex: 1; min-width: 0; }
.st-name { font-family: var(--font-display); font-weight: 600; font-size: 1.15rem; overflow-wrap: anywhere; }
.st-team {
  margin-left: 8px;
  font-family: var(--font-body);
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  border: 1.5px solid currentColor;
  border-radius: 3px;
  padding: 0 6px;
  vertical-align: 2px;
  opacity: 0.85;
}
.st-parts { display: flex; flex-wrap: wrap; gap: 2px 14px; margin-top: 2px; font-size: 0.78rem; color: var(--text-dim); }
.st-row.is-first .st-parts { color: inherit; opacity: 0.8; }
.st-parts .is-none { opacity: 0.45; }
.st-total { font-family: var(--font-display); font-weight: 700; font-size: 1.7rem; min-width: 52px; text-align: right; }
.st--large .st-name { font-size: 1.6rem; }
.st--large .st-total { font-size: 2.4rem; }
.st--large .st-rank { font-size: 1.2rem; width: 40px; }
.st--large .st-parts { font-size: 0.95rem; }
.st--large .st-row { padding: 14px 0; }
</style>
