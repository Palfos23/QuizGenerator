<template>
  <details class="translation-editor" open @toggle="onToggle">
    <summary>{{ $t('Oversettelser (norsk / engelsk)') }}</summary>
    <p class="picker-hint" style="margin:8px 0 14px;">
      {{ $t('Skriv teksten på begge språk. Spillerne ser versjonen som passer språket de har valgt - et tomt felt bruker grunnteksten.') }}
    </p>

    <LoadingSkeleton v-if="loading" />
    <div v-else-if="error" class="banner error">{{ error }}</div>

    <template v-else>
      <div v-for="f in fields" :key="f.key" class="translation-row">
        <div class="translation-label">{{ labelFor(f.key) }}</div>
        <div v-if="f.base !== undefined" class="translation-base"><span>{{ $t('Grunntekst§tr') }}:</span> {{ f.base || '-' }}</div>
        <div class="translation-inputs">
          <label>
            {{ $t('Norsk§tr') }}
            <textarea v-model="f.values.no" rows="2" maxlength="2000"></textarea>
          </label>
          <label>
            {{ $t('Engelsk§tr') }}
            <textarea v-model="f.values.en" rows="2" maxlength="2000"></textarea>
          </label>
        </div>
      </div>
      <button v-if="fields.length && entityId" class="btn btn-secondary btn-sm" :disabled="saving" @click="save">
        {{ saving ? $t('Lagrer…') : $t('Lagre oversettelser') }}
      </button>
    </template>
  </details>
</template>

<script setup>
import LoadingSkeleton from './LoadingSkeleton.vue'
import { ref, reactive, watch, computed, onMounted } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import { t as _t } from '../i18n'

// Norwegian / English versions of a board's text, saved separately from the board itself (the board's own
// fields stay the fallback). Two modes: with an entityId it edits a saved board (loaded straight away), without
// one it is a blank draft for a board that doesn't exist yet - the parent then calls flush(newId) once the board
// has been created. The parent also calls flush(id) after saving an existing board, so the board's own Save button
// saves the translations with it.
const props = defineProps({
  type: { type: String, required: true },
  entityId: { type: [Number, String], default: null },
  // Draft mode only: the fields to show (default: the type's usual ones) and their base text, if the parent has it.
  keys: { type: Array, default: null },
  bases: { type: Object, default: () => ({}) }
})

const DRAFT_KEYS = {
  GRID: ['title', 'theme'],
  LINEUP: ['title', 'competition'],
  IMPOSTER: ['title', 'description'],
  BULLSEYE: ['title', 'statLabel'],
  TENSION: ['title', 'mainCategory', 'answersCategory', 'source', 'tiebreaker'],
  FIVEOHONE: ['title', 'description'],
  FLASHBACK: [],
  SHOOTOUT: ['title', 'competition']
}

const fields = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
let loaded = false

const LABELS = {
  title: 'Tittel§tr',
  theme: 'Tema / instruksjoner§tr',
  competition: 'Turnering / sammenheng§tr',
  description: 'Beskrivelse§tr',
  statLabel: 'Resten av oppgaven§tr',
  source: 'Tilleggsinformasjon§tr',
  mainCategory: 'Hovedkategori§tr',
  answersCategory: 'Svarkategori§tr',
  tiebreaker: 'Avgjørende spørsmål§tr'
}

function labelFor(key) {
  if (key.startsWith('hints#')) return _t('Hint {0}§tr', [Number(key.split('#')[1]) + 1])
  return _t(LABELS[key] || key)
}

// Draft mode: a blank row per field, keeping anything already typed when the field list changes (hints added).
let draftRows = {}
const draftRow = key => draftRows[key] || (draftRows[key] = reactive({ no: '', en: '' }))
const draftFields = computed(() =>
  (props.keys || DRAFT_KEYS[props.type] || []).map(key => ({ key, base: props.bases[key], values: draftRow(key) }))
)
watch(draftFields, rows => { if (!props.entityId) fields.value = rows }, { immediate: true })

async function load() {
  if (!props.entityId || loading.value) return
  loading.value = true
  error.value = ''
  try {
    fields.value = await api.adminGetTranslations(props.type, props.entityId)
    loaded = true
  } catch (e) {
    error.value = _t('Kunne ikke laste oversettelsene.')
  } finally {
    loading.value = false
  }
}

function onToggle(event) {
  if (event.target.open && !loaded) load()
}

onMounted(() => { if (props.entityId && !loaded) load() })

// Saves the texts for board `id` (a new board's id, or the one being edited). Only fields the board really has
// are sent - a hint removed in the same save no longer exists. A draft sends only what was written.
async function flush(id = props.entityId) {
  if (!id) return
  const draftMode = !props.entityId
  if (!draftMode && !loaded) return
  try {
    const allowed = new Set((await api.adminGetTranslations(props.type, id)).map(f => f.key))
    const values = {}
    for (const f of fields.value) {
      if (!allowed.has(f.key)) continue
      const no = (f.values.no || '').trim()
      const en = (f.values.en || '').trim()
      if (draftMode && !no && !en) continue
      values[f.key] = { no, en }
    }
    if (Object.keys(values).length) await api.adminSaveTranslations(props.type, id, values)
  } catch (e) {
    toast.show(e.response?.data?.message || _t('Kunne ikke lagre oversettelsene.'), 'error')
  }
}

defineExpose({ flush })

async function save() {
  saving.value = true
  try {
    const values = {}
    for (const f of fields.value) values[f.key] = { no: f.values.no || '', en: f.values.en || '' }
    fields.value = await api.adminSaveTranslations(props.type, props.entityId, values)
    toast.show(_t('Oversettelsene er lagret.'))
  } catch (e) {
    toast.show(e.response?.data?.message || _t('Kunne ikke lagre oversettelsene.'), 'error')
  } finally {
    saving.value = false
  }
}

// Switching to another board (e.g. via "duplicate as new version") must not keep showing the old one's texts.
watch(() => props.entityId, id => {
  loaded = false
  if (!id) draftRows = {}
  fields.value = id ? [] : draftFields.value
  if (id) load()
})
</script>

<style scoped>
.translation-editor {
  margin: 22px 0 6px;
  padding: 14px 16px;
  border: 1.5px solid var(--rule);
  border-radius: var(--radius-sm);
  background: var(--ink-raised);
}
.translation-editor > summary {
  cursor: pointer;
  font-weight: 700;
  font-size: 0.95rem;
}
.translation-row { margin-bottom: 16px; }
.translation-label { font-weight: 700; font-size: 0.85rem; margin-bottom: 2px; }
.translation-base { font-size: 0.82rem; color: var(--text-dim); margin-bottom: 6px; }
.translation-base span { text-transform: uppercase; letter-spacing: 0.04em; font-size: 0.7rem; }
.translation-inputs { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.translation-inputs label { font-size: 0.78rem; font-weight: 600; }
.translation-inputs textarea { width: 100%; margin-top: 4px; resize: vertical; font-family: inherit; font-size: 0.9rem; }
@media (max-width: 640px) { .translation-inputs { grid-template-columns: 1fr; } }
</style>
