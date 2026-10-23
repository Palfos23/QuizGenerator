<template>
  <div v-if="!bdayAuth.hasPin.value" style="max-width:360px; margin:80px auto; padding:0 20px; text-align:center;">
    <h1 style="margin-bottom:6px;">🎂 Bursdagsquiz</h1>
    <p class="page-subtitle">Denne siden er bare for arrangøren - skriv inn PIN-koden.</p>
    <div v-if="error" class="banner error">{{ error }}</div>
    <div class="field" style="text-align:left;">
      <label>PIN</label>
      <input
        type="password"
        v-model="pinInput"
        placeholder="PIN"
        autocomplete="off"
        autofocus
        @keydown.enter="submit"
      />
    </div>
    <button class="btn btn-primary" style="width:100%;" :disabled="checking" @click="submit">
      {{ checking ? 'Sjekker…' : 'Gå inn' }}
    </button>
  </div>
  <slot v-else />
</template>

<script setup>
import { ref } from 'vue'
import bdayAuth from '../composables/useBdayPin'
import bdayApi from '../services/bdayApi'

// Wraps the host's page: asks for the quiz's PIN (not the site's admin login - admins may be playing) and only shows
// the page once the server has accepted it. A wrong PIN, or a PIN the server later turns away, drops back to this prompt.
const pinInput = ref('')
const error = ref('')
const checking = ref(false)

async function submit() {
  const value = pinInput.value.trim()
  if (!value) return
  checking.value = true
  error.value = ''
  bdayAuth.setPin(value)
  try {
    await bdayApi.checkPin()
  } catch (e) {
    bdayAuth.clearPin()
    error.value = e.response?.status === 429 ? (e.response?.data?.message || 'For mange forsøk. Prøv igjen senere.') : 'Feil PIN-kode.'
  } finally {
    checking.value = false
  }
}
</script>
