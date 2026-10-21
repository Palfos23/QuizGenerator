<template>
  <section class="expert-card" :class="`is-${state}`">
    <svg class="expert-card-star" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9L12 3z" /></svg>
    <div class="expert-card-text">
      <strong>{{ title }}</strong>
      <span>{{ detail }}</span>
    </div>
  </section>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed } from 'vue'

// "Beat the expert": one calm line pair - a title that asks (or answers) the question, and the numbers
// underneath. Used on the daily quiz results page, the leaderboard window and the weekly standings.
// `max` is the most points available for a single day (null for a week's running total, where the notes
// say how many days each side has played).
const props = defineProps({
  expertValue: { type: Number, required: true },
  max: { type: Number, default: null },
  yourValue: { type: Number, default: null },
  expertNote: { type: String, default: '' },
  yourNote: { type: String, default: '' }
})

const hasYou = computed(() => props.yourValue !== null && props.yourValue !== undefined)

// ahead / level / behind - or "open" when there's no score of yours to compare with.
const state = computed(() => {
  if (!hasYou.value) return 'open'
  if (props.yourValue > props.expertValue) return 'ahead'
  if (props.yourValue === props.expertValue) return 'level'
  return 'behind'
})

const title = computed(() => {
  if (state.value === 'ahead') return _t('Du slo eksperten!')
  if (state.value === 'level') return _t('Du var like god som eksperten!')
  return _t('Kan du få høyere poengsum enn eksperten?')
})

function part(value, note) {
  const base = props.max !== null ? `${value} / ${props.max}` : _t('{0} poeng', [value])
  return note ? `${base} (${note})` : base
}

const detail = computed(() => {
  const expert = _t('Eksperten: {0}', [part(props.expertValue, props.expertNote)])
  return hasYou.value ? _t('{0} · Du: {1}', [expert, part(props.yourValue, props.yourNote)]) : expert
})
</script>

<style scoped>
.expert-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 0 0 14px;
  padding: 12px 14px;
  border-radius: 2px;
  background: var(--ink-raised);
  border: 2px solid var(--rule);
  text-align: left;
}
.expert-card.is-ahead { border-color: var(--teal); background: rgb(var(--teal-rgb) / 0.08); }

.expert-card-star { width: 18px; height: 18px; flex-shrink: 0; margin-top: 2px; color: var(--gold); }
.expert-card.is-ahead .expert-card-star { color: var(--teal); }

.expert-card-text { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.expert-card-text strong { font-size: 0.95rem; line-height: 1.3; font-family: var(--font-display); font-weight: 600; }
.expert-card.is-ahead .expert-card-text strong { color: var(--teal); }
.expert-card-text span { color: var(--text-dim); font-size: 0.85rem; line-height: 1.4; white-space: pre-wrap; }
</style>
