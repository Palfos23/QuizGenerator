<template>
  <GameAccessGate game="penalty-shootout">
  <div>
    <template v-if="stage === 'landing'">
      <h1>Penalty Shootout</h1>
      <p class="page-subtitle">
        Guess who took each penalty, in real order - get it right and the kick reveals (and you score a point),
        get it wrong and you lose a life. Play solo, or pass the device around and take turns.
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="savedPassAndPlay" class="banner" style="background:rgba(242,183,5,0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>You have a shootout in progress.</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" @click="resumePassAndPlay">Resume</button>
          <button class="btn btn-secondary btn-sm" @click="dismissPassAndPlay">Dismiss</button>
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>Players <span class="picker-hint">1 = solo</span></label>
          <select v-model.number="numPlayers">
            <option v-for="n in [1,2,3,4]" :key="n" :value="n">{{ n }}</option>
          </select>
        </div>
      </div>

      <div class="field">
        <label>How should the shootout be picked?</label>
        <div class="language-row">
          <button class="language-btn" :class="{ active: pickMode === 'random' }" @click="pickMode = 'random'">Random</button>
          <button class="language-btn" :class="{ active: pickMode === 'manual' }" @click="pickMode = 'manual'">Pick my own</button>
        </div>
      </div>

      <button class="btn btn-primary" :disabled="checkingPool" @click="goToSetup">
        {{ checkingPool ? 'Checking…' : (numPlayers === 1 ? 'Play →' : 'Next →') }}
      </button>

      <details class="advanced-disclosure" style="margin-top:24px;">
        <summary>Not sure how it works? See an example</summary>
        <div style="margin-top:16px; padding:16px 20px; border:1px solid var(--border); border-radius:var(--radius-md); background:rgba(255,255,255,0.02);">
          <p style="margin-top:0;">
            A shootout board shows every kick in the real order it happened, split by side - a small badge already
            shows whether each one was <strong style="color:var(--teal);">scored</strong> or
            <strong style="color:var(--coral);">missed</strong>, but not who took it.
          </p>
          <p>Search for a player you think took one of the hidden kicks and submit a guess.</p>
          <ul style="margin:0 0 14px; padding-left:20px; line-height:1.7;">
            <li><strong style="color:var(--teal);">Guess right</strong> - that kick reveals their name and photo, and you score a point.</li>
            <li><strong style="color:var(--coral);">Guess wrong</strong> - you lose one of your lives (5 by default).</li>
          </ul>
          <p style="margin-bottom:0;">
            Playing solo, it's just you against the board. With friends, the turn passes to the next player after
            every guess - each player has their own separate lives, so the shootout keeps going until it's either
            fully solved, or everyone's out of lives.
          </p>
        </div>
      </details>
    </template>

    <template v-else-if="stage === 'setup'">
      <h1>Who's playing?</h1>
      <p class="page-subtitle" v-if="duplicateNames">Two players can't have the same name.</p>

      <div v-for="(p, i) in setupPlayers" :key="i" class="field">
        <input type="text" v-model="p.name" :placeholder="`Player ${i + 1}`" />
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'landing'">← Back</button>
        <button class="btn btn-primary" :disabled="!allNamed || duplicateNames" @click="goToShootoutChoice">
          Next →
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'pickShootout'">
      <h1>Choose a shootout</h1>

      <LoadingState v-if="loadingShootouts" full />
      <div v-else-if="!availableShootouts.length" class="empty-state">No shootouts available yet.</div>

      <div v-else class="saved-quiz-list">
        <div v-for="s in availableShootouts" :key="s.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ s.title }}</div>
            <div class="saved-quiz-meta">{{ s.teamName }} vs {{ s.opponentName }} · {{ s.kickCount }} kicks</div>
          </div>
          <button class="btn btn-primary btn-sm" @click="chooseShootout(s)">Play</button>
        </div>
      </div>

      <button class="btn btn-secondary" style="margin-top:16px;" @click="stage = 'setup'">← Back</button>
    </template>

    <PenaltyShootoutGame
      v-else-if="stage === 'game'"
      :shootout-id="gameShootoutId"
      :players="setupPlayers"
      @game-over="onGameOver"
    />

    <template v-else-if="stage === 'done'">
      <div class="modal-backdrop">
        <div class="completion-popup" :class="{ 'podium-host': setupPlayers.length > 1 }">
          <h2 style="margin-top:0;">{{ setupPlayers.length > 1 ? 'Game over!' : 'Nice work!' }}</h2>

          <template v-if="setupPlayers.length > 1">
            <div class="podium-row">
              <div v-for="(entry, i) in sortedScores.slice(0, 3)" :key="entry[0]" class="podium-block" :class="`rank-${i + 1}`">
                <div class="podium-rank-number">{{ i + 1 }}</div>
                <div class="podium-name">{{ entry[0] }}</div>
                <div class="podium-score">{{ entry[1] }}</div>
              </div>
            </div>
            <div v-if="sortedScores.length > 3" style="max-width:420px; margin:0 auto;">
              <div v-for="(entry, i) in sortedScores.slice(3)" :key="entry[0]" class="podium-rest-row">
                <span>{{ i + 4 }}. {{ entry[0] }}</span>
                <span style="color:var(--text-dim);">{{ entry[1] }}</span>
              </div>
            </div>
          </template>
          <p v-else class="page-subtitle" style="text-align:center;">
            You found {{ sortedScores[0]?.[1] ?? 0 }} kick{{ sortedScores[0]?.[1] === 1 ? '' : 's' }}.
          </p>

          <button class="btn btn-primary" style="margin-top:16px; width:100%;" @click="resetGame">Play again</button>
        </div>
      </div>
    </template>
  </div>
  </GameAccessGate>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import passAndPlayState from '../services/passAndPlayState'
import navTrigger from '../services/navTrigger'
import PenaltyShootoutGame from '../components/PenaltyShootoutGame.vue'
import LoadingState from '../components/LoadingState.vue'
import GameAccessGate from '../components/GameAccessGate.vue'

const colorOptions = [
  { hex: '#4f46e5', name: 'Indigo' },
  { hex: '#7C7CFC', name: 'Light indigo' },
  { hex: '#F22C05', name: 'Red' },
  { hex: '#F2BB05', name: 'Yellow' },
  { hex: '#032E8A', name: 'Blue' },
  { hex: '#05D6F2', name: 'Light blue' },
  { hex: '#f43f5e', name: 'Pink' },
  { hex: '#5D038A', name: 'Purple' }
]

const stage = ref('landing')
const error = ref('')
const numPlayers = ref(1)
const pickMode = ref('random')
const checkingPool = ref(false)

const setupPlayers = reactive([])
const gameShootoutId = ref(null)
const finalScores = ref([])

const savedPassAndPlay = ref(null)
onMounted(() => {
  savedPassAndPlay.value = passAndPlayState.load('penalty-shootout')
})

function resumePassAndPlay() {
  const saved = savedPassAndPlay.value
  gameShootoutId.value = saved.shootoutId
  setupPlayers.length = 0
  saved.players.forEach(p => setupPlayers.push(p))
  stage.value = 'game'
}

function dismissPassAndPlay() {
  passAndPlayState.clear('penalty-shootout')
  savedPassAndPlay.value = null
}

const allNamed = computed(() => setupPlayers.every(p => p.name.trim().length > 0))
const duplicateNames = computed(() => {
  const names = setupPlayers.map(p => p.name.trim().toLowerCase()).filter(n => n.length > 0)
  return names.some((n, i) => names.indexOf(n) !== i)
})

function rebuildSetupPlayers() {
  setupPlayers.length = 0
  if (numPlayers.value === 1) {
    setupPlayers.push({ name: 'You', color: colorOptions[0].hex })
    return
  }
  const shuffled = [...colorOptions].sort(() => Math.random() - 0.5)
  for (let i = 0; i < numPlayers.value; i++) {
    setupPlayers.push({ name: '', color: shuffled[i % shuffled.length].hex })
  }
}

// Solo mode skips naming entirely - "You" needs no picker, so this goes
// straight from the landing screen to picking a shootout.
async function goToSetup() {
  error.value = ''
  rebuildSetupPlayers()
  if (numPlayers.value === 1) {
    await goToShootoutChoice()
  } else {
    stage.value = 'setup'
  }
}

const availableShootouts = ref([])
const loadingShootouts = ref(false)

async function loadAvailableShootouts() {
  loadingShootouts.value = true
  try {
    availableShootouts.value = await api.fetchPenaltyShootouts()
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not load shootouts.'
  } finally {
    loadingShootouts.value = false
  }
}

async function goToShootoutChoice() {
  error.value = ''
  if (pickMode.value === 'random') {
    checkingPool.value = true
    let pool = []
    let accessError = ''
    try {
      pool = await api.fetchPenaltyShootoutRoundChoices(1, [])
    } catch (e) {
      accessError = e.response?.data?.message || ''
    } finally {
      checkingPool.value = false
    }
    if (accessError) {
      error.value = accessError
      stage.value = 'landing'
      return
    }
    if (!pool.length) {
      error.value = 'No shootouts available yet - try picking your own, or ask an admin to add some.'
      stage.value = 'landing'
      return
    }
    startGame(pool[0].id)
  } else {
    stage.value = 'pickShootout'
    await loadAvailableShootouts()
  }
}

function chooseShootout(s) {
  startGame(s.id)
}

function startGame(id) {
  gameShootoutId.value = id
  passAndPlayState.save('penalty-shootout', { shootoutId: id, players: [...setupPlayers] })
  savedPassAndPlay.value = passAndPlayState.load('penalty-shootout')
  stage.value = 'game'
}

const sortedScores = computed(() => [...finalScores.value].sort((a, b) => b[1] - a[1]))

function onGameOver(scores) {
  api.recordGamePlayed('PENALTY_SHOOTOUT')
  passAndPlayState.clear('penalty-shootout')
  savedPassAndPlay.value = null
  finalScores.value = scores
  stage.value = 'done'
}

function resetGame() {
  passAndPlayState.clear('penalty-shootout')
  savedPassAndPlay.value = null
  gameShootoutId.value = null
  finalScores.value = []
  stage.value = 'landing'
}

// Clicking the "Penalty Shootout" nav tab while already on this page doesn't
// trigger any navigation event on its own - same reasoning as
// MultiplayerGridView's equivalent watcher.
watch(() => navTrigger.state.penaltyShootout, () => {
  stage.value = 'landing'
})
</script>
