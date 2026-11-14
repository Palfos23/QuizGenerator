// The palette change runs through the View Transitions API as a circular reveal. It is deliberately NOT used for page
// navigation: while a transition waits for the browser to capture the old page nothing paints, and routing must never
// wait on that. Browsers without the API (and people who asked for reduced motion) just get the instant change.

const reduced = () => window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
const supported = () => typeof document.startViewTransition === 'function' && !reduced()

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
