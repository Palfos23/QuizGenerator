<template>
  <div class="dashboard">
    <p class="dashboard-welcome">{{ $t('Velkommen tilbake,') }} <strong>{{ auth.state.displayName }}</strong></p>

    <div class="dashboard-hero-row">
      <section class="dashboard-hero">
        <h1>{{ $t('Lag en quiz') }}</h1>
        <p class="page-subtitle landing-lede">
          {{ $t('Lag en egen quiz for venner, kolleger eller quizkvelden på lokalpuben.') }}
        </p>
        <div class="dashboard-hero-actions">
          <router-link to="/generate" class="btn btn-primary btn-lg">{{ $t('Lag en quiz →') }}</router-link>
        </div>
      </section>

      <!-- Not folded into the "Create a quiz" hero above - joining a game
           someone else is hosting has nothing to do with making a quiz, it's
           the entry point into the games/rooms side of the app. Still gets
           top billing of its own instead, right beside the other hero. -->
      <section class="dashboard-join-card">
        <h2>{{ $t('Bli med i et spill') }}</h2>
        <p class="page-subtitle">{{ $t('Har du fått en romkode fra en vert? Hopp rett inn.') }}</p>
        <router-link to="/join" class="btn btn-secondary btn-lg">{{ $t('Bli med med kode →') }}</router-link>
      </section>
    </div>

    <template v-for="section in dashboardSections" :key="section.title">
      <h2 class="dashboard-section-title">{{ section.title }}</h2>
      <div class="dashboard-features">
        <!-- Grouped into pairs so mobile can lay each pair out as its own
             2-tall column and scroll horizontally between columns (see
             .dashboard-feature-column's mobile rule in style.css). On desktop
             display:contents makes this wrapper invisible to layout, so cards
             flow straight into the existing wrapping grid as before. -->
        <div v-for="(column, i) in section.columns" :key="i" class="dashboard-feature-column">
          <router-link
            v-for="card in column"
            :key="card.to"
            :to="card.to"
            class="dashboard-feature-card"
            :style="{ '--card-accent': card.accent }"
          >
            <h3>{{ card.title }}</h3>
            <p>{{ card.description }}</p>
            <span class="dashboard-feature-play">{{ $t('Spill →') }}</span>
          </router-link>
        </div>
      </div>
    </template>

    <!-- Deliberately not another small tile in the grid above - buried among
         12 game cards it was easy to miss, and it's not a "way to play" like
         the rest of this page. A full-width, always-visible strip is the
         obvious/findable version of the same link. -->
    <router-link to="/report-problem" class="dashboard-report-callout">
      <div>
        <h3>{{ $t('Funnet en feil eller noe forvirrende?') }}</h3>
        <p>{{ $t('Si fra, så tar vi en titt.') }}</p>
      </div>
      <span class="btn btn-secondary">{{ $t('Meld fra om et problem →') }}</span>
    </router-link>

    <!-- "Your quiz has been reviewed": an admin has finished checking answers the player had waiting, so
         their score is final. Shown until they've dismissed it or opened the quiz (that's tracked on
         the server, so it follows them across devices) - and instead of the nudge below, never both. -->
    <div v-if="reviewNotifications.length" class="modal-backdrop" @click.self="dismissReviewNotifications">
      <div class="modal dq-modal" style="max-width:400px; text-align:center;" role="dialog" aria-modal="true" :aria-label="$t('Dagens quiz er gjennomgått')">
        <div class="dq-empty-icon" style="margin:0 auto 14px;">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>
        </div>
        <h2 class="dq-modal-title" style="margin-bottom:6px;"> {{ reviewNotifications.length === 1 ? $t('Quizen din er') : $t('Quizene dine er') }} {{ $t('gjennomgått') }}</h2>
        <p class="dq-modal-sub">{{ $t('En administrator er ferdig med å sjekke svarene dine - poengsummen din er endelig.') }}</p>
        <div class="dq-list" style="margin:16px 0 20px;">
          <router-link
            v-for="n in reviewNotifications"
            :key="n.setId"
            :to="`/daily-quiz/${n.setId}`"
            class="dq-row"
            @click="dismissReviewNotifications"
          >
            <div class="dq-row-main" style="text-align:left;">
              <div class="dq-row-title">{{ formatQuizDate(n.quizDate) }}</div>
              <div class="dq-row-meta">{{ $t('Se svarene dine§DashboardViewT') }}</div>
            </div>
            <div class="dq-row-end">
              <span class="dq-pill dq-pill--ok">{{ n.score }} / {{ n.maxScore }}</span>
              <svg class="dq-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6" /></svg>
            </div>
          </router-link>
        </div>
        <button class="btn btn-secondary" style="width:100%;" @click="dismissReviewNotifications">{{ $t('Lukk§DashboardViewT') }}</button>
      </div>
    </div>

    <!-- One nudge per day, per browser (see dismissDailyQuizNudge) - not on
         every dashboard visit, just the first one that finds today's quiz
         still un-submitted. Guests/admins never trigger the check that would
         show this at all (see the onMounted guard below). -->
    <div v-if="showDailyQuizNudge" class="modal-backdrop" @click.self="dismissDailyQuizNudge">
      <div class="modal" style="max-width:380px; text-align:center;">
        <h2 style="margin-top:0;">{{ $t('Dagens quiz venter på deg') }}</h2>
        <p class="page-subtitle">
          {{ dailyQuizNudge.questionCount }} {{ $t('spørsmål i pubquiz-stil -') }}
          {{ dailyQuizNudge.status === 'IN_PROGRESS' ? $t('fortsett der du slapp.') : $t('tar bare noen minutter.') }}
        </p>
        <div style="display:flex; gap:8px; margin-top:16px;">
          <button class="btn btn-secondary" style="flex:1;" @click="dismissDailyQuizNudge">{{ $t('Kanskje senere') }}</button>
          <router-link :to="`/daily-quiz/${dailyQuizNudge.id}`" class="btn btn-primary" style="flex:1;" @click="dismissDailyQuizNudge">
            {{ dailyQuizNudge.status === 'IN_PROGRESS' ? $t('Fortsett§DashboardViewM') : $t('Spill nå') }}
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, onMounted, ref } from 'vue'
import auth from '../services/auth'
import api from '../services/api'

// Cards are grouped into named sections (rendered as their own labeled block,
// see dashboardSections below) instead of one flat 13-card grid - weekly
// solo quizzes, the pass-the-device/online games, and the non-game "manage
// your quizzes" actions each read as their own thing now, not just a color.
// "Report a problem" isn't in any section - see the callout below instead.
const WEEKLY_QUIZ_ACCENT = 'var(--teal)'
const GAME_ACCENT = 'var(--violet)'
const QUIZ_MANAGEMENT_ACCENT = 'var(--gold)'

const dashboardSectionDefs = [
  {
    title: _t('Daglige og ukentlige quizer'),
    cards: [
      { to: '/daily-quiz', title: _t('Dagens quiz§DashboardViewJ'), description: _t('15 tilfeldige spørsmål i pubquiz-stil - fritekst, et nytt sett hver dag.'), accent: WEEKLY_QUIZ_ACCENT },
      { to: '/weekly-grid', title: _t('Ukens grid§DashboardViewJ'), description: _t('Gjett alle svarene som passer ukens tema før du går tom for liv.'), accent: WEEKLY_QUIZ_ACCENT },
      { to: '/starting-xi', title: 'Starting XI', description: _t('Gjett en hel startellever, posisjon for posisjon, før ukens brett går tom for liv.'), accent: WEEKLY_QUIZ_ACCENT }
    ]
  },
  {
    title: _t('Spill§DashboardViewJ'),
    cards: [
      { to: '/grid-battle', title: 'Grid Battle', description: _t('En flerspillerversjon av Ukens grid der enheten sendes rundt - bytt på, eller mist et liv i forsøket.'), accent: GAME_ACCENT },
      { to: '/starting-xi-battle', title: 'XI Battle', description: _t('Samme idé som Grid Battle, men for et Starting XI-brett - bytt på å nevne laget.'), accent: GAME_ACCENT },
      { to: '/tension', title: 'Tension', description: _t('En selskapsquiz der enheten sendes rundt - gå for en gjetning med høy verdi, eller spill det trygt.'), accent: GAME_ACCENT },
      { to: '/501', title: '501', description: _t('En dartlignende nedtelling fra 501 - én mot én, sjekk ut mellom 0 og -10 for å vinne.'), accent: GAME_ACCENT },
      { to: '/imposter', title: 'Imposter', description: _t('Én spiller får ikke svaret - finn ut hvem ved å spørre rundt bordet.'), accent: GAME_ACCENT },
      { to: '/bullseye', title: 'Bullseye', description: _t('Alle svarer, den med lavest poengsum slås ut hver runde, til én spiller er igjen.'), accent: GAME_ACCENT },
      // No standalone card for Penalty Shootout - it's reached from the XI
      // Battle card above instead, not as its own destination.
      { to: '/flashback', title: 'Flashback', description: _t('Gjett det nøyaktige året ut fra et hint - det blir lettere hver runde, men poengene venter ikke.'), accent: GAME_ACCENT }
    ]
  },
  {
    title: _t('Administrer quizene dine'),
    cards: [
      { to: '/my-quizzes?tab=templates', title: _t('Quizmaler'), description: _t('Ferdiglagde quizer publisert av en administrator - last ned en PDF med en gang, eller kopier en for å redigere.'), accent: QUIZ_MANAGEMENT_ACCENT },
      { to: '/my-quizzes', title: _t('Mine quizer'), description: _t('Se på, rediger eller last ned på nytt alt du har lagret.'), accent: QUIZ_MANAGEMENT_ACCENT },
      { to: '/suggest-question', title: _t('Foreslå et spørsmål'), description: _t('Legg til i den felles spørsmålsbanken - gjennomgås av en administrator, og kan uansett brukes i dine egne quizer.'), accent: QUIZ_MANAGEMENT_ACCENT }
    ]
  }
]

function toColumns(cards) {
  const columns = []
  for (let i = 0; i < cards.length; i += 2) {
    columns.push(cards.slice(i, i + 2))
  }
  return columns
}

const dashboardSections = computed(() =>
  dashboardSectionDefs.map(section => ({ title: section.title, columns: toColumns(section.cards) }))
)

// --- Daily Quiz nudge - once per day, per browser, for a real (non-guest,
// non-admin) user who hasn't finished today's quiz yet. Reuses the same
// /daily-quiz/active endpoint the Daily Quiz list page itself calls (it
// lazily creates today's set if it doesn't exist yet, and reports this
// user's own attempt status against it) rather than adding a new endpoint
// just for this check.
const dailyQuizNudge = ref(null)
const showDailyQuizNudge = ref(false)
const DAILY_QUIZ_NUDGE_KEY = 'daily_quiz_nudge_dismissed_date'

// --- "Your quiz has been reviewed" - results an admin finished checking since the player last looked.
// Takes priority over the nudge: no point asking someone to play today's quiz in the same breath as
// telling them yesterday's score is in.
const reviewNotifications = ref([])

function formatQuizDate(iso) {
  return new Date(iso).toLocaleDateString(window.QUIZ_LOCALE, { weekday: 'long', day: 'numeric', month: 'long' })
}

async function dismissReviewNotifications() {
  const toDismiss = reviewNotifications.value
  reviewNotifications.value = []
  await Promise.allSettled(toDismiss.map(n => api.dismissDailyQuizReviewNotification(n.setId)))
}

onMounted(async () => {
  if (auth.isAdmin.value || auth.isGuest.value) return
  try {
    reviewNotifications.value = await api.getDailyQuizReviewNotifications()
    if (reviewNotifications.value.length) return
  } catch (e) {
    // a nice-to-have - carry on to the regular nudge
  }
  try {
    const [active] = await api.getActiveDailyQuizzes()
    if (!active || (active.status !== 'NOT_STARTED' && active.status !== 'IN_PROGRESS')) return

    // Keyed on the quiz's own date (a server-assigned string), not the
    // browser's local date - sidesteps any client/server timezone mismatch
    // and doubles as "one nudge per quiz day" regardless of how that lines
    // up with the visitor's own clock.
    let dismissedDate = null
    try { dismissedDate = localStorage.getItem(DAILY_QUIZ_NUDGE_KEY) } catch (e) { /* private window etc. - just always show it */ }
    if (dismissedDate === active.quizDate) return

    dailyQuizNudge.value = active
    showDailyQuizNudge.value = true
  } catch (e) {
    // the nudge is a nice-to-have, not core dashboard functionality - fail quietly
  }
})

function dismissDailyQuizNudge() {
  showDailyQuizNudge.value = false
  try {
    if (dailyQuizNudge.value) localStorage.setItem(DAILY_QUIZ_NUDGE_KEY, dailyQuizNudge.value.quizDate)
  } catch (e) { /* ignore - worst case the nudge reappears next visit */ }
}
</script>
