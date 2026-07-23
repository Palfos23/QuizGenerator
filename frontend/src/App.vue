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
      <button @click="logout">Log out</button>
    </nav>

    <ToastHost />
  </div>
</template>

<script setup>
import auth from './services/auth'
import { useRouter } from 'vue-router'
import ToastHost from './components/ToastHost.vue'

const router = useRouter()

function logout() {
  auth.logout()
  router.push('/')
}
</script>
