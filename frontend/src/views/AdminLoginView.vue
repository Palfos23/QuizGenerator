<template>
  <div style="max-width:380px; margin-top:60px;">
    <h1>{{ $t('Administratorinnlogging') }}</h1>
    <p class="page-subtitle">{{ $t('Egne innloggingsdata, atskilt fra vanlig Google-innlogging.') }}</p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field">
      <label>{{ $t('Brukernavn') }}<input type="text" v-model="username" @keyup.enter="submit" autocomplete="username" /></label>
    </div>
    <div class="field">
      <label>{{ $t('Passord') }}<input type="password" v-model="password" @keyup.enter="submit" autocomplete="current-password" /></label>
    </div>

    <button class="btn btn-primary" :disabled="loading" @click="submit">
      {{ loading ? $t('Logger inn…') : $t('Logg inn') }}
    </button>

    <p style="margin-top:24px;">
      <router-link to="/" style="color:var(--text-dim); font-size:0.85rem;">
        {{ $t('Ikke administrator? Gå til vanlig innlogging') }}
      </router-link>
    </p>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'
import { safeRedirectTarget } from '../router'

const route = useRoute()
const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

onMounted(() => {
  if (auth.isAdmin.value) {
    router.push(safeRedirectTarget(route, '/admin/questions'))
  }
})

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const result = await api.loginAsAdmin(username.value, password.value)
    auth.login({ token: result.token, displayName: result.displayName, role: result.role })
    router.push(safeRedirectTarget(route, '/admin/questions'))
  } catch (e) {
    error.value = e.response?.data?.message || _t('Ugyldig administratorlogin.')
  } finally {
    loading.value = false
  }
}
</script>
