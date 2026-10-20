<template>
  <GameAccessGate game="penalty-shootout">
  <div>
    <template v-if="stage === 'landing'">
      <h1>{{ $t('Straffekonkurranse') }}</h1>
      <p class="page-subtitle">
        {{ $t('Gjett hvem som tok hvert straffespark, i riktig rekkefølge - gjetter du riktig, avsløres sparket (og du får ett poeng), gjetter du feil, mister du et liv. Spill alene, eller send enheten rundt og bytt på.') }}
      </p>

      <div v-if="error" class="banner error">{{ error }}</div>

      <div v-if="savedPassAndPlay" class="banner" style="background:rgba(216, 67, 43, 0.1); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <span>{{ $t('Du har en straffekonkurranse i gang.') }}</span>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" @click="resumePassAndPlay">{{ $t('Fortsett§PenaltyShootoutViewT') }}</button>
          <button class="btn btn-secondary btn-sm" @click="dismissPassAndPlay">{{ $t('Lukk§PenaltyShootoutViewT') }}</button>
        </div>
      </div>

      <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
        <div style="flex:1; min-width:160px;">
          <label>{{ $t('Spillere') }} <span class="picker-hint">{{ $t('1 = alene') }}</span>
            <select v-model.number="numPlayers">
              <option v-for="n in [1,2,3,4]" :key="n" :value="n">{{ n }}</option>
            </select>
          </label>
        </div>
      </div>

      <div class="field">
        <label>{{ $t('Hvordan skal konkurransen velges?') }}</label>
        <div class="language-row">
          <button class="language-btn" :class="{ active: pickMode === 'random' }" @click="pickMode = 'random'">{{ $t('Tilfeldig') }}</button>
          <button class="language-btn" :class="{ active: pickMode === 'manual' }" @click="pickMode = 'manual'">{{ $t('Velg selv') }}</button>
        </div>
      </div>

      <button class="btn btn-primary" :disabled="checkingPool" @click="goToSetup">
        {{ checkingPool ? $t('Sjekker…') : (numPlayers === 1 ? $t('Spill →') : $t('Neste →')) }}
      </button>

      <details class="advanced-disclosure" style="margin-top:24px;">
        <summary>{{ $t('Usikker på hvordan det fungerer? Se et eksempel') }}</summary>
        <div style="margin-top:16px; padding:16px 20px; border:1px solid var(--border); border-radius: 2px; background:rgba(27, 26, 23, 0.06);">
          <p style="margin-top:0;">
            {{ $t('Et konkurransebrett viser hvert spark i den virkelige rekkefølgen, delt på lag - et lite merke viser allerede om hvert enkelt ble') }} <strong style="color:var(--teal);">{{ $t('scoret') }}</strong> {{ $t('eller') }}
            <strong style="color:var(--coral);">{{ $t('bommet') }}</strong>{{ $t(', men ikke hvem som tok det.') }}
          </p>
          <p>{{ $t('Søk etter en spiller du tror tok ett av de skjulte sparkene, og send inn en gjetning.') }}</p>
          <ul style="margin:0 0 14px; padding-left:20px; line-height:1.7;">
            <li><strong style="color:var(--teal);">{{ $t('Gjett riktig') }}</strong> {{ $t('- sparket avslører navnet og bildet deres, og du får ett poeng.') }}</li>
            <li><strong style="color:var(--coral);">{{ $t('Gjett feil') }}</strong> {{ $t('- du mister ett av livene dine (5 som standard).') }}</li>
          </ul>
          <p style="margin-bottom:0;">
            {{ $t('Spiller du alene, er det bare deg mot brettet. Med venner går turen videre til neste spiller etter hver gjetning - hver spiller har sine egne liv, så konkurransen fortsetter til den enten er helt løst, eller alle er tomme for liv.') }}
          </p>
        </div>
      </details>
    </template>

    <template v-else-if="stage === 'setup'">
      <h1>{{ $t('Hvem spiller?') }}</h1>
      <p class="page-subtitle" v-if="duplicateNames">{{ $t('To spillere kan ikke ha samme navn.') }}</p>

      <div v-for="(p, i) in setupPlayers" :key="i" class="field">
        <input type="text" v-model="p.name" :placeholder="$t('Spiller {0}', [i + 1])" />
      </div>

      <div style="display:flex; gap:12px;">
        <button class="btn btn-secondary" @click="stage = 'landing'">{{ $t('← Tilbake') }}</button>
        <button class="btn btn-primary" :disabled="!allNamed || duplicateNames" @click="goToShootoutChoice">
          {{ $t('Neste →') }}
        </button>
      </div>
    </template>

    <template v-else-if="stage === 'pickShootout'">
      <h1>{{ $t('Velg en straffekonkurranse') }}</h1>

      <LoadingState v-if="loadingShootouts" full />
      <div v-else-if="!availableShootouts.length" class="empty-state">{{ $t('Ingen straffekonkurranser tilgjengelige ennå.') }}</div>

      <div v-else class="saved-quiz-list">
        <div v-for="s in availableShootouts" :key="s.id" class="saved-quiz-row">
          <div class="saved-quiz-info">
            <div class="saved-quiz-title">{{ s.title }}</div>
            <div class="saved-quiz-meta">{{ s.teamName }} {{ $t('mot') }} {{ s.opponentName }} · {{ s.kickCount }} {{ $t('spark§PenaltyShootoutViewT') }}</div>
          </div>
          <button class="btn btn-primary btn-sm" @click="chooseShootout(s)">{{ $t('Spill§PenaltyShootoutViewT') }}</button>
        </div>
      </div>

      <button class="btn btn-secondary" style="margin-top:16px;" @click="stage = 'setup'">{{ $t('← Tilbake') }}</button>
    </template>

    <PenaltyShootoutGame
      v-else-if="stage === 'game'"
      :key="gameShootoutId"
      :shootout-id="gameShootoutId"
      :players="setupPlayers"
      @game-over="onGameOver"
      @swap="swapShootout"
    />

    <template v-else-if="stage === 'done'">
      <div class="modal-backdrop">
        <div class="completion-popup" :class="{ 'podium-host': setupPlayers.length > 1 }">
          <h2 style="margin-top:0;">{{ setupPlayers.length > 1 ? $t('Spillet er over!') : $t('Bra jobba!') }}</h2>

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
            {{ $t('Du fant {0} spark.¦Du fant {0} spark.', [sortedScores[0]?.[1] ?? 0]) }}
          </p>

          <button class="btn btn-primary" style="margin-top:16px; width:100%;" @click="resetGame">{{ $t('Spill igjen') }}</button>
        </div>
      </div>
    </template>
  </div>
  </GameAccessGate>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import passAndPlayState from '../services/passAndPlayState'
import toast from '../services/toast'
import navTrigger from '../services/navTrigger'
import PenaltyShootoutGame from '../components/PenaltyShootoutGame.vue'
import LoadingState from '../components/LoadingState.vue'
import GameAccessGate from '../components/GameAccessGate.vue'

const colorOptions = [
  { hex: '#4f46e5', name: 'Indigo' },
  { hex: '#7C7CFC', name: _t('Lys indigo') },
  { hex: '#F22C05', name: _t('Rød') },
  { hex: '#F2BB05', name: _t('Gul') },
  { hex: '#032E8A', name: _t('Blå') },
  { hex: '#05D6F2', name: _t('Lyseblå') },
  { hex: '#f43f5e', name: _t('Rosa') },
  { hex: '#5D038A', name: _t('Lilla') }
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
    setupPlayers.push({ name: _t('Du'), color: colorOptions[0].hex })
    return
  }
  const shuffled = [...colorOptions].sort(() => Math.random() - 0.5)
  for (let i = 0; i < numPlayers.value; i++) {
    setupPlayers.push({ name: '', color: shuffled[i % shuffled.length].hex })
  }
}

// Solo mode skips naming entirely - 'Du' needs no picker, so this goes
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
    error.value = e.response?.data?.message || _t('Kunne ikke laste straffekonkurranser.§PenaltyShootoutViewJ')
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
      error.value = _t('Ingen straffekonkurranser tilgjengelige ennå - prøv å velge selv, eller be en administrator legge til noen.')
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

// Swaps the shootout currently on screen for a different random one (the key
// on <PenaltyShootoutGame> remounts it fresh). If there's nothing else, the
// current one stays.
async function swapShootout() {
  try {
    const pool = await api.fetchPenaltyShootoutRoundChoices(1, [gameShootoutId.value])
    if (pool.length) {
      startGame(pool[0].id)
    } else {
      toast.show(_t('Ingen andre straffekonkurranser tilgjengelige.'), 'error')
    }
  } catch (e) {
    toast.show(e.response?.data?.message || _t('Kunne ikke laste en annen straffekonkurranse.'), 'error')
  }
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
