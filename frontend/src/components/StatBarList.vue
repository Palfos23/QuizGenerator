<template>
  <div v-if="!items.length" class="empty-state" style="padding:18px;">{{ empty }}</div>
  <div v-else class="stat-barlist">
    <div v-for="i in items" :key="i.label" class="stat-barlist-row">
      <div class="stat-barlist-label" :title="i.label">{{ i.label }}</div>
      <div class="stat-barlist-track">
        <div class="stat-barlist-fill" :style="{ width: (i.count / max * 100) + '%', backgroundColor: color }"></div>
      </div>
      <div class="stat-barlist-value">{{ i.count.toLocaleString($locale) }}</div>
    </div>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  color: { type: String, default: 'var(--gold)' },
  empty: { type: String, default: _t('Ingenting å vise ennå.') }
})

// Bars scale to the largest value in the set.
const max = computed(() => Math.max(...props.items.map(i => i.count), 1))
</script>

<style scoped>
.stat-barlist {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.stat-barlist-row {
  display: grid;
  grid-template-columns: minmax(90px, 160px) 1fr auto;
  align-items: center;
  gap: 12px;
}
.stat-barlist-label {
  color: var(--text);
  font-size: 0.86rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.stat-barlist-track {
  background: rgb(var(--fg-rgb) / 0.10);
  border-radius: 0;
  height: 10px;
  overflow: hidden;
}
.stat-barlist-fill {
  height: 100%;
  border-radius: 0;
  min-width: 2px;
  transition: width 0.4s ease;
}
.stat-barlist-value {
  font-family: var(--font-mono);
  font-size: 0.85rem;
  color: var(--text);
  min-width: 32px;
  text-align: right;
}
</style>
