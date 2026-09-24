<template>
  <div style="max-width:640px; margin:0 auto;">
    <h1 style="margin:0 0 4px;">Account</h1>
    <p class="page-subtitle">Manage your data, or download or delete your account.</p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field">
      <label>Download my data</label>
      <p class="page-subtitle" style="margin-top:-6px;">
        A copy of everything tied to your account - profile, saved quizzes, submitted questions, reports, and game
        history - as a JSON file.
      </p>
      <button class="btn btn-secondary" :disabled="exporting" @click="exportData">
        {{ exporting ? 'Preparing…' : '⇩ Download my data' }}
      </button>
    </div>

    <div class="field" style="margin-top:32px; padding-top:24px; border-top:1px solid var(--border);">
      <label style="color:var(--coral);">Delete my account</label>
      <p class="page-subtitle" style="margin-top:-6px;">
        Permanently deletes your account, saved quizzes, and game history. This can't be undone. See our
        <router-link to="/privacy">Privacy Policy</router-link> for what happens to content you've contributed
        elsewhere.
      </p>
      <button class="btn btn-danger" @click="showDeleteModal = true">Delete my account…</button>
    </div>

    <DeleteAccountModal
      v-if="showDeleteModal"
      ref="deleteModal"
      @cancel="showDeleteModal = false"
      @confirm="deleteAccount"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'
import toast from '../services/toast'
import DeleteAccountModal from '../components/DeleteAccountModal.vue'

const router = useRouter()
const error = ref('')
const exporting = ref(false)
const showDeleteModal = ref(false)
const deleteModal = ref(null)

async function exportData() {
  exporting.value = true
  error.value = ''
  try {
    const data = await api.exportMyData()
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'my-quizzes-data.json'
    link.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    error.value = 'Could not download your data - please try again.'
  } finally {
    exporting.value = false
  }
}

async function deleteAccount(password) {
  try {
    await api.deleteMyAccount(password)
    showDeleteModal.value = false
    auth.logout()
    toast.show('Your account has been deleted.')
    router.push('/')
  } catch (e) {
    deleteModal.value?.setError(e.response?.data?.message || 'Could not delete your account.')
  }
}
</script>
