<template>
  <div>
    <div class="grid-status-bar">
      <div class="grid-progress">{{ $t('Runde') }} {{ (state?.currentQuestionIndex ?? 0) + 1 }} / {{ state?.totalQuestions ?? '?' }}</div>
    </div>

    <LoadingState v-if="loading" full :message="$t('Laster spillet…')" />
    <div v-if="error" class="banner error">{{ error }}</div>

    <template v-if="state && !state.finished">
      <h1 style="text-align:center; margin:6px 0 20px;">{{ fmt(state.targetValue) }} {{ state.statLabel }}</h1>

      <div class="mp-player-row">
        <div
          v-for="p in state.players"
          :key="p.participantId"
          class="mp-player-card"
          :class="{ 'active-turn': p.participantId === state.currentTurnParticipantId && !state.roundRevealed, 'bullseye-just-eliminated': p.eliminated && p.eliminatedAtRound === state.currentQuestionIndex + 1 }"
          :style="{ borderColor: p.color, opacity: p.eliminated ? 0.5 : (p.connected === false && p.participantId !== props.yourParticipantId ? 0.55 : 1) }"
        >
          <strong>{{ p.name }}<template v-if="p.eliminated"> ❌</template></strong>
          <span v-if="p.connected === false && p.participantId !== props.yourParticipantId" class="tag offline" style="display:block; margin-top:4px;">{{ $t('Frakoblet§OnlineBullseyeGameT') }}</span>
          <div class="tension-player-answer">{{ p.answered ? $t('✓ svart') : (p.eliminated ? $t('ute§OnlineBullseyeGameM') : $t('— venter —')) }}</div>
        </div>
      </div>

      <template v-if="!state.roundRevealed">
        <!-- Host only, and only while nobody's answered yet - swapping later would
             throw answers away. -->
        <div v-if="isHost && !(state.answersSoFar && state.answersSoFar.length)" class="no-print" style="text-align:center; margin-top:10px;">
          <button type="button" class="btn btn-secondary btn-sm" :disabled="swapping" @click="swapQuestion">
            {{ swapping ? $t('Finner et nytt…') : $t('↻ Hatt dette før? Velg et annet spørsmål') }}
          </button>
        </div>
        <div v-if="state.answersSoFar && state.answersSoFar.length" style="max-width:420px; margin:16px auto 0; border:1px solid var(--border); border-radius: 2px; padding:10px 14px;">
          <div style="color:var(--text-dim); font-size:0.78rem; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:6px;">
            {{ $t('Svart så langt denne runden') }}
          </div>
          <div v-for="a in state.answersSoFar" :key="a.name" style="display:flex; justify-content:space-between; font-size:0.9rem; padding:2px 0;">
            <span>{{ a.name }}</span>
            <span style="color:var(--text-dim);">{{ a.guessedName }}</span>
          </div>
        </div>

        <p v-if="!isYourTurn" style="text-align:center; color:var(--text-dim); margin-top:16px;">
          {{ $t('Venter på') }} {{ currentTurnName }}{{ $t('sin tur…') }}
        </p>
      </template>

      <template v-else>
        <div class="tension-answers-panel" style="margin:16px auto 0;">
          <h3 style="text-align:center; margin-top:0;">{{ $t('Svar§OnlineBullseyeGameT') }}</h3>

          <div class="bullseye-reveal-list">
            <div
              v-for="(a, idx) in state.roundResults"
              :key="a.participantId"
              class="bullseye-reveal-row"
              :class="{
                'is-revealed': revealIndex > idx,
                'is-eliminated': revealIndex > idx && a.eliminatedThisRound
              }"
            >
              <div class="bullseye-reveal-rank">{{ revealIndex > idx ? idx + 1 : '?' }}</div>
              <div class="bullseye-reveal-main">
                <div class="bullseye-reveal-player">{{ revealIndex > idx ? a.name : '???' }}</div>
                <div class="bullseye-reveal-guess">{{ revealIndex > idx ? a.guessedName : $t('Skjult til avsløringen') }}</div>
              </div>
              <div v-if="revealIndex > idx" class="bullseye-reveal-stats">
                <div class="bullseye-reveal-value">{{ fmt(a.statValue) }}</div>
                <div class="bullseye-reveal-distance">{{ fmt(a.distance) }} {{ $t('unna') }}</div>
              </div>
            </div>
          </div>

          <template v-if="revealIndex >= (state.roundResults?.length || 0)">
            <div v-if="eliminatedName" class="bullseye-elimination-banner">❌ {{ eliminatedName }} {{ $t('er ute!') }}</div>

            <div v-if="state.bullseyeAnswers && state.bullseyeAnswers.length" class="bullseye-reveal-divider">{{ $t('Det riktige svaret') }}</div>
            <div v-if="state.bullseyeAnswers && state.bullseyeAnswers.length" class="bullseye-truth-callout">
              <div class="bullseye-truth-label">{{ state.bullseyeAnswers[0].distance === 0 ? '🎯 Bullseye' : $t('Nærmeste mulige svar') }}</div>
              <div class="bullseye-truth-list">
                <span
                  v-for="e in state.bullseyeAnswers"
                  :key="e.athleteId ?? e.athleteName"
                  class="bullseye-truth-name"
                  :class="e.foundBy ? 'found' : 'not-found'"
                >
                  {{ e.foundBy ? '✓' : '✕' }} {{ e.athleteName }} ({{ fmt(e.statValue) }})<template v-if="e.foundBy"> {{ $t('— funnet av') }} {{ e.foundBy }}</template>
                </span>
              </div>
            </div>
          </template>

          <div style="text-align:center; margin-top:16px;">
            <button v-if="revealIndex < (state.roundResults?.length || 0)" class="btn btn-secondary" @click="skipReveal">
              {{ $t('Hopp over avsløringen') }}
            </button>
            <button
              v-else-if="isHost"
              class="btn btn-primary"
              :disabled="advancing"
              @click="nextRound"
            >{{ advancing ? $t('Laster…') : (state.currentQuestionIndex + 1 < state.totalQuestions ? $t('Neste runde') : $t('Fullfør spillet')) }}</button>
            <div v-else style="color:var(--text-dim);">{{ $t('Venter på at verten skal fortsette…') }}</div>
          </div>
        </div>
      </template>
    </template>

    <BullseyeAnswerModal
      v-if="state && isYourTurn && !state.roundRevealed"
      :current-player="currentTurnName"
      :target-value="state.targetValue"
      :stat-label="state.statLabel"
      :group-digits="state.groupDigits !== false"
      :entries="state.entries"
      :answered-players="(state.answersSoFar || []).map(a => a.name)"
      :all-players="state.players.filter(p => !p.eliminated).map(p => p.name)"
      :used-names="(state.answersSoFar || []).map(a => a.guessedName)"
      @submit="submit"
    />

    <div style="display:flex; align-items:center; gap:12px; margin-top:20px; flex-wrap:wrap;">
      <button class="btn btn-secondary btn-sm no-print" @click="leave">{{ $t('← Forlat spillet') }}</button>
      <span class="tag no-print" style="background:rgb(var(--fg-rgb) / 0.06); color:var(--text-dim);">{{ $t('Rom:') }} {{ roomCode }}</span>
    </div>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, onUnmounted, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import LoadingState from './LoadingState.vue'
import BullseyeAnswerModal from './BullseyeAnswerModal.vue'
import { useRoomChannel, createStaleGuard } from '../composables/useRoomChannel'
import { useTurnTitleAlert } from '../composables/useTurnTitleAlert'
import { formatNumber } from '../constants'

const props = defineProps({
  roomCode: { type: String, required: true },
  yourParticipantId: { type: [Number, String], required: true },
  isHost: { type: Boolean, default: false }
})
const emit = defineEmits(['gameOver', 'leave'])

const state = ref(null)

// See BullseyeQuestion.groupDigits - false for a non-population stat (a
// year, say), where "1 798" reads as wrong rather than just differently
// formatted. Falls back to grouped when the flag is missing entirely,
// matching the DB column's own default.
function fmt(n) {
  if (n === null || n === undefined) return n
  return state.value && state.value.groupDigits === false ? String(n) : formatNumber(n)
}

const loading = ref(true)
const error = ref('')
const submitting = ref(false)
const advancing = ref(false)

let wasRevealed = false
let revealTimer = null
const revealIndex = ref(0)
const eliminatedName = ref(null)

const isYourTurn = computed(() => !!state.value && state.value.currentTurnParticipantId === props.yourParticipantId)
useTurnTitleAlert(isYourTurn)
const currentTurnName = computed(() =>
  state.value?.players.find(p => p.participantId === state.value.currentTurnParticipantId)?.name || '…'
)

const staleGuard = createStaleGuard()

async function poll() {
  const stillFresh = staleGuard.begin()
  try {
    const fresh = await api.getBullseyeOnlineState(props.roomCode)
    if (stillFresh()) applyState(fresh)
  } catch (e) {
    error.value = _t('Mistet forbindelsen til rommet - prøver igjen…')
  } finally {
    loading.value = false
  }
}

function applyState(fresh) {
  staleGuard.claim()
  error.value = ''
  state.value = fresh
  if (fresh.roundRevealed && !wasRevealed) {
    revealIndex.value = 0
    eliminatedName.value = null
    scheduleReveal()
  } else if (!fresh.roundRevealed && wasRevealed) {
    clearTimeout(revealTimer)
    revealIndex.value = 0
    eliminatedName.value = null
  }
  wasRevealed = fresh.roundRevealed
  if (fresh.finished) {
    stopPolling()
    const scores = fresh.players.map(p => [p.name, p.eliminatedAtRound ?? fresh.players.length])
    emit('gameOver', scores)
  }
}

function scheduleReveal() {
  clearTimeout(revealTimer)
  const total = state.value?.roundResults?.length || 0
  if (revealIndex.value < total) {
    revealTimer = setTimeout(() => {
      revealIndex.value += 1
      if (revealIndex.value >= total) {
        eliminatedName.value = state.value.roundResults.find(r => r.eliminatedThisRound)?.name || null
      }
      scheduleReveal()
    }, 1400)
  }
}

function skipReveal() {
  clearTimeout(revealTimer)
  const total = state.value?.roundResults?.length || 0
  revealIndex.value = total
  eliminatedName.value = state.value?.roundResults?.find(r => r.eliminatedThisRound)?.name || null
}

const { stop: stopPolling } = useRoomChannel(`/topic/rooms/${props.roomCode}/state`, { poll, onMessage: applyState })

onUnmounted(() => clearTimeout(revealTimer))

async function submit(guessedName) {
  if (submitting.value) return
  submitting.value = true
  const stillFresh = staleGuard.begin()
  try {
    const fresh = await api.submitBullseyeOnlineAnswer(props.roomCode, guessedName)
    if (stillFresh()) applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke sende inn svaret.')
  } finally {
    submitting.value = false
  }
}

const swapping = ref(false)
async function swapQuestion() {
  swapping.value = true
  const stillFresh = staleGuard.begin()
  try {
    const fresh = await api.rerollBullseyeOnlineQuestion(props.roomCode)
    if (stillFresh()) applyState(fresh)
  } catch (e) {
    toast.show(e.response?.data?.message || _t('Kunne ikke bytte til et annet spørsmål - prøv igjen.'), 'error')
  } finally {
    swapping.value = false
  }
}

async function nextRound() {
  advancing.value = true
  const stillFresh = staleGuard.begin()
  try {
    const fresh = await api.advanceBullseyeOnlineRound(props.roomCode)
    if (stillFresh()) applyState(fresh)
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke gå videre til neste runde.')
  } finally {
    advancing.value = false
  }
}

function leave() {
  stopPolling()
  emit('leave')
}
</script>
