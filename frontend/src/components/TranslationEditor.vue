<template>
  <details class="translation-editor" @toggle="onToggle">
    <summary>{{ $t('Oversettelser (norsk / engelsk)') }}</summary>
    <p class="picker-hint" style="margin:8px 0 14px;">
      {{ $t('Valgfritt - la et felt stå tomt for å bruke teksten over. Spillerne ser versjonen som passer språket de har valgt.') }}
    </p>

    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>
    <div v-else-if="error" class="banner error">{{ error }}</div>

    <template v-else>
      <div v-for="f in fields" :key="f.key" class="translation-row">
        <div class="translation-label">{{ labelFor(f.key) }}</div>
        <div class="translation-base"><span>{{ $t('Grunntekst§tr') }}:</span> {{ f.base || '-' }}</div>
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
      <button v-if="fields.length" class="btn btn-secondary btn-sm" :disabled="saving" @click="save">
        {{ saving ? $t('Lagrer…') : $t('Lagre oversettelser') }}
      </button>
    </template>
  </details>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import { t as _t } from '../i18n'

// Norwegian / English versions of a game board's text, saved separately from the board itself (the board's own
// fields stay the fallback). The board must already exist - it's addressed by (type, id) - so the parent shows
// this only while editing a saved board. Loaded the first time the panel is opened.
const props = defineProps({
  type: { type: String, required: true },
  entityId: { type: [Number, String], required: true }
})

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
  tiebreaker: 'Avgjørende spørsmål§tr'
}

function labelFor(key) {
  if (key.startsWith('hints#')) return _t('Hint {0}§tr', [Number(key.split('#')[1]) + 1])
  return _t(LABELS[key] || key)
}

async function load() {
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
watch(() => props.entityId, () => { loaded = false; fields.value = [] })
</script>

<style scoped>
.translation-editor {
  margin: 22px 0 6px;
  padding: 14px 16px;
  border: 1.5px solid var(--rule);
  border-radius: 3px;
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
