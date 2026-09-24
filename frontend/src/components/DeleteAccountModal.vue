<template>
  <div class="modal-backdrop" @click.self="$emit('cancel')">
    <div class="modal" role="alertdialog" aria-modal="true" aria-label="Delete your account">
      <h2 style="margin-top:0;">Delete your account?</h2>
      <p class="page-subtitle">
        This permanently deletes your account, your saved quizzes, and your game history. Questions or reports
        you've submitted stay (other people may already be seeing or reviewing them), but are no longer linked to
        you. This can't be undone.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Password <span class="picker-hint">only if you signed up with an email + password - leave blank for a Google account</span></label>
        <input type="password" v-model="password" autocomplete="current-password" />
      </div>

      <div class="field">
        <label>Type <strong>DELETE</strong> to confirm</label>
        <input type="text" v-model="confirmText" autocomplete="off" />
      </div>

      <div style="display:flex; gap:10px; justify-content:flex-end;">
        <button class="btn btn-secondary" @click="$emit('cancel')">Cancel</button>
        <button class="btn btn-danger-solid" :disabled="confirmText !== 'DELETE' || deleting" @click="submit">
          {{ deleting ? 'Deleting…' : 'Delete my account' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useEscapeKey } from '../composables/useEscapeKey'

const emit = defineEmits(['confirm', 'cancel'])

const password = ref('')
const confirmText = ref('')
const deleting = ref(false)
const error = ref('')

useEscapeKey(() => emit('cancel'))

function submit() {
  if (confirmText.value !== 'DELETE' || deleting.value) return
  deleting.value = true
  error.value = ''
  emit('confirm', password.value)
}

// Parent (AccountView) owns the actual API call - calls this via a template
// ref if it fails, so the modal stays open with the error shown instead of
// silently closing.
function setError(message) {
  error.value = message
  deleting.value = false
}

defineExpose({ setError })
</script>
