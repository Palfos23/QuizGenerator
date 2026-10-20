<template>
  <div class="lang-toggle" role="group" aria-label="Language / Språk">
    <button
      v-for="opt in OPTIONS"
      :key="opt.code"
      type="button"
      class="lang-toggle-btn"
      :class="{ 'is-on': lang === opt.code }"
      :aria-pressed="lang === opt.code"
      :title="opt.title"
      @click="choose(opt.code)"
    >{{ opt.label }}</button>
  </div>
</template>

<script setup>
import { lang, setLang } from '../i18n'

const OPTIONS = [
  { code: 'no', label: 'NO', title: 'Norsk' },
  { code: 'en', label: 'EN', title: 'English' }
]

// Switching reloads the page (see i18n.js) - skip it when the chosen language is already active.
function choose(code) {
  if (code !== lang) setLang(code)
}
</script>
