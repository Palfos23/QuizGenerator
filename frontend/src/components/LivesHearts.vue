<template>
  <div
    class="lives-hearts"
    role="img"
    :aria-label="`${remaining} of ${max} lives remaining`"
  >
    <span
      v-for="i in max"
      :key="i"
      class="life-heart"
      :class="{ lost: i > remaining, 'just-lost': i === flashIndex }"
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
      </svg>
    </span>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

// One lives indicator, shared by every game mode with a "lose a life on a wrong
// answer" rule (solo Weekly Grid / Starting XI and their Battle variants).
// Hearts start filled red; each life lost empties the rightmost filled heart to
// an outline. Pass how many lives have been `used` (i.e. strikes taken).
const props = defineProps({
  max: { type: Number, required: true },
  used: { type: Number, default: 0 }
})

const remaining = computed(() => Math.max(0, props.max - Math.max(0, props.used)))

// Pop the heart that just emptied. Internal so no caller has to track it -
// only fires on a single-life drop (skips batch jumps from an initial sync).
const flashIndex = ref(null)
let flashTimer = null
watch(() => props.used, (now, before) => {
  if (now === before + 1) {
    flashIndex.value = remaining.value + 1
    clearTimeout(flashTimer)
    flashTimer = setTimeout(() => { flashIndex.value = null }, 500)
  }
})
</script>
