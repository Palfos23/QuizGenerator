import { loadLang, t, locale } from './i18n'
import { initTheme } from './services/theme'
import './style.css'
import './styles/daily-quiz.css'
import './styles/paper.css'

initTheme()

// The English catalog (if that's the chosen language) has to be loaded before any app module runs: some
// texts are evaluated the moment their module is imported (component prop defaults, menu constants), so the
// app itself is imported only afterwards.
loadLang().then(async () => {
  const [{ createApp }, { default: App }, { default: router }] = await Promise.all([
    import('vue'),
    import('./App.vue'),
    import('./router')
  ])
  const app = createApp(App).use(router)
  app.config.globalProperties.$t = t
  app.config.globalProperties.$locale = locale
  app.mount('#app')
})
