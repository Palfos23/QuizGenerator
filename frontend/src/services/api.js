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
    if (error.response?.status === 401 && auth.state.token) {
      auth.logout()
      router.push('/?sessionExpired=1')
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
  addQuestions(payload) {
    return client.post('/quiz/add-questions', payload).then(r => r.data)
  },
  searchQuestions(language, search, category) {
    const query = new URLSearchParams({ language })
    if (search) query.set('search', search)
    if (category) query.set('category', category)
    return client.get(`/quiz/search-questions?${query.toString()}`).then(r => r.data)
  },
  getCategories(language) {
    return client.get(`/quiz/categories?language=${language}`).then(r => r.data)
  },
  exportPdf(quiz, includeAnswers) {
    return client.post(`/quiz/export/pdf?includeAnswers=${includeAnswers}`, quiz, {
      responseType: 'blob'
    }).then(r => r.data)
  },

  // --- My Quizzes (explicitly saved, not auto-saved on generate) ---
  saveQuiz(quiz) {
    return client.post('/quiz/saved', quiz).then(r => r.data)
  },
  listSavedQuizzes() {
    return client.get('/quiz/saved').then(r => r.data)
  },
  getSavedQuiz(id) {
    return client.get(`/quiz/saved/${id}`).then(r => r.data)
  },
  updateSavedQuiz(id, quiz) {
    return client.put(`/quiz/saved/${id}`, quiz).then(r => r.data)
  },
  deleteSavedQuiz(id) {
    return client.delete(`/quiz/saved/${id}`)
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
  },
  adminImportCsv(file) {
    const formData = new FormData()
    formData.append('file', file)
    return client.post('/admin/questions/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(r => r.data)
  },
  adminGetStats() {
    return client.get('/admin/questions/stats').then(r => r.data)
  },
  adminAddStarterPack() {
    return client.post('/admin/questions/starter-pack').then(r => r.data)
  },

  // --- Weekly grids: user-facing ---
  getActiveGrids() {
    return client.get('/grids/active').then(r => r.data)
  },
  getArchiveGrids() {
    return client.get('/grids/archive').then(r => r.data)
  },
  getFutureGrids() {
    return client.get('/grids/future').then(r => r.data)
  },
  getBattleEligibleGrids() {
    return client.get('/grids/battle-eligible').then(r => r.data)
  },
  getGridPlayState(id) {
    return client.get(`/grids/${id}/play`).then(r => r.data)
  },
  searchGridCandidates(id, search) {
    return client.get(`/grids/${id}/candidates?search=${encodeURIComponent(search || '')}`).then(r => r.data)
  },
  getMultiplayerGridStart(id) {
    return client.get(`/grids/${id}/multiplayer-start`).then(r => r.data)
  },
  submitMultiplayerGridGuess(id, athleteId, revealedEntryIds) {
    return client.post(`/grids/${id}/multiplayer-guess`, { athleteId, revealedEntryIds }).then(r => r.data)
  },
  submitGridGuess(id, athleteId) {
    return client.post(`/grids/${id}/guess`, { athleteId }).then(r => r.data)
  },
  enterGridOvertime(id) {
    return client.post(`/grids/${id}/overtime`).then(r => r.data)
  },
  revealGrid(id) {
    return client.post(`/grids/${id}/reveal`).then(r => r.data)
  },
  getGridScoreboard(id) {
    return client.get(`/grids/${id}/scoreboard`).then(r => r.data)
  },
  setGridLeaderboardPreference(id, include) {
    return client.put(`/grids/${id}/leaderboard-preference?include=${include}`)
  },
  revealAllGridEntries(id) {
    return client.get(`/grids/${id}/reveal-all`).then(r => r.data)
  },

  // --- Weekly grids: admin ---
  adminListGrids() {
    return client.get('/admin/grids').then(r => r.data)
  },
  adminGetGrid(id) {
    return client.get(`/admin/grids/${id}`).then(r => r.data)
  },
  adminCreateGrid(payload) {
    return client.post('/admin/grids', payload).then(r => r.data)
  },
  adminUpdateGrid(id, payload) {
    return client.put(`/admin/grids/${id}`, payload).then(r => r.data)
  },
  adminDeleteGrid(id) {
    return client.delete(`/admin/grids/${id}`)
  },

  // --- Athlete roster: admin ---
  adminSearchAthletes(params = {}) {
    const query = new URLSearchParams()
    if (params.sport) query.set('sport', params.sport)
    if (params.team) query.set('team', params.team)
    if (params.name) query.set('name', params.name)
    return client.get(`/admin/athletes?${query.toString()}`).then(r => r.data)
  },
  adminCreateAthlete(payload) {
    return client.post('/admin/athletes', payload).then(r => r.data)
  },
  adminUpdateAthlete(id, payload) {
    return client.put(`/admin/athletes/${id}`, payload).then(r => r.data)
  },
  adminDeleteAthlete(id) {
    return client.delete(`/admin/athletes/${id}`)
  },

  // --- Clubs: admin ---
  adminSearchClubs(sport, name) {
    const query = new URLSearchParams()
    if (sport) query.set('sport', sport)
    if (name) query.set('name', name)
    return client.get(`/admin/clubs?${query.toString()}`).then(r => r.data)
  },
  adminCreateClub(payload) {
    return client.post('/admin/clubs', payload).then(r => r.data)
  },
  adminUpdateClub(id, payload) {
    return client.put(`/admin/clubs/${id}`, payload).then(r => r.data)
  },
  adminDeleteClub(id) {
    return client.delete(`/admin/clubs/${id}`)
  },

  // --- Tension: gameplay ---
  fetchTensionQuestions(count, category) {
    const query = new URLSearchParams({ count: String(count) })
    if (category) query.set('category', category)
    return client.get(`/tension/questions/random?${query.toString()}`).then(r => r.data)
  },
  fetchTensionRoundChoices(count, category, excludeIds) {
    const query = new URLSearchParams({ count: String(count) })
    if (category) query.set('category', category)
    excludeIds.forEach(id => query.append('excludeIds', String(id)))
    return client.get(`/tension/questions/round-choices?${query.toString()}`).then(r => r.data)
  },
  fetchTensionAnswerOptions(categoryName) {
    if (!categoryName) return Promise.resolve([])
    return client.get(`/tension/categories/${encodeURIComponent(categoryName)}/options`).then(r => r.data)
  },
  fetchTensionMainCategories() {
    return client.get('/tension/questions/categories').then(r => r.data)
  },

  // --- Tension: admin questions ---
  adminListTensionQuestions() {
    return client.get('/admin/tension/questions').then(r => r.data)
  },
  adminGetTensionQuestion(id) {
    return client.get(`/admin/tension/questions/${id}`).then(r => r.data)
  },
  adminCreateTensionQuestion(payload) {
    return client.post('/admin/tension/questions', payload).then(r => r.data)
  },
  adminUpdateTensionQuestion(id, payload) {
    return client.put(`/admin/tension/questions/${id}`, payload).then(r => r.data)
  },
  adminDeleteTensionQuestion(id) {
    return client.delete(`/admin/tension/questions/${id}`)
  },

  // --- Tension: admin categories ---
  adminListTensionCategories() {
    return client.get('/admin/tension/categories').then(r => r.data)
  },
  adminGetTensionCategory(id) {
    return client.get(`/admin/tension/categories/${id}`).then(r => r.data)
  },
  adminCreateTensionCategory(payload) {
    return client.post('/admin/tension/categories', payload).then(r => r.data)
  },
  adminUpdateTensionCategory(id, payload) {
    return client.put(`/admin/tension/categories/${id}`, payload).then(r => r.data)
  },
  adminDeleteTensionCategory(id) {
    return client.delete(`/admin/tension/categories/${id}`)
  },

  // --- Question submissions: user-facing ---
  submitQuestion(payload) {
    return client.post('/questions/submissions', payload).then(r => r.data)
  },
  myQuestionSubmissions() {
    return client.get('/questions/submissions/mine').then(r => r.data)
  },

  // --- Question submissions: admin review ---
  adminListSubmissions() {
    return client.get('/admin/question-submissions').then(r => r.data)
  },
  adminApproveSubmission(id) {
    return client.post(`/admin/question-submissions/${id}/approve`).then(r => r.data)
  },
  adminRejectSubmission(id, reason) {
    return client.post(`/admin/question-submissions/${id}/reject`, { reason }).then(r => r.data)
  },

  // --- Quiz templates: user-facing ---
  listQuizTemplates() {
    return client.get('/quiz-templates').then(r => r.data)
  },
  getQuizTemplate(id) {
    return client.get(`/quiz-templates/${id}`).then(r => r.data)
  },
  copyQuizTemplate(id) {
    return client.post(`/quiz-templates/${id}/copy`).then(r => r.data)
  },

  // --- Quiz templates: admin ---
  adminListQuizTemplates() {
    return client.get('/admin/quiz-templates').then(r => r.data)
  },
  adminGetQuizTemplate(id) {
    return client.get(`/admin/quiz-templates/${id}`).then(r => r.data)
  },
  adminCreateQuizTemplate(payload) {
    return client.post('/admin/quiz-templates', payload).then(r => r.data)
  },
  adminUpdateQuizTemplate(id, payload) {
    return client.put(`/admin/quiz-templates/${id}`, payload).then(r => r.data)
  },
  adminDeleteQuizTemplate(id) {
    return client.delete(`/admin/quiz-templates/${id}`)
  },

  // --- Reports: user-facing ---
  submitReport(payload) {
    return client.post('/reports', payload).then(r => r.data)
  },
  myReports() {
    return client.get('/reports/mine').then(r => r.data)
  },

  // --- Reports: admin ---
  adminListReports() {
    return client.get('/admin/reports').then(r => r.data)
  },
  adminResolveReport(id, adminNote) {
    return client.post(`/admin/reports/${id}/resolve`, { adminNote }).then(r => r.data)
  },

  // --- Online multiplayer rooms ---
  createRoom(payload) {
    return client.post('/rooms', payload).then(r => r.data)
  },
  joinRoom(code, payload) {
    return client.post(`/rooms/${code}/join`, payload).then(r => r.data)
  },
  getRoom(code) {
    return client.get(`/rooms/${code}`).then(r => r.data)
  },
  startRoom(code) {
    return client.post(`/rooms/${code}/start`).then(r => r.data)
  },

  // --- Online Grid Battle ---
  getGridBattleState(code) {
    return client.get(`/rooms/${code}/grid-battle/state`).then(r => r.data)
  },
  submitGridBattleGuess(code, athleteId) {
    return client.post(`/rooms/${code}/grid-battle/guess`, { athleteId }).then(r => r.data)
  },
  advanceGridBattleGrid(code) {
    return client.post(`/rooms/${code}/grid-battle/next-grid`).then(r => r.data)
  },

  // --- Online Tension ---
  getTensionOnlineState(code) {
    return client.get(`/rooms/${code}/tension/state`).then(r => r.data)
  },
  submitTensionOnlineAnswer(code, answerText) {
    return client.post(`/rooms/${code}/tension/answer`, { answerText }).then(r => r.data)
  },
  advanceTensionOnlineQuestion(code) {
    return client.post(`/rooms/${code}/tension/next-question`).then(r => r.data)
  },

  // --- 501: user-facing ---
  listFiveOhOneCategories() {
    return client.get('/501/categories').then(r => r.data)
  },
  getFiveOhOneCategory(id) {
    return client.get(`/501/categories/${id}`).then(r => r.data)
  },

  // --- 501: admin ---
  adminListFiveOhOneCategories() {
    return client.get('/admin/501/categories').then(r => r.data)
  },
  adminGetFiveOhOneCategory(id) {
    return client.get(`/admin/501/categories/${id}`).then(r => r.data)
  },
  adminCreateFiveOhOneCategory(payload) {
    return client.post('/admin/501/categories', payload).then(r => r.data)
  },
  adminUpdateFiveOhOneCategory(id, payload) {
    return client.put(`/admin/501/categories/${id}`, payload).then(r => r.data)
  },
  adminDeleteFiveOhOneCategory(id) {
    return client.delete(`/admin/501/categories/${id}`)
  }
}
