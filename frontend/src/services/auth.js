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
  logout,
  isAuthenticated,
  isAdmin,
  isTokenExpired
}
