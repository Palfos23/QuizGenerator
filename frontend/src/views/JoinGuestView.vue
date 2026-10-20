<template>
  <div style="max-width:420px; margin:40px auto;">
    <h1>Bli med i et spill</h1>
    <p class="page-subtitle">
      Har du fått en romkode fra noen som er vert for et spill? Skriv den inn under - du trenger ingen konto, bare et navn slik at de andre spillerne vet hvem du er.
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field">
      <label>Romkode
      <input
        type="text"
        v-model="code"
        placeholder="f.eks. ABCDE"
        style="text-transform:uppercase; letter-spacing:0.1em; font-size:1.2rem; text-align:center;"
        maxlength="5"
        @keyup.enter="join"
      /></label>
    </div>

    <div v-if="needsName" class="field">
      <label>Navnet ditt <span class="picker-hint">vises for andre spillere</span><input type="text" v-model="displayName" placeholder="Navnet ditt" @keyup.enter="join" /></label>
    </div>

    <button class="btn btn-primary" style="width:100%;" :disabled="!canSubmit || joining" @click="join">
      {{ joining ? 'Blir med…' : 'Bli med' }}
    </button>

    <p v-if="needsName" style="margin-top:20px; text-align:center;">
      <router-link :to="{ path: '/', query: { redirect: signInRedirect } }">Har du en konto? Logg inn i stedet</router-link>
    </p>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'

const route = useRoute()
const router = useRouter()

const code = ref((route.params.code || '').toUpperCase())
const displayName = ref('')
const error = ref('')
const joining = ref(false)

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

onMounted(() => {
  if (route.params.code) {
    // Coming from a shared link (e.g. /join/ABCDE) - jump straight to typing
    // a name (or submitting, if already signed in) instead of retyping the code.
    code.value = String(route.params.code).toUpperCase()
  }
})

async function join() {
  if (!canSubmit.value) return
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
      error.value = "Denne romtypen støttes ikke her."
      return
    }
    router.push({ path: target, query: { code: roomCode } })
  } catch (e) {
    error.value = e.response?.data?.message || 'Fant ikke rommet - sjekk koden og prøv igjen.'
  } finally {
    joining.value = false
  }
}
</script>
