<template>
  <div>
    <h1>Starting XI</h1>
    <p class="page-subtitle">Guess every player who started a specific match. Wrong guesses cost you a life.</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
    <div v-else-if="!lineups.length" class="empty-state friendly">
      No Starting XI boards yet - check back soon, or ask an admin to publish one.
    </div>

    <div v-else class="saved-quiz-list">
      <div v-for="l in lineups" :key="l.id" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ l.title }}</div>
          <div class="saved-quiz-meta">
            {{ l.teamName }} vs {{ l.opponentName }}
            <template v-if="l.scoreFor != null && l.scoreAgainst != null"> ({{ l.scoreFor }}-{{ l.scoreAgainst }})</template>
            <template v-if="l.matchDate"> · {{ formatDate(l.matchDate) }}</template>
            · {{ l.formation }}
          </div>
        </div>
        <div style="display:flex; align-items:center; gap:12px;">
          <span class="tag" :style="statusStyle(l.status)">{{ statusLabel(l) }}</span>
          <router-link :to="`/starting-xi/${l.id}`" class="btn btn-primary btn-sm">{{ buttonLabel(l.status) }}</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'

const lineups = ref([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    lineups.value = await api.listLineups()
  } catch (e) {
    error.value = 'Could not load Starting XI boards.'
  } finally {
    loading.value = false
  }
})

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

function buttonLabel(status) {
  if (status === 'NOT_STARTED') return 'Play'
  if (status === 'COMPLETED') return 'View'
  return 'Continue'
}

function statusLabel(l) {
  if (l.status === 'COMPLETED') return `Completed · ${l.guessedCount}/${l.entryCount} found`
  if (l.status === 'IN_PROGRESS') return `In progress · ${l.guessedCount}/${l.entryCount} found`
  return 'Not started'
}

function statusStyle(status) {
  if (status === 'COMPLETED') return { background: 'rgba(61,220,151,0.15)', color: 'var(--teal)' }
  if (status === 'IN_PROGRESS') return { background: 'rgba(242,183,5,0.15)', color: 'var(--gold)' }
  return { background: 'rgba(255,255,255,0.06)', color: 'var(--text-dim)' }
}
</script>
