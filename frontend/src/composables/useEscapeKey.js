import { onMounted, onUnmounted } from 'vue'

// Calls handler() whenever Escape is pressed while the calling component is
// mounted - used by modals so a keyboard user can close one the same way a
// mouse user does by clicking the backdrop.
export function useEscapeKey(handler) {
  function onKeydown(e) {
    if (e.key === 'Escape') handler(e)
  }
  onMounted(() => document.addEventListener('keydown', onKeydown))
  onUnmounted(() => document.removeEventListener('keydown', onKeydown))
}
