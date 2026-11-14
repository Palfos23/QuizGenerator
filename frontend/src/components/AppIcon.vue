<template>
  <svg class="app-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
    <template v-if="name === 'gift'"><rect x="3" y="8" width="18" height="4" rx="1" /><path d="M12 8v13M5 12v7a2 2 0 002 2h10a2 2 0 002-2v-7" /><path d="M7.5 8a2.5 2.5 0 010-5C11 3 12 8 12 8s1-5 4.5-5a2.5 2.5 0 010 5" /></template>
    <template v-else-if="name === 'layers'"><path d="M12 3l9 5-9 5-9-5 9-5z" /><path d="M3 13l9 5 9-5" /></template>
    <template v-else-if="name === 'dice'"><rect x="3.5" y="3.5" width="17" height="17" rx="3.5" /><circle cx="8.5" cy="8.5" r="1" fill="currentColor" stroke="none" /><circle cx="15.5" cy="15.5" r="1" fill="currentColor" stroke="none" /><circle cx="12" cy="12" r="1" fill="currentColor" stroke="none" /></template>
    <template v-else-if="name === 'star'"><path d="M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.8 6.8 19.6l1-5.8L3.5 9.7l5.9-.9L12 3.5z" /></template>
    <template v-else-if="name === 'swap'"><path d="M7 7h12l-3-3M17 17H5l3 3" /></template>
    <template v-else-if="name === 'target'"><circle cx="12" cy="12" r="8.5" /><circle cx="12" cy="12" r="4.5" /><circle cx="12" cy="12" r="1" fill="currentColor" stroke="none" /></template>
    <template v-else-if="name === 'flame'"><path d="M12 3c1 3.5 5 5.5 5 10a5 5 0 01-10 0c0-2 1-3.2 2-4.2.2 1.2.9 2 1.8 2.2C10.5 8 10.8 5 12 3z" /></template>
    <template v-else-if="name === 'cards'"><rect x="5" y="3.5" width="11" height="15" rx="2" /><path d="M8.5 21H17a2 2 0 002-2V8" /></template>
    <template v-else-if="name === 'tree'"><path d="M12 3l5 6h-3l4 5h-4l3 4H7l3-4H6l4-5H7l5-6z" /><path d="M12 18v3" /></template>
    <template v-else-if="name === 'calendar'"><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M3 10h18M8 3v4M16 3v4" /></template>
    <template v-else-if="name === 'medal'"><circle cx="12" cy="15" r="5" /><path d="M8.5 11L6 3h4l2 5 2-5h4l-2.5 8" /></template>
    <template v-else-if="name === 'crown'"><path d="M3.5 18.5h17M4 17l-1-9 5 4 4-7 4 7 5-4-1 9H4z" /></template>
    <template v-else-if="name === 'lock'"><rect x="5" y="11" width="14" height="10" rx="2" /><path d="M8 11V8a4 4 0 018 0v3" /></template>
    <template v-else-if="name === 'flask'"><path d="M9 3h6M10 3v6l-5.5 9.5A1.5 1.5 0 005.8 21h12.4a1.5 1.5 0 001.3-2.5L14 9V3" /><path d="M7.5 15h9" /></template>
    <template v-else-if="name === 'copy'"><rect x="9" y="9" width="11" height="11" rx="2" /><path d="M5 15V6a2 2 0 012-2h9" /></template>
    <template v-else-if="name === 'check'"><path d="M5 12.5l4.5 4.5L19 7.5" /></template>
  </svg>
</template>

<script setup>
// The one icon set for the app's own symbols: 24px line icons, 1.75 stroke, round ends - so they read as a family
// (emoji are left to the content a player writes or shares). Sized by the text around them (1.1em).
defineProps({ name: { type: String, required: true } })
</script>

<style>
.app-icon { width: 1.1em; height: 1.1em; vertical-align: -0.16em; flex-shrink: 0; }
</style>
