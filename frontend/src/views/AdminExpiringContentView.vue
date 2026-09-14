<template>
  <div>
    <h1>Can expire</h1>
    <p class="page-subtitle">
      Every quiz board across all the games that's been flagged as containing a time-sensitive fact
      (e.g. "current all-time top scorer") - use this to periodically recheck them. Flag a board as
      "Can expire" from its own edit screen in each game's admin page.
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>

    <div v-else-if="!sections.length" class="empty-state friendly">
      Nothing flagged as "Can expire" yet.
    </div>

    <div v-else>
      <div v-for="s in sections" :key="s.label" style="margin-bottom:28px;">
        <h2 style="margin-bottom:10px;">{{ s.label }} <span style="color:var(--text-dim); font-weight:400;">({{ s.items.length }})</span></h2>
        <div class="saved-quiz-list">
          <div v-for="item in s.items" :key="item.id" class="saved-quiz-row">
            <div class="saved-quiz-info">
              <div class="saved-quiz-title">{{ item.title }}</div>
              <div class="saved-quiz-meta">
                {{ item.subtitle }}<span v-if="item.subtitle"> · </span>Last updated {{ formatUpdatedAt(item.updatedAt) }}
              </div>
            </div>
            <router-link class="btn btn-secondary btn-sm" :to="s.to">Open in {{ s.label }} →</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'

const sections = ref([])
const loading = ref(true)
const error = ref('')

// One entry per game: how to load its boards, where "edit" sends the admin,
// and a short one-line context string per item (varies per game, since each
// has different fields worth showing at a glance here).
const GAMES = [
  {
    label: 'Weekly grids',
    to: '/admin/grids',
    load: () => api.adminListGrids(),
    subtitle: g => [g.sport, g.weekStartDate].filter(Boolean).join(' · ')
  },
  {
    label: 'Starting XI',
    to: '/admin/lineups',
    load: () => api.adminListLineups(),
    subtitle: l => [l.teamName, l.opponentName].filter(Boolean).join(' vs ')
  },
  {
    label: 'Tension',
    to: '/admin/tension-questions',
    load: () => api.adminListTensionQuestions(),
    subtitle: q => q.mainCategory || 'Uncategorized'
  },
  {
    label: '501',
    to: '/admin/501',
    load: () => api.adminListFiveOhOneCategories(),
    subtitle: c => c.description || ''
  },
  {
    label: 'Imposter',
    to: '/admin/imposter',
    load: () => api.adminListImposterGrids(),
    subtitle: g => g.sport || ''
  },
  {
    label: 'Bullseye',
    to: '/admin/bullseye',
    load: () => api.adminListBullseyeQuestions(),
    subtitle: q => [q.sport, q.statLabel].filter(Boolean).join(' · ')
  },
  {
    label: 'Flashback',
    to: '/admin/flashback',
    load: () => api.adminListFlashbackYears(),
    subtitle: y => y.year ? String(y.year) : ''
  }
]

onMounted(load)

function formatUpdatedAt(value) {
  if (!value) return 'unknown'
  return new Date(value).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const results = await Promise.all(GAMES.map(async g => {
      const all = await g.load()
      const flagged = all.filter(item => item.canExpire)
      return {
        label: g.label,
        to: g.to,
        items: flagged.map(item => ({ id: item.id, title: item.title, subtitle: g.subtitle(item), updatedAt: item.updatedAt }))
      }
    }))
    sections.value = results.filter(s => s.items.length)
  } catch (e) {
    error.value = 'Could not load "can expire" content across the games.'
  } finally {
    loading.value = false
  }
}
</script>
