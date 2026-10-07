import { computed, ref } from 'vue'
import toast from '../services/toast'

// Drives the shared leaderboard modal (components/DailyQuizScoreboardModal.vue) for the weekly
// games' list pages, where one page shows several boards and each row opens its own leaderboard.
// `fetchBoard(id)` returns the backend's { averageScore, entryCount, yourLeaderboardPreference,
// entries: [{ userName, guessedCount, entryCount, isYou }] }; `savePreference(id, include)` stores
// the "show my name" choice. The modal itself wants { averageScore, maxScore, entries: [{ userName,
// score, maxScore, isYou }] }, which is what `data` is mapped to here.
export function useBoardLeaderboard(fetchBoard, savePreference) {
  const show = ref(false)
  const loading = ref(false)
  const title = ref('Leaderboard')
  const raw = ref(null)
  const optIn = ref(true)
  let boardId = null

  const data = computed(() => {
    const d = raw.value
    if (!d) return null
    return {
      averageScore: d.averageScore ?? 0,
      maxScore: d.entryCount,
      entries: (d.entries || []).map(e => ({ userName: e.userName, score: e.guessedCount, maxScore: e.entryCount, isYou: e.isYou }))
    }
  })
  // null hides the switch: only a player with a score on the board has a choice to make.
  const preference = computed(() => (raw.value && raw.value.yourLeaderboardPreference !== null ? optIn.value : null))

  async function open(id, boardTitle) {
    boardId = id
    title.value = boardTitle ? `Leaderboard - ${boardTitle}` : 'Leaderboard'
    raw.value = null
    show.value = true
    loading.value = true
    try {
      raw.value = await fetchBoard(id)
      optIn.value = raw.value.yourLeaderboardPreference ?? true
    } catch (e) {
      // a nice-to-have - fail quietly, the modal's empty state covers it
    } finally {
      loading.value = false
    }
  }

  async function updatePreference(include) {
    const previous = optIn.value
    optIn.value = include
    try {
      await savePreference(boardId, include)
      raw.value = await fetchBoard(boardId)
    } catch (e) {
      toast.show('Could not update your leaderboard preference.')
      optIn.value = previous
    }
  }

  return { show, loading, title, data, preference, open, updatePreference }
}
