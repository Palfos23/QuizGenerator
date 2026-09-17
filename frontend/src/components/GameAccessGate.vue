<template>
  <LoadingState v-if="state === 'checking'" full />

  <div v-else-if="state === 'denied'" class="empty-state" style="max-width:480px; margin:80px auto; padding:32px 28px; text-align:center;">
    <p style="margin:0; font-size:1.05rem;">{{ message }}</p>
  </div>

  <div v-else-if="state === 'guestRestricted'" class="empty-state" style="max-width:480px; margin:80px auto; padding:32px 28px; text-align:center;">
    <p style="margin:0 0 16px; font-size:1.05rem;">
      As a guest, you can only join a game using a room code from the host - not create one or browse other games.
    </p>
    <router-link to="/join" class="btn btn-primary">Join a game with a code →</router-link>
  </div>

  <slot v-else />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import auth from '../services/auth'
import activeRoom from '../services/activeRoom'
import LoadingState from './LoadingState.vue'

const props = defineProps({
  // Matches PlayAccessService.requireAccessForKey's keys - see api.js's route
  // list ('tension', 'grid-battle', '501', 'imposter', 'starting-xi-battle', 'bullseye').
  game: { type: String, required: true }
})

const route = useRoute()

// The RoomGameType string each game's own View.vue already uses for
// activeRoom.save()/.get() - see e.g. TensionView.vue's `activeRoom.save(...,
// 'TENSION')`. Keyed by the same `game` prop values used above, so no
// per-view changes are needed for the guest restriction below to work.
// Penalty Shootout has no online room mode at all, so it's deliberately
// absent - nothing here applies to it.
const ROOM_GAME_TYPE_BY_KEY = {
  tension: 'TENSION',
  'grid-battle': 'GRID_BATTLE',
  imposter: 'IMPOSTER',
  '501': 'FIVE_O_ONE',
  'starting-xi-battle': 'STARTING_XI_BATTLE',
  bullseye: 'BULLSEYE',
  flashback: 'FLASHBACK'
}

const state = ref('checking') // 'checking' | 'denied' | 'guestRestricted' | 'allowed'
const message = ref('')

// Runs before any of the wrapped game's own setup UI ever renders, so a
// restricted user only ever sees this message - never player-name inputs,
// mode choices, or a "Create game" button they'd just get rejected from later.
onMounted(async () => {
  // A guest has no AppUser row for /api/play-access to look up (it would just
  // deny them - see PlayAccessService#require), so this check doesn't apply to
  // them at all: a guest's only way in is a room code from a host who already
  // passed it.
  if (auth.isGuest.value) {
    // Only actually let a guest see this game's own page in two cases: they
    // just arrived via a room code (JoinGuestView.vue routes here with
    // ?code=...), or they're resuming a room they're already in (this exact
    // game's own activeRoom entry is set). Anything else - typing the URL
    // directly, a stale bookmark, poking around after finishing a game - gets
    // the message above instead of a "Create game" UI they'd only be
    // rejected from server-side after typing a name and clicking through.
    // Both checks are synchronous (no API round trip) specifically so this
    // can't race against this same page's own onMounted, which may be doing
    // the actual join (and its own activeRoom.save) at the same moment.
    const roomGameType = ROOM_GAME_TYPE_BY_KEY[props.game]
    const arrivingWithCode = !!route.query.code
    const hasActiveRoom = !!(roomGameType && activeRoom.get(roomGameType))
    state.value = (arrivingWithCode || hasActiveRoom) ? 'allowed' : 'guestRestricted'
    return
  }
  try {
    await api.checkGameAccess(props.game)
    state.value = 'allowed'
  } catch (e) {
    message.value = e.response?.data?.message || "You don't currently have access to this game - contact an administrator."
    state.value = 'denied'
  }
})
</script>
