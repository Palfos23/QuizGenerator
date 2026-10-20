<template>
  <BdayPinGate>
    <div style="max-width:720px; margin:0 auto; padding:20px 20px 60px;">
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <h1 style="margin:0;">{{ $t('🎂 Bursdagsquiz') }}</h1>
        <div style="display:flex; gap:8px;">
          <router-link to="/bday/admin" class="btn btn-secondary btn-sm">{{ $t('Administrer spillere') }}</router-link>
          <router-link to="/bday/leaderboard" class="btn btn-secondary btn-sm">{{ $t('Åpne topplisten') }}</router-link>
        </div>
      </div>

      <div v-if="error" class="banner error" style="margin-top:16px;">{{ error }}</div>

      <template v-if="step === 'picking'">
        <p class="page-subtitle">{{ $t('Velg navnet ditt for å starte. Når du er ferdig, vises ikke navnet ditt igjen.') }}</p>

        <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>
        <div v-else-if="!guests.length" class="empty-state">
          {{ $t('Ingen venter på å spille - sjekk topplisten, eller') }}
          <router-link to="/bday/admin">{{ $t('administrer spillere') }}</router-link> {{ $t('for å legge til noen.') }}
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
        <h2>{{ activeGuest.name }}{{ $t('sin tur') }}</h2>

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
          {{ submitting ? $t('Sender inn…') : $t('Send inn svar') }}
        </button>
      </template>

      <template v-else-if="step === 'result' && result">
        <h2>{{ activeGuest.name }} {{ $t('fikk') }} {{ result.score }} / {{ result.maxScore }}</h2>

        <div v-if="result.mapResults.length" class="field">
          <label>{{ $t('Kartspørsmål') }}</label>
          <ul style="margin:0; padding-left:20px; line-height:1.8;">
            <li v-for="r in result.mapResults" :key="r.questionId">
              {{ r.correct ? '✓' : '✕' }} {{ $t('Riktig by:') }} <strong>{{ r.correctCityName }}</strong>
              <span style="color:var(--text-dim);"> - {{ Math.round(r.distanceKm) }} {{ $t('km unna') }}</span>
            </li>
          </ul>
        </div>

        <div v-if="result.tileResults.length" class="field">
          <label>{{ $t('Rutespørsmål') }}</label>
          <div style="color:var(--text-dim); font-size:0.9rem;">{{ correctTileCount }} / {{ result.tileResults.length }} {{ $t('riktige ruter') }}</div>
        </div>

        <button class="btn btn-primary" style="margin-top:12px;" @click="backToPicker">{{ $t('Neste gjest') }}</button>
      </template>
    </div>
  </BdayPinGate>
</template>

<script setup>
import { t as _t } from '../../i18n'
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
    error.value = _t('Kunne ikke laste gjesteoversikten.')
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
    error.value = e.response?.data?.message || _t('Kunne ikke starte quizen for den gjesten.')
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
    error.value = e.response?.data?.message || _t('Kunne ikke sende inn svarene.')
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
