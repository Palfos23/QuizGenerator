<template>
  <section v-if="weekly" class="dq-weekly">
    <div class="dq-card">
      <div class="dq-weekly-head">
        <span class="dq-weekly-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0V4zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3" /></svg>
        </span>
        <div>
          <h2 style="margin:0; font-size:1.2rem;">{{ $t('Ukens vinner') }}</h2>
          <div class="dq-row-meta" style="margin:0;">{{ blurb }}</div>
        </div>
      </div>

      <div class="dq-weekly-label">{{ $t('Denne uken ·') }} {{ formatRange(weekly.current.weekStart, weekly.current.weekEnd) }}</div>

      <div v-if="!standings.length" class="dq-row-meta" style="padding:6px 0 2px;">{{ $t('Ingen resultater denne uken ennå - vær den første.') }}</div>
      <div v-else class="dq-ledger">
        <template v-for="row in rows" :key="row.kind === 'expert' ? 'expert' : 'p' + row.index">
          <!-- The expert is not a player: a line across the list at their real total. Everyone above it beat them. -->
          <div v-if="row.kind === 'expert'" class="dq-expert-line" role="separator" :title="daysNote(row.daysPlayed)" :aria-label="$t('Eksperten') + ' ' + row.total">
            <span class="dq-expert-tag">
              <svg viewBox="0 0 12 12" aria-hidden="true"><path d="M6 1l5 5-5 5-5-5z" /></svg>
              <b>{{ $t('Eksperten') }}</b>
              <span class="dq-expert-score">{{ row.total }}<small>{{ $t('poeng§DailyQuizWeeklyPanelPts') }}</small></span>
            </span>
          </div>

          <div v-else class="dq-led" :class="{ 'is-winner': row.rank === 1, 'is-you': row.entry.isYou }">
            <span class="dq-led-rank">{{ pad(row.rank) }}</span>
            <span class="dq-led-name">
              {{ firstName(row.entry.playerName) }}<span v-if="row.entry.isYou" class="dq-you-tag" :title="$t('Det er deg')">{{ $t('DU') }}</span>
              <small class="dq-led-meta">{{ daysNote(row.entry.daysPlayed) }}</small>
            </span>
            <span class="dq-led-dots"></span>
            <span class="dq-led-score">{{ row.entry.total }}<small>{{ $t('poeng§DailyQuizWeeklyPanelPts') }}</small></span>
          </div>
        </template>

        <template v-if="yourRank && yourRank.index >= TOP_N">
          <div class="dq-led-gap">···</div>
          <div class="dq-led is-you">
            <span class="dq-led-rank">{{ pad(yourRank.rank) }}</span>
            <span class="dq-led-name">
              {{ firstName(yourRank.entry.playerName) }}<span class="dq-you-tag" :title="$t('Det er deg')">{{ $t('DU') }}</span>
              <small class="dq-led-meta">{{ daysNote(yourRank.entry.daysPlayed) }}</small>
            </span>
            <span class="dq-led-dots"></span>
            <span class="dq-led-score">{{ yourRank.entry.total }}<small>{{ $t('poeng§DailyQuizWeeklyPanelPts') }}</small></span>
          </div>
        </template>
      </div>

      <!-- Last week's winner as one line; the full history (every week, and who has won the most) opens in a window. -->
      <div v-if="weekly.pastWeeks.length" class="dq-champ-line">
        <svg class="dq-champ-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M3 8l4.5 4L12 5l4.5 7L21 8l-2 11H5L3 8z" /></svg>
        <span class="dq-champ-line-text">
          <span class="dq-champ-when">{{ $t('Forrige uke') }}</span>
          <span class="dq-champ-name">{{ winnersLabel(latestWeek) }}</span>
          <span class="dq-champ-score">{{ $t('{0} poeng¦{0} poeng', [latestWeek.winningScore]) }}</span>
          <span v-if="latestWeek.winners.length > 1" class="dq-led-pill">{{ $t('Delt') }}</span>
          <span v-if="latestWeek.provisional" class="dq-led-pill" :title="$t('Noen av svarene den uken ble fortsatt gjennomgått')">{{ $t('Foreløpig') }}</span>
        </span>
        <button type="button" class="dq-champ-all" @click="showWinners = true">{{ $t('Alle vinnere') }} →</button>
      </div>
    </div>

    <div v-if="showWinners" class="modal-backdrop" @click.self="showWinners = false">
      <div class="modal dq-winners-modal" role="dialog" aria-modal="true" :aria-label="$t('Vinnere')">
        <div class="dq-winners-head">
          <h2>{{ $t('Vinnere') }}</h2>
          <button type="button" class="dq-winners-close" :aria-label="$t('Lukk')" @click="showWinners = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
          </button>
        </div>

        <!-- Who has won the most weeks (a tie counts as a win for each winner) - the top five, as bars. -->
        <template v-if="winTally.length">
          <div class="dq-weekly-label">{{ $t('Flest seiere') }}</div>
          <div v-for="row in winTally" :key="row.name" class="dq-tally">
            <span class="dq-tally-name">{{ firstName(row.name) }}</span>
            <span class="dq-tally-bar" aria-hidden="true"><i :class="{ top: row.wins === winTally[0].wins }" :style="{ width: Math.round(row.wins / winTally[0].wins * 100) + '%' }"></i></span>
            <span class="dq-tally-count">{{ row.wins }}</span>
          </div>
        </template>

        <!-- Every week, newest first - it grows by one each Monday, so the list scrolls inside the window. -->
        <div class="dq-weekly-label">{{ $t('Alle uker') }}</div>
        <div class="dq-winners-list">
          <div v-for="w in weekly.pastWeeks" :key="w.weekStart" class="dq-roll">
            <span class="dq-roll-when">{{ formatRange(w.weekStart, w.weekEnd) }}</span>
            <span class="dq-roll-name">{{ winnersLabel(w) }}</span>
            <span v-if="w.winners.length > 1" class="dq-led-pill">{{ $t('Delt') }}</span>
            <span v-if="w.provisional" class="dq-led-pill" :title="$t('Noen av svarene den uken ble fortsatt gjennomgått')">{{ $t('Foreløpig') }}</span>
            <span class="dq-led-dots"></span>
            <span class="dq-roll-score">{{ $t('{0} poeng¦{0} poeng', [w.winningScore]) }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { sharedRanks } from '../composables/sharedRanks'
import { t as _t } from '../i18n'
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import { shortNames } from '../services/displayNames'

// Shared by the daily quiz (the default) and the two weekly games, which pass their own endpoint:
// every endpoint returns the same { current, pastWeeks } shape.
const props = defineProps({
  fetcher: { type: Function, default: () => api.getDailyQuizWeekly() },
  unit: { type: String, default: 'day' }, // what one "played" is - 'day' for the daily quiz, 'board' for the weekly games
  blurb: { type: String, default: _t('Poengene fra hver dag legges sammen fra mandag til søndag - høyest sum vinner.') }
})

const weekly = ref(null)
const showWinners = ref(false)

onMounted(async () => {
  try {
    weekly.value = await props.fetcher()
  } catch (e) {
    // a nice-to-have next to the quizzes themselves - if it can't load, the panel just isn't shown
  }
})

const standings = computed(() => weekly.value?.current.standings || [])
// The top ten (your own row is added under them when you're further down).
const TOP_N = 10
const topFive = computed(() => standings.value.slice(0, TOP_N))
// Equal totals share a place (13, 12, 12, 11 -> 1, 2, 2, 4).
const ranks = computed(() => sharedRanks(standings.value, s => s.total))

// The top players with the expert slotted in at their real total (after everyone who has at least as
// many points, before the first with fewer) - listed, but not ranked.
const rows = computed(() => {
  const list = topFive.value.map((entry, i) => ({ kind: 'player', entry, rank: ranks.value[i], index: i }))
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
  return idx === -1 ? null : { rank: ranks.value[idx], index: idx, entry: standings.value[idx] }
})

function daysNote(n) {
  return props.unit === 'board'
    ? _t('{0} brett spilt¦{0} brett spilt', [n])
    : _t('{0} dag spilt¦{0} dager spilt', [n])
}

// 01, 02 ... - the ledger sets rank in a fixed two-digit column
function pad(n) {
  return String(n).padStart(2, '0')
}

// First names, with a surname initial added where two people on the panel share one.
const shortName = computed(() => shortNames([
  ...standings.value.map(s => s.playerName),
  ...(weekly.value?.pastWeeks || []).flatMap(w => w.winners)
]))

function firstName(name) {
  return shortName.value(name)
}

const latestWeek = computed(() => weekly.value.pastWeeks[0])

// Wins per player across the past weeks the server sends, most first (ties broken by name).
const winTally = computed(() => {
  const wins = new Map()
  for (const w of weekly.value?.pastWeeks || []) {
    for (const name of w.winners) wins.set(name, (wins.get(name) || 0) + 1)
  }
  return [...wins.entries()]
    .map(([name, count]) => ({ name, wins: count }))
    .sort((a, b) => b.wins - a.wins || a.name.localeCompare(b.name))
    .slice(0, 5)
})

function winnersLabel(w) {
  return w.winners.map(firstName).join(' & ')
}

function formatRange(startIso, endIso) {
  const opts = { day: 'numeric', month: 'short' }
  return `${new Date(startIso).toLocaleDateString(window.QUIZ_LOCALE, opts)} - ${new Date(endIso).toLocaleDateString(window.QUIZ_LOCALE, opts)}`
}
</script>
