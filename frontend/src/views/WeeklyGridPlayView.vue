<template>
  <div>
    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="state" class="grid-page">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px; flex-wrap:wrap; gap:10px;">
        <h1 style="margin:0;">{{ state.title }}</h1>
        <router-link to="/weekly-grid" class="btn btn-secondary btn-sm no-print">← All grids</router-link>
      </div>
      <p class="page-subtitle">{{ state.theme }}</p>

      <div class="grid-status-bar">
        <div class="grid-progress">{{ solvedCount }} / {{ state.entries.length }} found</div>
        <div class="strike-dots">
          <span
            v-for="i in state.maxStrikes"
            :key="i"
            class="strike-dot"
            :class="{ used: i <= state.strikesUsed, 'just-used': i === state.strikesUsed && justStruck }"
          ></span>
        </div>
      </div>

      <div v-if="state.overtime" class="banner" style="background:rgba(139,124,255,0.15); color:var(--violet); border:1px solid rgba(139,124,255,0.35);">
        Overtime - further guesses don't cost strikes or count toward your result.
      </div>
      <div v-else-if="state.revealed" class="banner error">Answers revealed - this attempt is finished.</div>
      <div v-else-if="allSolved" class="banner success">You found them all!</div>
      <div v-else-if="gameOver" class="banner error">Out of strikes. Reveal the rest, or keep going in Overtime just for fun.</div>

      <div v-if="canStillGuess" class="guess-box" :class="{ shake: shakeGuessBox }">
        <input
          type="text"
          v-model="searchTerm"
          placeholder="Search for a player…"
          aria-label="Search for an athlete"
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
            correct: e.guessedByUser,
            'revealed-only': e.solved && !e.guessedByUser,
            'just-solved': e.id === justSolvedId
          }"
        >
          <span v-if="e.guessedByUser" class="grid-tile-status correct">✓</span>
          <span v-else-if="e.solved" class="grid-tile-status wrong">✕</span>
          <img
            v-if="tileImage(e)"
            :src="tileImage(e)"
            alt=""
            class="grid-tile-logo"
            :class="{ 'is-photo': e.solved && e.athletePhotoUrl }"
          />
          <div
            class="grid-tile-hint"
            :style="{ background: e.hintColor || 'var(--gold)', color: readableTextColor(e.hintColor) }"
          >{{ e.hintLabel }} | {{ e.hintValue }}</div>
          <div class="grid-tile-name">{{ e.solved ? e.athleteName : '?' }}</div>
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
import { readableTextColor } from '../constants'

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

const solvedCount = computed(() => state.value?.entries.filter(e => e.solved).length || 0)
const allSolved = computed(() => !!state.value && solvedCount.value === state.value.entries.length)
const gameOver = computed(() => !!state.value && state.value.completed && !allSolved.value && !state.value.revealed)
const canStillGuess = computed(() =>
  !!state.value && !allSolved.value && !state.value.revealed && (!state.value.completed || state.value.overtime)
)

// Before solving: the club logo is the hint. Once solved: swap to the athlete's own
// photo if one's set, falling back to the logo (or nothing) if not - a solved tile
// should never look emptier than an unsolved one just because no photo was added.
function tileImage(entry) {
  if (entry.solved && entry.athletePhotoUrl) return entry.athletePhotoUrl
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
  if (!val || val.trim().length < 2) {
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
  } catch (e) {
    error.value = 'Could not reveal the answers.'
  } finally {
    actionBusy.value = false
  }
}
</script>
