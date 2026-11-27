<template>
  <div style="max-width:640px; margin:0 auto;">
    <h1 style="margin:0 0 4px;">{{ $t('Konto') }}</h1>
    <p class="page-subtitle">{{ $t('Administrer dataene dine, eller last ned eller slett kontoen din.') }}</p>
    <TipsModal id="account" :tips="getTips('account')" />

    <div v-if="error" class="banner error">{{ error }}</div>

    <form class="field" @submit.prevent="saveName">
      <label for="display-name">{{ $t('Visningsnavn') }}</label>
      <p class="page-subtitle" style="margin-top:-6px;">
        {{ $t('Navnet som vises for deg på topplistene, ukens stilling og i spillrom. Bruk gjerne for- og etternavn - når to spillere har samme fornavn, legges første bokstav i etternavnet til (slik at «Erik H.» og «Erik S.» kan skilles).') }}
      </p>
      <div class="display-name-row">
        <input id="display-name" type="text" v-model="displayName" maxlength="40" autocomplete="name" :placeholder="$t('Navnet ditt')" />
        <button type="submit" class="btn btn-primary" :disabled="savingName || !nameChanged">
          {{ savingName ? $t('Lagrer…') : $t('Lagre navn') }}
        </button>
      </div>
    </form>

    <!-- password: only for accounts that have one (a Google-only account has nothing to change) -->
    <form v-if="hasPassword" class="field" style="margin-top:32px; padding-top:24px; border-top:1px solid var(--border);" @submit.prevent="savePassword">
      <label for="current-password">{{ $t('Bytt passord') }}</label>
      <p class="page-subtitle" style="margin-top:-6px;">
        {{ $t('Skriv inn passordet du bruker nå, og velg et nytt på minst 8 tegn. Husker du ikke det nåværende? Logg ut og bruk «Glemt passord?» på innloggingen.') }}
      </p>
      <div v-if="passwordError" class="banner error">{{ passwordError }}</div>
      <div class="password-grid">
        <label class="password-label">{{ $t('Nåværende passord') }}
          <input id="current-password" type="password" v-model="currentPassword" autocomplete="current-password" />
        </label>
        <label class="password-label">{{ $t('Nytt passord') }}
          <input type="password" v-model="newPassword" autocomplete="new-password" minlength="8" />
        </label>
        <label class="password-label">{{ $t('Gjenta nytt passord') }}
          <input type="password" v-model="repeatPassword" autocomplete="new-password" />
        </label>
      </div>
      <button type="submit" class="btn btn-primary" :disabled="savingPassword || !passwordReady">
        {{ savingPassword ? $t('Lagrer…') : $t('Bytt passord') }}
      </button>
    </form>
    <div v-else-if="hasPassword === false" class="field" style="margin-top:32px; padding-top:24px; border-top:1px solid var(--border);">
      <label>{{ $t('Passord') }}</label>
      <p class="page-subtitle" style="margin-top:-6px;">
        {{ $t('Du logger inn med Google, så det er ikke noe passord å bytte. Vil du også kunne logge inn med e-post og passord, kan du bruke «Glemt passord?» på innloggingen med e-postadressen din.') }}
      </p>
    </div>

    <div class="field" style="margin-top:32px; padding-top:24px; border-top:1px solid var(--border);">
      <label>{{ $t('Fargetema') }}</label>
      <div class="palette-row">
        <PaletteSwitcher class="palette-switch--lg" />
        <span class="palette-name">{{ currentPaletteName }}</span>
      </div>
    </div>

    <div class="field" style="margin-top:32px; padding-top:24px; border-top:1px solid var(--border);">
      <label>{{ $t('Last ned dataene mine') }}</label>
      <p class="page-subtitle" style="margin-top:-6px;">
        {{ $t('En kopi av alt som er knyttet til kontoen din - profil, lagrede quizer, innsendte spørsmål, rapporter og spillhistorikk - som en JSON-fil.') }}
      </p>
      <button class="btn btn-secondary" :disabled="exporting" @click="exportData">
        {{ exporting ? $t('Forbereder…') : $t('⇩ Last ned dataene mine') }}
      </button>
    </div>

    <div class="field" style="margin-top:32px; padding-top:24px; border-top:1px solid var(--border);">
      <label style="color:var(--coral);">{{ $t('Slett kontoen min') }}</label>
      <p class="page-subtitle" style="margin-top:-6px;">
        {{ $t('Sletter kontoen din, lagrede quizer og spillhistorikk for godt. Dette kan ikke angres. Se vår') }}
        <router-link to="/privacy">{{ $t('personvernerklæring') }}</router-link> {{ $t('for hva som skjer med innhold du har bidratt med andre steder.') }}
      </p>
      <button class="btn btn-danger" @click="showDeleteModal = true">{{ $t('Slett kontoen min…') }}</button>
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
import TipsModal from '../components/TipsModal.vue'
import { getTips } from '../content/tips'
import { t as _t } from '../i18n'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'
import toast from '../services/toast'
import DeleteAccountModal from '../components/DeleteAccountModal.vue'
import PaletteSwitcher from '../components/PaletteSwitcher.vue'
import { PALETTES, palette } from '../services/palette'

const router = useRouter()
const currentPaletteName = computed(() => PALETTES.find(p => p.id === palette.value)?.name || '')
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
    toast.show(_t('Navnet er oppdatert.'))
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke oppdatere navnet ditt.')
  } finally {
    savingName.value = false
  }
}
// --- password ---
const hasPassword = ref(null) // null until we know; false for a Google-only account
const currentPassword = ref('')
const newPassword = ref('')
const repeatPassword = ref('')
const savingPassword = ref(false)
const passwordError = ref('')
const passwordReady = computed(() => currentPassword.value && newPassword.value.length >= 8 && repeatPassword.value)

onMounted(async () => {
  try {
    hasPassword.value = (await api.getMyPasswordStatus()).hasPassword
  } catch (e) {
    hasPassword.value = null // unknown: show nothing rather than something wrong
  }
})

async function savePassword() {
  passwordError.value = ''
  if (newPassword.value !== repeatPassword.value) {
    passwordError.value = _t('De to nye passordene er ikke like.')
    return
  }
  savingPassword.value = true
  try {
    const result = await api.changeMyPassword(currentPassword.value, newPassword.value)
    // Stay signed in here with the new sign-in; every other device is signed out by the change.
    if (result?.token) auth.login({ token: result.token, displayName: result.displayName, role: result.role })
    currentPassword.value = newPassword.value = repeatPassword.value = ''
    toast.show(_t('Passordet er endret. Andre enheter er logget ut.'))
  } catch (e) {
    passwordError.value = e.response?.data?.message || _t('Kunne ikke bytte passordet.')
  } finally {
    savingPassword.value = false
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
    error.value = _t('Kunne ikke laste ned dataene dine - prøv igjen.')
  } finally {
    exporting.value = false
  }
}

async function deleteAccount(password) {
  try {
    await api.deleteMyAccount(password)
    showDeleteModal.value = false
    auth.logout()
    toast.show(_t('Kontoen din er slettet.'))
    router.push('/')
  } catch (e) {
    deleteModal.value?.setError(e.response?.data?.message || _t('Kunne ikke slette kontoen din.'))
  }
}
</script>

<style scoped>
.palette-row { display: flex; align-items: center; flex-wrap: wrap; gap: var(--space-2) var(--space-4); padding: var(--space-2) var(--space-1); min-width: 0; }
.palette-name { font-weight: 600; }
.display-name-row { display: flex; gap: 10px; flex-wrap: wrap; }
.password-grid { display: grid; gap: 14px; margin-bottom: 16px; }
.password-label { display: grid; gap: 6px; font-size: 0.85rem; color: var(--text-dim); }
.password-label input { width: 100%; }
.display-name-row input {
  flex: 1;
  min-width: 200px;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid rgb(var(--fg-rgb) / 0.16);
  background: rgb(var(--fg-rgb) / 0.08);
  color: var(--text);
  font-size: 0.95rem;
  font-family: var(--font-body);
}
.display-name-row input:focus {
  outline: none;
  border-color: var(--gold);
  box-shadow: 0 0 0 4px rgb(var(--gold-rgb) / 0.16);
}
</style>
