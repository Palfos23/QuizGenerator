const STORAGE_KEY = 'activeOnlineRoom'
const MAX_AGE_MS = 24 * 60 * 60 * 1000 // matches the backend's own 24h retention window for in-progress rooms

export default {
  save(code, gameType) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ code, gameType, savedAt: Date.now() }))
    } catch (e) {
      // localStorage can fail (private browsing, quota) - rejoin is a convenience, not essential
    }
  },
  clear() {
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch (e) {
      // nothing to do if this fails
    }
  },
  get(gameType) {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) return null
      const parsed = JSON.parse(raw)
      if (parsed.gameType !== gameType) return null
      if (Date.now() - parsed.savedAt > MAX_AGE_MS) return null
      return parsed.code
    } catch (e) {
      return null
    }
  }
}
