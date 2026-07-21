<template>
  <div style="max-width:420px; margin-top:60px;">
    <h1>Welcome</h1>
    <p class="page-subtitle">Sign in with Google to create and manage your quizzes.</p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="loadingScript" style="color:var(--text-dim); font-size:0.9rem;">
      Loading sign-in…
    </div>
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
const loadingScript = ref(true)
const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
const clientIdConfigured = !!clientId

// The <script> tag in index.html loads Google's Identity Services library
// asynchronously - it may well not have finished (or even started) by the
// time this component mounts, especially on a slow connection or a fresh
// incognito session with nothing cached. Poll briefly instead of assuming
// it's already there, so the button reliably appears without needing a
// manual refresh to win the race.
function waitForGoogleIdentity(timeoutMs = 10000) {
  return new Promise((resolve, reject) => {
    const start = Date.now()
    const check = () => {
      if (window.google?.accounts?.id) {
        resolve()
      } else if (Date.now() - start > timeoutMs) {
        reject(new Error('Google Identity Services script did not load in time'))
      } else {
        setTimeout(check, 100)
      }
    }
    check()
  })
}

onMounted(async () => {
  if (auth.isAuthenticated.value) {
    router.push('/generate')
    return
  }
  if (!clientIdConfigured) {
    loadingScript.value = false
    return
  }

  try {
    await waitForGoogleIdentity()
    window.google.accounts.id.initialize({
      client_id: clientId,
      callback: handleCredentialResponse
    })
    window.google.accounts.id.renderButton(buttonEl.value, {
      theme: 'outline',
      size: 'large',
      text: 'continue_with',
      width: 320
    })
  } catch (e) {
    error.value = 'Could not load Google Sign-In - check your connection and try refreshing.'
  } finally {
    loadingScript.value = false
  }
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
