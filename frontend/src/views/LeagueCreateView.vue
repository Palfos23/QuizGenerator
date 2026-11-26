<template>
  <div class="dq-page">
    <router-link to="/ligaer" class="dq-back">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6" /></svg>
      {{ $t('Ligaer') }}
    </router-link>
    <header class="dq-hero" style="margin-top:12px;">
      <h1 class="dq-title">{{ $t('Opprett en liga') }}</h1>
      <p class="dq-sub">{{ $t('Du får en kode du kan dele, så kan vennene dine bli med.') }}</p>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>
    <section class="dq-card">
      <LeagueSeasonForm :submit-label="busy ? $t('Oppretter…') : $t('Opprett ligaen')" :busy="busy" @submit="create">
        <div class="field" style="order:-1; margin:0;">
          <label>{{ $t('Navn på ligaen') }}
            <input ref="nameInput" v-model="name" type="text" maxlength="40" autocomplete="off" :aria-invalid="nameMissing" :placeholder="$t('F.eks. Lunsjgjengen')" />
          </label>
          <p v-if="nameMissing" class="lg-error" role="alert">{{ $t('Gi ligaen et navn.') }}</p>
        </div>
      </LeagueSeasonForm>
    </section>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { t as _t } from '../i18n'
import api from '../services/api'
import LeagueSeasonForm from '../components/league/LeagueSeasonForm.vue'

const router = useRouter()
const name = ref('')
const nameInput = ref(null)
const nameMissing = ref(false)
const busy = ref(false)
const error = ref('')

watch(name, () => { nameMissing.value = false })

async function create(settings) {
  if (!name.value.trim()) {
    nameMissing.value = true
    nameInput.value?.focus()
    return
  }
  busy.value = true
  error.value = ''
  try {
    const league = await api.createLeague({ name: name.value.trim(), ...settings })
    router.push({ path: `/ligaer/${league.id}`, query: { ny: '1' } })
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke opprette ligaen.')
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.lg-error { margin: 6px 0 0; color: var(--coral-ink); font-size: 0.9rem; }
</style>
