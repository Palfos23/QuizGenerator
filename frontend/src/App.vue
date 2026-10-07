<template>
  <div class="app-shell">
    <nav class="top-nav">
      <router-link to="/" class="nav-brand">Quizzes</router-link>

      <template v-if="auth.isAuthenticated.value">
        <router-link v-if="!auth.isAdmin.value && !auth.isGuest.value" to="/generate" class="nav-link" @click="onNavClick('/generate', 'generate')">Create a quiz</router-link>
        <router-link v-if="!auth.isAdmin.value && !auth.isGuest.value" to="/my-quizzes" class="nav-link" @click="onNavClick('/my-quizzes', 'myQuizzes')">My quizzes</router-link>
        <router-link v-if="!auth.isAdmin.value && !auth.isGuest.value" to="/daily-quiz" class="nav-link">Daily Quiz</router-link>
        <template v-if="!auth.isAdmin.value && !auth.isGuest.value">
          <div v-for="menu in PLAYER_MENUS" :key="menu.key" class="top-nav-dropdown">
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
          </div>
        </template>
        <template v-if="auth.isAdmin.value">
          <div v-for="menu in ADMIN_MENUS" :key="menu.label" class="top-nav-dropdown">
            <button
              type="button"
              class="nav-link top-nav-dropdown-toggle"
              :class="{ 'router-link-exact-active': adminMenuActive(menu) }"
              aria-haspopup="true"
              :aria-expanded="openAdminMenu === menu.label"
              @click="toggleAdminMenu(menu.label)"
            >
              {{ menu.label }} ▾
            </button>
            <div v-if="openAdminMenu === menu.label" class="top-nav-dropdown-popup" role="menu">
              <router-link
                v-for="item in menu.items"
                :key="item.to"
                :to="item.to"
                class="nav-link"
                role="menuitem"
                @click="closeAdminMenu"
              >{{ item.label }}</router-link>
            </div>
          </div>
        </template>

        <div class="top-nav-spacer"></div>
        <router-link v-if="!auth.isAdmin.value && !auth.isGuest.value" to="/account" class="nav-link" @click="onNavClick('/account', 'account')">Account</router-link>
        <span class="top-nav-user">{{ auth.state.displayName }}<template v-if="auth.isGuest.value"> (guest)</template></span>
        <button class="btn btn-secondary btn-sm" @click="logout">{{ auth.isGuest.value ? 'Leave' : 'Log out' }}</button>
      </template>
      <template v-else>
        <div class="top-nav-spacer"></div>
      </template>
    </nav>

    <main class="main-content">
      <router-view />
    </main>

    <footer class="app-footer">
      <router-link to="/privacy">Privacy Policy</router-link>
      <router-link to="/cookies">Cookie Policy</router-link>
      <button type="button" class="app-footer-link-btn" @click="cookieConsent.reset()">Cookie settings</button>
    </footer>

    <!-- Mobile-only bottom tab bar - the top nav collapses to just the brand below 760px -->
    <nav class="bottom-nav" v-if="auth.isAuthenticated.value">
      <router-link v-if="!auth.isAdmin.value && !auth.isGuest.value" to="/generate" @click="onNavClick('/generate', 'generate')">Create</router-link>
      <router-link v-if="!auth.isAdmin.value && !auth.isGuest.value" to="/my-quizzes" @click="onNavClick('/my-quizzes', 'myQuizzes')">My quizzes</router-link>
      <router-link v-if="!auth.isAdmin.value && !auth.isGuest.value" to="/daily-quiz">Daily Quiz</router-link>
      <template v-if="!auth.isAdmin.value && !auth.isGuest.value">
        <div v-for="menu in PLAYER_MENUS" :key="menu.key" class="bottom-nav-menu" style="position:relative; flex:1; display:flex;">
          <button
            aria-haspopup="true"
            :aria-expanded="openPlayerMenu === menu.key"
            @click="togglePlayerMenu(menu.key)"
            :class="{ active: playerMenuActive(menu) }"
          >{{ menu.label }} ▾</button>
          <div v-if="openPlayerMenu === menu.key" class="games-popup" role="menu">
            <router-link
              v-for="item in menu.items"
              :key="item.to"
              :to="item.to"
              role="menuitem"
              @click="closePlayerMenuItem(item.to, item.key)"
            >{{ item.label }}</router-link>
          </div>
        </div>
      </template>
      <template v-if="auth.isAdmin.value">
        <div v-for="menu in ADMIN_MENUS" :key="menu.label" class="bottom-nav-menu" style="position:relative; flex:1; display:flex;">
          <button
            aria-haspopup="true"
            :aria-expanded="openAdminMenu === menu.label"
            :class="{ active: adminMenuActive(menu) }"
            @click="toggleAdminMenu(menu.label)"
          >{{ menu.label }} ▾</button>
          <div v-if="openAdminMenu === menu.label" class="games-popup admin-menu-popup" role="menu">
            <router-link
              v-for="item in menu.items"
              :key="item.to"
              :to="item.to"
              role="menuitem"
              @click="closeAdminMenu"
            >{{ item.label }}</router-link>
          </div>
        </div>
      </template>
      <button @click="logout">{{ auth.isGuest.value ? 'Leave' : 'Log out' }}</button>
    </nav>

    <ToastHost />
    <CookieConsentBanner />

    <div v-if="showInactivityWarning" class="modal-backdrop">
      <div class="modal" role="alertdialog" aria-modal="true" aria-label="Still there?" style="max-width:420px; text-align:center;">
        <h2 style="margin-top:0;">Still there?</h2>
        <p class="page-subtitle">You've been inactive for a while - you'll be logged out in {{ inactivityWarningSecondsLeft }}s.</p>
        <div style="display:flex; gap:10px; justify-content:center; margin-top:20px;">
          <button class="btn btn-secondary" @click="logoutNow">Log out now</button>
          <button class="btn btn-primary" @click="staySignedIn">Stay logged in</button>
        </div>
      </div>
    </div>

    <div v-else-if="showExpiryWarning" class="modal-backdrop">
      <div class="modal" role="alertdialog" aria-modal="true" aria-label="Your session will expire soon" style="max-width:420px; text-align:center;">
        <h2 style="margin-top:0;">Your session will expire soon</h2>
        <p class="page-subtitle">You'll be logged out shortly and will need to sign in again - staying active won't postpone this one. Might be a good time to wrap up.</p>
        <button class="btn btn-primary" style="margin-top:8px;" @click="dismissExpiryWarning">Got it</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import auth from './services/auth'
import api from './services/api'
import { useRouter } from 'vue-router'
import navTrigger from './services/navTrigger'
import cookieConsent from './services/cookieConsent'
import ToastHost from './components/ToastHost.vue'
import CookieConsentBanner from './components/CookieConsentBanner.vue'
import { useEscapeKey } from './composables/useEscapeKey'

const router = useRouter()

// Player dropdowns, config-driven like ADMIN_MENUS below and used for both the
// desktop top nav and the mobile bottom-nav popup. Daily Quiz is deliberately not
// in either - it has its own standalone nav button, next to Create a quiz/My quizzes.
//
// "Weekly" holds only the two weekly boards (Grid, Starting XI); "Games" holds every
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
    label: 'Weekly',
    items: [
      { to: '/weekly-grid', label: 'Grid', key: null },
      { to: '/starting-xi', label: 'Starting XI', key: null }
    ]
  },
  {
    key: 'games',
    label: 'Games',
    items: [
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
// Prefix match, not exact - Grid/Starting XI each have a /:id play sub-route
// beyond their own list page.
function playerMenuActive(menu) {
  const path = router.currentRoute.value.path
  return menu.items.some(item => path === item.to || path.startsWith(item.to + '/'))
}

// Single "which player dropdown is open" ref (null | 'weekly' | 'games') - one at a
// time, same pattern as openAdminMenu below.
const openPlayerMenu = ref(null)
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
// single `openAdminMenu` (one menu open at a time), mirroring the Games popup.
const ADMIN_MENUS = [
  {
    label: 'Questions',
    items: [
      { to: '/admin/questions', label: 'Question bank' },
      { to: '/admin/question-labels', label: 'Labels' },
      { to: '/admin/question-submissions', label: 'User submissions' },
      { to: '/admin/quiz-templates', label: 'Quiz templates' }
    ]
  },
  {
    label: 'Content',
    items: [
      { to: '/admin/athletes', label: 'Subjects' },
      { to: '/admin/athlete-pools', label: 'Pools' },
      { to: '/admin/grid-categories', label: 'Categories' },
      { to: '/admin/clubs', label: 'Clubs' },
      { to: '/admin/tension-categories', label: 'Tension categories' }
    ]
  },
  {
    label: 'Games',
    items: [
      { to: '/admin/daily-quiz-review', label: 'Daily quiz review' },
      { to: '/admin/grids', label: 'Weekly grids' },
      { to: '/admin/lineups', label: 'Starting XI' },
      { to: '/admin/tension-questions', label: 'Tension' },
      { to: '/admin/501', label: '501' },
      { to: '/admin/imposter', label: 'Imposter' },
      { to: '/admin/bullseye', label: 'Bullseye' },
      { to: '/admin/penalty-shootouts', label: 'Penalty Shootout' },
      { to: '/admin/flashback', label: 'Flashback' }
    ]
  },
  {
    label: 'Insights',
    items: [
      { to: '/admin/statistics', label: 'Statistics' },
      { to: '/admin/reports', label: 'Reports' },
      { to: '/admin/expiring-content', label: 'Can expire' },
      { to: '/admin/duplicate-subjects', label: 'Duplicate subjects' }
    ]
  }
]
const openAdminMenu = ref(null)
function toggleAdminMenu(label) {
  openAdminMenu.value = openAdminMenu.value === label ? null : label
}
function closeAdminMenu() {
  openAdminMenu.value = null
}
// Highlights the toggle whenever the current route lives under one of its items.
function adminMenuActive(menu) {
  const path = router.currentRoute.value.path
  return menu.items.some(item => path === item.to || path.startsWith(item.to + '/'))
}

// Belt-and-suspenders close for both popups on any navigation - covers the
// case where a click lands on a different nav item entirely (not one of
// this popup's own links, which already close it themselves) while a
// dropdown happens to be open.
watch(() => router.currentRoute.value.path, () => {
  closePlayerMenu()
  openAdminMenu.value = null
})

// A keyboard user can open either dropdown, but until now had no way to
// close it again without tabbing all the way through its links.
useEscapeKey(() => {
  closePlayerMenu()
  openAdminMenu.value = null
})

// Clicking anywhere outside an open dropdown closes it. (A full-screen backdrop used to do this, but
// the nav bars' backdrop-filter makes `position: fixed` children cover only the bar itself, so a
// click on the page below never reached it.) Taps on a menu's own toggle or popup are left alone -
// they handle themselves, including switching straight from one menu to another.
function closeMenusOnOutsidePress(e) {
  if (openPlayerMenu.value === null && openAdminMenu.value === null) return
  if (e.target instanceof Element && e.target.closest('.top-nav-dropdown, .bottom-nav-menu')) return
  closePlayerMenu()
  openAdminMenu.value = null
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
// handles "stayed away for a month", this handles "left a tab open and walked away". Regular players
// get 24 hours: the limit used to be 60 minutes, then 4 hours, and both were the main source of
// "logged out too often" - a slow party-game round or an unhurried quiz routinely outlasts them.
// Admins (and guests) keep the tighter 4 hours, since an admin session can change anything and a
// forgotten tab on a shared device is the case this exists for.
const USER_INACTIVITY_LIMIT_MS = 24 * 60 * 60 * 1000
const STRICT_INACTIVITY_LIMIT_MS = 4 * 60 * 60 * 1000
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
// a player's 30-day token keeps moving forward every time they come back (admins/guests have a 12h
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
    router.push('/?sessionExpired=1')
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
    router.push('/?sessionExpired=1')
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
