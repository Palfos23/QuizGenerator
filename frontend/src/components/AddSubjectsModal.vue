<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal" role="dialog" aria-modal="true" aria-label="Add new subjects">
      <h2 style="margin-top:0;">Add {{ includedCount }} new subject{{ includedCount === 1 ? '' : 's' }}?</h2>
      <p class="page-subtitle">
        These names weren't found in <strong>"{{ sport }}"</strong>. Fix any typos below, uncheck any
        you don't want to add, then add the rest as new subjects right away - no need to leave this
        page and re-import.
      </p>

      <div class="candidate-list" style="max-height:340px; overflow-y:auto;">
        <label v-for="item in items" :key="item.key" class="candidate-row" style="cursor:pointer;">
          <input type="checkbox" v-model="item.include" style="width:auto;" />
          <input
            type="text"
            v-model="item.name"
            :disabled="!item.include"
            style="flex:1; min-width:140px;"
            @click.stop
          />
          <span style="color:var(--text-dim); font-size:0.85rem; white-space:nowrap;">value: {{ item.value }}</span>
        </label>
      </div>

      <div style="display:flex; gap:10px; justify-content:flex-end; margin-top:20px;">
        <button class="btn btn-secondary" @click="$emit('close')">Cancel</button>
        <button class="btn btn-primary" :disabled="!includedCount || submitting" @click="submit">
          {{ submitting ? 'Adding…' : `Add ${includedCount} subject${includedCount === 1 ? '' : 's'}` }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import { useEscapeKey } from '../composables/useEscapeKey'

const props = defineProps({
  // [{ name, value }] - the rows an import/bulk-paste couldn't match to an
  // existing subject. `value` is carried through only so the caller can wire
  // a freshly-created subject straight into its entry list on success - this
  // modal itself never touches entries.
  names: { type: Array, required: true },
  sport: { type: String, required: true }
})
const emit = defineEmits(['close', 'added'])

const items = reactive(props.names.map((n, i) => ({ key: i, name: n.name, value: n.value, include: true })))
const includedCount = computed(() => items.filter(i => i.include).length)
const submitting = ref(false)

useEscapeKey(() => emit('close'))

async function submit() {
  const toCreate = items.filter(i => i.include && i.name.trim())
  if (!toCreate.length || submitting.value) return
  submitting.value = true
  try {
    const created = await api.adminCreateAthletesBulk(
      toCreate.map(i => ({ name: i.name.trim(), sport: props.sport }))
    )
    toast.show(`Added ${created.length} subject${created.length === 1 ? '' : 's'} to "${props.sport}".`)
    emit('added', created.map((athlete, idx) => ({ athlete, value: toCreate[idx].value })))
  } catch (e) {
    toast.show(e.response?.data?.message || 'Could not add those subjects.', 'error')
  } finally {
    submitting.value = false
  }
}
</script>
