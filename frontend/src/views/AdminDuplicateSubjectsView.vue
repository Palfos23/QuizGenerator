<template>
  <div>
    <h1>{{ $t('Dupliserte emner') }}</h1>
    <p class="page-subtitle">
      {{ $t('Emner som kan være samme person registrert flere ganger - samme navn når spesialtegn normaliseres, et navn som er noen få tegn unna, eller én oppføring som bare er et etternavn. Dette er bare en gjennomgangsliste - ingenting slås sammen eller slettes automatisk.') }}
      <router-link to="/admin/athletes">{{ $t('Administrer emner →') }}</router-link>
    </p>

    <div style="display:flex; gap:16px; flex-wrap:wrap; margin-bottom:20px;">
      <div class="field" style="margin-bottom:0; flex:1; min-width:180px;">
        <label>{{ $t('Kategori') }}
          <select v-model="sportFilter">
            <option value="ALL">{{ $t('Alle kategorier') }}</option>
            <option v-for="s in gridCategories.categories.value" :key="s" :value="s">{{ s }}</option>
          </select>
        </label>
      </div>
      <div class="field" style="margin-bottom:0; flex:1; min-width:220px;">
        <label>{{ $t('Samsvarstype') }} <span class="picker-hint">{{ $t('hvor like navn må være for å regnes som duplikat') }}</span>
          <select v-model.number="maxDistance">
            <option :value="0">{{ $t('Samme navn (aksenter/spesialtegn ignoreres, pluss bare etternavn)') }}</option>
            <option :value="1">{{ $t('1 tegn forskjell') }}</option>
            <option :value="2">{{ $t('2 tegn forskjell') }}</option>
            <option :value="3">{{ $t('3 tegn forskjell') }}</option>
          </select>
        </label>
      </div>
      <div style="display:flex; align-items:flex-end;">
        <button class="btn btn-primary" :disabled="loading" @click="scan">{{ loading ? $t('Skanner…') : $t('Skann') }}</button>
      </div>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div v-if="!hasScanned && !loading" class="empty-state friendly">
      {{ $t('Velg en kategori og samsvarstype, og trykk deretter Skann.') }}
    </div>

    <div v-else-if="hasScanned && !loading && !groups.length" class="empty-state friendly">
      {{ $t('Ingen sannsynlige duplikater funnet. Fint og ryddig!') }}
    </div>

    <div v-else-if="groups.length" class="saved-quiz-list">
      <div v-for="(g, idx) in groups" :key="idx" class="saved-quiz-row" style="align-items:flex-start; flex-direction:column; gap:10px;">
        <div style="display:flex; justify-content:space-between; align-items:baseline; width:100%; flex-wrap:wrap; gap:6px;">
          <div class="saved-quiz-title">{{ sportLabel(g.sport) || $t('Uten kategori') }}</div>
          <span class="tag" style="color:var(--coral); border-color:var(--coral);">{{ g.reason }}</span>
        </div>
        <div style="display:flex; gap:12px; flex-wrap:wrap; width:100%;">
          <div v-for="a in g.athletes" :key="a.id" style="display:flex; align-items:center; gap:10px; border:1px solid var(--border); border-radius: 2px; padding:8px 12px; flex:1; min-width:220px;">
            <img v-if="a.photoUrl" :src="a.photoUrl" alt="" class="club-logo-thumb" />
            <div style="flex:1;">
              <div style="font-weight:600;">{{ a.name }}</div>
              <div class="saved-quiz-meta">{{ a.team || $t('Ingen lag satt') }}</div>
            </div>
            <router-link class="btn btn-secondary btn-sm" :to="`/admin/athletes?q=${encodeURIComponent(a.name)}`">{{ $t('Rediger') }}</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
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
    error.value = _t('Kunne ikke skanne etter dupliserte emner.')
  } finally {
    loading.value = false
  }
}
</script>
