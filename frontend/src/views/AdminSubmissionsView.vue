<template>
  <div>
    <h1>Innsendte spørsmål</h1>
    <p class="page-subtitle">Gå gjennom spørsmål foreslått av brukere. Godkjente blir en del av den felles spørsmålsbanken.</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Laster…</div>

    <div v-else-if="!submissions.length" class="empty-state friendly">Ingen innsendinger ennå.</div>

    <div v-else class="saved-quiz-list">
      <div v-for="s in submissions" :key="s.id" class="saved-quiz-row" style="align-items:flex-start;">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ s.questionText }}</div>
          <div class="saved-quiz-meta">
            {{ s.category }} · {{ languageLabel(s.language) }} · vanskelighetsgrad {{ s.difficultyLevel }} · sendt inn av {{ s.submitterName }}
          </div>
          <div style="color:var(--text-dim); font-size:0.85rem; margin-top:6px;">Svar: {{ s.answer }}</div>
          <div v-if="s.status === 'REJECTED'" style="color:var(--coral); font-size:0.85rem; margin-top:6px;">
            Avvist: {{ s.rejectionReason }}
          </div>
        </div>
        <div style="display:flex; flex-direction:column; align-items:flex-end; gap:8px;">
          <span class="tag" :style="statusStyle(s.status)">{{ s.status }}</span>
          <div v-if="s.status === 'PENDING'" style="display:flex; gap:8px;">
            <button class="btn btn-primary btn-sm" @click="approve(s)">Godkjenn</button>
            <button class="btn btn-danger btn-sm" @click="openReject(s)">Avvis</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="rejecting" class="modal-backdrop" @click.self="rejecting = null">
      <div class="modal">
        <h2>Avvise dette spørsmålet?</h2>
        <p class="page-subtitle">Den som sendte det inn ser denne begrunnelsen - vedkommende kan fortsatt bruke spørsmålet i sine egne quizer.</p>
        <div class="field">
          <label>Begrunnelse<textarea v-model="rejectReason" rows="3" placeholder="f.eks. Svaret er utdatert / duplikat av et eksisterende spørsmål"></textarea></label>
        </div>
        <div style="display:flex; gap:10px; justify-content:flex-end;">
          <button class="btn btn-secondary" @click="rejecting = null">Avbryt</button>
          <button class="btn btn-danger" :disabled="!rejectReason.trim()" @click="confirmReject">Avvis</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import { languageLabel } from '../constants'

const submissions = ref([])
const loading = ref(true)
const error = ref('')
const rejecting = ref(null)
const rejectReason = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    submissions.value = await api.adminListSubmissions()
  } catch (e) {
    error.value = 'Kunne ikke laste innsendinger.'
  } finally {
    loading.value = false
  }
}

async function approve(s) {
  error.value = ''
  try {
    await api.adminApproveSubmission(s.id)
    toast.show('Godkjent - lagt til i den felles spørsmålsbanken.')
    load()
  } catch (e) {
    error.value = 'Kunne ikke godkjenne innsendingen.'
  }
}

function openReject(s) {
  rejecting.value = s
  rejectReason.value = ''
}

async function confirmReject() {
  error.value = ''
  try {
    await api.adminRejectSubmission(rejecting.value.id, rejectReason.value)
    toast.show('Avvist.')
    rejecting.value = null
    load()
  } catch (e) {
    error.value = 'Kunne ikke avvise innsendingen.'
  }
}

function statusStyle(status) {
  if (status === 'APPROVED') return { background: 'rgba(31, 122, 80, 0.15)', color: 'var(--teal)' }
  if (status === 'REJECTED') return { background: 'rgba(179, 38, 30, 0.15)', color: 'var(--coral)' }
  return { background: 'rgba(216, 67, 43, 0.15)', color: 'var(--gold)' }
}
</script>
