<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal" role="dialog" aria-modal="true" :aria-label="$t('Legg til nye emner')">
      <h2 style="margin-top:0;">{{ $t('Legg til') }} {{ includedCount }} {{ includedCount === 1 ? $t('nytt emne') : $t('nye emner') }}?</h2>
      <p class="page-subtitle">
        {{ $t('Disse navnene ble ikke funnet i') }} <strong>"{{ sport }}"</strong>{{ $t('. Rett eventuelle skrivefeil under, fjern avhukingen for de du ikke vil legge til, og legg til resten som nye emner med en gang - du slipper å forlate siden og importere på nytt.') }}
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
          <span style="color:var(--text-dim); font-size:0.85rem; white-space:nowrap;">{{ $t('verdi:') }} {{ item.value }}</span>
        </label>
      </div>

      <div class="modal-actions">
        <button class="btn btn-secondary" @click="$emit('close')">{{ $t('Avbryt') }}</button>
        <button class="btn btn-primary" :disabled="!includedCount || submitting" @click="submit">
          {{ submitting ? $t('Legger til…') : $t('Legg til {0} emne¦Legg til {0} emner', [includedCount]) }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
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
    toast.show(_t('La til {0} emne i "{1}".¦La til {0} emner i "{1}".', [created.length, props.sport]))
    emit('added', created.map((athlete, idx) => ({ athlete, value: toCreate[idx].value })))
  } catch (e) {
    toast.show(e.response?.data?.message || _t('Kunne ikke legge til emnene.'), 'error')
  } finally {
    submitting.value = false
  }
}
</script>
