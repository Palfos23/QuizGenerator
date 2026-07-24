<template>
  <div>
    <h1>Weekly grid</h1>
    <p class="page-subtitle">Guess every athlete that fits the week's theme. Wrong guesses cost you a strike.</p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="!activeGrids.length" class="empty-state friendly">
      No active grid this week yet - check back soon, or ask an admin to publish one.
    </div>

    <div v-else class="saved-quiz-list">
      <div v-for="g in activeGrids" :key="g.id" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ g.title }}</div>
          <div class="saved-quiz-meta">
            {{ sportLabel(g.sport) }} · {{ g.entryCount }} athletes to find · week of {{ formatDate(g.weekStartDate) }}
          </div>
        </div>
        <div style="display:flex; align-items:center; gap:12px;">
          <span class="tag" :style="statusStyle(g.status)">{{ statusLabel(g) }}</span>
          <router-link :to="`/weekly-grid/${g.id}`" class="btn btn-primary btn-sm">
            {{ g.status === 'NOT_STARTED' ? 'Play' : 'Continue' }}
          </router-link>
        </div>
      </div>
    </div>

    <div class="field" style="margin-top:32px;">
      <label style="cursor:pointer;" @click="showArchive = !showArchive">
        {{ showArchive ? 'Hide' : 'Show' }} previous boards
      </label>
      <div v-if="showArchive">
        <div v-if="!archiveGrids.length" style="color:var(--text-dim); font-size:0.9rem;">No previous boards yet.</div>
        <div v-else class="saved-quiz-list">
          <div v-for="g in archiveGrids" :key="g.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ g.title }}</div>
              <div class="saved-quiz-meta">
                {{ sportLabel(g.sport) }} · {{ g.entryCount }} athletes to find · week of {{ formatDate(g.weekStartDate) }}
              </div>
            </div>
            <div style="display:flex; align-items:center; gap:12px;">
              <span class="tag" :style="statusStyle(g.status)">{{ statusLabel(g) }}</span>
              <router-link :to="`/weekly-grid/${g.id}`" class="btn btn-secondary btn-sm">
                {{ g.status === 'NOT_STARTED' ? 'Play' : 'Continue' }}
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'
import { sportLabel } from '../constants'

const activeGrids = ref([])
const archiveGrids = ref([])
const loading = ref(true)
const error = ref('')
const showArchive = ref(false)

onMounted(async () => {
  try {
    activeGrids.value = await api.getActiveGrids()
  } catch (e) {
    error.value = 'Could not load this week\'s grids.'
  } finally {
    loading.value = false
  }
  try {
    archiveGrids.value = await api.getArchiveGrids()
  } catch (e) {
    // archive is a nice-to-have - fail quietly
  }
})

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

function statusLabel(g) {
  if (g.status === 'COMPLETED') return `Completed · ${g.guessedCount}/${g.entryCount} found`
  if (g.status === 'IN_PROGRESS') return `In progress · ${g.guessedCount}/${g.entryCount} found`
  return 'Not started'
}

function statusStyle(status) {
  if (status === 'COMPLETED') return { background: 'rgba(61,220,151,0.15)', color: 'var(--teal)' }
  if (status === 'IN_PROGRESS') return { background: 'rgba(242,183,5,0.15)', color: 'var(--gold)' }
  return { background: 'rgba(255,255,255,0.06)', color: 'var(--text-dim)' }
}
</script>
