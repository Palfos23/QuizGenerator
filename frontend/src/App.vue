<template>
  <div class="app-shell">
    <nav class="top-nav">
      <router-link to="/" class="nav-brand">Quizzes</router-link>

      <template v-if="auth.isAuthenticated.value">
        <router-link v-if="!auth.isAdmin.value" to="/generate" class="nav-link" @click="onNavClick('/generate', 'generate')">Create a quiz</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/my-quizzes" class="nav-link" @click="onNavClick('/my-quizzes', 'myQuizzes')">My quizzes</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/weekly-grid" class="nav-link">Weekly grid</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/tension" class="nav-link" @click="onNavClick('/tension', 'tension')">Tension</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/501" class="nav-link">501</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/grid-battle" class="nav-link" @click="onNavClick('/grid-battle', 'gridBattle')">Grid Battle</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/questions" class="nav-link">Question bank</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/athletes" class="nav-link">Athletes</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/athlete-pools" class="nav-link">Pools</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/grids" class="nav-link">Weekly grids</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/tension-questions" class="nav-link">Tension</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/501" class="nav-link">501</router-link>
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
      <router-link v-if="!auth.isAdmin.value" to="/weekly-grid">Weekly grid</router-link>
      <div v-if="!auth.isAdmin.value" style="position:relative; flex:1; display:flex;">
        <div v-if="showGamesMenu" class="bottom-nav-backdrop" @click="showGamesMenu = false"></div>
        <button @click="showGamesMenu = !showGamesMenu" :class="{ active: isGameRoute }">Games ▾</button>
        <div v-if="showGamesMenu" class="games-popup">
          <router-link to="/tension" @click="closeGamesMenu('/tension', 'tension')">Tension</router-link>
          <router-link to="/501" @click="closeGamesMenu('/501')">501</router-link>
          <router-link to="/grid-battle" @click="closeGamesMenu('/grid-battle', 'gridBattle')">Grid Battle</router-link>
        </div>
      </div>
      <router-link v-if="auth.isAdmin.value" to="/admin/questions">Bank</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/athletes">Athletes</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/athlete-pools">Pools</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/grids">Grids</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/tension-questions">Tension</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/501">501</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/reports">Reports</router-link>
      <button @click="logout">Log out</button>
    </nav>

    <ToastHost />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import auth from './services/auth'
import { useRouter } from 'vue-router'
import navTrigger from './services/navTrigger'
import ToastHost from './components/ToastHost.vue'

const router = useRouter()

const GAME_PATHS = ['/tension', '/501', '/grid-battle']
const showGamesMenu = ref(false)
const isGameRoute = computed(() => GAME_PATHS.includes(router.currentRoute.value.path))

function closeGamesMenu(path, key) {
  showGamesMenu.value = false
  if (key) onNavClick(path, key)
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
