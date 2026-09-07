<template>
  <div style="max-width:420px; margin:40px auto;">
    <template v-if="!token">
      <h1>Reset your password</h1>
      <p class="page-subtitle">This link is missing its reset token - open the link from your email again, or request a new one from the sign-in page.</p>
      <router-link to="/" class="btn btn-secondary">Back to sign in</router-link>
    </template>

    <template v-else-if="done">
      <h1>Password updated</h1>
      <p class="page-subtitle">Your password's been changed - sign in with it below.</p>
      <router-link to="/" class="btn btn-primary">Back to sign in</router-link>
    </template>

    <template v-else>
      <h1>Reset your password</h1>
      <p class="page-subtitle">Choose a new password for your account.</p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>New password</label>
        <input type="password" v-model="newPassword" placeholder="At least 8 characters" autocomplete="new-password" @keyup.enter="submit" />
      </div>
      <div class="field">
        <label>Confirm password</label>
        <input type="password" v-model="confirmPassword" placeholder="Type it again" autocomplete="new-password" @keyup.enter="submit" />
      </div>

      <button class="btn btn-primary" style="width:100%;" :disabled="!canSubmit || submitting" @click="submit">
        {{ submitting ? 'Saving…' : 'Save new password' }}
      </button>
    </template>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'

const route = useRoute()
const token = route.query.token || ''

const newPassword = ref('')
const confirmPassword = ref('')
const error = ref('')
const submitting = ref(false)
const done = ref(false)

const canSubmit = computed(() =>
  newPassword.value.length >= 8 && newPassword.value === confirmPassword.value
)

async function submit() {
  error.value = ''
  if (newPassword.value.length < 8) {
    error.value = 'Password must be at least 8 characters.'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    error.value = "Passwords don't match."
    return
  }
  submitting.value = true
  try {
    await api.resetPassword(token, newPassword.value)
    done.value = true
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not reset your password - the link may have expired.'
  } finally {
    submitting.value = false
  }
}
</script>
