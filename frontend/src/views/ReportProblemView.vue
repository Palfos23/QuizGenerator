<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:12px;">
      <h1 style="margin:0;">{{ $t('Meld fra om et problem') }}</h1>
      <router-link to="/dashboard" class="btn btn-secondary btn-sm">{{ $t('← Tilbake til oversikten') }}</router-link>
    </div>
    <p class="page-subtitle">
      {{ $t('Funnet en feil, noe forvirrende, eller innhold som må rettes? Si fra til oss.') }}
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field">
      <label>{{ $t('Hvilken del av appen?') }} <span class="picker-hint">{{ $t('valgfritt') }}</span>
        <select v-model="form.area">
          <option value="">{{ $t('Usikker / generelt') }}</option>
          <option>{{ $t('Lag en quiz') }}</option>
          <option>{{ $t('Mine quizer') }}</option>
          <option>{{ $t('Ukens grid§ReportProblemViewT') }}</option>
          <option>Tension</option>
          <option>{{ $t('Foreslå et spørsmål') }}</option>
          <option>{{ $t('Administrasjonssider') }}</option>
          <option>{{ $t('Noe annet') }}</option>
        </select>
      </label>
    </div>

    <div class="field">
      <label>{{ $t('Hva skjedde?') }}<textarea v-model="form.message" rows="5" :placeholder="$t('Beskriv problemet - jo flere detaljer, jo lettere er det å spore opp.')"></textarea></label>
    </div>

    <button class="btn btn-primary" :disabled="submitting || !form.message.trim()" @click="submit">
      {{ submitting ? $t('Sender inn…') : $t('Send inn rapport') }}
    </button>

    <h2 style="margin-top:40px;">{{ $t('Dine rapporter') }}</h2>

    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>
    <div v-else-if="!reports.length" class="empty-state">{{ $t('Du har ikke meldt fra om noe ennå.') }}</div>

    <div v-else class="saved-quiz-list">
      <div v-for="r in reports" :key="r.id" class="saved-quiz-row" style="align-items:flex-start;">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ r.message }}</div>
          <div class="saved-quiz-meta">{{ r.area || $t('Generelt') }}</div>
          <div v-if="r.status === 'RESOLVED' && r.adminNote" style="color:var(--teal); font-size:0.85rem; margin-top:6px;">
            {{ $t('Løst:') }} {{ r.adminNote }}
          </div>
        </div>
        <span class="tag" :style="statusStyle(r.status)">{{ r.status === 'RESOLVED' ? $t('Løst') : $t('Åpen') }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
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
    error.value = _t('Kunne ikke laste rapportene dine.')
  } finally {
    loading.value = false
  }
}

async function submit() {
  error.value = ''
  if (!form.message.trim()) {
    error.value = _t('Beskriv hva som skjedde.')
    return
  }
  submitting.value = true
  try {
    await api.submitReport(form)
    toast.show(_t('Rapporten er sendt inn.'))
    form.area = ''
    form.message = ''
    loadMine()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke sende inn rapporten.')
  } finally {
    submitting.value = false
  }
}

function statusStyle(status) {
  if (status === 'RESOLVED') return { background: 'rgb(var(--teal-rgb) / 0.15)', color: 'var(--teal)' }
  return { background: 'rgb(var(--gold-rgb) / 0.15)', color: 'var(--gold)' }
}
</script>
