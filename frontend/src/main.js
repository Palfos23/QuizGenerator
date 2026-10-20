import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { loadLang, t, locale } from './i18n'
import './style.css'
import './styles/daily-quiz.css'
import './styles/paper.css'

// The English catalog (if that's the chosen language) has to be in before anything renders.
loadLang().then(() => {
  const app = createApp(App).use(router)
  app.config.globalProperties.$t = t
  app.config.globalProperties.$locale = locale
  app.mount('#app')
})
