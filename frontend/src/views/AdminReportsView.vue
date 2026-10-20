<template>
  <div>
    <h1>Rapporter</h1>
    <p class="page-subtitle">Problemer og tilbakemeldinger sendt inn av brukere.</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Laster…</div>

    <div v-else-if="!reports.length" class="empty-state friendly">Ingen rapporter ennå.</div>

    <div v-else class="saved-quiz-list">
      <div v-for="r in reports" :key="r.id" class="saved-quiz-row" style="align-items:flex-start;">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ r.message }}</div>
          <div class="saved-quiz-meta">{{ r.area || 'Generelt' }} · meldt av {{ r.reporterName }}</div>
          <div v-if="r.status === 'RESOLVED' && r.adminNote" style="color:var(--teal); font-size:0.85rem; margin-top:6px;">
            Løst: {{ r.adminNote }}
          </div>
        </div>
        <div style="display:flex; flex-direction:column; align-items:flex-end; gap:8px;">
          <span class="tag" :style="statusStyle(r.status)">{{ r.status === 'RESOLVED' ? 'Løst' : 'Åpen' }}</span>
          <button v-if="r.status === 'OPEN'" class="btn btn-primary btn-sm" @click="openResolve(r)">Marker som løst</button>
        </div>
      </div>
    </div>

    <div v-if="resolving" class="modal-backdrop" @click.self="resolving = null">
      <div class="modal">
        <h2>Markere denne rapporten som løst?</h2>
        <div class="field">
          <label>Notat <span class="picker-hint">valgfritt - vises til den som meldte fra</span><textarea v-model="resolveNote" rows="3" placeholder="f.eks. Rettet i siste oppdatering"></textarea></label>
        </div>
        <div style="display:flex; gap:10px; justify-content:flex-end;">
          <button class="btn btn-secondary" @click="resolving = null">Avbryt</button>
          <button class="btn btn-primary" @click="confirmResolve">Marker som løst</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'

const reports = ref([])
const loading = ref(true)
const error = ref('')
const resolving = ref(null)
const resolveNote = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    reports.value = await api.adminListReports()
  } catch (e) {
    error.value = 'Kunne ikke laste rapporter.'
  } finally {
    loading.value = false
  }
}

function openResolve(r) {
  resolving.value = r
  resolveNote.value = ''
}

async function confirmResolve() {
  error.value = ''
  try {
    await api.adminResolveReport(resolving.value.id, resolveNote.value)
    toast.show('Markert som løst.')
    resolving.value = null
    load()
  } catch (e) {
    error.value = 'Kunne ikke oppdatere rapporten.'
  }
}

function statusStyle(status) {
  if (status === 'RESOLVED') return { background: 'rgba(31, 122, 80, 0.15)', color: 'var(--teal)' }
  return { background: 'rgba(216, 67, 43, 0.15)', color: 'var(--gold)' }
}
</script>
