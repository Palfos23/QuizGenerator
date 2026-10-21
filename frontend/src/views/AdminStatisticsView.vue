<template>
  <div class="dq-page dq-page--wide">
    <header class="stat-head">
      <div>
        <span class="dq-eyebrow">Admin</span>
        <h1 class="dq-title">{{ $t('Statistikk') }}</h1>
        <p class="dq-sub">{{ $t('Et skrivebeskyttet øyeblikksbilde av spillere, innhold og hva som skjer denne uken.') }}</p>
      </div>
      <button class="dq-chip-btn" :disabled="loading" @click="load">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 11a8 8 0 10-2.3 5.7M20 5v6h-6" /></svg>
        {{ loading ? $t('Oppdaterer…§AdminStatisticsViewM') : $t('Oppdater§AdminStatisticsViewM') }}
      </button>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading && !stats" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

    <template v-else-if="stats">
      <!-- Waiting on an admin -->
      <div class="stat-attention">
        <router-link v-for="a in attention" :key="a.label" :to="a.to" class="stat-attn" :class="{ 'is-clear': !a.count }">
          <span class="stat-attn-count">{{ a.count }}</span>
          <span class="stat-attn-text">
            <strong>{{ a.label }}</strong>
            <small>{{ a.count ? a.hint : $t('Alt i orden') }}</small>
          </span>
          <svg class="dq-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6" /></svg>
        </router-link>
      </div>

      <!-- Headline figures -->
      <div class="stat-kpis">
        <div v-for="k in kpis" :key="k.label" class="stat-kpi" :class="`stat-kpi--${k.tone}`">
          <div class="stat-kpi-value">{{ k.value.toLocaleString($locale) }}</div>
          <div class="stat-kpi-label">{{ k.label }}</div>
          <div v-if="k.note" class="stat-kpi-note">{{ k.note }}</div>
        </div>
      </div>

      <!-- Daily quiz, last 14 days -->
      <section class="dq-card stat-section">
        <div class="stat-section-head">
          <div>
            <h2>{{ $t('Dagens quiz - siste 14 dager') }}</h2>
            <p>{{ $t('Spillere som ble ferdige hver dag. Hold over en stolpe for dagens gjennomsnittspoeng.') }}</p>
          </div>
          <div class="stat-summary">
            <strong>{{ dailyTotalPlayers }}</strong> {{ $t('ferdige') }}<span v-if="dailyAveragePercent !== null"> · <strong>{{ dailyAveragePercent }}%</strong> {{ $t('snitt') }}</span>
          </div>
        </div>
        <div class="stat-bars stat-bars--tall">
          <div
            v-for="d in stats.dailyQuizActivity"
            :key="d.date"
            class="stat-bar-col"
            :title="d.players ? $t('{0}: {1} spillere, {2}% snitt', [formatDay(d.date), d.players, Math.round(d.averagePercent)]) : $t('{0}: ingen spilte', [formatDay(d.date)])"
          >
            <div class="stat-bar-col-value">{{ d.players || '' }}</div>
            <div class="stat-bar-col-track">
              <div class="stat-bar-col-fill stat-bar-col-fill--teal" :style="{ height: dayBarHeight(d.players) }"></div>
            </div>
            <div class="stat-bar-col-label">{{ dayLabel(d.date) }}</div>
          </div>
        </div>
      </section>

      <!-- This week's boards -->
      <section class="stat-section stat-section--plain">
        <h2>{{ $t('Ukens grid§AdminStatisticsViewT') }}</h2>
        <p class="stat-note">{{ $t('Fullførte forsøk på hvert grid der den aktive uken dekker i dag. Poengsummen teller svar funnet innenfor en spillers liv - funn i overtid teller ikke.') }}</p>
        <div v-if="!stats.weeklyGrids.length" class="dq-card dq-card--flat dq-empty" style="padding:22px;">{{ $t('Ingen grid er aktive denne uken.') }}</div>
        <div v-else class="stat-grid-cards">
          <div v-for="g in stats.weeklyGrids" :key="g.gridId" class="dq-card stat-card">
            <div class="stat-card-head">
              <div>
                <div class="stat-card-title">{{ g.title }}</div>
                <div class="stat-card-sub">{{ g.category }} · {{ g.entryCount }} {{ $t('svar') }}</div>
              </div>
              <span class="dq-pill dq-pill--year">{{ g.players }} {{ $t('spilt') }}</span>
            </div>
            <template v-if="g.players">
              <div class="stat-card-metrics">
                <div><span class="stat-num">{{ g.averageScore.toFixed(1) }}</span><span class="stat-num-label">{{ $t('snitt') }}</span></div>
                <div><span class="stat-num">{{ g.lowestScore }}</span><span class="stat-num-label">{{ $t('lavest') }}</span></div>
                <div><span class="stat-num">{{ g.highestScore }}</span><span class="stat-num-label">{{ $t('høyest') }}</span></div>
                <div v-if="g.entryCount"><span class="stat-num">{{ Math.round(g.averageScore / g.entryCount * 100) }}%</span><span class="stat-num-label">{{ $t('av gridet') }}</span></div>
              </div>
              <div v-if="g.entryCount" class="stat-range">
                <div class="stat-range-track">
                  <div class="stat-range-fill" :style="{ left: (g.lowestScore / g.entryCount * 100) + '%', width: ((g.highestScore - g.lowestScore) / g.entryCount * 100) + '%' }"></div>
                  <div class="stat-range-avg" :style="{ left: (g.averageScore / g.entryCount * 100) + '%' }"></div>
                </div>
                <div class="stat-range-ends"><span>0</span><span>{{ g.entryCount }}</span></div>
              </div>
            </template>
            <div v-else class="stat-card-empty">{{ $t('Ingen fullførte forsøk ennå.') }}</div>
          </div>
        </div>
      </section>

      <section class="stat-section stat-section--plain">
        <h2>{{ $t('Ukens Starting XI') }}</h2>
        <p class="stat-note">{{ $t('Fullførte forsøk på brettene som er aktive denne uken. Poengsummen er hvor mange av de elleve spillerne som ble funnet.') }}</p>
        <div v-if="!stats.weeklyLineups.length" class="dq-card dq-card--flat dq-empty" style="padding:22px;">{{ $t('Ingen Starting XI-brett er aktive denne uken.') }}</div>
        <div v-else class="stat-grid-cards">
          <div v-for="g in stats.weeklyLineups" :key="g.gridId" class="dq-card stat-card">
            <div class="stat-card-head">
              <div>
                <div class="stat-card-title">{{ g.title }}</div>
                <div class="stat-card-sub">{{ g.category }} · {{ g.entryCount }} {{ $t('spillere') }}</div>
              </div>
              <span class="dq-pill dq-pill--year">{{ g.players }} {{ $t('spilt') }}</span>
            </div>
            <template v-if="g.players">
              <div class="stat-card-metrics">
                <div><span class="stat-num">{{ g.averageScore.toFixed(1) }}</span><span class="stat-num-label">{{ $t('snitt') }}</span></div>
                <div><span class="stat-num">{{ g.lowestScore }}</span><span class="stat-num-label">{{ $t('lavest') }}</span></div>
                <div><span class="stat-num">{{ g.highestScore }}</span><span class="stat-num-label">{{ $t('høyest') }}</span></div>
                <div v-if="g.entryCount"><span class="stat-num">{{ Math.round(g.averageScore / g.entryCount * 100) }}%</span><span class="stat-num-label">{{ $t('av laget') }}</span></div>
              </div>
              <div v-if="g.entryCount" class="stat-range">
                <div class="stat-range-track">
                  <div class="stat-range-fill" :style="{ left: (g.lowestScore / g.entryCount * 100) + '%', width: ((g.highestScore - g.lowestScore) / g.entryCount * 100) + '%' }"></div>
                  <div class="stat-range-avg" :style="{ left: (g.averageScore / g.entryCount * 100) + '%' }"></div>
                </div>
                <div class="stat-range-ends"><span>0</span><span>{{ g.entryCount }}</span></div>
              </div>
            </template>
            <div v-else class="stat-card-empty">{{ $t('Ingen fullførte forsøk ennå.') }}</div>
          </div>
        </div>
      </section>

      <div class="stat-two-col">
        <section class="dq-card stat-section">
          <h2>{{ $t('Nye registreringer per måned') }}</h2>
          <p class="stat-note">{{ $t('Siste 12 måneder.') }}</p>
          <div class="stat-bars">
            <div v-for="m in stats.usersByMonth" :key="m.label" class="stat-bar-col" :title="`${shortMonth(m.label)}: ${m.count}`">
              <div class="stat-bar-col-value">{{ m.count }}</div>
              <div class="stat-bar-col-track">
                <div class="stat-bar-col-fill" :style="{ height: monthBarHeight(m.count) }"></div>
              </div>
              <div class="stat-bar-col-label">{{ shortMonth(m.label) }}</div>
            </div>
          </div>
        </section>

        <section class="dq-card stat-section">
          <h2>{{ $t('Spilte Battle-spill') }}</h2>
          <p class="stat-note">{{ $t('Fullførte spill, på nett og der enheten sendes rundt, til sammen.') }}</p>
          <StatBarList :items="stats.battleGamesPlayed" color="var(--teal)" />
        </section>
      </div>

      <div class="stat-two-col">
        <section class="dq-card stat-section">
          <h2>{{ $t('Brett etter spillmodus') }}</h2>
          <StatBarList :items="stats.boardsByGameMode" color="var(--gold)" />
        </section>
        <section class="dq-card stat-section">
          <h2>{{ $t('Spørsmål etter språk') }}</h2>
          <StatBarList :items="stats.questionsByLanguage" color="#3d4f9f" :empty="$t('Ingen spørsmål ennå.')" />
        </section>
      </div>

      <div class="stat-two-col">
        <section class="dq-card stat-section">
          <h2>{{ $t('Emner etter kategori') }}</h2>
          <StatBarList :items="stats.subjectsByCategory" color="var(--teal)" />
        </section>
        <section class="dq-card stat-section">
          <h2>{{ $t('Grids etter kategori') }}</h2>
          <StatBarList :items="stats.gridsByCategory" color="var(--violet)" :empty="$t('Ingen grids ennå.')" />
        </section>
      </div>

      <section class="dq-card stat-section">
        <h2>{{ $t('Tension-spørsmål etter kategori') }}</h2>
        <StatBarList :items="stats.tensionQuestionsByCategory" color="var(--coral)" :empty="$t('Ingen Tension-spørsmål ennå.')" />
      </section>
    </template>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import StatBarList from '../components/StatBarList.vue'

const stats = ref(null)
const loading = ref(true)
const error = ref('')

const totalBoards = computed(() =>
  (stats.value?.boardsByGameMode || []).reduce((sum, b) => sum + b.count, 0)
)

const totalBattleGamesPlayed = computed(() =>
  (stats.value?.battleGamesPlayed || []).reduce((sum, b) => sum + b.count, 0)
)

// Things waiting on an admin, each linking to where it gets handled.
const attention = computed(() => [
  { label: _t('Gjennomganger av dagens quiz'), count: stats.value.dailyQuizPendingReviews, hint: _t('svar venter på en avgjørelse'), to: '/admin/daily-quiz-review' },
  { label: _t('Innsendte spørsmål§AdminStatisticsViewJ'), count: stats.value.pendingSubmissions, hint: _t('venter på godkjenning'), to: '/admin/question-submissions' },
  { label: _t('Åpne rapporter'), count: stats.value.openReports, hint: _t('problemer meldt av spillere'), to: '/admin/reports' }
])

const kpis = computed(() => {
  const s = stats.value
  return [
    { label: _t('Registrerte brukere'), value: s.totalUsers, tone: 'gold', note: s.newUsersLast7Days ? _t('+{0} de siste 7 dagene', [s.newUsersLast7Days]) : _t('Ingen nye registreringer denne uken') },
    { label: _t('Aktive denne uken'), value: s.activePlayersThisWeek, tone: 'teal', note: s.totalUsers ? _t('{0}% av alle brukere', [Math.round(s.activePlayersThisWeek / s.totalUsers * 100)]) : '' },
    { label: _t('Spørsmål i banken'), value: s.totalQuestions, tone: 'violet' },
    { label: _t('Brett, alle spillmodi'), value: totalBoards.value, tone: 'coral' },
    { label: _t('Emner i oversikten'), value: s.totalSubjects, tone: 'teal' },
    { label: _t('Spilte Battle-spill'), value: totalBattleGamesPlayed.value, tone: 'gold' }
  ]
})

const dailyTotalPlayers = computed(() => (stats.value?.dailyQuizActivity || []).reduce((sum, d) => sum + d.players, 0))
// Weighted by players, so a day with 20 players counts for more than a day with 2.
const dailyAveragePercent = computed(() => {
  const days = (stats.value?.dailyQuizActivity || []).filter(d => d.players)
  const total = days.reduce((sum, d) => sum + d.players, 0)
  if (!total) return null
  return Math.round(days.reduce((sum, d) => sum + d.averagePercent * d.players, 0) / total)
})
const maxDayPlayers = computed(() => Math.max(...(stats.value?.dailyQuizActivity || []).map(d => d.players), 1))
function dayBarHeight(players) {
  return players ? Math.max(4, players / maxDayPlayers.value * 100) + '%' : '2px'
}
function formatDay(iso) {
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { weekday: 'short', day: 'numeric', month: 'short' })
}
function dayLabel(iso) {
  const d = new Date(iso)
  return `${d.toLocaleDateString(window.QUIZ_LOCALE, { weekday: 'short' }).slice(0, 2)} ${d.getDate()}`
}

const maxMonth = computed(() =>
  Math.max(...(stats.value?.usersByMonth || []).map(m => m.count), 1)
)

function monthBarHeight(count) {
  return Math.max(2, count / maxMonth.value * 100) + '%'
}

function shortMonth(label) {
  // label is "YYYY-MM"
  const [y, m] = label.split('-')
  const name = [_t('jan'), _t('feb'), _t('mar'), _t('apr'), _t('mai'), _t('jun'), _t('jul'), _t('aug'), _t('sep'), _t('okt'), _t('nov'), _t('des')][Number(m) - 1]
  return `${name} ${y.slice(2)}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    stats.value = await api.adminGetStatistics()
  } catch (e) {
    error.value = _t('Kunne ikke laste statistikk.')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.stat-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
}

/* Needs attention */
.stat-attention {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}
.stat-attn {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 2px;
  text-decoration: none;
  color: var(--text);
  background: var(--ink-raised);
  border: 2px solid var(--rule);
  transition: transform 0.12s ease, box-shadow 0.12s ease;
}
.stat-attn:hover { transform: translate(-1px, -1px); box-shadow: 4px 4px 0 var(--gold); }
.stat-attn.is-clear { border-color: var(--line); }
.stat-attn.is-clear:hover { box-shadow: none; transform: none; }
.stat-attn-count {
  display: grid;
  place-items: center;
  min-width: 44px;
  height: 44px;
  padding: 0 8px;
  border-radius: 2px;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.4rem;
  color: #fff;
  background: var(--gold);
}
.stat-attn.is-clear .stat-attn-count { color: var(--teal); background: transparent; border: 1.5px solid var(--teal); }
.stat-attn-text { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.stat-attn-text small { color: var(--text-dim); font-size: 0.82rem; }
.stat-attn .dq-chevron { flex-shrink: 0; }

/* Headline figures */
.stat-kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
  margin-bottom: 22px;
}
.stat-kpi {
  --accent: var(--gold);
  padding: 18px 20px;
  border-radius: 2px;
  border: 2px solid var(--rule);
  background: var(--ink-raised);
  box-shadow: none;
}
.stat-kpi--gold { --accent: var(--gold); }
.stat-kpi--teal { --accent: var(--teal); }
.stat-kpi--violet { --accent: var(--violet); }
.stat-kpi--coral { --accent: var(--coral); }
.stat-kpi-value {
  font-family: var(--font-display);
  font-size: 2.3rem;
  font-weight: 600;
  line-height: 1.1;
}
.stat-kpi-label { margin-top: 4px; color: var(--text-dim); font-size: 0.86rem; }
.stat-kpi-note { margin-top: 8px; font-size: 0.8rem; font-weight: 600; color: var(--accent); }

/* Sections */
.stat-section { margin-bottom: 20px; }
.stat-section h2 { font-size: 1.05rem; margin: 0 0 4px; }
.stat-section--plain { padding: 0; background: none; border: none; box-shadow: none; }
.stat-section--plain h2 { margin-bottom: 4px; }
.stat-note { margin: 0 0 14px; color: var(--text-dim); font-size: 0.86rem; }
.stat-section-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; margin-bottom: 6px; }
.stat-section-head p { margin: 0; color: var(--text-dim); font-size: 0.86rem; }
.stat-summary { color: var(--text-dim); font-size: 0.9rem; }
.stat-summary strong { color: var(--text); }

.stat-two-col {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}
.stat-two-col .stat-section { margin-bottom: 0; }

/* Vertical bar charts */
.stat-bars {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 170px;
  padding-top: 18px;
}
.stat-bars--tall { height: 190px; }
.stat-bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  min-width: 0;
  cursor: default;
}
.stat-bar-col-value {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  color: var(--text-dim);
  margin-bottom: 4px;
  min-height: 1em;
}
.stat-bar-col-track { flex: 1; width: 100%; display: flex; align-items: flex-end; }
.stat-bar-col-fill {
  width: 100%;
  background: var(--text);
  border-radius: 0;
  min-height: 2px;
  transition: height 0.4s ease, filter 0.15s ease;
}
.stat-bar-col-fill--teal { background: var(--teal); }
.stat-bar-col:hover .stat-bar-col-fill { filter: brightness(1.18); }
.stat-bar-col-label {
  margin-top: 6px;
  font-size: 0.66rem;
  color: var(--text-dim);
  white-space: nowrap;
}
.stat-bars:not(.stat-bars--tall) .stat-bar-col-label { transform: rotate(-35deg); transform-origin: center; }

/* This week's board cards */
.stat-grid-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(290px, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}
.stat-card { padding: 18px; }
.stat-card-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; }
.stat-card-title { font-weight: 650; }
.stat-card-sub { margin-top: 2px; color: var(--text-dim); font-size: 0.82rem; }
.stat-card-metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-top: 16px; }
.stat-card-metrics > div { display: flex; flex-direction: column; gap: 2px; }
.stat-num { font-family: var(--font-display); font-size: 1.3rem; font-weight: 700; }
.stat-num-label { font-size: 0.72rem; color: var(--text-dim); }
.stat-card-empty { margin-top: 12px; color: var(--text-dim); font-size: 0.86rem; }
.stat-range { margin-top: 16px; }
.stat-range-track { position: relative; height: 8px; background: rgb(var(--fg-rgb) / 0.10); border-radius: 999px; }
.stat-range-fill { position: absolute; top: 0; bottom: 0; background: rgb(var(--teal-rgb) / 0.45); border-radius: 999px; min-width: 2px; }
.stat-range-avg { position: absolute; top: -3px; width: 3px; height: 14px; background: var(--dq-ok); border-radius: 2px; transform: translateX(-50%); }
.stat-range-ends { display: flex; justify-content: space-between; margin-top: 4px; font-size: 0.7rem; color: var(--text-dim); }

@media (max-width: 640px) {
  .stat-kpis { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .stat-kpi { padding: 14px; }
  .stat-kpi-value { font-size: 1.7rem; }
  .stat-card-metrics { grid-template-columns: repeat(2, 1fr); }
  .stat-bars { gap: 3px; }
  .stat-bar-col-value { font-size: 0.62rem; }
}
</style>
