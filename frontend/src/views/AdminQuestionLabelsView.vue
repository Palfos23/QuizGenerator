<template>
  <div>
    <h1 style="margin:0;">{{ $t('Spørsmålsetiketter') }}</h1>
    <p class="page-subtitle">
      {{ $t('Gjenbrukbare merkelapper som kan gå på tvers av kategorier - f.eks. «Ringenes herre» på både et litteraturspørsmål og et filmspørsmål. Når du lager en quiz, kan du filtrere på bare spørsmål merket med en eller flere av disse.') }}
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field" style="display:flex; gap:10px; align-items:flex-end; max-width:420px;">
      <div style="flex:1;">
        <label>{{ $t('Ny etikett') }}<input type="text" v-model="newName" :placeholder="$t('f.eks. Ringenes herre')" @keyup.enter="createLabel" /></label>
      </div>
      <button class="btn btn-primary" :disabled="!newName.trim() || saving" @click="createLabel">{{ $t('+ Legg til') }}</button>
    </div>

    <div v-if="loading" style="color:var(--text-dim); margin-top:20px;">{{ $t('Laster…') }}</div>
    <div v-else-if="!labels.length" class="empty-state friendly">{{ $t('Ingen etiketter ennå - legg til din første over.') }}</div>

    <div v-else class="saved-quiz-list" style="margin-top:20px;">
      <div v-for="l in labels" :key="l.id" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <input
            v-if="editingId === l.id"
            type="text"
            v-model="editingName"
            class="saved-quiz-title"
            style="width:100%; background:transparent; border:1px solid var(--border); border-radius: 2px; padding:4px 8px;"
            @keyup.enter="confirmRename(l)"
            @keyup.esc="editingId = null"
          />
          <div v-else class="saved-quiz-title">{{ l.name }}</div>
        </div>
        <div style="display:flex; gap:8px;">
          <template v-if="editingId === l.id">
            <button class="btn btn-primary btn-sm" :disabled="!editingName.trim() || saving" @click="confirmRename(l)">{{ $t('Lagre') }}</button>
            <button class="btn btn-secondary btn-sm" @click="editingId = null">{{ $t('Avbryt') }}</button>
          </template>
          <template v-else>
            <button class="btn btn-secondary btn-sm" @click="startRename(l)">{{ $t('Gi nytt navn') }}</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(l)">{{ $t('Slett') }}</button>
          </template>
        </div>
      </div>
    </div>

    <ConfirmModal
      v-if="pendingDelete"
      :title="$t('Slette denne etiketten?')"
      :message="$t('«{0}» fjernes fra alle spørsmål den er knyttet til nå.', [pendingDelete.name])"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { onMounted, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import ConfirmModal from '../components/ConfirmModal.vue'

const labels = ref([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const newName = ref('')
const editingId = ref(null)
const editingName = ref('')
const pendingDelete = ref(null)

onMounted(loadLabels)

async function loadLabels() {
  loading.value = true
  error.value = ''
  try {
    labels.value = await api.adminListQuestionLabels()
  } catch (e) {
    error.value = _t('Kunne ikke laste etiketter.')
  } finally {
    loading.value = false
  }
}

async function createLabel() {
  const name = newName.value.trim()
  if (!name) return
  saving.value = true
  error.value = ''
  try {
    await api.adminCreateQuestionLabel(name)
    newName.value = ''
    toast.show(_t('Etiketten er lagt til.'))
    loadLabels()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke legge til etiketten.')
  } finally {
    saving.value = false
  }
}

function startRename(l) {
  editingId.value = l.id
  editingName.value = l.name
}

async function confirmRename(l) {
  const name = editingName.value.trim()
  if (!name) return
  saving.value = true
  error.value = ''
  try {
    await api.adminUpdateQuestionLabel(l.id, name)
    editingId.value = null
    toast.show(_t('Etiketten har fått nytt navn.'))
    loadLabels()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke gi etiketten nytt navn.')
  } finally {
    saving.value = false
  }
}

function requestDelete(l) {
  pendingDelete.value = l
}

async function doDelete() {
  const l = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.adminDeleteQuestionLabel(l.id)
    toast.show(_t('Etiketten er slettet.'))
    loadLabels()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke slette etiketten.')
  }
}
</script>
