<template>
  <div class="dq-page dq-page--wide aq">
    <header class="dq-hero">
      <span class="dq-eyebrow">Admin</span>
      <h1 class="dq-title">{{ $t('Rett svar per svar') }}</h1>
      <p class="dq-sub">{{ $t('Hvert ulike svar vises én gang, uansett hvor mange som skrev det. Én avgjørelse gjelder alle - og kan huskes til neste gang.') }}</p>
    </header>

    <div class="aq-tabs" role="tablist">
      <button type="button" role="tab" :aria-selected="tab === 'answers'" :class="{ 'is-on': tab === 'answers' }" @click="tab = 'answers'">
        {{ $t('Svar til gjennomgang') }} <span v-if="groups && groups.pendingTotal" class="dq-pill dq-pill--wait">{{ groups.pendingTotal }}</span>
      </button>
      <button type="button" role="tab" :aria-selected="tab === 'complaints'" :class="{ 'is-on': tab === 'complaints' }" @click="tab = 'complaints'">
        {{ $t('Klager') }} <span v-if="complaints.length" class="dq-pill dq-pill--wait">{{ complaints.length }}</span>
      </button>
      <router-link to="/admin/daily-quiz-review" class="aq-old">{{ $t('Gjennomgå per spiller') }} →</router-link>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

    <!-- ====================================================== the answers -->
    <template v-else-if="tab === 'answers'">
      <div class="aq-bar">
        <label class="aq-remember">
          <input type="checkbox" v-model="remember" />
          <span><strong>{{ $t('Husk valget') }}</strong> - {{ $t('samme svar rettes automatisk neste gang spørsmålet brukes') }}</span>
        </label>
        <label class="aq-remember">
          <input type="checkbox" v-model="showDecided" />
          <span>{{ $t('Vis også svar som allerede er avgjort') }}</span>
        </label>
        <button v-if="groups.likelyTypoTotal" class="btn btn-secondary btn-sm" :disabled="busy" @click="confirmTypos = true">
          {{ $t('Godta alle som ser ut som skrivefeil ({0})', [groups.likelyTypoTotal]) }}
        </button>
      </div>

      <div v-if="!visibleDays.length" class="dq-card dq-empty">
        <div class="dq-empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>
        </div>
        <strong>{{ $t('Alt er gjennomgått') }}</strong>
        {{ $t('Ingenting venter på gjennomgang.') }}
      </div>

      <section v-for="day in visibleDays" :key="day.setId" class="dq-card aq-day">
        <h2 class="aq-day-title">{{ formatLong(day.quizDate) }} <span v-if="day.pendingCount" class="dq-pill dq-pill--wait">{{ $t('{0} svar venter', [day.pendingCount]) }}</span></h2>

        <div v-for="q in day.questions" :key="q.questionId" class="aq-question">
          <div class="aq-q-text"><span class="aq-q-num">{{ q.number }}</span> {{ q.questionText }}</div>
          <div class="aq-q-correct">{{ $t('Riktig svar:') }} <strong>{{ q.correctAnswer }}</strong></div>

          <div v-for="g in q.groups" :key="g.key" class="aq-group" :class="{ 'is-pending': g.pendingCount > 0 }">
            <div class="aq-group-main">
              <span class="aq-group-text">{{ g.text }}</span>
              <span class="aq-count">{{ $t('{0} spillere', [g.count]) }}</span>
              <span v-if="g.likelyTypo" class="dq-pill dq-pill--wait">{{ $t('ser ut som skrivefeil') }}</span>
              <span v-if="g.rule !== null" class="dq-pill" :class="g.rule ? 'dq-pill--ok' : 'dq-pill--bad'">{{ g.rule ? $t('husket: riktig') : $t('husket: feil') }}</span>
              <span v-if="g.pendingCount === 0" class="aq-state" :class="g.correctCount ? 'is-ok' : 'is-bad'">{{ g.correctCount ? $t('regnet som riktig') : $t('regnet som feil') }}</span>
            </div>
            <div class="aq-actions">
              <button class="btn btn-secondary btn-sm aq-yes" :disabled="busy || (g.pendingCount === 0 && g.correctCount === g.count)" @click="decide(q, g, true)">✓ {{ $t('Riktig§AdminAnswers') }}</button>
              <button class="btn btn-secondary btn-sm aq-no" :disabled="busy || (g.pendingCount === 0 && g.incorrectCount === g.count)" @click="decide(q, g, false)">✕ {{ $t('Feil§AdminAnswers') }}</button>
            </div>
          </div>
        </div>
      </section>
    </template>

    <!-- ====================================================== complaints -->
    <template v-else>
      <div v-if="!complaints.length" class="dq-card dq-empty">
        <div class="dq-empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>
        </div>
        <strong>{{ $t('Ingen klager') }}</strong>
        {{ $t('Ingen spillere venter på ny vurdering.') }}
      </div>

      <article v-for="c in complaints" :key="c.id" class="dq-card aq-complaint">
        <div class="aq-c-head">
          <strong>{{ c.playerName }}</strong>
          <span class="aq-count">{{ formatLong(c.quizDate) }}</span>
          <span v-if="c.decidedBy" class="dq-pill">{{ decidedLabel(c.decidedBy) }}</span>
        </div>
        <div class="aq-q-text">{{ c.questionText }}</div>
        <div class="aq-c-lines">
          <div><span class="aq-label">{{ $t('Riktig svar:') }}</span> <strong>{{ c.correctAnswer }}</strong></div>
          <div><span class="aq-label">{{ $t('Spillerens svar:') }}</span> <strong>{{ c.playerAnswer }}</strong></div>
          <div v-if="c.comment" class="aq-c-comment">«{{ c.comment }}»</div>
        </div>
        <label class="aq-remember" style="margin:10px 0 0;">
          <input type="checkbox" v-model="acceptForEveryone[c.id]" />
          <span>{{ $t('Godta også for alle som skrev det samme - og husk det') }}</span>
        </label>
        <div class="aq-c-actions">
          <button class="btn btn-primary btn-sm" :disabled="busy" @click="acceptComplaint(c)">✓ {{ $t('Godta svaret') }}</button>
          <input type="text" class="adv-input aq-note" v-model="notes[c.id]" maxlength="300" :placeholder="$t('Kort begrunnelse til spilleren (valgfritt)')" />
          <button class="btn btn-secondary btn-sm" :disabled="busy" @click="rejectComplaint(c)">✕ {{ $t('Avslå') }}</button>
        </div>
      </article>
    </template>

    <ConfirmModal
      v-if="confirmTypos"
      :title="$t('Godta alle skrivefeil?')"
      :message="$t('{0} svar som ser ut som skrivefeil av det riktige svaret regnes som riktige, og huskes til neste gang. Se gjennom listen først hvis du er usikker - noen svar kan se like ut, men være noe annet.', [groups.likelyTypoTotal])"
      :confirm-text="$t('Godta alle')"
      @confirm="acceptTypos"
      @cancel="confirmTypos = false"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { t as _t, locale } from '../i18n'
import api from '../services/api'
import toast from '../services/toast'
import ConfirmModal from '../components/ConfirmModal.vue'

// The admin's faster way through the daily quiz answers: every distinct answer once, decided for everybody who wrote it
// (and remembered for next time), plus the players' complaints.
const route = useRoute()
const tab = ref(route.query.tab === 'complaints' ? 'complaints' : 'answers')
const groups = ref({ days: [], pendingTotal: 0, likelyTypoTotal: 0 })
const complaints = ref([])
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const remember = ref(true)
const showDecided = ref(false)
const confirmTypos = ref(false)
const acceptForEveryone = reactive({})
const notes = reactive({})

const visibleDays = computed(() => groups.value.days
  .map(day => ({
    ...day,
    questions: day.questions
      .map(q => ({ ...q, groups: showDecided.value ? q.groups : q.groups.filter(g => g.pendingCount > 0) }))
      .filter(q => q.groups.length)
  }))
  .filter(day => day.questions.length))

onMounted(async () => {
  await reload()
  loading.value = false
})

async function reload() {
  try {
    const [g, c] = await Promise.all([api.adminGetDailyQuizAnswerGroups(), api.adminListDailyQuizComplaints()])
    groups.value = g
    complaints.value = c
    for (const x of c) if (acceptForEveryone[x.id] === undefined) acceptForEveryone[x.id] = true
    error.value = ''
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke laste svarene.')
  }
}

async function run(fn, okMessage) {
  busy.value = true
  error.value = ''
  try {
    await fn()
    if (okMessage) toast.show(okMessage)
    await reload()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Det gikk ikke - prøv igjen.')
  } finally {
    busy.value = false
  }
}

const decide = (q, g, correct) => run(async () => {
  const r = await api.adminDecideDailyQuizAnswerGroup(q.questionId, g.text, correct, remember.value)
  toast.show(_t('{0} svar rettet.', [r.answers]))
})

const acceptTypos = () => {
  confirmTypos.value = false
  return run(async () => {
    const r = await api.adminAcceptDailyQuizTypos(null)
    toast.show(_t('{0} svar godtatt.', [r.answers]))
  })
}

const acceptComplaint = c => run(async () => {
  const r = await api.adminAcceptDailyQuizComplaint(c.id, !!acceptForEveryone[c.id])
  toast.show(_t('Godtatt - {0} svar rettet.', [r.answers]))
})

const rejectComplaint = c => run(async () => {
  await api.adminRejectDailyQuizComplaint(c.id, notes[c.id] || '')
  toast.show(_t('Klagen er avslått.'))
})

function formatLong(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString(locale, { weekday: 'long', day: 'numeric', month: 'long' })
}

function decidedLabel(source) {
  return { EXACT: _t('rettet automatisk'), NORMALIZED: _t('rettet automatisk'), RULE: _t('husket regel'), ADMIN: _t('rettet av admin'), COMPLAINT: _t('rettet av admin') }[source] || source
}
</script>

<style scoped>
.aq-tabs { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin: 4px 0 16px; }
.aq-tabs button { display: inline-flex; gap: 8px; align-items: center; padding: 8px 16px; background: transparent; color: var(--text); border: 2px solid var(--rule); border-radius: 2px; font-family: var(--font-body); font-weight: 600; cursor: pointer; }
.aq-tabs button.is-on { background: var(--text); color: var(--ink-raised); }
.aq-tabs button.is-on .dq-pill { background: var(--ink-raised); color: var(--text); }
.aq-old { margin-left: auto; color: var(--text-dim); font-size: 0.9rem; }
.aq-bar { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; margin-bottom: 14px; }
.aq-remember { display: flex; gap: 8px; align-items: center; margin: 0; text-transform: none; letter-spacing: normal; font-weight: 400; color: var(--text); font-size: 0.92rem; cursor: pointer; }
.aq-remember input { width: 18px; height: 18px; flex-shrink: 0; }
.aq-day { margin-bottom: 16px; }
.aq-day-title { margin: 0 0 12px; font-size: 1.25rem; display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.aq-question { padding: 12px 0; border-top: 1px solid var(--line); }
.aq-question:first-of-type { border-top: none; }
.aq-q-text { font-weight: 600; overflow-wrap: anywhere; }
.aq-q-num { display: inline-block; min-width: 1.6em; font-family: var(--font-mono); color: var(--text-dim); font-size: 0.85rem; }
.aq-q-correct { margin: 2px 0 8px; color: var(--text-dim); font-size: 0.92rem; }
.aq-q-correct strong { color: var(--teal-ink, var(--teal)); }
.aq-group { display: flex; gap: 10px; align-items: center; justify-content: space-between; flex-wrap: wrap; padding: 7px 0 7px 12px; border-left: 3px solid var(--line); margin-top: 6px; }
.aq-group.is-pending { border-left-color: var(--gold); }
.aq-group-main { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; min-width: 0; flex: 1; }
.aq-group-text { font-weight: 650; overflow-wrap: anywhere; }
.aq-count { font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-dim); }
.aq-state { font-size: 0.82rem; font-weight: 600; }
.aq-state.is-ok { color: var(--teal-ink, var(--teal)); }
.aq-state.is-bad { color: var(--coral-ink, var(--coral)); }
.aq-actions { display: flex; gap: 6px; }
.aq-yes:hover:not(:disabled) { border-color: var(--teal); }
.aq-no:hover:not(:disabled) { border-color: var(--coral); }
.aq-complaint { margin-bottom: 12px; }
.aq-c-head { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 6px; }
.aq-c-lines { display: grid; gap: 4px; margin-top: 6px; font-size: 0.95rem; }
.aq-label { color: var(--text-dim); font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.06em; }
.aq-c-comment { margin-top: 4px; padding: 8px 12px; border-left: 3px solid var(--gold); background: rgb(var(--gold-rgb) / 0.07); font-style: italic; overflow-wrap: anywhere; }
.aq-c-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; margin-top: 12px; }
.aq-note { flex: 1 1 220px; min-width: 0; }
</style>
