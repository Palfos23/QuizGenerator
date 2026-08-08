<template>
  <div>
    <h1 style="margin:0;">Question labels</h1>
    <p class="page-subtitle">
      Reusable tags that can span categories - e.g. "Lord of the Rings" on both a Literature
      question and a Movies question. When generating a quiz, you can filter to only questions
      tagged with one or more of these.
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field" style="display:flex; gap:10px; align-items:flex-end; max-width:420px;">
      <div style="flex:1;">
        <label>New label</label>
        <input type="text" v-model="newName" placeholder="e.g. Lord of the Rings" @keyup.enter="createLabel" />
      </div>
      <button class="btn btn-primary" :disabled="!newName.trim() || saving" @click="createLabel">+ Add</button>
    </div>

    <div v-if="loading" style="color:var(--text-dim); margin-top:20px;">Loading…</div>
    <div v-else-if="!labels.length" class="empty-state friendly">No labels yet - add your first one above.</div>

    <div v-else class="saved-quiz-list" style="margin-top:20px;">
      <div v-for="l in labels" :key="l.id" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <input
            v-if="editingId === l.id"
            type="text"
            v-model="editingName"
            class="saved-quiz-title"
            style="width:100%; background:transparent; border:1px solid var(--border); border-radius:6px; padding:4px 8px;"
            @keyup.enter="confirmRename(l)"
            @keyup.esc="editingId = null"
          />
          <div v-else class="saved-quiz-title">{{ l.name }}</div>
        </div>
        <div style="display:flex; gap:8px;">
          <template v-if="editingId === l.id">
            <button class="btn btn-primary btn-sm" :disabled="!editingName.trim() || saving" @click="confirmRename(l)">Save</button>
            <button class="btn btn-secondary btn-sm" @click="editingId = null">Cancel</button>
          </template>
          <template v-else>
            <button class="btn btn-secondary btn-sm" @click="startRename(l)">Rename</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(l)">Delete</button>
          </template>
        </div>
      </div>
    </div>

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this label?"
      :message="`'${pendingDelete.name}' will be removed from any question it's currently attached to.`"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
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
    error.value = 'Could not load labels.'
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
    toast.show('Label added.')
    loadLabels()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not add that label.'
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
    toast.show('Label renamed.')
    loadLabels()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not rename that label.'
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
    toast.show('Label deleted.')
    loadLabels()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not delete that label.'
  }
}
</script>
