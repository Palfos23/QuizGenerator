<template>
  <div style="max-width:640px; margin:0 auto;">
    <h1 style="margin:0 0 4px;">Account</h1>
    <p class="page-subtitle">Manage your data, or download or delete your account.</p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <form class="field" @submit.prevent="saveName">
      <label for="display-name">Display name</label>
      <p class="page-subtitle" style="margin-top:-6px;">
        The name shown for you on leaderboards, the weekly standings and in game rooms. Use your first and last
        name if you like - when two players share a first name, the first letter of the last name is added
        (so "Erik H." and "Erik S." can be told apart).
      </p>
      <div class="display-name-row">
        <input id="display-name" type="text" v-model="displayName" maxlength="40" autocomplete="name" placeholder="Your name" />
        <button type="submit" class="btn btn-primary" :disabled="savingName || !nameChanged">
          {{ savingName ? 'Saving…' : 'Save name' }}
        </button>
      </div>
    </form>

    <div class="field" style="margin-top:32px; padding-top:24px; border-top:1px solid var(--border);">
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
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'
import toast from '../services/toast'
import DeleteAccountModal from '../components/DeleteAccountModal.vue'

const router = useRouter()
const error = ref('')
const exporting = ref(false)

// --- display name ---
const displayName = ref(auth.state.displayName || '')
const savingName = ref(false)
const nameChanged = computed(() => displayName.value.trim() !== (auth.state.displayName || '') && displayName.value.trim().length > 0)

async function saveName() {
  savingName.value = true
  error.value = ''
  try {
    const result = await api.updateMyDisplayName(displayName.value)
    auth.updateToken(result) // a fresh token carrying the new name, so the whole app switches over now
    displayName.value = result.displayName
    toast.show('Name updated.')
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not update your name.'
  } finally {
    savingName.value = false
  }
}
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

<style scoped>
.display-name-row { display: flex; gap: 10px; flex-wrap: wrap; }
.display-name-row input {
  flex: 1;
  min-width: 200px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.08);
  color: var(--text);
  font-size: 0.95rem;
  font-family: var(--font-body);
}
.display-name-row input:focus {
  outline: none;
  border-color: var(--gold);
  box-shadow: 0 0 0 4px rgba(242, 183, 5, 0.16);
}
</style>
