<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal">
      <h2>{{ isEdit ? 'Edit subject' : 'Add a subject' }}</h2>

      <div v-if="localError" class="banner error">{{ localError }}</div>

      <div class="field">
        <label>Name</label>
        <input type="text" v-model="local.name" placeholder="e.g. Harry Kane, or The Godfather" />
      </div>

      <div class="field">
        <label>Category <span class="picker-hint">choose one</span></label>
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
        <label>{{ gridCategories.groupLabelFor(local.sport) }}</label>
        <input type="text" v-model="local.team" placeholder="e.g. Tottenham Hotspur, or Warner Bros." />
      </div>

      <div class="field">
        <label>Photo URL <span class="picker-hint">a hosted image link - shown once a player is guessed</span></label>
        <input type="text" v-model="local.photoUrl" placeholder="https://…" />
        <div v-if="local.photoUrl" style="margin-top:10px;">
          <img :src="local.photoUrl" alt="" class="club-logo-preview" />
        </div>
      </div>

      <div class="field">
        <label>
          Additional photos <span class="picker-hint">optional - lets different grids using this subject show a different picture</span>
        </label>
        <div v-for="(photo, idx) in local.additionalPhotos" :key="idx" class="additional-photo-row">
          <input type="text" v-model="photo.photoUrl" placeholder="https://…" style="flex:1;" />
          <input type="text" v-model="photo.label" placeholder="Label (optional), e.g. Alternate poster" style="flex:1;" />
          <button type="button" class="chip-remove-btn" @click="local.additionalPhotos.splice(idx, 1)">✕</button>
          <img v-if="photo.photoUrl" :src="photo.photoUrl" alt="" class="club-logo-preview" style="width:40px; height:40px;" />
        </div>
        <button type="button" class="btn btn-secondary btn-sm" style="margin-top:8px;" @click="addPhoto">
          + Add another photo
        </button>
      </div>

      <div class="field">
        <label>
          Descriptions <span class="picker-hint">optional - a quote a grid can show as the hint instead of a photo</span>
        </label>
        <div v-for="(description, idx) in local.additionalDescriptions" :key="idx" class="additional-photo-row">
          <textarea v-model="description.text" placeholder="e.g. a quote from the book" style="flex:1; min-height:60px;"></textarea>
          <input type="text" v-model="description.label" placeholder="Label (optional), e.g. Opening line" style="flex:1;" />
          <button type="button" class="chip-remove-btn" @click="local.additionalDescriptions.splice(idx, 1)">✕</button>
        </div>
        <button type="button" class="btn btn-secondary btn-sm" style="margin-top:8px;" @click="addDescription">
          + Add another description
        </button>
      </div>

      <div style="display:flex; gap:10px; justify-content:flex-end;">
        <button class="btn btn-secondary" @click="$emit('close')">Cancel</button>
        <button class="btn btn-primary" :disabled="saving" @click="save">
          {{ saving ? 'Saving…' : 'Save' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
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
    localError.value = 'Name is required.'
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
    localError.value = e.response?.data?.message || 'Could not save the subject.'
  } finally {
    saving.value = false
  }
}
</script>
