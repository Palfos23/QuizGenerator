<template>
  <div class="pitch pitch-recap">
    <PitchMarkings />
    <div v-for="(row, ri) in rows" :key="ri" class="pitch-row" :class="`pitch-row--${row.kind}`">
      <div v-for="slot in row.items" :key="slot.id ?? slot.slotIndex" class="pitch-slot">
        <div
          class="pitch-shirt"
          :class="{ solved: slot.wasFound, 'revealed-only': !slot.wasFound, goalkeeper: slot.slotIndex === 0 }"
          :style="shirtStyle(slot)"
        >
          <img
            v-if="slot.athletePhotoUrl"
            :src="slot.athletePhotoUrl"
            alt=""
            class="pitch-slot-photo"
            @error="$event.target.style.display = 'none'"
          />
          <template v-else>{{ slot.shirtNumber }}</template>
          <span v-if="slot.captain" class="pitch-shirt-captain">C</span>
        </div>
        <div class="pitch-slot-name">{{ slot.athleteName || '?' }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { readableTextColor } from '../constants'
import PitchMarkings from './PitchMarkings.vue'

// Shrunk, non-interactive copy of the play pitch for the Starting XI Battle
// results modal. Every slot is revealed - green shirt if a player found it,
// coral if it went unguessed (mirrors .grid-tile.revealed-only). The parent
// passes rows already merged with the reveal data and a `wasFound` flag per
// slot, plus resolved kit colours.
const props = defineProps({
  rows: { type: Array, default: () => [] },
  kitColor: { type: String, required: true },
  goalkeeperKitColor: { type: String, required: true }
})

function shirtStyle(slot) {
  const color = slot.slotIndex === 0 ? props.goalkeeperKitColor : props.kitColor
  return { '--kit-color': color, '--kit-text': readableTextColor(color) }
}
</script>
