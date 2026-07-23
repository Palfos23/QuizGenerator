<template>
  <div>
    <h1>Question submissions</h1>
    <p class="page-subtitle">Review questions suggested by users. Approved ones join the shared question bank.</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="!submissions.length" class="empty-state friendly">No submissions yet.</div>

    <div v-else class="saved-quiz-list">
      <div v-for="s in submissions" :key="s.id" class="saved-quiz-row" style="align-items:flex-start;">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ s.questionText }}</div>
          <div class="saved-quiz-meta">
            {{ s.category }} · {{ languageLabel(s.language) }} · difficulty {{ s.difficultyLevel }} ·
            submitted by {{ s.submitterName }}
          </div>
          <div style="color:var(--text-dim); font-size:0.85rem; margin-top:6px;">Answer: {{ s.answer }}</div>
          <div v-if="s.status === 'REJECTED'" style="color:var(--coral); font-size:0.85rem; margin-top:6px;">
            Rejected: {{ s.rejectionReason }}
          </div>
        </div>
        <div style="display:flex; flex-direction:column; align-items:flex-end; gap:8px;">
          <span class="tag" :style="statusStyle(s.status)">{{ s.status }}</span>
          <div v-if="s.status === 'PENDING'" style="display:flex; gap:8px;">
            <button class="btn btn-primary btn-sm" @click="approve(s)">Approve</button>
            <button class="btn btn-danger btn-sm" @click="openReject(s)">Reject</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="rejecting" class="modal-backdrop" @click.self="rejecting = null">
      <div class="modal">
        <h2>Reject this question?</h2>
        <p class="page-subtitle">The submitter will see this reason - they can still use the question in their own quizzes.</p>
        <div class="field">
          <label>Reason</label>
          <textarea v-model="rejectReason" rows="3" placeholder="e.g. Answer is outdated / duplicate of an existing question"></textarea>
        </div>
        <div style="display:flex; gap:10px; justify-content:flex-end;">
          <button class="btn btn-secondary" @click="rejecting = null">Cancel</button>
          <button class="btn btn-danger" :disabled="!rejectReason.trim()" @click="confirmReject">Reject</button>
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
    error.value = 'Could not load submissions.'
  } finally {
    loading.value = false
  }
}

async function approve(s) {
  error.value = ''
  try {
    await api.adminApproveSubmission(s.id)
    toast.show('Approved - added to the shared question bank.')
    load()
  } catch (e) {
    error.value = 'Could not approve that submission.'
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
    toast.show('Rejected.')
    rejecting.value = null
    load()
  } catch (e) {
    error.value = 'Could not reject that submission.'
  }
}

function statusStyle(status) {
  if (status === 'APPROVED') return { background: 'rgba(61,220,151,0.15)', color: 'var(--teal)' }
  if (status === 'REJECTED') return { background: 'rgba(255,77,109,0.15)', color: 'var(--coral)' }
  return { background: 'rgba(242,183,5,0.15)', color: 'var(--gold)' }
}
</script>
