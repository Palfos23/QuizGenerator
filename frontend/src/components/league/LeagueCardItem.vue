<template>
  <router-link :to="`/ligaer/${card.id}`" class="lg-card" :class="{ 'is-nudge': nudge }">
    <div class="lg-card-main">
      <div class="lg-card-title">{{ card.name }}</div>
      <div class="lg-card-meta">
        {{ formatName(card.format) }} · {{ $t('Sesong {0}', [card.seasonNumber]) }}
        <template v-if="card.state === 'ACTIVE' && card.myRank"> · <strong>{{ $t('{0}. plass', [card.myRank]) }}</strong> {{ $t('av {0}', [card.participantCount]) }}</template>
      </div>
      <!-- The nudge lives here: no notifications on the web, so it shows when you come back -->
      <div class="lg-card-line" :class="{ 'is-alert': nudge }">{{ line }}</div>
    </div>
    <span class="lg-card-arrow" aria-hidden="true">→</span>
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import { t as _t } from '../../i18n'
import { formatName, playerName, shortDate } from '../../composables/leagueFormat'

const props = defineProps({ card: { type: Object, required: true } })

// "Your opponent has played today, you haven't"
const nudge = computed(() => props.card.state === 'ACTIVE' && props.card.todayOpponent && props.card.opponentPlayed && !props.card.iPlayed)

const line = computed(() => {
  const c = props.card
  if (c.state === 'FINISHED') {
    if (c.recapReady && c.champion) return _t('Sesongen er over - {0} vant. Se oppsummeringen.', [playerName(c.champion)])
    return _t('Sesongen er over.')
  }
  if (c.state === 'UPCOMING') return _t('Starter {0}', [shortDate(c.startDate)])
  if (!c.inSeason) return _t('Du er med fra neste sesong.')
  if (c.todayOpponent) {
    const name = playerName(c.todayOpponent)
    if (c.iPlayed) return _t('I dag mot {0} - du har spilt.', [name])
    if (c.opponentPlayed) return _t('{0} har spilt dagens quiz - din tur!', [name])
    return _t('I dag: du mot {0}', [name])
  }
  if (c.format === 'HEAD_TO_HEAD') return _t('Fri runde i dag.')
  return _t('{0} av {1} har spilt i dag', [c.playedToday, c.participantCount])
})
</script>

<style scoped>
.lg-card { display: flex; align-items: center; gap: 14px; padding: 16px 18px; border: 1px solid var(--rule); border-radius: var(--radius-lg); background: var(--ink-raised); color: inherit; text-decoration: none; }
.lg-card:hover { border-color: rgb(var(--primary-rgb) / 0.5); }
.lg-card.is-nudge { border-color: var(--gold); box-shadow: inset 3px 0 0 var(--gold); }
.lg-card-main { flex: 1; min-width: 0; }
.lg-card-title { font-weight: 700; font-size: 1.05rem; }
.lg-card-meta { margin-top: 2px; color: var(--text-dim); font-size: 0.86rem; }
.lg-card-meta strong { color: var(--text); }
.lg-card-line { margin-top: 8px; font-size: 0.92rem; }
.lg-card-line.is-alert { color: var(--gold-ink); font-weight: 600; }
.lg-card-arrow { color: var(--text-dim); transition: translate 150ms var(--ease-out); }
.lg-card:hover .lg-card-arrow { translate: 3px 0; color: var(--primary); }
</style>
