<template>
  <div v-if="totalPages > 1" class="pagination-bar no-print">
    <button class="btn btn-secondary btn-sm" :disabled="page <= 1" @click="$emit('update:page', page - 1)">← Prev</button>
    <span class="pagination-info">Page {{ page }} of {{ totalPages }} · {{ totalItems }} total</span>
    <button class="btn btn-secondary btn-sm" :disabled="page >= totalPages" @click="$emit('update:page', page + 1)">Next →</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, required: true },
  pageSize: { type: Number, required: true },
  totalItems: { type: Number, required: true }
})
defineEmits(['update:page'])

const totalPages = computed(() => Math.max(1, Math.ceil(props.totalItems / props.pageSize)))
</script>
