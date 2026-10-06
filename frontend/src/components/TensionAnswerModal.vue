<template>
  <div class="modal-backdrop">
    <div class="modal">
      <div style="text-align:center;">
        <div style="color:var(--gold); font-weight:700; font-size:1.1rem; margin-bottom:6px;">
          {{ currentPlayer }}'s turn
        </div>
        <p style="font-style:italic; font-size:1.15rem; margin:0 0 6px;">{{ questionTitle }}</p>
        <p style="color:var(--text-dim); font-size:0.8rem; margin-bottom:20px;">
          Tension answers on this one: {{ tensionCount }}
        </p>

        <div v-if="answeredPlayers.length" style="text-align:left; margin-bottom:20px; border:1px solid var(--border); border-radius:var(--radius-sm); padding:10px 14px;">
          <div style="color:var(--text-dim); font-size:0.78rem; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:6px;">
            Answered so far this round
          </div>
          <div v-for="(name, i) in answeredPlayers" :key="name" style="display:flex; justify-content:space-between; font-size:0.9rem; padding:2px 0;">
            <span>{{ name }}</span>
            <span style="color:var(--text-dim);">{{ usedAnswers[i] }}</span>
          </div>
        </div>

        <form @submit.prevent="submit" style="position:relative;">
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

          <div v-if="duplicateError" style="color:var(--coral); font-size:0.9rem; margin-top:8px;">
            That answer's already been used by another player this round.
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

          <div v-if="showDropdown" class="guess-results" style="position:absolute; bottom:100%; left:0; right:0; margin-bottom:6px; max-height:220px; overflow-y:auto;">
            <button
              v-for="opt in filteredOptions"
              :key="opt"
              type="button"
              class="guess-result-row"
              @click="select(opt)"
            >{{ opt }}</button>
            <div v-if="!filteredOptions.length" class="guess-result-row" style="opacity:0.6; font-style:italic;">No matches</div>
          </div>

          <button type="submit" class="btn btn-primary" :disabled="!canSubmit" style="margin-top:16px; width:100%;">
            Submit
          </button>
        </form>

        <div style="display:flex; justify-content:center; gap:8px; margin-top:20px;">
          <span
            v-for="p in allPlayers"
            :key="p"
            :title="p"
            class="turn-dot"
            :class="{ answered: answeredPlayers.includes(p) }"
          ></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAnswerOptions } from '../composables/useAnswerOptions'

const props = defineProps({
  currentPlayer: { type: String, required: true },
  questionTitle: { type: String, required: true },
  tensionCount: { type: Number, default: 0 },
  category: { type: String, default: '' },
  answersFromSubjects: { type: Boolean, default: false },
  answersSport: { type: String, default: '' },
  answeredPlayers: { type: Array, default: () => [] },
  allPlayers: { type: Array, default: () => [] },
  usedAnswers: { type: Array, default: () => [] }
})
const emit = defineEmits(['submit'])

const value = ref('')
const dropdownOpen = ref(false)
const validSelection = ref(false)
const duplicateError = ref(false)

const { options: allOptions, status: optionsStatus, unavailable: optionsUnavailable, load: loadOptions, retry: retryOptions } = useAnswerOptions()

onMounted(() => {
  loadOptions(props.answersFromSubjects, props.answersFromSubjects ? props.answersSport : props.category)
})

// Computed from what's typed AND what's loaded, so suggestions appear the moment a list
// that was still loading arrives - not only on the next keystroke.
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
  // While the list is still loading, or unavailable, the status line below the box already
  // says so - a dropdown reading "No matches" there would be misleading.
  if (length >= 3) return optionsStatus.value === 'ready'
  if (length === 2) return filteredOptions.value.length > 0
  return false
})

// Normally the answer has to be picked from the list. When there's no list to pick from
// (failed to load, or empty) typing is all there is - blocking the player there would
// stall the whole game over a lookup problem.
const canSubmit = computed(() =>
  validSelection.value || (optionsUnavailable.value && value.value.trim().length > 0)
)

function onInput() {
  validSelection.value = false
  duplicateError.value = false
  dropdownOpen.value = true
}

function select(option) {
  value.value = option
  dropdownOpen.value = false
  validSelection.value = true
  duplicateError.value = false
}

function submit() {
  const duplicate = props.usedAnswers.some(a => a.toLowerCase() === value.value.trim().toLowerCase())
  if (duplicate) {
    duplicateError.value = true
    return
  }
  if (!canSubmit.value) return
  emit('submit', value.value.trim())
  value.value = ''
  validSelection.value = false
  dropdownOpen.value = false
}
</script>
