<template>
  <div style="max-width:640px; margin:0 auto;">
    <h1 style="margin:0 0 4px;">Konto</h1>
    <p class="page-subtitle">Administrer dataene dine, eller last ned eller slett kontoen din.</p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <form class="field" @submit.prevent="saveName">
      <label for="display-name">Visningsnavn</label>
      <p class="page-subtitle" style="margin-top:-6px;">
        Navnet som vises for deg på topplistene, ukens stilling og i spillrom. Bruk gjerne for- og etternavn - når to spillere har samme fornavn, legges første bokstav i etternavnet til (slik at «Erik H.» og «Erik S.» kan skilles).
      </p>
      <div class="display-name-row">
        <input id="display-name" type="text" v-model="displayName" maxlength="40" autocomplete="name" placeholder="Navnet ditt" />
        <button type="submit" class="btn btn-primary" :disabled="savingName || !nameChanged">
          {{ savingName ? 'Lagrer…' : 'Lagre navn' }}
        </button>
      </div>
    </form>

    <div class="field" style="margin-top:32px; padding-top:24px; border-top:1px solid var(--border);">
      <label>Last ned dataene mine</label>
      <p class="page-subtitle" style="margin-top:-6px;">
        En kopi av alt som er knyttet til kontoen din - profil, lagrede quizer, innsendte spørsmål, rapporter og spillhistorikk - som en JSON-fil.
      </p>
      <button class="btn btn-secondary" :disabled="exporting" @click="exportData">
        {{ exporting ? 'Forbereder…' : '⇩ Last ned dataene mine' }}
      </button>
    </div>

    <div class="field" style="margin-top:32px; padding-top:24px; border-top:1px solid var(--border);">
      <label style="color:var(--coral);">Slett kontoen min</label>
      <p class="page-subtitle" style="margin-top:-6px;">
        Sletter kontoen din, lagrede quizer og spillhistorikk for godt. Dette kan ikke angres. Se vår
        <router-link to="/privacy">personvernerklæring</router-link> for hva som skjer med innhold du har bidratt med andre steder.
      </p>
      <button class="btn btn-danger" @click="showDeleteModal = true">Slett kontoen min…</button>
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
    toast.show('Navnet er oppdatert.')
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke oppdatere navnet ditt.'
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
    error.value = 'Kunne ikke laste ned dataene dine - prøv igjen.'
  } finally {
    exporting.value = false
  }
}

async function deleteAccount(password) {
  try {
    await api.deleteMyAccount(password)
    showDeleteModal.value = false
    auth.logout()
    toast.show('Kontoen din er slettet.')
    router.push('/')
  } catch (e) {
    deleteModal.value?.setError(e.response?.data?.message || 'Kunne ikke slette kontoen din.')
  }
}
</script>

<style scoped>
.display-name-row { display: flex; gap: 10px; flex-wrap: wrap; }
.display-name-row input {
  flex: 1;
  min-width: 200px;
  padding: 12px 14px;
  border-radius: 2px;
  border: 1px solid rgba(27, 26, 23, 0.16);
  background: rgba(27, 26, 23, 0.08);
  color: var(--text);
  font-size: 0.95rem;
  font-family: var(--font-body);
}
.display-name-row input:focus {
  outline: none;
  border-color: var(--gold);
  box-shadow: 0 0 0 4px rgba(216, 67, 43, 0.16);
}
</style>
