<template>
  <!-- The inside of the dashboard's Dagens quiz box: up to three pages you swipe (or step with the arrows) between -
       today (a ring with your score, or the questions waiting for you), your answers (one dot per question) and your
       week (a bar a day). Pages without anything to show yet are simply left out. -->
  <div class="dqh" :class="{ 'is-pending': pending }" :aria-busy="pending">
    <div class="dqh-stage">
      <button v-if="pages.length > 1" type="button" class="dqh-arrow dqh-arrow--prev" :disabled="current === 0" :aria-label="$t('Forrige')" @click="go(current - 1)">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 18l-6-6 6-6" /></svg>
      </button>

      <div ref="trackEl" class="dqh-track" :class="{ single: pages.length < 2 }" @scroll="onScroll">
        <!-- Today -->
        <div class="dqh-page" :aria-hidden="pageKey !== 'today'">
          <div class="dqh-today">
            <svg class="dqh-ring" viewBox="0 0 60 60" aria-hidden="true">
              <circle class="dqh-ring-track" cx="30" cy="30" r="25" />
              <circle
                v-if="graded"
                class="dqh-ring-fill"
                cx="30" cy="30" r="25"
                :stroke-dasharray="`${ringFill} ${CIRC}`"
                transform="rotate(-90 30 30)"
              />
              <template v-if="graded">
                <text x="30" y="34" class="dqh-ring-big">{{ quiz.score }}/{{ quiz.maxScore }}</text>
              </template>
              <template v-else>
                <text x="30" y="31" class="dqh-ring-big">{{ status === 'SUBMITTED' ? '…' : (quiz?.questionCount || 15) }}</text>
                <text x="30" y="41" class="dqh-ring-small">{{ status === 'SUBMITTED' ? $t('sjekkes') : $t('spørsmål') }}</text>
              </template>
            </svg>
            <div class="dqh-today-text">
              <template v-if="graded">
                <div class="dqh-headline">{{ praise }}</div>
                <div v-if="standing && standing.better !== null" class="dqh-dim">{{ $t('Bedre enn {0} % i dag', [standing.better]) }}</div>
                <span v-if="standing" class="dqh-chip">{{ $t('#{0} av {1}', [standing.rank, standing.of]) }}</span>
              </template>
              <template v-else>
                <div class="dqh-lede">{{ lede }}</div>
                <div v-if="!status || status === 'NOT_STARTED' || status === 'IN_PROGRESS'" class="dqh-dim">{{ $t('Stenger om {0}', [untilMidnight]) }}</div>
              </template>
            </div>
          </div>
        </div>

        <!-- Answers -->
        <div v-if="answers" class="dqh-page" :aria-hidden="pageKey !== 'answers'">
          <div class="dqh-dim">{{ answersSummary }}</div>
          <div class="dqh-dots" :style="{ gridTemplateColumns: `repeat(${answers.length}, 1fr)` }">
            <i v-for="a in answers" :key="a.questionNumber" :class="dotClass(a)" :title="$t('Spørsmål {0}', [a.questionNumber])"></i>
          </div>
          <div class="dqh-dim dqh-small">{{ $t('Se hvilke du bommet på under «Se resultatet».') }}</div>
        </div>

        <!-- Week -->
        <div v-if="week" class="dqh-page" :aria-hidden="pageKey !== 'week'">
          <div class="dqh-week">
            <div v-for="d in week.days" :key="d.label + d.date" class="dqh-day" :class="{ today: d.today, empty: d.score === null }">
              <span class="dqh-day-score">{{ d.score ?? '' }}</span>
              <span class="dqh-day-bar" :style="{ height: d.score === null ? '4px' : Math.max(6, Math.round(d.score / week.max * 44)) + 'px' }"></span>
              <span class="dqh-day-label">{{ d.label }}</span>
            </div>
          </div>
          <div class="dqh-dim">
            {{ $t('Uka: {0} poeng', [week.total]) }}<template v-if="week.rank"> · <span class="dqh-good">{{ $t('#{0} i ukestillingen', [week.rank]) }}</span></template><template v-if="week.streak > 1"> · {{ $t('{0} dager på rad', [week.streak]) }}</template>
          </div>
        </div>
      </div>

      <button v-if="pages.length > 1" type="button" class="dqh-arrow dqh-arrow--next" :disabled="current === pages.length - 1" :aria-label="$t('Neste')" @click="go(current + 1)">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18l6-6-6-6" /></svg>
      </button>
    </div>

    <div v-if="pages.length > 1" class="dqh-foot">
      <span class="dqh-page-name">{{ PAGE_NAMES[pageKey] }}</span>
      <span class="dqh-indicator">
        <button v-for="(p, i) in pages" :key="p" type="button" :class="{ on: i === current }" :aria-label="PAGE_NAMES[p]" @click="go(i)"></button>
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { t as _t } from '../i18n'
import api from '../services/api'
import { sharedRanks } from '../composables/sharedRanks'

const props = defineProps({
  quiz: { type: Object, default: null }, // today's set summary: { id, status, score, maxScore, questionCount }
  lede: { type: String, default: '' },
  pending: { type: Boolean, default: false }
})

const CIRC = 2 * Math.PI * 25
const PAGE_NAMES = { today: _t('I dag'), answers: _t('Svarene'), week: _t('Uka') }

const status = computed(() => props.quiz?.status || null)
const graded = computed(() => status.value === 'GRADED' && props.quiz?.score != null)
const ringFill = computed(() => (graded.value ? (Math.min(1, props.quiz.score / Math.max(1, props.quiz.maxScore)) * CIRC).toFixed(1) : 0))
const praise = computed(() => {
  const share = props.quiz.score / Math.max(1, props.quiz.maxScore)
  if (share >= 0.85) return _t('Knallsterkt!')
  if (share >= 0.6) return _t('Godt jobba!')
  if (share >= 0.35) return _t('Ikke verst')
  return _t('Bedre lykke i morgen')
})

// --- the extras, fetched once we know what to ask for; any that fail just leave their page out
const standing = ref(null) // { rank, of, better } - today's place among everyone on the scoreboard
const answers = ref(null) // [{ questionNumber, verdict, pointsAwarded, yearQuestion }]

async function loadToday(q) {
  standing.value = null
  answers.value = null
  if (!q || !['GRADED', 'SUBMITTED'].includes(q.status)) return
  api.getDailyQuizPlayState(q.id)
    .then(s => { answers.value = s.result?.answers?.length ? s.result.answers : null })
    .catch(() => {})
  if (q.status !== 'GRADED') return
  api.getDailyQuizScoreboard(q.id).then(board => {
    const entries = [...(board.entries || [])].sort((a, b) => b.score - a.score)
    const i = entries.findIndex(e => e.isYou)
    if (i === -1) return
    const ranks = sharedRanks(entries, e => e.score)
    const mine = entries[i].score
    const below = entries.filter(e => e.score < mine).length
    standing.value = { rank: ranks[i], of: entries.length, better: entries.length > 2 ? Math.round(below / (entries.length - 1) * 100) : null }
  }).catch(() => {})
}

function iso(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// The week page is worked out from what the server says about earlier days (fetched once) plus today's quiz
// (which may arrive later, or change once graded) - so it's a computed, not something filled in once.
const weekBase = ref(null) // { scores: Map<date, score>, played: Set<date>, standings: [...] }

async function loadWeek() {
  try {
    const [archive, weekly] = await Promise.all([api.getArchiveDailyQuizzes(), api.getDailyQuizWeekly()])
    weekBase.value = {
      scores: new Map((archive || []).filter(s => s.status === 'GRADED' && s.score != null).map(s => [s.quizDate, s.score])),
      played: new Set((archive || []).filter(s => s.status !== 'NOT_STARTED').map(s => s.quizDate)),
      standings: weekly?.current?.standings || []
    }
  } catch (e) { /* the week page just stays out */ }
}

const week = computed(() => {
  const base = weekBase.value
  if (!base) return null
  const scores = new Map(base.scores)
  const played = new Set(base.played)
  const today = new Date()
  const todayIso = iso(today)
  if (graded.value) scores.set(todayIso, props.quiz.score)
  if (status.value === 'SUBMITTED' || status.value === 'GRADED') played.add(todayIso)

  const monday = new Date(today)
  monday.setDate(today.getDate() - ((today.getDay() + 6) % 7))
  const labels = [_t('M§uke'), _t('T§uke-tir'), _t('O§uke'), _t('T§uke-tor'), _t('F§uke'), _t('L§uke'), _t('S§uke')]
  const days = labels.map((label, i) => {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    const key = iso(d)
    return { label, date: key, today: key === todayIso, score: scores.has(key) ? scores.get(key) : null }
  })
  if (!days.some(d => d.score !== null)) return null

  // days in a row, counting back from today (or from yesterday, while today isn't done yet)
  let streak = 0
  const cursor = new Date(today)
  if (!played.has(todayIso)) cursor.setDate(cursor.getDate() - 1)
  while (played.has(iso(cursor))) { streak++; cursor.setDate(cursor.getDate() - 1) }

  const sorted = [...base.standings].sort((a, b) => b.total - a.total)
  const ranks = sharedRanks(sorted, x => x.total)
  const youAt = sorted.findIndex(x => x.isYou ?? x.you)
  return {
    days,
    max: Math.max(16, ...days.map(d => d.score || 0)),
    total: youAt !== -1 ? sorted[youAt].total : days.reduce((sum, d) => sum + (d.score || 0), 0),
    rank: youAt !== -1 ? ranks[youAt] : null,
    streak
  }
})

watch(() => props.quiz && `${props.quiz.id}|${props.quiz.status}|${props.quiz.score}`, () => loadToday(props.quiz), { immediate: true })
onMounted(loadWeek)

const answersSummary = computed(() => {
  if (!answers.value) return ''
  const pending = answers.value.filter(a => a.verdict === 'PENDING').length
  if (pending) return _t('{0} svar venter på sjekk', [pending])
  const right = answers.value.filter(a => a.verdict === 'CORRECT').length
  const wrong = answers.value.length - right
  const year = answers.value.find(a => a.yearQuestion && a.verdict === 'CORRECT')
  const base = _t('{0} riktige · {1} feil', [right, wrong])
  return year ? `${base} · ${_t('årstallet ga {0} poeng', [year.pointsAwarded ?? 0])}` : base
})
function dotClass(a) {
  if (a.verdict === 'PENDING') return 'pending'
  if (a.verdict !== 'CORRECT') return 'wrong'
  return a.yearQuestion && a.pointsAwarded === 1 ? 'partial' : 'right'
}

// --- paging
const pages = computed(() => ['today', ...(answers.value ? ['answers'] : []), ...(week.value ? ['week'] : [])])
const current = ref(0)
const pageKey = computed(() => pages.value[current.value] || 'today')
const trackEl = ref(null)
function go(i) {
  const n = Math.max(0, Math.min(pages.value.length - 1, i))
  current.value = n
  trackEl.value?.scrollTo({ left: n * trackEl.value.clientWidth, behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' })
}
function onScroll() {
  const el = trackEl.value
  if (el && el.clientWidth) current.value = Math.round(el.scrollLeft / el.clientWidth)
}
watch(() => pages.value.length, len => { if (current.value >= len) go(len - 1) })

// --- "open for another 4 t 17 min"
const now = ref(new Date())
let timer = null
onMounted(() => { timer = setInterval(() => { now.value = new Date() }, 60000) })
onBeforeUnmount(() => clearInterval(timer))
const untilMidnight = computed(() => {
  const midnight = new Date(now.value)
  midnight.setHours(24, 0, 0, 0)
  const mins = Math.max(1, Math.round((midnight - now.value) / 60000))
  const h = Math.floor(mins / 60)
  return h > 0 ? _t('{0} t {1} min', [h, mins % 60]) : _t('{0} min', [mins])
})
</script>
