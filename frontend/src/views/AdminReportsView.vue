<template>
  <div>
    <h1>Reports</h1>
    <p class="page-subtitle">Problems and feedback submitted by users.</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="!reports.length" class="empty-state friendly">No reports yet.</div>

    <div v-else class="saved-quiz-list">
      <div v-for="r in reports" :key="r.id" class="saved-quiz-row" style="align-items:flex-start;">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ r.message }}</div>
          <div class="saved-quiz-meta">{{ r.area || 'General' }} · reported by {{ r.reporterName }}</div>
          <div v-if="r.status === 'RESOLVED' && r.adminNote" style="color:var(--teal); font-size:0.85rem; margin-top:6px;">
            Resolved: {{ r.adminNote }}
          </div>
        </div>
        <div style="display:flex; flex-direction:column; align-items:flex-end; gap:8px;">
          <span class="tag" :style="statusStyle(r.status)">{{ r.status === 'RESOLVED' ? 'Resolved' : 'Open' }}</span>
          <button v-if="r.status === 'OPEN'" class="btn btn-primary btn-sm" @click="openResolve(r)">Mark resolved</button>
        </div>
      </div>
    </div>

    <div v-if="resolving" class="modal-backdrop" @click.self="resolving = null">
      <div class="modal">
        <h2>Mark this report resolved?</h2>
        <div class="field">
          <label>Note <span class="picker-hint">optional - shown back to the reporter</span></label>
          <textarea v-model="resolveNote" rows="3" placeholder="e.g. Fixed in the latest update"></textarea>
        </div>
        <div style="display:flex; gap:10px; justify-content:flex-end;">
          <button class="btn btn-secondary" @click="resolving = null">Cancel</button>
          <button class="btn btn-primary" @click="confirmResolve">Mark resolved</button>
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
    error.value = 'Could not load reports.'
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
    toast.show('Marked resolved.')
    resolving.value = null
    load()
  } catch (e) {
    error.value = 'Could not update that report.'
  }
}

function statusStyle(status) {
  if (status === 'RESOLVED') return { background: 'rgba(61,220,151,0.15)', color: 'var(--teal)' }
  return { background: 'rgba(242,183,5,0.15)', color: 'var(--gold)' }
}
</script>
