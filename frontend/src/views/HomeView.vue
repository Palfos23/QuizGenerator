<template>
  <div class="landing">
    <section class="landing-hero">
      <h1>Quizzes, party games, and trivia - all in one place</h1>
      <p class="page-subtitle landing-lede">
        Build a custom quiz from a shared question bank, play a themed weekly guessing
        grid, battle friends head-to-head, or run a pass-the-device party game together.
        Sign in with Google to get started.
      </p>
    </section>

    <!-- Every card is a real route, not just descriptive text - clicking one while
         signed out bounces through the login guard (which remembers it via
         ?redirect=, see router/index.js and the watcher below) and lands there
         the moment sign-in succeeds, instead of just dumping the visitor on
         the generic dashboard. -->
    <section class="landing-features">
      <router-link
        v-for="f in features"
        :key="f.to"
        :to="f.to"
        class="feature-card"
      >
        <h3>{{ f.title }}</h3>
        <p>{{ f.description }}</p>
      </router-link>
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
      <!-- Google's own rendered button keeps changing appearance out from under us (a
           filled_black pill on first paint, then Chrome silently swaps it for its own
           native, unstylable "Continue as ..." card once it spots an existing session -
           confirmed live, and use_fedcm_for_button:false didn't stop it either). Rather
           than keep chasing whatever Google/Chrome decides to render, this button is
           entirely ours - real markup, real CSS, will never change again - and Google's
           actual button is rendered into the invisible overlay layered on top of it, so
           every click still goes through Google's real, working auth flow underneath. -->
      <div v-show="!loggingIn && !loadingScript && clientIdConfigured" class="google-btn-slot">
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
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'
import { safeRedirectTarget } from '../router'

const route = useRoute()
const router = useRouter()
const buttonEl = ref(null)
const error = ref('')
const loadingScript = ref(true)
const loggingIn = ref(false)
const slowLogin = ref(false)
const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
const clientIdConfigured = !!clientId

// The full current game roster, kept here rather than hand-copied per card in
// the template - this list drifted badly out of date before (missing Starting
// XI, 501, Imposter, Grid Battle, XI Battle and Bullseye entirely, added well
// after these four were first written), and a plain array is much harder to
// forget to update than markup is.
const features = [
  { to: '/generate', title: 'Create a quiz', description: 'Mix categories, set a difficulty, then reorder, search in specific questions, or swap out anything before you print it.' },
  { to: '/weekly-grid', title: 'Weekly grid', description: "Guess every answer that fits the week's theme before you run out of strikes." },
  { to: '/starting-xi', title: 'Starting XI', description: "Guess a full lineup, position by position, before the week's board runs out of lives." },
  { to: '/tension', title: 'Tension', description: 'A pass-the-device party quiz - push for a high-value guess, or play it safe.' },
  { to: '/501', title: '501', description: 'A darts-style countdown from 501 - checkout between 0 and -10 to win.' },
  { to: '/imposter', title: 'Imposter', description: "One player doesn't get the answer - find out who by asking around the table." },
  { to: '/grid-battle', title: 'Grid Battle', description: 'A pass-the-device or online multiplayer version of Weekly grid - take turns, or lose a life trying.' },
  { to: '/starting-xi-battle', title: 'XI Battle', description: 'Same idea as Grid Battle, for a Starting XI board - take turns naming the lineup.' },
  { to: '/bullseye', title: 'Bullseye', description: 'Everyone answers, lowest score is eliminated each round, until one player is left.' },
  { to: '/penalty-shootout', title: 'Penalty Shootout', description: 'Guess who took each penalty, in real order - solo, or pass the device around and take turns.' },
  { to: '/suggest-question', title: 'Suggest a question', description: 'Add to the shared bank yourself - admin-reviewed, and usable in your own quizzes either way.' }
]

// Every card above is a real route, so clicking one while signed out is
// already captured by the login guard as ?redirect= (see router/index.js) -
// but since that guard bounces back to this same page, nothing would
// otherwise look like it happened without this. Bring the actual next step
// (the sign-in button below) into view instead of leaving the click looking
// dead. Reacts to the query itself rather than a @click on the cards - tried
// that first, but RouterLink handles navigation via its own internal
// listener, and a plain @click alongside it never actually fired in testing
// (confirmed: the navigation itself worked, the handler didn't run). Covers
// two cases: the query changing while already sitting on this page (a card
// click - see the watch below) and landing here already carrying ?redirect=
// on first mount (a bookmarked/shared deep link the guard bounced from a
// route this browser was never on - see the call in onMounted below).
function scrollToLoginIfRedirected() {
  if (route.query.redirect && !auth.isAuthenticated.value) {
    requestAnimationFrame(() => {
      document.querySelector('.landing-login')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }
}
watch(() => route.query.redirect, scrollToLoginIfRedirected)

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
    router.push(auth.isAdmin.value ? '/admin/questions' : safeRedirectTarget(route, '/dashboard'))
    return
  }
  if (route.query.sessionExpired) {
    error.value = 'Your session expired - please sign in again.'
  }
  scrollToLoginIfRedirected()
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
    router.push(safeRedirectTarget(route, '/dashboard'))
  } catch (e) {
    error.value = e.response?.data?.message || 'Sign-in failed. Please try again.'
    loggingIn.value = false
  } finally {
    clearTimeout(slowTimer)
  }
}
</script>
