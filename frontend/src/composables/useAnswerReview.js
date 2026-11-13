import { computed, reactive, ref } from 'vue'
import api from '../services/api'
import { t as _t } from '../i18n'

// State and actions behind the admin's "Rett svar" page. The point is speed: a decision shows up at once on the screen and
// is sent to the server in the background, in batches (a quick run of clicks becomes one request), so there is never a
// wait or a list that jumps. Until a batch has been sent it can be undone.
const SEND_AFTER_MS = 4000 // quiet time before a batch of decisions goes to the server
const SEND_AT_ONCE = 40 // ...or as soon as this many are waiting

export function useAnswerReview() {
  const tree = ref({ days: [], pendingTotal: 0, likelyTypoTotal: 0 })
  const loading = ref(true)
  const error = ref('')
  const remember = ref(true) // "husk valget": the same answer is graded automatically next time
  const local = reactive({}) // group id -> 'correct' | 'wrong': what has been decided on this page
  const queue = ref([]) // decisions not yet sent
  const history = ref([]) // batches of decisions, newest last, for undo
  const saving = ref(false)
  const failed = ref(false)
  let timer = null
  let inflight = null

  // ---- reading ----

  function stateOf(g, id) {
    if (local[id]) return local[id]
    if (g.pendingCount > 0) return 'pending'
    if (g.correctCount > 0 && g.incorrectCount > 0) return 'mixed'
    return g.correctCount > 0 ? 'correct' : 'wrong'
  }

  // every distinct answer, in page order, with what is known about it
  const rows = computed(() => {
    const out = []
    for (const day of tree.value.days) {
      for (const q of day.questions) {
        for (const g of q.groups) {
          const id = `${q.questionId}|${g.key}`
          out.push({ id, day, q, g, state: stateOf(g, id), wasPending: g.pendingCount > 0 })
        }
      }
    }
    return out
  })
  const pendingRows = computed(() => rows.value.filter(r => r.state === 'pending'))
  const typoRows = computed(() => pendingRows.value.filter(r => r.g.likelyTypo))
  const pendingAnswers = computed(() => pendingRows.value.reduce((sum, r) => sum + r.g.pendingCount, 0))

  const canUndo = computed(() => {
    const last = history.value[history.value.length - 1]
    return !!last && last.items.every(i => !i.sent)
  })
  const lastBatch = computed(() => history.value[history.value.length - 1] || null)
  // 'saved' | 'waiting' (decisions on their way) | 'saving' | 'failed'
  const saveState = computed(() => (failed.value ? 'failed' : saving.value ? 'saving' : queue.value.length ? 'waiting' : 'saved'))

  // ---- loading ----

  async function load() {
    try {
      tree.value = await api.adminGetDailyQuizAnswerGroups()
      if (!queue.value.length && !saving.value) Object.keys(local).forEach(k => delete local[k]) // the server knows it all now
      error.value = ''
    } catch (e) {
      error.value = e.response?.data?.message || _t('Kunne ikke laste svarene.')
    } finally {
      loading.value = false
    }
  }

  // ---- deciding ----

  /** Marks these answers right or wrong here and queues them for the server. Returns the batch (for undo), or null if nothing changed. */
  function decide(targets, correct, label) {
    const batch = { label, correct, items: [] }
    for (const r of targets) {
      if (r.state === (correct ? 'correct' : 'wrong')) continue
      const at = queue.value.findIndex(d => d.id === r.id)
      batch.items.push({ id: r.id, prev: local[r.id] ?? null, prevQueued: at >= 0 ? queue.value[at] : null, sent: false })
      local[r.id] = correct ? 'correct' : 'wrong'
      const entry = { id: r.id, questionId: r.q.questionId, text: r.g.text, correct, remember: remember.value }
      if (at >= 0) queue.value[at] = entry
      else queue.value.push(entry)
    }
    if (!batch.items.length) return null
    history.value.push(batch)
    if (history.value.length > 50) history.value.shift()
    schedule()
    return batch
  }

  function undo() {
    if (!canUndo.value) return false
    const batch = history.value.pop()
    for (const item of batch.items) {
      if (item.prev) local[item.id] = item.prev
      else delete local[item.id]
      const at = queue.value.findIndex(d => d.id === item.id)
      if (item.prevQueued) {
        if (at >= 0) queue.value[at] = item.prevQueued
        else queue.value.push(item.prevQueued)
      } else if (at >= 0) {
        queue.value.splice(at, 1)
      }
    }
    return true
  }

  // ---- sending ----

  function schedule() {
    clearTimeout(timer)
    if (queue.value.length >= SEND_AT_ONCE) flush()
    else timer = setTimeout(flush, SEND_AFTER_MS)
  }

  /** Sends whatever is waiting. Resolves when it has been saved (or failed). */
  async function flush() {
    clearTimeout(timer)
    timer = null
    if (inflight) await inflight
    if (!queue.value.length) return
    const batch = queue.value.splice(0)
    saving.value = true
    failed.value = false
    inflight = api.adminDecideDailyQuizAnswerGroups(batch.map(({ questionId, text, correct, remember }) => ({ questionId, text, correct, remember })))
      .then(() => {
        const sent = new Set(batch.map(d => d.id))
        for (const b of history.value) for (const i of b.items) if (sent.has(i.id)) i.sent = true
      })
      .catch(e => {
        queue.value.unshift(...batch) // nothing is lost: it stays queued until it can be sent
        failed.value = true
        error.value = e.response?.data?.message || _t('Kunne ikke lagre - prøv igjen.')
      })
      .finally(() => {
        saving.value = false
        inflight = null
      })
    await inflight
  }

  function dispose() {
    clearTimeout(timer)
  }

  return {
    tree, loading, error, remember, rows, pendingRows, typoRows, pendingAnswers, canUndo, lastBatch, saveState,
    queue, load, decide, undo, flush, dispose
  }
}
