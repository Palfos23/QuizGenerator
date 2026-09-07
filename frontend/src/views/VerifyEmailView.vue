<template>
  <div style="max-width:420px; margin:40px auto; text-align:center;">
    <template v-if="!token">
      <h1>Verify your email</h1>
      <p class="page-subtitle">This link is missing its verification token - open the link from your email again.</p>
      <router-link to="/" class="btn btn-secondary">Back to sign in</router-link>
    </template>

    <template v-else-if="state === 'checking'">
      <h1>Verifying…</h1>
      <p class="page-subtitle">One moment.</p>
    </template>

    <template v-else-if="state === 'done'">
      <h1>You're verified!</h1>
      <p class="page-subtitle">Signing you in…</p>
    </template>

    <template v-else>
      <h1>Couldn't verify that link</h1>
      <p class="page-subtitle">{{ error }}</p>
      <router-link to="/" class="btn btn-primary">Back to sign in</router-link>
    </template>
  </div>
</template>

<script setup>
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
    error.value = e.response?.data?.message || 'This verification link is invalid or has expired - request a new one from the sign-in page.'
    state.value = 'error'
  }
})
</script>
