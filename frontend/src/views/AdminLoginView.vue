<template>
  <div style="max-width:380px; margin-top:60px;">
    <h1>Admin sign-in</h1>
    <p class="page-subtitle">Separate credentials from the regular Google login.</p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field">
      <label>Username</label>
      <input type="text" v-model="username" @keyup.enter="submit" autocomplete="username" />
    </div>
    <div class="field">
      <label>Password</label>
      <input type="password" v-model="password" @keyup.enter="submit" autocomplete="current-password" />
    </div>

    <button class="btn btn-primary" :disabled="loading" @click="submit">
      {{ loading ? 'Signing in…' : 'Sign in' }}
    </button>

    <p style="margin-top:24px;">
      <router-link to="/" style="color:var(--text-dim); font-size:0.85rem;">
        Not an admin? Go to regular sign-in
      </router-link>
    </p>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

onMounted(() => {
  if (auth.isAdmin.value) {
    router.push('/admin/questions')
  }
})

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const result = await api.loginAsAdmin(username.value, password.value)
    auth.login({ token: result.token, displayName: result.displayName, role: result.role })
    router.push('/admin/questions')
  } catch (e) {
    error.value = e.response?.data?.message || 'Invalid admin credentials.'
  } finally {
    loading.value = false
  }
}
</script>
