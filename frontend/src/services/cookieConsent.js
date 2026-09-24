import { reactive, computed } from 'vue'

// This app sets no cookies of its own (auth is JWT-in-localStorage), but the
// Google Sign-In script embedded on the home page can set cookies on
// Google's own domain the moment it loads - see HomeView.vue, which only
// loads that script once status is 'accepted'. Same reactive-singleton shape
// as services/auth.js/services/toast.js.
const STORAGE_KEY = 'cookie_consent'

const state = reactive({
  status: localStorage.getItem(STORAGE_KEY) || 'unknown' // 'unknown' | 'accepted' | 'declined'
})

function accept() {
  state.status = 'accepted'
  localStorage.setItem(STORAGE_KEY, 'accepted')
}

function decline() {
  state.status = 'declined'
  localStorage.setItem(STORAGE_KEY, 'declined')
}

// Re-shows the banner - used by the "Cookie settings" footer link so the
// choice can be changed later without clearing anything else.
function reset() {
  state.status = 'unknown'
  localStorage.removeItem(STORAGE_KEY)
}

export default {
  state,
  isUnknown: computed(() => state.status === 'unknown'),
  accept,
  decline,
  reset
}
