<template>
  <div>
    <div class="grid-status-bar">
      <div class="grid-progress">{{ category.title }}</div>
      <div style="color:var(--text-dim); font-size:0.85rem;">First to checkout between 0 and -10 wins</div>
    </div>

    <div class="mp-player-row">
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
      </div>
    </div>

    <template v-if="!winner">
      <div class="guess-box" :class="{ shake: shakeGuessBox }">
        <p style="text-align:center; margin:0 0 8px; color:var(--gold); font-weight:600;">{{ currentPlayer }}'s throw</p>
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
            @click="submitThrow(e)"
          >{{ e.name }}</button>
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

    <div v-else class="banner success" style="text-align:center;">
      <h2 style="color:var(--gold); margin-top:0;">🏆 {{ winner }} wins!</h2>
      <div v-for="p in players" :key="p" style="margin:4px 0;">{{ p }}: {{ totals[p] }}</div>
      <button class="btn btn-primary" style="margin-top:16px;" @click="$emit('gameOver', players.map(p => [p, totals[p]]))">
        Continue
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
  }

  currentPlayerIdx.value = (currentPlayerIdx.value + 1) % props.players.length
}
</script>
