<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
      <div>
        <h1>Athletes</h1>
        <p class="page-subtitle">The roster used to build weekly grid candidate pools.</p>
      </div>
      <button class="btn btn-primary" @click="openCreate">+ Add athlete</button>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="filter-bar">
      <div class="field" style="margin-bottom:0; flex:2; min-width:200px;">
        <label>Search</label>
        <input type="text" v-model="searchText" placeholder="Search name or team…" />
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

    <div v-else-if="!athletes.length" class="empty-state friendly">
      No athletes yet. Add a few here before building a weekly grid.
    </div>

    <div v-else-if="!filteredAthletes.length" class="empty-state">No athletes match those filters.</div>

    <div v-else class="table-scroll">
      <table class="table">
        <thead>
          <tr>
            <th>Photo</th>
            <th>Name</th>
            <th>Sport</th>
            <th>Team</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in pagedAthletes" :key="a.id">
            <td><img v-if="a.photoUrl" :src="a.photoUrl" alt="" class="club-logo-thumb" /></td>
            <td>{{ a.name }}</td>
            <td>{{ sportLabel(a.sport) }}</td>
            <td>{{ a.team }}</td>
            <td style="white-space:nowrap;">
              <button class="btn btn-secondary btn-sm" @click="openEdit(a)">Edit</button>
              <button class="btn btn-danger btn-sm" style="margin-left:6px;" @click="requestDelete(a)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>

      <Pagination v-model:page="page" :page-size="PAGE_SIZE" :total-items="filteredAthletes.length" />
    </div>

    <AthleteFormModal
      v-if="showModal"
      :athlete="editingAthlete"
      @close="showModal = false"
      @saved="onSaved"
    />

    <ConfirmModal
      v-if="pendingDelete"
      title="Delete this athlete?"
      :message="`'${pendingDelete.name}' will be removed from the roster.`"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import AthleteFormModal from '../components/AthleteFormModal.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import Pagination from '../components/Pagination.vue'
import { SPORTS, sportLabel } from '../constants'

const athletes = ref([])
const loading = ref(true)
const error = ref('')
const searchText = ref('')
const sportFilter = ref('ALL')

const showModal = ref(false)
const editingAthlete = ref(null)
const pendingDelete = ref(null)

const filteredAthletes = computed(() => {
  const term = searchText.value.trim().toLowerCase()
  return athletes.value.filter(a => {
    if (sportFilter.value !== 'ALL' && a.sport !== sportFilter.value) return false
    if (term && !`${a.name} ${a.team || ''}`.toLowerCase().includes(term)) return false
    return true
  })
})

const PAGE_SIZE = 15
const page = ref(1)
const pagedAthletes = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return filteredAthletes.value.slice(start, start + PAGE_SIZE)
})
watch([searchText, sportFilter], () => { page.value = 1 })

onMounted(loadAthletes)

async function loadAthletes() {
  loading.value = true
  error.value = ''
  try {
    athletes.value = await api.adminSearchAthletes()
  } catch (e) {
    error.value = 'Could not load athletes.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingAthlete.value = null
  showModal.value = true
}

function openEdit(a) {
  editingAthlete.value = a
  showModal.value = true
}

function onSaved() {
  showModal.value = false
  toast.show('Athlete saved.')
  loadAthletes()
}

function requestDelete(a) {
  pendingDelete.value = a
}

async function doDelete() {
  const a = pendingDelete.value
  pendingDelete.value = null
  error.value = ''
  try {
    await api.adminDeleteAthlete(a.id)
    toast.show('Athlete deleted.')
    loadAthletes()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not delete that athlete.'
  }
}
</script>
