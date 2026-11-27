// A league invite (/liga/KODE) opened while signed out. A new player has to sign in - or create an account,
// confirm their email and come back - before they can join, and along the way they easily lose the link.
// The code is remembered in this browser for a while, and the first sign-in afterwards goes back to it.
const KEY = 'quiz_pending_league_invite'
const MAX_AGE_MS = 14 * 24 * 60 * 60 * 1000
const CODE = /^[A-Z0-9]{4,12}$/

function clean(code) {
  const c = String(code || '').trim().toUpperCase()
  return CODE.test(c) ? c : null
}

export function rememberInvite(code) {
  const c = clean(code)
  if (!c) return
  try { localStorage.setItem(KEY, JSON.stringify({ code: c, at: Date.now() })) } catch (e) { /* private mode - the dialog's own redirect still works */ }
}

export function peekInvite() {
  try {
    const saved = JSON.parse(localStorage.getItem(KEY) || 'null')
    if (!saved || Date.now() - saved.at > MAX_AGE_MS) return null
    return clean(saved.code)
  } catch (e) {
    return null
  }
}

export function forgetInvite() {
  try { localStorage.removeItem(KEY) } catch (e) { /* nothing to do */ }
}

/** The invite page to go to after signing in (and forget it), or null. */
export function takeInviteRoute() {
  const code = peekInvite()
  forgetInvite()
  return code ? `/liga/${code}` : null
}

export function inviteRouteFor(code) {
  const c = clean(code)
  return c ? `/liga/${c}` : null
}
