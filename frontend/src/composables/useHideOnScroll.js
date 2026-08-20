import { onMounted, onUnmounted, ref } from 'vue'

// Hides a sticky element while the page is scrolling down, reveals it again
// on the very next upward scroll (or once back near the top) - same pattern
// as a phone browser's toolbar. Without this, a sticky search box stays
// pinned on screen for the entire time someone's scrolling down to look at a
// tall board/pitch, permanently eating into their view of it.
export function useHideOnScroll(threshold = 8) {
  const hidden = ref(false)
  let lastY = window.scrollY

  function onScroll() {
    const y = window.scrollY
    const delta = y - lastY
    if (y < 80) {
      hidden.value = false
    } else if (delta > threshold) {
      hidden.value = true
    } else if (delta < -threshold) {
      hidden.value = false
    }
    lastY = y
  }

  onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
  onUnmounted(() => window.removeEventListener('scroll', onScroll))

  return { hidden }
}
