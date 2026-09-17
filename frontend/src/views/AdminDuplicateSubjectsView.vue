<template>
  <div>
    <h1>Duplicate subjects</h1>
    <p class="page-subtitle">
      Subjects that may be the same person entered more than once - same name once special letters
      are normalized, a name that's a few characters off, or one entry that's just a last name. This
      is a review list only - nothing is merged or deleted automatically.
      <router-link to="/admin/athletes">Manage subjects →</router-link>
    </p>

    <div style="display:flex; gap:16px; flex-wrap:wrap; margin-bottom:20px;">
      <div class="field" style="margin-bottom:0; flex:1; min-width:180px;">
        <label>Category
          <select v-model="sportFilter">
            <option value="ALL">All categories</option>
            <option v-for="s in gridCategories.categories.value" :key="s" :value="s">{{ s }}</option>
          </select>
        </label>
      </div>
      <div class="field" style="margin-bottom:0; flex:1; min-width:220px;">
        <label>Match type <span class="picker-hint">how close names need to be to count as a duplicate</span>
          <select v-model.number="maxDistance">
            <option :value="0">100% match (plus last-name-only)</option>
            <option :value="1">1 character different</option>
            <option :value="2">2 characters different</option>
            <option :value="3">3 characters different</option>
          </select>
        </label>
      </div>
      <div style="display:flex; align-items:flex-end;">
        <button class="btn btn-primary" :disabled="loading" @click="scan">{{ loading ? 'Scanning…' : 'Scan' }}</button>
      </div>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="!hasScanned && !loading" class="empty-state friendly">
      Choose a category and match type, then hit Scan.
    </div>

    <div v-else-if="hasScanned && !loading && !groups.length" class="empty-state friendly">
      No likely duplicates found. Nice and tidy!
    </div>

    <div v-else-if="groups.length" class="saved-quiz-list">
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
import gridCategories from '../services/gridCategories'

const groups = ref([])
const loading = ref(false)
const error = ref('')
const hasScanned = ref(false)
const sportFilter = ref('ALL')
const maxDistance = ref(0)

onMounted(() => gridCategories.ensureLoaded())

async function scan() {
  loading.value = true
  error.value = ''
  try {
    groups.value = await api.adminFindDuplicateAthletes({
      sport: sportFilter.value === 'ALL' ? undefined : sportFilter.value,
      maxDistance: maxDistance.value
    })
    hasScanned.value = true
  } catch (e) {
    error.value = 'Could not scan for duplicate subjects.'
  } finally {
    loading.value = false
  }
}
</script>
