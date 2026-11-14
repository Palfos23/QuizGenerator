<template>
  <div class="home">
    <section class="home-hero">
      <h1><span class="home-num">{{ $t('15 nye spørsmål hver dag').split(' ')[0] }}</span> {{ $t('15 nye spørsmål hver dag').split(' ').slice(1).join(' ') }}</h1>
      <p class="home-lede">{{ $t('Svar med egne ord, se fasit med en gang og kjemp om ukens seier.') }}</p>
      <div class="home-cta">
        <button type="button" class="btn btn-primary btn-lg" @click="goToLogin">{{ $t('Spill dagens quiz§HomeViewT') }}</button>
      </div>
      <DailyQuizPreview class="home-preview" />
    </section>

    <section id="login" class="landing-login home-login" ref="loginEl">
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

    <section class="home-more">
      <h2>{{ $t('Lag egne quizer og spill sammen') }}</h2>
      <ul class="home-more-list">
        <li>
          <span class="home-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M4 6h16M4 12h10M4 18h7" /><path d="M17 15l2 2 3-4" /></svg>
          </span>
          <div>
            <h3>{{ $t('Lag en quiz') }}</h3>
            <p>{{ $t('Velg kategorier og vanskelighetsgrad, bytt ut spørsmål og skriv ut quizen.') }}</p>
          </div>
        </li>
        <li>
          <span class="home-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /></svg>
          </span>
          <div>
            <h3>{{ $t('Ukens grid§HomeViewT') }}</h3>
            <p>{{ $t('Gjett alle svarene som passer ukens tema før du går tom for forsøk.') }}</p>
          </div>
        </li>
        <li>
          <span class="home-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="8" r="3.2" /><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6" /><circle cx="17.5" cy="9.5" r="2.5" /><path d="M16.5 14.2c2.7.2 4.5 2.3 4.5 5" /></svg>
          </span>
          <div>
            <h3>{{ $t('Selskapsspill') }}</h3>
            <p>{{ $t('Tension, Imposter, Bullseye og mer - på én skjerm, eller hver på sin egen telefon med en romkode.') }}</p>
          </div>
        </li>
        <li>
          <span class="home-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14" /></svg>
          </span>
          <div>
            <h3>{{ $t('Foreslå et spørsmål') }}</h3>
            <p>{{ $t('Legg til i den felles banken - en administrator gjennomgår det, og du kan bruke det i dine egne quizer.') }}</p>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../services/api'
import DailyQuizPreview from '../components/previews/DailyQuizPreview.vue'
import auth from '../services/auth'
import cookieConsent from '../services/cookieConsent'
import { loadScript } from '../services/loadScript'
import { safeRedirectTarget } from '../router'

const GIS_SCRIPT_URL = 'https://accounts.google.com/gsi/client'

const route = useRoute()
const router = useRouter()
const buttonEl = ref(null)
const loginEl = ref(null)

// "Spill dagens quiz" needs an account (or a guest code), so it takes you to the sign-in card and puts you on its first button
function goToLogin() {
  const el = loginEl.value
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  const first = el.querySelector('button:not([tabindex="-1"]), a.btn, input')
  if (first) setTimeout(() => first.focus({ preventScroll: true }), 350)
}
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

const googleLoaded = ref(false)

// The Google Identity Services script itself only ever loads once cookie
// consent has been accepted (see CookieConsentBanner.vue) - it can set
// cookies on Google's own domain the moment it runs. Not loaded at all in
// index.html any more; loadScript() injects it here, on demand.
async function loadGoogleButton() {
  if (googleLoaded.value) return
  loadingScript.value = true
  try {
    await loadScript(GIS_SCRIPT_URL)
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
    googleLoaded.value = true
  } catch (e) {
    error.value = _t('Kunne ikke laste Google-innlogging - sjekk tilkoblingen og prøv å laste siden på nytt.')
  } finally {
    loadingScript.value = false
  }
}

onMounted(async () => {
  if (auth.isAuthenticated.value) {
    router.push(auth.isAdmin.value ? '/admin/questions' : safeRedirectTarget(route, '/dashboard'))
    return
  }
  if (!clientIdConfigured) {
    return
  }
  if (cookieConsent.state.status === 'accepted') {
    await loadGoogleButton()
  }
})

// Covers accepting from the banner while already sitting on this page -
// loads the script immediately instead of needing a refresh.
watch(() => cookieConsent.state.status, (status) => {
  if (status === 'accepted' && clientIdConfigured) {
    loadGoogleButton()
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
/* A two-column page: the product and its one call to action on the left, the sign-in card on the right (sticky).
   On a phone the card moves under the preview, so "Spill dagens quiz" scrolls you to it. */
.home {
  max-width: 1040px;
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  grid-template-areas:
    "hero login"
    "more more";
  column-gap: var(--space-7);
  row-gap: var(--space-8);
  align-items: start;
  padding: var(--space-6) 0 var(--space-7);
}
.home-hero { grid-area: hero; min-width: 0; }
.home-login { grid-area: login; position: sticky; top: calc(var(--nav-height) + var(--space-4)); align-self: start; }
.home-more { grid-area: more; }

.home-num { color: var(--gold); }
.home-hero h1::before { content: ''; display: block; width: 56px; height: 5px; margin-bottom: var(--space-4); border-radius: 3px; background: var(--gold); }
.home-hero h1 { font-size: clamp(2.3rem, 5.4vw, 3.4rem); line-height: 1.04; letter-spacing: -0.025em; margin: 0 0 var(--space-3); max-width: 14ch; }
.home-lede { font-size: 1.15rem; line-height: 1.5; color: var(--text-dim); margin: 0 0 var(--space-5); max-width: 34ch; }
.home-cta { margin-bottom: var(--space-6); }
.home-cta .btn-lg { padding-inline: var(--space-6); min-height: 52px; font-size: 1.05rem; }
.home-preview { max-width: 560px; }

.home-more h2 { font-size: 1.5rem; margin: 0 0 var(--space-5); max-width: 24ch; }
.home-more-list { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-6) var(--space-7); }
.home-more-list li { display: flex; gap: var(--space-4); align-items: flex-start; }
.home-icon { flex-shrink: 0; width: 40px; height: 40px; display: grid; place-items: center; border-radius: var(--radius-md); background: rgb(var(--primary-rgb) / 0.09); color: var(--primary); }
.home-icon svg { width: 22px; height: 22px; }
.home-more-list h3 { margin: 0 0 var(--space-1); font-family: var(--font-body); font-size: 1.05rem; font-weight: 650; letter-spacing: 0; }
.home-more-list p { margin: 0; color: var(--text-dim); font-size: 0.95rem; line-height: 1.5; max-width: 38ch; }

@media (max-width: 900px) {
  .home { grid-template-columns: minmax(0, 1fr); grid-template-areas: "hero" "login" "more"; row-gap: var(--space-7); padding-top: var(--space-4); }
  .home-login { position: static; }
  .home-preview { max-width: none; }
}
@media (max-width: 560px) {
  .home-more-list { grid-template-columns: 1fr; gap: var(--space-5); }
  .home-cta .btn-lg { width: 100%; }
}
</style>
