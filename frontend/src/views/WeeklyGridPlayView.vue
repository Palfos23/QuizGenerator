<template>
  <div>
    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="state" class="grid-page">
      <h1 style="margin:0 0 10px;">{{ state.title }}</h1>
      <div style="display:flex; gap:8px; margin-bottom:6px;" class="no-print">
        <router-link to="/weekly-grid" class="btn btn-secondary btn-sm">← All grids</router-link>
        <button class="btn btn-secondary btn-sm" @click="openScoreboard" title="Scoreboard">🏆 Results</button>
      </div>
      <p class="page-subtitle">{{ state.theme }}</p>

      <div v-if="showScoreboard" class="modal-backdrop no-print" @click.self="showScoreboard = false">
        <div class="modal">
          <h2 style="margin-top:0;">Scoreboard</h2>

          <div v-if="scoreboardData && scoreboardEntries.length" class="stats-panel" style="text-align:center;">
            <div style="color:var(--text-dim); font-size:0.78rem; text-transform:uppercase; letter-spacing:0.5px;">Average score</div>
            <div style="font-size:1.5rem; font-weight:700; margin-top:2px;">{{ averageScore.toFixed(1) }} / {{ scoreboardData.entryCount }}</div>
            <div
              v-if="averageDelta"
              style="margin-top:6px; font-weight:600; font-size:0.9rem;"
              :style="{ color: averageDelta > 0 ? 'var(--teal)' : 'var(--coral)' }"
            >{{ averageDelta > 0 ? '▲' : '▼' }} You're {{ Math.abs(averageDelta) }} {{ averageDelta > 0 ? 'above' : 'below' }} average</div>
            <div v-else-if="yourRank" style="margin-top:6px; color:var(--text-dim); font-size:0.9rem;">Right at the average</div>
          </div>

          <div v-if="scoreboardLoading" style="color:var(--text-dim); font-size:0.9rem;">Loading…</div>
          <div v-else-if="!scoreboardEntries.length" style="color:var(--text-dim); font-size:0.9rem;">
            Nobody has completed this grid yet.
          </div>
          <table v-else class="table scoreboard-table">
            <thead>
              <tr><th style="width:14%;">#</th><th style="width:56%;">Player</th><th style="width:30%; text-align:right;">Score</th></tr>
            </thead>
            <tbody>
              <tr v-for="(s, i) in topFive" :key="s.userName + i" :class="{ 'you-row': s.isYou }">
                <td>{{ i + 1 }}</td>
                <td>{{ firstName(s.userName) }}</td>
                <td style="text-align:right;">
                  {{ s.guessedCount }} / {{ s.entryCount }}
                  <span v-if="s.usedOvertime" style="display:block; color:var(--violet); font-size:0.75rem;">+{{ s.overtimeCount }} in overtime</span>
                </td>
              </tr>
              <tr v-if="yourRank && yourRank.rank > 5">
                <td colspan="3" style="text-align:center; color:var(--text-dim); padding:4px 0;">···</td>
              </tr>
              <tr v-if="yourRank && yourRank.rank > 5" class="you-row">
                <td>{{ yourRank.rank }}</td>
                <td>{{ firstName(yourRank.entry.userName) }}</td>
                <td style="text-align:right;">
                  {{ yourRank.entry.guessedCount }} / {{ yourRank.entry.entryCount }}
                  <span v-if="yourRank.entry.usedOvertime" style="display:block; color:var(--violet); font-size:0.75rem;">+{{ yourRank.entry.overtimeCount }} in overtime</span>
                </td>
              </tr>
            </tbody>
          </table>

          <label
            v-if="scoreboardData && scoreboardData.yourLeaderboardPreference !== null"
            style="display:flex; align-items:center; gap:8px; margin-top:16px; text-transform:none; font-weight:400; color:var(--text-dim); font-size:0.9rem; cursor:pointer;"
          >
            <input type="checkbox" v-model="leaderboardOptIn" @change="updateLeaderboardPreference" style="width:auto;" />
            Show my name on this leaderboard
          </label>

          <button class="btn btn-secondary" style="margin-top:16px; width:100%;" @click="showScoreboard = false">Close</button>
        </div>
      </div>

      <div class="grid-status-bar">
        <div class="grid-progress">{{ guessedCount }} / {{ state.entries.length }} found</div>
        <div class="strike-dots">
          <span
            v-for="i in state.maxStrikes"
            :key="i"
            class="strike-dot"
            :class="{ used: i <= state.strikesUsed, 'just-used': i === state.strikesUsed && justStruck }"
          ></span>
        </div>
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

      <div v-if="canStillGuess" class="guess-box" :class="{ shake: shakeGuessBox }">
        <input
          type="text"
          v-model="searchTerm"
          placeholder="Search for a player…"
          aria-label="Search for an answer"
          autocomplete="off"
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
            {{ a.name }} <span style="color:var(--text-dim); font-size:0.85rem;">{{ a.team }}</span>
          </button>
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
          class="grid-tile"
          :class="{
            correct: e.guessedByUser && !e.solvedInOvertime,
            'solved-overtime': e.solvedInOvertime,
            'revealed-only': e.solved && !e.guessedByUser,
            'just-solved': e.id === justSolvedId
          }"
        >
          <span v-if="e.solvedInOvertime" class="grid-tile-status overtime" title="Found during Overtime">⏱</span>
          <span v-else-if="e.guessedByUser" class="grid-tile-status correct">✓</span>
          <span v-else-if="e.solved" class="grid-tile-status wrong">✕</span>
          <img
            v-if="tileImage(e)"
            :src="tileImage(e)"
            alt=""
            class="grid-tile-logo"
            :class="{ 'is-photo': !!e.athletePhotoUrl }"
            @error="$event.target.style.display = 'none'"
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
          <div class="completion-icon">🎉</div>
          <h2>Perfect clear!</h2>
          <p>You found all {{ state.entries.length }} - no Overtime needed.</p>
        </template>
        <template v-else-if="completionPopup === 'overtime'">
          <div class="completion-icon">⏱</div>
          <h2>Got there in the end!</h2>
          <p>You completed the grid, with a little help from Overtime.</p>
        </template>
        <template v-else-if="completionPopup === 'given-up'">
          <div class="completion-icon">🏳️</div>
          <h2>No shame in that</h2>
          <p>You found {{ guessedCount }} / {{ state.entries.length }} - take a look at what you missed below.</p>
        </template>

        <button class="btn btn-primary" style="margin-top:8px;" @click="completionPopup = null">Continue</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import toast from '../services/toast'
import { readableTextColor, formatHint } from '../constants'
import ConfirmModal from '../components/ConfirmModal.vue'

const route = useRoute()
const gridId = route.params.id

const state = ref(null)
const loading = ref(true)
const error = ref('')

const searchTerm = ref('')
const searchResults = ref([])
const guessing = ref(false)
const actionBusy = ref(false)

const justSolvedId = ref(null)
const justStruck = ref(false)
const shakeGuessBox = ref(false)

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
    state.value = await api.getGridPlayState(gridId)
  } catch (e) {
    error.value = 'Could not load this grid.'
  } finally {
    loading.value = false
  }
}

let searchDebounce = null
watch(searchTerm, (val) => {
  clearTimeout(searchDebounce)
  if (!val || val.trim().length < 3) {
    searchResults.value = []
    return
  }
  searchDebounce = setTimeout(async () => {
    try {
      searchResults.value = await api.searchGridCandidates(gridId, val)
    } catch (e) {
      // search failures are non-critical - just show no results
    }
  }, 250)
})

async function submitGuess(athlete) {
  guessing.value = true
  error.value = ''
  searchTerm.value = ''
  searchResults.value = []
  try {
    const result = await api.submitGridGuess(gridId, athlete.id)
    toast.show(result.correct ? `Correct - ${result.entry.athleteName}!` : 'Wrong guess', result.correct ? 'success' : 'error')

    // Update just the bits that changed locally instead of refetching the whole grid -
    // keeps the update instant and lets a CSS transition animate the specific tile
    // that changed, rather than the whole board flashing into a new state at once.
    state.value.strikesUsed = result.strikesUsed

    if (result.correct) {
      const idx = state.value.entries.findIndex(e => e.id === result.entry.id)
      if (idx !== -1) state.value.entries.splice(idx, 1, result.entry)
      justSolvedId.value = result.entry.id
      setTimeout(() => { if (justSolvedId.value === result.entry.id) justSolvedId.value = null }, 700)
    } else {
      justStruck.value = true
      shakeGuessBox.value = true
      setTimeout(() => { justStruck.value = false }, 500)
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
    state.value = await api.enterGridOvertime(gridId)
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
    state.value = await api.revealGrid(gridId)
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

const scoreboardEntries = computed(() => scoreboardData.value?.entries || [])
const topFive = computed(() => scoreboardEntries.value.slice(0, 5))
const yourRank = computed(() => {
  const idx = scoreboardEntries.value.findIndex(s => s.isYou)
  if (idx === -1) return null
  return { rank: idx + 1, entry: scoreboardEntries.value[idx] }
})
const averageScore = computed(() => scoreboardData.value?.averageScore ?? 0)
const averageDelta = computed(() => {
  if (!yourRank.value) return null
  return Math.round((yourRank.value.entry.guessedCount - averageScore.value) * 10) / 10
})

function firstName(fullName) {
  return fullName ? fullName.trim().split(/\s+/)[0] : fullName
}

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
async function updateLeaderboardPreference() {
  try {
    await api.setGridLeaderboardPreference(gridId, leaderboardOptIn.value)
    scoreboardData.value = await api.getGridScoreboard(gridId)
  } catch (e) {
    toast.show('Could not update your leaderboard preference.')
    leaderboardOptIn.value = !leaderboardOptIn.value
  }
}

async function confirmGiveUp() {
  showGiveUpConfirm.value = false
  await doReveal()
}
</script>
