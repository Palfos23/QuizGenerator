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

        <form @submit.prevent="submit" style="position:relative;">
          <input
            type="text"
            v-model="value"
            @input="onInput"
            placeholder="Type your answer…"
            autocomplete="off"
            style="text-align:center;"
          />

          <div v-if="duplicateError" style="color:var(--coral); font-size:0.9rem; margin-top:8px;">
            That answer's already been used by another player this round.
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

          <button type="submit" class="btn btn-primary" :disabled="!validSelection" style="margin-top:16px; width:100%;">
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
import { onMounted, ref } from 'vue'
import api from '../services/api'

const props = defineProps({
  currentPlayer: { type: String, required: true },
  questionTitle: { type: String, required: true },
  tensionCount: { type: Number, default: 0 },
  category: { type: String, default: '' },
  answeredPlayers: { type: Array, default: () => [] },
  allPlayers: { type: Array, default: () => [] },
  usedAnswers: { type: Array, default: () => [] }
})
const emit = defineEmits(['submit'])

const value = ref('')
const allOptions = ref([])
const filteredOptions = ref([])
const showDropdown = ref(false)
const validSelection = ref(false)
const duplicateError = ref(false)

onMounted(async () => {
  if (!props.category) return
  try {
    allOptions.value = await api.fetchTensionAnswerOptions(props.category)
  } catch (e) {
    // autocomplete is a convenience, not essential - fail quietly
  }
})

function onInput() {
  validSelection.value = false
  duplicateError.value = false
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
  duplicateError.value = false
}

function submit() {
  const duplicate = props.usedAnswers.some(a => a.toLowerCase() === value.value.trim().toLowerCase())
  if (duplicate) {
    duplicateError.value = true
    return
  }
  if (!validSelection.value) return
  emit('submit', value.value.trim())
  value.value = ''
  validSelection.value = false
}
</script>
