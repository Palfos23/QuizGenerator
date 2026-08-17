<template>
  <div>
    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="state" class="grid-page">
      <div style="display:flex; gap:8px; margin-bottom:6px;" class="no-print">
        <router-link to="/starting-xi" class="btn btn-secondary btn-sm">← All boards</router-link>
      </div>
      <h1 style="margin:0 0 6px; text-align:center;">{{ state.title }}</h1>
      <p class="page-subtitle" style="text-align:center;">{{ state.competition }}</p>

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

      <div class="grid-status-bar" style="justify-content:center; gap:20px;">
        <div class="grid-progress">{{ guessedCount }} / {{ state.slots.length }} found</div>
        <div class="strike-dots">
          <span
            v-for="i in state.maxStrikes"
            :key="i"
            class="strike-dot"
            :class="{ used: i <= strikesUsed, 'just-used': i === strikesUsed && justStruck }"
          ></span>
        </div>
      </div>

      <div v-if="allSolved" class="banner success">
        <strong>Perfect - you found the whole XI!</strong>
      </div>
      <div v-else-if="revealed" class="banner error">
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
            <div class="pitch-shirt" :class="{ solved: slot.solved, goalkeeper: slot.slotIndex === 0 }" :style="shirtStyle(slot)">
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
import { displayRowsFor } from '../services/formations'
import { readableTextColor } from '../constants'
import PitchMarkings from '../components/PitchMarkings.vue'

const DEFAULT_KIT_COLOR = '#d92332'
const DEFAULT_GK_KIT_COLOR = '#f2c230'

const route = useRoute()
const lineupId = computed(() => Number(route.params.id))

const state = ref(null)
const loading = ref(true)
const error = ref('')

const strikesUsed = ref(0)
const guessedSlotIds = ref(new Set())
const solvedById = ref({}) // slot id -> solved LineupSlotDto from the server
const revealedNames = ref({}) // slot id -> name, once given up
const revealed = ref(false)
const justStruck = ref(false)
const shakeGuessBox = ref(false)
const searchTerm = ref('')
const searchResults = ref([])
const guessing = ref(false)

const guessedCount = computed(() => guessedSlotIds.value.size)
const allSolved = computed(() => !!state.value && guessedCount.value >= state.value.slots.length)
const gameOver = computed(() => !!state.value && strikesUsed.value >= state.value.maxStrikes && !allSolved.value)
const canStillGuess = computed(() => !!state.value && !allSolved.value && !gameOver.value && !revealed.value)

const rows = computed(() => {
  if (!state.value) return []
  const merged = state.value.slots.map(s => {
    if (solvedById.value[s.id]) return solvedById.value[s.id]
    if (revealedNames.value[s.id]) return { ...s, solved: true, athleteName: revealedNames.value[s.id] }
    return s
  })
  return displayRowsFor(state.value.formation, merged)
})

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
  strikesUsed.value = 0
  guessedSlotIds.value = new Set()
  solvedById.value = {}
  revealedNames.value = {}
  revealed.value = false
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
    const result = await api.submitLineupGuess(lineupId.value, athlete.id, [...guessedSlotIds.value])
    if (result.correct) {
      guessedSlotIds.value.add(result.slot.id)
      solvedById.value[result.slot.id] = result.slot
    } else {
      strikesUsed.value++
      justStruck.value = true
      shakeGuessBox.value = true
      setTimeout(() => { justStruck.value = false; shakeGuessBox.value = false }, 400)
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit that guess.'
  } finally {
    guessing.value = false
  }
}

async function giveUp() {
  try {
    revealedNames.value = await api.revealLineup(lineupId.value)
    revealed.value = true
  } catch (e) {
    error.value = 'Could not reveal the remaining answers.'
  }
}

watch(searchTerm, onSearchInput)
</script>
