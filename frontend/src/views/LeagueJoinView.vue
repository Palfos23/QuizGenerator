<template>
  <div class="dq-page lg-join-page">
    <LoadingSkeleton v-if="loading" />

    <!-- A visitor without an account: say what the invite is, and sign in from here -->
    <section v-else-if="!signedIn" class="dq-card lg-invite">
      <span class="dq-eyebrow">{{ $t('Invitasjon til en liga') }}</span>
      <h1 class="dq-title">{{ $t('Bli med i ligaen') }}</h1>
      <p class="dq-sub">{{ $t('Ligaer spilles på dagens quiz. Logg inn eller lag en gratis konto for å bli med.') }}</p>
      <button type="button" class="btn btn-primary btn-lg" @click="signIn">{{ $t('Logg inn og bli med') }}</button>
    </section>

    <section v-else-if="guest" class="dq-card lg-invite">
      <h1 class="dq-title">{{ $t('Bli med i ligaen') }}</h1>
      <p class="dq-sub">{{ $t('Ligaer krever en konto, siden poengene dine lagres over tid. Logg ut som gjest og logg inn med en konto.') }}</p>
    </section>

    <div v-else-if="error" class="dq-card lg-invite">
      <h1 class="dq-title">{{ $t('Fant ikke ligaen') }}</h1>
      <p class="dq-sub">{{ error }}</p>
      <router-link to="/ligaer" class="btn btn-secondary">{{ $t('Til ligaene dine') }}</router-link>
    </div>

    <section v-else-if="preview" class="dq-card lg-invite">
      <span class="dq-eyebrow">{{ $t('Invitasjon til en liga') }}</span>
      <h1 class="dq-title">{{ preview.name }}</h1>
      <p class="dq-sub">
        {{ formatName(preview.format) }} · {{ $t('{0} spiller¦{0} spillere', [preview.memberCount]) }} · {{ seasonSpan(preview) }}
      </p>
      <p v-if="!preview.joinsCurrentSeason && !preview.alreadyMember" class="lg-note">
        {{ $t('Oppgjørene for denne sesongen er allerede trukket. Du kan bli med nå og spille fra neste sesong.') }}
      </p>
      <p class="lg-note lg-note--quiet">{{ $t('Medlemmene ser poengene dine i ligaen. Du må svare på dagens quiz samme dag for at det skal telle.') }}</p>
      <router-link v-if="preview.alreadyMember" :to="`/ligaer/${preview.id}`" class="btn btn-primary btn-lg">{{ $t('Åpne ligaen') }}</router-link>
      <button v-else type="button" class="btn btn-primary btn-lg" :disabled="joining" :aria-busy="joining" @click="join">
        {{ joining ? $t('Blir med…') : $t('Bli med i ligaen') }}
      </button>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { t as _t } from '../i18n'
import api from '../services/api'
import auth from '../services/auth'
import loginDialog from '../services/loginDialog'
import LoadingSkeleton from '../components/LoadingSkeleton.vue'
import { formatName, seasonSpan } from '../composables/leagueFormat'

const route = useRoute()
const router = useRouter()
const code = String(route.params.code || '').toUpperCase()
const signedIn = computed(() => auth.isAuthenticated.value)
const guest = computed(() => auth.isGuest.value)
const preview = ref(null)
const loading = ref(true)
const error = ref('')
const joining = ref(false)

function signIn() {
  loginDialog.open({ redirect: `/liga/${code}`, reason: _t('Logg inn eller opprett en gratis konto for å bli med i ligaen.') })
}

async function join() {
  joining.value = true
  try {
    const league = await api.joinLeague(code)
    router.push(`/ligaer/${league.id}`)
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke bli med i ligaen.')
  } finally {
    joining.value = false
  }
}

onMounted(async () => {
  if (!signedIn.value || guest.value) {
    loading.value = false
    return
  }
  try {
    preview.value = await api.previewLeague(code)
  } catch (e) {
    error.value = e.response?.data?.message || _t('Sjekk koden og prøv igjen.')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.lg-join-page { max-width: 520px; }
.lg-invite { display: flex; flex-direction: column; align-items: flex-start; gap: 12px; padding: 28px; }
.lg-invite .dq-title { margin: 0; }
.lg-note { margin: 0; padding: 10px 12px; border: 1px solid var(--rule); border-radius: var(--radius-md); font-size: 0.9rem; }
.lg-note--quiet { border: none; padding: 0; color: var(--text-dim); }
</style>
