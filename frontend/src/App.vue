<template>
  <div class="app-shell">
    <nav class="nav-rail">
      <div class="nav-brand">Quiz<span>Night</span></div>

      <template v-if="auth.isAuthenticated.value">
        <router-link v-if="!auth.isAdmin.value" to="/generate" class="nav-link">Create a quiz</router-link>
        <router-link v-if="auth.isAdmin.value" to="/admin/questions" class="nav-link">Question bank</router-link>

        <div style="flex:1;"></div>
        <div style="padding:10px 12px; font-size:0.8rem; color:var(--text-dim);">
          {{ auth.state.displayName }}
        </div>
        <button class="btn btn-secondary btn-sm" style="margin:0 12px;" @click="logout">Log out</button>
      </template>
      <template v-else>
        <router-link to="/" class="nav-link">Log in</router-link>
        <div style="flex:1;"></div>
      </template>
    </nav>

    <div class="main-content">
      <router-view />
    </div>

    <!-- Mobile-only bottom tab bar - mirrors the sidebar, hidden on desktop via CSS -->
    <nav class="bottom-nav" v-if="auth.isAuthenticated.value">
      <router-link v-if="!auth.isAdmin.value" to="/generate">Create</router-link>
      <router-link v-if="auth.isAdmin.value" to="/admin/questions">Bank</router-link>
      <button @click="logout">Log out</button>
    </nav>
  </div>
</template>

<script setup>
import auth from './services/auth'
import { useRouter } from 'vue-router'

const router = useRouter()

function logout() {
  auth.logout()
  router.push('/')
}
</script>
