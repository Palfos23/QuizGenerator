<template>
  <div class="loading-state">
    <span class="spinner"></span>
    <p v-if="message">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  message: { type: String, default: 'Loading…' }
})
</script>
