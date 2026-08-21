<template>
  <div class="loading-state" :class="{ 'loading-state-full': full }">
    <span class="spinner"></span>
    <template v-if="full">
      <h1>{{ message }}</h1>
      <p v-if="subtitle" class="loading-state-subtitle">{{ subtitle }}</p>
    </template>
    <p v-else-if="message">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  message: { type: String, default: 'Loading…' },
  subtitle: { type: String, default: '' },
  full: { type: Boolean, default: false }
})
</script>
