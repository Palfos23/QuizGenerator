<template>
  <section v-if="weekly" class="dq-weekly">
    <div class="dq-card">
      <div class="dq-weekly-head">
        <span class="dq-weekly-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
        </span>
        <div>
          <h2 style="margin:0; font-size:1.2rem;">Weekly winner</h2>
          <div class="dq-row-meta" style="margin:0;">{{ blurb }}</div>
        </div>
      </div>

      <div class="dq-weekly-label">This week · {{ formatRange(weekly.current.weekStart, weekly.current.weekEnd) }}</div>

      <div v-if="!standings.length" class="dq-row-meta" style="padding:6px 0 2px;">No scores yet this week - be the first.</div>
      <div v-else class="dq-board">
        <template v-for="row in rows" :key="row.kind === 'expert' ? 'expert' : row.entry.playerName + row.rank">
          <!-- The expert is in the list at their real total, but not ranked: a star instead of a number. -->
          <div v-if="row.kind === 'expert'" class="dq-board-row dq-board-row--expert">
            <span class="dq-rank dq-rank--star" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9L12 3z" /></svg>
            </span>
            <div style="min-width:0;">
              <div class="dq-board-name">
                The Expert
                <small>Not ranked · {{ daysNote(row.daysPlayed) }} played</small>
              </div>
              <div class="dq-board-bar"><i :style="{ width: barWidth(row) }"></i></div>
            </div>
            <div class="dq-board-score">{{ row.total }}<small> pts</small></div>
          </div>

          <div v-else class="dq-board-row" :class="{ 'is-you': row.entry.isYou }">
            <span class="dq-rank" :class="row.rank <= 3 ? `dq-rank--${row.rank}` : ''">{{ row.rank }}</span>
            <div style="min-width:0;">
              <div class="dq-board-name">
                {{ firstName(row.entry.playerName) }}
                <small>{{ row.entry.isYou ? 'That\'s you · ' : '' }}{{ daysNote(row.entry.daysPlayed) }} played</small>
              </div>
              <div class="dq-board-bar"><i :style="{ width: barWidth(row.entry) }"></i></div>
            </div>
            <div class="dq-board-score">{{ row.entry.total }}<small> pts</small></div>
          </div>
        </template>

        <template v-if="yourRank && yourRank.rank > 5">
          <div class="dq-board-gap">···</div>
          <div class="dq-board-row is-you">
            <span class="dq-rank">{{ yourRank.rank }}</span>
            <div style="min-width:0;">
              <div class="dq-board-name">
                {{ firstName(yourRank.entry.playerName) }}
                <small>That's you · {{ yourRank.entry.daysPlayed }} {{ yourRank.entry.daysPlayed === 1 ? unit : unit + 's' }} played</small>
              </div>
              <div class="dq-board-bar"><i :style="{ width: barWidth(yourRank.entry) }"></i></div>
            </div>
            <div class="dq-board-score">{{ yourRank.entry.total }}<small> pts</small></div>
          </div>
        </template>
      </div>

      <template v-if="weekly.pastWeeks.length">
        <div class="dq-weekly-label">Previous winners</div>
        <div v-for="(w, i) in weekly.pastWeeks" :key="w.weekStart" class="dq-winner" :class="{ 'is-latest': i === 0 }">
          <svg class="dq-winner-icon" viewBox="0 0 24 24" fill="currentColor"><path d="M3 8l4.5 4L12 5l4.5 7L21 8l-2 11H5L3 8z" /></svg>
          <div class="dq-winner-main">
            <div class="dq-winner-name">{{ winnersLabel(w) }}</div>
            <div class="dq-winner-meta">{{ formatRange(w.weekStart, w.weekEnd) }} · {{ w.winningScore }} points</div>
          </div>
          <span v-if="w.winners.length > 1" class="dq-pill">Tied</span>
          <span v-if="w.provisional" class="dq-pill dq-pill--wait" title="Some of that week's answers were still being reviewed">Provisional</span>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import { shortNames } from '../services/displayNames'

// Shared by the daily quiz (the default) and the two weekly games, which pass their own endpoint:
// every endpoint returns the same { current, pastWeeks } shape.
const props = defineProps({
  fetcher: { type: Function, default: () => api.getDailyQuizWeekly() },
  unit: { type: String, default: 'day' }, // what one "played" is - 'day' for the daily quiz, 'board' for the weekly games
  blurb: { type: String, default: 'Every day\'s score adds up Monday to Sunday - the highest total wins.' }
})

const weekly = ref(null)

onMounted(async () => {
  try {
    weekly.value = await props.fetcher()
  } catch (e) {
    // a nice-to-have next to the quizzes themselves - if it can't load, the panel just isn't shown
  }
})

const standings = computed(() => weekly.value?.current.standings || [])
const topFive = computed(() => standings.value.slice(0, 5))
const topTotal = computed(() => Math.max(1, weekly.value?.expert?.total ?? 0, ...standings.value.map(s => s.total)))

// The top five players with the expert slotted in at their real total (after everyone who has at least as
// many points, before the first with fewer) - listed, but not ranked.
const rows = computed(() => {
  const list = topFive.value.map((entry, i) => ({ kind: 'player', entry, rank: i + 1 }))
  const expert = weekly.value?.expert
  if (!expert) return list
  const expertRow = { kind: 'expert', total: expert.total, daysPlayed: expert.daysPlayed }
  const at = list.findIndex(r => r.entry.total < expert.total)
  if (at === -1) list.push(expertRow)
  else list.splice(at, 0, expertRow)
  return list
})
const yourRank = computed(() => {
  const idx = standings.value.findIndex(s => s.isYou)
  return idx === -1 ? null : { rank: idx + 1, entry: standings.value[idx] }
})

function daysNote(n) {
  return `${n} ${n === 1 ? props.unit : props.unit + 's'}`
}

function barWidth(entry) {
  return `${Math.max(0, Math.min(100, (entry.total / topTotal.value) * 100))}%`
}

// First names, with a surname initial added where two people on the panel share one.
const shortName = computed(() => shortNames([
  ...standings.value.map(s => s.playerName),
  ...(weekly.value?.pastWeeks || []).flatMap(w => w.winners)
]))

function firstName(name) {
  return shortName.value(name)
}

function winnersLabel(w) {
  return w.winners.map(firstName).join(' & ')
}

function formatRange(startIso, endIso) {
  const opts = { day: 'numeric', month: 'short' }
  return `${new Date(startIso).toLocaleDateString(undefined, opts)} - ${new Date(endIso).toLocaleDateString(undefined, opts)}`
}
</script>
