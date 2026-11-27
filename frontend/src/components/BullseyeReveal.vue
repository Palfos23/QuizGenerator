<template>
  <div class="bullseye-reveal">
    <!-- The answer key: the target is known from the start, the name(s) behind it appear once
         every guess has been revealed. -->
    <div class="bullseye-key" :class="{ open: showTruth }">
      <div class="bullseye-key-label">
        {{ showTruth && truth.length && truth[0].distance !== 0 ? $t('Nærmeste mulige svar') : $t('Riktig svar') }}
      </div>
      <div v-if="showTruth && truth.length" class="bullseye-key-names">
        <span v-for="e in truth" :key="e.key" class="bullseye-key-name">
          {{ e.name }} · {{ fmt(e.statValue) }}<span v-if="e.foundBy" class="bullseye-key-found">✓ {{ e.foundBy }}</span>
        </span>
      </div>
      <div v-else class="bullseye-key-names">
        <span class="bullseye-key-name">? · {{ fmt(target) }}</span>
      </div>
    </div>

    <!-- Number line: the target in the middle, each revealed guess where it landed. The scale is
         set by every guess (revealed or not), so the dots don't move as more of them appear. -->
    <div class="bullseye-line" aria-hidden="true">
      <div class="bullseye-line-track"></div>
      <div class="bullseye-line-target" style="left:50%">
        <span class="bullseye-line-target-dot"></span>
        <span class="bullseye-line-value target">{{ fmt(target) }}</span>
      </div>
      <TransitionGroup name="bullseye-dot">
        <div
          v-for="(r, idx) in revealedResults"
          :key="r.key"
          class="bullseye-line-mark"
          :class="[markState(r), idx % 2 ? 'below' : 'above']"
          :style="{ left: positionPct(r.statValue) + '%' }"
        >
          <span class="bullseye-line-name">{{ r.player }}</span>
          <span class="bullseye-line-dot">{{ idx + 1 }}</span>
        </div>
      </TransitionGroup>
    </div>

    <div class="bullseye-rows">
      <div
        v-for="(r, idx) in results"
        :key="r.key"
        class="bullseye-row"
        :class="idx < revealCount ? markState(r) : 'hidden'"
      >
        <span class="bullseye-row-rank">{{ idx < revealCount ? idx + 1 : '?' }}</span>
        <span class="bullseye-row-main">
          <template v-if="idx < revealCount">
            <strong>{{ r.player }}</strong>
            <span class="bullseye-row-guess">{{ r.guess }} · {{ fmt(r.statValue) }}</span>
          </template>
          <span v-else class="bullseye-row-guess">{{ $t('Skjult til avsløringen') }}</span>
        </span>
        <span v-if="idx < revealCount" class="bullseye-row-chip">
          <template v-if="r.distance === 0">{{ $t('Blink!') }}</template>
          <template v-else-if="r.eliminated">{{ $t('Ute · {0}', [fmt(r.distance)]) }}</template>
          <template v-else>{{ fmt(r.distance) }} {{ $t('unna') }}</template>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// Bullseye's round reveal, shared by pass-and-play and online: the answer key on top, a number
// line with every revealed guess placed where it landed relative to the target, and the ranked
// list (closest first) with how far off each was - and who's out.
const props = defineProps({
  target: { type: Number, required: true },
  // closest first: [{ key, player, guess, statValue, distance, eliminated }]
  results: { type: Array, default: () => [] },
  revealCount: { type: Number, default: 0 },
  // the closest possible answer(s): [{ key, name, statValue, distance, foundBy }]
  truth: { type: Array, default: () => [] },
  showTruth: { type: Boolean, default: false },
  fmt: { type: Function, default: n => n }
})

const revealedResults = computed(() => props.results.slice(0, props.revealCount))
const span = computed(() => Math.max(1, ...props.results.map(r => Math.abs(r.statValue - props.target))))

// 50% is the target; the furthest guess sits 44% away, so its label still fits on screen.
function positionPct(value) {
  return 50 + ((value - props.target) / span.value) * 44
}

function markState(r) {
  if (r.distance === 0) return 'hit'
  return r.eliminated ? 'out' : 'safe'
}
</script>
