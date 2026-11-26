<template>
  <section class="lg-recap dq-card">
    <span class="dq-eyebrow">{{ $t('Sesong {0} - oppsummering', [seasonNumber]) }}</span>
    <p v-if="recap.provisional" class="lg-recap-note">{{ $t('Foreløpig: noen svar venter fortsatt på gjennomgang, så resultatet kan endre seg.') }}</p>

    <div v-if="recap.champion" class="lg-champion">
      <AppIcon name="crown" />
      <div>
        <div class="lg-champion-label">{{ $t('Mester') }}</div>
        <div class="lg-champion-name">{{ playerName(recap.champion) }}</div>
      </div>
    </div>
    <ol v-if="recap.runnerUp || recap.third" class="lg-podium">
      <li v-if="recap.runnerUp"><span>2.</span> {{ playerName(recap.runnerUp) }}</li>
      <li v-if="recap.third"><span>3.</span> {{ playerName(recap.third) }}</li>
    </ol>

    <div v-if="recap.highlights.length" class="lg-highlights">
      <div v-for="h in recap.highlights" :key="h.kind" class="lg-highlight">
        <div class="lg-highlight-label">{{ label(h) }}</div>
        <div class="lg-highlight-who">{{ playerName(h.player) }}</div>
        <div class="lg-highlight-detail">{{ detail(h) }}</div>
      </div>
    </div>

    <div v-if="recap.myRow" class="lg-mine">
      <strong>{{ $t('Din sesong') }}:</strong>
      {{ $t('{0}. plass av {1}', [recap.myRow.rank, recap.participants]) }}
      <template v-if="format === 'HEAD_TO_HEAD'"> · {{ recap.myRow.wins }}–{{ recap.myRow.draws }}–{{ recap.myRow.losses }}</template>
      <template v-if="recap.myStats?.average != null"> · {{ $t('snitt {0}', [num(recap.myStats.average)]) }}</template>
      <template v-if="recap.myStats"> · {{ $t('{0} dager spilt', [recap.myStats.played]) }}</template>
    </div>
  </section>
</template>

<script setup>
import { t as _t } from '../../i18n'
import AppIcon from '../AppIcon.vue'
import { num, playerName, shortDate } from '../../composables/leagueFormat'

defineProps({
  recap: { type: Object, required: true },
  seasonNumber: { type: Number, required: true },
  format: { type: String, required: true }
})

function label(h) {
  return {
    BEST_DAY: _t('Beste dag'),
    BEST_AVERAGE: _t('Best snitt'),
    MOST_PLAYED: _t('Mest trofast'),
    MOST_WINS: _t('Flest seire'),
    LONGEST_WIN_STREAK: _t('Lengste seiersrekke'),
    BIGGEST_WIN: _t('Største seier'),
    BEAT_EXPERT: _t('Slo eksperten oftest')
  }[h.kind] || ''
}

function detail(h) {
  switch (h.kind) {
    case 'BEST_DAY': return _t('{0} poeng, {1}', [h.value, shortDate(h.date)])
    case 'BEST_AVERAGE': return _t('{0} i snitt', [num(h.average)])
    case 'MOST_PLAYED': return _t('{0} av {1} dager', [h.value, h.otherValue])
    case 'MOST_WINS': return _t('{0} seire', [h.value])
    case 'LONGEST_WIN_STREAK': return _t('{0} på rad', [h.value])
    case 'BIGGEST_WIN': return _t('{0}–{1} mot {2}', [h.value, h.otherValue, playerName(h.other)])
    case 'BEAT_EXPERT': return _t('{0} dag¦{0} dager', [h.value])
    default: return ''
  }
}
</script>

<style scoped>
.lg-recap { display: flex; flex-direction: column; gap: 16px; margin-bottom: 24px; border-color: var(--gold); }
.lg-recap-note { margin: 0; color: var(--amber-ink); font-size: 0.9rem; }
.lg-champion { display: flex; align-items: center; gap: 14px; }
.lg-champion .app-icon { width: 40px; height: 40px; color: var(--gold-ink); }
.lg-champion-label { font-size: 0.78rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; color: var(--text-dim); }
.lg-champion-name { font-family: var(--font-display); font-size: 1.8rem; line-height: 1.1; }
.lg-podium { display: flex; flex-wrap: wrap; gap: 6px 22px; margin: 0; padding: 0; list-style: none; font-weight: 600; }
.lg-podium span { color: var(--text-dim); margin-right: 4px; }
.lg-highlights { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 10px; }
.lg-highlight { padding: 12px 14px; border: 1px solid var(--rule); border-radius: var(--radius-md); }
.lg-highlight-label { font-size: 0.74rem; font-weight: 700; letter-spacing: 0.05em; text-transform: uppercase; color: var(--text-dim); }
.lg-highlight-who { margin-top: 4px; font-weight: 700; }
.lg-highlight-detail { color: var(--text-dim); font-size: 0.88rem; }
.lg-mine { font-size: 0.94rem; }
</style>
