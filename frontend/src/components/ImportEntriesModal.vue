<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal" role="dialog" aria-modal="true" aria-label="Choose entries to import">
      <h2 style="margin-top:0;">Import {{ includedCount }} of {{ items.length }} {{ items.length === 1 ? 'entry' : 'entries' }}?</h2>
      <p class="page-subtitle">
        {{ sourceLabel }} - uncheck any you don't want added to the list below.
        <template v-if="hasSubjectMatches">A <strong style="color:var(--teal);">✓</strong> tag means that name already matches a subject.</template>
        <template v-if="pool.length"> If a name looks unmatched only because of spelling or special characters (e.g. "ø"/"ö"), search for the real subject and link it.</template>
      </p>

      <div class="candidate-list" style="max-height:380px; overflow-y:auto;">
        <div v-for="item in items" :key="item.key" style="display:flex; flex-direction:column; gap:6px; border:1px solid var(--border); border-radius:var(--radius-sm); padding:10px 12px;">
          <label class="candidate-row" style="cursor:pointer; border:none; padding:0;">
            <input type="checkbox" v-model="item.include" style="width:auto;" />
            <span style="flex:1; min-width:140px; font-weight:600;">{{ item.name }}</span>
            <span v-if="item.athlete" class="tag" style="background:rgba(61,220,151,0.15); color:var(--teal); flex-shrink:0;">✓ {{ item.athlete.name }}</span>
            <button
              v-if="pool.length"
              type="button"
              class="btn btn-secondary btn-sm"
              style="padding:2px 10px;"
              @click.stop.prevent="toggleSearch(item)"
            >{{ item.athlete ? 'Change' : 'Link subject' }}</button>
            <button
              v-if="item.athlete"
              type="button"
              class="btn btn-secondary btn-sm"
              style="padding:2px 8px;"
              title="Unlink subject"
              @click.stop.prevent="clearAthlete(item)"
            >✕</button>
            <span style="color:var(--text-dim); font-size:0.85rem; min-width:50px; text-align:right;">{{ item.value }}</span>
          </label>

          <div v-if="item.searching" @click.stop>
            <input
              type="text"
              v-model="item.searchTerm"
              placeholder="Search subjects…"
              autocomplete="off"
              autocorrect="off"
              autocapitalize="off"
              spellcheck="false"
            />
            <div v-if="resultsFor(item).length" class="guess-results">
              <button
                v-for="a in resultsFor(item)"
                :key="a.id"
                type="button"
                class="guess-result-row"
                @click="pickAthlete(item, a)"
              >{{ a.name }}</button>
            </div>
            <p v-else-if="item.searchTerm.trim()" class="page-subtitle" style="margin:6px 0 0;">No subjects match "{{ item.searchTerm.trim() }}".</p>
          </div>
        </div>
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
  sourceLabel: { type: String, default: 'Import' },
  // The full subject pool for this sport - lets the admin search out and
  // link the real subject when auto-match missed it (e.g. the imported name
  // dropped a special character like "ø"/"ö"). Empty when the caller has no
  // sport/pool to search (import always still works, just without linking).
  pool: { type: Array, default: () => [] }
})
const emit = defineEmits(['close', 'confirm'])

const items = reactive(props.rows.map((r, i) => ({
  key: i,
  originalName: r.name,
  name: r.name,
  value: r.value,
  athlete: r.athlete || null,
  include: true,
  searching: false,
  searchTerm: ''
})))
const includedCount = computed(() => items.filter(i => i.include).length)
const hasSubjectMatches = computed(() => items.some(i => i.athlete))

useEscapeKey(() => emit('close'))

function toggleSearch(item) {
  item.searching = !item.searching
  item.searchTerm = ''
}

function resultsFor(item) {
  const term = item.searchTerm.trim().toLowerCase()
  if (!term) return []
  return props.pool.filter(a => a.name.toLowerCase().includes(term)).slice(0, 8)
}

// Links this row to an existing subject and swaps in its canonical spelling -
// this is exactly the fix for a CSV name that's unmatched only because it
// dropped a special character (e.g. "Bjorn" imported vs "Bjørn" on file).
function pickAthlete(item, athlete) {
  item.athlete = athlete
  item.name = athlete.name
  item.searching = false
  item.searchTerm = ''
}

function clearAthlete(item) {
  item.athlete = null
  item.name = item.originalName
}

function confirm() {
  const selected = items.filter(i => i.include).map(({ name, value, athlete }) => ({ name, value, athlete }))
  if (!selected.length) return
  emit('confirm', selected)
}
</script>
