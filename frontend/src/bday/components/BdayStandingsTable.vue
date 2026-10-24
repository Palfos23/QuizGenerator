<template>
  <div class="st" :class="{ 'st--large': large }">
    <div v-if="!players.length" class="empty-state">Ingen spillere ennå.</div>
    <div v-for="p in players" :key="p.guestId" class="st-row" :class="{ 'is-first': p.rank === 1 }">
      <span class="st-rank">{{ p.rank }}</span>
      <div class="st-main">
        <div class="st-name">{{ p.name }}<span v-if="p.teamName" class="st-team">{{ p.teamName }}</span></div>
        <div class="st-parts">
          <span :class="{ 'is-none': !p.quizDone }">Quiz {{ p.quizDone ? p.quizScore : '–' }}</span>
          <span :class="{ 'is-none': p.marioKartTimeMs === null }">Mario Kart {{ p.marioKartTimeMs === null ? '–' : p.marioKartPoints }}</span>
          <span :class="{ 'is-none': !p.teamName }">Musikk {{ p.teamName ? p.musicPoints : '–' }}</span>
          <span :class="{ 'is-none': p.bonusPoints === 0 }">Bonus {{ p.bonusPoints > 0 ? '+' + p.bonusPoints : p.bonusPoints }}</span>
        </div>
      </div>
      <span class="st-total">{{ p.total }}</span>
    </div>
  </div>
</template>

<script setup>
// The combined birthday-night board: one row per guest, the total big and the four parts it is made of small underneath.
defineProps({
  players: { type: Array, required: true }, // PlayerStanding[]
  large: { type: Boolean, default: false } // for the big screen
})
</script>

<style scoped>
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
