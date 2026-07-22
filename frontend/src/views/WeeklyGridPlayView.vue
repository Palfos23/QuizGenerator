<template>
  <div>
    <router-link to="/weekly-grid" class="nav-link" style="padding-left:0; display:inline-block; margin-bottom:12px;">← All grids</router-link>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <template v-else-if="state">
      <h1 style="margin-bottom:6px;">{{ state.title }}</h1>
      <p class="page-subtitle">{{ state.theme }}</p>

      <div class="grid-status-bar">
        <div class="grid-progress">{{ solvedCount }} / {{ state.entries.length }} found</div>
        <div class="strike-dots">
          <span
            v-for="i in state.maxStrikes"
            :key="i"
            class="strike-dot"
            :class="{ used: i <= state.strikesUsed }"
          ></span>
        </div>
      </div>

      <div v-if="state.overtime" class="banner" style="background:rgba(139,124,255,0.15); color:var(--violet); border:1px solid rgba(139,124,255,0.35);">
        Overtime - further guesses don't cost strikes or count toward your result.
      </div>
      <div v-else-if="state.revealed" class="banner error">Answers revealed - this attempt is finished.</div>
      <div v-else-if="allSolved" class="banner success">You found them all!</div>
      <div v-else-if="gameOver" class="banner error">Out of strikes. Reveal the rest, or keep going in Overtime just for fun.</div>

      <div class="grid-tiles">
        <div v-for="e in state.entries" :key="e.id" class="grid-tile" :class="{ solved: e.solved }">
          <img v-if="e.logoUrl" :src="e.logoUrl" alt="" class="grid-tile-logo" />
          <div class="grid-tile-hint">{{ e.hintLabel }} | {{ e.hintValue }}</div>
          <div class="grid-tile-name">{{ e.solved ? e.athleteName : '?' }}</div>
        </div>
      </div>

      <div v-if="canStillGuess" class="field guess-box">
        <label>Search for an athlete…</label>
        <input
          type="text"
          v-model="searchTerm"
          placeholder="Type a name…"
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

      <div v-else-if="gameOver" class="no-print" style="margin-top:20px; display:flex; gap:12px; flex-wrap:wrap;">
        <button class="btn btn-primary" :disabled="actionBusy" @click="doOvertime">Continue in Overtime</button>
        <button class="btn btn-secondary" :disabled="actionBusy" @click="doReveal">Reveal remaining answers</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import toast from '../services/toast'

const route = useRoute()
const gridId = route.params.id

const state = ref(null)
const loading = ref(true)
const error = ref('')

const searchTerm = ref('')
const searchResults = ref([])
const guessing = ref(false)
const actionBusy = ref(false)

const solvedCount = computed(() => state.value?.entries.filter(e => e.solved).length || 0)
const allSolved = computed(() => !!state.value && solvedCount.value === state.value.entries.length)
const gameOver = computed(() => !!state.value && state.value.completed && !allSolved.value && !state.value.revealed)
const canStillGuess = computed(() =>
  !!state.value && !allSolved.value && !state.value.revealed && (!state.value.completed || state.value.overtime)
)

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
    await loadState()
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
