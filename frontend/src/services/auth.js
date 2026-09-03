import { computed, reactive } from 'vue'

const state = reactive({
  token: localStorage.getItem('quiz_token') || null,
  displayName: localStorage.getItem('quiz_display_name') || null,
  role: localStorage.getItem('quiz_role') || null // 'USER' | 'ADMIN'
})

function login({ token, displayName, role }) {
  state.token = token
  state.displayName = displayName
  state.role = role
  localStorage.setItem('quiz_token', token)
  localStorage.setItem('quiz_display_name', displayName || '')
  localStorage.setItem('quiz_role', role)
}

// Reads the token's own expiry claim locally - no network call, so this works
// even before the device has reconnected after being asleep/backgrounded,
// which is exactly when a stale session is most likely to be sitting there
// unnoticed (mobile/installed-app tabs get suspended in the background for
// arbitrarily long stretches, well past the token's 12-hour expiry).
function isTokenExpired() {
  if (!state.token) return false
  try {
    const payload = JSON.parse(atob(state.token.split('.')[1]))
    if (!payload.exp) return false
    return Date.now() >= payload.exp * 1000
  } catch (e) {
    // Malformed token - treat as expired rather than trusting something unreadable
    return true
  }
}

// Milliseconds remaining until the token's own (absolute) expiry, or null if
// there's no token or no exp claim. Lets a caller warn as that hard cap
// approaches - unlike App.vue's separate inactivity timeout, this one can't
// be postponed by staying active, since there's no refresh token to extend it.
function msUntilTokenExpiry() {
  if (!state.token) return null
  try {
    const payload = JSON.parse(atob(state.token.split('.')[1]))
    if (!payload.exp) return null
    return payload.exp * 1000 - Date.now()
  } catch (e) {
    return 0 // malformed - treat as already expired, same as isTokenExpired()
  }
}

// Swaps in a freshly-issued token from /auth/refresh without the rest of a full
// login (no navigation, no "welcome back" side effects) - role/displayName are
// included in the refresh response too and reapplied here mostly for safety
// (they aren't expected to actually change mid-session), same fields login()
// already persists to localStorage.
function updateToken({ token, displayName, role }) {
  state.token = token
  state.displayName = displayName
  state.role = role
  localStorage.setItem('quiz_token', token)
  localStorage.setItem('quiz_display_name', displayName || '')
  localStorage.setItem('quiz_role', role)
}

function logout() {
  state.token = null
  state.displayName = null
  state.role = null
  localStorage.removeItem('quiz_token')
  localStorage.removeItem('quiz_display_name')
  localStorage.removeItem('quiz_role')
}

const isAuthenticated = computed(() => !!state.token)
const isAdmin = computed(() => state.role === 'ADMIN')

export default {
  state,
  login,
  updateToken,
  logout,
  isAuthenticated,
  isAdmin,
  isTokenExpired,
  msUntilTokenExpiry
}
