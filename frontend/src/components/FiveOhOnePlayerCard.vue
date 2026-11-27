<template>
  <div class="fiveoo-player" :class="{ active, 'in-window': inWindow, offline }">
    <span v-if="active" class="fiveoo-player-turn">{{ turnLabel }}</span>
    <svg class="fiveoo-ring" viewBox="0 0 60 60" role="img" :aria-label="$t('{0} igjen', [total])">
      <circle class="fiveoo-ring-track" cx="30" cy="30" :r="RADIUS" />
      <circle
        class="fiveoo-ring-fill"
        cx="30"
        cy="30"
        :r="RADIUS"
        :stroke-dasharray="`${filled} ${CIRCUMFERENCE}`"
        transform="rotate(-90 30 30)"
      />
      <text x="30" y="35" text-anchor="middle" class="fiveoo-ring-number">{{ total }}</text>
    </svg>
    <div class="fiveoo-player-name">{{ name }}</div>
    <div v-if="offline" class="tag offline fiveoo-player-tag">{{ $t('Frakoblet§OnlineFiveOhOneGameT') }}</div>
    <div v-else-if="inWindow" class="fiveoo-player-window">{{ $t('I vinduet!') }}</div>
    <div v-if="throws.length" class="fiveoo-recent-throws">
      <span
        v-for="(t, i) in throws"
        :key="i"
        class="fiveoo-throw-chip"
        :class="{ bust: t.bust, zero: !t.bust && t.score === 0 }"
        :title="t.name"
      >{{ t.bust ? 'bust' : t.score }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// One player's seat in 501 (pass-and-play and online): what's left as a number inside a ring
// that fills up on the way from 501 down to 0, plus their last few throws.
const props = defineProps({
  name: { type: String, required: true },
  total: { type: Number, required: true },
  active: { type: Boolean, default: false },
  turnLabel: { type: String, default: '' },
  inWindow: { type: Boolean, default: false },
  offline: { type: Boolean, default: false },
  throws: { type: Array, default: () => [] } // [{ score, bust, name }], oldest first
})

const START = 501
const RADIUS = 25
const CIRCUMFERENCE = 2 * Math.PI * RADIUS
const filled = computed(() => {
  const progress = Math.min(1, Math.max(0, (START - props.total) / START))
  return (progress * CIRCUMFERENCE).toFixed(1)
})
</script>
