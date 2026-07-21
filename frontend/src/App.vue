<template>
  <div class="app-shell">
    <nav class="nav-rail">
      <div class="nav-brand">Quiz<span>Creator</span></div>

      <template v-if="auth.isAuthenticated.value">
        <router-link v-if="!auth.isAdmin.value" to="/generate" class="nav-link">Create a quiz</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin" class="nav-link">Question bank</router-link>

        <div style="flex:1;"></div>
        <div style="padding:10px 12px; font-size:0.8rem; color:var(--text-dim);">
          {{ auth.state.displayName }}
        </div>
        <button class="btn btn-secondary btn-sm" style="margin:0 12px;" @click="logout">Log out</button>
      </template>
      <template v-else>
        <router-link to="/login" class="nav-link">Log in</router-link>
        <div style="flex:1;"></div>
      </template>

      <!-- Intentionally tiny and unlabeled-in-nav-terms: the real gate is the backend's
           ADMIN role check, not this link being hard to spot. -->
      <router-link to="/admin-access" style="font-size:0.7rem; color:var(--text-dim); opacity:0.5; padding:8px 12px; text-decoration:none;">
        Admin
      </router-link>
    </nav>

    <div class="main-content">
      <router-view />
    </div>

    <!-- Mobile-only bottom tab bar - mirrors the sidebar, hidden on desktop via CSS -->
    <nav class="bottom-nav" v-if="auth.isAuthenticated.value">
      <router-link v-if="!auth.isAdmin.value" to="/generate">
        <span>📝</span> Create
      </router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin">
        <span>🗂️</span> Bank
      </router-link>
      <button @click="logout">
        <span>🚪</span> Log out
      </button>
    </nav>
  </div>
</template>

<script setup>
import auth from './services/auth'
import { useRouter } from 'vue-router'

const router = useRouter()

function logout() {
  auth.logout()
  router.push('/login')
}
</script>
