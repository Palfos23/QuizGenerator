<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal">
      <h2>{{ isEdit ? 'Edit question' : 'Add a question' }}</h2>

      <div v-if="localError" class="banner error">{{ localError }}</div>

      <div class="field">
        <label>Language <span class="picker-hint">choose one</span></label>
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
        <label>Question</label>
        <textarea v-model="local.questionText" placeholder="What year did..."></textarea>
      </div>

      <div class="field">
        <label>Category</label>
        <select v-model="categorySelection">
          <option value="" disabled>Select a category…</option>
          <option v-for="c in existingCategories" :key="c" :value="c">{{ c }}</option>
          <option value="__new__">+ Add new category…</option>
        </select>
        <input
          v-if="categorySelection === '__new__'"
          type="text"
          v-model="local.category"
          placeholder="e.g. Movies"
          style="margin-top:8px;"
        />
      </div>

      <div class="field">
        <label>Difficulty <span style="text-transform:none;font-weight:400;">(1 = easiest, 10 = hardest)</span></label>
        <div class="difficulty-slider-row">
          <input type="range" min="1" max="10" v-model.number="local.difficultyLevel" />
          <output>{{ local.difficultyLevel }}/10</output>
        </div>
      </div>

      <div class="field">
        <label>Answer</label>
        <input type="text" v-model="local.answer" placeholder="e.g. Paris" />
      </div>

      <div class="field">
        <label>Photo URL <span class="picker-hint">optional - shown to players and included in PDF downloads</span></label>
        <input type="text" v-model="local.photoUrl" placeholder="https://…" />
      </div>

      <div v-if="allLabels.length" class="field">
        <label>Labels <span class="picker-hint">optional - lets a quiz be generated to pull questions by theme</span></label>
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

      <div class="field" style="display:flex; align-items:flex-start; gap:8px;">
        <input type="checkbox" id="couldChange" v-model="local.couldChange" style="width:auto; margin-top:3px;" />
        <label for="couldChange" style="margin:0; text-transform:none; font-weight:400;">
          Answer could change over time
          <div style="color:var(--text-dim); font-size:0.8rem; font-weight:400; margin-top:2px;">
            e.g. "current Premier League top scorer" - flag this so it's easy to find and recheck later.
            Leave unchecked for stable facts like "capital of Norway".
          </div>
        </label>
      </div>

      <div style="display:flex; gap:10px; justify-content:flex-end; margin-top:12px;">
        <button class="btn btn-secondary" @click="$emit('close')">Cancel</button>
        <button class="btn btn-primary" :disabled="saving" @click="save">
          {{ saving ? 'Saving…' : 'Save' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
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
    localError.value = 'Question, category and answer are all required.'
    return
  }

  saving.value = true
  try {
    const saved = isEdit
      ? await api.adminUpdateQuestion(local.id, local)
      : await api.adminCreateQuestion(local)
    emit('saved', saved)
  } catch (e) {
    localError.value = e.response?.data?.message || 'Could not save the question.'
  } finally {
    saving.value = false
  }
}
</script>
