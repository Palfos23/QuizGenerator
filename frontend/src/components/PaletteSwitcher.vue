<template>
  <div class="palette-switch" role="group" :aria-label="$t('Fargetema')" :style="{ '--i': activeIndex }">
    <!-- one ring, reused: it travels to whichever dot is chosen instead of one ring vanishing and another appearing -->
    <span class="palette-ring" :class="{ 'is-moving': moving }" aria-hidden="true" @animationend="moving = false" />
    <button
      v-for="p in PALETTES"
      :key="p.id"
      type="button"
      class="palette-dot"
      :aria-pressed="palette === p.id"
      :aria-label="p.name"
      :title="p.name"
      :style="{ background: `conic-gradient(${p.swatch[0]} 0 34%, ${p.swatch[1]} 0 67%, ${p.swatch[2]} 0)` }"
      @click="choose(p.id, $event)"
    />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { PALETTES, palette, setPalette } from '../services/palette'

const moving = ref(false)
const activeIndex = computed(() => Math.max(0, PALETTES.findIndex(p => p.id === palette.value)))

function choose(id, event) {
  if (id === palette.value) return
  const r = event.currentTarget.getBoundingClientRect()
  moving.value = false
  requestAnimationFrame(() => { moving.value = true })
  setPalette(id, { x: r.left + r.width / 2, y: r.top + r.height / 2 })
}
</script>
