<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal dq-modal" role="dialog" aria-modal="true" :aria-label="title">
      <h2 class="dq-modal-title">{{ title }}</h2>
      <p class="dq-modal-sub">How everyone did on this quiz.</p>

      <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

      <div v-else-if="!entries.length" class="dq-empty" style="padding:22px 0;">
        <strong>No scores yet</strong>
        Nobody's been fully graded yet.
      </div>

      <template v-else>
        <div class="dq-stat">
          <span>Average</span>
          <strong>{{ data.averageScore.toFixed(1) }} / {{ data.maxScore }}</strong>
        </div>

        <div class="dq-board">
          <div v-for="(s, i) in shown" :key="s.userName + i" class="dq-board-row" :class="{ 'is-you': s.isYou }">
            <span class="dq-rank" :class="i < 3 ? `dq-rank--${i + 1}` : ''">{{ i + 1 }}</span>
            <div style="min-width:0;">
              <div class="dq-board-name">
                {{ displayName(s.userName) }}<small v-if="s.isYou">That's you</small>
              </div>
              <div class="dq-board-bar"><i :style="{ width: barWidth(s) }"></i></div>
            </div>
            <div class="dq-board-score">{{ s.score }}<small> / {{ s.maxScore }}</small></div>
          </div>

          <template v-if="yourRank && yourRank.rank > shown.length">
            <div class="dq-board-gap">···</div>
            <div class="dq-board-row is-you">
              <span class="dq-rank">{{ yourRank.rank }}</span>
              <div style="min-width:0;">
                <div class="dq-board-name">{{ displayName(yourRank.entry.userName) }}<small>That's you</small></div>
                <div class="dq-board-bar"><i :style="{ width: barWidth(yourRank.entry) }"></i></div>
              </div>
              <div class="dq-board-score">{{ yourRank.entry.score }}<small> / {{ yourRank.entry.maxScore }}</small></div>
            </div>
          </template>
        </div>
      </template>

      <!-- Only for a player who has a graded result of their own - hidden for admins and for anyone
           who hasn't been graded yet. -->
      <label v-if="preference !== null && preference !== undefined" class="dq-switch">
        <input type="checkbox" :checked="preference" @change="emit('update:preference', $event.target.checked)" />
        <span class="dq-switch-track" aria-hidden="true"></span>
        <span>Show my name on this leaderboard</span>
      </label>

      <div class="dq-modal-actions">
        <button class="btn btn-secondary" @click="emit('close')">Close</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useEscapeKey } from '../composables/useEscapeKey'

// One scoreboard for the player's "Leaderboard" button and both admin views. `compact` is the player
// view (top five, plus your own row if you're further down, first names only); the admin view lists
// everyone with full names.
const props = defineProps({
  title: { type: String, default: 'Leaderboard' },
  data: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  compact: { type: Boolean, default: false },
  preference: { type: Boolean, default: null }
})
const emit = defineEmits(['close', 'update:preference'])

useEscapeKey(() => emit('close'))

const entries = computed(() => props.data?.entries || [])
const shown = computed(() => (props.compact ? entries.value.slice(0, 5) : entries.value))
const yourRank = computed(() => {
  if (!props.compact) return null
  const idx = entries.value.findIndex(s => s.isYou)
  return idx === -1 ? null : { rank: idx + 1, entry: entries.value[idx] }
})

function displayName(name) {
  return props.compact ? (name || '').split(' ')[0] : name
}

function barWidth(entry) {
  const pct = entry.maxScore ? (entry.score / entry.maxScore) * 100 : 0
  return `${Math.max(0, Math.min(100, pct))}%`
}
</script>
