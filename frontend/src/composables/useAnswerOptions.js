import { computed } from 'vue'
import api from '../services/api'
import { useReferenceList } from './useReferenceList'

/**
 * The answer-box suggestion list for a Tension question, from either a hand-made category or
 * from Subjects - see useReferenceList for the loading/retry/reporting behaviour.
 *
 * The list is remembered by SOURCE + name ("subjects:Football" vs "category:Football"): a Tension
 * category and a Subjects sport can share a name, and keying on the bare name would reuse the
 * wrong one's list.
 *
 * `unavailable` means the player can't pick from a list right now (failed or empty) - the caller
 * should then let them submit what they typed instead of blocking the game.
 */
export function useAnswerOptions() {
  const list = useReferenceList({
    area: 'tension-suggestions',
    failureToast: "Couldn't load the suggestions - retrying. You can still type your answer."
  })

  const unavailable = computed(() => list.status.value === 'error' || list.status.value === 'empty')

  function load(fromSubjects, key) {
    if (!key) {
      list.reset()
      return
    }
    return list.load(
      `${fromSubjects ? 'subjects' : 'category'}:${key}`,
      () => (fromSubjects ? api.fetchTensionSubjectOptions(key) : api.fetchTensionAnswerOptions(key))
    )
  }

  return { options: list.items, status: list.status, unavailable, load, retry: list.retry }
}
