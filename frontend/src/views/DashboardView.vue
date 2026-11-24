<template>
  <div class="dashboard">
    <p class="dashboard-welcome">{{ $t('Velkommen tilbake,') }} <strong>{{ auth.state.displayName }}</strong></p>
    <TipsModal id="dashboard" :tips="getTips('dashboard')" />

    <div class="dashboard-hero-row">
      <section class="dashboard-hero">
        <h1>{{ $t('Dagens quiz§DashboardViewJ') }}</h1>
        <!-- Says where you stand with today's quiz (it used to be a pop-up on arrival, plus a second card further down) -->
        <p class="page-subtitle landing-lede">{{ hero.lede }}</p>
        <div class="dashboard-hero-actions">
          <router-link :to="hero.to" class="btn btn-lg" :class="hero.done ? 'btn-secondary' : 'btn-primary'">{{ hero.button }} →</router-link>
          <span v-if="hero.score" class="dashboard-hero-score"><strong>{{ hero.score }}</strong> {{ $t('poeng i dag') }}</span>
          <router-link v-if="hero.done" to="/daily-quiz" class="dashboard-hero-link">{{ $t('Tidligere quizer og ukestilling') }}</router-link>
        </div>
      </section>

      <div class="dashboard-side">
        <section class="dashboard-join-card" :style="{ '--card-accent': 'var(--primary)', '--card-ink': 'var(--primary)' }">
          <span class="dash-icon" aria-hidden="true"><AppIcon name="layers" /></span>
          <h2>{{ $t('Lag en quiz') }}</h2>
          <p class="page-subtitle">{{ $t('Lag en egen quiz for venner, kolleger eller quizkvelden på lokalpuben.') }}</p>
          <router-link to="/generate" class="btn btn-secondary">{{ $t('Lag en quiz →') }}</router-link>
        </section>
        <section class="dashboard-join-card" :style="{ '--card-accent': 'var(--violet)', '--card-ink': 'var(--violet-ink)' }">
          <span class="dash-icon" aria-hidden="true"><AppIcon name="swap" /></span>
          <h2>{{ $t('Bli med i et spill') }}</h2>
          <p class="page-subtitle">{{ $t('Har du fått en romkode fra en vert? Hopp rett inn.') }}</p>
          <router-link to="/join" class="btn btn-secondary">{{ $t('Bli med med kode →') }}</router-link>
        </section>
      </div>
    </div>

    <!-- The advent calendar: a banner from November until the season is over (it says "coming soon" until 1 December). -->
    <router-link v-if="showAdventBanner" v-reveal to="/julekalender" class="dashboard-advent">
      <span class="dashboard-advent-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M3 10h18M8 3v4M16 3v4M8 15h3" /></svg></span>
      <div>
        <h3>{{ $t('Julekalender') }}</h3>
        <p>{{ $t('En ny luke hver dag 1.-24. desember - tenk som spillmesteren og samle poeng.') }}</p>
      </div>
      <span class="btn btn-secondary">{{ $t('Åpne →') }}</span>
    </router-link>

    <template v-for="section in dashboardSections" :key="section.title">
      <h2 v-reveal class="dashboard-section-title" :style="{ '--section-accent': section.accent }">{{ section.title }}</h2>
      <div class="dashboard-features">
        <!-- Grouped into pairs so mobile can lay each pair out as its own
             2-tall column and scroll horizontally between columns (see
             .dashboard-feature-column's mobile rule in style.css). On desktop
             display:contents makes this wrapper invisible to layout, so cards
             flow straight into the existing wrapping grid as before. -->
        <div v-for="(column, i) in section.columns" :key="i" class="dashboard-feature-column">
          <router-link
            v-for="(card, j) in column"
            :key="card.to"
            v-reveal="(i * 2 + j) % 4 * 70"
            :to="card.to"
            class="dashboard-feature-card"
            :style="{ '--card-accent': card.accent, '--card-ink': card.ink }"
          >
            <span class="dash-icon" data-draw aria-hidden="true"><AppIcon :name="card.icon" /></span>
            <h3>{{ card.title }}</h3>
            <p>{{ card.description }}</p>
            <span class="dashboard-feature-play">{{ $t('Spill →').replace('→', '').trim() }} <span class="dash-arrow">→</span></span>
          </router-link>
        </div>
      </div>
    </template>

    <!-- Deliberately not another small tile in the grid above - buried among
         12 game cards it was easy to miss, and it's not a "way to play" like
         the rest of this page. A full-width, always-visible strip is the
         obvious/findable version of the same link. -->
    <router-link v-reveal to="/report-problem" class="dashboard-report-callout">
      <div>
        <h3>{{ $t('Funnet en feil eller noe forvirrende?') }}</h3>
        <p>{{ $t('Si fra, så tar vi en titt.') }}</p>
      </div>
      <span class="btn btn-secondary">{{ $t('Meld fra om et problem →') }}</span>
    </router-link>

    <!-- "Your quiz has been reviewed": an admin has finished checking answers the player had waiting, so
         their score is final. Shown until they've dismissed it or opened the quiz (that's tracked on
         the server, so it follows them across devices). -->
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
  </div>
</template>

<script setup>
import TipsModal from '../components/TipsModal.vue'
import { getTips } from '../content/tips'
import { t as _t } from '../i18n'
import { computed, onMounted, ref } from 'vue'
import auth from '../services/auth'
import api from '../services/api'
import AppIcon from '../components/AppIcon.vue'
import reveal from '../directives/reveal'

// Cards are grouped into named sections (rendered as their own labeled block,
// see dashboardSections below) instead of one flat 13-card grid - weekly
// solo quizzes, the pass-the-device/online games, and the non-game "manage
// your quizzes" actions each read as their own thing now, not just a color.
// "Report a problem" isn't in any section - see the callout below instead.
// The advent calendar banner shows from 1 November through January (the season's wrapped is up until then).
const showAdventBanner = [0, 10, 11].includes(new Date().getMonth())

const vReveal = reveal // cards fade up as they scroll into view (see directives/reveal.js)

// Each section has its own colour from the palette (primary, secondary, accent); [fill, readable ink for icons and text]
const WEEKLY_QUIZ_ACCENT = ['var(--primary)', 'var(--primary)']
const GAME_ACCENT = ['var(--violet)', 'var(--violet-ink)']
const QUIZ_MANAGEMENT_ACCENT = ['var(--gold)', 'var(--gold-ink)']

const dashboardSectionDefs = [
  {
    title: _t('Ukentlige quizer'),
    accent: WEEKLY_QUIZ_ACCENT[0],
    cards: [
      // Dagens quiz is the hero at the top of the page - not repeated as a card here.
      { to: '/weekly-grid', icon: 'layers', title: _t('Ukens grid§DashboardViewJ'), description: _t('Gjett alle svarene som passer ukens tema før du går tom for liv.'), accent: WEEKLY_QUIZ_ACCENT[0], ink: WEEKLY_QUIZ_ACCENT[1] },
      { to: '/starting-xi', icon: 'star', title: 'Starting XI', description: _t('Gjett en hel startellever, posisjon for posisjon, før ukens brett går tom for liv.'), accent: WEEKLY_QUIZ_ACCENT[0], ink: WEEKLY_QUIZ_ACCENT[1] }
    ]
  },
  {
    title: _t('Spill§DashboardViewJ'),
    accent: GAME_ACCENT[0],
    cards: [
      { to: '/grid-battle', icon: 'swap', title: 'Grid Battle', description: _t('En flerspillerversjon av Ukens grid der enheten sendes rundt - bytt på, eller mist et liv i forsøket.'), accent: GAME_ACCENT[0], ink: GAME_ACCENT[1] },
      { to: '/starting-xi-battle', icon: 'crown', title: 'XI Battle', description: _t('Samme idé som Grid Battle, men for et Starting XI-brett - bytt på å nevne laget.'), accent: GAME_ACCENT[0], ink: GAME_ACCENT[1] },
      { to: '/tension', icon: 'flame', title: 'Tension', description: _t('En selskapsquiz der enheten sendes rundt - gå for en gjetning med høy verdi, eller spill det trygt.'), accent: GAME_ACCENT[0], ink: GAME_ACCENT[1] },
      { to: '/501', icon: 'dice', title: '501', description: _t('En dartlignende nedtelling fra 501 - én mot én, sjekk ut mellom 0 og -10 for å vinne.'), accent: GAME_ACCENT[0], ink: GAME_ACCENT[1] },
      { to: '/imposter', icon: 'cards', title: 'Imposter', description: _t('Én spiller får ikke svaret - finn ut hvem ved å spørre rundt bordet.'), accent: GAME_ACCENT[0], ink: GAME_ACCENT[1] },
      { to: '/bullseye', icon: 'target', title: 'Bullseye', description: _t('Alle svarer, den med lavest poengsum slås ut hver runde, til én spiller er igjen.'), accent: GAME_ACCENT[0], ink: GAME_ACCENT[1] },
      // No standalone card for Penalty Shootout - it's reached from the XI
      // Battle card above instead, not as its own destination.
      { to: '/flashback', icon: 'medal', title: 'Flashback', description: _t('Gjett det nøyaktige året ut fra et hint - det blir lettere hver runde, men poengene venter ikke.'), accent: GAME_ACCENT[0], ink: GAME_ACCENT[1] }
    ]
  },
  {
    title: _t('Administrer quizene dine'),
    accent: QUIZ_MANAGEMENT_ACCENT[0],
    cards: [
      { to: '/my-quizzes?tab=templates', icon: 'gift', title: _t('Quizmaler'), description: _t('Ferdiglagde quizer publisert av en administrator - last ned en PDF med en gang, eller kopier en for å redigere.'), accent: QUIZ_MANAGEMENT_ACCENT[0], ink: QUIZ_MANAGEMENT_ACCENT[1] },
      { to: '/my-quizzes', icon: 'check', title: _t('Mine quizer'), description: _t('Se på, rediger eller last ned på nytt alt du har lagret.'), accent: QUIZ_MANAGEMENT_ACCENT[0], ink: QUIZ_MANAGEMENT_ACCENT[1] },
      { to: '/suggest-question', icon: 'flask', title: _t('Foreslå et spørsmål'), description: _t('Legg til i den felles spørsmålsbanken - gjennomgås av en administrator, og kan uansett brukes i dine egne quizer.'), accent: QUIZ_MANAGEMENT_ACCENT[0], ink: QUIZ_MANAGEMENT_ACCENT[1] }
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
  dashboardSectionDefs.map(section => ({ title: section.title, accent: section.accent, columns: toColumns(section.cards) }))
)

// --- Today's quiz, as the hero shows it: not started, under way, handed in, or scored. Reuses the same
// /daily-quiz/active endpoint the Daily Quiz list page itself calls (it lazily creates today's set if it
// doesn't exist yet, and reports this user's own attempt status against it). Until that answer is in -
// and for an admin or a guest, who never ask - the hero is the plain invitation to play.
const todayQuiz = ref(null)
const hero = computed(() => {
  const q = todayQuiz.value
  const to = q ? `/daily-quiz/${q.id}` : '/daily-quiz'
  if (q?.status === 'IN_PROGRESS') {
    return { to, lede: _t('Du er i gang med dagens quiz - svarene du har lagret venter på deg.'), button: _t('Fortsett der du slapp') }
  }
  if (q?.status === 'SUBMITTED') {
    return { to, done: true, lede: _t('Svarene dine er sendt inn. En administrator sjekker noen av dem - poengsummen kommer når alt er sjekket.'), button: _t('Se svarene dine§DashboardViewT') }
  }
  if (q?.status === 'GRADED') {
    return { to, done: true, score: `${q.score} / ${q.maxScore}`, lede: _t('Dagens quiz er levert. Et nytt sett med spørsmål kommer i morgen.'), button: _t('Se resultatet') }
  }
  return { to, lede: _t('15 tilfeldige spørsmål i pubquiz-stil - fritekst, et nytt sett hver dag.'), button: _t('Spill dagens quiz§HomeViewT') }
})

// --- "Your quiz has been reviewed" - results an admin finished checking since the player last looked.
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
  } catch (e) {
    // a nice-to-have - the page works without it
  }
  try {
    const [active] = await api.getActiveDailyQuizzes()
    todayQuiz.value = active || null
  } catch (e) {
    // a nice-to-have - the hero then just invites you to play
  }
})
</script>
