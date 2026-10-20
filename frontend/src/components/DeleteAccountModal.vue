<template>
  <div class="modal-backdrop" @click.self="$emit('cancel')">
    <div class="modal" role="alertdialog" aria-modal="true" aria-label="Slett kontoen din">
      <h2 style="margin-top:0;">Slette kontoen din?</h2>
      <p class="page-subtitle">
        Dette sletter kontoen din, de lagrede quizene dine og spillhistorikken din for godt. Spørsmål eller rapporter du har sendt inn blir værende (andre kan allerede se eller vurdere dem), men er ikke lenger knyttet til deg. Dette kan ikke angres.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Passord <span class="picker-hint">bare hvis du registrerte deg med e-post og passord - la stå tomt for en Google-konto</span></label>
        <input type="password" v-model="password" autocomplete="current-password" />
      </div>

      <div class="field">
        <label>Skriv <strong>SLETT</strong> for å bekrefte</label>
        <input type="text" v-model="confirmText" autocomplete="off" />
      </div>

      <div style="display:flex; gap:10px; justify-content:flex-end;">
        <button class="btn btn-secondary" @click="$emit('cancel')">Avbryt</button>
        <button class="btn btn-danger-solid" :disabled="confirmText !== 'DELETE' || deleting" @click="submit">
          {{ deleting ? 'Sletter…' : 'Slett kontoen min' }}
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
