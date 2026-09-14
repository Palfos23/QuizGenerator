<template>
  <div>
    <h1>Duplicate subjects</h1>
    <p class="page-subtitle">
      Subjects that may be the same person entered more than once - same name once special letters
      are normalized, or one entry that's just a last name. This is a review list only - nothing is
      merged or deleted automatically. <router-link to="/admin/athletes">Manage subjects →</router-link>
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">Scanning…</div>

    <div v-else-if="!groups.length" class="empty-state friendly">
      No likely duplicates found. Nice and tidy!
    </div>

    <div v-else class="saved-quiz-list">
      <div v-for="(g, idx) in groups" :key="idx" class="saved-quiz-row" style="align-items:flex-start; flex-direction:column; gap:10px;">
        <div style="display:flex; justify-content:space-between; align-items:baseline; width:100%; flex-wrap:wrap; gap:6px;">
          <div class="saved-quiz-title">{{ sportLabel(g.sport) || 'Uncategorized' }}</div>
          <span class="tag" style="color:var(--coral); border-color:var(--coral);">{{ g.reason }}</span>
        </div>
        <div style="display:flex; gap:12px; flex-wrap:wrap; width:100%;">
          <div v-for="a in g.athletes" :key="a.id" style="display:flex; align-items:center; gap:10px; border:1px solid var(--border); border-radius:8px; padding:8px 12px; flex:1; min-width:220px;">
            <img v-if="a.photoUrl" :src="a.photoUrl" alt="" class="club-logo-thumb" />
            <div style="flex:1;">
              <div style="font-weight:600;">{{ a.name }}</div>
              <div class="saved-quiz-meta">{{ a.team || 'No team set' }}</div>
            </div>
            <router-link class="btn btn-secondary btn-sm" :to="`/admin/athletes?q=${encodeURIComponent(a.name)}`">Edit</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'
import { sportLabel } from '../constants'

const groups = ref([])
const loading = ref(true)
const error = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    groups.value = await api.adminFindDuplicateAthletes()
  } catch (e) {
    error.value = 'Could not scan for duplicate subjects.'
  } finally {
    loading.value = false
  }
}
</script>
