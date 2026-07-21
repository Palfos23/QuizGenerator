import axios from 'axios'
import auth from './auth'
import router from '../router'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
})

client.interceptors.request.use(config => {
  if (auth.state.token) {
    config.headers.Authorization = `Bearer ${auth.state.token}`
  }
  return config
})

client.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      auth.logout()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default {
  // --- Auth ---
  loginWithGoogle(idToken) {
    return client.post('/auth/google', { idToken }).then(r => r.data)
  },
  loginAsAdmin(username, password) {
    return client.post('/auth/admin/login', { username, password }).then(r => r.data)
  },

  // --- Public quiz endpoints (require a logged-in user) ---
  generateQuiz(payload) {
    return client.post('/quiz/generate', payload).then(r => r.data)
  },
  replaceQuestion(payload) {
    return client.post('/quiz/replace-question', payload).then(r => r.data)
  },
  getCategories(language) {
    return client.get(`/quiz/categories?language=${language}`).then(r => r.data)
  },
  exportPdf(quiz, includeAnswers) {
    return client.post(`/quiz/export/pdf?includeAnswers=${includeAnswers}`, quiz, {
      responseType: 'blob'
    }).then(r => r.data)
  },
  emailQuiz(payload) {
    return client.post('/quiz/export/email', payload).then(r => r.data)
  },

  // --- Admin endpoints (require ADMIN role) ---
  adminListQuestions() {
    return client.get('/admin/questions').then(r => r.data)
  },
  adminGetQuestion(id) {
    return client.get(`/admin/questions/${id}`).then(r => r.data)
  },
  adminCreateQuestion(payload) {
    return client.post('/admin/questions', payload).then(r => r.data)
  },
  adminUpdateQuestion(id, payload) {
    return client.put(`/admin/questions/${id}`, payload).then(r => r.data)
  },
  adminDeleteQuestion(id) {
    return client.delete(`/admin/questions/${id}`)
  }
}
