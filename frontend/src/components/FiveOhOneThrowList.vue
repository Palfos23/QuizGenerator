<template>
  <div v-if="last" class="fiveoo-last-throw" :class="last.bust ? 'bust' : last.score === 0 ? 'zero' : 'hit'" aria-live="polite">
    <span class="fiveoo-last-throw-text">
      <strong>{{ last.player }}</strong> · {{ last.name }}
      <span class="fiveoo-last-throw-raw">({{ last.rawValue }})</span>
    </span>
    <span class="fiveoo-throw-chip" :class="{ bust: last.bust, zero: !last.bust && last.score === 0 }">{{ chipText(last) }}</span>
  </div>

  <details v-if="showHistory && throws.length" class="fiveoo-history">
    <summary>{{ $t('Kastehistorikk') }} <span class="fiveoo-history-count">{{ throws.length }}</span></summary>
    <div class="fiveoo-history-list">
      <div v-for="(t, i) in newestFirst" :key="i" class="fiveoo-history-row">
        <span class="fiveoo-history-player">{{ t.player }}</span>
        <span class="fiveoo-history-name">{{ t.name }} <span class="fiveoo-last-throw-raw">({{ t.rawValue }})</span></span>
        <span class="fiveoo-throw-chip" :class="{ bust: t.bust, zero: !t.bust && t.score === 0 }">{{ chipText(t) }}</span>
        <span class="fiveoo-history-total">{{ t.resultingTotal }}</span>
      </div>
    </div>
  </details>
</template>

<script setup>
import { computed } from 'vue'
import { t as _t } from '../i18n'

// 501's last throw (as a small card) and the full throw history (a compact list: who, what,
// what it scored, and what they're on now). Throws come in oldest first, already normalised to
// { player, name, rawValue, score, bust, resultingTotal } by the pass-and-play and online games.
const props = defineProps({
  throws: { type: Array, default: () => [] },
  showLast: { type: Boolean, default: true },
  showHistory: { type: Boolean, default: true }
})

const newestFirst = computed(() => [...props.throws].reverse())
const last = computed(() => (props.showLast && props.throws.length ? props.throws[props.throws.length - 1] : null))

function chipText(t) {
  if (t.bust) return _t('bust')
  return t.score === 0 ? '0' : '−' + t.score
}
</script>
