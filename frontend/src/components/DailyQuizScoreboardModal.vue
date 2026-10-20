<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal dq-modal" role="dialog" aria-modal="true" :aria-label="title">
      <h2 class="dq-modal-title">{{ title }}</h2>
      <p class="dq-modal-sub">{{ subtitle }}</p>

      <div v-if="loading" style="color:var(--text-dim);">Laster…</div>

      <div v-else-if="!entries.length" class="dq-empty" style="padding:22px 0;">
        <strong>Ingen resultater ennå</strong>
        {{ emptyText }}
      </div>

      <template v-else>
        <div class="dq-stat">
          <span>Snitt</span>
          <strong>{{ data.averageScore.toFixed(1) }} / {{ data.maxScore }}</strong>
        </div>
        <!-- Only for a viewer who is on the board - hidden for admins and anyone who hasn't played. -->
        <p v-if="yourDelta !== null" class="dq-delta" :class="{ 'is-up': yourDelta > 0, 'is-down': yourDelta < 0 }">
          {{ yourDelta === 0 ? 'Akkurat på snittet' : `Du er ${Math.abs(yourDelta)} ${yourDelta > 0 ? 'over' : 'under'} snittet` }}
        </p>

        <div class="dq-board">
          <template v-for="row in rows" :key="row.kind === 'expert' ? 'expert' : row.entry.userName + row.rank">
            <!-- The expert sits in the list at their real score, but isn't ranked: a star instead of a number. -->
            <div v-if="row.kind === 'expert'" class="dq-board-row dq-board-row--expert">
              <span class="dq-rank dq-rank--star" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9L12 3z" /></svg>
              </span>
              <div style="min-width:0;">
                <div class="dq-board-name">Eksperten<small>Ikke rangert</small></div>
                <div class="dq-board-bar"><i :style="{ width: barWidth(row) }"></i></div>
              </div>
              <div class="dq-board-score">{{ row.score }}<small> / {{ row.maxScore }}</small></div>
            </div>

            <div v-else class="dq-board-row" :class="{ 'is-you': row.entry.isYou }">
              <span class="dq-rank" :class="row.rank <= 3 ? `dq-rank--${row.rank}` : ''">{{ row.rank }}</span>
              <div style="min-width:0;">
                <div class="dq-board-name">
                  {{ displayName(row.entry.userName) }}<small v-if="row.entry.isYou">Det er deg</small>
                </div>
                <div class="dq-board-bar"><i :style="{ width: barWidth(row.entry) }"></i></div>
              </div>
              <div class="dq-board-score">{{ row.entry.score }}<small> / {{ row.entry.maxScore }}</small></div>
            </div>
          </template>

          <template v-if="yourRank && yourRank.rank > shownPlayers.length">
            <div class="dq-board-gap">···</div>
            <div class="dq-board-row is-you">
              <span class="dq-rank">{{ yourRank.rank }}</span>
              <div style="min-width:0;">
                <div class="dq-board-name">{{ displayName(yourRank.entry.userName) }}<small>Det er deg</small></div>
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
        <span>Vis navnet mitt på denne topplisten</span>
      </label>

      <div class="dq-modal-actions">
        <button class="btn btn-secondary" @click="emit('close')">Lukk</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useEscapeKey } from '../composables/useEscapeKey'
import { shortNames } from '../services/displayNames'

// One scoreboard for the player's "Toppliste" button and both admin views. `compact` is the player
// view (top five, plus your own row if you're further down, first names only); the admin view lists
// everyone with full names.
const props = defineProps({
  title: { type: String, default: 'Toppliste' },
  data: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  compact: { type: Boolean, default: false },
  preference: { type: Boolean, default: null },
  subtitle: { type: String, default: 'Hvordan det gikk med alle på denne quizen.' },
  emptyText: { type: String, default: 'Ingen er ferdig rettet ennå.' }
})
const emit = defineEmits(['close', 'update:preference'])

useEscapeKey(() => emit('close'))

const entries = computed(() => props.data?.entries || [])
const shownPlayers = computed(() => (props.compact ? entries.value.slice(0, 5) : entries.value))

// What the list shows: the players in rank order, with the expert slotted in at their real score (after
// everyone who scored at least as much, before the first who scored less). Not numbered or counted - just
// there, so you can see exactly who is above and below them.
const rows = computed(() => {
  const list = shownPlayers.value.map((entry, i) => ({ kind: 'player', entry, rank: i + 1 }))
  const expert = props.data?.expert
  if (!expert) return list
  const expertRow = { kind: 'expert', score: expert.score, maxScore: expert.maxScore }
  const at = list.findIndex(r => r.entry.score < expert.score)
  if (at === -1) list.push(expertRow)
  else list.splice(at, 0, expertRow)
  return list
})
const yourRank = computed(() => {
  if (!props.compact) return null
  const idx = entries.value.findIndex(s => s.isYou)
  return idx === -1 ? null : { rank: idx + 1, entry: entries.value[idx] }
})

// How far the viewer's own score is from the average - null when they aren't on the board.
const yourDelta = computed(() => {
  const mine = entries.value.find(s => s.isYou)
  if (!mine || !props.data) return null
  return Math.round((mine.score - props.data.averageScore) * 10) / 10
})


// Compact boards show first names, with a surname initial added where two people share one.
const shortName = computed(() => shortNames(entries.value.map(e => e.userName)))

function displayName(name) {
  return props.compact ? shortName.value(name) : name
}

function barWidth(entry) {
  const pct = entry.maxScore ? (entry.score / entry.maxScore) * 100 : 0
  return `${Math.max(0, Math.min(100, pct))}%`
}
</script>
