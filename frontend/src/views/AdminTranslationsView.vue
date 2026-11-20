<template>
  <div>
    <h1>{{ $t('Oversettelser') }}</h1>
    <p class="page-subtitle">
      {{ $t('Eksporter all spilltekst (titler, temaer, hint ...) som en fil, få den oversatt til norsk og engelsk utenfor appen, og importer den tilbake. Eksisterende oversettelser beholdes.') }}
    </p>

    <div class="card translations-card">
      <h2>{{ $t('1. Eksporter') }}</h2>
      <p class="picker-hint">
        {{ $t('Filen inneholder hvert brett med grunnteksten og eventuelle oversettelser som allerede finnes.') }}
      </p>
      <label class="translations-check">
        <input type="checkbox" v-model="onlyMissing" />
        {{ $t('Bare tekster som mangler en oversettelse') }}
      </label>
      <button class="btn btn-primary" :disabled="exporting" @click="doExport">
        {{ exporting ? $t('Henter…') : $t('Last ned eksportfil') }}
      </button>
      <p v-if="exportInfo" class="picker-hint" style="margin-top:10px;">{{ exportInfo }}</p>
    </div>

    <div class="card translations-card">
      <h2>{{ $t('2. Importer') }}</h2>
      <p class="picker-hint">
        {{ $t('Last opp filen med oversettelsene. Tomme felt ignoreres.') }}
      </p>
      <label class="translations-check">
        <input type="checkbox" v-model="overwrite" />
        {{ $t('Overskriv oversettelser som allerede finnes') }}
      </label>
      <input ref="fileInput" type="file" accept="application/json,.json" @change="onFile" />
      <div v-if="error" class="banner error" style="margin-top:12px;">{{ error }}</div>
      <div v-if="result" class="banner success" style="margin-top:12px;">
        {{ $t('{0} tekster lagret, {1} hoppet over fordi de fantes fra før.', [result.saved, result.skippedExisting]) }}
        <span v-if="result.unknownBoards || result.unknownFields">
          {{ $t('{0} ukjente brett og {1} ukjente felt ble ignorert.', [result.unknownBoards, result.unknownFields]) }}
        </span>
        <ul v-if="result.problems && result.problems.length" style="margin:8px 0 0 18px;">
          <li v-for="p in result.problems" :key="p">{{ p }}</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import { t as _t } from '../i18n'

const onlyMissing = ref(true)
const overwrite = ref(false)
const exporting = ref(false)
const exportInfo = ref('')
const error = ref('')
const result = ref(null)
const fileInput = ref(null)

async function doExport() {
  exporting.value = true
  exportInfo.value = ''
  try {
    const items = await api.adminExportTranslations(onlyMissing.value)
    const fields = items.reduce((n, i) => n + i.fields.length, 0)
    const blob = new Blob([JSON.stringify(items, null, 1)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'translations-export.json'
    a.click()
    URL.revokeObjectURL(url)
    exportInfo.value = _t('{0} brett og {1} tekster eksportert.', [items.length, fields])
  } catch (e) {
    toast.show(e.response?.data?.message || _t('Kunne ikke eksportere.'), 'error')
  } finally {
    exporting.value = false
  }
}

// The export is [{ type, id, context, fields: [{ key, base, no, en }] }]; the import wants
// [{ type, id, fields: { key: { no, en } } }] - this accepts the export's shape directly.
function toImport(items) {
  return items.map(i => ({
    type: i.type,
    id: i.id,
    fields: Array.isArray(i.fields)
      ? Object.fromEntries(i.fields.map(f => [f.key, { no: f.no || '', en: f.en || '' }]))
      : i.fields
  }))
}

async function onFile(event) {
  const file = event.target.files[0]
  error.value = ''
  result.value = null
  if (!file) return
  try {
    const items = JSON.parse(await file.text())
    if (!Array.isArray(items)) throw new Error('not a list')
    result.value = await api.adminImportTranslations(toImport(items), overwrite.value)
  } catch (e) {
    error.value = e.response?.data?.message || _t('Filen kunne ikke leses eller importeres.')
  } finally {
    if (fileInput.value) fileInput.value.value = ''
  }
}
</script>

<style scoped>
.translations-card { margin-bottom: 18px; padding: 20px; }
.translations-card h2 { margin: 0 0 6px; }
.translations-check { display: flex; align-items: center; gap: 8px; margin: 12px 0; font-size: 0.92rem; }
</style>
