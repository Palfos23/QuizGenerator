<template>
  <div class="modal-backdrop">
    <div class="modal">
      <div style="text-align:center;">
        <div style="color:var(--gold); font-weight:700; font-size:1.1rem; margin-bottom:6px;">
          {{ currentPlayer }}'s turn
        </div>
        <p style="font-size:1.4rem; font-weight:700; margin:0 0 20px;">{{ targetValue }} {{ statLabel }}</p>

        <div v-if="answeredPlayers.length" style="text-align:left; margin-bottom:20px; border:1px solid var(--border); border-radius:var(--radius-sm); padding:10px 14px;">
          <div style="color:var(--text-dim); font-size:0.78rem; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:6px;">
            Answered so far this round
          </div>
          <div v-for="(name, i) in answeredPlayers" :key="name" style="display:flex; justify-content:space-between; font-size:0.9rem; padding:2px 0;">
            <span>{{ name }}</span>
            <span style="color:var(--text-dim);">{{ usedNames[i] }}</span>
          </div>
        </div>

        <form @submit.prevent="submit" style="position:relative;">
          <div class="field" style="margin-bottom:0;">
            <input
              type="text"
              v-model="value"
              @input="onInput"
              placeholder="Type any name…"
              autocomplete="off"
              style="text-align:center;"
            />
          </div>

          <div v-if="duplicateError" style="color:var(--coral); font-size:0.9rem; margin-top:8px;">
            That name's already been used this round.
          </div>

          <div v-if="showDropdown" class="guess-results" style="position:absolute; bottom:100%; left:0; right:0; margin-bottom:6px; max-height:220px; overflow-y:auto;">
            <button
              v-for="opt in filteredOptions"
              :key="opt"
              type="button"
              class="guess-result-row"
              @click="select(opt)"
            >{{ opt }}</button>
          </div>

          <button type="submit" class="btn btn-primary" :disabled="!value.trim()" style="margin-top:16px; width:100%;">
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
import { ref } from 'vue'

const props = defineProps({
  currentPlayer: { type: String, required: true },
  targetValue: { type: Number, required: true },
  statLabel: { type: String, required: true },
  // The full answer key, already loaded once for the round - suggestions are
  // just a click-to-fill convenience over this, not a validation gate. Any
  // non-empty text can be submitted, matching or not.
  entries: { type: Array, default: () => [] },
  answeredPlayers: { type: Array, default: () => [] },
  allPlayers: { type: Array, default: () => [] },
  usedNames: { type: Array, default: () => [] }
})
const emit = defineEmits(['submit'])

const value = ref('')
const filteredOptions = ref([])
const showDropdown = ref(false)
const duplicateError = ref(false)

function onInput() {
  duplicateError.value = false
  const term = value.value.trim().toLowerCase()
  if (term.length >= 2) {
    filteredOptions.value = props.entries
      .map(e => e.athleteName)
      .filter(name => name.toLowerCase().includes(term))
      .slice(0, 8)
    showDropdown.value = filteredOptions.value.length > 0
  } else {
    filteredOptions.value = []
    showDropdown.value = false
  }
}

function select(option) {
  value.value = option
  showDropdown.value = false
  duplicateError.value = false
}

function submit() {
  const trimmed = value.value.trim()
  if (!trimmed) return
  const duplicate = props.usedNames.some(n => n.toLowerCase() === trimmed.toLowerCase())
  if (duplicate) {
    duplicateError.value = true
    return
  }
  emit('submit', trimmed)
  value.value = ''
  showDropdown.value = false
}
</script>
