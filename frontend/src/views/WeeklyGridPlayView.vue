<template>
  <div>
    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="state" class="grid-page">
      <h1 style="margin:0 0 10px; text-align:center;">{{ state.title }}</h1>
      <div style="display:flex; gap:8px; margin-bottom:6px;" class="no-print">
        <router-link to="/weekly-grid" class="btn btn-secondary btn-sm">← All grids</router-link>
        <button class="dq-chip-btn" @click="openScoreboard">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
            Leaderboard
          </button>
      </div>
      <p class="page-subtitle" style="text-align:center;">{{ state.theme }}</p>
      <p v-if="lastUpdatedLabel" class="page-subtitle" style="text-align:center; margin-top:-8px; font-size:0.78rem;">{{ lastUpdatedLabel }}</p>

      <DailyQuizScoreboardModal
        v-if="showScoreboard"
        class="no-print"
        title="Leaderboard"
        subtitle="How everyone did on this grid."
        empty-text="Nobody has completed this grid yet."
        compact
        :data="boardForModal"
        :loading="scoreboardLoading"
        :preference="preferenceShown"
        @update:preference="updateLeaderboardPreference"
        @close="showScoreboard = false"
      />

      <div class="grid-status-bar">
        <div class="grid-progress">{{ guessedCount }} / {{ state.entries.length }} found</div>
        <LivesHearts :max="state.maxStrikes" :used="state.strikesUsed" />
      </div>

      <div v-if="allSolved" class="banner success">
        <div><strong>Game complete - you found them all!</strong></div>
        <div v-if="overtimeSolvedCount">
          {{ overtimeSolvedCount }} of those were found during Overtime, so this wasn't a clean solve - but nice work regardless.
        </div>
      </div>
      <div v-else-if="state.revealed" class="banner error">
        <div><strong>Game over.</strong> You found {{ guessedCount }} / {{ state.entries.length }} before revealing the rest.</div>
      </div>
      <div v-else-if="state.overtime" class="banner" style="background:rgba(139,124,255,0.15); color:var(--violet); border:1px solid rgba(139,124,255,0.35);">
        Overtime - further guesses don't cost strikes, and won't count toward a clean solve.
      </div>
      <div v-else-if="gameOver" class="banner error">
        <div><strong>Game over - out of strikes.</strong> You found {{ guessedCount }} / {{ state.entries.length }}. Reveal the rest, or keep going in Overtime just for fun.</div>
      </div>

      <div v-if="canStillGuess" class="guess-box-wrap no-print">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <input
            type="text"
            v-model="searchTerm"
            placeholder="Search for an answer…"
            aria-label="Search for an answer"
            autocomplete="off"
            autocorrect="off"
            autocapitalize="off"
            spellcheck="false"
            @keydown.esc="searchTerm = ''"
          />
          <div v-if="searchResults.length" class="guess-results">
            <button
              v-for="a in searchResults"
              :key="a.id"
              class="guess-result-row"
              :disabled="guessing"
              @click="submitGuess(a)"
            >
              {{ a.name }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="canStillGuess" class="no-print" style="margin-bottom:20px;">
        <button class="btn btn-secondary btn-sm" :disabled="actionBusy" @click="showGiveUpConfirm = true">Give up &amp; reveal remaining answers</button>
      </div>

      <div v-else-if="gameOver" class="no-print" style="margin-bottom:20px; display:flex; gap:12px; flex-wrap:wrap;">
        <button class="btn btn-primary" :disabled="actionBusy" @click="doOvertime">Continue in Overtime</button>
        <button class="btn btn-secondary" :disabled="actionBusy" @click="doReveal">Reveal remaining answers</button>
      </div>

      <div class="grid-tiles">
        <div
          v-for="e in state.entries"
          :key="e.id"
          :ref="el => { if (el) tileRefs[e.id] = el }"
          class="grid-tile"
          :class="{
            correct: e.guessedByUser && !e.solvedInOvertime,
            'solved-overtime': e.solvedInOvertime,
            'revealed-only': e.solved && !e.guessedByUser,
            'just-solved': e.id === justSolvedId
          }"
        >
          <span v-if="e.solvedInOvertime" class="grid-tile-status overtime" title="Found during Overtime">OT</span>
          <span v-else-if="e.guessedByUser" class="grid-tile-status correct">✓</span>
          <span v-else-if="e.solved" class="grid-tile-status wrong">✕</span>
          <div v-if="state.revealMode === 'DESCRIPTION'" class="grid-tile-description">
            {{ e.revealedDescription || '?' }}
          </div>
          <GameImage
            v-else-if="tileImage(e)"
            :src="tileImage(e)"
            alt=""
            class="grid-tile-logo"
            :class="{ 'is-photo': !!e.athletePhotoUrl, 'is-fit': state.fitImages && !!e.athletePhotoUrl }"
          />
          <div
            v-if="e.hintValue != null || e.hintLabel"
            class="grid-tile-hint"
            :style="{ background: e.hintColor || 'var(--gold)', color: readableTextColor(e.hintColor) }"
          >{{ e.hintValue != null ? formatHint(e.hintLabel, e.hintValue) : e.hintLabel }}</div>
          <div class="grid-tile-name">{{ e.solved ? e.athleteName : '?' }}</div>
        </div>
      </div>
    </div>

    <ConfirmModal
      v-if="showGiveUpConfirm"
      title="Give up on this grid?"
      message="This ends your attempt and reveals every remaining answer. You won't be able to keep guessing on this board afterward."
      confirm-text="Give up"
      @confirm="confirmGiveUp"
      @cancel="showGiveUpConfirm = false"
    />

    <div v-if="completionPopup" class="modal-backdrop" @click.self="completionPopup = null">
      <div class="completion-popup" :class="completionPopup">
        <div v-if="completionPopup === 'full'" class="confetti-container">
          <span v-for="(piece, i) in confettiPieces" :key="i" class="confetti-piece" :style="piece"></span>
        </div>

        <template v-if="completionPopup === 'full'">
          <h2>Perfect clear!</h2>
          <p>You found all {{ state.entries.length }} - no Overtime needed.</p>
        </template>
        <template v-else-if="completionPopup === 'overtime'">
          <h2>Got there in the end!</h2>
          <p>You completed the grid, with a little help from Overtime.</p>
        </template>
        <template v-else-if="completionPopup === 'given-up'">
          <h2>No shame in that</h2>
          <p>You found {{ guessedCount }} / {{ state.entries.length }} - take a look at what you missed below.</p>
        </template>

        <div class="stats-panel" style="margin-top:4px;">
          <div style="color:var(--text-dim); font-size:0.78rem; text-transform:uppercase; letter-spacing:0.5px;">Score</div>
          <div style="font-size:1.6rem; font-weight:700;">{{ guessedCount }} / {{ state.entries.length }}</div>
        </div>

        <button class="btn btn-primary" style="margin-top:8px;" @click="completionPopup = null">Continue</button>
      </div>
    </div>

    <div v-if="resultOverlay" class="grid-result-overlay" :class="resultOverlay.correct ? 'correct' : 'wrong'">
      <div class="grid-result-text">{{ resultOverlay.correct ? 'Correct' : 'Wrong' }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import { useDebouncedSearch } from '../composables/useDebouncedSearch'
import toast from '../services/toast'
import { readableTextColor, formatHint, formatLastUpdated } from '../constants'
import { preloadImage, preloadImages } from '../services/imagePreload'
import ConfirmModal from '../components/ConfirmModal.vue'
import LivesHearts from '../components/LivesHearts.vue'
import GameImage from '../components/GameImage.vue'
import DailyQuizScoreboardModal from '../components/DailyQuizScoreboardModal.vue'

const route = useRoute()
const gridId = route.params.id

const state = ref(null)
const loading = ref(true)
const error = ref('')

const searchTerm = ref('')
const { results: searchResults, search: triggerSearch } = useDebouncedSearch(
  term => api.searchGridCandidates(gridId, term),
  { delay: 250, minLength: 1, postFilter: (term, results) => term.length < 3 ? results.filter(a => a.name.toLowerCase() === term.toLowerCase()) : results }
)
const guessing = ref(false)
const actionBusy = ref(false)

const justSolvedId = ref(null)
const tileRefs = {} // { [entryId]: HTMLElement } - populated via the tile's :ref in the template

// Scrolls the just-solved tile into view - a sticky input keeps the input box
// reachable, but doesn't help if the specific tile that changed is scrolled
// out of view (e.g. answering an earlier tile while looking at a later row).
// Respects prefers-reduced-motion like the rest of this app's animations.
function scrollSolvedTileIntoView(entryId) {
  nextTick(() => {
    const el = tileRefs[entryId]
    if (!el) return
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    el.scrollIntoView({ behavior: reduceMotion ? 'auto' : 'smooth', block: 'center' })
  })
}
const shakeGuessBox = ref(false)

const resultOverlay = ref(null) // { correct } or null when hidden
let resultOverlayTimeout = null
function showResultOverlay(correct) {
  clearTimeout(resultOverlayTimeout)
  resultOverlay.value = null
  requestAnimationFrame(() => {
    resultOverlay.value = { correct }
    resultOverlayTimeout = setTimeout(() => { resultOverlay.value = null }, 1200)
  })
}

const lastUpdatedLabel = computed(() => formatLastUpdated(state.value?.updatedAt))
const guessedCount = computed(() => state.value?.entries.filter(e => e.guessedByUser).length || 0)
const allSolved = computed(() => !!state.value && guessedCount.value === state.value.entries.length)
const overtimeSolvedCount = computed(() => state.value?.entries.filter(e => e.solvedInOvertime).length || 0)
const gameOver = computed(() => !!state.value && state.value.completed && !allSolved.value && !state.value.revealed && !state.value.overtime)
const canStillGuess = computed(() =>
  !!state.value && !allSolved.value && !state.value.revealed && (!state.value.completed || state.value.overtime)
)

// Before solving: the club logo is the hint. Once solved: swap to the athlete's own
// photo if one's set, falling back to the logo (or nothing) if not - a solved tile
// should never look emptier than an unsolved one just because no photo was added.
function tileImage(entry) {
  if (entry.athletePhotoUrl) return entry.athletePhotoUrl
  return entry.logoUrl
}

onMounted(loadState)

async function loadState() {
  loading.value = true
  error.value = ''
  try {
    const fresh = await api.getGridPlayState(gridId)
    // Covers resuming a grid already partway solved - those tiles' photos
    // load now, behind the spinner, instead of popping in right after.
    await preloadImages(fresh.entries.map(tileImage))
    state.value = fresh
  } catch (e) {
    error.value = 'Could not load this grid.'
  } finally {
    loading.value = false
  }
}

watch(searchTerm, triggerSearch)

async function submitGuess(athlete) {
  guessing.value = true
  error.value = ''
  searchTerm.value = ''
  searchResults.value = []
  try {
    const result = await api.submitGridGuess(gridId, athlete.id)
    toast.show(result.correct ? `Correct - ${result.entry.athleteName}!` : 'Wrong guess', result.correct ? 'success' : 'error')
    showResultOverlay(result.correct)

    // Update just the bits that changed locally instead of refetching the whole grid -
    // keeps the update instant and lets a CSS transition animate the specific tile
    // that changed, rather than the whole board flashing into a new state at once.
    state.value.strikesUsed = result.strikesUsed

    if (result.correct) {
      // Loads the tile's photo/logo before it ever appears in the DOM, so the
      // reveal itself never shows a blank/loading image mid-round.
      await preloadImage(tileImage(result.entry))
      const idx = state.value.entries.findIndex(e => e.id === result.entry.id)
      if (idx !== -1) state.value.entries.splice(idx, 1, result.entry)
      justSolvedId.value = result.entry.id
      setTimeout(() => { if (justSolvedId.value === result.entry.id) justSolvedId.value = null }, 700)
    } else {
      shakeGuessBox.value = true
      setTimeout(() => { shakeGuessBox.value = false }, 450)
    }

    if (result.gameOver || result.allSolved) {
      state.value.completed = true
    }
    if (result.allSolved) {
      triggerCompletionPopup(overtimeSolvedCount.value > 0 ? 'overtime' : 'full')
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit that guess.'
  } finally {
    guessing.value = false
  }
}

async function doOvertime() {
  actionBusy.value = true
  error.value = ''
  try {
    const fresh = await api.enterGridOvertime(gridId)
    await preloadImages(fresh.entries.map(tileImage))
    state.value = fresh
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not start overtime.'
  } finally {
    actionBusy.value = false
  }
}

async function doReveal() {
  actionBusy.value = true
  error.value = ''
  try {
    const fresh = await api.revealGrid(gridId)
    await preloadImages(fresh.entries.map(tileImage))
    state.value = fresh
    triggerCompletionPopup('given-up')
  } catch (e) {
    error.value = 'Could not reveal the answers.'
  } finally {
    actionBusy.value = false
  }
}

const showGiveUpConfirm = ref(false)
const showScoreboard = ref(false)
const scoreboardData = ref(null)
const scoreboardLoading = ref(false)

const completionPopup = ref(null) // null | 'full' | 'overtime' | 'given-up'
const confettiPieces = ref([])
const CONFETTI_COLORS = ['var(--gold)', 'var(--teal)', 'var(--coral)', 'var(--violet)', '#ffffff']

function triggerCompletionPopup(type) {
  if (type === 'full') {
    confettiPieces.value = Array.from({ length: 36 }, (_, i) => ({
      left: Math.random() * 100 + '%',
      background: CONFETTI_COLORS[i % CONFETTI_COLORS.length],
      animationDuration: (1.1 + Math.random() * 0.9).toFixed(2) + 's',
      animationDelay: (Math.random() * 0.35).toFixed(2) + 's'
    }))
  }
  completionPopup.value = type
}

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
      scoreboardData.value = await api.getGridScoreboard(gridId)
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
    await api.setGridLeaderboardPreference(gridId, include)
    scoreboardData.value = await api.getGridScoreboard(gridId)
  } catch (e) {
    toast.show('Could not update your leaderboard preference.')
    leaderboardOptIn.value = previous
  }
}

async function confirmGiveUp() {
  showGiveUpConfirm.value = false
  await doReveal()
}
</script>
