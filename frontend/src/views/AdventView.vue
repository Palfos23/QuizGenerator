<template>
  <div class="dq-page adv-page">
    <header class="dq-hero adv-hero">
      <span class="adv-hero-scene"><ChristmasScene variant="hero" /></span>
      <ChristmasScene variant="snow" />
      <span class="dq-eyebrow">{{ $t('Julekalender') }} <template v-if="state">{{ state.year }}</template></span>
      <h1 class="dq-title">{{ $t('Tenk som spillmesteren') }}</h1>
      <p class="dq-sub">{{ $t('En ny luke hver dag til jul - velg 4 svar og prøv å treffe spillmesterens skjulte svar.') }}</p>
    </header>

    <p v-if="state && state.simulatedDate" class="adv-testbanner"><AppIcon name="flask" /> {{ $t('Testmodus: kalenderen later som om det er {0}.', [simulatedLabel]) }}</p>
    <div v-if="error" class="banner error">{{ error }}</div>
    <LoadingSkeleton v-if="loading" />

    <template v-else-if="state">
      <!-- Before the 1st of December -->
      <section v-if="state.phase === 'BEFORE'" class="dq-card adv-soon">
        <span class="adv-soon-emoji" aria-hidden="true"><AppIcon name="gift" /></span>
        <h2 class="adv-soon-title">{{ $t('Kommer snart') }}</h2>
        <p class="adv-soon-text">
          {{ $t('Første luke åpner {0}.', [startLabel]) }}
          <strong>{{ $t('Åpner om {0} dag¦Åpner om {0} dager', [state.daysUntilStart]) }}</strong>
        </p>
      </section>

      <template v-else>
        <p v-if="!state.player" class="banner">{{ $t('Du må være logget inn som spiller for å delta i julekalenderen.') }}</p>

        <!-- Numbers -->
        <section class="adv-stats" aria-label="">
          <div class="adv-stat"><span class="adv-stat-n">{{ state.totalPoints }}</span><span class="adv-stat-l">{{ $t('poeng') }}</span></div>
          <div class="adv-stat"><span class="adv-stat-n">{{ state.rank ? '#' + state.rank : '–' }}</span><span class="adv-stat-l">{{ $t('plass') }}</span></div>
          <div class="adv-stat"><span class="adv-stat-n"><AppIcon name="flame" /> {{ state.streak }}</span><span class="adv-stat-l">{{ $t('dager på rad') }}</span></div>
          <div class="adv-stat"><span class="adv-stat-n"><AppIcon name="cards" /> {{ state.jokersLeft }}<small>/{{ state.jokersTotal }}</small></span><span class="adv-stat-l">{{ $t('jokere igjen') }}</span></div>
        </section>

        <!-- Wrapped, once it is all over -->
        <router-link v-if="state.wrappedAvailable" to="/julekalender/wrapped" class="dq-card adv-wrapped-cta">
          <span class="adv-wrapped-emoji" aria-hidden="true"><AppIcon name="gift" /></span>
          <span>
            <strong>{{ $t('Din Julekalender-wrapped er klar!') }}</strong><br />
            {{ $t('Se hvordan det gikk - poeng, rekker og favorittøyeblikk.') }}
          </span>
        </router-link>

        <!-- Today's door -->
        <section v-else-if="todayDoor" class="dq-card adv-today">
          <span class="dq-eyebrow">{{ $t('I dag') }}</span>
          <div class="adv-today-title">{{ $t('Luke {0}', [todayDoor.day]) }} <AppIcon v-if="todayDoor.type" :name="typeInfo(todayDoor.type).icon" /></div>
          <p v-if="todayDoor.type" class="adv-today-type">{{ typeInfo(todayDoor.type).name }}</p>
          <div class="adv-today-cta">
            <router-link :to="`/julekalender/luke/${todayDoor.day}`" class="btn btn-primary">
              {{ todayDoor.entered ? $t('Se svarene dine') : $t('Åpne luka') }}
            </router-link>
            <span class="adv-today-note">{{ todayDoor.entered ? $t('Svarene dine er sendt inn - resultatet kommer i morgen.') : $t('Stenger ved midnatt.') }}</span>
          </div>
        </section>
        <div v-else-if="state.phase === 'RUNNING'" class="dq-card adv-today">
          <strong>{{ $t('Dagens luke er ikke klar ennå') }}</strong> {{ $t('Kom tilbake snart.') }}
        </div>

        <!-- The 24 doors -->
        <h2 class="dq-section-title">{{ $t('Lukene') }}</h2>
        <div class="adv-grid">
          <button
            v-for="door in state.doors"
            :key="door.day"
            type="button"
            class="adv-door"
            :class="['is-' + door.status.toLowerCase(), { 'is-entered': door.entered, 'is-finale': door.day === 24 }]"
            :disabled="door.status === 'LOCKED' || door.status === 'EMPTY'"
            :aria-label="doorLabel(door)"
            @click="openDoor(door)"
          >
            <span class="adv-door-bow" aria-hidden="true"></span>
            <span class="adv-door-tag">
              <span class="adv-door-n">{{ door.day }}</span>
              <span class="adv-door-sub"><AppIcon v-if="door.status === 'LOCKED'" name="lock" /><template v-else>{{ doorSub(door) }}</template></span>
            </span>
          </button>
        </div>

        <!-- Leaderboard -->
        <h2 class="dq-section-title">{{ $t('Toppliste') }}</h2>
        <div v-if="leaderboard && leaderboard.playedDays" class="dq-card adv-board">
          <div v-for="p in visiblePlayers" :key="p.rank + p.name" class="adv-row" :class="{ 'is-you': p.you }">
            <span class="adv-row-rank" :class="p.rank <= 3 ? 'medal-' + p.rank : ''">{{ p.rank }}</span>
            <span class="adv-row-name">{{ p.name }} <small v-if="p.you">({{ $t('deg') }})</small></span>
            <span class="adv-row-points">{{ p.points }}</span>
          </div>
          <div class="adv-row adv-row--gm">
            <span class="adv-row-rank"><AppIcon name="crown" /></span>
            <span class="adv-row-name">{{ $t('Spillmesteren') }}</span>
            <span class="adv-row-points">{{ leaderboard.gamemasterPoints }}</span>
          </div>
          <button v-if="leaderboard.players.length > 10" type="button" class="dq-disclosure" :class="{ 'is-open': showAll }" @click="showAll = !showAll">
            {{ showAll ? $t('Vis færre') : $t('Vis alle {0}', [leaderboard.players.length]) }}
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6" /></svg>
          </button>
          <label v-if="state.player" class="adv-showname">
            <input type="checkbox" :checked="leaderboard.showName" @change="changeShowName($event.target.checked)" />
            {{ $t('Vis navnet mitt på topplisten') }}
          </label>
        </div>
        <div v-else class="dq-card dq-empty">{{ $t('Topplisten dukker opp når den første luka er over.') }}</div>
      </template>

      <!-- Rules: open while it is "coming soon", folded away once it is running -->
      <h2 class="dq-section-title">{{ $t('Slik fungerer det') }}</h2>
      <button v-if="state.phase !== 'BEFORE'" class="dq-disclosure" :class="{ 'is-open': showRules }" @click="showRules = !showRules">
        {{ showRules ? $t('Skjul reglene') : $t('Vis reglene') }}
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6" /></svg>
      </button>
      <div v-if="state.phase === 'BEFORE' || showRules" class="dq-card adv-rules-card">
        <AdventRules />
      </div>
    </template>
  </div>
</template>

<script setup>
import LoadingSkeleton from '../components/LoadingSkeleton.vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { t as _t, locale } from '../i18n'
import api from '../services/api'
import { typeInfo, pointsText } from '../services/advent'
import AdventRules from '../components/AdventRules.vue'
import AppIcon from '../components/AppIcon.vue'
import ChristmasScene from '../components/ChristmasScene.vue'

const router = useRouter()
const state = ref(null)
const leaderboard = ref(null)
const loading = ref(true)
const error = ref('')
const showAll = ref(false)
const showRules = ref(false)

const startLabel = computed(() => {
  if (!state.value) return ''
  const [y, m, d] = state.value.startDate.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString(locale, { day: 'numeric', month: 'long' })
})
const simulatedLabel = computed(() => {
  if (!state.value?.simulatedDate) return ''
  const [y, m, d] = state.value.simulatedDate.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString(locale, { day: 'numeric', month: 'long' })
})
const todayDoor = computed(() => state.value?.doors?.find(d => d.status === 'OPEN') || null)
const visiblePlayers = computed(() => (leaderboard.value ? (showAll.value ? leaderboard.value.players : leaderboard.value.players.slice(0, 10)) : []))

onMounted(async () => {
  try {
    state.value = await api.getAdvent()
    if (state.value.phase !== 'BEFORE') {
      leaderboard.value = await api.getAdventLeaderboard()
    }
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke laste julekalenderen.')
  } finally {
    loading.value = false
  }
})

function openDoor(door) {
  if (door.status === 'OPEN' || door.status === 'CLOSED') router.push(`/julekalender/luke/${door.day}`)
}

function doorSub(door) {
  if (door.status === 'OPEN') return door.entered ? '✓' : _t('I dag')
  if (door.status === 'CLOSED') return door.entered ? (door.points < 0 ? '−' + Math.abs(door.points) : '+' + door.points) : '–'
  return '–'
}

function doorLabel(door) {
  if (door.status === 'CLOSED' && door.entered) return `${_t('Luke {0}', [door.day])}: ${pointsText(door.points)}`
  return _t('Luke {0}', [door.day])
}

async function changeShowName(value) {
  try {
    await api.setAdventShowName(value)
    leaderboard.value = await api.getAdventLeaderboard()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke lagre valget.')
  }
}
</script>

<style scoped>
.adv-page { max-width: 760px; }

/* A snowy night over the header: stars, a moon, pine trees (one decorated) on the hills below the text. */
.adv-hero {
  position: relative;
  overflow: hidden;
  padding: 26px 24px 150px;
  border-radius: var(--radius-lg);
  background:
    radial-gradient(90% 70% at 85% 0%, rgb(70 120 160 / 0.35) 0%, transparent 70%),
    linear-gradient(180deg, #0d1f33 0%, #13324a 55%, #1a4a4a 100%);
  color: #f4f7fb;
  margin-bottom: 18px;
}
.adv-hero > :not(.xmas-scene):not(.adv-hero-scene) { position: relative; z-index: 1; }
.adv-hero .dq-eyebrow { color: #ffd45c; }
.adv-hero .dq-title { color: #fff; text-shadow: 0 2px 12px rgb(0 0 0 / 0.35); }
.adv-hero .dq-sub { color: rgb(244 247 251 / 0.85); max-width: 460px; }
.adv-hero-scene { position: absolute; left: 0; right: 0; bottom: 0; height: 170px; }
.adv-hero-scene :deep(.xmas-scene) { position: absolute; inset: 0; }
.adv-testbanner { margin: 0 0 14px; padding: 8px 14px; border: 1px solid rgb(var(--gold-rgb) / 0.4); border-radius: var(--radius-md); background: rgb(var(--gold-rgb) / 0.06); font-size: 0.92rem; }
.adv-soon { text-align: center; padding: 34px 20px; }
.adv-soon-emoji { display: grid; place-items: center; width: 64px; height: 64px; margin: 0 auto var(--space-3); border-radius: var(--radius-lg); background: rgb(var(--primary-rgb) / 0.09); color: var(--primary); font-size: 2rem; }
.adv-soon-title { margin: 6px 0; font-size: 2rem; }
.adv-soon-text { margin: 0; color: var(--text-dim); }
.adv-soon-text strong { display: block; color: var(--text); margin-top: 6px; font-size: 1.1rem; }

.adv-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 14px; }
.adv-stat { border: 1px solid var(--rule); border-top: 3px solid #a32a2a; border-radius: var(--radius-sm); background: var(--ink-raised); padding: 10px 6px; text-align: center; }
.adv-stat:nth-child(even) { border-top-color: #1f5a3a; }
.adv-stat-n { display: block; font-family: var(--font-display); font-weight: 700; font-size: 1.45rem; line-height: 1.1; }
.adv-stat-n small { font-size: 0.8rem; font-weight: 500; color: var(--text-dim); }
.adv-stat-l { display: block; font-size: 0.68rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--text-dim); margin-top: 3px; }

.adv-wrapped-cta { display: flex; gap: 14px; align-items: center; text-decoration: none; color: inherit; margin-bottom: 8px; }
.adv-wrapped-emoji { display: grid; place-items: center; width: 48px; height: 48px; border-radius: var(--radius-md); background: rgb(var(--primary-rgb) / 0.09); color: var(--primary); font-size: 1.5rem; flex-shrink: 0; }
.adv-today-title { font-family: var(--font-display); font-size: 1.8rem; font-weight: 700; margin: 2px 0; }
.adv-today-type { margin: 0 0 12px; color: var(--text-dim); }
.adv-today-cta { display: flex; gap: 14px; align-items: center; flex-wrap: wrap; }
.adv-today-note { color: var(--text-dim); font-size: 0.9rem; }

/* The doors: little presents, red and green in turn, a gold bow on each and snow along the top. Today's glows gold;
   the 24th is all gold, with a star. */
.adv-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 10px; }
.adv-door {
  position: relative;
  aspect-ratio: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px;
  border: none; border-radius: 10px; color: #fff; cursor: pointer; padding: 2px;
  font-family: var(--font-body);
  background:
    linear-gradient(90deg, transparent 46%, rgb(233 185 73 / 0.85) 46% 54%, transparent 54%),
    radial-gradient(circle at 30% 25%, rgb(255 255 255 / 0.08) 0 2px, transparent 3px) 0 0 / 14px 14px,
    #a32a2a;
  box-shadow: 0 3px 0 rgb(0 0 0 / 0.25), inset 0 0 0 1px rgb(255 255 255 / 0.12);
  transition: transform 0.15s ease, box-shadow 0.2s ease, filter 0.2s ease;
}
.adv-door:nth-child(even) {
  background:
    linear-gradient(90deg, transparent 46%, rgb(233 185 73 / 0.85) 46% 54%, transparent 54%),
    radial-gradient(circle at 30% 25%, rgb(255 255 255 / 0.08) 0 2px, transparent 3px) 0 0 / 14px 14px,
    #1f5a3a;
}
/* snow on the lid */
.adv-door::before {
  content: '';
  position: absolute;
  left: 4px; right: 4px; top: -3px;
  height: 9px;
  border-radius: 6px 6px 50% 50% / 6px 6px 100% 100%;
  background: #f4fbff;
  box-shadow: 0 1px 0 rgb(0 0 0 / 0.12);
}
.adv-door-bow {
  position: absolute;
  top: 2px; left: 50%;
  width: 18px; height: 10px;
  translate: -50% 0;
  background:
    radial-gradient(ellipse at 25% 50%, #e9b949 0 45%, transparent 47%),
    radial-gradient(ellipse at 75% 50%, #e9b949 0 45%, transparent 47%);
  z-index: 1;
}
.adv-door:not(:disabled):hover { transform: translateY(-2px); filter: brightness(1.1); }
/* the number sits on a cream gift tag, so the ribbon never runs through it */
.adv-door-tag {
  position: relative;
  z-index: 1;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-width: 52%;
  padding: 4px 6px 3px;
  border-radius: 8px;
  background: #fdf6e6;
  color: #6a1a1d;
  box-shadow: 0 2px 4px rgb(0 0 0 / 0.3);
}
.adv-door:nth-child(even) .adv-door-tag { color: #17442c; }
.adv-door-n { font-family: var(--font-display); font-weight: 700; font-size: 1.35rem; line-height: 1; }
.adv-door-sub { font-size: 0.68rem; font-weight: 700; line-height: 1.2; min-height: 1.2em; color: rgb(60 30 20 / 0.7); }
.adv-door-sub :deep(svg) { width: 11px; height: 11px; }
.adv-door.is-locked, .adv-door.is-empty { cursor: default; filter: saturate(0.55) brightness(0.7); }
.adv-door.is-open {
  box-shadow: 0 0 0 3px #ffd45c, 0 0 18px 2px rgb(255 212 92 / 0.55), 0 3px 0 rgb(0 0 0 / 0.25);
  animation: adv-door-glow 2.2s ease-in-out infinite;
}
.adv-door.is-open .adv-door-sub { color: #a36a00; }
.adv-door.is-closed.is-entered { filter: none; }
.adv-door.is-closed.is-entered .adv-door-sub { color: #1f7a4a; }
.adv-door.is-closed:not(.is-entered) { filter: saturate(0.5) brightness(0.75); }
.adv-door.is-finale {
  background:
    radial-gradient(circle at 50% 40%, rgb(255 255 255 / 0.35) 0, transparent 55%),
    linear-gradient(160deg, #f2c75c, #c8902a);
  color: #4d2a05;
}
.adv-door.is-finale .adv-door-bow { display: none; }
.adv-door.is-finale::after {
  content: '★';
  position: absolute;
  top: 3px;
  font-size: 0.9rem;
  color: #fff7d6;
  text-shadow: 0 0 6px rgb(255 255 255 / 0.9);
  z-index: 1;
}
@keyframes adv-door-glow {
  0%, 100% { box-shadow: 0 0 0 3px #ffd45c, 0 0 14px 1px rgb(255 212 92 / 0.45), 0 3px 0 rgb(0 0 0 / 0.25); }
  50% { box-shadow: 0 0 0 3px #ffd45c, 0 0 24px 4px rgb(255 212 92 / 0.7), 0 3px 0 rgb(0 0 0 / 0.25); }
}
@media (prefers-reduced-motion: reduce) {
  .adv-door.is-open { animation: none; }
  .adv-door:not(:disabled):hover { transform: none; }
}

.adv-board { padding: 6px 14px 12px; }
.adv-row { display: flex; align-items: baseline; gap: 12px; padding: 9px 0; border-top: 1px solid var(--line); }
.adv-row:first-child { border-top: none; }
.adv-row.is-you { background: rgb(var(--gold-rgb) / 0.09); margin: 0 -14px; padding: 9px 14px; }
.adv-row-rank { width: 28px; color: var(--text-dim); font-size: 0.9rem; }
/* the top three get a little bauble in gold, silver and bronze */
.adv-row-rank[class*='medal-'] {
  width: 24px; height: 24px; margin-right: 4px; border-radius: 50%;
  display: inline-grid; place-items: center; align-self: center;
  font-size: 0.78rem; font-weight: 700; color: #2b1a05;
  box-shadow: inset 0 -3px 0 rgb(0 0 0 / 0.15);
}
.adv-row-rank.medal-1 { background: radial-gradient(circle at 35% 30%, #fff3c4, #e9b949 60%); }
.adv-row-rank.medal-2 { background: radial-gradient(circle at 35% 30%, #ffffff, #b9c2cc 60%); }
.adv-row-rank.medal-3 { background: radial-gradient(circle at 35% 30%, #ffe0c4, #c58a55 60%); }
.adv-row-name { flex: 1; min-width: 0; font-family: var(--font-display); font-weight: 600; overflow-wrap: anywhere; }
.adv-row-name small { font-family: var(--font-body); font-weight: 400; color: var(--text-dim); }
.adv-row-points { font-family: var(--font-display); font-weight: 700; font-size: 1.3rem; }
.adv-row--gm { border-top: 1px solid var(--rule); color: var(--text-dim); }
.adv-showname { display: flex; gap: 8px; align-items: center; margin-top: 10px; font-size: 0.92rem; text-transform: none; letter-spacing: normal; font-weight: 400; color: var(--text); }

.adv-rules-card { margin-top: 8px; }

@media (max-width: 560px) {
  .adv-hero { padding: 20px 16px 130px; }
  .adv-hero-scene { height: 140px; }
  .adv-grid { grid-template-columns: repeat(4, 1fr); gap: 8px; }
  .adv-stat-n { font-size: 1.15rem; }
  .adv-stat-l { font-size: 0.6rem; }
}
</style>
