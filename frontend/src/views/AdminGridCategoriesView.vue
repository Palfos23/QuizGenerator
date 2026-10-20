<template>
  <div>
    <h1 style="margin:0;">Grid-kategorier</h1>
    <p class="page-subtitle">
      Listen over kategorier som er tilgjengelige når du lager emner, klubber, grids og utvalg - ikke lenger begrenset til bare fotball og sykling. Å gi en kategori nytt navn her oppdaterer automatisk alle emner, klubber, grids og utvalg som allerede bruker den. Hver kategori har også sitt eget «grupperings»-ord - «Team» for en sport, «Continent» for land, «Label» for artister - brukt til funksjonen for å legge til en hel gruppe og vist ved siden av hvert emne.
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field" style="display:flex; gap:10px; align-items:flex-end; flex-wrap:wrap; max-width:560px;">
      <div style="flex:1; min-width:200px;">
        <label>Ny kategori<input type="text" v-model="newName" placeholder="f.eks. Land etter folketall" /></label>
      </div>
      <div style="flex:1; min-width:160px;">
        <label>Grupperingsord <span class="picker-hint">valgfritt, standard er «Team»</span><input type="text" v-model="newGroupLabel" placeholder="f.eks. Kontinent" @keyup.enter="createCategory" /></label>
      </div>
      <button class="btn btn-primary" :disabled="!newName.trim() || saving" @click="createCategory">+ Legg til</button>
    </div>

    <div v-if="loading" style="color:var(--text-dim); margin-top:20px;">Laster…</div>
    <div v-else-if="!categories.length" class="empty-state friendly">Ingen kategorier ennå - legg til din første over.</div>

    <div v-else class="saved-quiz-list" style="margin-top:20px;">
      <div v-for="c in categories" :key="c.id" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <template v-if="editingId === c.id">
            <input
              type="text"
              v-model="editingName"
              class="saved-quiz-title"
              style="width:100%; background:transparent; border:1px solid var(--border); border-radius: 2px; padding:4px 8px; margin-bottom:6px;"
              @keyup.esc="editingId = null"
            />
            <input
              type="text"
              v-model="editingGroupLabel"
              placeholder="Grupperingsord, f.eks. Lag"
              style="width:100%; background:transparent; border:1px solid var(--border); border-radius: 2px; padding:4px 8px; font-size:0.85rem;"
              @keyup.enter="confirmRename(c)"
              @keyup.esc="editingId = null"
            />
          </template>
          <template v-else>
            <div class="saved-quiz-title">{{ c.name }}</div>
            <div class="saved-quiz-meta">Grupperingsord: {{ c.groupLabel }}</div>
          </template>
        </div>
        <div style="display:flex; gap:8px;">
          <template v-if="editingId === c.id">
            <button class="btn btn-primary btn-sm" :disabled="!editingName.trim() || saving" @click="confirmRename(c)">Lagre</button>
            <button class="btn btn-secondary btn-sm" @click="editingId = null">Avbryt</button>
          </template>
          <template v-else>
            <button class="btn btn-secondary btn-sm" @click="startRename(c)">Gi nytt navn</button>
            <button class="btn btn-danger btn-sm" @click="requestDelete(c)">Slett</button>
          </template>
        </div>
      </div>
    </div>

    <ConfirmModal
      v-if="pendingDelete"
      title="Slette denne kategorien?"
      :message="`«${pendingDelete.name}» fjernes. Hvis et emne, en klubb, et grid eller et utvalg fortsatt bruker den, blokkeres dette til du har flyttet dem først.`"
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
    error.value = 'Kunne ikke laste kategorier.'
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
    toast.show('Kategorien er lagt til.')
    gridCategories.invalidate()
    loadCategories()
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke legge til kategorien.'
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
    toast.show('Kategorien er oppdatert - endringene er brukt overalt den allerede ble brukt.')
    gridCategories.invalidate()
    loadCategories()
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke oppdatere kategorien.'
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
    toast.show('Kategorien er slettet.')
    gridCategories.invalidate()
    loadCategories()
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke slette kategorien.'
  }
}
</script>
