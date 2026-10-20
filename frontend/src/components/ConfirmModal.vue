<template>
  <div class="modal-backdrop" @click.self="!busy && $emit('cancel')">
    <div class="modal" role="alertdialog" aria-modal="true" :aria-label="title">
      <h2>{{ title }}</h2>
      <p class="page-subtitle" style="margin-bottom:24px;">{{ message }}</p>
      <div class="modal-actions">
        <button class="btn btn-secondary" :disabled="busy" @click="$emit('cancel')">{{ cancelText }}</button>
        <button class="btn btn-danger-solid" :disabled="busy" @click="$emit('confirm')">
          {{ busy ? busyText : confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { useEscapeKey } from '../composables/useEscapeKey'

const props = defineProps({
  title: { type: String, default: _t('Er du sikker?') },
  message: { type: String, required: true },
  confirmText: { type: String, default: _t('Slett') },
  cancelText: { type: String, default: _t('Avbryt') },
  // Optional - a caller passes :busy="deleting" (plus a busyText if "Jobber…"
  // isn't the right verb) so the modal visibly reflects an in-flight request
  // instead of just sitting there or vanishing with no feedback while it waits.
  busy: { type: Boolean, default: false },
  busyText: { type: String, default: _t('Jobber…') }
})

const emit = defineEmits(['confirm', 'cancel'])

useEscapeKey(() => { if (!props.busy) emit('cancel') })
</script>
