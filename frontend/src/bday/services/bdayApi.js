import axios from 'axios'
import bdayAuth from '../composables/useBdayPin'

// A dedicated, minimal client for the one-off birthday quiz - deliberately not services/api.js, which carries the
// real site's JWT and its redirect-to-home on a 401. The guests' calls (name list, quiz, submit, leaderboard) are
// open and need nothing. The host's calls (/bday/admin/**) carry the quiz's PIN in X-Bday-Pin; a PIN the server turns
// away is forgotten, which makes BdayPinGate ask again.
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
})

client.interceptors.request.use(config => {
  if (bdayAuth.state.pin) {
    config.headers['X-Bday-Pin'] = bdayAuth.state.pin
  }
  return config
})

client.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401 && String(error.config?.url || '').startsWith('/bday/admin')) {
      bdayAuth.clearPin()
    }
    return Promise.reject(error)
  }
)

export default {
  // --- Guests ---
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
  },

  // --- The host (PIN) ---
  // No dedicated "check PIN" endpoint - the question list is harmless to call just to confirm a freshly typed PIN works.
  checkPin() {
    return client.get('/bday/admin/questions').then(() => true)
  },
  adminGuests() { return client.get('/bday/admin/guests').then(r => r.data) },
  adminAddGuest(name) { return client.post('/bday/admin/guests', { name }).then(r => r.data) },
  adminDeleteGuest(id) { return client.delete(`/bday/admin/guests/${id}`) },
  adminSetScore(id, score) { return client.put(`/bday/admin/guests/${id}/score`, { score }).then(r => r.data) },
  adminResetGuest(id) { return client.post(`/bday/admin/guests/${id}/reset`).then(r => r.data) },
  adminGuestAnswers(id) { return client.get(`/bday/admin/guests/${id}/answers`).then(r => r.data) },
  adminQuestions() { return client.get('/bday/admin/questions').then(r => r.data) },
  adminCreateQuestion(payload) { return client.post('/bday/admin/questions', payload).then(r => r.data) },
  adminUpdateQuestion(id, payload) { return client.put(`/bday/admin/questions/${id}`, payload).then(r => r.data) },
  adminDeleteQuestion(id) { return client.delete(`/bday/admin/questions/${id}`) },
  adminUploadImage(file) {
    const formData = new FormData()
    formData.append('file', file)
    return client.post('/bday/admin/images', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then(r => r.data)
  }
}
