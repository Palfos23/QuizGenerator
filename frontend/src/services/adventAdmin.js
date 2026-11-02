import { reactive } from 'vue'

// The password for the advent calendar's game master page (asked for on top of the admin login). Kept for the
// browser tab only (sessionStorage), so it's asked for again in a new session - the page shows the hidden answers.
const KEY = 'advent_admin_password'

function read() {
  try { return sessionStorage.getItem(KEY) || '' } catch (e) { return '' }
}

const state = reactive({ password: read() })

function setPassword(value) {
  state.password = value
  try { sessionStorage.setItem(KEY, value) } catch (e) { /* private mode - it just won't survive a reload */ }
}

function clear() {
  state.password = ''
  try { sessionStorage.removeItem(KEY) } catch (e) { /* nothing to clear */ }
}

export default { state, setPassword, clear }
