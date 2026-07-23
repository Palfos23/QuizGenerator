import { createRouter, createWebHashHistory } from 'vue-router'
import QuizGeneratorView from '../views/QuizGeneratorView.vue'
import AdminView from '../views/AdminView.vue'
import HomeView from '../views/HomeView.vue'
import AdminLoginView from '../views/AdminLoginView.vue'
import MyQuizzesView from '../views/MyQuizzesView.vue'
import WeeklyGridListView from '../views/WeeklyGridListView.vue'
import WeeklyGridPlayView from '../views/WeeklyGridPlayView.vue'
import AdminAthletesView from '../views/AdminAthletesView.vue'
import AdminGridsView from '../views/AdminGridsView.vue'
import AdminClubsView from '../views/AdminClubsView.vue'
import TensionView from '../views/TensionView.vue'
import AdminTensionQuestionsView from '../views/AdminTensionQuestionsView.vue'
import AdminTensionCategoriesView from '../views/AdminTensionCategoriesView.vue'
import SuggestQuestionView from '../views/SuggestQuestionView.vue'
import AdminSubmissionsView from '../views/AdminSubmissionsView.vue'
import AdminQuizTemplatesView from '../views/AdminQuizTemplatesView.vue'
import auth from '../services/auth'

const routes = [
  // Public landing page: explains the app and hosts the Google sign-in button.
  { path: '/', name: 'home', component: HomeView },

  { path: '/generate', name: 'generate', component: QuizGeneratorView, meta: { requiresAuth: true } },
  { path: '/my-quizzes', name: 'my-quizzes', component: MyQuizzesView, meta: { requiresAuth: true } },
  { path: '/weekly-grid', name: 'weekly-grid', component: WeeklyGridListView, meta: { requiresAuth: true } },
  { path: '/weekly-grid/:id', name: 'weekly-grid-play', component: WeeklyGridPlayView, meta: { requiresAuth: true } },

  // The admin's own front door - a dedicated login landing page, separate from the
  // regular Google sign-in. Note this URL being "findable" isn't the security boundary:
  // the backend still requires a valid ADMIN-role JWT on every /api/admin/** call regardless
  // of how someone got to this page.
  { path: '/admin', name: 'admin-login', component: AdminLoginView },
  { path: '/admin/questions', name: 'admin', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/athletes', name: 'admin-athletes', component: AdminAthletesView, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/grids', name: 'admin-grids', component: AdminGridsView, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/clubs', name: 'admin-clubs', component: AdminClubsView, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/tension', name: 'tension', component: TensionView, meta: { requiresAuth: true } },
  { path: '/admin/tension-questions', name: 'admin-tension-questions', component: AdminTensionQuestionsView, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/tension-categories', name: 'admin-tension-categories', component: AdminTensionCategoriesView, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/suggest-question', name: 'suggest-question', component: SuggestQuestionView, meta: { requiresAuth: true } },
  { path: '/admin/question-submissions', name: 'admin-question-submissions', component: AdminSubmissionsView, meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/quiz-templates', name: 'admin-quiz-templates', component: AdminQuizTemplatesView, meta: { requiresAuth: true, requiresAdmin: true } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.requiresAdmin && !auth.isAdmin.value) {
    return { name: 'admin-login' }
  }
  if (to.meta.requiresAuth && !auth.isAuthenticated.value) {
    return { name: 'home' }
  }
  return true
})

export default router
