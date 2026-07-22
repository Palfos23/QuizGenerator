<template>
  <div class="modal-backdrop" @click.self="$emit('cancel')">
    <div class="modal" role="alertdialog" aria-modal="true" :aria-label="title">
      <h2>{{ title }}</h2>
      <p class="page-subtitle" style="margin-bottom:24px;">{{ message }}</p>
      <div style="display:flex; gap:10px; justify-content:flex-end;">
        <button class="btn btn-secondary" @click="$emit('cancel')">{{ cancelText }}</button>
        <button class="btn btn-danger-solid" @click="$emit('confirm')">
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: 'Are you sure?' },
  message: { type: String, required: true },
  confirmText: { type: String, default: 'Delete' },
  cancelText: { type: String, default: 'Cancel' }
})

defineEmits(['confirm', 'cancel'])
</script>
