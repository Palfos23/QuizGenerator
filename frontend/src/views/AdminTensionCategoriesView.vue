<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
      <div>
        <h1>Tension answer categories</h1>
        <p class="page-subtitle">
          Word lists that power the answer-box autocomplete while playing - not necessarily
          all correct answers, just plausible same-category words to type from.
          <router-link to="/admin/tension-questions">← Back to questions</router-link>
        </p>
      </div>
      <button class="btn btn-primary" @click="openCreate">+ Add category</button>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="!categories.length" class="empty-state friendly">
      No answer categories yet. Add one (e.g. "countries") with a list of suggestion words.
    </div>

    <div v-else class="saved-quiz-list">
      <div v-for="c in categories" :key="c.id" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ c.name }}</div>
          <div class="saved-quiz-meta">{{ c.options.length }} suggestion word(s)</div>
        </div>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-secondary btn-sm" @click="openEdit(c)">Edit</button>
          <button class="btn btn-danger btn-sm" @click="requestDelete(c)">Delete</button>
        </div>
      </div>
    </div>

    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal">
        <h2>{{ editingId ? 'Edit category' : 'Add category' }}</h2>
        <div v-if="modalError" class="banner error">{{ modalError }}</div>

        <div class="field">
          <label>Name</label>
          <input type="text" v-model="form.name" placeholder="e.g. countries" />
        </div>

        <div class="field">
          <label>Suggestion words <span class="picker-hint">one per line</span></label>
          <textarea v-model="optionsText" rows="10" placeholder="Norway&#10;Sweden&#10;Denmark&#10;…"></textarea>
        </div>

        <div style="display:flex; gap:10px; justify-content:flex-end;">
          <button class="btn btn-secondary" @click="showModal = false">Cancel</button>
          <button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? 'Saving…' : 'Save' }}</button>
        </div>
      </div>
    </div>

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this category?"
      :message="`'${pendingDelete.name}' will be removed. Questions using it will lose their answer-box suggestions.`"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import ConfirmModal from '../components/ConfirmModal.vue'

const categories = ref([])
const loading = ref(true)
const error = ref('')
const showModal = ref(false)
const editingId = ref(null)
const optionsText = ref('')
const saving = ref(false)
const modalError = ref('')
const pendingDelete = ref(null)

const form = reactive({ name: '' })

onMounted(loadCategories)

async function loadCategories() {
  loading.value = true
  error.value = ''
  try {
    categories.value = await api.adminListTensionCategories()
  } catch (e) {
    error.value = 'Could not load categories.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.name = ''
  optionsText.value = ''
  editingId.value = null
  modalError.value = ''
  showModal.value = true
}

function openEdit(c) {
  form.name = c.name
  optionsText.value = c.options.join('\n')
  editingId.value = c.id
  modalError.value = ''
  showModal.value = true
}

async function save() {
  modalError.value = ''
  if (!form.name.trim()) {
    modalError.value = 'Name is required.'
    return
  }
  const options = optionsText.value.split('\n').map(s => s.trim()).filter(Boolean)
  saving.value = true
  try {
    const payload = { name: form.name, options }
    if (editingId.value) {
      await api.adminUpdateTensionCategory(editingId.value, payload)
    } else {
      await api.adminCreateTensionCategory(payload)
    }
    toast.show('Category saved.')
    showModal.value = false
    loadCategories()
  } catch (e) {
    modalError.value = e.response?.data?.message || 'Could not save the category.'
  } finally {
    saving.value = false
  }
}

function requestDelete(c) {
  pendingDelete.value = c
}

async function doDelete() {
  const c = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.adminDeleteTensionCategory(c.id)
    toast.show('Category deleted.')
    loadCategories()
  } catch (e) {
    error.value = 'Could not delete that category.'
  }
}
</script>
