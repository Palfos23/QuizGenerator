<template>
  <div style="max-width:420px; margin-top:60px;">
    <h1>Welcome</h1>
    <p class="page-subtitle">Sign in with Google to create and manage your quizzes.</p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div ref="buttonEl"></div>

    <p v-if="!clientIdConfigured" style="color:var(--text-dim); font-size:0.85rem; margin-top:20px;">
      Google sign-in isn't configured yet - set <code>VITE_GOOGLE_CLIENT_ID</code> in the frontend's
      <code>.env</code> file.
    </p>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'

const router = useRouter()
const buttonEl = ref(null)
const error = ref('')
const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
const clientIdConfigured = !!clientId

onMounted(() => {
  if (auth.isAuthenticated.value) {
    router.push('/generate')
    return
  }
  if (!clientIdConfigured || !window.google) return

  window.google.accounts.id.initialize({
    client_id: clientId,
    callback: handleCredentialResponse
  })
  window.google.accounts.id.renderButton(buttonEl.value, {
    theme: 'outline',
    size: 'large',
    text: 'continue_with'
  })
})

async function handleCredentialResponse(response) {
  error.value = ''
  try {
    const result = await api.loginWithGoogle(response.credential)
    auth.login({ token: result.token, displayName: result.displayName, role: result.role })
    router.push('/generate')
  } catch (e) {
    error.value = e.response?.data?.message || 'Sign-in failed. Please try again.'
  }
}
</script>
