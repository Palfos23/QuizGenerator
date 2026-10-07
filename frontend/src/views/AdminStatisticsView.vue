<template>
  <div class="dq-page dq-page--wide">
    <header class="stat-head">
      <div>
        <span class="dq-eyebrow">Admin</span>
        <h1 class="dq-title">Statistics</h1>
        <p class="dq-sub">A read-only snapshot of players, content and what's happening this week.</p>
      </div>
      <button class="dq-chip-btn" :disabled="loading" @click="load">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 11a8 8 0 10-2.3 5.7M20 5v6h-6" /></svg>
        {{ loading ? 'Refreshing…' : 'Refresh' }}
      </button>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading && !stats" style="color:var(--text-dim);">Loading…</div>

    <template v-else-if="stats">
      <!-- Waiting on an admin -->
      <div class="stat-attention">
        <router-link v-for="a in attention" :key="a.label" :to="a.to" class="stat-attn" :class="{ 'is-clear': !a.count }">
          <span class="stat-attn-count">{{ a.count }}</span>
          <span class="stat-attn-text">
            <strong>{{ a.label }}</strong>
            <small>{{ a.count ? a.hint : 'All clear' }}</small>
          </span>
          <svg class="dq-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6" /></svg>
        </router-link>
      </div>

      <!-- Headline figures -->
      <div class="stat-kpis">
        <div v-for="k in kpis" :key="k.label" class="stat-kpi" :class="`stat-kpi--${k.tone}`">
          <div class="stat-kpi-value">{{ k.value.toLocaleString() }}</div>
          <div class="stat-kpi-label">{{ k.label }}</div>
          <div v-if="k.note" class="stat-kpi-note">{{ k.note }}</div>
        </div>
      </div>

      <!-- Daily quiz, last 14 days -->
      <section class="dq-card stat-section">
        <div class="stat-section-head">
          <div>
            <h2>Daily quiz - last 14 days</h2>
            <p>Players who finished each day. Hover a bar for the day's average score.</p>
          </div>
          <div class="stat-summary">
            <strong>{{ dailyTotalPlayers }}</strong> finished<span v-if="dailyAveragePercent !== null"> · <strong>{{ dailyAveragePercent }}%</strong> average</span>
          </div>
        </div>
        <div class="stat-bars stat-bars--tall">
          <div
            v-for="d in stats.dailyQuizActivity"
            :key="d.date"
            class="stat-bar-col"
            :title="d.players ? `${formatDay(d.date)}: ${d.players} players, ${Math.round(d.averagePercent)}% average` : `${formatDay(d.date)}: nobody played`"
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
        <h2>This week's Grid</h2>
        <p class="stat-note">Completed attempts on every Grid whose live week covers today. Score counts answers found within a player's lives - overtime finds don't count.</p>
        <div v-if="!stats.weeklyGrids.length" class="dq-card dq-card--flat dq-empty" style="padding:22px;">No Grid is running this week.</div>
        <div v-else class="stat-grid-cards">
          <div v-for="g in stats.weeklyGrids" :key="g.gridId" class="dq-card stat-card">
            <div class="stat-card-head">
              <div>
                <div class="stat-card-title">{{ g.title }}</div>
                <div class="stat-card-sub">{{ g.category }} · {{ g.entryCount }} answers</div>
              </div>
              <span class="dq-pill dq-pill--year">{{ g.players }} played</span>
            </div>
            <template v-if="g.players">
              <div class="stat-card-metrics">
                <div><span class="stat-num">{{ g.averageScore.toFixed(1) }}</span><span class="stat-num-label">average</span></div>
                <div><span class="stat-num">{{ g.lowestScore }}</span><span class="stat-num-label">lowest</span></div>
                <div><span class="stat-num">{{ g.highestScore }}</span><span class="stat-num-label">highest</span></div>
                <div v-if="g.entryCount"><span class="stat-num">{{ Math.round(g.averageScore / g.entryCount * 100) }}%</span><span class="stat-num-label">of the grid</span></div>
              </div>
              <div v-if="g.entryCount" class="stat-range">
                <div class="stat-range-track">
                  <div class="stat-range-fill" :style="{ left: (g.lowestScore / g.entryCount * 100) + '%', width: ((g.highestScore - g.lowestScore) / g.entryCount * 100) + '%' }"></div>
                  <div class="stat-range-avg" :style="{ left: (g.averageScore / g.entryCount * 100) + '%' }"></div>
                </div>
                <div class="stat-range-ends"><span>0</span><span>{{ g.entryCount }}</span></div>
              </div>
            </template>
            <div v-else class="stat-card-empty">No completed attempts yet.</div>
          </div>
        </div>
      </section>

      <section class="stat-section stat-section--plain">
        <h2>This week's Starting XI</h2>
        <p class="stat-note">Completed attempts on the boards live this week. Score is how many of the eleven players were found.</p>
        <div v-if="!stats.weeklyLineups.length" class="dq-card dq-card--flat dq-empty" style="padding:22px;">No Starting XI board is running this week.</div>
        <div v-else class="stat-grid-cards">
          <div v-for="g in stats.weeklyLineups" :key="g.gridId" class="dq-card stat-card">
            <div class="stat-card-head">
              <div>
                <div class="stat-card-title">{{ g.title }}</div>
                <div class="stat-card-sub">{{ g.category }} · {{ g.entryCount }} players</div>
              </div>
              <span class="dq-pill dq-pill--year">{{ g.players }} played</span>
            </div>
            <template v-if="g.players">
              <div class="stat-card-metrics">
                <div><span class="stat-num">{{ g.averageScore.toFixed(1) }}</span><span class="stat-num-label">average</span></div>
                <div><span class="stat-num">{{ g.lowestScore }}</span><span class="stat-num-label">lowest</span></div>
                <div><span class="stat-num">{{ g.highestScore }}</span><span class="stat-num-label">highest</span></div>
                <div v-if="g.entryCount"><span class="stat-num">{{ Math.round(g.averageScore / g.entryCount * 100) }}%</span><span class="stat-num-label">of the XI</span></div>
              </div>
              <div v-if="g.entryCount" class="stat-range">
                <div class="stat-range-track">
                  <div class="stat-range-fill" :style="{ left: (g.lowestScore / g.entryCount * 100) + '%', width: ((g.highestScore - g.lowestScore) / g.entryCount * 100) + '%' }"></div>
                  <div class="stat-range-avg" :style="{ left: (g.averageScore / g.entryCount * 100) + '%' }"></div>
                </div>
                <div class="stat-range-ends"><span>0</span><span>{{ g.entryCount }}</span></div>
              </div>
            </template>
            <div v-else class="stat-card-empty">No completed attempts yet.</div>
          </div>
        </div>
      </section>

      <div class="stat-two-col">
        <section class="dq-card stat-section">
          <h2>New sign-ups per month</h2>
          <p class="stat-note">Last 12 months.</p>
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
          <h2>Battle games played</h2>
          <p class="stat-note">Completed games, online and pass-and-play combined.</p>
          <StatBarList :items="stats.battleGamesPlayed" color="var(--teal)" />
        </section>
      </div>

      <div class="stat-two-col">
        <section class="dq-card stat-section">
          <h2>Boards by game mode</h2>
          <StatBarList :items="stats.boardsByGameMode" color="var(--gold)" />
        </section>
        <section class="dq-card stat-section">
          <h2>Questions by language</h2>
          <StatBarList :items="stats.questionsByLanguage" color="#a99cff" empty="No questions yet." />
        </section>
      </div>

      <div class="stat-two-col">
        <section class="dq-card stat-section">
          <h2>Subjects by category</h2>
          <StatBarList :items="stats.subjectsByCategory" color="var(--teal)" />
        </section>
        <section class="dq-card stat-section">
          <h2>Grids by category</h2>
          <StatBarList :items="stats.gridsByCategory" color="var(--violet)" empty="No grids yet." />
        </section>
      </div>

      <section class="dq-card stat-section">
        <h2>Tension questions by category</h2>
        <StatBarList :items="stats.tensionQuestionsByCategory" color="var(--coral)" empty="No tension questions yet." />
      </section>
    </template>
  </div>
</template>

<script setup>
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
  { label: 'Daily quiz reviews', count: stats.value.dailyQuizPendingReviews, hint: 'answers waiting for a decision', to: '/admin/daily-quiz-review' },
  { label: 'Question submissions', count: stats.value.pendingSubmissions, hint: 'waiting for approval', to: '/admin/question-submissions' },
  { label: 'Open reports', count: stats.value.openReports, hint: 'problems reported by players', to: '/admin/reports' }
])

const kpis = computed(() => {
  const s = stats.value
  return [
    { label: 'Registered users', value: s.totalUsers, tone: 'gold', note: s.newUsersLast7Days ? `+${s.newUsersLast7Days} in the last 7 days` : 'No new sign-ups this week' },
    { label: 'Active this week', value: s.activePlayersThisWeek, tone: 'teal', note: s.totalUsers ? `${Math.round(s.activePlayersThisWeek / s.totalUsers * 100)}% of all users` : '' },
    { label: 'Questions in the bank', value: s.totalQuestions, tone: 'violet' },
    { label: 'Boards, all game modes', value: totalBoards.value, tone: 'coral' },
    { label: 'Subjects in the roster', value: s.totalSubjects, tone: 'teal' },
    { label: 'Battle games played', value: totalBattleGamesPlayed.value, tone: 'gold' }
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
  return new Date(iso).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' })
}
function dayLabel(iso) {
  const d = new Date(iso)
  return `${d.toLocaleDateString(undefined, { weekday: 'short' }).slice(0, 2)} ${d.getDate()}`
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
  const name = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'][Number(m) - 1]
  return `${name} ${y.slice(2)}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    stats.value = await api.adminGetStatistics()
  } catch (e) {
    error.value = 'Could not load statistics.'
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
  border-radius: 16px;
  text-decoration: none;
  color: var(--text);
  background: var(--dq-wait-bg);
  border: 1px solid rgba(255, 212, 92, 0.45);
  transition: transform 0.15s ease, background 0.15s ease;
}
.stat-attn:hover { transform: translateY(-2px); }
.stat-attn.is-clear { background: var(--dq-card); border-color: var(--dq-line); }
.stat-attn-count {
  display: grid;
  place-items: center;
  min-width: 44px;
  height: 44px;
  padding: 0 8px;
  border-radius: 12px;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.3rem;
  color: #3a2c00;
  background: var(--dq-wait);
}
.stat-attn.is-clear .stat-attn-count { color: var(--dq-ok); background: var(--dq-ok-bg); }
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
  border-radius: 18px;
  border: 1px solid var(--dq-line);
  background:
    radial-gradient(260px circle at 100% 0%, color-mix(in srgb, var(--accent) 22%, transparent), transparent 70%),
    linear-gradient(160deg, rgba(255, 255, 255, 0.11), rgba(255, 255, 255, 0.05));
  box-shadow: var(--dq-shadow);
}
.stat-kpi--gold { --accent: var(--gold); }
.stat-kpi--teal { --accent: var(--teal); }
.stat-kpi--violet { --accent: #a99cff; }
.stat-kpi--coral { --accent: #ff7893; }
.stat-kpi-value {
  font-family: var(--font-display);
  font-size: 2.1rem;
  font-weight: 700;
  line-height: 1.1;
}
.stat-kpi-label { margin-top: 4px; color: var(--text-dim); font-size: 0.86rem; }
.stat-kpi-note { margin-top: 8px; font-size: 0.8rem; font-weight: 650; color: var(--accent); }

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
  background: linear-gradient(180deg, var(--gold), #e08a1a);
  border-radius: 6px 6px 2px 2px;
  min-height: 2px;
  transition: height 0.4s ease, filter 0.15s ease;
}
.stat-bar-col-fill--teal { background: linear-gradient(180deg, #55e8ac, #1f9f78); }
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
.stat-range-track { position: relative; height: 8px; background: rgba(255, 255, 255, 0.10); border-radius: 999px; }
.stat-range-fill { position: absolute; top: 0; bottom: 0; background: rgba(85, 232, 172, 0.45); border-radius: 999px; min-width: 2px; }
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
