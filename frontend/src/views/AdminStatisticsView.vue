<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:24px;">
      <div>
        <h1>Statistics</h1>
        <p class="page-subtitle">A read-only snapshot of players, content and this week's Grid results.</p>
      </div>
      <button class="btn btn-secondary btn-sm" :disabled="loading" @click="load">
        {{ loading ? 'Refreshing…' : 'Refresh' }}
      </button>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="loading && !stats" style="color:var(--text-dim);">Loading…</div>

    <template v-else-if="stats">
      <!-- Headline figures -->
      <div class="stat-kpis">
        <div class="stat-kpi">
          <div class="stat-kpi-value">{{ stats.totalUsers.toLocaleString() }}</div>
          <div class="stat-kpi-label">Registered users</div>
        </div>
        <div class="stat-kpi">
          <div class="stat-kpi-value">{{ totalBoards.toLocaleString() }}</div>
          <div class="stat-kpi-label">Boards across all game modes</div>
        </div>
        <div class="stat-kpi">
          <div class="stat-kpi-value">{{ stats.totalSubjects.toLocaleString() }}</div>
          <div class="stat-kpi-label">Subjects in the roster</div>
        </div>
        <div class="stat-kpi">
          <div class="stat-kpi-value">{{ stats.totalCategories.toLocaleString() }}</div>
          <div class="stat-kpi-label">Categories</div>
        </div>
      </div>

      <!-- This week's Grid -->
      <section class="stat-section">
        <h2>This week's Grid</h2>
        <p class="page-subtitle" style="margin-top:0;">
          Completed attempts on every Grid whose live week covers today. Score counts correct
          answers found within a player's lives — overtime finds don't count.
        </p>

        <div v-if="!stats.weeklyGrids.length" class="empty-state">
          No Grid is running this week.
        </div>

        <div v-else class="stat-grid-cards">
          <div v-for="g in stats.weeklyGrids" :key="g.gridId" class="stat-card">
            <div class="stat-card-head">
              <div>
                <div class="stat-card-title">{{ g.title }}</div>
                <div class="stat-card-sub">{{ g.category }} · {{ g.entryCount }} answers · week of {{ g.weekStartDate }}</div>
              </div>
              <span class="tag" style="background:rgba(139,124,255,0.16); color:var(--violet);">{{ g.players }} played</span>
            </div>

            <div v-if="g.players" class="stat-card-metrics">
              <div><span class="stat-num">{{ g.averageScore.toFixed(1) }}</span><span class="stat-num-label">avg score</span></div>
              <div><span class="stat-num">{{ g.lowestScore }}</span><span class="stat-num-label">lowest</span></div>
              <div><span class="stat-num">{{ g.highestScore }}</span><span class="stat-num-label">highest</span></div>
              <div v-if="g.entryCount"><span class="stat-num">{{ Math.round(g.averageScore / g.entryCount * 100) }}%</span><span class="stat-num-label">of the grid</span></div>
            </div>
            <div v-else class="stat-card-empty">No completed attempts yet.</div>

            <div v-if="g.players && g.entryCount" class="stat-range">
              <div class="stat-range-track">
                <div
                  class="stat-range-fill"
                  :style="{
                    left: (g.lowestScore / g.entryCount * 100) + '%',
                    width: ((g.highestScore - g.lowestScore) / g.entryCount * 100) + '%'
                  }"
                ></div>
                <div class="stat-range-avg" :style="{ left: (g.averageScore / g.entryCount * 100) + '%' }"></div>
              </div>
              <div class="stat-range-ends">
                <span>0</span><span>{{ g.entryCount }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Boards by game mode -->
      <section class="stat-section">
        <h2>Boards by game mode</h2>
        <StatBarList :items="stats.boardsByGameMode" color="var(--gold)" />
      </section>

      <div class="stat-two-col">
        <!-- Sign-ups per month -->
        <section class="stat-section">
          <h2>New sign-ups per month</h2>
          <p class="page-subtitle" style="margin-top:0;">Last 12 months.</p>
          <div class="stat-bars">
            <div v-for="m in stats.usersByMonth" :key="m.label" class="stat-bar-col">
              <div class="stat-bar-col-value">{{ m.count }}</div>
              <div class="stat-bar-col-track">
                <div
                  class="stat-bar-col-fill"
                  :style="{ height: monthBarHeight(m.count) }"
                ></div>
              </div>
              <div class="stat-bar-col-label">{{ shortMonth(m.label) }}</div>
            </div>
          </div>
        </section>

        <!-- Subjects by category -->
        <section class="stat-section">
          <h2>Subjects by category</h2>
          <StatBarList :items="stats.subjectsByCategory" color="var(--teal)" />
        </section>
      </div>

      <div class="stat-two-col">
        <section class="stat-section">
          <h2>Grids by category</h2>
          <StatBarList :items="stats.gridsByCategory" color="var(--violet)" empty="No grids yet." />
        </section>

        <section class="stat-section">
          <h2>Tension questions by category</h2>
          <StatBarList :items="stats.tensionQuestionsByCategory" color="var(--coral)" empty="No tension questions yet." />
        </section>
      </div>
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
.stat-kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 14px;
  margin-bottom: 28px;
}
.stat-kpi {
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: rgba(255,255,255,0.02);
}
.stat-kpi-value {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 700;
  line-height: 1.1;
  color: var(--text);
}
.stat-kpi-label {
  margin-top: 4px;
  color: var(--text-dim);
  font-size: 0.85rem;
}

.stat-section {
  margin-bottom: 28px;
}
.stat-section h2 {
  font-size: 1.05rem;
  margin: 0 0 10px;
}

.stat-two-col {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
}

/* Vertical bar chart (sign-ups) */
.stat-bars {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 160px;
  padding-top: 18px;
}
.stat-bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  min-width: 0;
}
.stat-bar-col-value {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  color: var(--text-dim);
  margin-bottom: 4px;
}
.stat-bar-col-track {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
}
.stat-bar-col-fill {
  width: 100%;
  background: var(--gold);
  border-radius: 4px 4px 0 0;
  min-height: 2px;
  transition: height 0.4s ease;
}
.stat-bar-col-label {
  margin-top: 6px;
  font-size: 0.65rem;
  color: var(--text-dim);
  white-space: nowrap;
  transform: rotate(-35deg);
  transform-origin: center;
}

/* This week's Grid cards */
.stat-grid-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}
.stat-card {
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: rgba(255,255,255,0.02);
}
.stat-card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}
.stat-card-title {
  font-weight: 600;
}
.stat-card-sub {
  margin-top: 2px;
  color: var(--text-dim);
  font-size: 0.8rem;
}
.stat-card-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 14px;
}
.stat-card-metrics > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.stat-num {
  font-family: var(--font-display);
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text);
}
.stat-num-label {
  font-size: 0.7rem;
  color: var(--text-dim);
}
.stat-card-empty {
  margin-top: 12px;
  color: var(--text-dim);
  font-size: 0.85rem;
}
.stat-range {
  margin-top: 14px;
}
.stat-range-track {
  position: relative;
  height: 8px;
  background: rgba(255,255,255,0.05);
  border-radius: 999px;
}
.stat-range-fill {
  position: absolute;
  top: 0;
  bottom: 0;
  background: rgba(61,220,151,0.35);
  border-radius: 999px;
  min-width: 2px;
}
.stat-range-avg {
  position: absolute;
  top: -3px;
  width: 3px;
  height: 14px;
  background: var(--teal);
  border-radius: 2px;
  transform: translateX(-50%);
}
.stat-range-ends {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
  font-size: 0.7rem;
  color: var(--text-dim);
}
</style>
