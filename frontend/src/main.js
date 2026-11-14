import { loadLang, t, locale } from './i18n'
import './style.css'
import './styles/daily-quiz.css'
import './styles/design.css'
import './styles/palettes.css'
import './styles/motion.css'
import { initPalette } from './services/palette'
import { installPageTransitions } from './services/viewTransition'

initPalette()


// The English catalog (if that's the chosen language) has to be loaded before any app module runs: some
// texts are evaluated the moment their module is imported (component prop defaults, menu constants), so the
// app itself is imported only afterwards.
loadLang().then(async () => {
  const [{ createApp }, { default: App }, { default: router }] = await Promise.all([
    import('vue'),
    import('./App.vue'),
    import('./router')
  ])
  installPageTransitions(router)
  const app = createApp(App).use(router)
  app.config.globalProperties.$t = t
  app.config.globalProperties.$locale = locale
  app.mount('#app')
})
