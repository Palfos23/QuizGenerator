import { createRouter, createWebHashHistory } from 'vue-router'
import QuizGeneratorView from '../views/QuizGeneratorView.vue'
import AdminView from '../views/AdminView.vue'
import LoginView from '../views/LoginView.vue'
import AdminLoginView from '../views/AdminLoginView.vue'
import auth from '../services/auth'

const routes = [
  { path: '/', redirect: '/generate' },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/generate', name: 'generate', component: QuizGeneratorView, meta: { requiresAuth: true } },

  // Deliberately not linked anywhere in the main nav - see App.vue. Real protection is the
  // ADMIN-role check the backend enforces on every /api/admin/** call, not this URL being obscure.
  { path: '/admin-access', name: 'admin-login', component: AdminLoginView },
  { path: '/admin', name: 'admin', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } }
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
    return { name: 'login' }
  }
  return true
})

export default router
