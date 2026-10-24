<template>
  <div class="bp">
    <div class="bp-top">
      <h1>🎂 Bursdagsquiz</h1>
      <router-link to="/bday" class="bp-link">Oversikt</router-link>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>

    <!-- 1. Pick your name -->
    <template v-if="step === 'picking'">
      <p class="page-subtitle">Velg navnet ditt for å starte. Når du er ferdig, vises ikke navnet ditt igjen.</p>

      <div v-if="loading" style="color:var(--text-dim);">Laster…</div>
      <div v-else-if="!guests.length" class="empty-state">
        Ingen venter på å spille akkurat nå. Står ikke navnet ditt her? Si fra til Pål.
      </div>
      <template v-else>
        <input v-if="guests.length > 8" v-model="search" type="search" class="bp-search" placeholder="Søk etter navnet ditt" autocomplete="off" />
        <div class="bp-names">
          <button v-for="g in visibleGuests" :key="g.id" type="button" class="bp-name" @click="pickGuest(g)">{{ g.name }}</button>
          <div v-if="!visibleGuests.length" style="color:var(--text-dim); padding:8px 2px;">Ingen treff.</div>
        </div>
      </template>
    </template>

    <!-- 2. The quiz -->
    <template v-else-if="step === 'quiz' && quiz">
      <div class="bp-who">
        <span>Quiz for <strong>{{ activeGuest.name }}</strong></span>
        <button type="button" class="bp-link bp-link--btn" @click="changeName">Ikke deg? Bytt navn</button>
      </div>

      <div class="bp-progress" role="progressbar" :aria-valuenow="answeredCount" :aria-valuemax="totalCount">
        <div class="bp-progress-text">{{ answeredCount }} av {{ totalCount }} besvart</div>
        <div class="bp-progress-track"><i :style="{ width: progressPct + '%' }"></i></div>
      </div>

      <section v-for="q in quiz.mapQuestions" :key="q.id" class="bp-section">
        <h2>{{ q.prompt }}</h2>
        <div class="bp-map"><MapPinQuestion :image-url="quiz.mapImageUrl" v-model="mapAnswers[q.id]" /></div>
      </section>

      <section v-if="statements.length" class="bp-section">
        <h2>Hvem stemmer dette for?</h2>
        <BdayQuestionCard v-for="(q, i) in statements" :key="q.id" :question="q" :options="quiz.statementOptions" :number="i + 1" v-model="answers[q.id]" />
      </section>

      <section v-if="photos.length" class="bp-section">
        <h2>Hvem er på bildet?</h2>
        <BdayQuestionCard v-for="(q, i) in photos" :key="q.id" :question="q" :options="quiz.photoOptions" :number="i + 1" v-model="answers[q.id]" />
      </section>

      <section v-if="texts.length" class="bp-section">
        <h2>Skriv svaret</h2>
        <BdayQuestionCard v-for="(q, i) in texts" :key="q.id" :question="q" :number="i + 1" v-model="answers[q.id]" />
      </section>

      <div class="bp-submit">
        <button class="btn btn-primary" :disabled="submitting || !allAnswered" @click="submit">
          {{ submitting ? 'Sender inn…' : 'Send inn svar' }}
        </button>
        <div v-if="!allAnswered" class="bp-submit-hint">{{ totalCount - answeredCount }} igjen å svare på</div>
      </div>
    </template>

    <!-- 3. Done - no "next player": everyone plays on their own phone -->
    <template v-else-if="step === 'result' && result">
      <div class="bp-score">
        <div class="bp-score-label">{{ activeGuest.name }} fikk</div>
        <div class="bp-score-num">{{ result.score }}<small> / {{ result.maxScore }}</small></div>
      </div>

      <div v-for="r in result.mapResults" :key="r.questionId" class="bp-line">
        <span>Kartet</span>
        <span>{{ r.correct ? '✓' : '✕' }} {{ r.distanceKm >= 0 ? Math.round(r.distanceKm) + ' km unna ' + r.correctCityName : 'ikke besvart' }}</span>
      </div>
      <div v-for="s in result.sections" :key="s.type" class="bp-line">
        <span>{{ sectionLabel(s.type) }}</span>
        <span>{{ s.correct }} av {{ s.total }} riktige</span>
      </div>

      <p class="page-subtitle" style="margin-top:18px;">Takk for at du spilte! Se hvordan det går på topplisten.</p>
      <router-link to="/bday/leaderboard" class="btn btn-primary">Se topplisten</router-link>
      <router-link to="/bday/mariokart" class="bp-link" style="margin-left:14px;">Mario Kart</router-link>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import bdayApi from '../services/bdayApi'
import MapPinQuestion from '../components/MapPinQuestion.vue'
import BdayQuestionCard from '../components/BdayQuestionCard.vue'

// The guests' page: open to everyone, no login. Each guest opens this on their own phone, finds their name, answers
// and submits. The page remembers which name was picked, so a reload mid-quiz doesn't send them back to the list
// (their answers so far are lost, but their turn isn't).
const STORAGE_KEY = 'bday_guest_id'

const step = ref('picking') // 'picking' | 'quiz' | 'result'
const guests = ref([])
const loading = ref(true)
const error = ref('')
const search = ref('')

const activeGuest = ref(null)
const quiz = ref(null)
const mapAnswers = reactive({})
const answers = reactive({})
const submitting = ref(false)
const result = ref(null)

const statements = computed(() => (quiz.value?.questions || []).filter(q => q.type === 'STATEMENT'))
const photos = computed(() => (quiz.value?.questions || []).filter(q => q.type === 'PHOTO'))
const texts = computed(() => (quiz.value?.questions || []).filter(q => q.type === 'TEXT'))

const visibleGuests = computed(() => {
  const term = search.value.trim().toLowerCase()
  return term ? guests.value.filter(g => g.name.toLowerCase().includes(term)) : guests.value
})

const totalCount = computed(() => (quiz.value ? quiz.value.mapQuestions.length + quiz.value.questions.length : 0))
const answeredCount = computed(() => {
  if (!quiz.value) return 0
  const maps = quiz.value.mapQuestions.filter(q => mapAnswers[q.id]).length
  const rest = quiz.value.questions.filter(q => (answers[q.id] || '').trim()).length
  return maps + rest
})
const allAnswered = computed(() => totalCount.value > 0 && answeredCount.value === totalCount.value)
const progressPct = computed(() => (totalCount.value ? Math.round((answeredCount.value / totalCount.value) * 100) : 0))

onMounted(async () => {
  await loadGuests()
  const remembered = Number(localStorage.getItem(STORAGE_KEY))
  const guest = remembered && guests.value.find(g => g.id === remembered)
  if (guest) await pickGuest(guest)
  else localStorage.removeItem(STORAGE_KEY)
})

async function loadGuests() {
  loading.value = true
  error.value = ''
  try {
    guests.value = await bdayApi.listGuests()
  } catch (e) {
    error.value = 'Kunne ikke laste navnelisten. Prøv å laste siden på nytt.'
  } finally {
    loading.value = false
  }
}

async function pickGuest(guest) {
  error.value = ''
  try {
    const fetchedQuiz = await bdayApi.getQuiz(guest.id)
    Object.keys(mapAnswers).forEach(k => delete mapAnswers[k])
    Object.keys(answers).forEach(k => delete answers[k])
    quiz.value = fetchedQuiz
    activeGuest.value = guest
    localStorage.setItem(STORAGE_KEY, String(guest.id))
    step.value = 'quiz'
    window.scrollTo(0, 0)
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke starte quizen. Prøv igjen.'
    loadGuests()
  }
}

// Picked the wrong name by mistake: back to the list, nothing has been submitted.
function changeName() {
  localStorage.removeItem(STORAGE_KEY)
  step.value = 'picking'
  quiz.value = null
  activeGuest.value = null
  loadGuests()
}

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    const payload = {
      mapAnswers: quiz.value.mapQuestions
        .filter(q => mapAnswers[q.id])
        .map(q => ({ questionId: q.id, x: mapAnswers[q.id].x, y: mapAnswers[q.id].y })),
      answers: quiz.value.questions.map(q => ({ questionId: q.id, value: (answers[q.id] || '').trim() }))
    }
    result.value = await bdayApi.submitQuiz(activeGuest.value.id, payload)
    localStorage.removeItem(STORAGE_KEY)
    step.value = 'result'
    window.scrollTo(0, 0)
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke sende inn svarene. Prøv igjen.'
  } finally {
    submitting.value = false
  }
}

function sectionLabel(type) {
  return { STATEMENT: 'Påstander', PHOTO: 'Bildespørsmål', TEXT: 'Tekstspørsmål' }[type] || type
}
</script>

<style scoped>
.bp { max-width: 560px; margin: 0 auto; padding: 18px 16px 90px; width: 100%; }
.bp-top { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 4px; }
.bp-top h1 { margin: 0; font-size: 1.8rem; }
.bp-link { color: var(--text); font-weight: 600; font-size: 0.9rem; text-decoration: underline; text-underline-offset: 3px; }
.bp-link--btn { background: none; border: none; padding: 0; cursor: pointer; font-family: inherit; }
.bp-search { width: 100%; margin-bottom: 10px; min-height: 46px; font-size: 1rem; }
.bp-names { display: flex; flex-direction: column; gap: 8px; }
.bp-name {
  min-height: 52px;
  padding: 12px 16px;
  text-align: left;
  background: var(--ink-raised);
  color: var(--text);
  border: 2px solid var(--rule);
  border-radius: 3px;
  font-family: var(--font-display);
  font-size: 1.15rem;
  font-weight: 600;
  cursor: pointer;
}
.bp-name:active { background: var(--text); color: var(--ink-raised); }
.bp-who { display: flex; justify-content: space-between; align-items: baseline; gap: 10px; flex-wrap: wrap; margin: 6px 0 10px; }
.bp-progress {
  position: sticky;
  top: calc(var(--nav-height, 0px) + 4px);
  z-index: 5;
  background: var(--ink);
  padding: 8px 0 10px;
  margin-bottom: 8px;
  border-bottom: 1.5px solid var(--line);
}
.bp-progress-text { font-family: var(--font-mono); font-size: 0.78rem; color: var(--text-dim); margin-bottom: 5px; }
.bp-progress-track { height: 6px; background: rgb(var(--fg-rgb) / 0.12); }
.bp-progress-track i { display: block; height: 100%; background: var(--gold); transition: width 0.2s ease; }
.bp-section { margin-top: 22px; }
.bp-section h2 { font-size: 1.3rem; margin: 0 0 10px; }
.bp-map { max-width: 520px; margin: 0 auto; }
.bp-submit {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: var(--ink);
  border-top: 2px solid var(--rule);
  display: flex;
  align-items: center;
  gap: 14px;
  justify-content: center;
}
.bp-submit .btn { flex: 1; max-width: 360px; min-height: 48px; font-size: 1.05rem; }
.bp-submit-hint { font-size: 0.8rem; color: var(--text-dim); white-space: nowrap; }
.bp-score { text-align: center; padding: 24px 0 14px; }
.bp-score-label { font-family: var(--font-mono); font-size: 0.8rem; letter-spacing: 0.08em; text-transform: uppercase; color: var(--text-dim); }
.bp-score-num { font-family: var(--font-display); font-weight: 700; font-size: 4.5rem; line-height: 1.05; }
.bp-score-num small { font-size: 1.6rem; color: var(--text-dim); font-weight: 500; }
.bp-line { display: flex; justify-content: space-between; gap: 12px; padding: 9px 0; border-top: 1px solid var(--line); }
</style>
