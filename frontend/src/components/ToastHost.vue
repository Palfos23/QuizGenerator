<template>
  <transition name="toast-fade">
    <div v-if="toast.state.visible" class="toast" :class="toast.state.type" role="status" aria-live="polite" aria-atomic="true">
      {{ toast.state.message }}
    </div>
  </transition>
</template>

<script setup>
import toast from '../services/toast'
</script>
