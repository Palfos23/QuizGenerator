<template>
  <slot v-if="unlocked" />
  <div v-else class="adv-gate">
    <h1 style="margin-bottom:6px;">🎄 {{ $t('Julekalender') }}</h1>
    <p class="page-subtitle">{{ $t('Denne siden er bare for spillmesteren - skriv inn passordet.') }}</p>
    <div v-if="error" class="banner error">{{ error }}</div>
    <div class="field" style="text-align:left;">
      <label>{{ $t('Passord') }}</label>
      <input type="password" v-model="input" :placeholder="$t('Passord')" autocomplete="off" autofocus @keydown.enter="submit" />
    </div>
    <button class="btn btn-primary" style="width:100%;" :disabled="checking" @click="submit">
      {{ checking ? $t('Sjekker…') : $t('Gå inn') }}
    </button>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { t as _t } from '../i18n'
import adventAdmin from '../services/adventAdmin'
import api from '../services/api'

// Wraps the game master's page: asks for the calendar's password (on top of the admin login the page already needs) and
// only shows the page once the server has accepted it. A password the server later turns away drops back to this prompt.
const unlocked = ref(false)
const input = ref('')
const error = ref('')
const checking = ref(false)

async function verify(typed) {
  try {
    await api.adminGetAdventSeason()
    unlocked.value = true
    return true
  } catch (e) {
    if (e.response?.status === 403) {
      adventAdmin.clear()
      if (typed) error.value = _t('Feil passord.')
    } else if (e.response?.status === 429) {
      error.value = e.response?.data?.message || _t('For mange forsøk. Prøv igjen senere.')
    } else {
      error.value = _t('Kunne ikke sjekke passordet.')
    }
    return false
  }
}

async function submit() {
  const value = input.value.trim()
  if (!value) return
  checking.value = true
  error.value = ''
  adventAdmin.setPassword(value)
  if (!(await verify(true))) adventAdmin.clear()
  checking.value = false
}

// someone who already entered it in this tab goes straight in - unless it has stopped working
onMounted(() => {
  if (adventAdmin.state.password) verify(false)
})

// the server turning the password away mid-use (changed in the dashboard, say) puts the prompt back
watch(() => adventAdmin.state.password, value => {
  if (!value) unlocked.value = false
})
</script>

<style scoped>
.adv-gate { max-width: 360px; margin: 80px auto; padding: 0 20px; text-align: center; }
</style>
