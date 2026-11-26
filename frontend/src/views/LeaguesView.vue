<template>
  <div class="dq-page">
    <header class="dq-hero">
      <h1 class="dq-title">{{ $t('Ligaer') }}</h1>
      <p class="dq-sub">{{ $t('Konkurrer med vennene dine på dagens quiz - head-to-head eller på poeng.') }}</p>
    </header>

    <div class="lg-actions">
      <router-link to="/ligaer/ny" class="btn btn-primary">{{ $t('Opprett en liga') }}</router-link>
      <form class="lg-join" @submit.prevent="goJoin">
        <label class="sr-only" for="lg-code">{{ $t('Ligakode') }}</label>
        <input id="lg-code" v-model="code" type="text" maxlength="8" autocomplete="off" autocapitalize="characters" spellcheck="false" :placeholder="$t('Ligakode')" />
        <button type="submit" class="btn btn-secondary">{{ $t('Bli med') }}</button>
      </form>
    </div>
    <p v-if="codeMissing" class="lg-error" role="alert">{{ $t('Skriv inn koden du har fått.') }}</p>

    <div v-if="error" class="banner error">{{ error }}</div>
    <LoadingSkeleton v-if="loading" />
    <div v-else class="lg-list">
      <!-- Everyone is in the global league: it is the daily quiz's own leaderboard -->
      <router-link to="/daily-quiz" class="lg-global">
        <AppIcon name="star" />
        <div>
          <div class="lg-global-title">{{ $t('Global liga') }}</div>
          <div class="lg-global-sub">{{ $t('Alle som spiller dagens quiz. Ukens vinner kåres hver søndag.') }}</div>
        </div>
        <span aria-hidden="true">→</span>
      </router-link>
      <LeagueCardItem v-for="c in cards" :key="c.id" :card="c" />
      <p v-if="!cards.length" class="lg-empty">{{ $t('Du er ikke med i noen egne ligaer ennå. Opprett en og del koden med vennene dine.') }}</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { t as _t } from '../i18n'
import api from '../services/api'
import AppIcon from '../components/AppIcon.vue'
import LoadingSkeleton from '../components/LoadingSkeleton.vue'
import LeagueCardItem from '../components/league/LeagueCardItem.vue'

const router = useRouter()
const cards = ref([])
const loading = ref(true)
const error = ref('')
const code = ref('')
const codeMissing = ref(false)

watch(code, () => { codeMissing.value = false })

function goJoin() {
  const c = code.value.trim().toUpperCase()
  if (!c) {
    codeMissing.value = true
    return
  }
  router.push(`/liga/${encodeURIComponent(c)}`)
}

onMounted(async () => {
  try {
    cards.value = await api.getMyLeagues()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke laste ligaene dine.')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.lg-actions { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; margin-bottom: 8px; }
.lg-join { display: flex; gap: 8px; flex: 1; min-width: 240px; }
.lg-join input { flex: 1; min-width: 0; text-transform: uppercase; letter-spacing: 0.1em; }
.lg-error { margin: 4px 0 0; color: var(--coral-ink); font-size: 0.9rem; }
.lg-list { display: flex; flex-direction: column; gap: 10px; margin-top: 20px; }
.lg-global { display: flex; align-items: center; gap: 14px; padding: 16px 18px; border: 1px dashed var(--rule); border-radius: var(--radius-lg); color: inherit; text-decoration: none; }
.lg-global .app-icon { width: 26px; height: 26px; color: var(--gold-ink); }
.lg-global > div { flex: 1; }
.lg-global-title { font-weight: 700; }
.lg-global-sub { color: var(--text-dim); font-size: 0.88rem; }
.lg-global > span { color: var(--text-dim); }
.lg-empty { margin: 8px 0 0; color: var(--text-dim); }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
</style>
