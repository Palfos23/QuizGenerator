<template>
  <div style="max-width:420px; margin:40px auto;">
    <h1>{{ $t('Bli med i et spill') }}</h1>
    <p class="page-subtitle">
      {{ $t('Har du fått en romkode fra noen som er vert for et spill? Skriv den inn under - du trenger ingen konto, bare et navn slik at de andre spillerne vet hvem du er.') }}
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field">
      <label>{{ $t('Romkode') }}
      <input
        ref="codeInput"
        type="text"
        v-model="code"
        :placeholder="$t('f.eks. ABCDE')"
        style="text-transform:uppercase; letter-spacing:0.1em; font-size:1.2rem; text-align:center;"
        maxlength="5"
        autocomplete="off"
        autocapitalize="characters"
        autocorrect="off"
        spellcheck="false"
        enterkeyhint="next"
        :aria-invalid="missing === 'code'"
        @input="onCodeInput"
        @keyup.enter="join"
      /></label>
    </div>

    <div v-if="needsName" class="field">
      <label>{{ $t('Navnet ditt') }} <span class="picker-hint">{{ $t('vises for andre spillere') }}</span><input ref="nameInput" type="text" v-model="displayName" :placeholder="$t('Navnet ditt')" autocomplete="nickname" maxlength="30" enterkeyhint="go" :aria-invalid="missing === 'name'" @keyup.enter="join" /></label>
    </div>

    <!-- Never greyed out for a missing field (a dim button says nothing about why): pressing it early
         points at what's missing instead. -->
    <button class="btn btn-primary btn-lg" style="width:100%;" :disabled="joining" :aria-busy="joining" @click="join">
      {{ joining ? $t('Blir med…') : $t('Bli med') }}
    </button>
    <p v-if="missing" class="join-hint" role="alert">
      {{ missing === 'code' ? $t('Skriv inn romkoden du har fått av verten.') : $t('Skriv inn et navn, så de andre ser hvem du er.') }}
    </p>

    <p v-if="needsName" style="margin-top:20px; text-align:center;">
      <router-link :to="{ path: '/', query: { redirect: signInRedirect } }">{{ $t('Har du en konto? Logg inn i stedet') }}</router-link>
    </p>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'

const route = useRoute()
const router = useRouter()

const code = ref((route.params.code || '').toUpperCase())
const displayName = ref('')
const error = ref('')
const joining = ref(false)
const codeInput = ref(null)
const nameInput = ref(null)
// Which field the visitor still has to fill in, once they've pressed "Bli med" without it ('code' | 'name' | null)
const missing = ref(null)
const CODE_LENGTH = 5

// An already-signed-in visitor (real account or a still-valid guest session
// from an earlier room) doesn't need to type a name again - reuse what's
// already there rather than forcing a fresh guest identity on top of it.
const needsName = computed(() => !auth.isAuthenticated.value)
const canSubmit = computed(() => code.value.trim().length > 0 && (!needsName.value || displayName.value.trim().length > 0))

// Signing in instead of joining as guest used to drop the room code entirely
// (a bare "/" link) - carry whatever code is currently typed as a ?redirect=
// so HomeView.vue lands them back here (see safeRedirectTarget) once signed in.
const signInRedirect = computed(() => {
  const c = code.value.trim().toUpperCase()
  return c ? `/join/${c}` : '/join'
})

// Route this game type's room code to the matching game view - each one
// already has its own "join with a code" flow (see joinOnlineRoom in
// MultiplayerGridView.vue and its four siblings); passing ?code= here just
// triggers that same flow automatically instead of the player retyping the
// code they just entered.
const ROUTE_BY_GAME_TYPE = {
  GRID_BATTLE: '/grid-battle',
  TENSION: '/tension',
  IMPOSTER: '/imposter',
  FIVE_O_ONE: '/501',
  STARTING_XI_BATTLE: '/starting-xi-battle',
  BULLSEYE: '/bullseye',
  FLASHBACK: '/flashback'
}

onMounted(async () => {
  if (route.params.code) {
    // Coming from a shared link (e.g. /join/ABCDE) - jump straight to typing
    // a name (or submitting, if already signed in) instead of retyping the code.
    code.value = String(route.params.code).toUpperCase()
  }
  // Ready to type straight away: the code, or the name when the link already carried the code
  await nextTick()
  const first = code.value.trim() ? nameInput.value : codeInput.value
  first?.focus({ preventScroll: true })
})

// A full code moves you on to the name, so it's code - name - enter without reaching for the next box
function onCodeInput() {
  if (code.value.trim().length >= CODE_LENGTH && needsName.value && !displayName.value.trim()) nameInput.value?.focus()
}

// The "what's missing" hint goes away as soon as it's been dealt with
watch([code, displayName], () => {
  if ((missing.value === 'code' && code.value.trim()) || (missing.value === 'name' && displayName.value.trim())) missing.value = null
})

async function join() {
  if (joining.value) return
  if (!canSubmit.value) {
    missing.value = code.value.trim() ? 'name' : 'code'
    const field = missing.value === 'code' ? codeInput.value : nameInput.value
    field?.focus()
    return
  }
  missing.value = null
  error.value = ''
  joining.value = true
  const roomCode = code.value.trim().toUpperCase()
  try {
    if (needsName.value) {
      const result = await api.loginAsGuest(displayName.value.trim())
      auth.login({ token: result.token, displayName: result.displayName, role: result.role })
    }
    const room = await api.getRoom(roomCode)
    const target = ROUTE_BY_GAME_TYPE[room.gameType]
    if (!target) {
      error.value = _t('Denne romtypen støttes ikke her.')
      return
    }
    router.push({ path: target, query: { code: roomCode } })
  } catch (e) {
    error.value = e.response?.data?.message || _t('Fant ikke rommet - sjekk koden og prøv igjen.')
  } finally {
    joining.value = false
  }
}
</script>

<style scoped>
.join-hint { margin: 10px 0 0; text-align: center; color: var(--coral-ink); font-size: 0.92rem; }
</style>
