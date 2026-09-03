<template>
  <div>
    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="state" class="grid-page">
      <div style="display:flex; justify-content:space-between; align-items:center; gap:8px; margin-bottom:6px; flex-wrap:wrap;" class="no-print">
        <div style="display:flex; gap:8px;">
          <router-link to="/starting-xi" class="btn btn-secondary btn-sm">← All boards</router-link>
          <button class="btn btn-secondary btn-sm" @click="openScoreboard" title="Scoreboard">Results</button>
        </div>
        <div class="grid-progress">{{ guessedCount }} / {{ state.slots.length }} found</div>
      </div>
      <h1 style="margin:0 0 6px; text-align:center;">{{ state.title }}</h1>
      <p class="page-subtitle" style="text-align:center;">{{ state.competition }}</p>
      <p v-if="lastUpdatedLabel" class="page-subtitle" style="text-align:center; margin-top:-8px; font-size:0.78rem;">{{ lastUpdatedLabel }}</p>

      <div class="pitch-scoreline">
        <div class="pitch-scoreline-team">
          <img v-if="state.teamCrestUrl" :src="state.teamCrestUrl" alt="" class="pitch-scoreline-crest" />
          <span>{{ state.teamName }}</span>
        </div>
        <div v-if="state.scoreFor != null && state.scoreAgainst != null" class="pitch-scoreline-score">
          <span>{{ state.scoreFor }}</span><span class="dash">-</span><span>{{ state.scoreAgainst }}</span>
        </div>
        <div v-else class="pitch-scoreline-vs">vs</div>
        <div class="pitch-scoreline-team away">
          <img v-if="state.opponentCrestUrl" :src="state.opponentCrestUrl" alt="" class="pitch-scoreline-crest" />
          <span>{{ state.opponentName }}</span>
        </div>
      </div>

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
            Nobody has completed this board yet.
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
              <img v-if="slot.solved && slot.athletePhotoUrl" :src="slot.athletePhotoUrl" alt="" class="pitch-slot-photo" />
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
import toast from '../services/toast'
import { displayRowsFor } from '../services/formations'
import { readableTextColor, formatLastUpdated } from '../constants'
import PitchMarkings from '../components/PitchMarkings.vue'
import LivesHearts from '../components/LivesHearts.vue'

const DEFAULT_KIT_COLOR = '#d92332'
const DEFAULT_GK_KIT_COLOR = '#f2c230'

const route = useRoute()
const lineupId = computed(() => Number(route.params.id))

const state = ref(null)
const loading = ref(true)
const error = ref('')

const shakeGuessBox = ref(false)
const searchTerm = ref('')
const searchResults = ref([])
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
    state.value = await api.getLineupPlayState(lineupId.value)
  } catch (e) {
    error.value = 'Could not load this board.'
  } finally {
    loading.value = false
  }
}

let searchDebounce = null
function onSearchInput() {
  clearTimeout(searchDebounce)
  const trimmed = searchTerm.value.trim()
  if (!trimmed) {
    searchResults.value = []
    return
  }
  searchDebounce = setTimeout(async () => {
    try {
      const results = await api.searchLineupCandidates(lineupId.value, trimmed)
      searchResults.value = trimmed.length < 3
        ? results.filter(a => a.name.toLowerCase() === trimmed.toLowerCase())
        : results
    } catch (e) {
      // autocomplete failing isn't worth surfacing
    }
  }, 250)
}

async function submitGuess(athlete) {
  guessing.value = true
  searchTerm.value = ''
  searchResults.value = []
  try {
    const result = await api.submitLineupGuess(lineupId.value, athlete.id)
    state.value.strikesUsed = result.strikesUsed

    if (result.correct) {
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
    state.value = await api.revealLineup(lineupId.value)
  } catch (e) {
    error.value = 'Could not reveal the remaining answers.'
  }
}

const showScoreboard = ref(false)
const scoreboardData = ref(null)
const scoreboardLoading = ref(false)

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
async function updateLeaderboardPreference() {
  try {
    await api.setLineupLeaderboardPreference(lineupId.value, leaderboardOptIn.value)
    scoreboardData.value = await api.getLineupScoreboard(lineupId.value)
  } catch (e) {
    toast.show('Could not update your leaderboard preference.')
    leaderboardOptIn.value = !leaderboardOptIn.value
  }
}

watch(searchTerm, onSearchInput)
</script>
