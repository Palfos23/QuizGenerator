<template>
  <div>
    <div class="grid-status-bar">
      <div class="grid-progress">Question {{ (state?.currentQuestionIndex ?? 0) + 1 }} / {{ state?.totalQuestions ?? '?' }}</div>
      <div v-if="state" style="color:var(--text-dim); font-size:0.85rem;">Tension answers: {{ state.tensionAnswerCount }}</div>
    </div>

    <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
    <div v-if="error" class="banner error">{{ error }}</div>

    <template v-if="state && !state.finished">
      <h1 style="text-align:center; margin:6px 0 24px;">{{ state.questionTitle }}</h1>

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
                <div class="field" style="margin-bottom:0;">
                  <input
                    type="text"
                    v-model="value"
                    @input="onInput"
                    placeholder="Type your answer…"
                    autocomplete="off"
                    style="text-align:center;"
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
            <h3 style="text-align:center; margin-top:0;">Results</h3>
            <table class="table scoreboard-table">
              <thead>
                <tr><th style="width:40%;">Player</th><th style="width:35%;">Answer</th><th style="width:25%; text-align:right;">Score</th></tr>
              </thead>
              <tbody>
                <tr v-for="r in state.roundResults" :key="r.participantId" :class="{ 'tension-row-trap': r.matchedTension }">
                  <td>{{ r.name }}</td>
                  <td>{{ r.answerText }}</td>
                  <td style="text-align:right;">{{ r.score > 0 ? '+' + r.score : r.score }}</td>
                </tr>
              </tbody>
            </table>
            <button class="btn btn-primary" style="margin-top:16px; width:100%;" :disabled="advancing" @click="nextQuestion">
              {{ advancing ? 'Loading…' : (state.currentQuestionIndex + 1 < state.totalQuestions ? 'Next question' : 'Finish game') }}
            </button>
          </template>
        </div>
      </div>
    </template>

    <button class="btn btn-secondary btn-sm no-print" style="margin-top:20px;" @click="leave">← Leave game</button>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import api from '../services/api'

const props = defineProps({
  roomCode: { type: String, required: true },
  yourParticipantId: { type: [Number, String], required: true }
})
const emit = defineEmits(['gameOver', 'leave'])

const state = ref(null)
const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const advancing = ref(false)

const value = ref('')
const allOptions = ref([])
const filteredOptions = ref([])
const showDropdown = ref(false)
const validSelection = ref(false)

let pollTimer = null
let lastCategory = null

const isYourTurn = computed(() => !!state.value && state.value.currentTurnParticipantId === props.yourParticipantId)
const currentTurnName = computed(() =>
  state.value?.players.find(p => p.participantId === state.value.currentTurnParticipantId)?.name || '…'
)

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
  if (fresh.finished) {
    clearInterval(pollTimer)
    const scores = {}
    fresh.players.forEach(p => { scores[p.name] = p.totalScore })
    emit('gameOver', scores)
  }
}

async function loadOptions(category) {
  try {
    allOptions.value = await api.fetchTensionAnswerOptions(category)
  } catch (e) {
    // autocomplete is a convenience, not essential - fail quietly
  }
}

onMounted(() => {
  poll()
  pollTimer = setInterval(poll, 2000)
})
onUnmounted(() => clearInterval(pollTimer))

function onInput() {
  validSelection.value = false
  if (value.value.trim().length >= 2) {
    const term = value.value.toLowerCase()
    filteredOptions.value = allOptions.value.filter(o => o.toLowerCase().includes(term)).slice(0, 12)
    showDropdown.value = true
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
  clearInterval(pollTimer)
  emit('leave')
}
</script>
