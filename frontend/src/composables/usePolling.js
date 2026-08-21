import { onMounted, onUnmounted } from 'vue'

// Runs fn() once immediately, then every intervalMs, until stop() is called or
// the component unmounts - the same setInterval/onMounted/onUnmounted
// scaffolding every Online*Game.vue component was reimplementing on its own
// to poll room state. stop() is exposed separately from unmount since a
// caller often needs to stop early - once the game finishes, or the player
// leaves the room - well before the component itself is torn down.
export function usePolling(fn, intervalMs) {
  let timer = null

  function stop() {
    clearInterval(timer)
    timer = null
  }

  onMounted(() => {
    fn()
    timer = setInterval(fn, intervalMs)
  })

  onUnmounted(stop)

  return { stop }
}
