<template>
  <Transition name="dialog">
    <div v-if="loginDialog.state.open" class="modal-backdrop" @click.self="!loggingIn && close()">
      <section ref="dialogEl" class="modal login-modal" role="dialog" aria-modal="true" :aria-label="$t('Logg inn')">
        <button type="button" class="login-close" :aria-label="$t('Lukk§LoginDialogT')" @click="close">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
        </button>
        <div v-if="error" class="banner error">{{ error }}</div>
  
          <div v-if="loggingIn" class="login-progress">
            <span class="spinner"></span>
            <div>
              <div>{{ $t('Logger deg inn…') }}</div>
              <div v-if="slowLogin" style="color:var(--text-dim); font-size:0.85rem; margin-top:4px;">
                {{ $t('Jobber fortsatt - dette tar lengre tid enn vanlig.') }}
              </div>
            </div>
          </div>
  
          <template v-if="!loggingIn">
            <div class="landing-login-head">
              <h2>{{ $t('Logg inn') }}</h2>
              <p>{{ $t('Logg inn for å lagre poengene og quizene dine.') }}</p>
            </div>
            <div v-if="loadingScript" style="color:var(--text-dim); font-size:0.9rem; text-align:center;">{{ $t('Laster innlogging…') }}</div>
  
            <!-- Google's own rendered button keeps changing appearance out from under us (a
                 filled_black pill on first paint, then Chrome silently swaps it for its own
                 native, unstylable "Continue as ..." card once it spots an existing session -
                 confirmed live, and use_fedcm_for_button:false didn't stop it either). Rather
                 than keep chasing whatever Google/Chrome decides to render, this button is
                 entirely ours - real markup, real CSS, will never change again - and Google's
                 actual button is rendered into the invisible overlay layered on top of it, so
                 every click still goes through Google's real, working auth flow underneath. -->
            <div
              v-show="clientIdConfigured && cookieConsent.state.status !== 'accepted'"
              style="text-align:center; padding:14px; border:1px solid var(--border); border-radius: var(--radius-sm); color:var(--text-dim); font-size:0.85rem;"
            >
              {{ $t('Innlogging med Google krever at den valgfrie cookie-innstillingen er slått på.') }}
              <div style="margin-top:8px;">
                <button type="button" class="btn btn-secondary btn-sm" @click="cookieConsent.accept()">{{ $t('Godta informasjonskapsler') }}</button>
              </div>
            </div>
  
            <div v-show="cookieConsent.state.status === 'accepted' && !loadingScript && clientIdConfigured" class="google-btn-slot">
              <button type="button" class="google-fake-btn" tabindex="-1" aria-hidden="true">
                <span class="google-fake-btn-icon" aria-hidden="true">
                  <svg viewBox="0 0 48 48" width="18" height="18">
                    <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
                    <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
                    <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
                    <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
                  </svg>
                </span>
                <span>{{ $t('Fortsett med Google') }}</span>
              </button>
              <div class="google-real-overlay" ref="buttonEl"></div>
            </div>
  
            <p v-if="!clientIdConfigured" style="color:var(--text-dim); font-size:0.85rem; text-align:center;">
              {{ $t('Google-innlogging er ikke satt opp ennå - sett') }} <code>VITE_GOOGLE_CLIENT_ID</code> {{ $t('i frontendens') }}
              <code>.env</code> {{ $t('-fil.') }}
            </p>
  
            <!-- Collapsed by default - only expands once someone actually wants the
                 email/password path, so the common case (Google) stays a short card
                 instead of a full form nobody asked to see. Once open it stays open
                 regardless of which of signin/register/forgot is showing. Styled as
                 a real secondary button (not a small link) so it reads as a peer
                 option to Google, not fine print. -->
            <template v-if="!emailFormOpen">
              <div class="landing-login-divider"><span>{{ $t('eller') }}</span></div>
              <button type="button" class="btn btn-secondary landing-login-option" @click="emailFormOpen = true">
                {{ $t('Fortsett med e-post') }}
              </button>
            </template>
  
            <template v-else>
              <div class="landing-login-divider"><span>{{ $t('eller') }}</span></div>
  
              <!-- Email + password: a second, independent sign-in method on the same
                   account table - registering or signing in either way works even if
                   the other method was used first (see AuthService on the backend). -->
              <template v-if="passwordStage === 'signin'">
                <form @submit.prevent="submitPasswordLogin">
                  <div class="field">
                    <label>{{ $t('E-post') }}<input type="email" v-model="loginEmail" autocomplete="email" :placeholder="$t('du@eksempel.no')" /></label>
                  </div>
                  <div class="field">
                    <label>{{ $t('Passord') }}<input type="password" v-model="loginPassword" autocomplete="current-password" :placeholder="$t('Passordet ditt')" /></label>
                  </div>
                  <button type="submit" class="btn btn-primary" style="width:100%;" :disabled="passwordBusy" :aria-busy="passwordBusy">
                    {{ passwordBusy ? $t('Logger inn…') : $t('Logg inn') }}
                  </button>
                </form>
                <p v-if="needsVerification && !resendSent" style="text-align:center; margin-top:10px; font-size:0.9rem;">
                  <a href="#" @click.prevent="submitResendVerification">{{ $t('Send bekreftelses-e-posten på nytt') }}</a>
                </p>
                <p v-else-if="resendSent" class="page-subtitle" style="text-align:center; margin-top:10px;">
                  {{ $t('Hvis den e-postadressen må bekreftes, har vi sendt en ny lenke.') }}
                </p>
                <p style="text-align:center; margin-top:10px; font-size:0.9rem;">
                  <a href="#" @click.prevent="passwordStage = 'forgot'">{{ $t('Glemt passord?') }}</a>
                  &nbsp;·&nbsp;
                  <a href="#" @click.prevent="switchToRegister">{{ $t('Opprett en konto') }}</a>
                </p>
              </template>
  
              <template v-else-if="passwordStage === 'register'">
                <template v-if="!registerSent">
                  <form @submit.prevent="submitRegister">
                    <div class="field">
                      <label>{{ $t('Navn') }}<input type="text" v-model="registerName" :placeholder="$t('Navnet ditt')" /></label>
                    </div>
                    <div class="field">
                      <label>{{ $t('E-post') }}<input type="email" v-model="registerEmail" autocomplete="email" :placeholder="$t('du@eksempel.no')" /></label>
                    </div>
                    <div class="field">
                      <label>{{ $t('Passord') }}<input type="password" v-model="registerPassword" autocomplete="new-password" :placeholder="$t('Minst 8 tegn')" /></label>
                    </div>
                    <button type="submit" class="btn btn-primary" style="width:100%;" :disabled="passwordBusy" :aria-busy="passwordBusy">
                      {{ passwordBusy ? $t('Oppretter konto…') : $t('Opprett konto') }}
                    </button>
                  </form>
                </template>
                <p v-else class="page-subtitle" style="margin-top:0;">
                  {{ $t('Nesten ferdig - sjekk e-posten din for en bekreftelseslenke (den virker i 30 minutter) før du logger inn.') }}
                </p>
                <p style="text-align:center; margin-top:10px; font-size:0.9rem;">
                  <a href="#" @click.prevent="passwordStage = 'signin'">{{ registerSent ? $t('Tilbake til innlogging') : $t('Har du allerede en konto? Logg inn') }}</a>
                </p>
              </template>
  
              <template v-else-if="passwordStage === 'forgot'">
                <template v-if="!forgotSent">
                  <div class="field">
                    <label>{{ $t('E-post') }}<input type="email" v-model="forgotEmail" autocomplete="email" :placeholder="$t('du@eksempel.no')" /></label>
                  </div>
                  <button class="btn btn-primary" style="width:100%;" :disabled="passwordBusy || !forgotEmail.trim()" @click="submitForgotPassword">
                    {{ passwordBusy ? $t('Sender…') : $t('Send lenke for å nullstille') }}
                  </button>
                </template>
                <p v-else class="page-subtitle" style="margin-top:0;">
                  {{ $t('Hvis den e-postadressen har en konto med passord, har vi sendt en lenke for å nullstille det.') }}
                </p>
                <p style="text-align:center; margin-top:10px; font-size:0.9rem;">
                  <a href="#" @click.prevent="passwordStage = 'signin'">{{ $t('← Tilbake til innlogging') }}</a>
                </p>
              </template>
            </template>
  
            <div class="landing-login-divider"><span>{{ $t('eller') }}</span></div>
            <router-link to="/join" class="btn btn-secondary landing-login-option">
              {{ $t('Bli med som gjest') }}
            </router-link>
            <p class="landing-login-guest-hint">{{ $t('Har du fått en romkode fra noen som er vert for et spill? Du trenger ingen konto.') }}</p>
          </template>
      </section>
    </div>
  </Transition>
</template>

<script setup>
import { t as _t } from '../i18n'
import { nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'
import cookieConsent from '../services/cookieConsent'
import { loadScript } from '../services/loadScript'
import { safeRedirectTarget } from '../router'
import loginDialog from '../services/loginDialog'
import { useEscapeKey } from '../composables/useEscapeKey'

const GIS_SCRIPT_URL = 'https://accounts.google.com/gsi/client'

const route = useRoute()
const router = useRouter()
const buttonEl = ref(null)
const dialogEl = ref(null)

const error = ref('')
const loadingScript = ref(false)
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

let googleReady = false

// The Google Identity Services script itself only ever loads once cookie
// consent has been accepted (see CookieConsentBanner.vue) - it can set
// cookies on Google's own domain the moment it runs. Not loaded at all in
// index.html; loadScript() injects it here, on demand, the first time the dialog is open.
// The dialog's DOM is rebuilt every time it opens, so Google's button is rendered again each time.
async function setupGoogleButton() {
  if (!clientIdConfigured || !loginDialog.state.open || cookieConsent.state.status !== 'accepted') return
  loadingScript.value = true
  try {
    if (!googleReady) {
      await loadScript(GIS_SCRIPT_URL)
      await waitForGoogleIdentity()
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: handleCredentialResponse
      })
      googleReady = true
    }
    await nextTick()
    // Rendered into .google-real-overlay, which sits invisibly on top of our own
    // .google-fake-btn (see template) - whatever Google/Chrome decides this looks
    // like (plain button, personalized FedCM card, or anything they change it to
    // next) no longer matters, since the user never sees it.
    if (buttonEl.value) {
      window.google.accounts.id.renderButton(buttonEl.value, {
        theme: 'filled_black',
        shape: 'pill',
        size: 'large',
        text: 'continue_with',
        logo_alignment: 'left',
        width: 320
      })
    }
  } catch (e) {
    error.value = _t('Kunne ikke laste Google-innlogging - sjekk tilkoblingen og prøv å laste siden på nytt.')
  } finally {
    loadingScript.value = false
  }
}

function close() { loginDialog.close() }
useEscapeKey(() => { if (loginDialog.state.open && !loggingIn.value) close() })

// Opening: put focus on the first thing you can press, and bring up Google's button
watch(() => loginDialog.state.open, async (open) => {
  if (!open) return
  error.value = ''
  setupGoogleButton()
  await nextTick()
  const first = dialogEl.value?.querySelector('input, a.btn, button.btn')
  if (first) first.focus({ preventScroll: true })
})

// Covers accepting from the banner while the dialog is already open - loads the script immediately instead of needing a refresh.
watch(() => cookieConsent.state.status, () => setupGoogleButton())

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
    loginDialog.close()
    router.push(safeRedirectTarget(route, '/dashboard'))
  } catch (e) {
    error.value = e.response?.data?.message || _t('Innloggingen mislyktes. Prøv igjen.')
    loggingIn.value = false
  } finally {
    clearTimeout(slowTimer)
  }
}

// --- Email + password (independent of the Google flow above) ---
const emailFormOpen = ref(false) // collapsed until "Sign in with email instead" is clicked - see template
const passwordStage = ref('signin') // 'signin' | 'register' | 'forgot'
const passwordBusy = ref(false)

const loginEmail = ref('')
const loginPassword = ref('')

const registerName = ref('')
const registerEmail = ref('')
const registerPassword = ref('')
const registerSent = ref(false)

const forgotEmail = ref('')
const forgotSent = ref(false)

// Set when loginWithPassword fails specifically because the account exists,
// the password's right, but the email hasn't been clicked-to-verify yet (see
// AuthService#loginWithPassword) - offers a resend right where the person's
// already stuck, instead of making them go find "forgot password" for a
// problem that isn't actually about their password.
const needsVerification = ref(false)
const resendSent = ref(false)

function switchToRegister() {
  registerEmail.value = loginEmail.value
  passwordStage.value = 'register'
}

async function submitPasswordLogin() {
  error.value = ''
  needsVerification.value = false
  resendSent.value = false
  passwordBusy.value = true
  try {
    const result = await api.loginWithPassword(loginEmail.value.trim(), loginPassword.value)
    auth.login({ token: result.token, displayName: result.displayName, role: result.role })
    loginDialog.close()
    router.push(safeRedirectTarget(route, '/dashboard'))
  } catch (e) {
    const message = e.response?.data?.message || _t('Innloggingen mislyktes. Prøv igjen.')
    error.value = message
    needsVerification.value = /bekreft e-postadressen|verify your email/i.test(message)
  } finally {
    passwordBusy.value = false
  }
}

async function submitResendVerification() {
  error.value = ''
  passwordBusy.value = true
  try {
    await api.resendVerification(loginEmail.value.trim())
    resendSent.value = true
  } catch (e) {
    error.value = e.response?.data?.message || _t('Noe gikk galt - prøv igjen.')
  } finally {
    passwordBusy.value = false
  }
}

async function submitRegister() {
  error.value = ''
  passwordBusy.value = true
  try {
    await api.register(registerEmail.value.trim(), registerPassword.value, registerName.value.trim())
    registerSent.value = true
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke opprette kontoen din. Prøv igjen.')
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
    error.value = e.response?.data?.message || _t('Noe gikk galt - prøv igjen.')
  } finally {
    passwordBusy.value = false
  }
}
</script>

<style scoped>
.login-modal { max-width: 420px; position: relative; padding: 28px 28px 24px; }
.login-close {
  position: absolute; top: 10px; right: 10px; width: 36px; height: 36px; padding: 0;
  display: grid; place-items: center; border: none; border-radius: var(--radius-md);
  background: transparent; color: var(--text-dim); cursor: pointer;
}
.login-close:hover { background: rgb(var(--fg-rgb) / 0.06); color: var(--text); }
.login-close svg { width: 18px; height: 18px; }
.login-modal .landing-login-head { margin-bottom: 16px; padding-right: 28px; }
.login-modal .landing-login-head h2 { text-align: left; }
</style>
