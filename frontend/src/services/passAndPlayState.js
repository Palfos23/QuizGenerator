function key(gameType) {
  return `pass-and-play-${gameType}`
}

export default {
  save(gameType, data) {
    try {
      localStorage.setItem(key(gameType), JSON.stringify(data))
    } catch (e) {
      // localStorage can fail (private browsing, full quota) - losing the
      // ability to resume is a minor inconvenience, not worth surfacing an error
    }
  },
  load(gameType) {
    try {
      const raw = localStorage.getItem(key(gameType))
      return raw ? JSON.parse(raw) : null
    } catch (e) {
      return null
    }
  },
  clear(gameType) {
    try {
      localStorage.removeItem(key(gameType))
    } catch (e) {
      // ignore
    }
  }
}
