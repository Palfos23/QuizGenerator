<template>
  <form class="lg-form" @submit.prevent="submit">
    <fieldset class="lg-field">
      <legend>{{ $t('Format') }}</legend>
      <div class="lg-options">
        <label class="lg-option" :class="{ 'is-on': format === 'HEAD_TO_HEAD' }">
          <input v-model="format" type="radio" value="HEAD_TO_HEAD" />
          <AppIcon name="swap" />
          <span>
            <strong>{{ $t('Head-to-head') }}</strong>
            <small>{{ $t('Hver dag møter du én i ligaen. Høyest poengsum vinner oppgjøret - 3 poeng for seier, 1 for uavgjort.') }}</small>
          </span>
        </label>
        <label class="lg-option" :class="{ 'is-on': format === 'POINTS' }">
          <input v-model="format" type="radio" value="POINTS" />
          <AppIcon name="layers" />
          <span>
            <strong>{{ $t('Poengliga') }}</strong>
            <small>{{ $t('Som den globale ligaen: poengene fra hver dag legges sammen, og flest poeng vinner.') }}</small>
          </span>
        </label>
      </div>
    </fieldset>

    <div class="lg-row">
      <div class="lg-field">
        <label for="lg-weeks">{{ $t('Lengde') }}</label>
        <select id="lg-weeks" v-model="weeks">
          <option v-if="format === 'POINTS'" value="">{{ $t('Ingen slutt') }}</option>
          <option v-for="w in weekOptions" :key="w" :value="String(w)">{{ $t('{0} uke¦{0} uker', [w]) }}</option>
        </select>
      </div>
      <div class="lg-field">
        <span class="lg-label">{{ $t('Start') }}</span>
        <div class="lg-segmented" role="radiogroup">
          <button type="button" role="radio" :aria-checked="start === 'TODAY'" :class="{ 'is-on': start === 'TODAY' }" @click="start = 'TODAY'">{{ $t('I dag') }}</button>
          <button type="button" role="radio" :aria-checked="start === 'TOMORROW'" :class="{ 'is-on': start === 'TOMORROW' }" @click="start = 'TOMORROW'">{{ $t('I morgen') }}</button>
        </div>
      </div>
    </div>

    <label v-if="format === 'HEAD_TO_HEAD'" class="lg-check">
      <input v-model="playoffs" type="checkbox" />
      <span>
        <strong>{{ $t('Sluttspill') }}</strong>
        <small>{{ $t('De fire beste møtes i semifinaler dagen etter serien, og vinnerne spiller finale dagen etter det. Krever minst fire spillere.') }}</small>
      </span>
    </label>

    <p class="lg-hint">{{ hint }}</p>

    <slot />
    <button type="submit" class="btn btn-primary btn-lg" :disabled="busy" :aria-busy="busy">{{ submitLabel }}</button>
  </form>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { t as _t } from '../../i18n'
import AppIcon from '../AppIcon.vue'

const props = defineProps({
  submitLabel: { type: String, required: true },
  busy: { type: Boolean, default: false }
})
const emit = defineEmits(['submit'])

const format = ref('HEAD_TO_HEAD')
const weeks = ref('2')
const start = ref('TOMORROW')
const playoffs = ref(true)

const weekOptions = computed(() => format.value === 'HEAD_TO_HEAD' ? [1, 2, 3, 4, 6, 8, 10, 12] : [1, 2, 4, 8, 12, 26, 52])

// Sensible defaults per format: head-to-head starts tomorrow so friends have a day to join before the draw
watch(format, (f) => {
  if (f === 'POINTS') {
    weeks.value = ''
    start.value = 'TODAY'
  } else {
    weeks.value = '2'
    start.value = 'TOMORROW'
  }
})

const hint = computed(() => {
  if (format.value === 'HEAD_TO_HEAD') {
    return _t('Oppgjørene trekkes når sesongen starter, blant dem som er med da. Du må svare samme dag - leverer du ikke, får du 0.')
  }
  return _t('Du må svare samme dag - leverer du ikke, får du 0 den dagen.')
})

function submit() {
  emit('submit', {
    format: format.value,
    start: start.value,
    weeks: weeks.value === '' ? null : Number(weeks.value),
    playoffs: format.value === 'HEAD_TO_HEAD' && playoffs.value
  })
}
</script>

<style scoped>
.lg-form { display: flex; flex-direction: column; gap: 18px; }
.lg-field { border: none; margin: 0; padding: 0; min-width: 0; display: flex; flex-direction: column; gap: 6px; }
.lg-field legend, .lg-field > label, .lg-label { margin-bottom: 6px; padding: 0; font-size: 0.78rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; color: var(--text-dim); }
.lg-options { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.lg-option { display: flex; gap: 12px; align-items: flex-start; padding: 14px; border: 1px solid var(--rule); border-radius: var(--radius-md); background: var(--ink-raised); cursor: pointer; }
.lg-option input { position: absolute; opacity: 0; pointer-events: none; }
.lg-option .app-icon { width: 24px; height: 24px; margin-top: 2px; color: var(--primary); }
.lg-option strong { display: block; margin-bottom: 4px; }
.lg-option small, .lg-check small { display: block; color: var(--text-dim); font-size: 0.86rem; line-height: 1.45; }
.lg-option.is-on { border-color: var(--primary); box-shadow: inset 0 0 0 1px var(--primary); }
.lg-option:has(input:focus-visible) { outline: 2px solid var(--gold); outline-offset: 2px; }
.lg-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.lg-segmented { display: grid; grid-template-columns: 1fr 1fr; border: 1px solid var(--rule); border-radius: var(--radius-md); overflow: hidden; }
.lg-segmented button { min-height: 42px; border: none; background: var(--ink-raised); color: var(--text-dim); font: inherit; font-weight: 600; cursor: pointer; }
.lg-segmented button + button { border-left: 1px solid var(--rule); }
.lg-segmented button.is-on { background: var(--primary); color: var(--on-primary); }
.lg-check { display: flex; gap: 12px; align-items: flex-start; cursor: pointer; }
.lg-check input { margin-top: 4px; width: 18px; height: 18px; flex-shrink: 0; }
.lg-hint { margin: 0; color: var(--text-dim); font-size: 0.88rem; line-height: 1.5; }
@media (max-width: 560px) {
  .lg-options, .lg-row { grid-template-columns: 1fr; }
}
</style>
