import { reactive, computed } from 'vue'

// The PIN for the host's side of the one-off birthday quiz (/bday/admin) - guests never need one. A single shared
// PIN, stored per-browser so entering it once also unlocks the host's other tabs. Same reactive-singleton shape as
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
