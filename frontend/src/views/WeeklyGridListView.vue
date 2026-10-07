<template>
  <div>
    <h1>Weekly grid</h1>
    <p class="page-subtitle">Guess every answer that fits the week's theme. Wrong guesses cost you a strike.</p>

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
            {{ sportLabel(g.sport) }} · {{ g.entryCount }} to find · week of {{ formatDate(g.weekStartDate) }}
          </div>
        </div>
        <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
          <button class="dq-chip-btn" @click="board.open(g.id, g.title)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
            Leaderboard
          </button>
          <span class="tag" :style="statusStyle(g.status)">{{ statusLabel(g) }}</span>
          <router-link :to="`/weekly-grid/${g.id}`" class="btn btn-primary btn-sm">
            {{ buttonLabel(g.status) }}
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
                {{ sportLabel(g.sport) }} · {{ g.entryCount }} to find · week of {{ formatDate(g.weekStartDate) }}
              </div>
            </div>
            <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
              <button class="dq-chip-btn" @click="board.open(g.id, g.title)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
                Leaderboard
              </button>
              <span class="tag" :style="statusStyle(g.status)">{{ statusLabel(g) }}</span>
              <router-link :to="`/weekly-grid/${g.id}`" class="btn btn-secondary btn-sm">
                {{ buttonLabel(g.status) }}
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>

    <DailyQuizWeeklyPanel
      style="margin-top:34px;"
      :fetcher="() => api.getGridWeeklyStandings()"
      unit="board"
      blurb="What you find on each of this week's boards adds up Monday to Sunday - the highest total wins."
    />

    <DailyQuizScoreboardModal
      v-if="board.show.value"
      :title="board.title.value"
      :subtitle="'How everyone did on this grid.'"
      :empty-text="'Nobody has completed this grid yet.'"
      compact
      :data="board.data.value"
      :loading="board.loading.value"
      :preference="board.preference.value"
      @update:preference="board.updatePreference"
      @close="board.show.value = false"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'
import DailyQuizWeeklyPanel from '../components/DailyQuizWeeklyPanel.vue'
import DailyQuizScoreboardModal from '../components/DailyQuizScoreboardModal.vue'
import { useBoardLeaderboard } from '../composables/useBoardLeaderboard'
import { sportLabel } from '../constants'

const activeGrids = ref([])
const archiveGrids = ref([])
const board = useBoardLeaderboard(id => api.getGridScoreboard(id), (id, include) => api.setGridLeaderboardPreference(id, include))
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

function buttonLabel(status) {
  if (status === 'NOT_STARTED') return 'Play'
  if (status === 'COMPLETED') return 'View'
  return 'Continue'
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
