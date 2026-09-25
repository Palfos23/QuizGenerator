<template>
  <BdayPinGate>
    <div style="max-width:720px; margin:0 auto; padding:20px 20px 60px;">
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <h1 style="margin:0;">🎂 Birthday Quiz</h1>
        <div style="display:flex; gap:8px;">
          <router-link to="/bday/admin" class="btn btn-secondary btn-sm">Manage players</router-link>
          <router-link to="/bday/leaderboard" class="btn btn-secondary btn-sm">Open leaderboard</router-link>
        </div>
      </div>

      <div v-if="error" class="banner error" style="margin-top:16px;">{{ error }}</div>

      <template v-if="step === 'picking'">
        <p class="page-subtitle">Pick your name to start. Once you finish, your name won't show up again.</p>

        <div v-if="loading" style="color:var(--text-dim);">Loading…</div>
        <div v-else-if="!guests.length" class="empty-state">
          No one's waiting to play - check the leaderboard, or
          <router-link to="/bday/admin">manage players</router-link> to add someone.
        </div>
        <div v-else class="candidate-list">
          <div v-for="g in guests" :key="g.id" class="candidate-row">
            <button type="button" style="flex:1; text-align:left; background:none; border:none; color:var(--text); font-weight:600; font-size:1rem; cursor:pointer;" @click="pickGuest(g)">
              {{ g.name }}
            </button>
          </div>
        </div>
      </template>

      <template v-else-if="step === 'quiz' && quiz">
        <h2>{{ activeGuest.name }}'s turn</h2>

        <div v-for="q in quiz.mapQuestions" :key="q.id" class="field">
          <label style="text-transform:none; font-size:1.05rem; letter-spacing:normal; color:var(--text);">{{ q.prompt }}</label>
          <MapPinQuestion :image-url="quiz.mapImageUrl" v-model="mapAnswers[q.id]" />
        </div>

        <div v-for="q in quiz.tileQuestions" :key="q.id" class="field">
          <label style="text-transform:none; font-size:1.05rem; letter-spacing:normal; color:var(--text);">{{ q.prompt }}</label>
          <TileGridQuestion
            :tiles="q.tiles"
            :person-a-label="q.personALabel"
            :person-b-label="q.personBLabel"
            v-model="tileAnswers[q.id]"
          />
        </div>

        <button class="btn btn-primary" style="margin-top:12px;" :disabled="submitting || !allAnswered" @click="submit">
          {{ submitting ? 'Submitting…' : 'Submit answers' }}
        </button>
      </template>

      <template v-else-if="step === 'result' && result">
        <h2>{{ activeGuest.name }} scored {{ result.score }} / {{ result.maxScore }}</h2>

        <div v-if="result.mapResults.length" class="field">
          <label>Map questions</label>
          <ul style="margin:0; padding-left:20px; line-height:1.8;">
            <li v-for="r in result.mapResults" :key="r.questionId">
              {{ r.correct ? '✓' : '✕' }} Correct city: <strong>{{ r.correctCityName }}</strong>
              <span style="color:var(--text-dim);"> - {{ Math.round(r.distanceKm) }} km away</span>
            </li>
          </ul>
        </div>

        <div v-if="result.tileResults.length" class="field">
          <label>Tile questions</label>
          <div style="color:var(--text-dim); font-size:0.9rem;">{{ correctTileCount }} / {{ result.tileResults.length }} tiles correct</div>
        </div>

        <button class="btn btn-primary" style="margin-top:12px;" @click="backToPicker">Next guest</button>
      </template>
    </div>
  </BdayPinGate>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import bdayApi from '../services/bdayApi'
import BdayPinGate from '../components/BdayPinGate.vue'
import MapPinQuestion from '../components/MapPinQuestion.vue'
import TileGridQuestion from '../components/TileGridQuestion.vue'

const step = ref('picking') // 'picking' | 'quiz' | 'result'
const guests = ref([])
const loading = ref(true)
const error = ref('')

const activeGuest = ref(null)
const quiz = ref(null)
const mapAnswers = reactive({})
const tileAnswers = reactive({})
const submitting = ref(false)
const result = ref(null)

onMounted(loadGuests)

async function loadGuests() {
  loading.value = true
  error.value = ''
  try {
    guests.value = await bdayApi.listGuests()
  } catch (e) {
    error.value = 'Could not load the guest list.'
  } finally {
    loading.value = false
  }
}

async function pickGuest(guest) {
  error.value = ''
  try {
    const fetchedQuiz = await bdayApi.getQuiz(guest.id)
    Object.keys(mapAnswers).forEach(k => delete mapAnswers[k])
    Object.keys(tileAnswers).forEach(k => delete tileAnswers[k])
    quiz.value = fetchedQuiz
    activeGuest.value = guest
    step.value = 'quiz'
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not start the quiz for that guest.'
  }
}

const allAnswered = computed(() => {
  if (!quiz.value) return false
  const mapsDone = quiz.value.mapQuestions.every(q => mapAnswers[q.id])
  const tilesDone = quiz.value.tileQuestions.every(q => q.tiles.every(t => tileAnswers[q.id]?.[t.id]))
  return mapsDone && tilesDone
})

const correctTileCount = computed(() => result.value ? result.value.tileResults.filter(r => r.correct).length : 0)

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    const payload = {
      mapAnswers: quiz.value.mapQuestions
        .filter(q => mapAnswers[q.id])
        .map(q => ({ questionId: q.id, x: mapAnswers[q.id].x, y: mapAnswers[q.id].y })),
      tileAnswers: quiz.value.tileQuestions.flatMap(q =>
        q.tiles.map(t => ({ tileId: t.id, chosenOwner: tileAnswers[q.id]?.[t.id] }))
      )
    }
    result.value = await bdayApi.submitQuiz(activeGuest.value.id, payload)
    step.value = 'result'
  } catch (e) {
    error.value = e.response?.data?.message || 'Could not submit those answers.'
  } finally {
    submitting.value = false
  }
}

function backToPicker() {
  step.value = 'picking'
  quiz.value = null
  activeGuest.value = null
  result.value = null
  loadGuests()
}
</script>
