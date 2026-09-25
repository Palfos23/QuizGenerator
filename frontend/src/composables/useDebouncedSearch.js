import { ref } from 'vue'
import { createStaleGuard } from './useRoomChannel'
import toast from '../services/toast'

// The shared "type into a box, get an autocomplete dropdown" behavior used by
// every game's guess box and several admin search boxes - previously copied
// inline into ~17 places, each with the same two bugs: a slow/late response
// from an OLDER keystroke could silently overwrite a newer, correct one (no
// staleness check), and a failed request (a network hiccup, or this app's
// Render backend cold-starting after being idle) was swallowed with an empty
// catch - the dropdown just never appeared, indistinguishable from "no
// matches". Fixed here once via createStaleGuard() (the same guard the
// Online*Game.vue poll loops already use for this exact class of race) and a
// one-time toast on failure, instead of in every call site separately.
//
// fetcher(term) does the actual network call and returns the raw results.
// postFilter(term, results), if given, runs after a successful fetch (e.g.
// narrowing to an exact match for very short terms) - purely a same-tick
// transform, never itself a source of staleness.
export function useDebouncedSearch(fetcher, { delay = 250, minLength = 2, postFilter } = {}) {
  const results = ref([])
  const loading = ref(false)
  const staleGuard = createStaleGuard()
  let timer = null
  let toastShownForThisFailureStreak = false

  function search(term) {
    clearTimeout(timer)
    const trimmed = (term || '').trim()

    if (trimmed.length < minLength) {
      // The box is now empty/too short to search - whatever request is still
      // in flight from a previous, longer term no longer matters either.
      staleGuard.markApplied()
      results.value = []
      loading.value = false
      return
    }

    timer = setTimeout(async () => {
      const stillFresh = staleGuard.begin()
      loading.value = true
      try {
        const raw = await fetcher(trimmed)
        if (!stillFresh()) return // a newer keystroke's request already won
        staleGuard.markApplied()
        results.value = postFilter ? postFilter(trimmed, raw) : raw
        toastShownForThisFailureStreak = false
      } catch (e) {
        if (!stillFresh()) return
        staleGuard.markApplied()
        // Once per run of failures, not once per keystroke - typing several
        // letters while offline shouldn't re-trigger the same toast each time.
        if (!toastShownForThisFailureStreak) {
          toast.show("Couldn't search - check your connection and try again.", 'error')
          toastShownForThisFailureStreak = true
        }
      } finally {
        if (stillFresh()) loading.value = false
      }
    }, delay)
  }

  return { results, loading, search }
}
