import { computed, onUnmounted, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import { reportClientEvent } from '../services/diagnostics'

// Gaps between automatic retries after a failed load - a cold-starting backend or a
// brief network drop is usually over within these.
const RETRY_DELAYS_MS = [2000, 5000, 10000]

/**
 * Loads the answer-box suggestion list for a Tension question (from a hand-made category
 * or from Subjects) and tracks how that's going, so the UI can say so instead of just
 * showing nothing:
 *
 *   status: 'idle' | 'loading' | 'ready' | 'empty' | 'error'
 *
 * - The list is remembered by SOURCE + name ("subjects:Football" vs "category:Football") -
 *   a Tension category and a Subjects sport can share a name, and keying on the bare name
 *   would reuse the wrong one's list.
 * - A failed load retries on its own a few times, and can be retried by hand (retry()).
 * - Every failure and every empty list is reported to the server log (reportClientEvent).
 * - `unavailable` means the player can't pick from a list right now (failed or empty) - the
 *   caller should then let them submit what they typed instead of blocking the game.
 */
export function useAnswerOptions() {
  const options = ref([])
  const status = ref('idle')
  let wantedKey = null
  let attempt = 0
  let retryTimer = null
  let toastShown = false
  let last = { fromSubjects: false, key: '' }

  const unavailable = computed(() => status.value === 'error' || status.value === 'empty')

  async function load(fromSubjects, key) {
    if (!key) {
      wantedKey = null
      options.value = []
      status.value = 'idle'
      return
    }
    const sourceKey = `${fromSubjects ? 'subjects' : 'category'}:${key}`
    // Already have it, or already trying for it (including waiting on a scheduled retry) -
    // callers invoke this on every state update, so this must be cheap and idempotent.
    if (sourceKey === wantedKey && (status.value === 'ready' || status.value === 'loading'
        || status.value === 'empty' || status.value === 'error')) {
      return
    }

    wantedKey = sourceKey
    last = { fromSubjects, key }
    attempt = 0
    await fetchOnce(sourceKey)
  }

  async function fetchOnce(sourceKey) {
    clearTimeout(retryTimer)
    status.value = 'loading'
    const { fromSubjects, key } = last
    try {
      const list = await (fromSubjects ? api.fetchTensionSubjectOptions(key) : api.fetchTensionAnswerOptions(key))
      if (wantedKey !== sourceKey) return // a different question's list took over meanwhile
      options.value = list
      if (list.length) {
        status.value = 'ready'
      } else {
        status.value = 'empty'
        reportClientEvent({ area: 'tension-suggestions', kind: 'EMPTY_LIST', key: sourceKey })
      }
    } catch (e) {
      if (wantedKey !== sourceKey) return
      status.value = 'error'
      reportClientEvent({
        area: 'tension-suggestions',
        kind: 'FETCH_FAILED',
        key: sourceKey,
        httpStatus: e.response?.status,
        detail: e.response?.data?.message || e.message
      })
      if (!toastShown) {
        toastShown = true
        toast.show("Couldn't load the suggestions - retrying. You can still type your answer.", 'error')
      }
      if (attempt < RETRY_DELAYS_MS.length) {
        const delay = RETRY_DELAYS_MS[attempt++]
        retryTimer = setTimeout(() => { if (wantedKey === sourceKey) fetchOnce(sourceKey) }, delay)
      }
    }
  }

  // Manual "try again" from the UI - starts the whole retry sequence over.
  function retry() {
    if (!last.key) return
    attempt = 0
    wantedKey = `${last.fromSubjects ? 'subjects' : 'category'}:${last.key}`
    fetchOnce(wantedKey)
  }

  onUnmounted(() => clearTimeout(retryTimer))

  return { options, status, unavailable, load, retry }
}
