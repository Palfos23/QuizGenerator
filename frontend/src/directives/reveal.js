// v-reveal: an element fades up when it scrolls into view; line icons inside it ([data-draw], or the landing page's
// .home-icon) draw their strokes as it arrives. `v-reveal="120"` delays it by 120ms, for staggering a row of them.
// Needs the .reveal / .reveal.in rules (styles/motion.css). With reduced motion, or no IntersectionObserver, it does nothing.
export default {
  mounted(el, binding) {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || typeof IntersectionObserver === 'undefined') return
    const delay = binding.value || 0
    el.classList.add('reveal')
    el.style.setProperty('--d', `${delay}ms`)
    const shapes = [...el.querySelectorAll('[data-draw] path, [data-draw] rect, [data-draw] circle, .home-icon svg path, .home-icon svg rect, .home-icon svg circle')]
    shapes.forEach(shape => {
      try {
        const len = Math.ceil(shape.getTotalLength())
        shape.style.strokeDasharray = len
        shape.style.strokeDashoffset = len
      } catch (e) { /* cannot be measured: it just stays drawn */ }
    })
    const io = new IntersectionObserver(([entry]) => {
      if (!entry.isIntersecting) return
      io.disconnect()
      el.classList.add('in')
      shapes.forEach(shape => {
        shape.style.transition = `stroke-dashoffset 800ms var(--ease-in-out) ${delay + 200}ms`
        shape.style.strokeDashoffset = 0
      })
    }, { threshold: 0.2 })
    io.observe(el)
  }
}
