<template>
  <div style="position:relative;">
    <input
      type="text"
      v-model="searchTerm"
      :placeholder="placeholder"
      autocomplete="off"
      @blur="onBlur"
    />
    <div v-if="showDropdown" class="guess-results" style="position:absolute; top:100%; left:0; right:0; z-index:10; margin-top:4px; max-height:220px; overflow-y:auto;">
      <button
        v-for="opt in filteredOptions"
        :key="opt"
        type="button"
        class="guess-result-row"
        @click="select(opt)"
      >{{ opt }}</button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: 'Search…' }
})
const emit = defineEmits(['update:modelValue'])

const searchTerm = ref(props.modelValue)

// Keep the visible text in sync if the confirmed value changes from outside
// (e.g. loading a different item to edit).
watch(() => props.modelValue, (val) => {
  searchTerm.value = val
})

const filteredOptions = computed(() => {
  const term = searchTerm.value.trim().toLowerCase()
  const pool = props.options
  if (!term) return pool.slice(0, 30)
  return pool.filter(o => o.toLowerCase().includes(term)).slice(0, 30)
})

// Only show suggestions while what's typed doesn't already match the
// confirmed value - i.e. while actively searching, not just displaying
// the current selection.
const showDropdown = computed(() =>
  searchTerm.value.trim().toLowerCase() !== (props.modelValue || '').trim().toLowerCase()
  && filteredOptions.value.length > 0
)

function select(opt) {
  searchTerm.value = opt
  emit('update:modelValue', opt)
}

function onBlur() {
  // A short delay so a click on a suggestion (which also blurs the input)
  // has time to register its own selection first - if nothing was picked,
  // revert the visible text to the last confirmed value rather than leaving
  // an unconfirmed, possibly-invalid value on display.
  setTimeout(() => {
    searchTerm.value = props.modelValue
  }, 150)
}
</script>
