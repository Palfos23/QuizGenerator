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
        <input type="text" v-model="local.category" placeholder="e.g. Movies" />
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
import { reactive, ref } from 'vue'
import api from '../services/api'
import { LANGUAGES } from '../constants'

const props = defineProps({
  question: { type: Object, default: null }
})
const emit = defineEmits(['close', 'saved'])

const isEdit = !!props.question
const saving = ref(false)
const localError = ref('')

const local = reactive(props.question
  ? JSON.parse(JSON.stringify(props.question))
  : {
      questionText: '',
      category: '',
      difficultyLevel: 5,
      language: 'EN',
      answer: '',
      couldChange: false
    })

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
