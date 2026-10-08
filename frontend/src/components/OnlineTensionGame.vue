<template>
  <div>
    <div class="grid-status-bar">
      <div class="grid-progress">Question {{ (state?.currentQuestionIndex ?? 0) + 1 }} / {{ state?.totalQuestions ?? '?' }}</div>
      <div v-if="state" style="color:var(--text-dim); font-size:0.85rem;">Tension answers: {{ state.tensionAnswerCount }}</div>
    </div>

    <LoadingState v-if="loading" full message="Loading the game…" />
    <div v-if="error" class="banner error">{{ error }}</div>

    <template v-if="state && !state.finished">
      <h1 style="text-align:center; margin:6px 0 4px;">{{ state.questionTitle }}</h1>
      <p v-if="state.source" style="text-align:center; margin:0 0 4px; color:var(--text-dim); font-size:0.8rem;">
        Additional information: {{ state.source }}
      </p>
      <p v-if="state.tiebreaker" style="text-align:center; margin:0 0 4px; color:var(--text-dim); font-size:0.8rem;">
        Tiebreaker: {{ state.tiebreaker }}
      </p>
      <p v-if="lastUpdatedLabel" style="text-align:center; margin:0 0 20px; color:var(--text-dim); font-size:0.75rem;">
        {{ lastUpdatedLabel }}
      </p>

      <div class="tension-layout">
        <div class="tension-player-col">
          <div
            v-for="p in state.players"
            :key="p.participantId"
            class="tension-player-card"
            :class="{ disconnected: p.connected === false && p.participantId !== props.yourParticipantId }"
            :style="{ borderColor: p.color }"
          >
            <div>
              <strong>{{ p.name }}</strong>
              <span v-if="p.connected === false && p.participantId !== props.yourParticipantId" class="tag offline" style="display:block; margin-top:4px;">Offline</span>
              <!-- Once the reveal starts, each box shows what that player actually answered. -->
              <div v-if="state.roundRevealed && resultFor(p)" class="tension-player-answer">{{ resultFor(p).answerText }}</div>
              <div v-else class="tension-player-answer">{{ p.answered ? '✓ answered' : '— waiting —' }}</div>
            </div>
            <div style="text-align:right;">
              <div
                v-if="roundScoreShown(p)"
                class="tension-round-score"
                :class="{ positive: resultFor(p).score > 0, negative: resultFor(p).score < 0 }"
              >{{ formatScore(resultFor(p).score) }}</div>
              <div style="font-size:0.8rem; color:var(--text-dim);">Total: {{ shownTotal(p) }}</div>
            </div>
          </div>
        </div>

        <div class="tension-answers-panel">
          <template v-if="!state.roundRevealed">
            <!-- Host only, and only while nobody's answered yet - swapping later would
                 throw answers away. -->
            <div v-if="isHost && !(state.answersSoFar && state.answersSoFar.length)" style="text-align:center; margin-bottom:16px;">
              <button type="button" class="btn btn-secondary btn-sm" :disabled="swapping" @click="swapQuestion">
                {{ swapping ? 'Finding another…' : '↻ Already had this one? Pick a different question' }}
              </button>
            </div>
            <div v-if="state.answersSoFar && state.answersSoFar.length" style="text-align:left; margin-bottom:16px; border:1px solid var(--border); border-radius:var(--radius-sm); padding:10px 14px;">
              <div style="color:var(--text-dim); font-size:0.78rem; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:6px;">
                Answered so far this round
              </div>
              <div v-for="a in state.answersSoFar" :key="a.name" style="display:flex; justify-content:space-between; font-size:0.9rem; padding:2px 0;">
                <span>{{ a.name }}</span>
                <span style="color:var(--text-dim);">{{ a.answerText }}</span>
              </div>
            </div>

            <template v-if="isYourTurn">
              <h3 style="text-align:center; margin-top:0;">Your turn</h3>
              <form @submit.prevent="submit">
                <div class="guess-box" style="margin:0 auto;">
                  <input
                    type="text"
                    v-model="value"
                    @input="onInput"
                    placeholder="Type your answer…"
                    autocomplete="off"
                    autocorrect="off"
                    autocapitalize="off"
                    spellcheck="false"
                  />
                </div>
                <!-- Say what's going on with the suggestion list instead of just showing nothing -->
                <div v-if="optionsStatus === 'loading'" style="color:var(--text-dim); font-size:0.85rem; margin-top:8px;">
                  Loading suggestions…
                </div>
                <div v-else-if="optionsStatus === 'error'" style="color:var(--coral); font-size:0.85rem; margin-top:8px;">
                  Couldn't load the suggestions.
                  <button type="button" class="btn btn-secondary btn-sm" style="margin-left:6px;" @click="retryOptions">Try again</button>
                  <div style="color:var(--text-dim); margin-top:4px;">You can still type your answer exactly and submit it.</div>
                </div>
                <div v-else-if="optionsStatus === 'empty'" style="color:var(--text-dim); font-size:0.85rem; margin-top:8px;">
                  No suggestions are available for this question - type your answer exactly and submit it.
                </div>

                <div v-if="showDropdown" class="guess-results" style="margin-top:6px; max-height:220px; overflow-y:auto;">
                  <button
                    v-for="opt in filteredOptions"
                    :key="opt"
                    type="button"
                    class="guess-result-row"
                    @click="select(opt)"
                  >{{ opt }}</button>
                  <div v-if="!filteredOptions.length" class="guess-result-row" style="opacity:0.6; font-style:italic;">No matches</div>
                </div>
                <button type="submit" class="btn btn-primary" :disabled="!canSubmit || submitting" style="margin-top:16px; width:100%;">
                  {{ submitting ? 'Submitting…' : 'Submit' }}
                </button>
              </form>
            </template>
            <div v-else style="text-align:center; color:var(--text-dim);">
              Waiting for {{ currentTurnName }}'s turn…
            </div>
          </template>

          <template v-else>
            <h3 style="text-align:center; margin-top:0;">Answers</h3>
            <div class="tension-reveal-list">
              <div
                v-for="(ans, idx) in allAnswersList"
                :key="ans.text"
                class="tension-reveal-row"
                :class="{ 'is-revealed': revealIndex > idx, 'is-trap': revealIndex > idx && ans.tension }"
              >
                <div class="tension-reveal-rank">{{ idx + 1 }}</div>
                <div class="tension-reveal-main">
                  <div class="tension-reveal-answer">{{ revealIndex > idx ? ans.text : 'Hidden until revealed' }}</div>
                  <div v-if="revealIndex > idx" class="tension-reveal-tag" :class="ans.tension ? 'trap' : 'safe'">
                    {{ ans.tension ? 'Tension answer' : 'Safe answer' }}
                  </div>
                </div>
                <div v-if="revealIndex > idx" class="tension-reveal-guessers">
                  <span v-if="!guessersFor(ans).length" class="tension-reveal-nobody">Nobody guessed this</span>
                  <span
                    v-for="g in guessersFor(ans)"
                    :key="g.name"
                    class="tension-reveal-chip"
                    :style="{ borderColor: colorOf(g.name) }"
                  >
                    {{ g.name }}
                    <span class="tension-round-score" :class="{ positive: g.score > 0, negative: g.score < 0 }">{{ formatScore(g.score) }}</span>
                  </span>
                </div>
              </div>
            </div>
            <button
              v-if="revealIndex < allAnswersList.length"
              class="btn btn-secondary"
              style="margin-top:16px; width:100%;"
              @click="skipReveal"
            >Skip reveal</button>
            <button
              v-else-if="isHost"
              class="btn btn-primary"
              style="margin-top:16px; width:100%;"
              :disabled="advancing"
              @click="nextQuestion"
            >
              {{ advancing ? 'Loading…' : (state.currentQuestionIndex + 1 < state.totalQuestions ? 'Next question' : 'Finish game') }}
            </button>
            <div v-else style="margin-top:16px; text-align:center; color:var(--text-dim);">Waiting for the host to continue…</div>
          </template>
        </div>
      </div>
    </template>

    <div style="display:flex; align-items:center; gap:12px; margin-top:20px; flex-wrap:wrap;">
      <button class="btn btn-secondary btn-sm no-print" @click="leave">← Leave game</button>
      <span class="tag no-print" style="background:rgba(255,255,255,0.06); color:var(--text-dim);">Room: {{ roomCode }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, onUnmounted, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import LoadingState from './LoadingState.vue'
import { useRoomChannel, createStaleGuard } from '../composables/useRoomChannel'
import { useAnswerOptions } from '../composables/useAnswerOptions'
import { useTurnTitleAlert } from '../composables/useTurnTitleAlert'
import { formatLastUpdated } from '../constants'

const props = defineProps({
  roomCode: { type: String, required: true },
  yourParticipantId: { type: [Number, String], required: true },
  isHost: { type: Boolean, default: false }
})
const emit = defineEmits(['gameOver', 'leave'])

const state = ref(null)
const lastUpdatedLabel = computed(() => formatLastUpdated(state.value?.questionUpdatedAt))
const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const advancing = ref(false)

const value = ref('')
const dropdownOpen = ref(false)
const validSelection = ref(false)

const { options: allOptions, status: optionsStatus, unavailable: optionsUnavailable, load: loadOptions, retry: retryOptions } = useAnswerOptions()

// Computed from what's typed AND what's loaded, so suggestions appear the moment a list that
// was still loading arrives - not only on the next keystroke.
const filteredOptions = computed(() => {
  const term = value.value.trim().toLowerCase()
  if (term.length >= 3) {
    return allOptions.value.filter(o => o.toLowerCase().includes(term)).slice(0, 8)
  }
  if (term.length === 2) {
    // Below the normal "contains" threshold (too noisy at 2 characters across
    // a big answer list), but a short answer that's an exact match - like
    // "MG" - needs to still be reachable, not just prefix/substring matches.
    return allOptions.value.filter(o => o.toLowerCase() === term)
  }
  return []
})

const showDropdown = computed(() => {
  if (!dropdownOpen.value) return false
  const length = value.value.trim().length
  // While the list is still loading, or unavailable, the status line already says so - a
  // dropdown reading "No matches" there would be misleading.
  if (length >= 3) return optionsStatus.value === 'ready'
  if (length === 2) return filteredOptions.value.length > 0
  return false
})

// Normally the answer has to be picked from the list. When there's no list to pick from
// (failed to load, or empty) typing is all there is - blocking the player there would stall
// the whole room over a lookup problem.
const canSubmit = computed(() =>
  validSelection.value || (optionsUnavailable.value && value.value.trim().length > 0)
)

let wasRevealed = false
let revealTimer = null
const revealIndex = ref(0)

const isYourTurn = computed(() => !!state.value && state.value.currentTurnParticipantId === props.yourParticipantId)
useTurnTitleAlert(isYourTurn)
const currentTurnName = computed(() =>
  state.value?.players.find(p => p.participantId === state.value.currentTurnParticipantId)?.name || '…'
)

const allAnswersList = computed(() => {
  if (!state.value?.safeAnswers) return []
  return [
    ...state.value.safeAnswers.map(a => ({ text: a.text, rank: a.rank, tension: false })),
    ...state.value.tensionAnswers.map(a => ({ text: a.text, rank: a.rank, tension: true }))
  ]
})

// The server adds this round's points to totalScore the instant the last answer
// comes in (before the reveal animation even starts), so showing p.totalScore
// as-is would give the result away. Until the reveal has finished, show the
// total as it was before this round.
const revealFinished = computed(() => revealIndex.value >= allAnswersList.value.length)

function shownTotal(p) {
  if (!state.value?.roundRevealed || revealFinished.value) return p.totalScore
  const roundScore = state.value.roundResults?.find(r => r.participantId === p.participantId)?.score ?? 0
  return p.totalScore - roundScore
}

// This round's result for a player (their answer + points), once the round has been revealed.
function resultFor(p) {
  return (state.value?.roundResults || []).find(r => r.participantId === p.participantId) || null
}

// A player's round score appears when the reveal reaches their answer (same as pass-and-play); an
// answer that isn't on the list at all (it scores 0) shows once the whole list has been revealed.
function roundScoreShown(p) {
  const r = resultFor(p)
  if (!state.value?.roundRevealed || !r) return false
  const idx = allAnswersList.value.findIndex(a => a.text.toLowerCase() === (r.answerText || '').toLowerCase())
  return idx === -1 ? revealFinished.value : revealIndex.value > idx
}

// One chip per player who landed on this exact answer, each with their own
// round score - a trap answer can still be guessed by more than one player.
function guessersFor(ans) {
  return (state.value?.roundResults || [])
    .filter(r => r.answerText.toLowerCase() === ans.text.toLowerCase())
    .map(r => ({ name: r.name, score: r.score }))
}

function colorOf(name) {
  return state.value?.players.find(p => p.name === name)?.color || 'var(--border)'
}

function formatScore(score) {
  if (score === undefined || score === null) return ''
  return score > 0 ? `+${score}` : String(score)
}

const staleGuard = createStaleGuard()

async function poll() {
  const stillFresh = staleGuard.begin()
  try {
    const fresh = await api.getTensionOnlineState(props.roomCode)
    if (stillFresh()) applyState(fresh)
  } catch (e) {
    error.value = 'Lost connection to the room - retrying…'
  } finally {
    loading.value = false
  }
}

function applyState(fresh) {
  staleGuard.claim()
  error.value = ''
  state.value = fresh
  // Idempotent - called on every poll/broadcast, only actually fetches when the question's
  // list source changes (see useAnswerOptions).
  loadOptions(fresh.answersFromSubjects, fresh.answersFromSubjects ? fresh.answersSport : fresh.answersCategory)
  if (fresh.roundRevealed && !wasRevealed) {
    revealIndex.value = 0
    scheduleReveal()
  } else if (!fresh.roundRevealed && wasRevealed) {
    clearTimeout(revealTimer)
    revealIndex.value = 0
  }
  wasRevealed = fresh.roundRevealed
  if (fresh.finished) {
    stopPolling()
    const scores = fresh.players.map(p => [p.name, p.totalScore])
    emit('gameOver', scores)
  }
}

const REVEAL_STEP_MS = 1100
// Extra beat between the last safe answer and the first tension answer - see
// the same constant in TensionGame.vue for why.
const TENSION_REVEAL_PAUSE_MS = 1500

function scheduleReveal() {
  clearTimeout(revealTimer)
  if (revealIndex.value < allAnswersList.value.length) {
    const isTensionTransition = revealIndex.value === (state.value?.safeAnswers?.length ?? 0)
    const delay = REVEAL_STEP_MS + (isTensionTransition ? TENSION_REVEAL_PAUSE_MS : 0)
    revealTimer = setTimeout(() => {
      revealIndex.value += 1
      scheduleReveal()
    }, delay)
  }
}

function skipReveal() {
  clearTimeout(revealTimer)
  revealIndex.value = allAnswersList.value.length
}

const { stop: stopPolling } = useRoomChannel(`/topic/rooms/${props.roomCode}/state`, { poll, onMessage: applyState })

onUnmounted(() => clearTimeout(revealTimer))

function onInput() {
  validSelection.value = false
  dropdownOpen.value = true
}

function select(option) {
  value.value = option
  dropdownOpen.value = false
  validSelection.value = true
}

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  const stillFresh = staleGuard.begin()
  try {
    const fresh = await api.submitTensionOnlineAnswer(props.roomCode, value.value.trim())
    value.value = ''
    validSelection.value = false
    dropdownOpen.value = false
    if (stillFresh()) applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit that answer.'
  } finally {
    submitting.value = false
  }
}

const swapping = ref(false)
async function swapQuestion() {
  swapping.value = true
  const stillFresh = staleGuard.begin()
  try {
    const fresh = await api.rerollTensionOnlineQuestion(props.roomCode)
    if (stillFresh()) applyState(fresh)
  } catch (e) {
    toast.show(e.response?.data?.message || 'Could not swap to another question - please try again.', 'error')
  } finally {
    swapping.value = false
  }
}

async function nextQuestion() {
  advancing.value = true
  const stillFresh = staleGuard.begin()
  try {
    const fresh = await api.advanceTensionOnlineQuestion(props.roomCode)
    if (stillFresh()) applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not advance to the next question.'
  } finally {
    advancing.value = false
  }
}

function leave() {
  stopPolling()
  emit('leave')
}
</script>
