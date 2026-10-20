<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal">
      <h2>{{ isEdit ? 'Rediger spørsmål' : 'Legg til et spørsmål' }}</h2>

      <div v-if="localError" class="banner error">{{ localError }}</div>

      <div class="field">
        <label>Språk <span class="picker-hint">velg en</span></label>
        <div class="language-row">
          <button
            v-for="lang in LANGUAGES"
            :key="lang.code"
            class="language-btn"
            :class="{ active: local.language === lang.code }"
            @click="local.language = lang.code"
          >
            <span>{{ lang.flag }}</span> {{ lang.label }}
          </button>
        </div>
      </div>

      <div class="field">
        <label>Spørsmål<textarea v-model="local.questionText" placeholder="Hvilket år..."></textarea></label>
      </div>

      <div class="field">
        <label>Kategori</label>
        <select v-model="categorySelection">
          <option value="" disabled>Velg en kategori…</option>
          <option v-for="c in existingCategories" :key="c" :value="c">{{ c }}</option>
          <option value="__new__">+ Legg til ny kategori…</option>
        </select>
        <input
          v-if="categorySelection === '__new__'"
          type="text"
          v-model="local.category"
          placeholder="f.eks. Film"
          style="margin-top:8px;"
        />
      </div>

      <div class="field">
        <label>Vanskelighetsgrad <span style="text-transform:none;font-weight:400;">(1 = lettest, 10 = vanskeligst)</span>
          <div class="difficulty-slider-row">
            <input type="range" min="1" max="10" v-model.number="local.difficultyLevel" />
            <output>{{ local.difficultyLevel }}/10</output>
          </div>
        </label>
      </div>

      <div class="field">
        <label>Svar<input type="text" v-model="local.answer" placeholder="f.eks. Paris" /></label>
      </div>

      <div class="field">
        <label>Bilde-URL <span class="picker-hint">valgfritt - vises for spillerne og tas med i PDF-nedlastinger</span><input type="text" v-model="local.photoUrl" placeholder="https://…" /></label>
        <div class="photo-upload-row">
          <PhotoUploadButton @uploaded="url => local.photoUrl = url" />
          <span class="photo-upload-hint">eller lim inn en lenke over</span>
        </div>
        <div v-if="local.photoUrl" style="margin-top:10px;">
          <img :src="local.photoUrl" alt="" class="club-logo-preview" />
        </div>
      </div>

      <div v-if="allLabels.length" class="field">
        <label>Etiketter <span class="picker-hint">valgfritt - lar en quiz genereres med spørsmål etter tema</span></label>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <button
            v-for="l in allLabels"
            :key="l.id"
            type="button"
            class="team-chip"
            :class="{ active: local.labelIds.includes(l.id) }"
            @click="toggleLabel(l.id)"
          >{{ l.name }}</button>
        </div>
      </div>

      <div class="field option-card">
        <input type="checkbox" id="couldChange" v-model="local.couldChange" style="width:auto; margin-top:3px;" />
        <label for="couldChange" style="margin:0; text-transform:none; font-weight:400;">
          Svaret kan endre seg over tid
          <div style="color:var(--text-dim); font-size:0.8rem; font-weight:400; margin-top:2px;">
            f.eks. «nåværende toppscorer i Premier League» - merk dette slik at det er lett å finne og sjekke på nytt senere. La stå uten hake for faste fakta som «Norges hovedstad».
          </div>
        </label>
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
import { onMounted, reactive, ref, watch } from 'vue'
import api from '../services/api'
import { LANGUAGES } from '../constants'

const props = defineProps({
  question: { type: Object, default: null }
})
const emit = defineEmits(['close', 'saved'])

const isEdit = !!props.question
const saving = ref(false)
const localError = ref('')
const allLabels = ref([])

const local = reactive(props.question
  ? JSON.parse(JSON.stringify(props.question))
  : {
      questionText: '',
      category: '',
      difficultyLevel: 5,
      language: 'EN',
      answer: '',
      couldChange: false,
      photoUrl: '',
      labelIds: []
    })

// Categories aren't a managed entity like Grid/Tension categories - just
// whatever strings already exist on questions - so this is a combo box, not
// a strict dropdown: pick an existing one, or "+ Add new category…" to type
// one that doesn't exist yet. /api/quiz/categories is language-scoped and
// permitAll, so it works the same for a fresh admin session as for a player.
const existingCategories = ref([])
// '' = nothing chosen yet, '__new__' = typing a new one, otherwise an
// existing category string (kept in sync with local.category below).
const categorySelection = ref('')

async function loadCategories() {
  try {
    existingCategories.value = await api.getCategories(local.language)
  } catch (e) {
    // non-critical - the dropdown just falls back to "+ Add new category…"
  }
  if (local.category && existingCategories.value.includes(local.category)) {
    categorySelection.value = local.category
  } else if (local.category) {
    categorySelection.value = '__new__'
  } else {
    categorySelection.value = ''
  }
}

// Switching language changes which categories exist for it, so re-check
// whether the current pick is still valid under the new list.
watch(() => local.language, loadCategories)

// Selecting an existing category from the dropdown writes it straight into
// local.category; picking "+ Add new category…" leaves local.category alone
// so the revealed text field starts from whatever was typed before.
watch(categorySelection, (val) => {
  if (val !== '__new__') local.category = val
})

onMounted(async () => {
  try {
    allLabels.value = await api.adminListQuestionLabels()
  } catch (e) {
    // non-critical - the label picker just stays empty
  }
  await loadCategories()
})

function toggleLabel(id) {
  const idx = local.labelIds.indexOf(id)
  if (idx === -1) local.labelIds.push(id)
  else local.labelIds.splice(idx, 1)
}

async function save() {
  localError.value = ''

  if (!local.questionText.trim() || !local.category.trim() || !local.answer.trim()) {
    localError.value = 'Spørsmål, kategori og svar må fylles ut.'
    return
  }

  saving.value = true
  try {
    const saved = isEdit
      ? await api.adminUpdateQuestion(local.id, local)
      : await api.adminCreateQuestion(local)
    emit('saved', saved)
  } catch (e) {
    localError.value = e.response?.data?.message || 'Kunne ikke lagre spørsmålet.'
  } finally {
    saving.value = false
  }
}
</script>
