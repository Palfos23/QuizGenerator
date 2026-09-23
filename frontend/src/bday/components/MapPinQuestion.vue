<template>
  <div>
    <div
      style="position:relative; display:inline-block; max-width:100%; cursor:crosshair; border:1px solid var(--border); border-radius:var(--radius-sm); overflow:hidden;"
      @click="onClick"
    >
      <img :src="imageUrl" alt="Map" style="max-width:100%; display:block; user-select:none;" draggable="false" />
      <div
        v-if="modelValue"
        class="bday-map-pin"
        :style="{ left: (modelValue.x * 100) + '%', top: (modelValue.y * 100) + '%' }"
      ></div>
    </div>
    <p v-if="modelValue" class="page-subtitle" style="margin-top:6px;">Pin placed - click again to move it.</p>
  </div>
</template>

<script setup>
// Emits {x, y} as fractions (0..1) of the image's *rendered* box - matches
// exactly how BdayQuestionCatalog's correctX/correctY are calibrated
// server-side, so this stays correct regardless of screen size/zoom.
defineProps({
  imageUrl: { type: String, required: true },
  modelValue: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue'])

function onClick(event) {
  const rect = event.currentTarget.getBoundingClientRect()
  const x = (event.clientX - rect.left) / rect.width
  const y = (event.clientY - rect.top) / rect.height
  emit('update:modelValue', { x, y })
}
</script>

<style scoped>
.bday-map-pin {
  position: absolute;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--coral);
  border: 2px solid #fff;
  transform: translate(-50%, -50%);
  pointer-events: none;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.4);
}
</style>
