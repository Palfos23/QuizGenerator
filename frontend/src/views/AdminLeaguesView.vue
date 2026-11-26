<template>
  <div class="dq-page dq-page--wide">
    <header class="stat-head">
      <div>
        <span class="dq-eyebrow">Admin</span>
        <h1 class="dq-title">{{ $t('Ligaer') }}</h1>
      </div>
      <button class="dq-chip-btn" :disabled="loading" @click="load">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 11a8 8 0 10-2.3 5.7M20 5v6h-6" /></svg>
        {{ loading ? $t('Oppdaterer…§AdminStatisticsViewM') : $t('Oppdater§AdminStatisticsViewM') }}
      </button>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>
    <LoadingSkeleton v-if="loading && !data" />

    <template v-else-if="data">
      <div class="stat-kpis">
        <div v-for="k in kpis" :key="k.label" class="stat-kpi">
          <div class="stat-kpi-value">{{ k.value }}</div>
          <div class="stat-kpi-label">{{ k.label }}</div>
          <div v-if="k.note" class="stat-kpi-note">{{ k.note }}</div>
        </div>
      </div>

      <section class="dq-card stat-section">
        <div class="stat-section-head">
          <div>
            <h2>{{ $t('Alle ligaer') }}</h2>
            <p>{{ $t('Nyeste først. «Spilt» er hvor stor del av dagene de siste 7 dagene sesongens spillere faktisk spilte.') }}</p>
          </div>
          <input v-model="search" type="search" class="al-search" :placeholder="$t('Søk etter navn, kode eller eier')" :aria-label="$t('Søk etter navn, kode eller eier')" />
        </div>
        <p v-if="!data.rows.length" class="al-empty">{{ $t('Ingen har opprettet en liga ennå.') }}</p>
        <p v-else-if="!rows.length" class="al-empty">{{ $t('Ingen ligaer passer søket.') }}</p>
        <div v-else class="al-scroll">
          <table class="al-table">
            <thead>
              <tr>
                <th class="al-left">{{ $t('Liga') }}</th>
                <th class="al-left al-hide-sm">{{ $t('Eier') }}</th>
                <th>{{ $t('Medlemmer') }}</th>
                <th class="al-left al-hide-sm">{{ $t('Format') }}</th>
                <th class="al-left">{{ $t('Sesong') }}</th>
                <th>{{ $t('Spilt') }}</th>
                <th class="al-hide-sm">{{ $t('Opprettet') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in rows" :key="r.id">
                <td class="al-left"><router-link :to="`/admin/leagues/${r.id}`" class="al-name">{{ r.name }}</router-link><small class="al-code">{{ r.code }}</small></td>
                <td class="al-left al-hide-sm">{{ r.ownerName || $t('Slettet spiller') }}</td>
                <td>{{ r.memberCount }}</td>
                <td class="al-left al-hide-sm">{{ formatName(r.format) }}<small v-if="r.playoffs"> · {{ $t('sluttspill') }}</small></td>
                <td class="al-left"><span class="dq-pill" :class="statePill(r.state)">{{ stateName(r.state) }}</span> <small>{{ $t('nr. {0}', [r.seasonNumber]) }}</small></td>
                <td>{{ r.possibleLast7 ? Math.round(100 * r.playedLast7 / r.possibleLast7) + ' %' : '–' }}</td>
                <td class="al-hide-sm">{{ created(r.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { t as _t } from '../i18n'
import api from '../services/api'
import LoadingSkeleton from '../components/LoadingSkeleton.vue'
import { formatName, num, stateName } from '../composables/leagueFormat'

const data = ref(null)
const loading = ref(true)
const error = ref('')
const search = ref('')

const kpis = computed(() => {
  const d = data.value
  return [
    { label: _t('Ligaer'), value: d.leagues, note: _t('{0} pågår · {1} starter snart · {2} ferdige', [d.activeLeagues, d.upcomingLeagues, d.finishedLeagues]) },
    { label: _t('Spillere i ligaer'), value: d.players, note: _t('{0} medlemskap · snitt {1} per liga', [d.memberships, num(d.averageMembers)]) },
    { label: _t('Spilt siste 7 dager'), value: d.playedPercentLast7 == null ? '–' : `${d.playedPercentLast7} %`, note: _t('av dagene ligaspillerne kunne spilt') },
    { label: _t('Nye siste 30 dager'), value: d.createdLast30, note: _t('ligaer opprettet · {0} har blitt med', [d.joinedLast30]) },
    { label: _t('Format'), value: `${d.headToHead} / ${d.points}`, note: _t('head-to-head / poeng · {0} med sluttspill', [d.withPlayoffs]) }
  ]
})

const rows = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return data.value.rows
  return data.value.rows.filter(r => [r.name, r.code, r.ownerName].some(v => (v || '').toLowerCase().includes(q)))
})

function statePill(state) {
  if (state === 'ACTIVE') return 'dq-pill--ok'
  if (state === 'UPCOMING') return 'dq-pill--gold'
  return ''
}

function created(iso) {
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { day: 'numeric', month: 'short', year: 'numeric' })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await api.adminGetLeagues()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke laste ligaene.')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.stat-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 20px; }
.stat-kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 14px; margin-bottom: 22px; }
.stat-kpi { padding: 18px 20px; border-radius: var(--radius-sm); border: 1px solid var(--rule); background: var(--ink-raised); }
.stat-kpi-value { font-family: var(--font-display); font-size: 2.3rem; font-weight: 600; line-height: 1.1; }
.stat-kpi-label { margin-top: 4px; color: var(--text-dim); font-size: 0.86rem; }
.stat-kpi-note { margin-top: 8px; font-size: 0.8rem; font-weight: 600; color: var(--gold-ink); }
.stat-section { margin-bottom: 20px; }
.stat-section h2 { font-size: 1.05rem; margin: 0 0 4px; }
.stat-section-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; margin-bottom: 12px; }
.stat-section-head p { margin: 0; max-width: 52ch; color: var(--text-dim); font-size: 0.86rem; }
.al-search { width: 260px; max-width: 100%; }
.al-empty { margin: 0; color: var(--text-dim); }
.al-scroll { overflow-x: auto; }
.al-table { width: 100%; border-collapse: collapse; font-variant-numeric: tabular-nums; }
.al-table th { padding: 8px 6px; font-size: 0.74rem; font-weight: 700; letter-spacing: 0.04em; text-transform: uppercase; color: var(--text-dim); text-align: center; border-bottom: 1px solid var(--rule); }
.al-table td { padding: 10px 6px; text-align: center; border-bottom: 1px solid var(--rule); vertical-align: middle; }
.al-table tr:last-child td { border-bottom: none; }
.al-table .al-left { text-align: left; }
.al-table small { color: var(--text-dim); }
.al-name { font-weight: 700; }
.al-code { display: block; font-family: var(--font-mono, monospace); letter-spacing: 0.1em; font-size: 0.76rem; }
@media (max-width: 640px) {
  .al-hide-sm { display: none; }
  .stat-kpis { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .stat-kpi { padding: 14px; }
  .stat-kpi-value { font-size: 1.8rem; }
}
</style>
