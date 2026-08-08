<template>
  <div>
    <h1 style="margin:0;">Grid categories</h1>
    <p class="page-subtitle">
      The list of categories available when creating subjects, clubs, grids, and pools -
      no longer limited to just Football and Cycling. Renaming a category here automatically
      updates every subject, club, grid, and pool that already uses it. Each category also has
      its own "grouping" word - "Team" for a sport, "Continent" for countries, "Label" for
      artists - used for the bulk-add-a-group feature and shown next to each subject.
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field" style="display:flex; gap:10px; align-items:flex-end; flex-wrap:wrap; max-width:560px;">
      <div style="flex:1; min-width:200px;">
        <label>New category</label>
        <input type="text" v-model="newName" placeholder="e.g. Countries by population" />
      </div>
      <div style="flex:1; min-width:160px;">
        <label>Grouping word <span class="picker-hint">optional, defaults to "Team"</span></label>
        <input type="text" v-model="newGroupLabel" placeholder="e.g. Continent" @keyup.enter="createCategory" />
      </div>
      <button class="btn btn-primary" :disabled="!newName.trim() || saving" @click="createCategory">+ Add</button>
    </div>

    <div v-if="loading" style="color:var(--text-dim); margin-top:20px;">Loading…</div>
    <div v-else-if="!categories.length" class="empty-state friendly">No categories yet - add your first one above.</div>

    <div v-else class="saved-quiz-list" style="margin-top:20px;">
      <div v-for="c in categories" :key="c.id" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <template v-if="editingId === c.id">
            <input
              type="text"
              v-model="editingName"
              class="saved-quiz-title"
              style="width:100%; background:transparent; border:1px solid var(--border); border-radius:6px; padding:4px 8px; margin-bottom:6px;"
              @keyup.esc="editingId = null"
            />
            <input
              type="text"
              v-model="editingGroupLabel"
              placeholder="Grouping word, e.g. Team"
              style="width:100%; background:transparent; border:1px solid var(--border); border-radius:6px; padding:4px 8px; font-size:0.85rem;"
              @keyup.enter="confirmRename(c)"
              @keyup.esc="editingId = null"
            />
          </template>
          <template v-else>
            <div class="saved-quiz-title">{{ c.name }}</div>
            <div class="saved-quiz-meta">Grouping word: {{ c.groupLabel }}</div>
          </template>
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
      :message="`'${pendingDelete.name}' will be removed. If any subject, club, grid, or pool still uses it, this will be blocked until you reassign them first.`"
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
const newGroupLabel = ref('')
const editingId = ref(null)
const editingName = ref('')
const editingGroupLabel = ref('')
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
    await api.adminCreateGridCategory(name, newGroupLabel.value.trim())
    newName.value = ''
    newGroupLabel.value = ''
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
  // Pre-fill the existing value - critical so saving without touching this
  // field doesn't accidentally reset a custom grouping word back to "Team".
  editingGroupLabel.value = c.groupLabel
}

async function confirmRename(c) {
  const name = editingName.value.trim()
  if (!name) return
  saving.value = true
  error.value = ''
  try {
    await api.adminUpdateGridCategory(c.id, name, editingGroupLabel.value.trim())
    editingId.value = null
    toast.show('Category updated - changes applied everywhere it was already used.')
    gridCategories.invalidate()
    loadCategories()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not update that category.'
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
