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
        Source: {{ state.source }}
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
            :style="{ borderColor: p.color }"
          >
            <div>
              <strong>{{ p.name }}</strong>
              <div class="tension-player-answer">{{ p.answered ? '✓ answered' : '— waiting —' }}</div>
            </div>
            <div style="text-align:right; font-size:0.8rem; color:var(--text-dim);">Total: {{ p.totalScore }}</div>
          </div>
        </div>

        <div class="tension-answers-panel">
          <template v-if="!state.roundRevealed">
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
                <div v-if="showDropdown" class="guess-results" style="margin-top:6px; max-height:220px; overflow-y:auto;">
                  <button
                    v-for="opt in filteredOptions"
                    :key="opt"
                    type="button"
                    class="guess-result-row"
                    @click="select(opt)"
                  >{{ opt }}</button>
                </div>
                <button type="submit" class="btn btn-primary" :disabled="!validSelection || submitting" style="margin-top:16px; width:100%;">
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
                <div class="tension-reveal-rank">{{ revealIndex > idx ? ans.rank : '?' }}</div>
                <div class="tension-reveal-main">
                  <div class="tension-reveal-answer">{{ revealIndex > idx ? ans.text : 'Hidden until revealed' }}</div>
                  <div v-if="revealIndex > idx" class="tension-reveal-tag" :class="ans.tension ? 'trap' : 'safe'">
                    {{ ans.tension ? '⚠ Tension answer' : 'Safe answer' }}
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
import LoadingState from './LoadingState.vue'
import { usePolling } from '../composables/usePolling'
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
const allOptions = ref([])
const filteredOptions = ref([])
const showDropdown = ref(false)
const validSelection = ref(false)

let lastCategory = null
let wasRevealed = false
let revealTimer = null
const revealIndex = ref(0)

const isYourTurn = computed(() => !!state.value && state.value.currentTurnParticipantId === props.yourParticipantId)
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

async function poll() {
  try {
    const fresh = await api.getTensionOnlineState(props.roomCode)
    applyState(fresh)
  } catch (e) {
    error.value = 'Lost connection to the room - retrying…'
  } finally {
    loading.value = false
  }
}

function applyState(fresh) {
  error.value = ''
  state.value = fresh
  if (fresh.answersCategory && fresh.answersCategory !== lastCategory) {
    lastCategory = fresh.answersCategory
    loadOptions(fresh.answersCategory)
  }
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

function scheduleReveal() {
  clearTimeout(revealTimer)
  if (revealIndex.value < allAnswersList.value.length) {
    revealTimer = setTimeout(() => {
      revealIndex.value += 1
      scheduleReveal()
    }, 1100)
  }
}

function skipReveal() {
  clearTimeout(revealTimer)
  revealIndex.value = allAnswersList.value.length
}

async function loadOptions(category) {
  try {
    allOptions.value = await api.fetchTensionAnswerOptions(category)
  } catch (e) {
    // autocomplete is a convenience, not essential - fail quietly
  }
}

const { stop: stopPolling } = usePolling(poll, 2000)

onUnmounted(() => clearTimeout(revealTimer))

function onInput() {
  validSelection.value = false
  const term = value.value.trim().toLowerCase()
  if (term.length >= 3) {
    filteredOptions.value = allOptions.value.filter(o => o.toLowerCase().includes(term)).slice(0, 8)
    showDropdown.value = true
  } else if (term.length === 2) {
    // Below the normal "contains" threshold (too noisy at 2 characters across
    // a big answer list), but a short answer that's an exact match - like
    // "MG" - needs to still be reachable, not just prefix/substring matches.
    filteredOptions.value = allOptions.value.filter(o => o.toLowerCase() === term)
    showDropdown.value = filteredOptions.value.length > 0
  } else {
    filteredOptions.value = []
    showDropdown.value = false
  }
}

function select(option) {
  value.value = option
  showDropdown.value = false
  validSelection.value = true
}

async function submit() {
  if (!validSelection.value) return
  submitting.value = true
  try {
    const fresh = await api.submitTensionOnlineAnswer(props.roomCode, value.value.trim())
    value.value = ''
    validSelection.value = false
    applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit that answer.'
  } finally {
    submitting.value = false
  }
}

async function nextQuestion() {
  advancing.value = true
  try {
    const fresh = await api.advanceTensionOnlineQuestion(props.roomCode)
    applyState(fresh)
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
