<template>
  <div class="lg-match" :class="{ 'is-mine': mine, 'is-final': match.status === 'FINAL' }">
    <span class="lg-side lg-side--a" :class="{ 'is-win': sideWon(match, 'a'), 'is-me': match.a?.me }">
      <small v-if="match.seedA" class="lg-seed">{{ match.seedA }}</small>{{ sideName(match, 'a') }}
    </span>
    <span class="lg-score">
      <b :class="{ 'is-win': sideWon(match, 'a') }">{{ sideScore(match, 'a') }}</b>
      <span class="lg-dash">–</span>
      <b :class="{ 'is-win': sideWon(match, 'b') }">{{ sideScore(match, 'b') }}</b>
    </span>
    <span class="lg-side lg-side--b" :class="{ 'is-win': sideWon(match, 'b'), 'is-me': match.b?.me }">
      {{ sideName(match, 'b') }}<small v-if="match.seedB" class="lg-seed">{{ match.seedB }}</small>
    </span>
    <span v-if="status" class="lg-status" :class="`is-${match.status.toLowerCase()}`">{{ status }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { matchStatusName, sideName, sideScore, sideWon } from '../../composables/leagueFormat'

const props = defineProps({ match: { type: Object, required: true } })
const mine = computed(() => props.match.a?.me || props.match.b?.me)
const status = computed(() => matchStatusName(props.match))
</script>

<style scoped>
.lg-match { display: grid; grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr); align-items: center; gap: 4px 12px; padding: 10px 12px; border-radius: var(--radius-md); }
.lg-match.is-mine { background: rgb(var(--primary-rgb) / 0.07); }
.lg-side { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-dim); }
.lg-side--a { text-align: right; }
.lg-side.is-me { color: var(--text); }
.lg-side.is-win { color: var(--text); font-weight: 700; }
.lg-seed { display: inline-block; margin: 0 6px; color: var(--text-dim); font-size: 0.72rem; font-weight: 700; }
.lg-score { display: inline-flex; align-items: baseline; gap: 6px; min-width: 64px; justify-content: center; font-variant-numeric: tabular-nums; }
.lg-score b { min-width: 1.4em; text-align: center; font-weight: 600; color: var(--text-dim); }
.lg-score b.is-win { color: var(--text); font-weight: 800; }
.lg-dash { color: var(--text-dim); }
.lg-status { grid-column: 1 / -1; justify-self: center; font-size: 0.74rem; font-weight: 600; color: var(--text-dim); }
.lg-status.is-live { color: var(--gold-ink); }
.lg-status.is-pending { color: var(--amber-ink); }
</style>
