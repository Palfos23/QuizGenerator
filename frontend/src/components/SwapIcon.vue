<template>
  <!-- two icons stacked: the outgoing one shrinks and blurs away while the incoming one sharpens in, overlapping -->
  <span class="swap-icon" :data-on="on" aria-hidden="true">
    <span class="swap-a"><AppIcon :name="a" /></span>
    <span class="swap-b"><AppIcon :name="b" /></span>
  </span>
</template>

<script setup>
import AppIcon from './AppIcon.vue'
defineProps({ a: { type: String, required: true }, b: { type: String, required: true }, on: { type: Boolean, default: false } })
</script>
