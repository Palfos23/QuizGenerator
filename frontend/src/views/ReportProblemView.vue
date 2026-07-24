<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:12px;">
      <h1 style="margin:0;">Report a problem</h1>
      <router-link to="/dashboard" class="btn btn-secondary btn-sm">← Back to dashboard</router-link>
    </div>
    <p class="page-subtitle">
      Found a bug, something confusing, or content that needs fixing? Let us know.
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field">
      <label>Which part of the app? <span class="picker-hint">optional</span></label>
      <select v-model="form.area">
        <option value="">Not sure / general</option>
        <option>Create a quiz</option>
        <option>My quizzes</option>
        <option>Weekly grid</option>
        <option>Tension</option>
        <option>Suggest a question</option>
        <option>Admin pages</option>
        <option>Something else</option>
      </select>
    </div>

    <div class="field">
      <label>What happened?</label>
      <textarea v-model="form.message" rows="5" placeholder="Describe the problem - the more detail, the easier it is to track down."></textarea>
    </div>

    <button class="btn btn-primary" :disabled="submitting || !form.message.trim()" @click="submit">
      {{ submitting ? 'Submitting…' : 'Submit report' }}
    </button>

    <h2 style="margin-top:40px;">Your reports</h2>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
    <div v-else-if="!reports.length" class="empty-state">You haven't reported anything yet.</div>

    <div v-else class="saved-quiz-list">
      <div v-for="r in reports" :key="r.id" class="saved-quiz-row" style="align-items:flex-start;">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ r.message }}</div>
          <div class="saved-quiz-meta">{{ r.area || 'General' }}</div>
          <div v-if="r.status === 'RESOLVED' && r.adminNote" style="color:var(--teal); font-size:0.85rem; margin-top:6px;">
            Resolved: {{ r.adminNote }}
          </div>
        </div>
        <span class="tag" :style="statusStyle(r.status)">{{ r.status === 'RESOLVED' ? 'Resolved' : 'Open' }}</span>
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
    error.value = 'Could not load your reports.'
  } finally {
    loading.value = false
  }
}

async function submit() {
  error.value = ''
  if (!form.message.trim()) {
    error.value = 'Please describe what happened.'
    return
  }
  submitting.value = true
  try {
    await api.submitReport(form)
    toast.show('Report submitted.')
    form.area = ''
    form.message = ''
    loadMine()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit the report.'
  } finally {
    submitting.value = false
  }
}

function statusStyle(status) {
  if (status === 'RESOLVED') return { background: 'rgba(61,220,151,0.15)', color: 'var(--teal)' }
  return { background: 'rgba(242,183,5,0.15)', color: 'var(--gold)' }
}
</script>
