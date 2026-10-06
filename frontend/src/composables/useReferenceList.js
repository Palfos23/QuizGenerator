import { onUnmounted, ref } from 'vue'
import toast from '../services/toast'
import { reportClientEvent } from '../services/diagnostics'

// Gaps between automatic retries after a failed load - a cold-starting backend or a
// brief network drop is usually over within these.
const RETRY_DELAYS_MS = [2000, 5000, 10000]

/**
 * Loads a list a game needs before a player can answer (the Tension suggestion list, 501's
 * entry list...) and tracks how that's going, so the UI can say so instead of showing nothing:
 *
 *   status: 'idle' | 'loading' | 'ready' | 'empty' | 'error'
 *
 * Deliberately independent of the game-state updates that trigger it: those arrive many times
 * (every poll and push), may overlap, and may be discarded as stale - none of which may drop
 * or skip this load. (A version of 501's loader that was tied to them could lose the list for
 * the whole game.)
 *
 * - `load(sourceKey, fetcher)` is idempotent for the same sourceKey - safe to call on every
 *   update. A different sourceKey starts a fresh load and ignores any older one still in flight.
 * - A failed load retries on its own a few times and can be retried by hand (`retry()`).
 * - Every failure and every empty result is reported to the server log (reportClientEvent).
 */
export function useReferenceList({ area, failureToast }) {
  const items = ref([])
  const status = ref('idle')
  let wantedKey = null
  let fetcher = null
  let attempt = 0
  let retryTimer = null
  let toastShown = false

  function reset() {
    clearTimeout(retryTimer)
    wantedKey = null
    fetcher = null
    items.value = []
    status.value = 'idle'
  }

  async function load(sourceKey, fetchFn) {
    if (!sourceKey) {
      reset()
      return
    }
    if (sourceKey === wantedKey && status.value !== 'idle') {
      return // already loaded, loading, or failed and waiting on its own retry/the Try again button
    }
    wantedKey = sourceKey
    fetcher = fetchFn
    attempt = 0
    await fetchOnce(sourceKey)
  }

  async function fetchOnce(sourceKey) {
    clearTimeout(retryTimer)
    status.value = 'loading'
    try {
      const list = await fetcher()
      if (wantedKey !== sourceKey) return // a different list was asked for meanwhile
      items.value = list
      if (list.length) {
        status.value = 'ready'
      } else {
        status.value = 'empty'
        reportClientEvent({ area, kind: 'EMPTY_LIST', key: sourceKey })
      }
    } catch (e) {
      if (wantedKey !== sourceKey) return
      status.value = 'error'
      reportClientEvent({
        area,
        kind: 'FETCH_FAILED',
        key: sourceKey,
        httpStatus: e.response?.status,
        detail: e.response?.data?.message || e.message
      })
      if (failureToast && !toastShown) {
        toastShown = true
        toast.show(failureToast, 'error')
      }
      if (attempt < RETRY_DELAYS_MS.length) {
        const delay = RETRY_DELAYS_MS[attempt++]
        retryTimer = setTimeout(() => { if (wantedKey === sourceKey) fetchOnce(sourceKey) }, delay)
      }
    }
  }

  // Manual "try again" from the UI - starts the whole retry sequence over.
  function retry() {
    if (!wantedKey || !fetcher) return
    attempt = 0
    fetchOnce(wantedKey)
  }

  onUnmounted(() => clearTimeout(retryTimer))

  return { items, status, load, retry, reset }
}
