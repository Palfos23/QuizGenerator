<template>
  <div class="app-shell">
    <nav class="top-nav">
      <router-link to="/" class="nav-brand">Quiz<span>Maker</span></router-link>

      <template v-if="auth.isAuthenticated.value">
        <router-link v-if="!auth.isAdmin.value" to="/generate" class="nav-link">Create a quiz</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/my-quizzes" class="nav-link">My quizzes</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/weekly-grid" class="nav-link">Weekly grid</router-link>
        <router-link v-if="!auth.isAdmin.value" to="/tension" class="nav-link">Tension</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/questions" class="nav-link">Question bank</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/athletes" class="nav-link">Athletes</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/grids" class="nav-link">Weekly grids</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/tension-questions" class="nav-link">Tension</router-link>
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
      <router-link v-if="!auth.isAdmin.value" to="/generate">Create</router-link>
      <router-link v-if="!auth.isAdmin.value" to="/my-quizzes">My quizzes</router-link>
      <router-link v-if="!auth.isAdmin.value" to="/weekly-grid">Weekly grid</router-link>
      <router-link v-if="!auth.isAdmin.value" to="/tension">Tension</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/questions">Bank</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/athletes">Athletes</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/grids">Grids</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/tension-questions">Tension</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/reports">Reports</router-link>
      <button @click="logout">Log out</button>
    </nav>

    <ToastHost />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import auth from './services/auth'
import { useRouter } from 'vue-router'
import ToastHost from './components/ToastHost.vue'

const router = useRouter()

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

onMounted(() => {
  activityEvents.forEach(evt => window.addEventListener(evt, resetActivity, { passive: true }))
  inactivityTimer = setInterval(checkInactivity, 60 * 1000)
})

onUnmounted(() => {
  activityEvents.forEach(evt => window.removeEventListener(evt, resetActivity))
  clearInterval(inactivityTimer)
})
</script>
