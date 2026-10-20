<template>
  <div>
    <div
      ref="viewport"
      class="bday-map"
      :class="{ 'is-dragging': dragging }"
      @wheel.prevent="onWheel"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      @pointercancel="onPointerCancel"
    >
      <!-- Everything inside is scaled/moved together, so the pin stays glued to the spot it was placed. -->
      <div ref="inner" class="bday-map-inner" :style="{ transform: `translate(${tx}px, ${ty}px) scale(${zoom})` }">
        <img :src="imageUrl" alt="Kart over Norge" draggable="false" />
        <div
          v-if="modelValue"
          class="bday-map-pin"
          :style="{ left: (modelValue.x * 100) + '%', top: (modelValue.y * 100) + '%', transform: `translate(-50%, -50%) scale(${1 / zoom})` }"
        ></div>
      </div>

      <div class="bday-map-controls" @pointerdown.stop @wheel.stop>
        <button type="button" aria-label="Zoom inn" @click="zoomBy(1.6)">+</button>
        <button type="button" aria-label="Zoom ut" :disabled="zoom <= 1" @click="zoomBy(1 / 1.6)">−</button>
        <button type="button" aria-label="Tilbakestill kartet" :disabled="zoom <= 1" class="reset" @click="resetView">1:1</button>
      </div>
    </div>
    <p class="page-subtitle" style="margin-top:6px;">
      {{ modelValue ? 'Pinnen er satt - trykk igjen for å flytte den.' : 'Trykk på kartet for å sette en pinne.' }}
      Zoom med to fingre, mushjulet eller knappene, og dra for å flytte kartet.
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// A zoomable, draggable map. The answer is still reported as {x, y} fractions (0..1) of the whole image,
// exactly as before - zooming and panning only change how much of the map you see while placing the pin,
// so the scoring on the server doesn't care (and the pin can be placed much more precisely when zoomed in).
defineProps({
  imageUrl: { type: String, required: true },
  modelValue: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue'])

const MIN_ZOOM = 1
const MAX_ZOOM = 10
const TAP_SLOP_PX = 6 // a press that moves less than this is a tap (place the pin), more is a drag (move the map)

const viewport = ref(null)
const inner = ref(null)
const zoom = ref(1)
const tx = ref(0)
const ty = ref(0)
const dragging = ref(false)

const pointers = new Map() // pointerId -> { x, y }
let tapStart = null // { x, y } of a single-finger press, until it turns into a drag
let pinchStart = null // { distance, zoom } while two fingers are down

function clampView() {
  const el = viewport.value
  if (!el) return
  const w = el.clientWidth
  const h = el.clientHeight
  tx.value = Math.min(0, Math.max(w - w * zoom.value, tx.value))
  ty.value = Math.min(0, Math.max(h - h * zoom.value, ty.value))
}

// Zooms to `next`, keeping the map point under (cx, cy) - viewport coordinates - where it is.
function zoomAround(next, cx, cy) {
  const clamped = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, next))
  const ratio = clamped / zoom.value
  tx.value = cx - (cx - tx.value) * ratio
  ty.value = cy - (cy - ty.value) * ratio
  zoom.value = clamped
  clampView()
}

function zoomBy(factor) {
  const el = viewport.value
  zoomAround(zoom.value * factor, el.clientWidth / 2, el.clientHeight / 2)
}

function resetView() {
  zoom.value = 1
  tx.value = 0
  ty.value = 0
}

function localPoint(event) {
  const rect = viewport.value.getBoundingClientRect()
  return { x: event.clientX - rect.left, y: event.clientY - rect.top }
}

function onWheel(event) {
  const p = localPoint(event)
  zoomAround(zoom.value * Math.pow(1.0015, -event.deltaY), p.x, p.y)
}

function onPointerDown(event) {
  if (event.pointerType === 'mouse' && event.button !== 0) return
  viewport.value.setPointerCapture(event.pointerId)
  pointers.set(event.pointerId, { x: event.clientX, y: event.clientY })
  if (pointers.size === 1) {
    tapStart = { x: event.clientX, y: event.clientY }
  } else if (pointers.size === 2) {
    tapStart = null // a second finger means pinch, never a tap
    const [a, b] = [...pointers.values()]
    pinchStart = { distance: Math.hypot(a.x - b.x, a.y - b.y) || 1, zoom: zoom.value }
  }
}

function onPointerMove(event) {
  const prev = pointers.get(event.pointerId)
  if (!prev) return

  if (pointers.size === 2 && pinchStart) {
    pointers.set(event.pointerId, { x: event.clientX, y: event.clientY })
    const [a, b] = [...pointers.values()]
    const rect = viewport.value.getBoundingClientRect()
    const midX = (a.x + b.x) / 2 - rect.left
    const midY = (a.y + b.y) / 2 - rect.top
    zoomAround(pinchStart.zoom * (Math.hypot(a.x - b.x, a.y - b.y) / pinchStart.distance), midX, midY)
    return
  }

  if (pointers.size === 1) {
    if (tapStart && Math.hypot(event.clientX - tapStart.x, event.clientY - tapStart.y) > TAP_SLOP_PX) {
      tapStart = null
      dragging.value = true
    }
    if (dragging.value) {
      tx.value += event.clientX - prev.x
      ty.value += event.clientY - prev.y
      clampView()
    }
  }
  pointers.set(event.pointerId, { x: event.clientX, y: event.clientY })
}

function onPointerUp(event) {
  const wasTap = tapStart !== null && pointers.size === 1
  pointers.delete(event.pointerId)
  if (pointers.size < 2) pinchStart = null
  if (pointers.size === 0) dragging.value = false

  if (wasTap) {
    // The inner box's on-screen rectangle already includes the current zoom and pan, so the click's
    // position inside it is the position on the whole map regardless of what part is showing.
    const rect = inner.value.getBoundingClientRect()
    const x = (event.clientX - rect.left) / rect.width
    const y = (event.clientY - rect.top) / rect.height
    if (x >= 0 && x <= 1 && y >= 0 && y <= 1) emit('update:modelValue', { x, y })
  }
  tapStart = null
}

function onPointerCancel(event) {
  pointers.delete(event.pointerId)
  pinchStart = null
  tapStart = null
  if (pointers.size === 0) dragging.value = false
}
</script>

<style scoped>
.bday-map {
  position: relative;
  overflow: hidden;
  max-width: 100%;
  border: 1px solid var(--border);
  border-radius: 2px;
  cursor: crosshair;
  touch-action: none; /* the page must not scroll or zoom while the map is being pinched / dragged */
  user-select: none;
  background: rgba(27, 26, 23, 0.04);
}
.bday-map.is-dragging { cursor: grabbing; }

.bday-map-inner { position: relative; transform-origin: 0 0; will-change: transform; }
.bday-map-inner img { display: block; width: 100%; user-select: none; pointer-events: none; }

.bday-map-pin {
  position: absolute;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--coral);
  border: 2px solid #fff;
  pointer-events: none;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.4);
}

.bday-map-controls {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.bday-map-controls button {
  width: 36px;
  height: 36px;
  border-radius: 2px;
  border: 1px solid rgba(27, 26, 23, 0.3);
  background: rgba(243, 237, 225, 0.96);
  color: var(--text);
  font-size: 1.2rem;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
}
.bday-map-controls button.reset { font-size: 0.72rem; }
.bday-map-controls button:disabled { opacity: 0.4; cursor: default; }
</style>
