<template>
  <div>
    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="loading" style="color:var(--text-dim);">Laster…</div>

    <div v-else-if="state" class="grid-page">
      <h1 style="margin:0 0 10px; text-align:center;">{{ state.title }}</h1>
      <div style="display:flex; gap:8px; margin-bottom:6px;" class="no-print">
        <router-link to="/weekly-grid" class="btn btn-secondary btn-sm">← Alle grids</router-link>
        <button class="dq-chip-btn" @click="openScoreboard">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
            Toppliste
          </button>
      </div>
      <p class="page-subtitle" style="text-align:center;">{{ state.theme }}</p>
      <p v-if="lastUpdatedLabel" class="page-subtitle" style="text-align:center; margin-top:-8px; font-size:0.78rem;">{{ lastUpdatedLabel }}</p>

      <DailyQuizScoreboardModal
        v-if="showScoreboard"
        class="no-print"
        title="Toppliste"
        subtitle="Hvordan det gikk med alle på dette gridet."
        empty-text="Ingen har fullført dette gridet ennå."
        compact
        :data="boardForModal"
        :loading="scoreboardLoading"
        :preference="preferenceShown"
        @update:preference="updateLeaderboardPreference"
        @close="showScoreboard = false"
      />

      <div class="grid-status-bar">
        <div class="grid-progress">{{ guessedCount }} / {{ state.entries.length }} funnet</div>
        <LivesHearts :max="state.maxStrikes" :used="state.strikesUsed" />
      </div>

      <div v-if="allSolved" class="banner success">
        <div><strong>Spillet er ferdig - du fant dem alle!</strong></div>
        <div v-if="overtimeSolvedCount">
          {{ overtimeSolvedCount }} av dem ble funnet i overtid, så dette var ikke en ren løsning - men bra jobba uansett.
        </div>
      </div>
      <div v-else-if="state.revealed" class="banner error">
        <div><strong>Spillet er over.</strong> Du fant {{ guessedCount }} / {{ state.entries.length }} før du viste resten.</div>
      </div>
      <div v-else-if="state.overtime" class="banner" style="background:rgba(61, 79, 159, 0.15); color:var(--violet); border:1px solid rgba(61, 79, 159, 0.35);">
        Overtid - flere gjetninger koster ikke liv, og teller ikke mot en ren løsning.
      </div>
      <div v-else-if="gameOver" class="banner error">
        <div><strong>Spillet er over - tom for liv.</strong> Du fant {{ guessedCount }} / {{ state.entries.length }}. Vis resten, eller fortsett i overtid bare for moro skyld.</div>
      </div>

      <div v-if="canStillGuess" class="guess-box-wrap no-print">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <input
            type="text"
            v-model="searchTerm"
            placeholder="Søk etter et svar…"
            aria-label="Søk etter et svar"
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
        <button class="btn btn-secondary btn-sm" :disabled="actionBusy" @click="showGiveUpConfirm = true">Gi opp og vis gjenstående svar</button>
      </div>

      <div v-else-if="gameOver" class="no-print" style="margin-bottom:20px; display:flex; gap:12px; flex-wrap:wrap;">
        <button class="btn btn-primary" :disabled="actionBusy" @click="doOvertime">Fortsett i overtid</button>
        <button class="btn btn-secondary" :disabled="actionBusy" @click="doReveal">Vis gjenstående svar</button>
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
          <span v-if="e.solvedInOvertime" class="grid-tile-status overtime" title="Funnet i overtid">OT</span>
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
      title="Gi opp dette gridet?"
      message="Dette avslutter forsøket ditt og viser alle gjenstående svar. Du kan ikke fortsette å gjette på dette brettet etterpå."
      confirm-text="Gi opp"
      @confirm="confirmGiveUp"
      @cancel="showGiveUpConfirm = false"
    />

    <div v-if="completionPopup" class="modal-backdrop" @click.self="completionPopup = null">
      <div class="completion-popup" :class="completionPopup">
        <div v-if="completionPopup === 'full'" class="confetti-container">
          <span v-for="(piece, i) in confettiPieces" :key="i" class="confetti-piece" :style="piece"></span>
        </div>

        <template v-if="completionPopup === 'full'">
          <h2>Perfekt løst!</h2>
          <p>Du fant alle {{ state.entries.length }} - uten behov for overtid.</p>
        </template>
        <template v-else-if="completionPopup === 'overtime'">
          <h2>Du kom i mål til slutt!</h2>
          <p>Du fullførte gridet, med litt hjelp fra overtid.</p>
        </template>
        <template v-else-if="completionPopup === 'given-up'">
          <h2>Ingen skam i det</h2>
          <p>Du fant {{ guessedCount }} / {{ state.entries.length }} - ta en titt på det du gikk glipp av under.</p>
        </template>

        <div class="stats-panel" style="margin-top:4px;">
          <div style="color:var(--text-dim); font-size:0.78rem; text-transform:uppercase; letter-spacing:0.5px;">Poeng</div>
          <div style="font-size:1.6rem; font-weight:700;">{{ guessedCount }} / {{ state.entries.length }}</div>
        </div>

        <button class="btn btn-primary" style="margin-top:8px;" @click="completionPopup = null">Fortsett</button>
      </div>
    </div>

    <div v-if="resultOverlay" class="grid-result-overlay" :class="resultOverlay.correct ? 'correct' : 'wrong'">
      <div class="grid-result-text">{{ resultOverlay.correct ? 'Riktig' : 'Feil' }}</div>
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
    error.value = 'Kunne ikke laste dette gridet.'
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
    toast.show(result.correct ? `Riktig - ${result.entry.athleteName}!` : 'Feil gjetning', result.correct ? 'success' : 'error')
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
    error.value = e.response?.data?.message || 'Kunne ikke sende inn gjetningen.'
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
    error.value = e.response?.data?.message || 'Kunne ikke starte overtid.'
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
    error.value = 'Kunne ikke vise svarene.'
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
    toast.show('Kunne ikke oppdatere innstillingen for topplisten.')
    leaderboardOptIn.value = previous
  }
}

async function confirmGiveUp() {
  showGiveUpConfirm.value = false
  await doReveal()
}
</script>
