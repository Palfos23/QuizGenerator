<template>
  <div>
    <template v-if="stage === 'category'">
      <h1>501</h1>
      <p class="page-subtitle">
        Start at 501, subtract your throw's value, first to checkout between 0 and -10 wins.
        Darts scoring applies - anything over 180, or one of the nine unreachable checkout numbers, scores zero.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="savedPassAndPlay" class="banner" style="background:rgba(242,183,5,0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>You have a game in progress.</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" @click="resumePassAndPlay">Resume</button>
          <button class="btn btn-secondary btn-sm" @click="dismissPassAndPlay">Dismiss</button>
        </div>
      </div>
      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
      <div v-else-if="!categories.length" class="empty-state friendly">No categories yet - ask an admin to add one.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="c in categories" :key="c.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ c.title }}</div>
            <div class="saved-quiz-meta">{{ c.entryCount }} entries<span v-if="c.description"> · {{ c.description }}</span></div>
          </div>
          <button class="btn btn-primary btn-sm" @click="chooseCategory(c.id)">Play</button>
        </div>
      </div>

      <details class="advanced-disclosure" style="margin-top:24px;">
        <summary>Not sure how it works? See an example</summary>
        <div style="margin-top:16px; padding:16px 20px; border:1px solid var(--border); border-radius:var(--radius-md); background:rgba(255,255,255,0.02);">
          <p style="margin-top:0;">
            Say the category is "Premier League appearances." You're on 501, and you search for a player -
            say one with <strong>136</strong> appearances. That's under 180 and not one of the unreachable numbers,
            so it scores in full: you're now on <strong>365</strong>.
          </p>
          <p>
            Later in the game you're sitting on <strong>184</strong>. You throw a player with <strong>212</strong>
            appearances - too high to count in darts (max is 180), so it scores <strong>zero</strong> and your total
            doesn't move. Another throw of exactly <strong>172</strong> would also score zero - it's one of the nine
            numbers that can't be hit with three darts, checkout-wise.
          </p>
          <p style="margin-bottom:0;">
            Once you're close, say you're on <strong>8</strong> and you throw a player with <strong>15</strong> appearances -
            that would take you to <strong>-7</strong>, past zero but still within the -10 window, so you land safely
            and you're "in the window." If instead you'd thrown a value that took you below -10, that throw busts -
            your total reverts to what it was before.
          </p>
        </div>
      </details>
    </template>

    <template v-else-if="stage === 'setup'">
      <h1>Name the players</h1>
      <p class="page-subtitle">501 is strictly 1v1.</p>
      <div v-if="error" class="banner error">{{ error }}</div>

      <div class="field">
        <label>Player 1</label>
        <input type="text" v-model="playerNames[0]" placeholder="Player 1" />
      </div>
      <div class="field">
        <label>Player 2</label>
        <input type="text" v-model="playerNames[1]" placeholder="Player 2" />
      </div>

      <button class="btn btn-secondary" @click="stage = 'category'">← Back</button>
      <button class="btn btn-primary" style="margin-left:10px;" :disabled="!bothNamed" @click="startGame">Start game</button>
    </template>

    <FiveOhOneGame
      v-else-if="stage === 'game'"
      :category="chosenCategory"
      :players="playerNames"
      @game-over="onGameOver"
    />

    <template v-else-if="stage === 'done'">
      <h1 style="text-align:center;">Game over!</h1>
      <h2 style="text-align:center; color:var(--gold);">🏆 {{ winner }}</h2>

      <table class="table" style="max-width:480px; margin:20px auto; table-layout:fixed; min-width:0;">
        <thead>
          <tr><th>Player</th><th style="text-align:right;">Final total</th></tr>
        </thead>
        <tbody>
          <tr v-for="([name, total], i) in finalTotals" :key="name" :class="{ 'tension-winner-row': i === 0 }">
            <td>{{ name }}</td>
            <td style="text-align:right;">{{ total }}</td>
          </tr>
        </tbody>
      </table>

      <div style="text-align:center;">
        <button class="btn btn-primary" @click="resetToStart">Play again</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import passAndPlayState from '../services/passAndPlayState'
import FiveOhOneGame from '../components/FiveOhOneGame.vue'

const stage = ref('category')
const error = ref('')
const loading = ref(true)
const categories = ref([])
const chosenCategory = ref(null)
const playerNames = ref(['', ''])
const finalTotals = ref([])

const bothNamed = computed(() => playerNames.value[0].trim().length > 0 && playerNames.value[1].trim().length > 0)
const winner = computed(() => finalTotals.value[0]?.[0] ?? null)

onMounted(loadCategories)

const savedPassAndPlay = ref(null)
onMounted(() => {
  savedPassAndPlay.value = passAndPlayState.load('501')
})

function resumePassAndPlay() {
  const saved = savedPassAndPlay.value
  chosenCategory.value = saved.category
  playerNames.value = saved.players
  stage.value = 'game'
}

function dismissPassAndPlay() {
  passAndPlayState.clear('501')
  passAndPlayState.clear('501-progress')
  savedPassAndPlay.value = null
}

async function loadCategories() {
  loading.value = true
  error.value = ''
  try {
    categories.value = await api.listFiveOhOneCategories()
  } catch (e) {
    error.value = 'Could not load categories.'
  } finally {
    loading.value = false
  }
}

async function chooseCategory(id) {
  error.value = ''
  try {
    chosenCategory.value = await api.getFiveOhOneCategory(id)
    stage.value = 'setup'
  } catch (e) {
    error.value = 'Could not load that category.'
  }
}

function startGame() {
  playerNames.value = [playerNames.value[0].trim(), playerNames.value[1].trim()]
  passAndPlayState.save('501', { category: chosenCategory.value, players: playerNames.value })
  savedPassAndPlay.value = passAndPlayState.load('501')
  stage.value = 'game'
}

function onGameOver(totals) {
  passAndPlayState.clear('501')
  passAndPlayState.clear('501-progress')
  savedPassAndPlay.value = null
  // sort by distance from zero (the actual winner is whoever's closer, matching the game's own win logic)
  finalTotals.value = [...totals].sort((a, b) => Math.abs(a[1]) - Math.abs(b[1]))
  stage.value = 'done'
}

function resetToStart() {
  chosenCategory.value = null
  playerNames.value = ['', '']
  finalTotals.value = []
  stage.value = 'category'
}
</script>
