<template>
  <div>
    <h1>Starting XI</h1>
    <p class="page-subtitle">{{ $t('Gjett alle spillerne som startet en bestemt kamp. Feil gjetninger koster deg et liv.') }}</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>
    <div v-else-if="!activeLineups.length" class="empty-state friendly">
      {{ $t('Ingen Starting XI-brett denne uken ennå - kom tilbake snart, eller be en administrator publisere ett.') }}
    </div>

    <div v-else class="saved-quiz-list">
      <div v-for="l in activeLineups" :key="l.id" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ l.title }}</div>
          <div class="saved-quiz-meta">
            {{ l.teamName }} {{ $t('mot') }} {{ l.opponentName }}
            <template v-if="l.scoreFor != null && l.scoreAgainst != null"> ({{ l.scoreFor }}-{{ l.scoreAgainst }})</template>
            <template v-if="l.matchDate"> · {{ formatDate(l.matchDate) }}</template>
            · {{ l.formation }}
          </div>
        </div>
        <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
          <button class="dq-chip-btn" @click="board.open(l.id, l.title)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
            {{ $t('Toppliste') }}
          </button>
          <span class="tag" :style="statusStyle(l.status)">{{ statusLabel(l) }}</span>
          <router-link :to="`/starting-xi/${l.id}`" class="btn btn-primary btn-sm">{{ buttonLabel(l.status) }}</router-link>
        </div>
      </div>
    </div>

    <div class="field" style="margin-top:32px;">
      <label style="cursor:pointer;" @click="showArchive = !showArchive">
        {{ showArchive ? $t('Skjul') : $t('Vis§StartingXiListViewM') }} {{ $t('tidligere brett') }}
      </label>
      <div v-if="showArchive">
        <div v-if="!archiveLineups.length" style="color:var(--text-dim); font-size:0.9rem;">{{ $t('Ingen tidligere brett ennå.') }}</div>
        <div v-else class="saved-quiz-list">
        <div v-for="l in archiveLineups" :key="l.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ l.title }}</div>
            <div class="saved-quiz-meta">
              {{ l.teamName }} {{ $t('mot') }} {{ l.opponentName }}
              <template v-if="l.scoreFor != null && l.scoreAgainst != null"> ({{ l.scoreFor }}-{{ l.scoreAgainst }})</template>
              <template v-if="l.matchDate"> · {{ formatDate(l.matchDate) }}</template>
              · {{ l.formation }}
            </div>
          </div>
          <div style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
            <button class="dq-chip-btn" @click="board.open(l.id, l.title)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
              {{ $t('Toppliste') }}
            </button>
            <span class="tag" :style="statusStyle(l.status)">{{ statusLabel(l) }}</span>
            <router-link :to="`/starting-xi/${l.id}`" class="btn btn-secondary btn-sm">{{ buttonLabel(l.status) }}</router-link>
          </div>
        </div>
        </div>
      </div>
    </div>

    <DailyQuizWeeklyPanel
      style="margin-top:34px;"
      :fetcher="() => api.getLineupWeeklyStandings()"
      unit="brett"
      unit-plural="brett"
      :blurb="$t('Det du finner på hvert av ukens brett legges sammen fra mandag til søndag - høyest sum vinner.')"
    />

    <DailyQuizScoreboardModal
      v-if="board.show.value"
      :title="board.title.value"
      :subtitle="$t('Hvordan det gikk med alle på dette brettet.')"
      :empty-text="$t('Ingen har fullført dette brettet ennå.')"
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

const activeLineups = ref([])
const archiveLineups = ref([])
const showArchive = ref(false)
const board = useBoardLeaderboard(id => api.getLineupScoreboard(id), (id, include) => api.setLineupLeaderboardPreference(id, include))
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    activeLineups.value = await api.getActiveLineups()
  } catch (e) {
    error.value = _t('Kunne ikke laste ukens Starting XI-brett.')
  } finally {
    loading.value = false
  }
  try {
    archiveLineups.value = await api.getArchiveLineups()
  } catch (e) {
    // archive is a nice-to-have - fail quietly
  }
})

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { year: 'numeric', month: 'short', day: 'numeric' })
}

function buttonLabel(status) {
  if (status === 'NOT_STARTED') return _t('Spill§StartingXiListViewJ')
  if (status === 'COMPLETED') return _t('Vis§StartingXiListViewJ')
  return _t('Fortsett§StartingXiListViewJ')
}

function statusLabel(l) {
  if (l.status === 'COMPLETED') return _t('Fullført · {0}/{1} funnet', [l.guessedCount, l.entryCount])
  if (l.status === 'IN_PROGRESS') return _t('Pågår · {0}/{1} funnet', [l.guessedCount, l.entryCount])
  return _t('Ikke startet')
}

function statusStyle(status) {
  if (status === 'COMPLETED') return { background: 'rgba(31, 122, 80, 0.15)', color: 'var(--teal)' }
  if (status === 'IN_PROGRESS') return { background: 'rgba(216, 67, 43, 0.15)', color: 'var(--gold)' }
  return { background: 'rgba(27, 26, 23, 0.06)', color: 'var(--text-dim)' }
}
</script>
