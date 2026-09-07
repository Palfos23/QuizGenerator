<template>
  <div style="max-width:420px; margin:40px auto;">
    <h1>Join a game</h1>
    <p class="page-subtitle">
      Got a room code from someone hosting a game? Enter it below - no account needed,
      just a name so the other players know who you are.
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field">
      <label>Room code</label>
      <input
        type="text"
        v-model="code"
        placeholder="e.g. ABCDE"
        style="text-transform:uppercase; letter-spacing:0.1em; font-size:1.2rem; text-align:center;"
        maxlength="5"
        @keyup.enter="join"
      />
    </div>

    <div v-if="needsName" class="field">
      <label>Your name <span class="picker-hint">shown to other players</span></label>
      <input type="text" v-model="displayName" placeholder="Your name" @keyup.enter="join" />
    </div>

    <button class="btn btn-primary" style="width:100%;" :disabled="!canSubmit || joining" @click="join">
      {{ joining ? 'Joining…' : 'Join' }}
    </button>

    <p style="margin-top:20px; text-align:center;">
      <router-link to="/">Have an account? Sign in instead</router-link>
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
      error.value = "This room's game type isn't supported here."
      return
    }
    router.push({ path: target, query: { code: roomCode } })
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not find that room - check the code and try again.'
  } finally {
    joining.value = false
  }
}
</script>
