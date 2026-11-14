<template>
  <div class="skeleton-block" role="status" aria-live="polite">
    <span class="skeleton skeleton-title"></span>
    <span v-for="n in rows" :key="n" class="skeleton skeleton-row" :style="{ opacity: 1 - n * 0.12 }"></span>
    <span class="sr-only">{{ label }}</span>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'

// What a page shows while its data is on the way: the rough shape of what is coming (a heading and a few rows) instead
// of a line of text, so the page doesn't jump when the content arrives. For waits inside games see LoadingState.
defineProps({
  rows: { type: Number, default: 3 },
  label: { type: String, default: _t('Laster…') }
})
</script>

<style scoped>
.skeleton-block { display: grid; gap: var(--space-3); padding: var(--space-2) 0; }
.skeleton-title { width: 38%; height: 22px; margin-bottom: var(--space-1); }
.skeleton-row { height: 64px; border-radius: var(--radius-lg); }
</style>
