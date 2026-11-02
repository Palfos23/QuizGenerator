<template>
  <div class="dq-page adv-page">
    <router-link to="/julekalender" class="adv-back">← {{ $t('Julekalenderen') }}</router-link>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

    <template v-else-if="w">
      <header class="dq-hero adv-hero">
        <span class="dq-eyebrow">🎁 {{ $t('Julekalender-wrapped') }} {{ w.year }}</span>
        <h1 class="dq-title">{{ $t('Slik gikk det for deg') }}</h1>
      </header>

      <section class="dq-card adv-w-main">
        <div class="adv-w-big">{{ w.totalPoints }}</div>
        <div class="adv-w-l">{{ $t('poeng¦poeng', [w.totalPoints]) }}</div>
        <p v-if="w.rank" class="adv-w-rank">{{ $t('Du endte på {0}. plass av {1} spillere', [w.rank, w.participants]) }}</p>
      </section>

      <div class="adv-w-grid">
        <div class="dq-card adv-w-card">
          <span class="adv-w-emoji">📅</span>
          <strong>{{ w.daysPlayed }} / {{ w.daysTotal }}</strong>
          <span>{{ $t('luker du var med på') }}</span>
        </div>
        <div class="dq-card adv-w-card">
          <span class="adv-w-emoji">🔥</span>
          <strong>{{ w.bestStreak }}</strong>
          <span>{{ $t('dager på rad - din lengste rekke') }}</span>
        </div>
        <div v-if="w.bestDay" class="dq-card adv-w-card">
          <span class="adv-w-emoji">🏅</span>
          <strong>{{ $t('Luke {0}', [w.bestDay.day]) }}: +{{ w.bestDay.points }}</strong>
          <span>{{ $t('din beste dag') }} - {{ w.bestDay.category }}</span>
        </div>
        <div class="dq-card adv-w-card">
          <span class="adv-w-emoji">🧠</span>
          <strong>{{ w.syncPercent }} %</strong>
          <span>{{ $t('av spillmesterens svar traff du - så likt tenkte dere') }}</span>
        </div>
        <div v-if="w.jokersUsed" class="dq-card adv-w-card">
          <span class="adv-w-emoji">🃏</span>
          <strong>+{{ w.jokerBonusPoints }}</strong>
          <span>{{ $t('ekstra poeng fra jokerne dine') }}</span>
        </div>
        <div v-if="w.risksTaken" class="dq-card adv-w-card">
          <span class="adv-w-emoji">🎲</span>
          <strong>{{ w.risksWon }} / {{ w.risksTaken }}</strong>
          <span>{{ $t('ganger du satset og traff') }}</span>
        </div>
        <div v-if="w.wildcardHits" class="dq-card adv-w-card">
          <span class="adv-w-emoji">🌟</span>
          <strong>{{ w.wildcardHits }}</strong>
          <span>{{ $t('wildcard-treff') }}</span>
        </div>
        <div v-if="w.perfectDays" class="dq-card adv-w-card">
          <span class="adv-w-emoji">🎯</span>
          <strong>{{ w.perfectDays }}</strong>
          <span>{{ $t('luker med alle 4 riktige') }}</span>
        </div>
      </div>

      <template v-if="earned.length">
        <h2 class="dq-section-title">{{ $t('Merkene dine') }}</h2>
        <div class="adv-badges">
          <div v-for="code in earned" :key="code" class="adv-badge">
            <span class="adv-badge-emoji" aria-hidden="true">{{ BADGES[code].emoji }}</span>
            <div><strong>{{ BADGES[code].name }}</strong><br /><small>{{ BADGES[code].text }}</small></div>
          </div>
        </div>
      </template>

      <template v-if="w.podium.length">
        <h2 class="dq-section-title">{{ $t('Pallen') }}</h2>
        <div class="dq-card adv-board">
          <div v-for="p in w.podium" :key="p.rank + p.name" class="adv-row" :class="{ 'is-you': p.you }">
            <span class="adv-row-rank">{{ medal(p.rank) }}</span>
            <span class="adv-row-name">{{ p.name }}</span>
            <span class="adv-row-points">{{ p.points }}</span>
          </div>
          <div class="adv-row adv-row--gm">
            <span class="adv-row-rank">🎩</span>
            <span class="adv-row-name">{{ $t('Spillmesteren') }}</span>
            <span class="adv-row-points">{{ w.gamemasterPoints }}</span>
          </div>
        </div>
      </template>

      <div style="margin-top:20px;">
        <button class="btn btn-primary" @click="share">{{ $t('Del wrapped-en din') }}</button>
        <span v-if="note" class="adv-saved" style="margin-left:10px;">{{ note }}</span>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { t as _t } from '../i18n'
import api from '../services/api'
import { BADGES, pointsText, shareOrCopy } from '../services/advent'

const w = ref(null)
const loading = ref(true)
const error = ref('')
const note = ref('')
const earned = computed(() => (w.value ? w.value.badges : []))

onMounted(async () => {
  try {
    w.value = await api.getAdventWrapped()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Julekalender-wrapped er ikke klar ennå.')
  } finally {
    loading.value = false
  }
})

function medal(rank) {
  return rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : rank
}

async function share() {
  const x = w.value
  const lines = [
    `🎁 ${_t('Julekalender-wrapped')} ${x.year}`,
    `🏆 ${pointsText(x.totalPoints)}${x.rank ? ` - ${_t('{0}. plass av {1}', [x.rank, x.participants])}` : ''}`,
    `📅 ${_t('{0} av {1} luker', [x.daysPlayed, x.daysTotal])} · 🔥 ${x.bestStreak}`,
    `🧠 ${_t('{0} % likt som spillmesteren', [x.syncPercent])}`,
    'quizzes.no/julekalender'
  ]
  const result = await shareOrCopy(lines.join('\n'))
  note.value = result === 'copied' ? _t('Kopiert!') : ''
  if (result === 'copied') setTimeout(() => { note.value = '' }, 2500)
}
</script>

<style scoped>
.adv-page { max-width: 640px; }
.adv-back { display: inline-block; margin-bottom: 10px; color: var(--text-dim); font-size: 0.9rem; text-decoration: none; }
.adv-saved { color: var(--teal-ink, var(--teal)); font-weight: 600; font-size: 0.92rem; }
.adv-w-main { text-align: center; padding: 28px 16px; background: rgb(var(--gold-rgb) / 0.08); }
.adv-w-big { font-family: var(--font-display); font-weight: 700; font-size: 5rem; line-height: 1; color: var(--gold-ink, var(--gold)); }
.adv-w-l { text-transform: uppercase; letter-spacing: 0.1em; font-size: 0.8rem; color: var(--text-dim); }
.adv-w-rank { margin: 12px 0 0; font-size: 1.05rem; }
.adv-w-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; margin-top: 12px; }
.adv-w-card { display: flex; flex-direction: column; gap: 4px; text-align: center; padding: 16px 12px; }
.adv-w-card strong { font-family: var(--font-display); font-size: 1.5rem; }
.adv-w-card span:last-child { color: var(--text-dim); font-size: 0.88rem; }
.adv-w-emoji { font-size: 1.8rem; }
.adv-badges { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 8px; }
.adv-badge { display: flex; gap: 10px; align-items: center; border: 2px solid var(--rule); border-radius: 3px; background: var(--ink-raised); padding: 10px 12px; }
.adv-badge small { color: var(--text-dim); }
.adv-badge-emoji { font-size: 1.6rem; }
.adv-board { padding: 6px 14px 12px; }
.adv-row { display: flex; align-items: baseline; gap: 12px; padding: 9px 0; border-top: 1px solid var(--line); }
.adv-row:first-child { border-top: none; }
.adv-row.is-you { background: rgb(var(--gold-rgb) / 0.09); margin: 0 -14px; padding: 9px 14px; }
.adv-row-rank { width: 28px; font-family: var(--font-mono); color: var(--text-dim); font-size: 0.95rem; }
.adv-row-name { flex: 1; min-width: 0; font-family: var(--font-display); font-weight: 600; overflow-wrap: anywhere; }
.adv-row-points { font-family: var(--font-display); font-weight: 700; font-size: 1.3rem; }
.adv-row--gm { border-top: 2px solid var(--rule); color: var(--text-dim); }
</style>
