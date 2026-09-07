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
        <p>Guess every answer that fits the week's theme before you run out of strikes.</p>
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
      <p class="page-subtitle">Use Google, or an email + password if you'd rather not.</p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="loggingIn" class="login-progress">
        <span class="spinner"></span>
        <div>
          <div>Signing you in…</div>
          <div v-if="slowLogin" style="color:var(--text-dim); font-size:0.85rem; margin-top:4px;">
            Still working - this is taking longer than usual.
          </div>
        </div>
      </div>

      <template v-if="!loggingIn">
        <div v-if="loadingScript" style="color:var(--text-dim); font-size:0.9rem;">Loading sign-in…</div>

        <!-- Google's own rendered button keeps changing appearance out from under us (a
             filled_black pill on first paint, then Chrome silently swaps it for its own
             native, unstylable "Continue as ..." card once it spots an existing session -
             confirmed live, and use_fedcm_for_button:false didn't stop it either). Rather
             than keep chasing whatever Google/Chrome decides to render, this button is
             entirely ours - real markup, real CSS, will never change again - and Google's
             actual button is rendered into the invisible overlay layered on top of it, so
             every click still goes through Google's real, working auth flow underneath. -->
        <div v-show="!loadingScript && clientIdConfigured" class="google-btn-slot">
          <button type="button" class="google-fake-btn" tabindex="-1" aria-hidden="true">
            <span class="google-fake-btn-icon" aria-hidden="true">
              <svg viewBox="0 0 48 48" width="18" height="18">
                <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
                <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
                <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
                <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
              </svg>
            </span>
            <span>Continue with Google</span>
          </button>
          <div class="google-real-overlay" ref="buttonEl"></div>
        </div>

        <p v-if="!clientIdConfigured" style="color:var(--text-dim); font-size:0.85rem; margin-top:20px;">
          Google sign-in isn't configured yet - set <code>VITE_GOOGLE_CLIENT_ID</code> in the frontend's
          <code>.env</code> file.
        </p>

        <div class="landing-login-divider"><span>or</span></div>

        <!-- Email + password: a second, independent sign-in method on the same
             account table - registering or signing in either way works even if
             the other method was used first (see AuthService on the backend). -->
        <template v-if="passwordStage === 'signin'">
          <form @submit.prevent="submitPasswordLogin">
            <div class="field">
              <label>Email</label>
              <input type="email" v-model="loginEmail" autocomplete="email" placeholder="you@example.com" />
            </div>
            <div class="field">
              <label>Password</label>
              <input type="password" v-model="loginPassword" autocomplete="current-password" placeholder="Your password" />
            </div>
            <button type="submit" class="btn btn-primary" style="width:100%;" :disabled="passwordBusy">
              {{ passwordBusy ? 'Signing in…' : 'Sign in' }}
            </button>
          </form>
          <p style="text-align:center; margin-top:10px; font-size:0.9rem;">
            <a href="#" @click.prevent="passwordStage = 'forgot'">Forgot password?</a>
            &nbsp;·&nbsp;
            <a href="#" @click.prevent="switchToRegister">Create an account</a>
          </p>
        </template>

        <template v-else-if="passwordStage === 'register'">
          <form @submit.prevent="submitRegister">
            <div class="field">
              <label>Name</label>
              <input type="text" v-model="registerName" placeholder="Your name" />
            </div>
            <div class="field">
              <label>Email</label>
              <input type="email" v-model="registerEmail" autocomplete="email" placeholder="you@example.com" />
            </div>
            <div class="field">
              <label>Password</label>
              <input type="password" v-model="registerPassword" autocomplete="new-password" placeholder="At least 8 characters" />
            </div>
            <button type="submit" class="btn btn-primary" style="width:100%;" :disabled="passwordBusy">
              {{ passwordBusy ? 'Creating account…' : 'Create account' }}
            </button>
          </form>
          <p style="text-align:center; margin-top:10px; font-size:0.9rem;">
            <a href="#" @click.prevent="passwordStage = 'signin'">Already have an account? Sign in</a>
          </p>
        </template>

        <template v-else-if="passwordStage === 'forgot'">
          <template v-if="!forgotSent">
            <div class="field">
              <label>Email</label>
              <input type="email" v-model="forgotEmail" autocomplete="email" placeholder="you@example.com" />
            </div>
            <button class="btn btn-primary" style="width:100%;" :disabled="passwordBusy || !forgotEmail.trim()" @click="submitForgotPassword">
              {{ passwordBusy ? 'Sending…' : 'Send reset link' }}
            </button>
          </template>
          <p v-else class="page-subtitle" style="margin-top:0;">
            If that email has an account with a password, we've sent a reset link to it.
          </p>
          <p style="text-align:center; margin-top:10px; font-size:0.9rem;">
            <a href="#" @click.prevent="passwordStage = 'signin'">← Back to sign in</a>
          </p>
        </template>

        <div class="landing-login-divider"><span>or</span></div>
        <p style="text-align:center; font-size:0.9rem;">
          Got a room code from someone hosting a game?
          <router-link to="/join">Join as a guest</router-link> - no account needed.
        </p>
      </template>
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
    // Rendered into .google-real-overlay, which sits invisibly on top of our own
    // .google-fake-btn (see template) - whatever Google/Chrome decides this looks
    // like (plain button, personalized FedCM card, or anything they change it to
    // next) no longer matters, since the user never sees it.
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

// --- Email + password (independent of the Google flow above) ---
const passwordStage = ref('signin') // 'signin' | 'register' | 'forgot'
const passwordBusy = ref(false)

const loginEmail = ref('')
const loginPassword = ref('')

const registerName = ref('')
const registerEmail = ref('')
const registerPassword = ref('')

const forgotEmail = ref('')
const forgotSent = ref(false)

function switchToRegister() {
  registerEmail.value = loginEmail.value
  passwordStage.value = 'register'
}

async function submitPasswordLogin() {
  error.value = ''
  passwordBusy.value = true
  try {
    const result = await api.loginWithPassword(loginEmail.value.trim(), loginPassword.value)
    auth.login({ token: result.token, displayName: result.displayName, role: result.role })
    router.push('/dashboard')
  } catch (e) {
    error.value = e.response?.data?.message || 'Sign-in failed. Please try again.'
  } finally {
    passwordBusy.value = false
  }
}

async function submitRegister() {
  error.value = ''
  passwordBusy.value = true
  try {
    const result = await api.register(registerEmail.value.trim(), registerPassword.value, registerName.value.trim())
    auth.login({ token: result.token, displayName: result.displayName, role: result.role })
    router.push('/dashboard')
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not create your account. Please try again.'
  } finally {
    passwordBusy.value = false
  }
}

async function submitForgotPassword() {
  error.value = ''
  passwordBusy.value = true
  try {
    await api.requestPasswordReset(forgotEmail.value.trim())
    forgotSent.value = true
  } catch (e) {
    // The backend always responds the same way regardless of whether the email
    // exists (see AuthService#requestPasswordReset) - a genuine error here
    // means something else went wrong (network, validation), worth surfacing.
    error.value = e.response?.data?.message || 'Something went wrong - please try again.'
  } finally {
    passwordBusy.value = false
  }
}
</script>
