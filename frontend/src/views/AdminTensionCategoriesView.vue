<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
      <div>
        <h1>{{ $t('Tension-svarkategorier') }}</h1>
        <p class="page-subtitle">
          {{ $t('Ordlister som driver autofullføringen i svarfeltet under spilling - ikke nødvendigvis alle riktige svar, bare sannsynlige ord i samme kategori å skrive fra.') }}
          <router-link to="/admin/tension-questions">{{ $t('← Tilbake til spørsmål') }}</router-link>
        </p>
      </div>
      <button class="btn btn-primary" @click="openCreate">{{ $t('+ Legg til kategori') }}</button>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

    <div v-else-if="!categories.length" class="empty-state friendly">
      {{ $t('Ingen svarkategorier ennå. Legg til en (f.eks. "land") med en liste over forslagsord.') }}
    </div>

    <div v-else>
      <input
        type="text"
        v-model="categorySearchTerm"
        :placeholder="$t('Søk etter kategorinavn…')"
        class="search-input"
        style="margin-bottom:16px;"
      />

      <div v-if="!filteredCategories.length" class="empty-state" style="padding:20px;">
        {{ $t('Ingen kategorier samsvarer med "') }}{{ categorySearchTerm }}".
      </div>

      <div v-else class="saved-quiz-list">
        <div v-for="c in pagedCategories" :key="c.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ c.name }}</div>
            <div class="saved-quiz-meta">{{ c.optionCount }} {{ $t('forslagsord') }}</div>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-secondary btn-sm" @click="openEdit(c)">{{ $t('Rediger') }}</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(c)">{{ $t('Slett') }}</button>
          </div>
        </div>
      </div>

      <Pagination v-model:page="categoryPage" :page-size="CATEGORY_PAGE_SIZE" :total-items="filteredCategories.length" />
    </div>

    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal">
        <h2>{{ editingId ? $t('Rediger kategori') : $t('Legg til kategori') }}</h2>
        <div v-if="modalError" class="banner error">{{ modalError }}</div>

        <div class="field">
          <label>{{ $t('Navn') }}<input type="text" v-model="form.name" :placeholder="$t('f.eks. land')" /></label>
        </div>

        <div class="field">
          <label>{{ $t('Forslagsord') }} <span class="picker-hint">{{ $t('ett per linje') }}</span><textarea v-model="optionsText" rows="10" :placeholder="$t('Norge\nSverige\nDanmark\n…')"></textarea></label>
        </div>

        <div style="display:flex; gap:10px; justify-content:flex-end;">
          <button class="btn btn-secondary" @click="showModal = false">{{ $t('Avbryt') }}</button>
          <button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? $t('Lagrer…') : $t('Lagre') }}</button>
        </div>
      </div>
    </div>

    <ConfirmModal
      v-if="pendingDelete"
      :title="$t('Slette denne kategorien?')"
      :message="$t('«{0}» fjernes. Spørsmål som bruker den mister forslagene i svarfeltet.', [pendingDelete.name])"
      @confirm="doDelete"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import ConfirmModal from '../components/ConfirmModal.vue'
import Pagination from '../components/Pagination.vue'

const categories = ref([])

const categorySearchTerm = ref('')
const filteredCategories = computed(() => {
  const term = categorySearchTerm.value.trim().toLowerCase()
  if (!term) return categories.value
  return categories.value.filter(c => c.name.toLowerCase().includes(term))
})
const CATEGORY_PAGE_SIZE = 25
const categoryPage = ref(1)
const pagedCategories = computed(() => {
  const start = (categoryPage.value - 1) * CATEGORY_PAGE_SIZE
  return filteredCategories.value.slice(start, start + CATEGORY_PAGE_SIZE)
})
watch(categorySearchTerm, () => { categoryPage.value = 1 })
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
    error.value = _t('Kunne ikke laste kategorier.')
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

async function openEdit(c) {
  modalError.value = ''
  try {
    const detail = await api.adminGetTensionCategory(c.id)
    form.name = detail.name
    optionsText.value = detail.options.join('\n')
    editingId.value = detail.id
    showModal.value = true
  } catch (e) {
    error.value = _t('Kunne ikke laste kategorien.')
  }
}

async function save() {
  modalError.value = ''
  if (!form.name.trim()) {
    modalError.value = _t('Navn må fylles ut.')
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
    toast.show(_t('Kategorien er lagret.'))
    showModal.value = false
    loadCategories()
  } catch (e) {
    modalError.value = e.response?.data?.message || _t('Kunne ikke lagre kategorien.§AdminTensionCategoriesViewJ')
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
    toast.show(_t('Kategorien er slettet.'))
    loadCategories()
  } catch (e) {
    error.value = _t('Kunne ikke slette kategorien.')
  }
}
</script>
