// Page changes and palette changes run through the View Transitions API: the browser keeps the old page on screen
// while the new one fades in over it, so the two states overlap instead of one vanishing before the other appears.
// Browsers without the API (and people who asked for reduced motion) just get the instant change.
import { nextTick } from 'vue'

const reduced = () => window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
const supported = () => typeof document.startViewTransition === 'function' && !reduced()

// Router: hold navigation until the old page is captured, then release it and finish once the new page has rendered.
export function installPageTransitions(router) {
  let finish = null
  router.beforeResolve((to, from) => {
    if (!supported() || !from.name || to.fullPath === from.fullPath) return
    return new Promise(resolve => {
      document.startViewTransition(() => new Promise(done => { finish = done; resolve() }))
    })
  })
  router.afterEach(() => {
    if (finish) { const done = finish; finish = null; nextTick(() => requestAnimationFrame(() => done())) }
  })
}

// Runs a synchronous DOM change as a circular reveal growing out of (x, y).
export function revealFrom(x, y, update) {
  if (!supported()) { update(); return }
  const root = document.documentElement
  root.classList.add('vt-reveal')
  const vt = document.startViewTransition(update)
  vt.ready.then(() => {
    const r = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y))
    root.animate(
      { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${r}px at ${x}px ${y}px)`] },
      { duration: 520, easing: 'cubic-bezier(0.65, 0, 0.35, 1)', pseudoElement: '::view-transition-new(root)' }
    )
  }).catch(() => {})
  vt.finished.finally(() => root.classList.remove('vt-reveal'))
}
