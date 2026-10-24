<template>
  <div class="lap" :class="{ 'lap--small': small }">
    <input v-model="minutes" class="lap-box lap-box--min" type="text" inputmode="numeric" maxlength="2" placeholder="1" aria-label="Minutter" @input="push" />
    <span class="lap-sep">:</span>
    <input v-model="seconds" class="lap-box lap-box--sec" type="text" inputmode="numeric" maxlength="2" placeholder="23" aria-label="Sekunder" @input="push" />
    <span class="lap-sep">.</span>
    <input v-model="millis" class="lap-box lap-box--ms" type="text" inputmode="numeric" maxlength="3" placeholder="456" aria-label="Tusendeler" @input="push" @keydown.enter="emit('enter')" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { splitLap } from '../lib/lap'

// m : ss . mmm as three number boxes - a phone's number pad has no ":" or ".", so a single text field can't be typed on
// one. The value is "m:ss.mmm" once there are seconds, '' until then; the server does the real checking.
const props = defineProps({
  modelValue: { type: String, default: '' },
  small: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'enter'])

const initial = splitLap(props.modelValue)
const minutes = ref(initial.minutes)
const seconds = ref(initial.seconds)
const millis = ref(initial.millis)

function digits(v) {
  return String(v || '').replace(/\D+/g, '')
}

function push() {
  minutes.value = digits(minutes.value)
  seconds.value = digits(seconds.value)
  millis.value = digits(millis.value)
  const text = seconds.value ? `${minutes.value || '0'}:${seconds.value.padStart(2, '0')}${millis.value ? '.' + millis.value : ''}` : ''
  emit('update:modelValue', text)
}

// the parent clearing the value (after saving) empties the boxes too
watch(() => props.modelValue, v => {
  if (!v && (minutes.value || seconds.value || millis.value)) {
    minutes.value = seconds.value = millis.value = ''
  }
})
</script>

<style scoped>
.lap { display: flex; align-items: center; gap: 6px; }
.lap-box {
  text-align: center;
  font-family: var(--font-mono);
  font-size: 1.5rem;
  font-weight: 600;
  padding: 10px 4px;
  min-height: 52px;
  background: var(--field-bg);
  color: var(--text);
  border: 2px solid var(--rule);
  border-radius: 2px;
}
.lap-box--min { width: 52px; }
.lap-box--sec { width: 68px; }
.lap-box--ms { width: 88px; }
.lap-box::placeholder { color: var(--text-dim); opacity: 0.45; }
.lap-sep { font-family: var(--font-mono); font-size: 1.5rem; font-weight: 700; }
.lap--small .lap-box { font-size: 1rem; min-height: 38px; padding: 4px; }
.lap--small .lap-box--min { width: 36px; }
.lap--small .lap-box--sec { width: 46px; }
.lap--small .lap-box--ms { width: 58px; }
.lap--small .lap-sep { font-size: 1rem; }
</style>
