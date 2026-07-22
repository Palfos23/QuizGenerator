<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal">
      <h2>{{ isEdit ? 'Edit athlete' : 'Add an athlete' }}</h2>

      <div v-if="localError" class="banner error">{{ localError }}</div>

      <div class="field">
        <label>Name</label>
        <input type="text" v-model="local.name" placeholder="e.g. Harry Kane" />
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
        <label>Team</label>
        <input type="text" v-model="local.team" placeholder="e.g. Tottenham Hotspur" />
      </div>

      <div class="field">
        <label>Photo URL <span class="picker-hint">a hosted image link - shown once a player is guessed</span></label>
        <input type="text" v-model="local.photoUrl" placeholder="https://…" />
        <div v-if="local.photoUrl" style="margin-top:10px;">
          <img :src="local.photoUrl" alt="" class="club-logo-preview" />
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
  athlete: { type: Object, default: null }
})
const emit = defineEmits(['close', 'saved'])

const isEdit = !!props.athlete
const saving = ref(false)
const localError = ref('')

const local = reactive(props.athlete
  ? { ...props.athlete }
  : { name: '', sport: 'FOOTBALL', team: '', photoUrl: '' })

async function save() {
  localError.value = ''
  if (!local.name.trim()) {
    localError.value = 'Name is required.'
    return
  }
  saving.value = true
  try {
    const saved = isEdit
      ? await api.adminUpdateAthlete(local.id, local)
      : await api.adminCreateAthlete(local)
    emit('saved', saved)
  } catch (e) {
    localError.value = e.response?.data?.message || 'Could not save the athlete.'
  } finally {
    saving.value = false
  }
}
</script>
