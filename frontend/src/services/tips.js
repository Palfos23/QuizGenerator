import { reactive } from 'vue'

// "Tips og triks" popups (components/TipsModal.vue). Each page's tips open by themselves ONCE - the first time the
// visitor reaches that page - and are never forced on them again; a small "Tips" button on the page reopens them on
// demand. Which pages have been seen is kept on this device (localStorage), and "Slå av tips" turns the automatic
// ones off altogether until the visitor turns them back on from the Account page.
const SEEN_PREFIX = 'quiz_tips_seen_'
const OFF_KEY = 'quiz_tips_off'

function read(key) {
  try { return localStorage.getItem(key) } catch (e) { return null }
}
function write(key, value) {
  try { localStorage.setItem(key, value) } catch (e) { /* private mode: the tips just show again next visit */ }
}

const state = reactive({ off: read(OFF_KEY) === '1' })

export default {
  state,
  /** True while this page's tips haven't been opened before and automatic tips haven't been switched off. */
  shouldAutoShow(id) {
    return !state.off && read(SEEN_PREFIX + id) !== '1'
  },
  markSeen(id) {
    write(SEEN_PREFIX + id, '1')
  },
  /** "Slå av tips": no more automatic popups (the Tips buttons keep working). */
  turnOff() {
    state.off = true
    write(OFF_KEY, '1')
  },
  /** Forgets every "seen" mark and switches automatic tips back on - so each page's tips open once more. */
  resetAll() {
    state.off = false
    try {
      const mine = []
      for (let i = 0; i < localStorage.length; i++) {
        const k = localStorage.key(i)
        if (k && (k.startsWith(SEEN_PREFIX) || k === OFF_KEY)) mine.push(k)
      }
      mine.forEach(k => localStorage.removeItem(k))
    } catch (e) { /* nothing to reset */ }
  }
}
