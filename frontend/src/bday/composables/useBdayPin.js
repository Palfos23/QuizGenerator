import { reactive, computed } from 'vue'

// One-off birthday-quiz feature - a single shared PIN, stored per-browser
// (not per-tab) so entering it once on the host's PC also unlocks the
// leaderboard tab without re-prompting. Same reactive-singleton shape as
// services/auth.js, just far simpler (no roles, no token expiry).
const STORAGE_KEY = 'bday_pin'

const state = reactive({
  pin: localStorage.getItem(STORAGE_KEY) || ''
})

function setPin(value) {
  state.pin = value
  localStorage.setItem(STORAGE_KEY, value)
}

function clearPin() {
  state.pin = ''
  localStorage.removeItem(STORAGE_KEY)
}

export default {
  state,
  hasPin: computed(() => !!state.pin),
  setPin,
  clearPin
}
