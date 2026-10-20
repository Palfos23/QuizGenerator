<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal">
      <h2>{{ isEdit ? 'Rediger emne' : 'Legg til et emne' }}</h2>

      <div v-if="localError" class="banner error">{{ localError }}</div>

      <div class="field">
        <label>Navn<input type="text" v-model="local.name" placeholder="f.eks. Harry Kane eller Gudfaren" /></label>
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
        <label>{{ gridCategories.groupLabelFor(local.sport) }}<input type="text" v-model="local.team" placeholder="f.eks. Tottenham Hotspur eller Warner Bros." /></label>
      </div>

      <div class="field">
        <label>Bilde-URL <span class="picker-hint">en lenke til et bilde på nett - vises når en spiller er gjettet</span><input type="text" v-model="local.photoUrl" placeholder="https://…" /></label>
        <div class="photo-upload-row">
          <PhotoUploadButton @uploaded="url => local.photoUrl = url" />
          <span class="photo-upload-hint">eller lim inn en lenke over</span>
        </div>
        <div v-if="local.photoUrl" style="margin-top:10px;">
          <img :src="local.photoUrl" alt="" class="club-logo-preview" />
        </div>
      </div>

      <div class="field">
        <label>
          Flere bilder <span class="picker-hint">valgfritt - lar ulike grids som bruker dette emnet vise et annet bilde</span>
        </label>
        <div v-for="(photo, idx) in local.additionalPhotos" :key="idx" class="additional-photo-row">
          <input type="text" v-model="photo.photoUrl" placeholder="https://…" style="flex:1;" />
          <PhotoUploadButton label="Last opp" @uploaded="url => photo.photoUrl = url" />
          <input type="text" v-model="photo.label" placeholder="Etikett (valgfritt), f.eks. Alternativ plakat" style="flex:1;" />
          <button type="button" class="chip-remove-btn" @click="local.additionalPhotos.splice(idx, 1)">✕</button>
          <img v-if="photo.photoUrl" :src="photo.photoUrl" alt="" class="club-logo-preview" style="width:40px; height:40px;" />
        </div>
        <button type="button" class="btn btn-secondary btn-sm" style="margin-top:8px;" @click="addPhoto">
          + Legg til et bilde til
        </button>
      </div>

      <div class="field">
        <label>
          Beskrivelser <span class="picker-hint">valgfritt - et sitat et grid kan vise som hint i stedet for et bilde</span>
        </label>
        <div v-for="(description, idx) in local.additionalDescriptions" :key="idx" class="additional-photo-row">
          <textarea v-model="description.text" placeholder="f.eks. et sitat fra boken" style="flex:1; min-height:60px;"></textarea>
          <input type="text" v-model="description.label" placeholder="Etikett (valgfritt), f.eks. Åpningsreplikk" style="flex:1;" />
          <button type="button" class="chip-remove-btn" @click="local.additionalDescriptions.splice(idx, 1)">✕</button>
        </div>
        <button type="button" class="btn btn-secondary btn-sm" style="margin-top:8px;" @click="addDescription">
          + Legg til en beskrivelse til
        </button>
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
import { onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import gridCategories from '../services/gridCategories'

const props = defineProps({
  athlete: { type: Object, default: null }
})
const emit = defineEmits(['close', 'saved'])

onMounted(async () => {
  await gridCategories.ensureLoaded()
  if (!isEdit && !local.sport) local.sport = gridCategories.categories.value[0] || ''
})

const isEdit = !!props.athlete
const saving = ref(false)
const localError = ref('')

const local = reactive(props.athlete
  ? { ...props.athlete, additionalPhotos: (props.athlete.additionalPhotos || []).map(p => ({ ...p })),
      additionalDescriptions: (props.athlete.additionalDescriptions || []).map(d => ({ ...d })) }
  : { name: '', sport: '', team: '', photoUrl: '', additionalPhotos: [], additionalDescriptions: [] })

function addPhoto() {
  local.additionalPhotos.push({ photoUrl: '', label: '' })
}

function addDescription() {
  local.additionalDescriptions.push({ text: '', label: '' })
}

async function save() {
  localError.value = ''
  if (!local.name.trim()) {
    localError.value = 'Navn må fylles ut.'
    return
  }
  saving.value = true
  try {
    const payload = {
      ...local,
      additionalPhotos: local.additionalPhotos.filter(p => p.photoUrl && p.photoUrl.trim()),
      additionalDescriptions: local.additionalDescriptions.filter(d => d.text && d.text.trim())
    }
    const saved = isEdit
      ? await api.adminUpdateAthlete(local.id, payload)
      : await api.adminCreateAthlete(payload)
    emit('saved', saved)
  } catch (e) {
    localError.value = e.response?.data?.message || 'Kunne ikke lagre emnet.'
  } finally {
    saving.value = false
  }
}
</script>
