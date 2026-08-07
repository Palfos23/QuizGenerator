<template>
  <div>
    <h1 style="margin:0;">Grid categories</h1>
    <p class="page-subtitle">
      The list of categories available when creating athletes, clubs, grids, and pools -
      no longer limited to just Football and Cycling. Renaming a category here automatically
      updates every athlete, club, grid, and pool that already uses it.
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field" style="display:flex; gap:10px; align-items:flex-end; max-width:420px;">
      <div style="flex:1;">
        <label>New category</label>
        <input type="text" v-model="newName" placeholder="e.g. Countries by population" @keyup.enter="createCategory" />
      </div>
      <button class="btn btn-primary" :disabled="!newName.trim() || saving" @click="createCategory">+ Add</button>
    </div>

    <div v-if="loading" style="color:var(--text-dim); margin-top:20px;">Loading…</div>
    <div v-else-if="!categories.length" class="empty-state friendly">No categories yet - add your first one above.</div>

    <div v-else class="saved-quiz-list" style="margin-top:20px;">
      <div v-for="c in categories" :key="c.id" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <input
            v-if="editingId === c.id"
            type="text"
            v-model="editingName"
            class="saved-quiz-title"
            style="width:100%; background:transparent; border:1px solid var(--border); border-radius:6px; padding:4px 8px;"
            @keyup.enter="confirmRename(c)"
            @keyup.esc="editingId = null"
          />
          <div v-else class="saved-quiz-title">{{ c.name }}</div>
        </div>
        <div style="display:flex; gap:8px;">
          <template v-if="editingId === c.id">
            <button class="btn btn-primary btn-sm" :disabled="!editingName.trim() || saving" @click="confirmRename(c)">Save</button>
            <button class="btn btn-secondary btn-sm" @click="editingId = null">Cancel</button>
          </template>
          <template v-else>
            <button class="btn btn-secondary btn-sm" @click="startRename(c)">Rename</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(c)">Delete</button>
          </template>
        </div>
      </div>
    </div>

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this category?"
      :message="`'${pendingDelete.name}' will be removed. If any athlete, club, grid, or pool still uses it, this will be blocked until you reassign them first.`"
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
import gridCategories from '../services/gridCategories'

const categories = ref([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const newName = ref('')
const editingId = ref(null)
const editingName = ref('')
const pendingDelete = ref(null)

onMounted(loadCategories)

async function loadCategories() {
  loading.value = true
  error.value = ''
  try {
    categories.value = await api.adminListGridCategories()
  } catch (e) {
    error.value = 'Could not load categories.'
  } finally {
    loading.value = false
  }
}

async function createCategory() {
  const name = newName.value.trim()
  if (!name) return
  saving.value = true
  error.value = ''
  try {
    await api.adminCreateGridCategory(name)
    newName.value = ''
    toast.show('Category added.')
    gridCategories.invalidate()
    loadCategories()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not add that category.'
  } finally {
    saving.value = false
  }
}

function startRename(c) {
  editingId.value = c.id
  editingName.value = c.name
}

async function confirmRename(c) {
  const name = editingName.value.trim()
  if (!name) return
  saving.value = true
  error.value = ''
  try {
    await api.adminUpdateGridCategory(c.id, name)
    editingId.value = null
    toast.show('Category renamed - updated everywhere it was already used.')
    gridCategories.invalidate()
    loadCategories()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not rename that category.'
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
    await api.adminDeleteGridCategory(c.id)
    toast.show('Category deleted.')
    gridCategories.invalidate()
    loadCategories()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not delete that category.'
  }
}
</script>
