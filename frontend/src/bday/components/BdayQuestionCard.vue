<template>
  <div class="bq">
    <div class="bq-head">
      <span class="bq-num">{{ number }}</span>
      <div class="bq-prompt">{{ question.prompt }}</div>
    </div>

    <img v-if="question.type === 'PHOTO' && question.photoUrl" :src="question.photoUrl" alt="" class="bq-photo" />

    <div v-if="question.type === 'TEXT'">
      <input
        type="text"
        class="bq-text"
        :value="modelValue"
        maxlength="300"
        autocomplete="off"
        autocapitalize="sentences"
        enterkeyhint="done"
        placeholder="Skriv svaret ditt"
        @input="emit('update:modelValue', $event.target.value)"
      />
    </div>
    <div v-else class="bq-options">
      <button
        v-for="o in options"
        :key="o.code"
        type="button"
        class="bq-opt"
        :class="{ 'is-on': modelValue === o.code }"
        :aria-pressed="modelValue === o.code"
        @click="emit('update:modelValue', o.code)"
      >{{ o.label }}</button>
    </div>
  </div>
</template>

<script setup>
// One statement / photo / text question of the birthday quiz. The answer is an option code (PAL, ERIK, BEGGE,
// INGEN) for the first two kinds and the typed text for the last - the server scores it, nothing is decided here.
defineProps({
  question: { type: Object, required: true }, // { id, type, prompt, photoUrl }
  options: { type: Array, default: () => [] }, // [{ code, label }] - unused for text questions
  number: { type: [Number, String], required: true },
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])
</script>

<style scoped>
.bq {
  background: var(--ink-raised);
  border: 2px solid var(--rule);
  border-radius: 3px;
  padding: 14px;
  margin-bottom: 12px;
}
.bq-head { display: flex; gap: 12px; align-items: flex-start; margin-bottom: 12px; }
.bq-num {
  flex-shrink: 0;
  min-width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border: 2px solid var(--rule);
  border-radius: 2px;
  font-family: var(--font-mono);
  font-size: 0.8rem;
  font-weight: 600;
}
.bq-prompt { font-family: var(--font-display); font-weight: 600; font-size: 1.15rem; line-height: 1.25; padding-top: 2px; }
.bq-photo {
  display: block;
  width: 100%;
  max-height: 380px;
  object-fit: contain;
  background: rgb(var(--fg-rgb) / 0.05);
  border: 1.5px solid var(--line);
  border-radius: 2px;
  margin-bottom: 12px;
}
.bq-options { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.bq-opt {
  min-height: 48px; /* a comfortable thumb target */
  padding: 10px 8px;
  background: transparent;
  color: var(--text);
  border: 2px solid var(--rule);
  border-radius: 2px;
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}
.bq-opt.is-on { background: var(--text); color: var(--ink-raised); box-shadow: 3px 3px 0 var(--gold); }
.bq-text {
  width: 100%;
  min-height: 48px;
  padding: 10px 12px;
  font-size: 1rem; /* 16px+ keeps iOS from zooming the page when the field is focused */
  background: var(--field-bg);
  color: var(--text);
  border: 2px solid var(--rule);
  border-radius: 2px;
}
</style>
