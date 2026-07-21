import { createRouter, createWebHashHistory } from 'vue-router'
import QuizGeneratorView from '../views/QuizGeneratorView.vue'
import AdminView from '../views/AdminView.vue'
import HomeView from '../views/HomeView.vue'
import AdminLoginView from '../views/AdminLoginView.vue'
import auth from '../services/auth'

const routes = [
  // Public landing page: explains the app and hosts the Google sign-in button.
  { path: '/', name: 'home', component: HomeView },

  { path: '/generate', name: 'generate', component: QuizGeneratorView, meta: { requiresAuth: true } },

  // The admin's own front door - a dedicated login landing page, separate from the
  // regular Google sign-in. Note this URL being "findable" isn't the security boundary:
  // the backend still requires a valid ADMIN-role JWT on every /api/admin/** call regardless
  // of how someone got to this page.
  { path: '/admin', name: 'admin-login', component: AdminLoginView },
  { path: '/admin/questions', name: 'admin', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } }
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
