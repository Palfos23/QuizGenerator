<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
      <div>
        <h1>{{ $t('Klubber') }}</h1>
        <p class="page-subtitle">{{ $t('Emblemer brukt som logohint på rutene i ukens grid.') }}</p>
      </div>
      <button class="btn btn-primary" @click="openCreate">{{ $t('+ Legg til klubb') }}</button>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="filter-bar">
      <div class="field" style="margin-bottom:0; flex:2; min-width:200px;">
        <label>{{ $t('Søk') }}<input type="text" v-model="searchText" :placeholder="$t('Søk etter navn…')" /></label>
      </div>
      <div class="field" style="margin-bottom:0; flex:1; min-width:160px;">
        <label>{{ $t('Kategori') }}
          <select v-model="sportFilter">
            <option value="ALL">{{ $t('Alle kategorier') }}</option>
            <option v-for="s in gridCategories.categories.value" :key="s" :value="s">{{ s }}</option>
          </select>
        </label>
      </div>
    </div>

    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

    <div v-else-if="!clubs.length" class="empty-state friendly">
      {{ $t('Ingen klubber ennå. Legg til en her, og velg den som logohint når du lager et ukentlig grid.') }}
    </div>

    <div v-else-if="!filteredClubs.length" class="empty-state">{{ $t('Ingen klubber samsvarer med disse filtrene.') }}</div>

    <div v-else class="table-scroll">
      <table class="table">
        <thead>
          <tr>
            <th>Logo</th>
            <th>{{ $t('Navn') }}</th>
            <th>{{ $t('Kategori') }}</th>
            <th>{{ $t('Farge') }}</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in pagedClubs" :key="c.id">
            <td>
              <img v-if="c.logoUrl" :src="c.logoUrl" alt="" class="club-logo-thumb" />
              <span v-else class="photo-thumb-placeholder" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>
              </span>
            </td>
            <td>{{ c.name }}</td>
            <td>{{ sportLabel(c.sport) }}</td>
            <td><span class="color-swatch" :style="{ background: c.color || 'var(--gold)' }"></span></td>
            <td style="white-space:nowrap;">
              <button class="btn btn-secondary btn-sm" @click="openEdit(c)">{{ $t('Rediger') }}</button>
              <button class="btn btn-danger btn-sm" style="margin-left:6px;" @click="requestDelete(c)">{{ $t('Slett') }}</button>
            </td>
          </tr>
        </tbody>
      </table>

      <Pagination v-model:page="page" :page-size="PAGE_SIZE" :total-items="filteredClubs.length" />
    </div>

    <ClubFormModal
      v-if="showModal"
      :club="editingClub"
      @close="showModal = false"
      @saved="onSaved"
    />

    <ConfirmModal
      v-if="pendingDelete"
      :title="$t('Slette denne klubben?')"
      :message="$t('«{0}» fjernes.', [pendingDelete.name])"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, onMounted, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import ClubFormModal from '../components/ClubFormModal.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import Pagination from '../components/Pagination.vue'
import { sportLabel } from '../constants'
import gridCategories from '../services/gridCategories'

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

const PAGE_SIZE = 15
const page = ref(1)
const pagedClubs = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return filteredClubs.value.slice(start, start + PAGE_SIZE)
})
watch([searchText, sportFilter], () => { page.value = 1 })

onMounted(() => {
  loadClubs()
  gridCategories.ensureLoaded()
})

async function loadClubs() {
  loading.value = true
  error.value = ''
  try {
    clubs.value = await api.adminSearchClubs()
  } catch (e) {
    error.value = _t('Kunne ikke laste klubber.')
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
  toast.show(_t('Klubben er lagret.'))
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
    toast.show(_t('Klubben er slettet.'))
    loadClubs()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke slette klubben.')
  }
}
</script>
