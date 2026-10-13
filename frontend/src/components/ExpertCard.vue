<template>
  <section class="expert-card" :class="verdict ? `is-${verdict.tone}` : ''">
    <header class="expert-card-head">
      <span class="expert-card-icon">
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9L12 3z" /></svg>
      </span>
      <div>
        <strong>Beat the expert</strong>
        <small>{{ name }} writes the questions - can you score higher?</small>
      </div>
    </header>

    <div class="expert-card-vs">
      <div v-if="hasYou" class="expert-card-side">
        <span class="expert-card-who">You</span>
        <b class="expert-card-score">{{ yourValue }}<small v-if="max !== null"> / {{ max }}</small><small v-else> pts</small></b>
        <i v-if="yourNote">{{ yourNote }}</i>
      </div>
      <span v-if="hasYou" class="expert-card-versus">vs</span>
      <div class="expert-card-side expert-card-side--expert">
        <span class="expert-card-who">{{ name }}</span>
        <b class="expert-card-score">{{ expertValue }}<small v-if="max !== null"> / {{ max }}</small><small v-else> pts</small></b>
        <i v-if="expertNote">{{ expertNote }}</i>
      </div>
    </div>

    <p v-if="verdict" class="expert-card-verdict">{{ verdict.text }}</p>
  </section>
</template>

<script setup>
import { computed } from 'vue'

// "Beat the expert": the question author's score next to yours, with a plain sentence saying where you
// stand. Used on the daily quiz results page, the leaderboard window and the weekly standings. `max` is the
// most points available for a single day (null for a week's running total).
const props = defineProps({
  name: { type: String, required: true },
  expertValue: { type: Number, required: true },
  max: { type: Number, default: null },
  yourValue: { type: Number, default: null },
  expertNote: { type: String, default: '' },
  yourNote: { type: String, default: '' },
  period: { type: String, default: '' } // e.g. "this week" - appended to the verdict sentence
})

const hasYou = computed(() => props.yourValue !== null && props.yourValue !== undefined)

const verdict = computed(() => {
  if (!hasYou.value) return null
  const diff = props.yourValue - props.expertValue
  const when = props.period ? ` ${props.period}` : ''
  const pts = n => `${n} ${n === 1 ? 'point' : 'points'}`
  if (diff > 0) return { tone: 'ahead', text: `You beat the expert${when} by ${pts(diff)}!` }
  if (diff === 0) return { tone: 'level', text: `You matched the expert${when}.` }
  return { tone: 'behind', text: `You're ${pts(-diff)} behind the expert${when}.` }
})
</script>

<style scoped>
.expert-card {
  margin: 0 0 16px;
  padding: 14px 16px 16px;
  border-radius: 16px;
  background: linear-gradient(145deg, rgba(139, 124, 255, 0.26), rgba(139, 124, 255, 0.08));
  box-shadow: inset 0 0 0 1.5px rgba(181, 169, 255, 0.4);
  text-align: left;
}
.expert-card.is-ahead { box-shadow: inset 0 0 0 1.5px rgba(85, 232, 172, 0.55); }

.expert-card-head { display: flex; align-items: center; gap: 12px; }
.expert-card-head > div { display: flex; flex-direction: column; min-width: 0; }
.expert-card-head strong { font-size: 1rem; }
.expert-card-head small { color: var(--text-dim); font-size: 0.82rem; line-height: 1.35; }
.expert-card-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 10px;
  color: #b5a9ff;
  background: rgba(139, 124, 255, 0.28);
}
.expert-card-icon svg { width: 18px; height: 18px; }

.expert-card-vs {
  display: flex;
  align-items: stretch;
  justify-content: center;
  gap: 10px;
  margin-top: 14px;
}
.expert-card-side {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.09);
  text-align: center;
}
.expert-card-side--expert { background: rgba(139, 124, 255, 0.22); }
.expert-card-who { font-size: 0.74rem; text-transform: uppercase; letter-spacing: 0.08em; font-weight: 700; color: var(--text-dim); overflow: hidden; text-overflow: ellipsis; max-width: 100%; white-space: nowrap; }
.expert-card-score { font-family: var(--font-display); font-size: 1.7rem; line-height: 1.15; margin-top: 2px; }
.expert-card-score small { font-family: var(--font-body); font-size: 0.82rem; font-weight: 500; color: var(--text-dim); }
.expert-card-side i { font-style: normal; font-size: 0.76rem; color: var(--text-dim); margin-top: 2px; }
.expert-card-versus { align-self: center; color: var(--text-dim); font-size: 0.8rem; font-weight: 700; }

.expert-card-verdict { margin: 12px 0 0; text-align: center; font-weight: 650; font-size: 0.92rem; }
.expert-card.is-ahead .expert-card-verdict { color: #55e8ac; }
</style>
