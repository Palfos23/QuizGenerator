<template>
  <span class="photo-upload">
    <button type="button" class="btn btn-secondary btn-sm" :disabled="busy" @click="fileInput.click()">
      <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 16V4M7 9l5-5 5 5M4 20h16" /></svg>
      {{ busy ? 'Uploading…' : label }}
    </button>
    <input ref="fileInput" type="file" accept="image/*" style="display:none;" @change="onChosen" />
    <span v-if="error" class="photo-upload-error">{{ error }}</span>
  </span>
</template>

<script setup>
import { ref } from 'vue'
import api from '../services/api'

// "Upload a picture" next to a photo-link field: sends the file to the server, which stores it and
// hands back a link - emitted as `uploaded`, for the parent to put in the same field a pasted link
// would go in.
defineProps({ label: { type: String, default: 'Upload a picture' } })
const emit = defineEmits(['uploaded'])

const fileInput = ref(null)
const busy = ref(false)
const error = ref('')

async function onChosen(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = '' // so choosing the same file again still fires
  if (!file) return
  busy.value = true
  error.value = ''
  try {
    const { url } = await api.adminUploadImage(file)
    emit('uploaded', url)
  } catch (err) {
    error.value = err.response?.data?.message || 'Could not upload that picture.'
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.photo-upload { display: inline-flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.photo-upload-error { color: var(--coral); font-size: 0.85rem; }
</style>
