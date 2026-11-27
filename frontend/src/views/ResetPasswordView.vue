<template>
  <div style="max-width:420px; margin:40px auto;">
    <template v-if="!token">
      <h1>{{ $t('Nullstill passordet ditt') }}</h1>
      <p class="page-subtitle">{{ $t('Denne lenken mangler nullstillingskode - åpne lenken fra e-posten din på nytt, eller be om en ny fra innloggingssiden.') }}</p>
      <router-link to="/" class="btn btn-secondary">{{ $t('Tilbake til innlogging') }}</router-link>
    </template>

    <template v-else-if="done">
      <h1>{{ $t('Passordet er oppdatert') }}</h1>
      <p class="page-subtitle">{{ $t('Du er logget inn - sender deg videre…') }}</p>
    </template>

    <template v-else>
      <h1>{{ $t('Nullstill passordet ditt') }}</h1>
      <p class="page-subtitle">{{ $t('Velg et nytt passord for kontoen din.') }}</p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>{{ $t('Nytt passord') }}<input type="password" v-model="newPassword" :placeholder="$t('Minst 8 tegn')" autocomplete="new-password" @keyup.enter="submit" /></label>
      </div>
      <div class="field">
        <label>{{ $t('Bekreft passord') }}<input type="password" v-model="confirmPassword" :placeholder="$t('Skriv det inn igjen')" autocomplete="new-password" @keyup.enter="submit" /></label>
      </div>

      <button class="btn btn-primary" style="width:100%;" :disabled="!canSubmit || submitting" @click="submit">
        {{ submitting ? $t('Lagrer…') : $t('Lagre nytt passord') }}
      </button>
    </template>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'
import toast from '../services/toast'
import { takeInviteRoute } from '../services/pendingInvite'

const route = useRoute()
const router = useRouter()
const token = route.query.token || ''

const newPassword = ref('')
const confirmPassword = ref('')
const error = ref('')
const submitting = ref(false)
const done = ref(false)

const canSubmit = computed(() =>
  newPassword.value.length >= 8 && newPassword.value === confirmPassword.value
)

async function submit() {
  error.value = ''
  if (newPassword.value.length < 8) {
    error.value = _t('Passordet må være minst 8 tegn.')
    return
  }
  if (new TextEncoder().encode(newPassword.value).length > 72) {
    error.value = _t('Passordet kan høyst være 72 tegn.')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    error.value = _t('Passordene er ikke like.')
    return
  }
  submitting.value = true
  try {
    // A successful reset logs you straight in - no going back to type the new password again.
    const result = await api.resetPassword(token, newPassword.value)
    done.value = true
    auth.login({ token: result.token, displayName: result.displayName, role: result.role })
    toast.show(_t('Passordet er oppdatert, og du er logget inn.'))
    router.push(takeInviteRoute() || '/dashboard')
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke nullstille passordet - lenken kan ha utløpt.')
  } finally {
    submitting.value = false
  }
}
</script>
