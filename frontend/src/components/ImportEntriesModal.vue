<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal" role="dialog" aria-modal="true" aria-label="Choose entries to import">
      <h2 style="margin-top:0;">Import {{ includedCount }} of {{ items.length }} {{ items.length === 1 ? 'entry' : 'entries' }}?</h2>
      <p class="page-subtitle">
        {{ sourceLabel }} - uncheck any you don't want added to the list below.
        <template v-if="hasSubjectMatches">A <strong style="color:var(--teal);">✓ Subject</strong> tag means that name already matches one.</template>
      </p>

      <div class="candidate-list" style="max-height:340px; overflow-y:auto;">
        <label v-for="item in items" :key="item.key" class="candidate-row" style="cursor:pointer;">
          <input type="checkbox" v-model="item.include" style="width:auto;" />
          <span style="flex:1; min-width:140px; font-weight:600;">{{ item.name }}</span>
          <span v-if="item.athlete" class="tag" style="background:rgba(61,220,151,0.15); color:var(--teal); flex-shrink:0;">✓ Subject</span>
          <span style="color:var(--text-dim); font-size:0.85rem; min-width:50px; text-align:right;">{{ item.value }}</span>
        </label>
      </div>

      <div style="display:flex; gap:10px; justify-content:flex-end; margin-top:20px;">
        <button class="btn btn-secondary" @click="$emit('close')">Cancel</button>
        <button class="btn btn-primary" :disabled="!includedCount" @click="confirm">
          Import {{ includedCount }} {{ includedCount === 1 ? 'entry' : 'entries' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'
import { useEscapeKey } from '../composables/useEscapeKey'

const props = defineProps({
  // [{ name, value, athlete }] - athlete is the matched AthleteDto (or null/
  // undefined if this name isn't a known subject yet) - resolved by the
  // caller before opening this modal, since matching needs an API call this
  // modal has no business making itself.
  rows: { type: Array, required: true },
  sourceLabel: { type: String, default: 'Import' }
})
const emit = defineEmits(['close', 'confirm'])

const items = reactive(props.rows.map((r, i) => ({ key: i, name: r.name, value: r.value, athlete: r.athlete || null, include: true })))
const includedCount = computed(() => items.filter(i => i.include).length)
const hasSubjectMatches = computed(() => items.some(i => i.athlete))

useEscapeKey(() => emit('close'))

function confirm() {
  const selected = items.filter(i => i.include).map(({ name, value, athlete }) => ({ name, value, athlete }))
  if (!selected.length) return
  emit('confirm', selected)
}
</script>
