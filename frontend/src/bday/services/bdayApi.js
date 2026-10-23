import axios from 'axios'

// The guests' side of the birthday quiz: open to everyone, so no login token and no PIN - a deliberately plain client,
// separate from services/api.js (which carries the real site's JWT and its redirect-to-home on a 401). The host's
// side (players, questions, scores) goes through api.js with the admin login instead.
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
})

export default {
  /** The names still waiting to play. */
  listGuests() {
    return client.get('/bday/guests').then(r => r.data)
  },
  getQuiz(guestId) {
    return client.get(`/bday/quiz/${guestId}`).then(r => r.data)
  },
  submitQuiz(guestId, payload) {
    return client.post(`/bday/quiz/${guestId}/submit`, payload).then(r => r.data)
  },
  leaderboard() {
    return client.get('/bday/leaderboard').then(r => r.data)
  }
}
