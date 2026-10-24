<template>
  <div class="st" :class="{ 'st--large': large, 'st--plain': !showParts }">
    <div v-if="!players.length" class="empty-state">Ingen spillere ennå.</div>

    <div v-if="showParts && players.some(p => !p.hidden)" class="st-head" aria-hidden="true">
      <span class="st-h-rank">#</span>
      <span class="st-h-player">Spiller</span>
      <span v-for="c in COLUMNS" :key="c.key" class="st-h-cell">{{ c.label }}</span>
      <span class="st-h-total">Totalt</span>
    </div>

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
        <span class="st-rank"><span class="st-badge">{{ p.rank }}</span></span>
        <span class="st-card" aria-hidden="true">?</span>
        <span class="st-tile-label">{{ canTurn(i) ? 'Trykk for å avsløre plass ' + p.rank : 'Plass ' + p.rank + ' - skjult' }}</span>
      </component>

      <div v-else class="st-row" :class="{ 'is-top': p.rank <= 3, 'is-first': p.rank === 1, 'is-flipping': flipped.includes(p.guestId) }">
        <span class="st-rank"><span class="st-badge" :class="'st-badge--' + Math.min(p.rank, 4)">{{ p.rank }}</span></span>

        <div class="st-player">
          <div class="st-name">{{ p.name }}<span v-if="p.teamName" class="st-team">{{ p.teamName }}</span></div>
          <div class="st-bar" aria-hidden="true"><span :style="{ width: barWidth(p) + '%' }"></span></div>
        </div>

        <div v-if="showParts" class="st-parts">
          <span class="st-cell" :class="{ 'is-none': !p.quizDone }" data-label="Quiz">{{ p.quizDone ? p.quizScore : '–' }}</span>
          <span class="st-cell" :class="{ 'is-none': p.marioKartTimeMs === null }" data-label="Kart">{{ p.marioKartTimeMs === null ? '–' : p.marioKartPoints }}</span>
          <span class="st-cell" :class="{ 'is-none': !p.teamName }" data-label="Musikk">{{ p.teamName ? p.musicPoints : '–' }}</span>
          <span class="st-cell" :class="{ 'is-none': p.bonusPoints === 0, 'is-plus': p.bonusPoints > 0, 'is-minus': p.bonusPoints < 0 }" data-label="Bonus">{{ p.bonusPoints === 0 ? '–' : p.bonusPoints > 0 ? '+' + p.bonusPoints : p.bonusPoints }}</span>
        </div>

        <span class="st-total">{{ p.total }}</span>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// The combined birthday-night board, laid out as a table: place, player, one column per competition, and the total.
// On a phone the four competition columns drop under the name, each with its label.
const props = defineProps({
  players: { type: Array, required: true }, // PlayerStanding[]
  large: { type: Boolean, default: false }, // for the big screen
  showParts: { type: Boolean, default: true }, // false while the final reveal is pending - the points per competition stay private
  turnable: { type: Boolean, default: false }, // the host's screen: the next face-down tile can be clicked to turn it
  flipped: { type: Array, default: () => [] } // guest ids that were just turned, for the flip animation
})
const emit = defineEmits(['reveal'])

const COLUMNS = [
  { key: 'quiz', label: 'Quiz' },
  { key: 'mk', label: 'Mario Kart' },
  { key: 'music', label: 'Musikk' },
  { key: 'bonus', label: 'Bonus' }
]

// the thin bar under each name is the total as a share of the leader's
const maxTotal = computed(() => Math.max(1, ...props.players.filter(p => !p.hidden).map(p => p.total)))
function barWidth(p) {
  return Math.max(0, Math.min(100, (p.total / maxTotal.value) * 100))
}

// tiles turn from the lowest place up, so only the last face-down row is next
function canTurn(i) {
  return props.turnable && props.players[i]?.hidden && !props.players[i + 1]?.hidden
}
</script>

<style scoped>
.st {
  --cols: 40px minmax(0, 1fr) 52px 74px 62px 56px 76px;
  border-top: 2px solid var(--rule);
  border-bottom: 2px solid var(--rule);
}
.st--plain { --cols: 40px minmax(0, 1fr) 84px; }

/* column headings */
.st-head, .st-row { display: grid; grid-template-columns: var(--cols); align-items: center; column-gap: 8px; }
.st-head {
  padding: 8px 12px;
  border-bottom: 1px solid var(--rule);
  font-family: var(--font-mono);
  font-size: 0.64rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-dim);
}
.st-h-cell, .st-h-total { text-align: right; }
.st-h-rank { text-align: center; }

.st-row { padding: 12px; border-top: 1px solid var(--line); position: relative; }
.st-head + .st-row, .st-row:first-child { border-top: none; }
.st-row.is-first { background: var(--gold-soft); }
.st-row.is-first::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 4px; background: var(--gold); }

/* place */
.st-rank { display: flex; justify-content: center; }
.st-badge {
  width: 28px; height: 28px; display: grid; place-items: center; border-radius: 50%;
  font-family: var(--font-mono); font-size: 0.85rem; font-weight: 700; color: var(--text-dim);
}
.st-badge--1, .st-badge--2, .st-badge--3 { border: 2px solid var(--text); color: var(--text); }
.st-badge--1 { background: var(--text); color: var(--ink-raised); }

/* player */
.st-player { min-width: 0; }
.st-name { font-family: var(--font-display); font-weight: 600; font-size: 1.15rem; line-height: 1.2; overflow-wrap: anywhere; }
.st-team {
  margin-left: 8px;
  font-family: var(--font-body);
  font-size: 0.66rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  border: 1.5px solid currentColor;
  border-radius: 3px;
  padding: 0 6px;
  vertical-align: 2px;
  color: var(--text-dim);
}
.st-bar { height: 3px; margin-top: 7px; background: var(--line); border-radius: 2px; overflow: hidden; }
.st-bar span { display: block; height: 100%; background: var(--text); opacity: 0.55; border-radius: 2px; transition: width 0.5s ease; }
.is-first .st-bar span { background: var(--gold); opacity: 1; }

/* the four competitions: columns on a wide screen */
.st-parts { display: contents; }
.st-cell { text-align: right; font-family: var(--font-mono); font-size: 0.95rem; font-weight: 500; }
.st-cell::before { content: none; }
.st-cell.is-none { color: var(--text-dim); opacity: 0.55; }
.st-cell.is-plus { color: var(--teal-ink, var(--teal)); }
.st-cell.is-minus { color: var(--coral-ink, var(--coral)); }

.st-total { text-align: right; font-family: var(--font-display); font-weight: 700; font-size: 1.75rem; line-height: 1; font-variant-numeric: tabular-nums; }
.is-first .st-total { color: var(--gold-ink, var(--gold)); }

/* face-down tiles on the final reveal */
.st-tile { display: flex; align-items: center; gap: 12px; width: 100%; text-align: left; background: transparent; color: inherit; font: inherit; border-left: none; border-right: none; border-bottom: none; }
.st-tile .st-rank { width: 40px; flex-shrink: 0; }
button.st-tile { cursor: pointer; }
.st-card {
  width: 46px; height: 46px; display: grid; place-items: center; border-radius: 3px; flex-shrink: 0;
  background: var(--text); color: var(--ink-raised);
  font-family: var(--font-display); font-weight: 700; font-size: 1.5rem;
}
.st-tile-label { flex: 1; color: var(--text-dim); font-size: 0.9rem; }
.st-tile.is-next .st-card { box-shadow: 0 0 0 3px var(--gold); animation: st-pulse 1.4s ease-in-out infinite; }
.st-tile.is-next .st-tile-label { color: var(--text); font-weight: 600; }
.st-row.is-flipping { animation: st-flip 0.7s ease-out; transform-origin: center; }
@keyframes st-flip { 0% { transform: rotateX(90deg); opacity: 0; } 100% { transform: rotateX(0); opacity: 1; } }
@keyframes st-pulse { 50% { box-shadow: 0 0 0 6px transparent; } }
@media (prefers-reduced-motion: reduce) { .st-row.is-flipping, .st-tile.is-next .st-card { animation: none; } .st-bar span { transition: none; } }

/* big screen */
.st--large { --cols: 48px minmax(0, 1fr) 70px 104px 86px 74px 104px; }
.st--large.st--plain { --cols: 48px minmax(0, 1fr) 120px; }
.st--large .st-row { padding: 16px 14px; }
.st--large .st-head { padding: 10px 14px; font-size: 0.78rem; }
.st--large .st-name { font-size: 1.7rem; }
.st--large .st-team { font-size: 0.75rem; }
.st--large .st-cell { font-size: 1.2rem; }
.st--large .st-total { font-size: 2.4rem; }
.st--large .st-badge { width: 38px; height: 38px; font-size: 1.1rem; }
.st--large .st-card { width: 60px; height: 60px; font-size: 2rem; }
.st--large .st-tile-label { font-size: 1.1rem; }
.st--large .st-tile .st-rank { width: 48px; }
.st--large .st-bar { height: 4px; }

/* phone: the competitions go under the name */
@media (max-width: 720px) {
  .st, .st--large, .st--large.st--plain, .st--plain { --cols: 36px minmax(0, 1fr) auto; }
  .st-head { display: none; }
  .st-row { align-items: start; row-gap: 8px; padding: 12px 10px; }
  .st-rank { padding-top: 2px; }
  .st-player { grid-column: 2; }
  .st-parts { display: grid; grid-column: 2 / 4; grid-template-columns: repeat(4, 1fr); gap: 4px 8px; padding-top: 2px; }
  .st-cell { text-align: left; font-size: 0.88rem; }
  .st-cell::before { content: attr(data-label); display: block; font-family: var(--font-body); font-size: 0.62rem; letter-spacing: 0.06em; text-transform: uppercase; color: var(--text-dim); opacity: 0.9; }
  .st-total { grid-column: 3; grid-row: 1; align-self: start; font-size: 1.9rem; }
  .st--large .st-name { font-size: 1.3rem; }
  .st--large .st-cell { font-size: 0.95rem; }
  .st--large .st-total { font-size: 2.2rem; }
  .st--large .st-badge { width: 30px; height: 30px; font-size: 0.9rem; }
  .st-tile { display: flex; align-items: center; }
}
</style>
