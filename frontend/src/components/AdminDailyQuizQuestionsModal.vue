<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal dq-modal dq-modal--wide" role="dialog" aria-modal="true" :aria-label="title">
      <h2 class="dq-modal-title">{{ title }}</h2>
      <p class="dq-modal-sub">Rett opp et feil spørsmål eller svar her - det er samme spørsmål som i spørsmålsbanken.</p>

      <div v-if="error" class="banner error">{{ error }}</div>
      <div v-if="loading" style="color:var(--text-dim);">Laster…</div>

      <div v-else class="dq-qlist">
        <article v-for="(q, i) in questions" :key="q.id" class="dq-qrow">
          <span class="dq-q-num">{{ i + 1 }}</span>
          <div class="dq-qrow-body">
            <div class="dq-qrow-text">{{ q.questionText }}</div>
            <img v-if="q.photoUrl" :src="q.photoUrl" alt="" class="dq-photo dq-photo--small" @error="e => e.target.style.display = 'none'" />
            <div class="dq-qrow-answer"><span>Svar</span><b>{{ q.answer }}</b></div>
            <DailyQuizEasyMeter v-if="statFor(q.id)" :percent="statFor(q.id).percentCorrect" :players="statFor(q.id).players" />
          </div>
          <button class="dq-chip-btn" @click="editing = q">Rediger</button>
        </article>
      </div>

      <div class="dq-modal-actions">
        <button class="btn btn-secondary" @click="emit('close')">Lukk</button>
      </div>
    </div>

    <QuestionFormModal v-if="editing" :question="editing" @close="editing = null" @saved="onSaved" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import QuestionFormModal from './QuestionFormModal.vue'
import DailyQuizEasyMeter from './DailyQuizEasyMeter.vue'
import { useEscapeKey } from '../composables/useEscapeKey'

// A day's questions with their answers, each editable in place (the same edit form as the question
// bank), for fixing a mistake spotted while reviewing - without searching the bank for it.
const props = defineProps({
  setId: { type: [Number, String], required: true },
  title: { type: String, default: 'Quizspørsmål' }
})
const emit = defineEmits(['close'])

const questions = ref([])
const loading = ref(true)
const error = ref('')
const editing = ref(null)
const statsByQuestion = ref({})
const statFor = id => statsByQuestion.value[id] || null

// Escape closes the edit form first, then this list.
useEscapeKey(() => { if (editing.value) editing.value = null; else emit('close') })

onMounted(async () => {
  try {
    const [list, stats] = await Promise.all([
      api.adminListDailyQuizQuestions(props.setId),
      api.adminListDailyQuizQuestionStats(props.setId).catch(() => []) // a nice-to-have: the list works without it
    ])
    questions.value = list
    statsByQuestion.value = Object.fromEntries(stats.map(st => [st.questionId, st]))
  } catch (e) {
    error.value = 'Kunne ikke laste spørsmålene til denne quizen.'
  } finally {
    loading.value = false
  }
})

function onSaved(saved) {
  const i = questions.value.findIndex(q => q.id === saved.id)
  if (i !== -1) questions.value[i] = saved
  editing.value = null
  toast.show('Spørsmålet er oppdatert.')
}
</script>
