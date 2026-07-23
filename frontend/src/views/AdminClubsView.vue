<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px;">
      <div>
        <h1>Clubs</h1>
        <p class="page-subtitle">Crests used as logo hints on weekly grid tiles.</p>
      </div>
      <button class="btn btn-primary" @click="openCreate">+ Add club</button>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="filter-bar">
      <div class="field" style="margin-bottom:0; flex:2; min-width:200px;">
        <label>Search</label>
        <input type="text" v-model="searchText" placeholder="Search name…" />
      </div>
      <div class="field" style="margin-bottom:0; flex:1; min-width:160px;">
        <label>Sport</label>
        <select v-model="sportFilter">
          <option value="ALL">All sports</option>
          <option v-for="s in SPORTS" :key="s.code" :value="s.code">{{ s.label }}</option>
        </select>
      </div>
    </div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="!clubs.length" class="empty-state friendly">
      No clubs yet. Add one here, then pick it as a logo hint when building a weekly grid.
    </div>

    <div v-else-if="!filteredClubs.length" class="empty-state">No clubs match those filters.</div>

    <div v-else class="table-scroll">
      <table class="table">
        <thead>
          <tr>
            <th>Logo</th>
            <th>Name</th>
            <th>Sport</th>
            <th>Color</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in filteredClubs" :key="c.id">
            <td><img v-if="c.logoUrl" :src="c.logoUrl" alt="" class="club-logo-thumb" /></td>
            <td>{{ c.name }}</td>
            <td>{{ sportLabel(c.sport) }}</td>
            <td><span class="color-swatch" :style="{ background: c.color || 'var(--gold)' }"></span></td>
            <td style="white-space:nowrap;">
              <button class="btn btn-secondary btn-sm" @click="openEdit(c)">Edit</button>
              <button class="btn btn-danger btn-sm" style="margin-left:6px;" @click="requestDelete(c)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <ClubFormModal
      v-if="showModal"
      :club="editingClub"
      @close="showModal = false"
      @saved="onSaved"
    />

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this club?"
      :message="`'${pendingDelete.name}' will be removed.`"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import ClubFormModal from '../components/ClubFormModal.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { SPORTS, sportLabel } from '../constants'

const clubs = ref([])
const loading = ref(true)
const error = ref('')
const searchText = ref('')
const sportFilter = ref('ALL')

const showModal = ref(false)
const editingClub = ref(null)
const pendingDelete = ref(null)

const filteredClubs = computed(() => {
  const term = searchText.value.trim().toLowerCase()
  return clubs.value.filter(c => {
    if (sportFilter.value !== 'ALL' && c.sport !== sportFilter.value) return false
    if (term && !c.name.toLowerCase().includes(term)) return false
    return true
  })
})

onMounted(loadClubs)

async function loadClubs() {
  loading.value = true
  error.value = ''
  try {
    clubs.value = await api.adminSearchClubs()
  } catch (e) {
    error.value = 'Could not load clubs.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingClub.value = null
  showModal.value = true
}

function openEdit(c) {
  editingClub.value = c
  showModal.value = true
}

function onSaved() {
  showModal.value = false
  toast.show('Club saved.')
  loadClubs()
}

function requestDelete(c) {
  pendingDelete.value = c
}

async function doDelete() {
  const c = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.adminDeleteClub(c.id)
    toast.show('Club deleted.')
    loadClubs()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not delete that club.'
  }
}
</script>
