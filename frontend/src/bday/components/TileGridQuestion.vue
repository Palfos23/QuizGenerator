<template>
  <div style="display:flex; flex-direction:column; gap:8px;">
    <div
      v-for="tile in tiles"
      :key="tile.id"
      class="candidate-row"
      style="display:flex; align-items:center; gap:10px;"
    >
      <span style="flex:1; font-weight:600;">{{ tile.label }}</span>
      <button
        type="button"
        class="btn btn-sm"
        :class="selection[tile.id] === 'A' ? 'btn-primary' : 'btn-secondary'"
        @click="choose(tile.id, 'A')"
      >{{ personALabel }}</button>
      <button
        type="button"
        class="btn btn-sm"
        :class="selection[tile.id] === 'B' ? 'btn-primary' : 'btn-secondary'"
        @click="choose(tile.id, 'B')"
      >{{ personBLabel }}</button>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'

const props = defineProps({
  tiles: { type: Array, required: true }, // [{id, label}]
  personALabel: { type: String, required: true },
  personBLabel: { type: String, required: true },
  modelValue: { type: Object, default: () => ({}) } // {tileId: 'A'|'B'}
})
const emit = defineEmits(['update:modelValue'])

const selection = reactive({ ...(props.modelValue || {}) })

function choose(tileId, owner) {
  selection[tileId] = owner
  emit('update:modelValue', { ...selection })
}
</script>
