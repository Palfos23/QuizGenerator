import axios from 'axios'
import bdayAuth from '../composables/useBdayPin'

// A dedicated, minimal client for the one-off birthday-quiz feature -
// deliberately not reusing services/api.js, which carries the real site's
// JWT auth header and its own 401-redirect-to-home interceptor. This one
// just attaches the shared PIN and clears it on a 401 so BdayPinGate can
// re-prompt.
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
    if (error.response?.status === 401) {
      bdayAuth.clearPin()
    }
    return Promise.reject(error)
  }
)

export default {
  listGuests() {
    return client.get('/bday/guests').then(r => r.data)
  },
  addGuest(name) {
    return client.post('/bday/guests', { name }).then(r => r.data)
  },
  deleteGuest(id) {
    return client.delete(`/bday/guests/${id}`).then(r => r.data)
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
  // No dedicated "check PIN" endpoint - the guests list is harmless to call
  // just to confirm a freshly-typed PIN is actually accepted.
  checkPin() {
    return client.get('/bday/guests').then(() => true)
  }
}
