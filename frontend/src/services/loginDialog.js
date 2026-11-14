import { reactive } from 'vue'

// The sign-in popup (components/LoginDialog.vue, mounted once in App.vue). Anything can open it: the "Logg inn" button
// in the header, the landing page's links, a page that needs an account for its next step.
//   redirect - where to go once signed in (an in-app path); without it the dialog falls back to ?redirect= / the dashboard
//   reason   - one line shown under the heading, saying why an account is needed right here
//   tab      - 'signin' (default) or 'register'
const state = reactive({ open: false, redirect: null, reason: '', tab: 'signin' })

export default {
  state,
  open(options) {
    // also used directly as a click handler, so anything that isn't a plain options object is ignored
    const o = options && typeof options === 'object' && !(options instanceof Event) ? options : {}
    state.redirect = typeof o.redirect === 'string' ? o.redirect : null
    state.reason = o.reason || ''
    state.tab = o.tab === 'register' ? 'register' : 'signin'
    state.open = true
  },
  close() { state.open = false }
}
