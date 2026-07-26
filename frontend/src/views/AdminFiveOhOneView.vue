<template>
  <div>
    <template v-if="view === 'list'">
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
        <h1 style="margin:0;">501 categories</h1>
        <button class="btn btn-primary" @click="openCreate">+ New category</button>
      </div>
      <p class="page-subtitle">Each category is a ranked set of names and numbers - e.g. "Premier League appearances".</p>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
      <div v-else-if="!categories.length" class="empty-state friendly">No categories yet - create the first one.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="c in categories" :key="c.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ c.title }}</div>
            <div class="saved-quiz-meta">{{ c.entryCount }} entries<span v-if="c.description"> · {{ c.description }}</span></div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(c.id)">Edit</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(c)">Delete</button>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <h1 style="margin:0;">{{ editingId ? 'Edit category' : 'New category' }}</h1>
        <button class="btn btn-secondary" @click="view = 'list'">← Back to list</button>
      </div>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Title</label>
        <input type="text" v-model="form.title" placeholder="e.g. Premier League appearances" />
      </div>

      <div class="field">
        <label>Description <span class="picker-hint">optional flavor text</span></label>
        <input type="text" v-model="form.description" placeholder="Shown to players before they pick this category" />
      </div>

      <details class="advanced-disclosure">
        <summary>Bulk paste entries</summary>
        <div style="margin-top:12px;">
          <p class="page-subtitle" style="margin-top:0;">One per line, as <code>Name, Number</code> - e.g. <code>Mohamed Salah, 233</code>. Adds to (or updates) what's below, doesn't replace it.</p>
          <textarea v-model="bulkText" rows="6" placeholder="Mohamed Salah, 233&#10;Harry Kane, 189"></textarea>
          <button class="btn btn-secondary btn-sm" style="margin-top:8px;" @click="applyBulkPaste">Add to list</button>
        </div>
      </details>

      <div class="field" style="margin-top:20px;">
        <label>Entries <span class="picker-hint">{{ entries.length }} total</span></label>
        <div v-if="!entries.length" class="empty-state" style="padding:20px;">No entries yet - paste some above, or add one at a time below.</div>
        <div v-else class="candidate-list">
          <div v-for="(e, idx) in pagedEntries" :key="idx" class="candidate-row">
            <input type="text" v-model="e.name" placeholder="Name" style="flex:1;" />
            <input type="number" v-model.number="e.value" placeholder="Value" style="width:100px;" />
            <button class="btn btn-danger btn-sm" @click="removeEntry(e)">✕</button>
          </div>
          <Pagination v-model:page="entryPage" :page-size="ENTRY_PAGE_SIZE" :total-items="entries.length" />
        </div>
        <button class="btn btn-secondary btn-sm" style="margin-top:10px;" @click="addBlankEntry">+ Add one manually</button>
      </div>

      <button class="btn btn-primary" :disabled="saving" @click="saveCategory" style="margin-top:20px;">
        {{ saving ? 'Saving…' : 'Save category' }}
      </button>
    </template>

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this category?"
      :message="`'${pendingDelete.title}' and all ${pendingDelete.entryCount} entries will be removed.`"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import ConfirmModal from '../components/ConfirmModal.vue'
import Pagination from '../components/Pagination.vue'

const view = ref('list')
const categories = ref([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const editingId = ref(null)
const pendingDelete = ref(null)

const form = reactive({ title: '', description: '' })
const entries = ref([]) // [{ name, value }]
const bulkText = ref('')

const ENTRY_PAGE_SIZE = 25
const entryPage = ref(1)
const pagedEntries = computed(() => {
  const start = (entryPage.value - 1) * ENTRY_PAGE_SIZE
  return entries.value.slice(start, start + ENTRY_PAGE_SIZE)
})

onMounted(loadCategories)

async function loadCategories() {
  loading.value = true
  error.value = ''
  try {
    categories.value = await api.adminListFiveOhOneCategories()
  } catch (e) {
    error.value = 'Could not load categories.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.title = ''
  form.description = ''
  entries.value = []
  bulkText.value = ''
  entryPage.value = 1
  error.value = ''
  view.value = 'form'
}

async function openEdit(id) {
  error.value = ''
  try {
    const detail = await api.adminGetFiveOhOneCategory(id)
    editingId.value = id
    form.title = detail.title
    form.description = detail.description || ''
    entries.value = detail.entries.map(e => ({ name: e.name, value: e.value }))
    bulkText.value = ''
    entryPage.value = 1
    view.value = 'form'
  } catch (e) {
    error.value = 'Could not load that category.'
  }
}

function applyBulkPaste() {
  const lines = bulkText.value.split('\n').map(l => l.trim()).filter(Boolean)
  const byName = new Map(entries.value.map(e => [e.name.toLowerCase(), e]))
  for (const line of lines) {
    const commaIdx = line.lastIndexOf(',')
    if (commaIdx === -1) continue
    const name = line.slice(0, commaIdx).trim()
    const value = parseInt(line.slice(commaIdx + 1).trim(), 10)
    if (!name || Number.isNaN(value)) continue
    const existing = byName.get(name.toLowerCase())
    if (existing) {
      existing.value = value
    } else {
      const fresh = { name, value }
      entries.value.push(fresh)
      byName.set(name.toLowerCase(), fresh)
    }
  }
  bulkText.value = ''
  toast.show(`Added/updated ${lines.length} line(s).`)
}

function addBlankEntry() {
  entries.value.push({ name: '', value: 0 })
  entryPage.value = Math.max(1, Math.ceil(entries.value.length / ENTRY_PAGE_SIZE))
}

function removeEntry(e) {
  const idx = entries.value.indexOf(e)
  if (idx !== -1) entries.value.splice(idx, 1)
  const maxPage = Math.max(1, Math.ceil(entries.value.length / ENTRY_PAGE_SIZE))
  if (entryPage.value > maxPage) entryPage.value = maxPage
}

async function saveCategory() {
  error.value = ''
  const cleanEntries = entries.value
    .map(e => ({ name: e.name.trim(), value: e.value }))
    .filter(e => e.name && Number.isFinite(e.value))

  if (!form.title.trim()) {
    error.value = 'Give this category a title.'
    return
  }
  if (!cleanEntries.length) {
    error.value = 'Add at least one entry.'
    return
  }

  saving.value = true
  try {
    const payload = { title: form.title.trim(), description: form.description.trim() || null, entries: cleanEntries }
    if (editingId.value) {
      await api.adminUpdateFiveOhOneCategory(editingId.value, payload)
      toast.show('Category updated.')
    } else {
      await api.adminCreateFiveOhOneCategory(payload)
      toast.show('Category created.')
    }
    view.value = 'list'
    loadCategories()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not save that category.'
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
  try {
    await api.adminDeleteFiveOhOneCategory(c.id)
    toast.show('Category deleted.')
    loadCategories()
  } catch (e) {
    error.value = 'Could not delete that category.'
  }
}
</script>
