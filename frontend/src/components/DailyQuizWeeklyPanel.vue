<template>
  <section v-if="weekly" class="dq-weekly">
    <div class="dq-card">
      <div class="dq-weekly-head">
        <span class="dq-weekly-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
        </span>
        <div>
          <h2 style="margin:0; font-size:1.2rem;">Weekly winner</h2>
          <div class="dq-row-meta" style="margin:0;">Every day's score adds up Monday to Sunday - the highest total wins.</div>
        </div>
      </div>

      <div class="dq-weekly-label">This week · {{ formatRange(weekly.current.weekStart, weekly.current.weekEnd) }}</div>

      <div v-if="!standings.length" class="dq-row-meta" style="padding:6px 0 2px;">No scores yet this week - be the first.</div>
      <div v-else class="dq-board">
        <div v-for="(s, i) in topFive" :key="s.playerName + i" class="dq-board-row" :class="{ 'is-you': s.isYou }">
          <span class="dq-rank" :class="i < 3 ? `dq-rank--${i + 1}` : ''">{{ i + 1 }}</span>
          <div style="min-width:0;">
            <div class="dq-board-name">
              {{ firstName(s.playerName) }}
              <small>{{ s.isYou ? 'That\'s you · ' : '' }}{{ s.daysPlayed }} {{ s.daysPlayed === 1 ? 'day' : 'days' }} played</small>
            </div>
            <div class="dq-board-bar"><i :style="{ width: barWidth(s) }"></i></div>
          </div>
          <div class="dq-board-score">{{ s.total }}<small> pts</small></div>
        </div>

        <template v-if="yourRank && yourRank.rank > 5">
          <div class="dq-board-gap">···</div>
          <div class="dq-board-row is-you">
            <span class="dq-rank">{{ yourRank.rank }}</span>
            <div style="min-width:0;">
              <div class="dq-board-name">
                {{ firstName(yourRank.entry.playerName) }}
                <small>That's you · {{ yourRank.entry.daysPlayed }} {{ yourRank.entry.daysPlayed === 1 ? 'day' : 'days' }} played</small>
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

const weekly = ref(null)

onMounted(async () => {
  try {
    weekly.value = await api.getDailyQuizWeekly()
  } catch (e) {
    // a nice-to-have next to the quizzes themselves - if it can't load, the panel just isn't shown
  }
})

const standings = computed(() => weekly.value?.current.standings || [])
const topFive = computed(() => standings.value.slice(0, 5))
const topTotal = computed(() => Math.max(1, ...standings.value.map(s => s.total)))
const yourRank = computed(() => {
  const idx = standings.value.findIndex(s => s.isYou)
  return idx === -1 ? null : { rank: idx + 1, entry: standings.value[idx] }
})

function barWidth(entry) {
  return `${Math.max(0, Math.min(100, (entry.total / topTotal.value) * 100))}%`
}

function firstName(name) {
  return (name || '').split(' ')[0]
}

function winnersLabel(w) {
  return w.winners.map(firstName).join(' & ')
}

function formatRange(startIso, endIso) {
  const opts = { day: 'numeric', month: 'short' }
  return `${new Date(startIso).toLocaleDateString(undefined, opts)} - ${new Date(endIso).toLocaleDateString(undefined, opts)}`
}
</script>
