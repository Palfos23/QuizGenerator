<template>
  <div class="fiveoo-dartboard-bg">
    <div class="fiveoo-content">
    <div class="fiveoo-header">
      <h2>{{ state?.categoryTitle }}</h2>
      <p v-if="state?.categoryDescription" class="fiveoo-description">{{ state.categoryDescription }}</p>
      <p class="fiveoo-rules-reminder">First to checkout between 0 and -10 wins</p>
    </div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
    <div v-if="error" class="banner error">{{ error }}</div>

    <template v-if="state && !state.finished">
      <div class="mp-player-row">
        <div
          v-for="p in state.players"
          :key="p.participantId"
          class="mp-player-card"
          :class="{ 'active-turn': p.participantId === state.currentTurnParticipantId, 'you-row': p.participantId === state.windowReacherParticipantId }"
        >
          <strong>{{ p.name }}</strong>
          <div style="font-size:1.8rem; font-weight:700; margin-top:4px;" :style="{ color: p.total <= 0 && p.total >= -10 ? 'var(--teal)' : 'var(--text)' }">
            {{ p.total }}
          </div>
          <div v-if="p.participantId === state.windowReacherParticipantId" style="font-size:0.78rem; color:var(--gold); margin-top:2px;">In the window!</div>
          <div v-if="recentThrowsFor(p.name).length" class="fiveoo-recent-throws">
            <span
              v-for="(t, i) in recentThrowsFor(p.name)"
              :key="i"
              class="fiveoo-throw-chip"
              :class="{ bust: t.bust || t.score === 0 }"
            >{{ t.bust ? 'BUST' : (t.score === 0 ? '✕' : t.score) }}</span>
          </div>
        </div>
      </div>

      <div v-if="isYourTurn" class="guess-box-center">
        <div class="guess-box" :class="{ shake: shakeGuessBox }">
          <p style="text-align:center; margin:0 0 8px; color:var(--gold); font-weight:600;">Your throw</p>
          <p v-if="state.bestAvailableScore !== null && state.bestAvailableScore !== undefined" style="text-align:center; margin:0 0 10px; color:var(--text-dim); font-size:0.85rem;">
            Best available score: <strong style="color:var(--text);">{{ state.bestAvailableScore }}</strong>
          </p>
          <p v-if="state.checkoutCount !== null && state.checkoutCount !== undefined" style="text-align:center; margin:0 0 10px; color:var(--text-dim); font-size:0.85rem;">
            <strong style="color:var(--text);">{{ state.checkoutCount }}</strong> possible checkout<span v-if="state.checkoutCount !== 1">s</span> remaining
          </p>
          <input
            type="text"
            v-model="searchTerm"
            placeholder="Search a name…"
            autocomplete="off"
          />
          <div v-if="searchResults.length" class="guess-results">
            <button
              v-for="e in searchResults"
              :key="e.id"
              class="guess-result-row"
              :disabled="throwing"
              @click="submitThrow(e)"
            >{{ e.name }}</button>
          </div>
        </div>
      </div>
      <div v-else class="banner" style="text-align:center; background:rgba(255,255,255,0.03);">
        Waiting for {{ currentTurnName }}'s turn…
      </div>

      <div v-if="lastThrow" style="text-align:center; margin-top:16px; color:var(--text-dim); font-size:0.9rem;">
        <span v-if="lastThrow.bust" style="color:var(--coral); font-weight:600;">BUST!</span>
        <template v-else>
          {{ lastThrow.playerName }} threw {{ lastThrow.entryName }} ({{ lastThrow.rawValue }})
          <span v-if="lastThrow.score === 0" style="color:var(--coral);"> - scores 0</span>
          <span v-else> - scores {{ lastThrow.score }}</span>
        </template>
      </div>
    </template>

    <div v-else-if="state && state.finished" class="fiveoo-gameover">
      <div class="darts-win-label">Winner</div>
      <div class="darts-win-name">{{ winnerName }}</div>

      <div class="score-square-grid">
        <div v-for="p in state.players" :key="p.participantId" class="score-square" :class="{ leader: p.participantId === state.winnerParticipantId }">
          <div class="score-square-name">{{ p.name }}</div>
          <div class="score-square-number">{{ p.total }}</div>
        </div>
      </div>

      <button class="btn btn-primary" style="margin-top:8px; min-width:220px;" @click="finish">
        Play again
      </button>
    </div>

    <details v-if="state" class="advanced-disclosure" style="margin-top:20px;">
      <summary>Throw history</summary>
      <div style="margin-top:10px; max-height:220px; overflow-y:auto;">
        <div v-for="(t, i) in [...state.throwHistory].reverse()" :key="i" style="font-size:0.85rem; color:var(--text-dim); padding:3px 0;">
          <strong style="color:var(--text);">{{ t.playerName }}</strong>
          {{ t.bust ? 'busted on' : 'threw' }} {{ t.entryName }} ({{ t.rawValue }})
          <span v-if="!t.bust">→ scored {{ t.score }}, now on {{ t.resultingTotal }}</span>
        </div>
      </div>
    </details>

    <button class="btn btn-secondary btn-sm no-print" style="margin-top:20px;" @click="leave">← Leave game</button>

    <div v-if="throwOverlay" class="fiveoo-throw-overlay" :class="throwOverlay.kind">
      <div class="fiveoo-overlay-text">{{ throwOverlay.text }}</div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import api from '../services/api'
import { usePolling } from '../composables/usePolling'

const props = defineProps({
  roomCode: { type: String, required: true },
  yourParticipantId: { type: [Number, String], required: true }
})
const emit = defineEmits(['gameOver', 'leave'])

const state = ref(null)
const categoryEntries = ref([]) // fetched once, from the existing category endpoint - not part of the polled state
let loadedCategoryId = null
const loading = ref(true)
const error = ref('')
const throwing = ref(false)
const searchTerm = ref('')
const shakeGuessBox = ref(false)
const lastThrow = ref(null)

const throwOverlay = ref(null)
let overlayTimeout = null
function showThrowOverlay(text, kind) {
  clearTimeout(overlayTimeout)
  throwOverlay.value = null
  requestAnimationFrame(() => {
    throwOverlay.value = { text, kind }
    overlayTimeout = setTimeout(() => { throwOverlay.value = null }, 1400)
  })
}

const isYourTurn = computed(() => !!state.value && state.value.currentTurnParticipantId === props.yourParticipantId)
const currentTurnName = computed(() =>
  state.value?.players.find(p => p.participantId === state.value.currentTurnParticipantId)?.name || '…'
)
const winnerName = computed(() =>
  state.value?.players.find(p => p.participantId === state.value.winnerParticipantId)?.name || ''
)

function recentThrowsFor(playerName) {
  if (!state.value) return []
  return state.value.throwHistory.filter(t => t.playerName === playerName).slice(-4)
}

const searchResults = computed(() => {
  const term = searchTerm.value.trim().toLowerCase()
  if (term.length < 3 || !state.value) return []
  const used = new Set(state.value.usedEntryIds)
  return categoryEntries.value
    .filter(e => !used.has(e.id) && e.name.toLowerCase().includes(term))
    .slice(0, 8)
})

async function poll() {
  try {
    const fresh = await api.getFiveOhOneOnlineState(props.roomCode)
    await applyState(fresh)
  } catch (e) {
    error.value = 'Lost connection to the room - retrying…'
  } finally {
    loading.value = false
  }
}

async function applyState(fresh) {
  error.value = ''
  if (fresh.categoryId && fresh.categoryId !== loadedCategoryId) {
    loadedCategoryId = fresh.categoryId
    try {
      const category = await api.getFiveOhOneCategory(fresh.categoryId)
      categoryEntries.value = category.entries
    } catch (e) {
      // search just won't have results if this fails - the next poll will retry
    }
  }
  if (fresh.throwHistory && fresh.throwHistory.length) {
    lastThrow.value = fresh.throwHistory[fresh.throwHistory.length - 1]
  }
  state.value = fresh
  if (fresh.finished) {
    stopPolling()
  }
}

const { stop: stopPolling } = usePolling(poll, 1200)

async function submitThrow(entry) {
  throwing.value = true
  searchTerm.value = ''
  try {
    const before = state.value.throwHistory.length
    const fresh = await api.throwFiveOhOneOnline(props.roomCode, entry.id)
    const newest = fresh.throwHistory[fresh.throwHistory.length - 1]
    if (fresh.throwHistory.length > before && newest) {
      if (newest.bust) {
        showThrowOverlay('BUST!', 'bust')
        shakeGuessBox.value = true
        setTimeout(() => { shakeGuessBox.value = false }, 400)
      } else if (newest.score === 0) {
        showThrowOverlay('✕', 'zero')
      } else {
        showThrowOverlay(String(newest.score), 'hit')
      }
    }
    await applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit that throw.'
  } finally {
    throwing.value = false
  }
}

function finish() {
  emit('gameOver')
}

function leave() {
  stopPolling()
  emit('leave')
}
</script>
