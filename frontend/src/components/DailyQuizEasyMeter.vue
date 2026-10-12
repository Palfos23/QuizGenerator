<template>
  <div v-if="percent !== null && percent !== undefined" class="dq-easy" :class="level.cls" :title="`Based on ${players} players' answers`">
    <span class="dq-easy-track"><i :style="{ width: percent + '%' }"></i></span>
    <span class="dq-easy-text"><b>{{ percent }}%</b> got it right<template v-if="!compact"> · {{ level.label }}</template></span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// How easy a daily quiz question was, from how the players scored on it: a thin bar, the percentage and
// (unless `compact`) an Easy / Medium / Hard word. Renders nothing when there are too few answers to say.
const props = defineProps({
  percent: { type: Number, default: null },
  players: { type: Number, default: 0 },
  compact: { type: Boolean, default: false }
})

const level = computed(() => {
  const p = props.percent ?? 0
  if (p >= 75) return { label: 'Easy', cls: 'is-easy' }
  if (p >= 45) return { label: 'Medium', cls: 'is-medium' }
  return { label: 'Hard', cls: 'is-hard' }
})
</script>
