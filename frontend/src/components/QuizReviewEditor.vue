<template>
  <div>
    <div class="card-stack">
      <QuestionCard
        v-for="(q, idx) in quiz.questions"
        :key="q.id ?? idx"
        :question="q"
        :index="idx"
        editable
        :busy="busyIndex === idx"
        :is-first="idx === 0"
        :is-last="idx === quiz.questions.length - 1"
        @discard="discardAndReplace(idx)"
        @remove="removeQuestion(idx)"
        @move-up="moveQuestion(idx, -1)"
        @move-down="moveQuestion(idx, 1)"
      />
    </div>

    <div v-if="!quiz.questions.length" class="empty-state">
      <slot name="empty">Every question was removed.</slot>
    </div>

    <div class="add-more-questions no-print">
      <span style="font-weight:600; font-size:0.9rem;">Add more questions</span>
      <select v-model="addCategory" style="flex:1; min-width:140px;">
        <option value="" disabled>Choose a category…</option>
        <option v-for="cat in availableCategories" :key="cat" :value="cat">{{ cat }}</option>
      </select>
      <input type="number" min="1" max="20" v-model.number="addCount" style="width:70px;" />
      <button class="btn btn-secondary btn-sm" :disabled="!addCategory || adding" @click="addMore">
        {{ adding ? 'Adding…' : '+ Add' }}
      </button>
    </div>

    <div v-if="quiz.questions.length" class="no-print" style="margin-top:20px; display:flex; gap:12px; flex-wrap:wrap; align-items:center;">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import QuestionCard from './QuestionCard.vue'
import api from '../services/api'

const props = defineProps({
  quiz: { type: Object, required: true }, // mutated in place: { title, language, questions: [...] }
  minDifficulty: { type: Number, default: 1 },
  maxDifficulty: { type: Number, default: 10 }
})

const emit = defineEmits(['error', 'changed'])

const busyIndex = ref(-1)
const availableCategories = ref([])
const addCategory = ref('')
const addCount = ref(3)
const adding = ref(false)

onMounted(loadCategories)
watch(() => props.quiz.language, loadCategories)

async function loadCategories() {
  try {
    availableCategories.value = await api.getCategories(props.quiz.language)
  } catch (e) {
    // the add-more picker just stays empty - not critical enough to surface an error banner
  }
}

async function addMore() {
  emit('error', '')
  adding.value = true
  try {
    const added = await api.addQuestions({
      category: addCategory.value,
      language: props.quiz.language,
      minDifficulty: props.minDifficulty,
      maxDifficulty: props.maxDifficulty,
      count: addCount.value,
      excludeIds: props.quiz.questions.map(q => q.id)
    })
    if (!added.length) {
      emit('error', `No more unused "${addCategory.value}" questions are available in that language/difficulty range.`)
      return
    }
    props.quiz.questions.push(...added)
    if (added.length < addCount.value) {
      emit('error', `Only found ${added.length} more "${addCategory.value}" question(s) - added what was available.`)
    }
    emit('changed')
  } catch (e) {
    emit('error', e.response?.data?.message || 'Could not add more questions.')
  } finally {
    adding.value = false
  }
}

async function discardAndReplace(idx) {
  emit('error', '')
  busyIndex.value = idx
  try {
    const discarded = props.quiz.questions[idx]
    const replacement = await api.replaceQuestion({
      category: discarded.category,
      language: props.quiz.language,
      minDifficulty: props.minDifficulty,
      maxDifficulty: props.maxDifficulty,
      excludeIds: props.quiz.questions.map(q => q.id)
    })
    props.quiz.questions.splice(idx, 1, replacement)
    emit('changed')
  } catch (e) {
    emit('error', e.response?.data?.message || 'No replacement question was available.')
  } finally {
    busyIndex.value = -1
  }
}

function removeQuestion(idx) {
  props.quiz.questions.splice(idx, 1)
  emit('changed')
}

function moveQuestion(idx, direction) {
  const target = idx + direction
  if (target < 0 || target >= props.quiz.questions.length) return
  const questions = props.quiz.questions
  const [moved] = questions.splice(idx, 1)
  questions.splice(target, 0, moved)
  emit('changed')
}
</script>
