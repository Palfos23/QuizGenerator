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
let lastActivity = Date.now()
let inactivityTimer = null

function resetActivity() {
  lastActivity = Date.now()
}

function checkInactivity() {
  if (auth.isAuthenticated.value && Date.now() - lastActivity > INACTIVITY_LIMIT_MS) {
    auth.logout()
    router.push('/?sessionExpired=1')
  }
}

const activityEvents = ['mousemove', 'keydown', 'click', 'touchstart', 'scroll']

// On mobile, or an installed/standalone Chrome or Safari "app", the tab can sit
// backgrounded for arbitrarily long stretches - background timers get
// throttled or suspended entirely to save battery, so the 60-second interval
// below isn't reliable while backgrounded. Worse, the very act of returning
// and clicking something resets `lastActivity` before checkInactivity() ever
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
  inactivityTimer = setInterval(checkInactivity, 60 * 1000)
  document.addEventListener('visibilitychange', checkTokenOnResume)
  checkTokenOnResume()
})

onUnmounted(() => {
  activityEvents.forEach(evt => window.removeEventListener(evt, resetActivity))
  clearInterval(inactivityTimer)
  document.removeEventListener('visibilitychange', checkTokenOnResume)
})
</script>
