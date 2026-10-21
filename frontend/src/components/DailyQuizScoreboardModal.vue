<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal dq-modal" role="dialog" aria-modal="true" :aria-label="title">
      <h2 class="dq-modal-title">{{ title }}</h2>
      <p class="dq-modal-sub">{{ subtitle }}</p>

      <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

      <div v-else-if="!entries.length" class="dq-empty" style="padding:22px 0;">
        <strong>{{ $t('Ingen resultater ennå') }}</strong>
        {{ emptyText }}
      </div>

      <template v-else>
        <!-- Only for a viewer who is on the board - hidden for admins and anyone who hasn't played. -->
        <p v-if="yourDelta !== null" class="dq-delta" :class="{ 'is-up': yourDelta > 0, 'is-down': yourDelta < 0 }">
          {{ yourDelta === 0 ? $t('Akkurat på snittet') : (yourDelta > 0 ? $t('Du er {0} over snittet', [Math.abs(yourDelta)]) : $t('Du er {0} under snittet', [Math.abs(yourDelta)])) }}
        </p>

        <!-- A results ledger: rank, name, a dotted leader, the score. The winner's row is inverted. -->
        <div class="dq-ledger">
          <template v-for="row in rows" :key="row.kind === 'expert' ? 'expert' : row.entry.userName + row.rank">
            <!-- The expert sits in the list at their real score, but isn't ranked: a dash instead of a number. -->
            <div v-if="row.kind === 'expert'" class="dq-led dq-led--expert">
              <span class="dq-led-rank">&ndash;</span>
              <span class="dq-led-name"><i>{{ $t('Eksperten') }}</i><span class="dq-led-pill">{{ $t('Ikke rangert') }}</span></span>
              <span class="dq-led-dots"></span>
              <span class="dq-led-score">{{ row.score }}<small>/{{ row.maxScore }}</small></span>
            </div>

            <div v-else class="dq-led" :class="{ 'is-winner': row.rank === 1, 'is-you': row.entry.isYou }">
              <span class="dq-led-rank">{{ pad(row.rank) }}</span>
              <span class="dq-led-name">{{ displayName(row.entry.userName) }}<span v-if="row.entry.isYou" class="dq-you-tag" :title="$t('Det er deg')">{{ $t('DU') }}</span></span>
              <span class="dq-led-dots"></span>
              <span class="dq-led-score">{{ row.entry.score }}<small>/{{ row.entry.maxScore }}</small></span>
            </div>
          </template>

          <template v-if="yourRank && yourRank.rank > shownPlayers.length">
            <div class="dq-led-gap">···</div>
            <div class="dq-led is-you">
              <span class="dq-led-rank">{{ pad(yourRank.rank) }}</span>
              <span class="dq-led-name">{{ displayName(yourRank.entry.userName) }}<span class="dq-you-tag" :title="$t('Det er deg')">{{ $t('DU') }}</span></span>
              <span class="dq-led-dots"></span>
              <span class="dq-led-score">{{ yourRank.entry.score }}<small>/{{ yourRank.entry.maxScore }}</small></span>
            </div>
          </template>

          <div class="dq-led-foot">
            <span>{{ $t('{0} spiller¦{0} spillere', [entries.length]) }}</span>
            <span class="dq-led-foot-avg">{{ $t('snitt {0} / {1}', [data.averageScore.toFixed(1), data.maxScore]) }}</span>
          </div>
        </div>
      </template>

      <!-- Only for a player who has a graded result of their own - hidden for admins and for anyone
           who hasn't been graded yet. -->
      <label v-if="preference !== null && preference !== undefined" class="dq-switch">
        <input type="checkbox" :checked="preference" @change="emit('update:preference', $event.target.checked)" />
        <span class="dq-switch-track" aria-hidden="true"></span>
        <span>{{ $t('Vis navnet mitt på denne topplisten') }}</span>
      </label>

      <div class="dq-modal-actions">
        <button class="btn btn-secondary" @click="emit('close')">{{ $t('Lukk§DailyQuizScoreboardModalT') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed } from 'vue'
import { useEscapeKey } from '../composables/useEscapeKey'
import { shortNames } from '../services/displayNames'

// One scoreboard for the player's 'Toppliste' button and both admin views. `compact` is the player
// view (top five, plus your own row if you're further down, first names only); the admin view lists
// everyone with full names.
const props = defineProps({
  title: { type: String, default: _t('Toppliste') },
  data: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  compact: { type: Boolean, default: false },
  preference: { type: Boolean, default: null },
  subtitle: { type: String, default: _t('Hvordan det gikk med alle på denne quizen.') },
  emptyText: { type: String, default: _t('Ingen er ferdig rettet ennå.') }
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

// 01, 02 ... - the ledger sets rank in a fixed two-digit column
function pad(n) {
  return String(n).padStart(2, '0')
}
</script>
