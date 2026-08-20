<template>
  <div class="loading-state" :class="{ 'loading-state-full': full }">
    <span class="spinner"></span>
    <p v-if="message">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  message: { type: String, default: 'Loading…' },
  full: { type: Boolean, default: false }
})
</script>
