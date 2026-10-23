<template>
  <div class="ba">
    <div class="ba-top">
      <div>
        <span class="dq-eyebrow">Admin</span>
        <h1 style="margin:2px 0 0;">🎂 Bursdagsquiz</h1>
      </div>
      <div class="ba-links">
        <router-link to="/bday" class="btn btn-secondary btn-sm">Åpne quizen</router-link>
        <router-link to="/bday/leaderboard" class="btn btn-secondary btn-sm">Topplisten</router-link>
      </div>
    </div>
    <p class="page-subtitle">Her styrer du spillerne og spørsmålene. Gjestene trenger ikke å logge inn - de åpner <strong>/bday</strong> på sin egen telefon og velger navnet sitt.</p>

    <div class="ba-tabs" role="tablist">
      <button type="button" role="tab" :aria-selected="tab === 'players'" :class="{ 'is-on': tab === 'players' }" @click="tab = 'players'">Spillere</button>
      <button type="button" role="tab" :aria-selected="tab === 'questions'" :class="{ 'is-on': tab === 'questions' }" @click="tab = 'questions'">Spørsmål</button>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>

    <!-- ===================================================== Players -->
    <template v-if="tab === 'players'">
      <div class="field">
        <label>Legg til en gjest</label>
        <div style="display:flex; gap:10px;">
          <input type="text" v-model="newGuestName" placeholder="Navn" @keydown.enter="addGuest" />
          <button class="btn btn-secondary" style="white-space:nowrap;" :disabled="addingGuest" @click="addGuest">Legg til</button>
        </div>
      </div>

      <div v-if="loadingGuests" style="color:var(--text-dim); margin-top:20px;">Laster…</div>
      <template v-else>
        <div v-if="pending.length" class="field" style="margin-top:24px;">
          <label>Venter på å spille ({{ pending.length }})</label>
          <div class="candidate-list">
            <div v-for="g in pending" :key="g.id" class="candidate-row">
              <span style="flex:1;">{{ g.name }}</span>
              <button class="btn btn-danger btn-sm" @click="removeGuest(g)">Fjern</button>
            </div>
          </div>
        </div>

        <div v-if="done.length" class="field" style="margin-top:24px;">
          <label>Allerede ferdig ({{ done.length }})</label>
          <div class="candidate-list">
            <div v-for="g in done" :key="g.id" class="candidate-row" style="display:flex; align-items:center; gap:10px; flex-wrap:wrap;">
              <span style="flex:1; min-width:120px;">{{ g.name }}</span>
              <label style="display:flex; align-items:center; gap:6px; margin:0; text-transform:none; font-weight:400; color:var(--text-dim); font-size:0.9rem;">
                Poeng
                <input
                  type="number"
                  min="0"
                  step="1"
                  :value="scoreDrafts[g.id] ?? g.score"
                  style="width:80px; padding:6px 8px;"
                  @input="scoreDrafts[g.id] = $event.target.value === '' ? null : Number($event.target.value)"
                  @keydown.enter="saveScore(g)"
                />
              </label>
              <button class="btn btn-secondary btn-sm" :disabled="savingId === g.id || !scoreChanged(g)" @click="saveScore(g)">
                {{ savingId === g.id ? 'Lagrer…' : 'Lagre' }}
              </button>
              <button class="btn btn-secondary btn-sm" @click="openAnswers(g)">Se svar</button>
              <button class="btn btn-danger btn-sm" @click="pendingReset = g">Nullstill</button>
            </div>
          </div>
        </div>

        <div v-if="!pending.length && !done.length" class="empty-state" style="margin-top:24px;">Ingen spillere registrert ennå.</div>
      </template>
    </template>

    <!-- ===================================================== Questions -->
    <template v-else>
      <p class="page-subtitle">
        Selve kartspørsmålet (Førde) er fast. Resten skriver du her. Anbefalt: 10 påstander, 5 bildespørsmål og 5 tekstspørsmål - hvert riktig svar er ett poeng.
      </p>
      <div v-if="loadingQuestions" style="color:var(--text-dim);">Laster…</div>
      <template v-else>
        <section v-for="sec in sections" :key="sec.type" class="ba-section">
          <div class="ba-section-head">
            <h2>{{ sec.title }}</h2>
            <span class="ba-count" :class="{ 'is-full': sec.items.length >= sec.target }">{{ sec.items.length }} / {{ sec.target }}</span>
            <button class="btn btn-secondary btn-sm" style="margin-left:auto;" @click="startNew(sec.type)">+ Legg til</button>
          </div>
          <p class="picker-hint" style="margin:0 0 8px;">{{ sec.hint }}</p>

          <div v-if="!sec.items.length" class="empty-state" style="padding:14px;">Ingen spørsmål ennå.</div>
          <div v-else class="candidate-list">
            <div v-for="q in sec.items" :key="q.id" class="candidate-row ba-q">
              <img v-if="q.type === 'PHOTO'" :src="q.photoUrl" alt="" class="ba-thumb" />
              <div class="ba-q-main">
                <div>{{ q.prompt }}</div>
                <div class="ba-q-answer">Riktig: <strong>{{ answerText(q) }}</strong></div>
              </div>
              <button class="btn btn-secondary btn-sm" @click="startEdit(q)">Rediger</button>
              <button class="btn btn-danger btn-sm" @click="pendingDelete = q">Slett</button>
            </div>
          </div>
        </section>
      </template>
    </template>

    <!-- ===================================================== Question form -->
    <div v-if="form" class="modal-backdrop" @click.self="form = null">
      <div class="modal" role="dialog" aria-modal="true" style="max-width:520px;">
        <h2 style="margin-top:0;">{{ form.id ? 'Rediger' : 'Nytt' }} {{ typeNames[form.type] }}</h2>

        <div v-if="formError" class="banner error">{{ formError }}</div>

        <div v-if="form.type === 'PHOTO'" class="field">
          <label>Bilde</label>
          <input type="text" v-model="form.photoUrl" placeholder="https://… eller last opp under" />
          <div style="margin-top:8px;"><PhotoUploadButton label="Last opp bilde" @uploaded="url => form.photoUrl = url" /></div>
          <img v-if="form.photoUrl" :src="form.photoUrl" alt="" class="ba-preview" />
        </div>

        <div class="field">
          <label>{{ form.type === 'STATEMENT' ? 'Påstand' : form.type === 'PHOTO' ? 'Spørsmål (valgfritt)' : 'Spørsmål' }}</label>
          <input
            type="text"
            v-model="form.prompt"
            :placeholder="form.type === 'STATEMENT' ? 'F.eks. Har vært i Japan' : form.type === 'PHOTO' ? 'Hvem er på bildet?' : 'F.eks. Hva het første bilen deres?'"
          />
        </div>

        <div v-if="form.type !== 'TEXT'" class="field">
          <label>Riktig svar</label>
          <div class="ba-pick">
            <button
              v-for="o in (form.type === 'STATEMENT' ? statementOptions : photoOptions)"
              :key="o.code"
              type="button"
              :class="{ 'is-on': form.correctOption === o.code }"
              @click="form.correctOption = o.code"
            >{{ o.label }}</button>
          </div>
        </div>

        <div v-else class="field">
          <label>Godkjente svar <span class="picker-hint">ett per linje - store/små bokstaver, æøå og tegn spiller ingen rolle</span></label>
          <textarea v-model="form.acceptedAnswers" rows="4" placeholder="Førde&#10;Forde"></textarea>
        </div>

        <div class="modal-actions">
          <button class="btn btn-secondary" @click="form = null">Avbryt</button>
          <button class="btn btn-primary" :disabled="savingQuestion" @click="saveQuestion">{{ savingQuestion ? 'Lagrer…' : 'Lagre' }}</button>
        </div>
      </div>
    </div>

    <!-- ===================================================== A finished guest's answers -->
    <div v-if="answersFor" class="modal-backdrop" @click.self="answersFor = null">
      <div class="modal" role="dialog" aria-modal="true" style="max-width:560px;">
        <h2 style="margin-top:0;">{{ answersFor.name }} - svar</h2>
        <div v-if="loadingAnswers" style="color:var(--text-dim);">Laster…</div>
        <div v-else>
          <div v-for="(a, i) in answerSheet" :key="i" class="ba-ans">
            <span class="ba-ans-mark" :class="a.points >= a.maxPoints ? 'is-ok' : 'is-bad'">{{ a.points >= a.maxPoints ? '✓' : '✕' }}</span>
            <div class="ba-ans-main">
              <div>{{ a.prompt }}</div>
              <div class="ba-q-answer">{{ a.response }}</div>
            </div>
            <span class="ba-ans-pts">{{ a.points }}/{{ a.maxPoints }}</span>
          </div>
          <p class="picker-hint" style="margin-top:10px;">Er et tekstsvar riktig men ikke godkjent? Endre poengsummen til spilleren i listen.</p>
        </div>
        <div class="modal-actions"><button class="btn btn-secondary" @click="answersFor = null">Lukk</button></div>
      </div>
    </div>

    <ConfirmModal
      v-if="pendingReset"
      title="Nullstille denne spilleren?"
      :message="`Poengsummen og svarene til ${pendingReset.name} slettes, og vedkommende havner tilbake på ventelisten, slik at de kan spille igjen.`"
      confirm-text="Nullstill spilleren"
      @confirm="doReset"
      @cancel="pendingReset = null"
    />
    <ConfirmModal
      v-if="pendingDelete"
      title="Slette dette spørsmålet?"
      :message="`«${pendingDelete.prompt}» fjernes fra quizen. Spillere som allerede er ferdige beholder poengene sine.`"
      confirm-text="Slett"
      @confirm="doDeleteQuestion"
      @cancel="pendingDelete = null"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../../services/api'
import ConfirmModal from '../../components/ConfirmModal.vue'
import PhotoUploadButton from '../../components/PhotoUploadButton.vue'

// The host's page for the birthday quiz - behind the real admin login (router meta + /api/admin/bday/**), so only the
// site's admin gets here. Two tabs: the players (add, remove, fix a score, reset, see answers) and the questions
// (statements with Pål / Erik / Begge / Ingen, photo questions, text questions).
const tab = ref('players')
const error = ref('')

const statementOptions = [
  { code: 'PAL', label: 'Pål' }, { code: 'ERIK', label: 'Erik' }, { code: 'BEGGE', label: 'Begge' }, { code: 'INGEN', label: 'Ingen' }
]
const photoOptions = statementOptions.slice(0, 2)
const typeNames = { STATEMENT: 'påstand', PHOTO: 'bildespørsmål', TEXT: 'tekstspørsmål' }

onMounted(() => {
  loadGuests()
  loadQuestions()
})

// ---------------------------------------------------------------- players
const guests = ref([])
const loadingGuests = ref(true)
const newGuestName = ref('')
const addingGuest = ref(false)
const scoreDrafts = reactive({}) // guest id -> the number typed in the box
const savingId = ref(null)
const pendingReset = ref(null)

const pending = computed(() => guests.value.filter(g => g.status !== 'DONE'))
const done = computed(() => guests.value.filter(g => g.status === 'DONE'))

async function loadGuests() {
  loadingGuests.value = true
  try {
    guests.value = await api.adminBdayGuests()
  } catch (e) {
    error.value = 'Kunne ikke laste spillerne.'
  } finally {
    loadingGuests.value = false
  }
}

async function addGuest() {
  const name = newGuestName.value.trim()
  if (!name) return
  addingGuest.value = true
  error.value = ''
  try {
    await api.adminBdayAddGuest(name)
    newGuestName.value = ''
    await loadGuests()
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke legge til gjesten.'
  } finally {
    addingGuest.value = false
  }
}

async function removeGuest(guest) {
  error.value = ''
  try {
    await api.adminBdayDeleteGuest(guest.id)
    guests.value = guests.value.filter(g => g.id !== guest.id)
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke fjerne gjesten.'
  }
}

function scoreChanged(g) {
  const draft = scoreDrafts[g.id]
  return draft !== undefined && draft !== null && draft !== g.score
}

async function saveScore(g) {
  if (!scoreChanged(g)) return
  savingId.value = g.id
  error.value = ''
  try {
    const updated = await api.adminBdaySetScore(g.id, scoreDrafts[g.id])
    guests.value = guests.value.map(x => (x.id === g.id ? { ...x, score: updated.score } : x))
    delete scoreDrafts[g.id]
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke endre poengsummen.'
  } finally {
    savingId.value = null
  }
}

async function doReset() {
  const g = pendingReset.value
  pendingReset.value = null
  if (!g) return
  error.value = ''
  try {
    await api.adminBdayResetGuest(g.id)
    delete scoreDrafts[g.id]
    await loadGuests()
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke nullstille spilleren.'
  }
}

const answersFor = ref(null)
const answerSheet = ref([])
const loadingAnswers = ref(false)

async function openAnswers(g) {
  answersFor.value = g
  answerSheet.value = []
  loadingAnswers.value = true
  try {
    answerSheet.value = await api.adminBdayGuestAnswers(g.id)
  } catch (e) {
    error.value = 'Kunne ikke laste svarene.'
    answersFor.value = null
  } finally {
    loadingAnswers.value = false
  }
}

// ---------------------------------------------------------------- questions
const questions = ref([])
const loadingQuestions = ref(true)
const form = ref(null)
const formError = ref('')
const savingQuestion = ref(false)
const pendingDelete = ref(null)

const sections = computed(() => [
  { type: 'STATEMENT', title: 'Påstander', target: 10, hint: 'Gjesten velger Pål, Erik, Begge eller Ingen.' },
  { type: 'PHOTO', title: 'Bildespørsmål', target: 5, hint: 'Et bilde - gjesten velger hvem som er på det (Pål eller Erik).' },
  { type: 'TEXT', title: 'Tekstspørsmål', target: 5, hint: 'Gjesten skriver svaret selv. Du legger inn hva som regnes som riktig.' }
].map(s => ({ ...s, items: questions.value.filter(q => q.type === s.type) })))

async function loadQuestions() {
  loadingQuestions.value = true
  try {
    questions.value = await api.adminBdayQuestions()
  } catch (e) {
    error.value = 'Kunne ikke laste spørsmålene.'
  } finally {
    loadingQuestions.value = false
  }
}

function answerText(q) {
  if (q.type === 'TEXT') return (q.acceptedAnswers || '').split('\n').filter(Boolean).join(' / ')
  return (statementOptions.find(o => o.code === q.correctOption) || {}).label || q.correctOption
}

function startNew(type) {
  formError.value = ''
  form.value = { id: null, type, prompt: '', photoUrl: '', correctOption: '', acceptedAnswers: '' }
}

function startEdit(q) {
  formError.value = ''
  form.value = {
    id: q.id,
    type: q.type,
    prompt: q.type === 'PHOTO' && q.prompt === 'Hvem er på bildet?' ? '' : q.prompt,
    photoUrl: q.photoUrl || '',
    correctOption: q.correctOption || '',
    acceptedAnswers: q.acceptedAnswers || ''
  }
}

async function saveQuestion() {
  const f = form.value
  formError.value = ''
  savingQuestion.value = true
  try {
    const payload = { type: f.type, prompt: f.prompt, photoUrl: f.photoUrl, correctOption: f.correctOption, acceptedAnswers: f.acceptedAnswers }
    if (f.id) await api.adminBdayUpdateQuestion(f.id, payload)
    else await api.adminBdayCreateQuestion(payload)
    form.value = null
    await loadQuestions()
  } catch (e) {
    formError.value = e.response?.data?.message || 'Kunne ikke lagre spørsmålet.'
  } finally {
    savingQuestion.value = false
  }
}

async function doDeleteQuestion() {
  const q = pendingDelete.value
  pendingDelete.value = null
  if (!q) return
  try {
    await api.adminBdayDeleteQuestion(q.id)
    questions.value = questions.value.filter(x => x.id !== q.id)
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke slette spørsmålet.'
  }
}
</script>

<style scoped>
.ba { max-width: 720px; margin: 0 auto; padding: 20px 16px 60px; width: 100%; }
.ba-top { display: flex; justify-content: space-between; align-items: flex-end; gap: 12px; flex-wrap: wrap; }
.ba-links { display: flex; gap: 8px; }
.ba-tabs { display: flex; gap: 0; margin: 14px 0 18px; border: 2px solid var(--rule); border-radius: 2px; width: fit-content; }
.ba-tabs button { padding: 8px 20px; background: transparent; color: var(--text); border: none; font-family: var(--font-body); font-weight: 600; cursor: pointer; }
.ba-tabs button + button { border-left: 2px solid var(--rule); }
.ba-tabs button.is-on { background: var(--text); color: var(--ink-raised); }
.ba-section { margin-bottom: 28px; }
.ba-section-head { display: flex; align-items: center; gap: 10px; margin-bottom: 2px; }
.ba-section-head h2 { margin: 0; font-size: 1.25rem; }
.ba-count { font-family: var(--font-mono); font-size: 0.78rem; border: 1.5px solid var(--rule); border-radius: 3px; padding: 0 7px; color: var(--text-dim); }
.ba-count.is-full { color: var(--teal); border-color: var(--teal); }
.ba-q { display: flex; align-items: center; gap: 10px; }
.ba-q-main { flex: 1; min-width: 0; overflow-wrap: anywhere; }
.ba-q-answer { font-size: 0.82rem; color: var(--text-dim); }
.ba-thumb { width: 52px; height: 52px; object-fit: cover; border: 1.5px solid var(--line); border-radius: 2px; flex-shrink: 0; }
.ba-preview { display: block; max-width: 100%; max-height: 220px; margin-top: 10px; border: 1.5px solid var(--line); border-radius: 2px; }
.ba-pick { display: flex; gap: 6px; flex-wrap: wrap; }
.ba-pick button { padding: 8px 16px; background: transparent; color: var(--text); border: 2px solid var(--rule); border-radius: 2px; font-weight: 600; cursor: pointer; font-family: var(--font-body); }
.ba-pick button.is-on { background: var(--text); color: var(--ink-raised); }
.ba-ans { display: flex; align-items: flex-start; gap: 10px; padding: 8px 0; border-top: 1px solid var(--line); }
.ba-ans-main { flex: 1; min-width: 0; overflow-wrap: anywhere; }
.ba-ans-mark { font-weight: 700; width: 18px; }
.ba-ans-mark.is-ok { color: var(--teal); }
.ba-ans-mark.is-bad { color: var(--coral); }
.ba-ans-pts { font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-dim); white-space: nowrap; }
</style>
