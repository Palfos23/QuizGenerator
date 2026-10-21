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
        <template v-for="row in rows" :key="row.kind === 'expert' ? 'expert' : row.entry.playerName + row.rank">
          <!-- The expert is in the list at their real total, but not ranked: a dash instead of a number. -->
          <div v-if="row.kind === 'expert'" class="dq-led dq-led--expert">
            <span class="dq-led-rank">&ndash;</span>
            <span class="dq-led-name">
              <i>{{ $t('Eksperten') }}</i><span class="dq-led-pill">{{ $t('Ikke rangert') }}</span>
              <small class="dq-led-meta">{{ daysNote(row.daysPlayed) }}</small>
            </span>
            <span class="dq-led-dots"></span>
            <span class="dq-led-score">{{ row.total }}<small>{{ $t('poeng§DailyQuizWeeklyPanelPts') }}</small></span>
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

        <template v-if="yourRank && yourRank.rank > 5">
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

      <template v-if="weekly.pastWeeks.length">
        <div class="dq-weekly-label">{{ $t('Tidligere vinnere') }}</div>

        <!-- Last week's winner gets the spotlight; the weeks before it are a plain dated roll. -->
        <div class="dq-champ">
          <svg class="dq-champ-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M3 8l4.5 4L12 5l4.5 7L21 8l-2 11H5L3 8z" /></svg>
          <div class="dq-champ-main">
            <div class="dq-champ-when">{{ $t('Forrige uke') }} &middot; {{ formatRange(latestWeek.weekStart, latestWeek.weekEnd) }}</div>
            <div class="dq-champ-name">{{ winnersLabel(latestWeek) }}</div>
            <div v-if="latestWeek.winners.length > 1 || latestWeek.provisional" class="dq-champ-flags">
              <span v-if="latestWeek.winners.length > 1" class="dq-champ-pill">{{ $t('Delt') }}</span>
              <span v-if="latestWeek.provisional" class="dq-champ-pill" :title="$t('Noen av svarene den uken ble fortsatt gjennomgått')">{{ $t('Foreløpig') }}</span>
            </div>
          </div>
          <div class="dq-champ-score">{{ latestWeek.winningScore }}<small>{{ $t('poeng§DailyQuizWeeklyPanelT') }}</small></div>
        </div>

        <!-- Only last week's winner is on show; the earlier weeks and the win tally open on request. -->
        <template v-if="olderWeeks.length">
          <button type="button" class="dq-more-toggle" :aria-expanded="showEarlier" @click="showEarlier = !showEarlier">
            {{ showEarlier ? $t('Skjul tidligere vinnere') : $t('Vis tidligere vinnere') }}
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" :class="{ 'is-open': showEarlier }"><path d="M6 9l6 6 6-6" /></svg>
          </button>
          <div v-if="showEarlier">
            <div v-for="w in olderWeeks" :key="w.weekStart" class="dq-roll">
              <span class="dq-roll-when">{{ formatRange(w.weekStart, w.weekEnd) }}</span>
              <span class="dq-roll-name">{{ winnersLabel(w) }}</span>
              <span v-if="w.winners.length > 1" class="dq-led-pill">{{ $t('Delt') }}</span>
              <span v-if="w.provisional" class="dq-led-pill" :title="$t('Noen av svarene den uken ble fortsatt gjennomgått')">{{ $t('Foreløpig') }}</span>
              <span class="dq-led-dots"></span>
              <span class="dq-roll-score">{{ $t('{0} poeng¦{0} poeng', [w.winningScore]) }}</span>
            </div>

            <!-- Who has won the most of the weeks listed above (a tie counts as a win for each winner). -->
            <template v-if="winTally.length">
              <div class="dq-weekly-label">{{ $t('Flest seiere') }}</div>
              <div v-for="row in winTally" :key="row.name" class="dq-tally">
                <span class="dq-tally-name">{{ firstName(row.name) }}</span>
                <span class="dq-tally-pips" aria-hidden="true"><i v-for="n in row.wins" :key="n"></i></span>
                <span class="dq-led-dots"></span>
                <span class="dq-tally-count">{{ row.wins }}</span>
              </div>
            </template>
          </div>
        </template>
      </template>
    </div>
  </section>
</template>

<script setup>
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
const showEarlier = ref(false)

onMounted(async () => {
  try {
    weekly.value = await props.fetcher()
  } catch (e) {
    // a nice-to-have next to the quizzes themselves - if it can't load, the panel just isn't shown
  }
})

const standings = computed(() => weekly.value?.current.standings || [])
const topFive = computed(() => standings.value.slice(0, 5))

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
const olderWeeks = computed(() => weekly.value.pastWeeks.slice(1))

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
