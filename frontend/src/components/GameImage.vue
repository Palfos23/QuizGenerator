<template>
  <span class="game-image" :class="{ 'is-ready': ready }">
    <img :src="src" v-bind="$attrs" @load="ready = true" @error="onError" />
  </span>
</template>

<script setup>
// Drop-in replacement for a bare <img> on a game board: fades in once loaded
// instead of popping in mid-load, and hides itself on a broken URL exactly
// like every call site used to do by hand with @error. Pairs with
// services/imagePreload.js, which callers use to warm the browser's cache
// *before* an image is ever assigned here - between the two, a player should
// basically never see this component's own loading gap, only the fade.
import { ref, watch } from 'vue'

defineOptions({ inheritAttrs: false })
const props = defineProps({
  src: { type: String, default: '' }
})

const ready = ref(false)

// A reused <img> element (e.g. a crest that swaps to a new URL on an
// already-mounted board) needs to re-hide until the new image is actually in.
watch(() => props.src, () => { ready.value = false })

function onError(event) {
  event.target.style.display = 'none'
}
</script>

<style scoped>
/* display:contents makes this wrapper invisible to layout and positioning -
   the <img> inside behaves exactly as if it were still a direct child of
   whatever this replaced. Several call sites (grid tiles) absolutely
   position the image relative to their own tile; a wrapper with any box of
   its own would break that. */
.game-image {
  display: contents;
}
.game-image img {
  opacity: 0;
  transition: opacity 0.2s ease;
}
.game-image.is-ready img {
  opacity: 1;
}
</style>
