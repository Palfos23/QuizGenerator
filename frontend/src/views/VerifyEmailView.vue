<template>
  <div style="max-width:420px; margin:40px auto; text-align:center;">
    <template v-if="!token">
      <h1>{{ $t('Bekreft e-postadressen din') }}</h1>
      <p class="page-subtitle">{{ $t('Denne lenken mangler bekreftelseskode - åpne lenken fra e-posten din på nytt.') }}</p>
      <router-link to="/" class="btn btn-secondary">{{ $t('Tilbake til innlogging') }}</router-link>
    </template>

    <template v-else-if="state === 'checking'">
      <h1>{{ $t('Bekrefter…') }}</h1>
      <p class="page-subtitle">{{ $t('Et øyeblikk.') }}</p>
    </template>

    <template v-else-if="state === 'done'">
      <h1>{{ $t('Du er bekreftet!') }}</h1>
      <p class="page-subtitle">{{ $t('Logger deg inn…') }}</p>
    </template>

    <template v-else>
      <h1>{{ $t('Kunne ikke bekrefte lenken') }}</h1>
      <p class="page-subtitle">{{ error }}</p>
      <router-link to="/" class="btn btn-primary">{{ $t('Tilbake til innlogging') }}</router-link>
    </template>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'

const route = useRoute()
const router = useRouter()
const token = route.query.token || ''

const state = ref('checking') // 'checking' | 'done' | 'error'
const error = ref('')

onMounted(async () => {
  if (!token) return
  try {
    const result = await api.verifyEmail(token)
    auth.login({ token: result.token, displayName: result.displayName, role: result.role })
    state.value = 'done'
    router.push('/dashboard')
  } catch (e) {
    error.value = e.response?.data?.message || _t('Denne bekreftelseslenken er ugyldig eller har utløpt - be om en ny fra innloggingssiden.')
    state.value = 'error'
  }
})
</script>
