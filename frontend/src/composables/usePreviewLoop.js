import { onBeforeUnmount, onMounted } from 'vue'

// The game previews are little films of the real games. This runs one: `play(sleep)` is a script that sets state,
// sleeps, sets more state, and is repeated forever. It
//  - stops cleanly when the component goes away,
//  - pauses while the preview is scrolled out of view (so it costs nothing offscreen),
//  - and with reduced motion never plays at all: `finalFrame()` sets the finished picture once.
const STOP = Symbol('stop')

export function usePreviewLoop(elRef, { play, finalFrame }) {
  let alive = true
  let visible = true
  let waiters = []
  let observer = null

  async function sleep(ms) {
    await new Promise(resolve => setTimeout(resolve, ms))
    while (alive && !visible) await new Promise(resolve => waiters.push(resolve))
    if (!alive) throw STOP
  }

  async function run() {
    try {
      while (alive) await play(sleep)
    } catch (e) {
      if (e !== STOP) throw e
    }
  }

  onMounted(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) { finalFrame(); return }
    if (typeof IntersectionObserver !== 'undefined' && elRef.value) {
      observer = new IntersectionObserver(([entry]) => {
        visible = entry.isIntersecting
        if (visible) waiters.splice(0).forEach(resolve => resolve())
      })
      observer.observe(elRef.value)
    }
    run()
  })

  onBeforeUnmount(() => {
    alive = false
    waiters.splice(0).forEach(resolve => resolve())
    observer?.disconnect()
  })
}

// Counts a ref from its current value to `to` (ease out) - for scores and years that tick up or down.
export function tweenRef(target, to, ms = 700) {
  const from = target.value
  const start = performance.now()
  return new Promise(resolve => {
    const tick = (now) => {
      const k = Math.min(1, (now - start) / ms)
      target.value = Math.round(from + (to - from) * (1 - Math.pow(1 - k, 3)))
      if (k < 1) requestAnimationFrame(tick)
      else resolve()
    }
    requestAnimationFrame(tick)
  })
}
