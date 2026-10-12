<template>
  <div v-if="percent !== null && percent !== undefined" class="dq-easy" :title="`Based on ${players} players' answers`">
    <span class="dq-easy-track"><i :style="{ width: percent + '%' }"></i></span>
    <span class="dq-easy-text"><b>{{ percent }}%</b> got it right</span>
  </div>
</template>

<script setup>
// How easy a daily quiz question was, from how the players scored on it: a thin neutral bar and the
// percentage. Renders nothing when there are too few answers to say.
defineProps({
  percent: { type: Number, default: null },
  players: { type: Number, default: 0 }
})
</script>
