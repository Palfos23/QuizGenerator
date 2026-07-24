<template>
  <div class="landing">
    <section class="landing-hero">
      <h1>Quizzes, party games, and trivia - all in one place</h1>
      <p class="page-subtitle landing-lede">
        Build a custom quiz from a shared question bank, play a themed weekly guessing
        grid, or run a pass-the-device party game with friends. Sign in with Google to get started.
      </p>
    </section>

    <section class="landing-features">
      <div class="feature-card">
        <h3>Create a quiz</h3>
        <p>Mix categories, set a difficulty, then reorder, search in specific questions, or swap out anything before you print it.</p>
      </div>
      <div class="feature-card">
        <h3>Weekly grid</h3>
        <p>Guess every athlete that fits the week's theme before you run out of strikes.</p>
      </div>
      <div class="feature-card">
        <h3>Tension</h3>
        <p>A pass-the-device party quiz - push for a high-value guess, or play it safe.</p>
      </div>
      <div class="feature-card">
        <h3>Suggest a question</h3>
        <p>Add to the shared bank yourself - admin-reviewed, and usable in your own quizzes either way.</p>
      </div>
    </section>

    <section class="landing-login">
      <h2>Sign in to get started</h2>
      <p class="page-subtitle">Your quizzes are tied to your Google account.</p>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loadingScript" style="color:var(--text-dim); font-size:0.9rem;">Loading sign-in…</div>

      <div v-if="loggingIn" class="login-progress">
        <span class="spinner"></span>
        <div>
          <div>Signing you in…</div>
          <div v-if="slowLogin" style="color:var(--text-dim); font-size:0.85rem; margin-top:4px;">
            Still working - this is taking longer than usual.
          </div>
        </div>
      </div>
      <div v-show="!loggingIn" ref="buttonEl"></div>

      <p v-if="!clientIdConfigured" style="color:var(--text-dim); font-size:0.85rem; margin-top:20px;">
        Google sign-in isn't configured yet - set <code>VITE_GOOGLE_CLIENT_ID</code> in the frontend's
        <code>.env</code> file.
      </p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'

const route = useRoute()
const router = useRouter()
const buttonEl = ref(null)
const error = ref('')
const loadingScript = ref(true)
const loggingIn = ref(false)
const slowLogin = ref(false)
const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
const clientIdConfigured = !!clientId

// The <script> tag in index.html loads Google's Identity Services library
// asynchronously - it may not have finished (or even started) by the time this
// component mounts, especially on a slow connection or a fresh incognito session
// with nothing cached. Poll briefly instead of assuming it's already there.
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
    router.push(auth.isAdmin.value ? '/admin/questions' : '/dashboard')
    return
  }
  if (route.query.sessionExpired) {
    error.value = 'Your session expired - please sign in again.'
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
      theme: 'filled_black',
      shape: 'pill',
      size: 'large',
      text: 'continue_with',
      logo_alignment: 'left',
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
  loggingIn.value = true
  slowLogin.value = false
  // Surface it if the request is taking a while, rather than leaving a bare spinner
  // with no indication of whether it's still working or has silently stalled.
  const slowTimer = setTimeout(() => { slowLogin.value = true }, 2500)

  try {
    const result = await api.loginWithGoogle(response.credential)
    auth.login({ token: result.token, displayName: result.displayName, role: result.role })
    router.push('/dashboard')
  } catch (e) {
    error.value = e.response?.data?.message || 'Sign-in failed. Please try again.'
    loggingIn.value = false
  } finally {
    clearTimeout(slowTimer)
  }
}
</script>
