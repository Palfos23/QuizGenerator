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
  isAdmin
}
