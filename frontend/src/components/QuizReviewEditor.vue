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
      <slot name="empty">No questions here yet - use the panels below to add some.</slot>
    </div>

    <div class="add-questions-row">
      <div class="add-panel no-print">
        <div class="add-panel-header">
          <span class="add-panel-title">Add a random batch</span>
          <span class="add-panel-hint">pulls unused questions from one category</span>
        </div>
        <div class="add-panel-row">
          <select v-model="addCategory" style="flex:1; min-width:140px;">
            <option value="" disabled>Choose a category…</option>
            <option v-for="cat in availableCategories" :key="cat" :value="cat">{{ cat }}</option>
          </select>
          <input type="number" min="1" max="20" v-model.number="addCount" style="width:70px;" />
          <button class="btn btn-secondary btn-sm" :disabled="!addCategory || adding" @click="addMore">
            {{ adding ? 'Adding…' : '+ Add' }}
          </button>
        </div>
      </div>

      <div class="add-panel no-print">
        <div class="add-panel-header">
          <span class="add-panel-title">Search and add specific questions</span>
          <span class="add-panel-hint">find one question at a time, by text or label</span>
        </div>
        <div class="add-panel-row">
          <input
            type="text"
            v-model="searchTerm"
            placeholder="Search question, category or answer…"
            style="flex:1; min-width:180px;"
          />
          <div v-if="searchResults.length" class="guess-results" style="position:absolute; top:100%; left:0; right:0; margin-top:4px; z-index:5; max-height:260px; overflow-y:auto;">
            <button
              v-for="r in searchResults"
              :key="r.id"
              type="button"
              class="guess-result-row"
              @click="addSpecific(r)"
            >
              {{ r.questionText }}
              <span style="color:var(--text-dim); font-size:0.85rem;">{{ r.category }} · {{ r.difficultyLevel }}/10</span>
              <span v-for="name in (r.labelNames || [])" :key="name" class="result-label" style="margin-left:6px;">{{ name }}</span>
            </button>
          </div>
        </div>
        <div v-if="allLabels.length" class="label-filter">
          <button
            v-for="l in allLabels"
            :key="l.id"
            type="button"
            class="label-filter-chip"
            :class="{ active: labelFilter.includes(l.id) }"
            @click="toggleLabelFilter(l.id)"
          >{{ l.name }}</button>
        </div>
      </div>
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
const searchTerm = ref('')
const searchResults = ref([])
const allLabels = ref([])
const labelFilter = ref([])

onMounted(() => {
  loadCategories()
  api.fetchQuestionLabels().then(list => { allLabels.value = list }).catch(() => {})
})
watch(() => props.quiz.language, loadCategories)

async function loadCategories() {
  try {
    availableCategories.value = await api.getCategories(props.quiz.language)
  } catch (e) {
    // the add-more picker just stays empty - not critical enough to surface an error banner
  }
}

function toggleLabelFilter(id) {
  const idx = labelFilter.value.indexOf(id)
  if (idx === -1) labelFilter.value.push(id)
  else labelFilter.value.splice(idx, 1)
}

let searchDebounce = null
function runSearch() {
  clearTimeout(searchDebounce)
  const val = searchTerm.value
  // Runs on text (2+ characters) OR a label filter alone - picking a label
  // with no text typed should still show matching questions, not require
  // text first.
  if ((!val || val.trim().length < 2) && !labelFilter.value.length) {
    searchResults.value = []
    return
  }
  searchDebounce = setTimeout(async () => {
    try {
      const results = await api.searchQuestions(props.quiz.language, val, undefined, labelFilter.value)
      const usedIds = new Set(props.quiz.questions.map(q => q.id))
      searchResults.value = results.filter(r => !usedIds.has(r.id))
    } catch (e) {
      // search failures are non-critical - just show no results
    }
  }, 250)
}
watch(searchTerm, runSearch)
watch(labelFilter, runSearch, { deep: true })

function addSpecific(question) {
  props.quiz.questions.push(question)
  searchTerm.value = ''
  searchResults.value = []
  emit('changed')
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
