<template>
  <div class="fiveoo-dartboard-bg">
    <div class="fiveoo-content">
    <div class="fiveoo-header">
      <h2>{{ category.title }}</h2>
      <p v-if="category.description" class="fiveoo-description">{{ category.description }}</p>
      <p class="fiveoo-rules-reminder">First to checkout between 0 and -10 wins</p>
    </div>

    <div v-if="!winner" class="mp-player-row">
      <div
        v-for="p in players"
        :key="p"
        class="mp-player-card"
        :class="{ 'active-turn': p === currentPlayer && !winner, 'you-row': windowReacher === p }"
      >
        <strong>{{ p }}</strong>
        <div style="font-size:1.8rem; font-weight:700; margin-top:4px;" :style="{ color: totals[p] <= 0 && totals[p] >= -10 ? 'var(--teal)' : 'var(--text)' }">
          {{ totals[p] }}
        </div>
        <div v-if="windowReacher === p && !winner" style="font-size:0.78rem; color:var(--gold); margin-top:2px;">In the window!</div>
        <div v-if="recentThrowsFor(p).length" class="fiveoo-recent-throws">
          <span
            v-for="(t, i) in recentThrowsFor(p)"
            :key="i"
            class="fiveoo-throw-chip"
            :class="{ bust: t.bust || t.score === 0 }"
          >{{ t.bust ? 'BUST' : (t.score === 0 ? '✕' : t.score) }}</span>
        </div>
      </div>
    </div>

    <template v-if="!winner">
      <div class="guess-box-center">
      <div class="guess-box" :class="{ shake: shakeGuessBox }">
        <p style="text-align:center; margin:0 0 8px; color:var(--gold); font-weight:600;">{{ currentPlayer }}'s throw</p>
        <p v-if="bestAvailableScore !== null" style="text-align:center; margin:0 0 10px; color:var(--text-dim); font-size:0.85rem;">
          Best available score: <strong style="color:var(--text);">{{ bestAvailableScore }}</strong>
        </p>
        <p v-if="checkoutCount !== null" style="text-align:center; margin:0 0 10px; color:var(--text-dim); font-size:0.85rem;">
          <strong style="color:var(--text);">{{ checkoutCount }}</strong> possible checkout<span v-if="checkoutCount !== 1">s</span> remaining
        </p>
        <input
          type="text"
          v-model="searchTerm"
          placeholder="Search a name…"
          autocomplete="off"
          autocorrect="off"
          autocapitalize="off"
          spellcheck="false"
        />
        <div v-if="searchResults.length" class="guess-results">
          <button
            v-for="e in searchResults"
            :key="e.id"
            class="guess-result-row"
            @click="submitThrow(e)"
          >{{ e.name }}</button>
        </div>
      </div>
      </div>

      <div v-if="lastThrow" style="text-align:center; margin-top:16px; color:var(--text-dim); font-size:0.9rem;">
        <span v-if="lastThrow.bust" style="color:var(--coral); font-weight:600;">BUST!</span>
        <template v-else>
          {{ lastThrow.player }} threw {{ lastThrow.name }} ({{ lastThrow.rawValue }})
          <span v-if="lastThrow.score === 0" style="color:var(--coral);"> - scores 0</span>
          <span v-else> - scores {{ lastThrow.score }}</span>
        </template>
      </div>
    </template>

    <div v-else class="fiveoo-gameover">
      <div class="darts-win-label">Winner</div>
      <div class="darts-win-name">{{ winner }}</div>

      <div class="score-square-grid">
        <div v-for="p in players" :key="p" class="score-square" :class="{ leader: p === winner }">
          <div class="score-square-name">{{ p }}</div>
          <div class="score-square-number">{{ totals[p] }}</div>
        </div>
      </div>

      <button class="btn btn-primary" style="margin-top:8px; min-width:220px;" @click="$emit('gameOver', players.map(p => [p, totals[p]]))">
        Play again
      </button>
    </div>

    <details class="advanced-disclosure" style="margin-top:20px;">
      <summary>Throw history</summary>
      <div style="margin-top:10px; max-height:220px; overflow-y:auto;">
        <div v-for="(t, i) in [...history].reverse()" :key="i" style="font-size:0.85rem; color:var(--text-dim); padding:3px 0;">
          <strong style="color:var(--text);">{{ t.player }}</strong>
          {{ t.bust ? 'busted on' : 'threw' }} {{ t.name }} ({{ t.rawValue }})
          <span v-if="!t.bust">→ scored {{ t.score }}, now on {{ t.resultingTotal }}</span>
        </div>
      </div>
    </details>

    <div v-if="throwOverlay" class="fiveoo-throw-overlay" :class="throwOverlay.kind">
      <div class="fiveoo-overlay-text">{{ throwOverlay.text }}</div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import passAndPlayState from '../services/passAndPlayState'

const props = defineProps({
  category: { type: Object, required: true },
  players: { type: Array, required: true }
})
defineEmits(['gameOver'])

const IMPOSSIBLE_CHECKOUTS = new Set([163, 166, 169, 172, 173, 175, 176, 178, 179])

const totals = reactive(Object.fromEntries(props.players.map(p => [p, 501])))
const usedEntryIds = ref(new Set())
const currentPlayerIdx = ref(0)
const currentPlayer = computed(() => props.players[currentPlayerIdx.value])

const windowReacher = ref(null) // player name once someone first lands in the 0..-10 window
const winner = ref(null)
const history = ref([])
const lastThrow = ref(null)
const shakeGuessBox = ref(false)

const searchTerm = ref('')

const throwOverlay = ref(null) // { text, kind: 'hit' | 'zero' | 'bust' } or null when hidden
let overlayTimeout = null

function showThrowOverlay(text, kind) {
  clearTimeout(overlayTimeout)
  // Reset first so re-triggering on back-to-back throws restarts the
  // animation cleanly instead of the new one silently no-oping because
  // the element never left the DOM.
  throwOverlay.value = null
  requestAnimationFrame(() => {
    throwOverlay.value = { text, kind }
    overlayTimeout = setTimeout(() => { throwOverlay.value = null }, 1400)
  })
}

function recentThrowsFor(player) {
  return history.value.filter(t => t.player === player).slice(-4)
}

function progressIdentity() {
  return { categoryId: props.category.id, players: props.players }
}

function identityMatches(saved) {
  const current = progressIdentity()
  return saved.categoryId === current.categoryId
      && JSON.stringify(saved.players) === JSON.stringify(current.players)
}

function saveProgress() {
  passAndPlayState.save('501-progress', {
    ...progressIdentity(),
    totals: { ...totals },
    usedEntryIds: [...usedEntryIds.value],
    currentPlayerIdx: currentPlayerIdx.value,
    windowReacher: windowReacher.value,
    winner: winner.value,
    history: history.value
  })
}

onMounted(() => {
  const saved = passAndPlayState.load('501-progress')
  if (saved && identityMatches(saved)) {
    Object.assign(totals, saved.totals)
    usedEntryIds.value = new Set(saved.usedEntryIds)
    currentPlayerIdx.value = saved.currentPlayerIdx
    windowReacher.value = saved.windowReacher
    winner.value = saved.winner
    history.value = saved.history
  }
})

watch([totals, () => [...usedEntryIds.value], currentPlayerIdx, windowReacher, winner, history], saveProgress, { deep: true })

const searchResults = computed(() => {
  const term = searchTerm.value.trim().toLowerCase()
  if (term.length < 3) return []
  return props.category.entries
    .filter(e => !usedEntryIds.value.has(e.id) && e.name.toLowerCase().includes(term))
    .slice(0, 8)
})

function effectiveScore(rawValue) {
  if (rawValue > 180) return 0
  if (IMPOSSIBLE_CHECKOUTS.has(rawValue)) return 0
  return rawValue
}

const currentPlayerTotal = computed(() => totals[currentPlayer.value])

const unusedEntries = computed(() =>
  props.category.entries.filter(e => !usedEntryIds.value.has(e.id))
)

// Above 180, a checkout isn't reachable on this throw no matter what's
// picked - the only meaningful info is the best score actually available,
// same idea as a player checking what their highest realistic score is.
const bestAvailableScore = computed(() => {
  if (currentPlayerTotal.value <= 180) return null
  if (!unusedEntries.value.length) return 0
  return Math.max(...unusedEntries.value.map(e => effectiveScore(e.value)))
})

// At or below 180, a checkout becomes possible on this throw - shown only
// as a count, never which names/numbers would do it, so it's a strategic
// signal without being a hint toward any specific answer.
const checkoutCount = computed(() => {
  if (currentPlayerTotal.value > 180) return null
  const total = currentPlayerTotal.value
  return unusedEntries.value.filter(e => {
    const resulting = total - effectiveScore(e.value)
    return resulting >= -10 && resulting <= 0
  }).length
})

function submitThrow(entry) {
  const player = currentPlayer.value
  searchTerm.value = ''

  usedEntryIds.value.add(entry.id) // the specific name is claimed regardless of how the throw scores

  const score = effectiveScore(entry.value)
  const previousTotal = totals[player]
  const candidateTotal = previousTotal - score
  const bust = candidateTotal < -10

  if (!bust) {
    totals[player] = candidateTotal
  }

  lastThrow.value = { player, name: entry.name, rawValue: entry.value, score, bust, resultingTotal: bust ? previousTotal : candidateTotal }
  history.value.push({ ...lastThrow.value })

  if (bust) {
    showThrowOverlay('BUST!', 'bust')
  } else if (score === 0) {
    showThrowOverlay('✕', 'zero')
  } else {
    showThrowOverlay(String(score), 'hit')
  }

  const landedInWindow = !bust && candidateTotal >= -10 && candidateTotal <= 0

  if (windowReacher.value && windowReacher.value !== player) {
    // this was the opponent's one response turn after someone reached the window
    if (landedInWindow) {
      const other = windowReacher.value
      // closer to zero wins; a genuine tie favors whoever reached the window first
      winner.value = Math.abs(candidateTotal) <= Math.abs(totals[other]) ? player : other
    } else {
      winner.value = windowReacher.value
    }
    return
  }

  if (landedInWindow && !windowReacher.value) {
    windowReacher.value = player
    if (player === props.players[1]) {
      // the second-starting player reached the window first - the first-starting
      // player doesn't get a response turn, the game ends right here
      winner.value = player
      return
    }
    // the first-starting player reached it - the second-starting player still
    // gets their natural next turn to respond, handled by the branch above
  }

  currentPlayerIdx.value = (currentPlayerIdx.value + 1) % props.players.length
}
</script>
