import { reactive } from 'vue'

// The sign-in popup (components/LoginDialog.vue, mounted once in App.vue). Anything can open it: the "Logg inn" button
// in the header, the landing page's call to action, a link that needs an account.
const state = reactive({ open: false })

export default {
  state,
  open() { state.open = true },
  close() { state.open = false }
}
