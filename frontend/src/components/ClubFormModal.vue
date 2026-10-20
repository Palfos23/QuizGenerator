<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal">
      <h2>{{ isEdit ? 'Rediger klubb' : 'Legg til en klubb' }}</h2>

      <div v-if="localError" class="banner error">{{ localError }}</div>

      <div class="field">
        <label>Navn<input type="text" v-model="local.name" placeholder="f.eks. Tottenham Hotspur eller Warner Bros." /></label>
      </div>

      <div class="field">
        <label>Kategori <span class="picker-hint">velg en</span></label>
        <div class="language-row">
          <button
            v-for="s in gridCategories.categories.value"
            :key="s"
            class="language-btn"
            :class="{ active: local.sport === s }"
            @click="local.sport = s"
          >
            {{ s }}
          </button>
        </div>
      </div>

      <div class="field">
        <label>Logo-URL <span class="picker-hint">en lenke til et bilde på nett - eller last opp et under</span><input type="text" v-model="local.logoUrl" placeholder="https://…" /></label>
        <div class="photo-upload-row">
          <PhotoUploadButton label="Last opp en logo" @uploaded="url => local.logoUrl = url" />
          <span class="photo-upload-hint">eller lim inn en lenke over</span>
        </div>
        <div v-if="local.logoUrl" style="margin-top:10px;">
          <img :src="local.logoUrl" alt="" class="club-logo-preview" @error="previewFailed = true" />
          <span v-if="previewFailed" style="color:var(--coral); font-size:0.85rem; margin-left:8px;">
            Kunne ikke laste bildet - sjekk URL-en.
          </span>
        </div>
      </div>

      <div class="field">
        <label>Farge på hintmerket <span class="picker-hint">vises på grid-rutene i stedet for standard gull</span></label>
        <div style="display:flex; align-items:center; gap:10px;">
          <input type="color" v-model="colorValue" style="width:52px; height:38px; padding:2px; cursor:pointer;" />
          <input type="text" v-model="local.color" placeholder="#d8432b (la stå tom for standard gull)" style="flex:1;" />
        </div>
      </div>

      <div class="modal-actions">
        <button class="btn btn-secondary" @click="$emit('close')">Avbryt</button>
        <button class="btn btn-primary" :disabled="saving" @click="save">
          {{ saving ? 'Lagrer…' : 'Lagre' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import PhotoUploadButton from './PhotoUploadButton.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import gridCategories from '../services/gridCategories'

const props = defineProps({
  club: { type: Object, default: null }
})
const emit = defineEmits(['close', 'saved'])

onMounted(async () => {
  await gridCategories.ensureLoaded()
  if (!isEdit && !local.sport) local.sport = gridCategories.categories.value[0] || ''
})

const isEdit = !!props.club
const saving = ref(false)
const localError = ref('')
const previewFailed = ref(false)

const local = reactive(props.club
  ? { ...props.club }
  : { name: '', sport: '', logoUrl: '', color: '' })

// The native color picker needs a valid hex value at all times, but the text field
// (and the club itself) is fine with no color set - fall back to the app's default
// gold just for what the swatch displays, without writing that default back until
// the admin actually picks something.
const colorValue = computed({
  get: () => local.color || '#d8432b',
  set: (val) => { local.color = val }
})

async function save() {
  localError.value = ''
  if (!local.name.trim()) {
    localError.value = 'Navn må fylles ut.'
    return
  }
  saving.value = true
  try {
    const saved = isEdit
      ? await api.adminUpdateClub(local.id, local)
      : await api.adminCreateClub(local)
    emit('saved', saved)
  } catch (e) {
    localError.value = e.response?.data?.message || 'Kunne ikke lagre klubben.'
  } finally {
    saving.value = false
  }
}
</script>
