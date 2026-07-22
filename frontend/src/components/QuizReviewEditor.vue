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

    <div v-else class="no-print" style="margin-top:32px; display:flex; gap:12px; flex-wrap:wrap; align-items:center;">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import QuestionCard from './QuestionCard.vue'
import api from '../services/api'

const props = defineProps({
  quiz: { type: Object, required: true }, // mutated in place: { title, language, questions: [...] }
  minDifficulty: { type: Number, default: 1 },
  maxDifficulty: { type: Number, default: 10 }
})

const emit = defineEmits(['error', 'changed'])

const busyIndex = ref(-1)

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
