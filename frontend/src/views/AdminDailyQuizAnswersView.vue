<template>
  <div class="dq-page dq-page--wide rq">
    <header class="rq-head">
      <div>
        <span class="dq-eyebrow">Admin</span>
        <h1 class="dq-title">{{ $t('Rett svar') }}</h1>
      </div>
      <div class="rq-save" :class="'is-' + review.saveState.value" role="status">
        <template v-if="review.saveState.value === 'saved'">✓ {{ $t('Alt er lagret') }}</template>
        <template v-else-if="review.saveState.value === 'waiting'">
          {{ $t('{0} endringer lagres snart…¦{0} endringer lagres snart…', [review.queue.value.length]) }}
          <button type="button" @click="review.flush()">{{ $t('Lagre nå') }}</button>
        </template>
        <template v-else-if="review.saveState.value === 'saving'">{{ $t('Lagrer…') }}</template>
        <template v-else>
          {{ $t('Kunne ikke lagre') }}
          <button type="button" @click="review.flush()">{{ $t('Prøv igjen') }}</button>
        </template>
      </div>
    </header>

    <div class="rq-tabs" role="tablist">
      <button type="button" role="tab" :aria-selected="tab === 'answers'" :class="{ 'is-on': tab === 'answers' }" @click="tab = 'answers'">
        {{ $t('Svar') }} <span class="rq-badge" v-if="review.pendingRows.value.length">{{ review.pendingRows.value.length }}</span>
      </button>
      <button type="button" role="tab" :aria-selected="tab === 'complaints'" :class="{ 'is-on': tab === 'complaints' }" @click="openComplaints">
        {{ $t('Klager') }} <span class="rq-badge" v-if="complaints.length">{{ complaints.length }}</span>
      </button>
      <router-link to="/admin/daily-quiz-review" class="rq-old">{{ $t('Gjennomgå per spiller') }} →</router-link>
    </div>

    <div v-if="review.error.value" class="banner error">{{ review.error.value }}</div>
    <LoadingSkeleton v-if="review.loading.value" />

    <!-- ================================================================ answers -->
    <template v-else-if="tab === 'answers'">

      <!-- one at a time -->
      <AnswerTriage
        v-if="mode === 'triage'"
        :rows="review.pendingRows.value"
        :all-rows="review.rows.value"
        :remember="review.remember.value"
        :can-undo="review.canUndo.value"
        :total="roundTotal"
        :focus-id="focusId"
        @update:remember="review.remember.value = $event"
        @decide="(row, correct) => decide([row], correct, correct ? row.g.text + ' ✓' : row.g.text + ' ✕')"
        @undo="undo"
        @exit="mode = 'list'"
      />

      <!-- overview -->
      <template v-else>
        <section v-if="review.pendingRows.value.length" class="rq-hero">
          <div class="rq-stats">
            <div><strong>{{ review.pendingAnswers.value }}</strong><span>{{ $t('svar venter') }}</span></div>
            <div><strong>{{ review.pendingRows.value.length }}</strong><span>{{ $t('ulike å ta stilling til') }}</span></div>
            <div v-if="review.typoRows.value.length"><strong>{{ review.typoRows.value.length }}</strong><span>{{ $t('ser ut som skrivefeil') }}</span></div>
          </div>
          <div class="rq-hero-actions">
            <button class="btn btn-primary" @click="startTriage">▶ {{ $t('Start hurtigretting') }}</button>
            <button v-if="review.typoRows.value.length" class="btn btn-secondary" @click="acceptTypos(review.typoRows.value)">
              ✓ {{ $t('Godta skrivefeil ({0})', [review.typoRows.value.length]) }}
            </button>
          </div>
          <p class="rq-hint">{{ $t('Hurtigretting viser ett svar om gangen - bruk tastene R (riktig) og F (feil). Eller huk av flere i listen under og rett dem samlet.') }}</p>
        </section>

        <div class="rq-toolbar">
          <label class="rq-switch">
            <input type="checkbox" v-model="review.remember.value" />
            <span><strong>{{ $t('Husk valget') }}</strong> <small>{{ $t('- samme svar rettes automatisk neste gang') }}</small></span>
          </label>
          <label class="rq-switch">
            <input type="checkbox" v-model="showDecided" />
            <span>{{ $t('Vis også avgjorte') }}</span>
          </label>
        </div>

        <div v-if="!days.length" class="rq-empty">
          <div class="rq-empty-mark" aria-hidden="true">✓</div>
          <strong>{{ $t('Alt er rettet') }}</strong>
          <span>{{ $t('Ingen svar venter på gjennomgang.') }}</span>
        </div>

        <template v-for="day in days" :key="day.setId">
          <h2 class="rq-day">{{ formatLong(day.quizDate) }}</h2>

          <section v-for="q in day.questions" :key="q.questionId" class="rq-q">
            <header class="rq-q-head">
              <label class="rq-check" :title="$t('Velg alle ventende')">
                <input type="checkbox" :checked="allSelected(q)" :indeterminate.prop="someSelected(q)" :disabled="!pendingIn(q).length" @change="toggleAll(q, $event.target.checked)" />
              </label>
              <div class="rq-q-main">
                <div class="rq-q-text"><span class="rq-q-num">{{ q.number }}</span>{{ q.questionText }}</div>
                <div class="rq-q-right">{{ $t('Riktig svar:') }} <strong>{{ q.correctAnswer }}</strong></div>
              </div>
              <div class="rq-q-actions">
                <button v-if="typosIn(q).length" class="rq-link rq-link--yes" @click="acceptTypos(typosIn(q))">✓ {{ $t('Skrivefeil ({0})', [typosIn(q).length]) }}</button>
                <button v-if="pendingIn(q).length" class="rq-link rq-link--no" @click="decide(pendingIn(q), false, $t('{0} svar markert som feil', [pendingIn(q).length]))">✕ {{ $t('Resten er feil ({0})', [pendingIn(q).length]) }}</button>
              </div>
            </header>

            <ul class="rq-rows">
              <li v-for="r in q.rows" :key="r.id" class="rq-row" :class="['is-' + r.state, { 'is-selected': selected.has(r.id) }]">
                <label class="rq-check">
                  <input type="checkbox" :checked="selected.has(r.id)" @change="toggle(r.id)" />
                </label>
                <div class="rq-row-main">
                  <span class="rq-text">{{ r.g.text }}</span>
                  <span class="rq-count">{{ r.g.count }}×</span>
                  <span v-if="r.g.likelyTypo && r.state === 'pending'" class="rq-chip rq-chip--hint">{{ $t('skrivefeil?') }}</span>
                  <span v-if="r.g.rule !== null" class="rq-chip" :title="$t('Husket fra før - rettes automatisk')">{{ $t('husket') }}</span>
                </div>
                <span class="rq-state">{{ stateLabel(r.state) }}</span>
                <div class="rq-btns">
                  <button class="rq-btn rq-btn--yes" :class="{ 'is-on': r.state === 'correct' }" :aria-label="$t('Riktig§AdminAnswers')" :title="$t('Riktig§AdminAnswers')" @click="decide([r], true, r.g.text + ' ✓')">✓</button>
                  <button class="rq-btn rq-btn--no" :class="{ 'is-on': r.state === 'wrong' }" :aria-label="$t('Feil§AdminAnswers')" :title="$t('Feil§AdminAnswers')" @click="decide([r], false, r.g.text + ' ✕')">✕</button>
                </div>
              </li>
            </ul>
          </section>
        </template>
      </template>
    </template>

    <!-- ================================================================ complaints -->
    <template v-else>
      <div v-if="!complaints.length" class="rq-empty">
        <div class="rq-empty-mark" aria-hidden="true">✓</div>
        <strong>{{ $t('Ingen klager') }}</strong>
        <span>{{ $t('Ingen spillere venter på ny vurdering.') }}</span>
      </div>

      <TransitionGroup name="rq-list" tag="div">
        <article v-for="c in complaints" :key="c.id" class="rq-complaint">
          <header class="rq-c-head">
            <strong>{{ c.playerName }}</strong>
            <span class="rq-count">{{ formatLong(c.quizDate) }}</span>
            <span v-if="c.decidedBy" class="rq-chip">{{ decidedLabel(c.decidedBy) }}</span>
          </header>
          <div class="rq-c-q">{{ c.questionText }}</div>
          <div class="rq-c-versus">
            <div class="rq-c-box rq-c-box--right"><span>{{ $t('Riktig svar:') }}</span><strong>{{ c.correctAnswer }}</strong></div>
            <div class="rq-c-box rq-c-box--given"><span>{{ $t('Spillerens svar:') }}</span><strong>{{ c.playerAnswer }}</strong></div>
          </div>
          <blockquote v-if="c.comment" class="rq-c-comment">«{{ c.comment }}»</blockquote>
          <div class="rq-c-actions">
            <button class="btn btn-primary btn-sm" @click="acceptComplaint(c)">✓ {{ $t('Godta svaret') }}</button>
            <label class="rq-switch rq-switch--small">
              <input type="checkbox" v-model="acceptForEveryone[c.id]" />
              <span>{{ $t('også for alle som skrev det samme, og husk det') }}</span>
            </label>
          </div>
          <div class="rq-c-reject">
            <input type="text" class="adv-input" v-model="notes[c.id]" maxlength="300" :placeholder="$t('Kort begrunnelse til spilleren (valgfritt)')" @keydown.enter="rejectComplaint(c)" />
            <button class="btn btn-secondary btn-sm" @click="rejectComplaint(c)">✕ {{ $t('Avslå') }}</button>
          </div>
        </article>
      </TransitionGroup>
    </template>

    <!-- ================================================================ floating bar: undo + selection -->
    <div class="rq-dock" v-if="tab === 'answers'">
      <Transition name="rq-pop">
        <div v-if="undoNote && review.canUndo.value && mode === 'list'" class="rq-snack">
          <span>{{ undoNote }}</span>
          <button type="button" @click="undo">↶ {{ $t('Angre') }}</button>
        </div>
      </Transition>
      <Transition name="rq-pop">
        <div v-if="selected.size && mode === 'list'" class="rq-selbar">
          <strong>{{ $t('{0} valgt', [selected.size]) }}</strong>
          <button class="rq-sel-yes" @click="decideSelected(true)">✓ {{ $t('Riktig§AdminAnswers') }}</button>
          <button class="rq-sel-no" @click="decideSelected(false)">✕ {{ $t('Feil§AdminAnswers') }}</button>
          <button class="rq-sel-clear" @click="selected.clear()">{{ $t('Fjern valg') }}</button>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import LoadingSkeleton from '../components/LoadingSkeleton.vue'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute } from 'vue-router'
import { t as _t, locale } from '../i18n'
import api from '../services/api'
import toast from '../services/toast'
import AnswerTriage from '../components/AnswerTriage.vue'
import { useAnswerReview } from '../composables/useAnswerReview'

// The admin's way through the daily quiz answers, built for speed: decisions show at once and are saved in the background,
// one answer at a time with the keyboard ("hurtigretting"), or several ticked at once in the list. Plus the complaints.
const route = useRoute()
const review = useAnswerReview()
const tab = ref(route.query.tab === 'complaints' ? 'complaints' : 'answers')
const mode = ref('list') // 'list' | 'triage'
const showDecided = ref(false)
const selected = reactive(new Set())
const undoNote = ref('')
const focusId = ref(null)
const roundTotal = ref(0)
let undoTimer = null

const complaints = ref([])
const acceptForEveryone = reactive({})
const notes = reactive({})

onMounted(async () => {
  await Promise.all([review.load(), loadComplaints()])
})
onBeforeUnmount(() => {
  review.dispose()
  clearTimeout(undoTimer)
  window.removeEventListener('beforeunload', warnBeforeUnload)
})

// whatever is still on its way to the server goes before the admin leaves the page
onBeforeRouteLeave(async () => { await review.flush() })
function warnBeforeUnload(e) {
  if (review.queue.value.length) {
    e.preventDefault()
    e.returnValue = ''
  }
}
window.addEventListener('beforeunload', warnBeforeUnload)

// ---- the list: day -> question -> answers (what was waiting when the page loaded stays put while it is worked through) ----

const days = computed(() => {
  const byQuestion = new Map()
  for (const r of review.rows.value) {
    if (!showDecided.value && !r.wasPending) continue
    const k = `${r.day.setId}|${r.q.questionId}`
    if (!byQuestion.has(k)) byQuestion.set(k, { day: r.day, q: r.q, rows: [] })
    byQuestion.get(k).rows.push(r)
  }
  const out = []
  for (const { day, q, rows } of byQuestion.values()) {
    let d = out.find(x => x.setId === day.setId)
    if (!d) out.push(d = { setId: day.setId, quizDate: day.quizDate, questions: [] })
    d.questions.push({ ...q, rows })
  }
  return out
})

const pendingIn = q => q.rows.filter(r => r.state === 'pending')
const typosIn = q => pendingIn(q).filter(r => r.g.likelyTypo)

function stateLabel(state) {
  return { pending: _t('venter'), correct: _t('riktig'), wrong: _t('feil'), mixed: _t('delvis') }[state]
}

// ---- selecting ----

function toggle(id) {
  if (selected.has(id)) selected.delete(id)
  else selected.add(id)
}
const allSelected = q => pendingIn(q).length > 0 && pendingIn(q).every(r => selected.has(r.id))
const someSelected = q => pendingIn(q).some(r => selected.has(r.id)) && !allSelected(q)
function toggleAll(q, on) {
  for (const r of pendingIn(q)) {
    if (on) selected.add(r.id)
    else selected.delete(r.id)
  }
}
const selectedRows = () => review.rows.value.filter(r => selected.has(r.id))
function decideSelected(correct) {
  const rows = selectedRows()
  decide(rows, correct, correct ? _t('{0} svar markert som riktig', [rows.length]) : _t('{0} svar markert som feil', [rows.length]))
  selected.clear()
}

// ---- deciding ----

function decide(rows, correct, note) {
  focusId.value = null
  const batch = review.decide(rows, correct, note)
  if (!batch) return
  for (const r of rows) selected.delete(r.id)
  undoNote.value = note
  clearTimeout(undoTimer)
  undoTimer = setTimeout(() => { undoNote.value = '' }, 7000)
}

function acceptTypos(rows) {
  decide(rows, true, _t('{0} skrivefeil godtatt', [rows.length]))
}

function undo() {
  const last = review.lastBatch.value
  const restored = last ? last.items.map(i => i.id) : []
  if (review.undo()) {
    focusId.value = restored[0] || null
    undoNote.value = ''
  }
}

function startTriage() {
  roundTotal.value = review.pendingRows.value.length
  focusId.value = null
  mode.value = 'triage'
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// leaving the one-at-a-time view: make sure the list shows the true picture
watch(mode, async value => {
  if (value === 'list') {
    await review.flush()
    await review.load()
  }
})

// ---- complaints ----

async function loadComplaints() {
  try {
    complaints.value = await api.adminListDailyQuizComplaints()
    for (const x of complaints.value) if (acceptForEveryone[x.id] === undefined) acceptForEveryone[x.id] = true
  } catch (e) {
    review.error.value = e.response?.data?.message || _t('Kunne ikke laste klagene.')
  }
}

async function openComplaints() {
  tab.value = 'complaints'
  await review.flush()
  await loadComplaints()
}

// removed from the list at once; put back if the server says no
async function resolveComplaint(c, call, okMessage) {
  const at = complaints.value.findIndex(x => x.id === c.id)
  if (at < 0) return
  complaints.value.splice(at, 1)
  try {
    const result = await call()
    toast.show(okMessage(result))
  } catch (e) {
    complaints.value.splice(at, 0, c)
    review.error.value = e.response?.data?.message || _t('Det gikk ikke - prøv igjen.')
  }
}

const acceptComplaint = c => resolveComplaint(c, () => api.adminAcceptDailyQuizComplaint(c.id, !!acceptForEveryone[c.id]),
  r => _t('Godtatt - {0} svar rettet.', [r.answers]))

const rejectComplaint = c => resolveComplaint(c, () => api.adminRejectDailyQuizComplaint(c.id, notes[c.id] || ''),
  () => _t('Klagen er avslått.'))

// ---- bits ----

function formatLong(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString(locale, { weekday: 'long', day: 'numeric', month: 'long' })
}

function decidedLabel(source) {
  return { EXACT: _t('rettet automatisk'), NORMALIZED: _t('rettet automatisk'), RULE: _t('husket regel'), ADMIN: _t('rettet av admin'), COMPLAINT: _t('rettet av admin') }[source] || source
}
</script>

<style scoped>
.rq { padding-bottom: 130px; }

/* ---------- header and tabs ---------- */
.rq-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 14px; flex-wrap: wrap; margin-bottom: 14px; }
.rq-head .dq-title { margin: 2px 0 0; }
.rq-save { font-size: 0.85rem; padding: 6px 12px; border-radius: 14px; border: 1.5px solid var(--line); color: var(--text-dim); display: inline-flex; gap: 10px; align-items: center; }
.rq-save.is-saved { color: var(--teal-ink, var(--teal)); border-color: rgb(var(--teal-rgb) / 0.4); }
.rq-save.is-waiting, .rq-save.is-saving { color: var(--text); border-color: var(--primary); }
.rq-save.is-failed { color: var(--coral-ink, var(--coral)); border-color: var(--coral); }
.rq-save button { background: none; border: none; font: inherit; font-weight: 700; text-decoration: underline; cursor: pointer; color: inherit; padding: 0; }

.rq-tabs { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin: 0 0 18px; }
.rq-tabs button { display: inline-flex; gap: 8px; align-items: center; padding: 9px 18px; background: transparent; color: var(--text); border: 1px solid var(--rule); border-radius: var(--radius-sm); font-family: var(--font-body); font-weight: 600; cursor: pointer; }
.rq-tabs button.is-on { background: var(--primary); border-color: var(--primary); color: var(--on-primary); }
.rq-badge { font-size: 0.75rem; min-width: 22px; text-align: center; padding: 1px 6px; border-radius: 10px; background: var(--primary); color: var(--on-primary); }
.rq-tabs button.is-on .rq-badge { background: var(--on-primary); color: var(--primary); }
.rq-old { margin-left: auto; color: var(--text-dim); font-size: 0.9rem; }

/* ---------- overview hero ---------- */
.rq-hero { border: 1px solid var(--rule); border-radius: var(--radius-sm); background: var(--ink-raised); padding: 20px 22px; margin-bottom: 16px; box-shadow: var(--shadow-card); }
.rq-stats { display: flex; gap: 28px; flex-wrap: wrap; margin-bottom: 16px; }
.rq-stats div { display: flex; flex-direction: column; }
.rq-stats strong { font-family: var(--font-display); font-size: 2.1rem; line-height: 1; }
.rq-stats span { font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--text-dim); margin-top: 4px; }
.rq-hero-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.rq-hint { margin: 12px 0 0; font-size: 0.88rem; color: var(--text-dim); }

.rq-toolbar { display: flex; gap: 22px; flex-wrap: wrap; align-items: center; margin: 0 0 6px; }
.rq-switch { display: inline-flex; gap: 8px; align-items: center; margin: 0; text-transform: none; letter-spacing: normal; font-weight: 400; color: var(--text); font-size: 0.92rem; cursor: pointer; }
.rq-switch input { width: 18px; height: 18px; flex-shrink: 0; }
.rq-switch small { color: var(--text-dim); font-size: 0.85rem; }
.rq-switch--small { font-size: 0.85rem; }

.rq-empty { display: flex; flex-direction: column; align-items: center; gap: 4px; text-align: center; padding: 44px 16px; border: 2px dashed var(--line); border-radius: var(--radius-sm); color: var(--text-dim); margin-top: 14px; }
.rq-empty strong { color: var(--text); font-size: 1.15rem; }
.rq-empty-mark { width: 52px; height: 52px; display: grid; place-items: center; border-radius: 50%; background: var(--teal); color: #fff; font-size: 1.6rem; margin-bottom: 6px; }

/* ---------- the list ---------- */
.rq-day { margin: 26px 0 10px; font-size: 1.25rem; }
.rq-day::first-letter { text-transform: uppercase; }
.rq-q { border: 1px solid var(--rule); border-radius: var(--radius-sm); background: var(--ink-raised); margin-bottom: 14px; overflow: hidden; }
.rq-q-head { display: grid; grid-template-columns: auto 1fr auto; gap: 12px; align-items: start; padding: 13px 16px; border-bottom: 1px solid var(--line); background: rgb(var(--fg-rgb) / 0.03); }
.rq-q-text { font-weight: 650; overflow-wrap: anywhere; }
.rq-q-num { display: inline-block; min-width: 1.7em; font-size: 0.8rem; color: var(--text-dim); }
.rq-q-right { font-size: 0.88rem; color: var(--text-dim); margin-top: 2px; padding-left: 1.7em; }
.rq-q-right strong { color: var(--teal-ink, var(--teal)); }
.rq-q-actions { display: flex; gap: 6px; flex-wrap: wrap; justify-content: flex-end; }
.rq-link { background: none; border: 1.5px solid var(--line); border-radius: var(--radius-sm); padding: 4px 10px; font: inherit; font-size: 0.82rem; font-weight: 600; cursor: pointer; color: var(--text); white-space: nowrap; }
.rq-link--yes:hover { border-color: var(--teal); color: var(--teal-ink, var(--teal)); }
.rq-link--no:hover { border-color: var(--coral); color: var(--coral-ink, var(--coral)); }

.rq-check { display: grid; place-items: center; margin: 0; padding: 2px; cursor: pointer; }
.rq-check input { width: 18px; height: 18px; cursor: pointer; }

.rq-rows { list-style: none; margin: 0; padding: 0; }
.rq-row { display: grid; grid-template-columns: auto 1fr auto auto; gap: 12px; align-items: center; padding: 9px 16px; border-top: 1px solid var(--line); border-left: 4px solid var(--stone); transition: background 0.15s ease, border-color 0.15s ease, opacity 0.2s ease; }
.rq-row:first-child { border-top: none; }
.rq-row.is-correct { border-left-color: var(--teal); background: rgb(var(--teal-rgb) / 0.06); }
.rq-row.is-wrong { border-left-color: var(--coral); background: rgb(var(--coral-rgb) / 0.05); }
.rq-row.is-correct .rq-text, .rq-row.is-wrong .rq-text { opacity: 0.75; }
.rq-row.is-mixed { border-left-color: var(--line); }
.rq-row.is-selected { background: rgb(var(--primary-rgb) / 0.08); }
.rq-row-main { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; min-width: 0; }
.rq-text { font-weight: 650; font-size: 1.02rem; overflow-wrap: anywhere; }
.rq-count { font-size: 0.8rem; color: var(--text-dim); }
.rq-chip { font-size: 0.74rem; padding: 1px 8px; border: 1.5px solid var(--line); border-radius: 10px; color: var(--text-dim); white-space: nowrap; }
.rq-chip--hint { border-color: rgb(var(--primary-rgb) / 0.45); color: var(--primary); background: rgb(var(--primary-rgb) / 0.07); font-weight: 600; }
.rq-state { font-size: 0.8rem; font-weight: 600; color: var(--text-dim); min-width: 3.5em; text-align: right; }
.rq-row.is-correct .rq-state { color: var(--teal-ink, var(--teal)); }
.rq-row.is-wrong .rq-state { color: var(--coral-ink, var(--coral)); }
.rq-btns { display: flex; gap: 6px; }
.rq-btn { width: 40px; height: 36px; display: grid; place-items: center; border: 1px solid var(--rule); border-radius: var(--radius-sm); background: var(--ink-raised); color: var(--text); font-size: 1.05rem; font-weight: 700; cursor: pointer; transition: background 0.1s ease, color 0.1s ease, transform 0.08s ease; }
.rq-btn:active { transform: scale(0.93); }
.rq-btn--yes:hover, .rq-btn--yes.is-on { background: var(--teal); border-color: var(--teal); color: #fff; }
.rq-btn--no:hover, .rq-btn--no.is-on { background: var(--coral); border-color: var(--coral); color: #fff; }

/* ---------- floating bar ---------- */
.rq-dock { position: fixed; left: 0; right: 0; bottom: 18px; display: flex; flex-direction: column; align-items: center; gap: 10px; pointer-events: none; z-index: 40; padding: 0 12px; }
.rq-dock > * { pointer-events: auto; }
.rq-snack { display: flex; gap: 16px; align-items: center; background: var(--text); color: var(--ink-raised); padding: 10px 16px; border-radius: var(--radius-sm); font-size: 0.92rem; box-shadow: 0 6px 22px rgb(0 0 0 / 0.25); max-width: 100%; }
.rq-snack button { background: none; border: none; color: inherit; font: inherit; font-weight: 700; text-decoration: underline; cursor: pointer; white-space: nowrap; }
.rq-selbar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; justify-content: center; background: var(--ink-raised); border: 1px solid var(--rule); border-radius: var(--radius-sm); padding: 10px 14px; box-shadow: var(--shadow-card); }
.rq-selbar button { padding: 7px 16px; border: 1px solid var(--rule); border-radius: var(--radius-sm); background: var(--ink-raised); color: var(--text); font: inherit; font-weight: 700; cursor: pointer; }
.rq-sel-yes:hover { background: var(--teal); border-color: var(--teal); color: #fff; }
.rq-sel-no:hover { background: var(--coral); border-color: var(--coral); color: #fff; }
.rq-selbar .rq-sel-clear { border: none; background: none; color: var(--text-dim); font-weight: 500; text-decoration: underline; }
.rq-pop-enter-active, .rq-pop-leave-active { transition: transform 0.18s ease, opacity 0.18s ease; }
.rq-pop-enter-from, .rq-pop-leave-to { transform: translateY(14px); opacity: 0; }

/* ---------- complaints ---------- */
.rq-complaint { border: 1px solid var(--rule); border-radius: var(--radius-sm); background: var(--ink-raised); padding: 16px 18px; margin-bottom: 12px; }
.rq-c-head { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 6px; }
.rq-c-q { font-weight: 600; overflow-wrap: anywhere; margin-bottom: 10px; }
.rq-c-versus { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.rq-c-box { border: 1.5px solid var(--line); border-radius: var(--radius-sm); padding: 9px 12px; min-width: 0; }
.rq-c-box span { display: block; font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.07em; color: var(--text-dim); font-weight: 700; }
.rq-c-box strong { font-family: var(--font-display); font-size: 1.15rem; overflow-wrap: anywhere; }
.rq-c-box--right { background: rgb(var(--teal-rgb) / 0.07); }
.rq-c-box--given { background: rgb(var(--primary-rgb) / 0.06); border-color: rgb(var(--primary-rgb) / 0.4); }
.rq-c-comment { margin: 10px 0 0; padding: 9px 14px; border-left: 3px solid var(--primary); background: rgb(var(--primary-rgb) / 0.05); font-style: italic; overflow-wrap: anywhere; }
.rq-c-actions { display: flex; gap: 14px; align-items: center; flex-wrap: wrap; margin-top: 14px; }
.rq-c-reject { display: flex; gap: 8px; align-items: center; margin-top: 10px; }
.rq-c-reject input { flex: 1; min-width: 0; }
.rq-list-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.rq-list-leave-to { opacity: 0; transform: translateX(30px); }

@media (max-width: 640px) {
  .rq { padding-bottom: 170px; }
  .rq-dock { bottom: 76px; } /* above the phone's bottom navigation */
  .rq-q-head { grid-template-columns: auto 1fr; }
  .rq-q-actions { grid-column: 1 / -1; justify-content: flex-start; }
  .rq-row { grid-template-columns: auto 1fr auto; row-gap: 4px; }
  .rq-state { display: none; }
  .rq-c-versus { grid-template-columns: 1fr; }
  .rq-hero { padding: 16px; }
}
@media (prefers-reduced-motion: reduce) { .rq-row, .rq-pop-enter-active, .rq-pop-leave-active, .rq-list-leave-active { transition: none; } }
</style>
