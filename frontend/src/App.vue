<template>
  <div class="app-shell">
    <nav class="top-nav">
      <router-link to="/" class="nav-brand">Quizzes</router-link>

      <template v-if="auth.isAuthenticated.value">
        <router-link v-if="!auth.isAdmin.value" to="/generate" class="nav-link" @click="onNavClick('/generate', 'generate')">Create a quiz</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/my-quizzes" class="nav-link" @click="onNavClick('/my-quizzes', 'myQuizzes')">My quizzes</router-link>
        <div v-if="!auth.isAdmin.value" class="top-nav-dropdown">
          <div v-if="showWeeklyMenu" class="top-nav-dropdown-backdrop" @click="showWeeklyMenu = false"></div>
          <button type="button" class="nav-link top-nav-dropdown-toggle" :class="{ 'router-link-exact-active': isWeeklyQuizRoute }" @click="showWeeklyMenu = !showWeeklyMenu">
            Weekly quiz ▾
          </button>
          <div v-if="showWeeklyMenu" class="top-nav-dropdown-popup">
            <router-link to="/weekly-grid" class="nav-link" @click="showWeeklyMenu = false">Grid</router-link>
            <router-link to="/starting-xi" class="nav-link" @click="showWeeklyMenu = false">Starting XI</router-link>
          </div>
        </div>
        <router-link v-if="!auth.isAdmin.value" to="/tension" class="nav-link" @click="onNavClick('/tension', 'tension')">Tension</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/501" class="nav-link" @click="onNavClick('/501', 'fiveOhOne')">501</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/imposter" class="nav-link" @click="onNavClick('/imposter', 'imposter')">Imposter</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/grid-battle" class="nav-link" @click="onNavClick('/grid-battle', 'gridBattle')">Grid Battle</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/starting-xi-battle" class="nav-link" @click="onNavClick('/starting-xi-battle', 'startingXiBattle')">XI Battle</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/questions" class="nav-link">Question bank</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/question-labels" class="nav-link">Labels</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/athletes" class="nav-link">Subjects</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/athlete-pools" class="nav-link">Pools</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/grid-categories" class="nav-link">Categories</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/grids" class="nav-link">Weekly grids</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/lineups" class="nav-link">Starting XI</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/tension-questions" class="nav-link">Tension</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/501" class="nav-link">501</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/imposter" class="nav-link">Imposter</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/reports" class="nav-link">Reports</router-link>

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
        <div v-if="showWeeklyMenu" class="bottom-nav-backdrop" @click="showWeeklyMenu = false"></div>
        <button @click="showWeeklyMenu = !showWeeklyMenu" :class="{ active: isWeeklyQuizRoute }">Weekly quiz ▾</button>
        <div v-if="showWeeklyMenu" class="games-popup">
          <router-link to="/weekly-grid" @click="showWeeklyMenu = false">Grid</router-link>
          <router-link to="/starting-xi" @click="showWeeklyMenu = false">Starting XI</router-link>
        </div>
      </div>
      <div v-if="!auth.isAdmin.value" style="position:relative; flex:1; display:flex;">
        <div v-if="showGamesMenu" class="bottom-nav-backdrop" @click="showGamesMenu = false"></div>
        <button @click="showGamesMenu = !showGamesMenu" :class="{ active: isGameRoute }">Games ▾</button>
        <div v-if="showGamesMenu" class="games-popup">
          <router-link to="/tension" @click="closeGamesMenu('/tension', 'tension')">Tension</router-link>
          <router-link to="/501" @click="closeGamesMenu('/501', 'fiveOhOne')">501</router-link>
          <router-link to="/imposter" @click="closeGamesMenu('/imposter', 'imposter')">Imposter</router-link>
          <router-link to="/grid-battle" @click="closeGamesMenu('/grid-battle', 'gridBattle')">Grid Battle</router-link>
          <router-link to="/starting-xi-battle" @click="closeGamesMenu('/starting-xi-battle', 'startingXiBattle')">XI Battle</router-link>
        </div>
      </div>
      <router-link v-if="auth.isAdmin.value" to="/admin/questions">Bank</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/question-labels">Labels</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/athletes">Subjects</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/athlete-pools">Pools</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/grid-categories">Categories</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/grids">Grids</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/lineups">Starting XI</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/tension-questions">Tension</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/501">501</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/imposter">Imposter</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/reports">Reports</router-link>
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
import { useRouter } from 'vue-router'
import navTrigger from './services/navTrigger'
import ToastHost from './components/ToastHost.vue'

const router = useRouter()

const GAME_PATHS = ['/tension', '/501', '/imposter', '/grid-battle', '/starting-xi-battle']
const showGamesMenu = ref(false)
const isGameRoute = computed(() => GAME_PATHS.includes(router.currentRoute.value.path))

function closeGamesMenu(path, key) {
  showGamesMenu.value = false
  if (key) onNavClick(path, key)
}

// "Weekly quiz" groups the two solo/pass-and-play weekly boards - Grid and
// Starting XI - into one dropdown, on both desktop (top-nav) and mobile
// (bottom-nav popup, mirroring the existing Games dropdown). Their own
// multiplayer Battle variants stay as separate top-level links since those
// aren't part of "this week's board".
const WEEKLY_QUIZ_PATH_PREFIXES = ['/weekly-grid', '/starting-xi']
const showWeeklyMenu = ref(false)
const isWeeklyQuizRoute = computed(() => {
  const path = router.currentRoute.value.path
  return WEEKLY_QUIZ_PATH_PREFIXES.some(prefix => path === prefix || path.startsWith(prefix + '/'))
})

// Belt-and-suspenders close for both popups on any navigation - covers the
// case where a click lands on a different nav item entirely (not one of
// this popup's own links, which already close it themselves) while a
// dropdown happens to be open.
watch(() => router.currentRoute.value.path, () => {
  showGamesMenu.value = false
  showWeeklyMenu.value = false
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
// week", this handles "left a tab open and walked away for a while".
const INACTIVITY_LIMIT_MS = 60 * 60 * 1000 // 60 minutes
// How long before either cutoff to show a heads-up, so a hard logout never
// just appears out of nowhere mid-game.
const INACTIVITY_WARNING_MS = 60 * 1000
const EXPIRY_WARNING_MS = 2 * 60 * 1000
let lastActivity = Date.now()
let inactivityTimer = null

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

  // The token's own absolute expiry can't be postponed by activity (no
  // refresh token exists) - this is purely a "wrap up soon" heads-up, not
  // something staySignedIn() can defer.
  const tokenRemaining = auth.msUntilTokenExpiry()
  if (!expiryWarningDismissed && tokenRemaining !== null && tokenRemaining > 0 && tokenRemaining <= EXPIRY_WARNING_MS) {
    showExpiryWarning.value = true
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
