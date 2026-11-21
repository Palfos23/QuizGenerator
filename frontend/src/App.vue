<template>
  <div class="app-shell">
    <nav class="top-nav">
      <router-link to="/" class="nav-brand">Quizzes</router-link>

      <template v-if="auth.isAuthenticated.value">
        <!-- A spacer on each side of the links: the wordmark sits left, the account right, and the links in the middle between them -->
        <div class="top-nav-spacer"></div>
        <router-link v-if="!auth.isAdmin.value && !auth.isGuest.value" to="/generate" class="nav-link" @click="onNavClick('/generate', 'generate')">{{ $t('Lag en quiz') }}</router-link>
        <router-link v-if="!auth.isAdmin.value && !auth.isGuest.value" to="/my-quizzes" class="nav-link" @click="onNavClick('/my-quizzes', 'myQuizzes')">{{ $t('Mine quizer') }}</router-link>
        <router-link v-if="!auth.isAdmin.value && !auth.isGuest.value" to="/daily-quiz" class="nav-link">{{ $t('Dagens quiz§AppT') }}</router-link>
        <!-- The same dropdowns for players and admins: only the menus differ -->
        <template v-if="!auth.isGuest.value">
          <div v-for="menu in topMenus" :key="menu.key" class="top-nav-dropdown">
            <button
              type="button"
              class="nav-link top-nav-dropdown-toggle"
              :class="{ 'router-link-exact-active': playerMenuActive(menu) }"
              aria-haspopup="true"
              :aria-expanded="openPlayerMenu === menu.key"
              @click="togglePlayerMenu(menu.key)"
            >
              {{ menu.label }} ▾
            </button>
            <Transition name="pop" @enter="popFromTrigger" @leave="popFromTrigger">
            <div v-if="openPlayerMenu === menu.key" class="top-nav-dropdown-popup" role="menu">
              <router-link
                v-for="item in menu.items"
                :key="item.to"
                :to="item.to"
                class="nav-link"
                role="menuitem"
                @click="closePlayerMenuItem(item.to, item.key)"
              >{{ item.label }}</router-link>
            </div>
            </Transition>
          </div>
        </template>

        <div class="top-nav-spacer"></div>
        <!-- Your initial instead of "Konto" plus the name: it opens the account page (the full name is its tooltip) -->
        <router-link
          v-if="!auth.isAdmin.value && !auth.isGuest.value"
          to="/account"
          class="user-avatar"
          :aria-label="$t('Konto')"
          :title="auth.state.displayName"
          @click="onNavClick('/account', 'account')"
        >{{ userInitial }}</router-link>
        <span v-else class="user-avatar user-avatar--static" :title="auth.state.displayName">{{ userInitial }}</span>
      </template>
      <template v-else>
        <div class="top-nav-spacer"></div>
        <button type="button" class="btn btn-primary btn-sm nav-login" @click="loginDialog.open()">{{ $t('Logg inn') }}</button>
      </template>
      <LanguageToggle />
      <button v-if="auth.isAuthenticated.value" type="button" class="btn btn-secondary btn-sm nav-logout" @click="logout">{{ auth.isGuest.value ? $t('Forlat') : $t('Logg ut') }}</button>
    </nav>

    <main class="main-content">
      <router-view />
    </main>

    <footer class="app-footer">
      <router-link to="/privacy">{{ $t('Personvernerklæring') }}</router-link>
      <router-link to="/cookies">{{ $t('Retningslinjer for informasjonskapsler') }}</router-link>
      <button type="button" class="app-footer-link-btn" @click="cookieConsent.reset()">{{ $t('Cookie-innstillinger§AppT') }}</button>
      <!-- Phones only: the language switch lives here (and in the Account sheet) instead of crowding the top bar -->
      <LanguageToggle class="lang-toggle--footer" />
    </footer>

    <!-- Phones only: dims the page behind an open bottom-bar sheet; pressing it closes the sheet (closeMenusOnOutsidePress) -->
    <Transition name="dialog">
      <div v-if="auth.isAuthenticated.value && openPlayerMenu" class="nav-sheet-backdrop" aria-hidden="true"></div>
    </Transition>

    <!-- Mobile-only bottom tab bar - the top nav collapses to just the brand below 760px -->
    <nav class="bottom-nav" v-if="auth.isAuthenticated.value">
      <!-- Every tab is an icon over a short label, a full thumb tall; the menus open as a sheet across the screen -->
      <router-link v-if="!auth.isAdmin.value && !auth.isGuest.value" to="/daily-quiz" class="bottom-nav-tab" :class="{ active: dailyQuizActive }">
        <AppIcon name="calendar" /><span>{{ $t('Dagens quiz§AppT') }}</span>
      </router-link>
      <template v-if="!auth.isGuest.value">
        <div v-for="menu in bottomMenus" :key="menu.key" class="bottom-nav-menu" style="position:relative; flex:1; display:flex;">
          <button
            type="button"
            class="bottom-nav-tab"
            aria-haspopup="true"
            :aria-expanded="openPlayerMenu === menu.key"
            @click="togglePlayerMenu(menu.key)"
            :class="{ active: playerMenuActive(menu) || openPlayerMenu === menu.key }"
          ><AppIcon :name="menu.icon" /><span>{{ menu.label }}</span></button>
          <Transition name="pop" @enter="popFromTrigger" @leave="popFromTrigger">
          <div v-if="openPlayerMenu === menu.key" class="games-popup nav-sheet" :class="{ 'nav-sheet--grid': menu.items.length > 4 }" role="menu" :aria-label="menu.label">
            <div class="nav-sheet-title">{{ menu.label }}</div>
            <div v-if="menu.key === 'account'" class="nav-sheet-row">
              <span>{{ $t('Språk') }}</span>
              <LanguageToggle />
            </div>
            <template v-for="item in menu.items" :key="item.to || item.action">
              <button v-if="item.action === 'logout'" type="button" role="menuitem" class="popup-action" @click="closePlayerMenu(); logout()">{{ item.label }}</button>
              <router-link
                v-else
                :to="item.to"
                role="menuitem"
                :class="{ 'popup-primary': item.primary }"
                @click="closePlayerMenuItem(item.to, item.key)"
              >{{ item.label }}</router-link>
            </template>
          </div>
          </Transition>
        </div>
      </template>
      <button v-if="auth.isGuest.value" @click="logout">{{ auth.isGuest.value ? $t('Forlat') : $t('Logg ut') }}</button>
    </nav>

    <LoginDialog />
    <ToastHost />
    <CookieConsentBanner />

    <div v-if="showInactivityWarning" class="modal-backdrop">
      <div class="modal" role="alertdialog" aria-modal="true" :aria-label="$t('Fortsatt der?')" style="max-width:420px; text-align:center;">
        <h2 style="margin-top:0;">{{ $t('Fortsatt der?') }}</h2>
        <p class="page-subtitle">{{ $t('Du har vært inaktiv en stund - du blir logget ut om') }} {{ inactivityWarningSecondsLeft }}s.</p>
        <div style="display:flex; gap:10px; justify-content:center; margin-top:20px;">
          <button class="btn btn-secondary" @click="logoutNow">{{ $t('Logg ut nå') }}</button>
          <button class="btn btn-primary" @click="staySignedIn">{{ $t('Forbli innlogget') }}</button>
        </div>
      </div>
    </div>

    <div v-else-if="showExpiryWarning" class="modal-backdrop">
      <div class="modal" role="alertdialog" aria-modal="true" :aria-label="$t('Økten din utløper snart')" style="max-width:420px; text-align:center;">
        <h2 style="margin-top:0;">{{ $t('Økten din utløper snart') }}</h2>
        <p class="page-subtitle">{{ $t('Du blir snart logget ut og må logge inn på nytt - å være aktiv utsetter ikke dette. Det kan være lurt å runde av nå.') }}</p>
        <button class="btn btn-primary" style="margin-top:8px;" @click="dismissExpiryWarning">{{ $t('Greit') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { t as _t } from './i18n'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import auth from './services/auth'
import api from './services/api'
import { useRouter } from 'vue-router'
import navTrigger from './services/navTrigger'
import cookieConsent from './services/cookieConsent'
import ToastHost from './components/ToastHost.vue'
import LanguageToggle from './components/LanguageToggle.vue'
import AppIcon from './components/AppIcon.vue'
import LoginDialog from './components/LoginDialog.vue'
import loginDialog from './services/loginDialog'
import { popFromTrigger } from './composables/popFromTrigger'
import CookieConsentBanner from './components/CookieConsentBanner.vue'
import { useEscapeKey } from './composables/useEscapeKey'

const router = useRouter()

// Player dropdowns, config-driven like ADMIN_MENUS and used for both the
// desktop top nav and the mobile bottom-nav popup. Daily Quiz is deliberately not
// in either - it has its own standalone nav button, next to Create a quiz/My quizzes.
//
// 'Ukentlig' holds only the two weekly boards (Grid, Starting XI); 'Spill' holds every
// party game and its online Battle variants. `key` on an item is only set for the
// ones that use onNavClick's "re-click while already there" retrigger (see
// closePlayerMenuItem); Grid/Starting XI never needed that, so theirs stay null.
// /penalty-shootout deliberately not listed - it's not a navbar destination of its
// own, reached only via the XI Battle page's mode choice instead (see
// StartingXiBattleView.vue), so there's no menu item that should read as active
// while on it.
const PLAYER_MENUS = [
  {
    key: 'weekly',
    icon: 'grid',
    label: _t('Ukentlig'),
    items: [
      { to: '/weekly-grid', label: 'Grid', key: null },
      { to: '/starting-xi', label: 'Starting XI', key: null }
    ]
  },
  {
    key: 'games',
    icon: 'dice',
    label: _t('Spill§AppJ'),
    items: [
      { to: '/julekalender', label: _t('Julekalender'), key: null },
      { to: '/tension', label: 'Tension', key: 'tension' },
      { to: '/501', label: '501', key: 'fiveOhOne' },
      { to: '/imposter', label: 'Imposter', key: 'imposter' },
      { to: '/grid-battle', label: 'Grid Battle', key: 'gridBattle' },
      { to: '/starting-xi-battle', label: 'XI Battle', key: 'startingXiBattle' },
      { to: '/bullseye', label: 'Bullseye', key: 'bullseye' },
      { to: '/flashback', label: 'Flashback', key: 'flashback' }
    ]
  }
]
// Mobile bottom bar only: 'Lag en quiz' and 'Mine quizer' share one dropdown there (six tabs side by
// side were cramped), with Create first and highlighted since it's the main action. On desktop the top
// nav has room, so they stay as two separate links. Opened/closed through the same openPlayerMenu as the
// others ('quizzes' is one more key).
const QUIZZES_MENU = {
  key: 'quizzes',
  icon: 'layers',
  label: _t('Quizer'),
  items: [
    { to: '/generate', label: _t('Lag en quiz'), key: 'generate', primary: true },
    { to: '/my-quizzes', label: _t('Mine quizer'), key: 'myQuizzes' }
  ]
}

// Mobile bottom bar only: the account page (where the display name is changed) had no way in from a phone -
// the top bar's 'Konto' link doesn't exist there - so the last tab is now an Account menu holding it
// together with Log out.
const ADMIN_ACCOUNT_MENU = {
  key: 'account',
  icon: 'user',
  label: _t('Konto'),
  items: [{ action: 'logout', label: _t('Logg ut') }]
}
const ACCOUNT_MENU = {
  key: 'account',
  icon: 'user',
  label: _t('Konto'),
  items: [
    { to: '/account', label: _t('Min konto og navn'), key: 'account' },
    { action: 'logout', label: _t('Logg ut') }
  ]
}

// Prefix match, not exact - Grid/Starting XI each have a /:id play sub-route
// beyond their own list page.
function playerMenuActive(menu) {
  const path = router.currentRoute.value.path
  return menu.items.some(item => [item.to, ...(item.also || [])].some(p => path === p || path.startsWith(p + '/')))
}

// Players and admins get the same navigation, just different menus
const topMenus = computed(() => auth.isAdmin.value ? ADMIN_MENUS : PLAYER_MENUS)
const bottomMenus = computed(() => auth.isAdmin.value ? [...ADMIN_MENUS, ADMIN_ACCOUNT_MENU] : [QUIZZES_MENU, ...PLAYER_MENUS, ACCOUNT_MENU])
// The Dagens quiz tab stays lit on a quiz's own page too (/daily-quiz/:id), not only on the list.
const dailyQuizActive = computed(() => router.currentRoute.value.path.startsWith('/daily-quiz'))

// Single "which player dropdown is open" ref (null | 'weekly' | 'games') - one at a
// time, one menu open at a time.
const openPlayerMenu = ref(null)
const userInitial = computed(() => (auth.state.displayName || '?').trim().charAt(0).toUpperCase() || '?')
function togglePlayerMenu(key) {
  openPlayerMenu.value = openPlayerMenu.value === key ? null : key
}
function closePlayerMenu() {
  openPlayerMenu.value = null
}

function closePlayerMenuItem(path, key) {
  closePlayerMenu()
  if (key) onNavClick(path, key)
}

// The admin nav used to be ~14 flat links crammed side by side (unusable as a
// mobile bottom bar). It's now four labelled dropdowns, driven by this config
// on both desktop (top-nav) and mobile (bottom-nav popup) - same markup, same
// single `openPlayerMenu` (one menu open at a time), the same as the player menus.
const ADMIN_MENUS = [
  {
    key: 'admin-questions',
    icon: 'cards',
    label: _t('Spørsmål§AppJ'),
    items: [
      { to: '/admin/questions', label: _t('Spørsmålsbank') },
      { to: '/admin/question-labels', label: _t('Etiketter') },
      { to: '/admin/question-submissions', label: _t('Innsendte spørsmål§AppJ') },
      { to: '/admin/quiz-templates', label: _t('Quizmaler') }
    ]
  },
  {
    key: 'admin-content',
    icon: 'layers',
    label: _t('Innhold'),
    items: [
      { to: '/admin/athletes', label: _t('Emner') },
      { to: '/admin/athlete-pools', label: _t('Emneutvalg') },
      { to: '/admin/grid-categories', label: _t('Kategorier') },
      { to: '/admin/clubs', label: _t('Klubber') },
      { to: '/admin/tension-categories', label: _t('Tension-kategorier') }
    ]
  },
  {
    key: 'admin-games',
    icon: 'dice',
    label: _t('Spill§AppJ'),
    items: [
      // One entry for correcting the daily quiz: the per-player review is a link inside that page ("also" keeps this menu lit there)
      { to: '/admin/daily-quiz-answers', label: _t('Rett svar per svar'), also: ['/admin/daily-quiz-review'] },
      { to: '/admin/daily-quiz-preview', label: _t('Morgendagens quiz') },
      { to: '/admin/advent', label: _t('Julekalender') },
      { to: '/admin/grids', label: _t('Ukentlige grids') },
      { to: '/admin/lineups', label: 'Starting XI' },
      { to: '/admin/tension-questions', label: 'Tension' },
      { to: '/admin/501', label: '501' },
      { to: '/admin/imposter', label: 'Imposter' },
      { to: '/admin/bullseye', label: 'Bullseye' },
      { to: '/admin/penalty-shootouts', label: _t('Straffekonkurranse') },
      { to: '/admin/flashback', label: 'Flashback' }
    ]
  },
  {
    key: 'admin-insight',
    icon: 'target',
    label: _t('Innsikt'),
    items: [
      { to: '/admin/statistics', label: _t('Statistikk') },
      { to: '/admin/reports', label: _t('Rapporter') },
      { to: '/admin/expiring-content', label: _t('Kan utløpe') },
      { to: '/admin/duplicate-subjects', label: _t('Dupliserte emner') },
      { to: '/admin/translations', label: _t('Oversettelser') }
    ]
  }
]
// Belt-and-suspenders close for both popups on any navigation - covers the
// case where a click lands on a different nav item entirely (not one of
// this popup's own links, which already close it themselves) while a
// dropdown happens to be open.
watch(() => router.currentRoute.value.path, () => {
  closePlayerMenu()
})

// A keyboard user can open either dropdown, but until now had no way to
// close it again without tabbing all the way through its links.
useEscapeKey(() => {
  closePlayerMenu()
})

// Clicking anywhere outside an open dropdown closes it. (A full-screen backdrop used to do this, but
// the nav bars' backdrop-filter makes `position: fixed` children cover only the bar itself, so a
// click on the page below never reached it.) Taps on a menu's own toggle or popup are left alone -
// they handle themselves, including switching straight from one menu to another.
function closeMenusOnOutsidePress(e) {
  if (openPlayerMenu.value === null) return
  if (e.target instanceof Element && e.target.closest('.top-nav-dropdown, .bottom-nav-menu')) return
  closePlayerMenu()
}

function onNavClick(path, key) {
  if (router.currentRoute.value.path === path) {
    navTrigger.fire(key)
  }
}

function logout() {
  auth.logout()
  router.push('/')
}

// Logs out after a period of no interaction, separate from the JWT's own expiry - the JWT expiring
// handles "stayed away for months", this handles "left a tab open and walked away". Regular players
// get 7 days: the limit used to be 60 minutes, then 4 hours, then 24 hours, and each was a source of
// "logged out too often" - a slow party-game round or an unhurried quiz routinely outlasts them.
// Admins (and guests) get a tighter 12 hours, since an admin session can change anything and a
// forgotten tab on a shared device is the case this exists for.
const USER_INACTIVITY_LIMIT_MS = 7 * 24 * 60 * 60 * 1000
const STRICT_INACTIVITY_LIMIT_MS = 12 * 60 * 60 * 1000
function inactivityLimitMs() {
  return auth.state.role === 'USER' ? USER_INACTIVITY_LIMIT_MS : STRICT_INACTIVITY_LIMIT_MS
}
// How long before either cutoff to show a heads-up, so a hard logout never
// just appears out of nowhere mid-game.
const INACTIVITY_WARNING_MS = 60 * 1000
const EXPIRY_WARNING_MS = 2 * 60 * 1000
// The other half of the "logged out too often" fix: silently swap in a fresh
// token (see attemptSilentRefresh below) once this little runway remains on
// the current one, rather than just watching the clock run out. 30 minutes
// gives an active tab several retries if the first attempt hits a network
// blip, well before EXPIRY_WARNING_MS would ever need to show.
const REFRESH_BEFORE_EXPIRY_MS = 30 * 60 * 1000
// Sliding session: while the app is open, swap in a fresh token once the current one is this old, so
// a player's 90-day token keeps moving forward every time they come back (admins/guests have a 24h
// token and get renewed at the same age, leaving them plenty of runway).
const REFRESH_AFTER_AGE_MS = 6 * 60 * 60 * 1000
let lastActivity = Date.now()
let inactivityTimer = null
let refreshInFlight = false

const showInactivityWarning = ref(false)
const inactivityWarningSecondsLeft = ref(0)
const showExpiryWarning = ref(false)
let expiryWarningDismissed = false

function resetActivity() {
  lastActivity = Date.now()
  showInactivityWarning.value = false
}

function staySignedIn() {
  resetActivity()
}

function logoutNow() {
  showInactivityWarning.value = false
  logout()
}

function dismissExpiryWarning() {
  expiryWarningDismissed = true
  showExpiryWarning.value = false
}

// Escape mirrors whichever action is the non-destructive one for the modal
// currently showing - staying signed in, or just acknowledging the heads-up -
// never the "log out now" option.
useEscapeKey(() => {
  if (showInactivityWarning.value) {
    staySignedIn()
  } else if (showExpiryWarning.value) {
    dismissExpiryWarning()
  }
})

// A new login (including a re-login after a session expired) gets its own
// fresh token - resets both the dismissal flag and the countdown state so a
// warning tied to the previous token can't linger onto the new one.
watch(() => auth.state.token, () => {
  expiryWarningDismissed = false
  showExpiryWarning.value = false
  showInactivityWarning.value = false
})

function checkSessionTimers() {
  if (!auth.isAuthenticated.value) return

  const idleRemaining = inactivityLimitMs() - (Date.now() - lastActivity)
  if (idleRemaining <= 0) {
    showInactivityWarning.value = false
    auth.logout()
    router.push('/')
    return
  }
  showInactivityWarning.value = idleRemaining <= INACTIVITY_WARNING_MS
  if (showInactivityWarning.value) {
    inactivityWarningSecondsLeft.value = Math.ceil(idleRemaining / 1000)
    return // don't stack the token-expiry warning on top of this one
  }

  // The token's own absolute expiry - silently swap in a fresh one well ahead
  // of it while the tab's still open and activity hasn't tripped the
  // inactivity cutoff above, so this normally never gets anywhere near
  // EXPIRY_WARNING_MS. That warning stays as the fallback for whenever the
  // swap itself can't happen (offline, server hiccup, ...).
  const tokenRemaining = auth.msUntilTokenExpiry()
  const tokenAge = auth.msSinceTokenIssued()
  const dueByAge = tokenAge !== null && tokenAge >= REFRESH_AFTER_AGE_MS
  const dueByExpiry = tokenRemaining !== null && tokenRemaining <= REFRESH_BEFORE_EXPIRY_MS
  if (tokenRemaining !== null && tokenRemaining > 0 && (dueByAge || dueByExpiry) && !refreshInFlight && Date.now() - lastRefreshAttempt > 5 * 60 * 1000) {
    attemptSilentRefresh()
  }
  if (!expiryWarningDismissed && tokenRemaining !== null && tokenRemaining > 0 && tokenRemaining <= EXPIRY_WARNING_MS) {
    showExpiryWarning.value = true
  }
}

let lastRefreshAttempt = 0
async function attemptSilentRefresh() {
  refreshInFlight = true
  lastRefreshAttempt = Date.now() // a failed attempt isn't retried every second - wait a few minutes
  try {
    const result = await api.refreshToken()
    auth.updateToken(result)
    // A previously-dismissed/shown warning was tied to the token that just
    // got replaced - clear it so a fresh one only shows if the new token
    // itself somehow ends up close to expiry too.
    showExpiryWarning.value = false
    expiryWarningDismissed = false
  } catch (e) {
    // Couldn't refresh (offline, the token already invalid, a server hiccup) -
    // the expiry warning above and api.js's response-interceptor logout are
    // both still there as fallbacks, so this fails quietly rather than
    // interrupting whatever the user's doing right now.
  } finally {
    refreshInFlight = false
  }
}

const activityEvents = ['mousemove', 'keydown', 'click', 'touchstart', 'scroll']

// On mobile, or an installed/standalone Chrome or Safari "app", the tab can sit
// backgrounded for arbitrarily long stretches - background timers get
// throttled or suspended entirely to save battery, so the 1-second interval
// below isn't reliable while backgrounded. Worse, the very act of returning
// and clicking something resets `lastActivity` before checkSessionTimers() ever
// gets a chance to notice the gap - so by the time a request goes out on the
// stale token, nothing about the UI looks wrong yet. Checking the token's own
// expiry the instant the tab becomes visible again catches this proactively,
// before that first click can mask it.
function checkTokenOnResume() {
  if (document.visibilityState === 'visible' && auth.isAuthenticated.value && auth.isTokenExpired()) {
    auth.logout()
    router.push('/')
  }
}

onMounted(() => {
  activityEvents.forEach(evt => window.addEventListener(evt, resetActivity, { passive: true }))
  inactivityTimer = setInterval(checkSessionTimers, 1000)
  document.addEventListener('visibilitychange', checkTokenOnResume)
  document.addEventListener('pointerdown', closeMenusOnOutsidePress)
  checkTokenOnResume()
})

onUnmounted(() => {
  activityEvents.forEach(evt => window.removeEventListener(evt, resetActivity))
  clearInterval(inactivityTimer)
  document.removeEventListener('visibilitychange', checkTokenOnResume)
  document.removeEventListener('pointerdown', closeMenusOnOutsidePress)
})
</script>
