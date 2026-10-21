<template>
  <div>
    <h1>{{ $t('Rapporter') }}</h1>
    <p class="page-subtitle">{{ $t('Problemer og tilbakemeldinger sendt inn av brukere.') }}</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

    <div v-else-if="!reports.length" class="empty-state friendly">{{ $t('Ingen rapporter ennå.') }}</div>

    <div v-else class="saved-quiz-list">
      <div v-for="r in reports" :key="r.id" class="saved-quiz-row" style="align-items:flex-start;">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ r.message }}</div>
          <div class="saved-quiz-meta">{{ r.area || $t('Generelt') }} {{ $t('· meldt av') }} {{ r.reporterName }}</div>
          <div v-if="r.status === 'RESOLVED' && r.adminNote" style="color:var(--teal); font-size:0.85rem; margin-top:6px;">
            {{ $t('Løst:') }} {{ r.adminNote }}
          </div>
        </div>
        <div style="display:flex; flex-direction:column; align-items:flex-end; gap:8px;">
          <span class="tag" :style="statusStyle(r.status)">{{ r.status === 'RESOLVED' ? $t('Løst') : $t('Åpen') }}</span>
          <button v-if="r.status === 'OPEN'" class="btn btn-primary btn-sm" @click="openResolve(r)">{{ $t('Marker som løst') }}</button>
        </div>
      </div>
    </div>

    <div v-if="resolving" class="modal-backdrop" @click.self="resolving = null">
      <div class="modal">
        <h2>{{ $t('Markere denne rapporten som løst?') }}</h2>
        <div class="field">
          <label>{{ $t('Notat') }} <span class="picker-hint">{{ $t('valgfritt - vises til den som meldte fra') }}</span><textarea v-model="resolveNote" rows="3" :placeholder="$t('f.eks. Rettet i siste oppdatering')"></textarea></label>
        </div>
        <div style="display:flex; gap:10px; justify-content:flex-end;">
          <button class="btn btn-secondary" @click="resolving = null">{{ $t('Avbryt') }}</button>
          <button class="btn btn-primary" @click="confirmResolve">{{ $t('Marker som løst') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
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
    error.value = _t('Kunne ikke laste rapporter.')
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
    toast.show(_t('Markert som løst.'))
    resolving.value = null
    load()
  } catch (e) {
    error.value = _t('Kunne ikke oppdatere rapporten.')
  }
}

function statusStyle(status) {
  if (status === 'RESOLVED') return { background: 'rgb(var(--teal-rgb) / 0.15)', color: 'var(--teal)' }
  return { background: 'rgb(var(--gold-rgb) / 0.15)', color: 'var(--gold)' }
}
</script>
