<template>
  <div class="app-shell">
    <nav class="top-nav">
      <router-link to="/" class="nav-brand">Quizzes</router-link>

      <template v-if="auth.isAuthenticated.value">
        <router-link v-if="!auth.isAdmin.value" to="/generate" class="nav-link" @click="onNavClick('/generate', 'generate')">Create a quiz</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/my-quizzes" class="nav-link" @click="onNavClick('/my-quizzes', 'myQuizzes')">My quizzes</router-link>
        <div v-if="!auth.isAdmin.value" class="top-nav-dropdown">
          <div v-if="openPlayerMenu === 'weekly'" class="top-nav-dropdown-backdrop" @click="closePlayerMenu"></div>
          <button
            type="button"
            class="nav-link top-nav-dropdown-toggle"
            :class="{ 'router-link-exact-active': isWeeklyQuizRoute }"
            aria-haspopup="true"
            :aria-expanded="openPlayerMenu === 'weekly'"
            @click="togglePlayerMenu('weekly')"
          >
            Weekly quiz ▾
          </button>
          <div v-if="openPlayerMenu === 'weekly'" class="top-nav-dropdown-popup" role="menu">
            <router-link to="/weekly-grid" class="nav-link" role="menuitem" @click="closePlayerMenu">Grid</router-link>
            <router-link to="/starting-xi" class="nav-link" role="menuitem" @click="closePlayerMenu">Starting XI</router-link>
          </div>
        </div>
        <router-link v-if="!auth.isAdmin.value" to="/tension" class="nav-link" @click="onNavClick('/tension', 'tension')">Tension</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/501" class="nav-link" @click="onNavClick('/501', 'fiveOhOne')">501</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/imposter" class="nav-link" @click="onNavClick('/imposter', 'imposter')">Imposter</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/grid-battle" class="nav-link" @click="onNavClick('/grid-battle', 'gridBattle')">Grid Battle</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/starting-xi-battle" class="nav-link" @click="onNavClick('/starting-xi-battle', 'startingXiBattle')">XI Battle</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/bullseye" class="nav-link" @click="onNavClick('/bullseye', 'bullseye')">Bullseye</router-link>
        <template v-if="auth.isAdmin.value">
          <div v-for="menu in ADMIN_MENUS" :key="menu.label" class="top-nav-dropdown">
            <div v-if="openAdminMenu === menu.label" class="top-nav-dropdown-backdrop" @click="closeAdminMenu"></div>
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
        <span class="top-nav-user">{{ auth.state.displayName }}</span>
        <button class="btn btn-secondary btn-sm" @click="logout">Log out</button>
      </template>
      <template v-else>
        <div class="top-nav-spacer"></div>
      </template>
    </nav>

    <div class="main-content">
      <router-view />
    </div>

    <!-- Mobile-only bottom tab bar - the top nav collapses to just the brand below 760px -->
    <nav class="bottom-nav" v-if="auth.isAuthenticated.value">
      <router-link v-if="!auth.isAdmin.value" to="/generate" @click="onNavClick('/generate', 'generate')">Create</router-link>
      <router-link v-if="!auth.isAdmin.value" to="/my-quizzes" @click="onNavClick('/my-quizzes', 'myQuizzes')">My quizzes</router-link>
      <div v-if="!auth.isAdmin.value" style="position:relative; flex:1; display:flex;">
        <div v-if="openPlayerMenu === 'weekly'" class="bottom-nav-backdrop" @click="closePlayerMenu"></div>
        <button
          aria-haspopup="true"
          :aria-expanded="openPlayerMenu === 'weekly'"
          @click="togglePlayerMenu('weekly')"
          :class="{ active: isWeeklyQuizRoute }"
        >Weekly quiz ▾</button>
        <div v-if="openPlayerMenu === 'weekly'" class="games-popup" role="menu">
          <router-link to="/weekly-grid" role="menuitem" @click="closePlayerMenu">Grid</router-link>
          <router-link to="/starting-xi" role="menuitem" @click="closePlayerMenu">Starting XI</router-link>
        </div>
      </div>
      <div v-if="!auth.isAdmin.value" style="position:relative; flex:1; display:flex;">
        <div v-if="openPlayerMenu === 'games'" class="bottom-nav-backdrop" @click="closePlayerMenu"></div>
        <button
          aria-haspopup="true"
          :aria-expanded="openPlayerMenu === 'games'"
          @click="togglePlayerMenu('games')"
          :class="{ active: isGameRoute }"
        >Games ▾</button>
        <div v-if="openPlayerMenu === 'games'" class="games-popup" role="menu">
          <router-link to="/tension" role="menuitem" @click="closeGamesMenu('/tension', 'tension')">Tension</router-link>
          <router-link to="/501" role="menuitem" @click="closeGamesMenu('/501', 'fiveOhOne')">501</router-link>
          <router-link to="/imposter" role="menuitem" @click="closeGamesMenu('/imposter', 'imposter')">Imposter</router-link>
          <router-link to="/grid-battle" role="menuitem" @click="closeGamesMenu('/grid-battle', 'gridBattle')">Grid Battle</router-link>
          <router-link to="/starting-xi-battle" role="menuitem" @click="closeGamesMenu('/starting-xi-battle', 'startingXiBattle')">XI Battle</router-link>
          <router-link to="/bullseye" role="menuitem" @click="closeGamesMenu('/bullseye', 'bullseye')">Bullseye</router-link>
        </div>
      </div>
      <template v-if="auth.isAdmin.value">
        <div v-for="menu in ADMIN_MENUS" :key="menu.label" style="position:relative; flex:1; display:flex;">
          <div v-if="openAdminMenu === menu.label" class="bottom-nav-backdrop" @click="closeAdminMenu"></div>
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
      <button @click="logout">Log out</button>
    </nav>

    <ToastHost />

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
import ToastHost from './components/ToastHost.vue'
import { useEscapeKey } from './composables/useEscapeKey'

const router = useRouter()

const GAME_PATHS = ['/tension', '/501', '/imposter', '/grid-battle', '/starting-xi-battle', '/bullseye']
const isGameRoute = computed(() => GAME_PATHS.includes(router.currentRoute.value.path))

// Single "which player dropdown is open" ref (null | 'weekly' | 'games'),
// same one-at-a-time pattern as openAdminMenu below - previously these were
// two independent booleans, which let both popups end up open together
// (open Games, then open Weekly quiz without the first closing) and stack
// on top of each other on mobile.
const openPlayerMenu = ref(null)
function togglePlayerMenu(key) {
  openPlayerMenu.value = openPlayerMenu.value === key ? null : key
}
function closePlayerMenu() {
  openPlayerMenu.value = null
}

function closeGamesMenu(path, key) {
  closePlayerMenu()
  if (key) onNavClick(path, key)
}

// "Weekly quiz" groups the two solo/pass-and-play weekly boards - Grid and
// Starting XI - into one dropdown, on both desktop (top-nav) and mobile
// (bottom-nav popup, mirroring the existing Games dropdown). Their own
// multiplayer Battle variants stay as separate top-level links since those
// aren't part of "this week's board".
const WEEKLY_QUIZ_PATH_PREFIXES = ['/weekly-grid', '/starting-xi']
const isWeeklyQuizRoute = computed(() => {
  const path = router.currentRoute.value.path
  return WEEKLY_QUIZ_PATH_PREFIXES.some(prefix => path === prefix || path.startsWith(prefix + '/'))
})

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
      { to: '/admin/clubs', label: 'Clubs' }
    ]
  },
  {
    label: 'Games',
    items: [
      { to: '/admin/grids', label: 'Weekly grids' },
      { to: '/admin/lineups', label: 'Starting XI' },
      { to: '/admin/tension-questions', label: 'Tension' },
      { to: '/admin/501', label: '501' },
      { to: '/admin/imposter', label: 'Imposter' },
      { to: '/admin/bullseye', label: 'Bullseye' }
    ]
  },
  {
    label: 'Insights',
    items: [
      { to: '/admin/statistics', label: 'Statistics' },
      { to: '/admin/reports', label: 'Reports' }
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

function onNavClick(path, key) {
  if (router.currentRoute.value.path === path) {
    navTrigger.fire(key)
  }
}

function logout() {
  auth.logout()
  router.push('/')
}

// Logs out after a period of no interaction, separate from the JWT's own
// (much longer) expiry - the JWT expiring handles "closed the laptop for a
// week", this handles "left a tab open and walked away for a while". Was 60
// minutes, which turned out to be the main source of "logged out too often"
// complaints - a slow party-game round, or someone quietly building a quiz
// between distractions, routinely clears an hour of zero clicks/scrolls. 4
// hours still protects a forgotten tab on a shared/borrowed device within
// the same day, without punishing normal unhurried use.
const INACTIVITY_LIMIT_MS = 4 * 60 * 60 * 1000 // 4 hours
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

  const idleRemaining = INACTIVITY_LIMIT_MS - (Date.now() - lastActivity)
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
  if (tokenRemaining !== null && tokenRemaining > 0 && tokenRemaining <= REFRESH_BEFORE_EXPIRY_MS && !refreshInFlight) {
    attemptSilentRefresh()
  }
  if (!expiryWarningDismissed && tokenRemaining !== null && tokenRemaining > 0 && tokenRemaining <= EXPIRY_WARNING_MS) {
    showExpiryWarning.value = true
  }
}

async function attemptSilentRefresh() {
  refreshInFlight = true
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
  checkTokenOnResume()
})

onUnmounted(() => {
  activityEvents.forEach(evt => window.removeEventListener(evt, resetActivity))
  clearInterval(inactivityTimer)
  document.removeEventListener('visibilitychange', checkTokenOnResume)
})
</script>
