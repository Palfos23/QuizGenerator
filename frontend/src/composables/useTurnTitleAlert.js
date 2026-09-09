import { watch, onUnmounted } from 'vue'

const TURN_PREFIX = '(Your turn) '

/**
 * Flips the browser tab title to call out that it's this player's turn, so a
 * host tabbed away to another window (or another browser tab) can tell
 * without switching back and forth to check. Reverts the instant it's no
 * longer their turn, and restores the original title on unmount so leaving
 * the game screen never leaves a stale "(Your turn)" behind in the tab bar.
 *
 * `isYourTurn` is each caller's own existing `isYourTurn` computed - already
 * present in every online game component - passed straight through.
 */
export function useTurnTitleAlert(isYourTurn) {
  const original = document.title

  const stop = watch(isYourTurn, yours => {
    document.title = yours ? TURN_PREFIX + original : original
  }, { immediate: true })

  onUnmounted(() => {
    stop()
    document.title = original
  })
}
