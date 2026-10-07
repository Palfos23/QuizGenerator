<template>
  <div>
    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="state" class="grid-page">
      <div style="display:flex; justify-content:space-between; align-items:center; gap:8px; margin-bottom:6px; flex-wrap:wrap;" class="no-print">
        <div style="display:flex; gap:8px;">
          <router-link to="/starting-xi" class="btn btn-secondary btn-sm">← All boards</router-link>
          <button class="dq-chip-btn" @click="openScoreboard">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
            Leaderboard
          </button>
        </div>
        <div class="grid-progress">{{ guessedCount }} / {{ state.slots.length }} found</div>
      </div>
      <h1 style="margin:0 0 6px; text-align:center;">{{ state.title }}</h1>
      <p class="page-subtitle" style="text-align:center;">{{ state.competition }}</p>
      <p v-if="lastUpdatedLabel" class="page-subtitle" style="text-align:center; margin-top:-8px; font-size:0.78rem;">{{ lastUpdatedLabel }}</p>

      <div class="pitch-scoreline">
        <div class="pitch-scoreline-team">
          <GameImage v-if="state.teamCrestUrl" :src="state.teamCrestUrl" alt="" class="pitch-scoreline-crest" />
          <span>{{ state.teamName }}</span>
        </div>
        <div v-if="state.scoreFor != null && state.scoreAgainst != null" class="pitch-scoreline-score">
          <span>{{ state.scoreFor }}</span><span class="dash">-</span><span>{{ state.scoreAgainst }}</span>
        </div>
        <div v-else class="pitch-scoreline-vs">vs</div>
        <div class="pitch-scoreline-team away">
          <GameImage v-if="state.opponentCrestUrl" :src="state.opponentCrestUrl" alt="" class="pitch-scoreline-crest" />
          <span>{{ state.opponentName }}</span>
        </div>
      </div>

      <DailyQuizScoreboardModal
        v-if="showScoreboard"
        class="no-print"
        title="Leaderboard"
        subtitle="How everyone did on this board."
        empty-text="Nobody has completed this board yet."
        compact
        :data="boardForModal"
        :loading="scoreboardLoading"
        :preference="preferenceShown"
        @update:preference="updateLeaderboardPreference"
        @close="showScoreboard = false"
      />

      <div class="grid-status-bar" style="justify-content:center; gap:20px;">
        <LivesHearts :max="state.maxStrikes" :used="state.strikesUsed" />
      </div>

      <div v-if="allSolved" class="banner success">
        <strong>Perfect - you found the whole XI!</strong>
      </div>
      <div v-else-if="state.revealed" class="banner error">
        <strong>Board over.</strong> You found {{ guessedCount }} / {{ state.slots.length }} before revealing the rest.
      </div>
      <div v-else-if="gameOver" class="banner error">
        <strong>Out of lives.</strong> You found {{ guessedCount }} / {{ state.slots.length }}.
        <button class="btn btn-secondary btn-sm" style="margin-left:8px;" @click="giveUp">Reveal the rest</button>
      </div>

      <div v-if="canStillGuess" class="guess-box-wrap no-print">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <input
            type="text"
            v-model="searchTerm"
            placeholder="Search for a player…"
            aria-label="Search for a player"
            autocomplete="off"
            autocorrect="off"
            autocapitalize="off"
            spellcheck="false"
            @keydown.esc="searchTerm = ''"
          />
          <div v-if="searchResults.length" class="guess-results">
            <button v-for="a in searchResults" :key="a.id" class="guess-result-row" :disabled="guessing" @click="submitGuess(a)">
              {{ a.name }}
            </button>
          </div>
        </div>
        <button class="btn btn-secondary btn-sm" style="margin-top:10px;" @click="giveUp">Give up &amp; reveal remaining answers</button>
      </div>

      <div class="pitch" style="margin-top:16px;">
        <PitchMarkings />
        <div v-for="(row, ri) in rows" :key="ri" class="pitch-row" :class="`pitch-row--${row.kind}`">
          <div v-for="slot in row.items" :key="slot.id ?? slot.slotIndex" class="pitch-slot">
            <div
              class="pitch-shirt"
              :class="{ solved: slot.guessedByUser, 'revealed-only': slot.solved && !slot.guessedByUser, goalkeeper: slot.slotIndex === 0 }"
              :style="shirtStyle(slot)"
            >
              <template v-if="!slot.solved">
                <span class="pitch-shirt-sleeve left"></span>
                <span class="pitch-shirt-sleeve right"></span>
                <span class="pitch-shirt-collar"></span>
              </template>
              <GameImage v-if="slot.solved && slot.athletePhotoUrl" :src="slot.athletePhotoUrl" alt="" class="pitch-slot-photo" />
              <template v-else>{{ slot.shirtNumber }}</template>
              <span v-if="slot.captain" class="pitch-shirt-captain">C</span>
            </div>
            <div class="pitch-slot-name">{{ slot.solved ? slot.athleteName : '?' }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import { useDebouncedSearch } from '../composables/useDebouncedSearch'
import toast from '../services/toast'
import { displayRowsFor } from '../services/formations'
import { readableTextColor, formatLastUpdated } from '../constants'
import { preloadImage, preloadImages } from '../services/imagePreload'
import PitchMarkings from '../components/PitchMarkings.vue'
import LivesHearts from '../components/LivesHearts.vue'
import GameImage from '../components/GameImage.vue'
import DailyQuizScoreboardModal from '../components/DailyQuizScoreboardModal.vue'

const DEFAULT_KIT_COLOR = '#d92332'
const DEFAULT_GK_KIT_COLOR = '#f2c230'

const route = useRoute()
const lineupId = computed(() => Number(route.params.id))

const state = ref(null)
const loading = ref(true)
const error = ref('')

const shakeGuessBox = ref(false)
const searchTerm = ref('')
const { results: searchResults, search: triggerSearch } = useDebouncedSearch(
  term => api.searchLineupCandidates(lineupId.value, term),
  { delay: 250, minLength: 1, postFilter: (term, results) => term.length < 3 ? results.filter(a => a.name.toLowerCase() === term.toLowerCase()) : results }
)
const guessing = ref(false)

const lastUpdatedLabel = computed(() => formatLastUpdated(state.value?.updatedAt))
const guessedCount = computed(() => state.value?.slots.filter(s => s.guessedByUser).length || 0)
const allSolved = computed(() => !!state.value && guessedCount.value === state.value.slots.length)
const gameOver = computed(() => !!state.value && state.value.completed && !allSolved.value && !state.value.revealed)
const canStillGuess = computed(() => !!state.value && !state.value.completed)

const rows = computed(() => state.value ? displayRowsFor(state.value.formation, state.value.slots) : [])

function shirtStyle(slot) {
  const color = slot.slotIndex === 0
    ? (state.value?.goalkeeperKitColor || DEFAULT_GK_KIT_COLOR)
    : (state.value?.kitColor || DEFAULT_KIT_COLOR)
  return { '--kit-color': color, '--kit-text': readableTextColor(color) }
}

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const fresh = await api.getLineupPlayState(lineupId.value)
    // Covers resuming a board already partway solved, plus both crests -
    // all load now, behind the spinner, instead of popping in right after.
    await preloadImages([
      fresh.teamCrestUrl, fresh.opponentCrestUrl,
      ...fresh.slots.filter(s => s.solved).map(s => s.athletePhotoUrl)
    ])
    state.value = fresh
  } catch (e) {
    error.value = 'Could not load this board.'
  } finally {
    loading.value = false
  }
}


async function submitGuess(athlete) {
  guessing.value = true
  searchTerm.value = ''
  searchResults.value = []
  try {
    const result = await api.submitLineupGuess(lineupId.value, athlete.id)
    state.value.strikesUsed = result.strikesUsed

    if (result.correct) {
      await preloadImage(result.slot.athletePhotoUrl)
      const idx = state.value.slots.findIndex(s => s.id === result.slot.id)
      if (idx !== -1) state.value.slots.splice(idx, 1, result.slot)
    } else {
      shakeGuessBox.value = true
      setTimeout(() => { shakeGuessBox.value = false }, 400)
    }

    if (result.gameOver || result.allSolved) {
      state.value.completed = true
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit that guess.'
  } finally {
    guessing.value = false
  }
}

async function giveUp() {
  try {
    const fresh = await api.revealLineup(lineupId.value)
    await preloadImages(fresh.slots.map(s => s.athletePhotoUrl))
    state.value = fresh
  } catch (e) {
    error.value = 'Could not reveal the remaining answers.'
  }
}

const showScoreboard = ref(false)
const scoreboardData = ref(null)
const scoreboardLoading = ref(false)

// The shared leaderboard modal wants { averageScore, maxScore, entries: [{ userName, score, maxScore, isYou }] }.
const boardForModal = computed(() => {
  const d = scoreboardData.value
  if (!d) return null
  return {
    averageScore: d.averageScore ?? 0,
    maxScore: d.entryCount,
    entries: (d.entries || []).map(e => ({ userName: e.userName, score: e.guessedCount, maxScore: e.entryCount, isYou: e.isYou }))
  }
})
// null hides the switch: only a player who has finished (and so has a score on the board) can choose.
const preferenceShown = computed(() =>
  scoreboardData.value && scoreboardData.value.yourLeaderboardPreference !== null ? leaderboardOptIn.value : null
)

async function openScoreboard() {
  showScoreboard.value = true
  if (!scoreboardData.value) {
    scoreboardLoading.value = true
    try {
      scoreboardData.value = await api.getLineupScoreboard(lineupId.value)
      leaderboardOptIn.value = scoreboardData.value.yourLeaderboardPreference ?? true
    } catch (e) {
      // scoreboard is a nice-to-have - fail quietly, empty state already covers it
    } finally {
      scoreboardLoading.value = false
    }
  }
}

const leaderboardOptIn = ref(true)
async function updateLeaderboardPreference(include) {
  const previous = leaderboardOptIn.value
  leaderboardOptIn.value = include
  try {
    await api.setLineupLeaderboardPreference(lineupId.value, include)
    scoreboardData.value = await api.getLineupScoreboard(lineupId.value)
  } catch (e) {
    toast.show('Could not update your leaderboard preference.')
    leaderboardOptIn.value = previous
  }
}

watch(searchTerm, triggerSearch)
</script>
