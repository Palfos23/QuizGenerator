<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:12px;">
      <h1 style="margin:0;">Meld fra om et problem</h1>
      <router-link to="/dashboard" class="btn btn-secondary btn-sm">← Tilbake til oversikten</router-link>
    </div>
    <p class="page-subtitle">
      Funnet en feil, noe forvirrende, eller innhold som må rettes? Si fra til oss.
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field">
      <label>Hvilken del av appen? <span class="picker-hint">valgfritt</span>
        <select v-model="form.area">
          <option value="">Usikker / generelt</option>
          <option>Lag en quiz</option>
          <option>Mine quizer</option>
          <option>Ukens grid</option>
          <option>Tension</option>
          <option>Foreslå et spørsmål</option>
          <option>Administrasjonssider</option>
          <option>Noe annet</option>
        </select>
      </label>
    </div>

    <div class="field">
      <label>Hva skjedde?<textarea v-model="form.message" rows="5" placeholder="Beskriv problemet - jo flere detaljer, jo lettere er det å spore opp."></textarea></label>
    </div>

    <button class="btn btn-primary" :disabled="submitting || !form.message.trim()" @click="submit">
      {{ submitting ? 'Sender inn…' : 'Send inn rapport' }}
    </button>

    <h2 style="margin-top:40px;">Dine rapporter</h2>

    <div v-if="loading" style="color:var(--text-dim);">Laster…</div>
    <div v-else-if="!reports.length" class="empty-state">Du har ikke meldt fra om noe ennå.</div>

    <div v-else class="saved-quiz-list">
      <div v-for="r in reports" :key="r.id" class="saved-quiz-row" style="align-items:flex-start;">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ r.message }}</div>
          <div class="saved-quiz-meta">{{ r.area || 'Generelt' }}</div>
          <div v-if="r.status === 'RESOLVED' && r.adminNote" style="color:var(--teal); font-size:0.85rem; margin-top:6px;">
            Løst: {{ r.adminNote }}
          </div>
        </div>
        <span class="tag" :style="statusStyle(r.status)">{{ r.status === 'RESOLVED' ? 'Løst' : 'Åpen' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'

const form = reactive({ area: '', message: '' })
const submitting = ref(false)
const error = ref('')
const reports = ref([])
const loading = ref(true)

onMounted(loadMine)

async function loadMine() {
  loading.value = true
  try {
    reports.value = await api.myReports()
  } catch (e) {
    error.value = 'Kunne ikke laste rapportene dine.'
  } finally {
    loading.value = false
  }
}

async function submit() {
  error.value = ''
  if (!form.message.trim()) {
    error.value = 'Beskriv hva som skjedde.'
    return
  }
  submitting.value = true
  try {
    await api.submitReport(form)
    toast.show('Rapporten er sendt inn.')
    form.area = ''
    form.message = ''
    loadMine()
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke sende inn rapporten.'
  } finally {
    submitting.value = false
  }
}

function statusStyle(status) {
  if (status === 'RESOLVED') return { background: 'rgba(31, 122, 80, 0.15)', color: 'var(--teal)' }
  return { background: 'rgba(216, 67, 43, 0.15)', color: 'var(--gold)' }
}
</script>
