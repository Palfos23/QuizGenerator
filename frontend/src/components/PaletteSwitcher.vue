<template>
  <div ref="rootEl" class="palette-switch" role="group" :aria-label="$t('Fargetema')">
    <!-- one ring, reused: it travels to whichever dot is chosen instead of one ring vanishing and another appearing -->
    <span class="palette-ring" :class="{ 'is-moving': moving, 'is-placed': placed }" :style="ringStyle" aria-hidden="true" @animationend="moving = false" />
    <button
      v-for="p in PALETTES"
      :key="p.id"
      type="button"
      class="palette-dot"
      :aria-pressed="palette === p.id"
      :aria-label="p.name"
      :title="p.name"
      :style="{ background: `conic-gradient(${p.swatch[0]} 0 25%, ${p.swatch[1]} 0 50%, ${p.swatch[2]} 0 75%, ${p.swatch[3]} 0)` }"
      @click="choose(p.id, $event)"
    />
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { PALETTES, palette, setPalette } from '../services/palette'

const rootEl = ref(null)
const moving = ref(false)
const ringStyle = ref({})
const placed = ref(false) // no slide on first paint: the ring only travels after it has been put on the chosen dot

// The ring sits over the chosen dot. Its position is measured rather than computed, so the dots can wrap onto several rows.
function placeRing() {
  const dot = rootEl.value?.querySelector('.palette-dot[aria-pressed="true"]')
  if (!dot) return
  ringStyle.value = { translate: `${dot.offsetLeft - 4}px ${dot.offsetTop - 4}px` }
  if (!placed.value) requestAnimationFrame(() => requestAnimationFrame(() => { placed.value = true }))
}

function choose(id, event) {
  if (id === palette.value) return
  const r = event.currentTarget.getBoundingClientRect()
  moving.value = false
  requestAnimationFrame(() => { moving.value = true })
  setPalette(id, { x: r.left + r.width / 2, y: r.top + r.height / 2 })
}

let observer = null
onMounted(() => {
  placeRing()
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(() => placeRing())
    observer.observe(rootEl.value)
  }
})
onUnmounted(() => observer?.disconnect())
watch(palette, () => nextTick(placeRing))
</script>
