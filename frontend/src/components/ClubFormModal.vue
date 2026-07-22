<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal">
      <h2>{{ isEdit ? 'Edit club' : 'Add a club' }}</h2>

      <div v-if="localError" class="banner error">{{ localError }}</div>

      <div class="field">
        <label>Name</label>
        <input type="text" v-model="local.name" placeholder="e.g. Tottenham Hotspur" />
      </div>

      <div class="field">
        <label>Sport <span class="picker-hint">choose one</span></label>
        <div class="language-row">
          <button
            v-for="s in SPORTS"
            :key="s.code"
            class="language-btn"
            :class="{ active: local.sport === s.code }"
            @click="local.sport = s.code"
          >
            {{ s.label }}
          </button>
        </div>
      </div>

      <div class="field">
        <label>Logo URL <span class="picker-hint">a hosted image link, not a file upload</span></label>
        <input type="text" v-model="local.logoUrl" placeholder="https://…" />
        <div v-if="local.logoUrl" style="margin-top:10px;">
          <img :src="local.logoUrl" alt="" class="club-logo-preview" @error="previewFailed = true" />
          <span v-if="previewFailed" style="color:var(--coral); font-size:0.85rem; margin-left:8px;">
            Couldn't load that image - double-check the URL.
          </span>
        </div>
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
import { reactive, ref } from 'vue'
import api from '../services/api'
import { SPORTS } from '../constants'

const props = defineProps({
  club: { type: Object, default: null }
})
const emit = defineEmits(['close', 'saved'])

const isEdit = !!props.club
const saving = ref(false)
const localError = ref('')
const previewFailed = ref(false)

const local = reactive(props.club
  ? { ...props.club }
  : { name: '', sport: 'FOOTBALL', logoUrl: '' })

async function save() {
  localError.value = ''
  if (!local.name.trim()) {
    localError.value = 'Name is required.'
    return
  }
  saving.value = true
  try {
    const saved = isEdit
      ? await api.adminUpdateClub(local.id, local)
      : await api.adminCreateClub(local)
    emit('saved', saved)
  } catch (e) {
    localError.value = e.response?.data?.message || 'Could not save the club.'
  } finally {
    saving.value = false
  }
}
</script>
