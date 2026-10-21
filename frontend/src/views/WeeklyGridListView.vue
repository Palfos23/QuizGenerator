<template>
  <div>
    <h1>{{ $t('Ukens grid§WeeklyGridListViewT') }}</h1>
    <p class="page-subtitle">{{ $t('Gjett alle svarene som passer ukens tema. Feil gjetninger koster deg et liv.') }}</p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

    <div v-else-if="!activeGrids.length" class="empty-state friendly">
      {{ $t('Ingen aktivt grid denne uken ennå - kom tilbake snart, eller be en administrator publisere ett.') }}
    </div>

    <div v-else class="saved-quiz-list">
      <div v-for="g in activeGrids" :key="g.id" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ g.title }}</div>
          <div class="saved-quiz-meta">
            {{ sportLabel(g.sport) }} · {{ g.entryCount }} {{ $t('å finne · uke') }} {{ formatDate(g.weekStartDate) }}
          </div>
        </div>
        <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
          <button class="dq-chip-btn" @click="board.open(g.id, g.title)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
            {{ $t('Toppliste') }}
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
        {{ showArchive ? $t('Skjul') : $t('Vis§WeeklyGridListViewM') }} {{ $t('tidligere brett') }}
      </label>
      <div v-if="showArchive">
        <div v-if="!archiveGrids.length" style="color:var(--text-dim); font-size:0.9rem;">{{ $t('Ingen tidligere brett ennå.') }}</div>
        <div v-else class="saved-quiz-list">
          <div v-for="g in archiveGrids" :key="g.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ g.title }}</div>
              <div class="saved-quiz-meta">
                {{ sportLabel(g.sport) }} · {{ g.entryCount }} {{ $t('å finne · uke') }} {{ formatDate(g.weekStartDate) }}
              </div>
            </div>
            <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
              <button class="dq-chip-btn" @click="board.open(g.id, g.title)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
                {{ $t('Toppliste') }}
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
      :blurb="$t('Det du finner på hvert av ukens brett legges sammen fra mandag til søndag - høyest sum vinner.')"
    />

    <DailyQuizScoreboardModal
      v-if="board.show.value"
      :title="board.title.value"
      :subtitle="$t('Hvordan det gikk med alle på dette gridet.')"
      :empty-text="$t('Ingen har fullført dette gridet ennå.')"
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
import { t as _t } from '../i18n'
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
    error.value = _t('Kunne ikke laste ukens grids.')
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
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { year: 'numeric', month: 'short', day: 'numeric' })
}

function buttonLabel(status) {
  if (status === 'NOT_STARTED') return _t('Spill§WeeklyGridListViewJ')
  if (status === 'COMPLETED') return _t('Vis§WeeklyGridListViewJ')
  return _t('Fortsett§WeeklyGridListViewJ')
}

function statusLabel(g) {
  if (g.status === 'COMPLETED') return _t('Fullført · {0}/{1} funnet', [g.guessedCount, g.entryCount])
  if (g.status === 'IN_PROGRESS') return _t('Pågår · {0}/{1} funnet', [g.guessedCount, g.entryCount])
  return _t('Ikke startet')
}

function statusStyle(status) {
  if (status === 'COMPLETED') return { background: 'rgb(var(--teal-rgb) / 0.15)', color: 'var(--teal)' }
  if (status === 'IN_PROGRESS') return { background: 'rgb(var(--gold-rgb) / 0.15)', color: 'var(--gold)' }
  return { background: 'rgb(var(--fg-rgb) / 0.06)', color: 'var(--text-dim)' }
}
</script>
