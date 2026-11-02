<template>
  <div class="dq-page adv-page">
    <header class="dq-hero adv-hero">
      <span class="dq-eyebrow">🎄 {{ $t('Julekalender') }} <template v-if="state">{{ state.year }}</template></span>
      <h1 class="dq-title">{{ $t('Tenk som spillmesteren') }}</h1>
      <p class="dq-sub">{{ $t('En ny luke hver dag til jul - velg 4 svar og prøv å treffe spillmesterens skjulte svar.') }}</p>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>
    <p v-if="state && state.simulatedDate" class="adv-testbanner">🧪 {{ $t('Testmodus: kalenderen later som om det er {0}.', [simulatedLabel]) }}</p>

    <template v-else-if="state">
      <!-- Before the 1st of December -->
      <section v-if="state.phase === 'BEFORE'" class="dq-card adv-soon">
        <span class="adv-soon-emoji" aria-hidden="true">🎁</span>
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
          <div class="adv-stat"><span class="adv-stat-n">🔥 {{ state.streak }}</span><span class="adv-stat-l">{{ $t('dager på rad') }}</span></div>
          <div class="adv-stat"><span class="adv-stat-n">🃏 {{ state.jokersLeft }}<small>/{{ state.jokersTotal }}</small></span><span class="adv-stat-l">{{ $t('jokere igjen') }}</span></div>
        </section>

        <!-- Wrapped, once it is all over -->
        <router-link v-if="state.wrappedAvailable" to="/julekalender/wrapped" class="dq-card adv-wrapped-cta">
          <span class="adv-wrapped-emoji" aria-hidden="true">🎁</span>
          <span>
            <strong>{{ $t('Din Julekalender-wrapped er klar!') }}</strong><br />
            {{ $t('Se hvordan det gikk - poeng, rekker og favorittøyeblikk.') }}
          </span>
        </router-link>

        <!-- Today's door -->
        <section v-else-if="todayDoor" class="dq-card adv-today">
          <span class="dq-eyebrow">{{ $t('I dag') }}</span>
          <div class="adv-today-title">{{ $t('Luke {0}', [todayDoor.day]) }} <span v-if="todayDoor.type" aria-hidden="true">{{ typeInfo(todayDoor.type).emoji }}</span></div>
          <p v-if="todayDoor.type" class="adv-today-type">{{ typeInfo(todayDoor.type).name }}</p>
          <div class="adv-today-cta">
            <router-link :to="`/julekalender/luke/${todayDoor.day}`" class="btn btn-primary">
              {{ todayDoor.entered ? $t('Se eller endre svarene dine') : $t('Åpne luka') }}
            </router-link>
            <span class="adv-today-note">{{ todayDoor.entered ? $t('Svarene dine er lagret. Du kan endre dem til midnatt.') : $t('Stenger ved midnatt.') }}</span>
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
            <span class="adv-door-n">{{ door.day }}</span>
            <span class="adv-door-sub">{{ doorSub(door) }}</span>
          </button>
        </div>

        <!-- Leaderboard -->
        <h2 class="dq-section-title">{{ $t('Toppliste') }}</h2>
        <div v-if="leaderboard && leaderboard.playedDays" class="dq-card adv-board">
          <div v-for="p in visiblePlayers" :key="p.rank + p.name" class="adv-row" :class="{ 'is-you': p.you }">
            <span class="adv-row-rank">{{ p.rank }}</span>
            <span class="adv-row-name">{{ p.name }} <small v-if="p.you">({{ $t('deg') }})</small></span>
            <span class="adv-row-points">{{ p.points }}</span>
          </div>
          <div class="adv-row adv-row--gm">
            <span class="adv-row-rank">🎩</span>
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

        <!-- Badges -->
        <h2 class="dq-section-title">{{ $t('Merker') }}</h2>
        <div class="adv-badges">
          <div v-for="b in state.badges" :key="b.code" class="adv-badge" :class="{ 'is-locked': !b.earned }">
            <span class="adv-badge-emoji" aria-hidden="true">{{ b.earned ? BADGES[b.code].emoji : '🔒' }}</span>
            <div><strong>{{ BADGES[b.code].name }}</strong><br /><small>{{ BADGES[b.code].text }}</small></div>
          </div>
        </div>
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
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { t as _t, locale } from '../i18n'
import api from '../services/api'
import { BADGES, typeInfo, pointsText } from '../services/advent'
import AdventRules from '../components/AdventRules.vue'

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
  if (door.status === 'LOCKED') return '🔒'
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
.adv-testbanner { margin: 0 0 14px; padding: 8px 14px; border: 2px dashed var(--gold); border-radius: 3px; background: rgb(var(--gold-rgb) / 0.08); font-size: 0.92rem; }
.adv-soon { text-align: center; padding: 34px 20px; }
.adv-soon-emoji { font-size: 3rem; display: block; }
.adv-soon-title { margin: 6px 0; font-size: 2rem; }
.adv-soon-text { margin: 0; color: var(--text-dim); }
.adv-soon-text strong { display: block; color: var(--text); margin-top: 6px; font-size: 1.1rem; }

.adv-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 14px; }
.adv-stat { border: 2px solid var(--rule); border-radius: 3px; background: var(--ink-raised); padding: 10px 6px; text-align: center; }
.adv-stat-n { display: block; font-family: var(--font-display); font-weight: 700; font-size: 1.45rem; line-height: 1.1; }
.adv-stat-n small { font-size: 0.8rem; font-weight: 500; color: var(--text-dim); }
.adv-stat-l { display: block; font-size: 0.68rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--text-dim); margin-top: 3px; }

.adv-wrapped-cta { display: flex; gap: 14px; align-items: center; text-decoration: none; color: inherit; margin-bottom: 8px; }
.adv-wrapped-emoji { font-size: 2.2rem; }
.adv-today-title { font-family: var(--font-display); font-size: 1.8rem; font-weight: 700; margin: 2px 0; }
.adv-today-type { margin: 0 0 12px; color: var(--text-dim); }
.adv-today-cta { display: flex; gap: 14px; align-items: center; flex-wrap: wrap; }
.adv-today-note { color: var(--text-dim); font-size: 0.9rem; }

.adv-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 8px; }
.adv-door {
  aspect-ratio: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px;
  border: 2px solid var(--rule); border-radius: 4px; background: var(--ink-raised); color: var(--text);
  font-family: var(--font-body); cursor: pointer; padding: 2px;
}
.adv-door-n { font-family: var(--font-display); font-weight: 700; font-size: 1.5rem; line-height: 1; }
.adv-door-sub { font-size: 0.7rem; color: var(--text-dim); font-family: var(--font-mono); }
.adv-door.is-locked, .adv-door.is-empty { opacity: 0.5; cursor: default; }
.adv-door.is-open { border-color: var(--gold); box-shadow: 0 0 0 3px rgb(var(--gold-rgb) / 0.22); background: rgb(var(--gold-rgb) / 0.08); }
.adv-door.is-open .adv-door-sub { color: var(--gold-ink, var(--gold)); font-weight: 700; }
.adv-door.is-closed.is-entered { background: rgb(var(--teal-rgb) / 0.1); }
.adv-door.is-closed.is-entered .adv-door-sub { color: var(--teal-ink, var(--teal)); font-weight: 700; }
.adv-door.is-closed:not(.is-entered) { opacity: 0.6; }
.adv-door.is-finale { border-style: double; border-width: 4px; }

.adv-board { padding: 6px 14px 12px; }
.adv-row { display: flex; align-items: baseline; gap: 12px; padding: 9px 0; border-top: 1px solid var(--line); }
.adv-row:first-child { border-top: none; }
.adv-row.is-you { background: rgb(var(--gold-rgb) / 0.09); margin: 0 -14px; padding: 9px 14px; }
.adv-row-rank { width: 28px; font-family: var(--font-mono); color: var(--text-dim); font-size: 0.9rem; }
.adv-row-name { flex: 1; min-width: 0; font-family: var(--font-display); font-weight: 600; overflow-wrap: anywhere; }
.adv-row-name small { font-family: var(--font-body); font-weight: 400; color: var(--text-dim); }
.adv-row-points { font-family: var(--font-display); font-weight: 700; font-size: 1.3rem; }
.adv-row--gm { border-top: 2px solid var(--rule); color: var(--text-dim); }
.adv-showname { display: flex; gap: 8px; align-items: center; margin-top: 10px; font-size: 0.92rem; text-transform: none; letter-spacing: normal; font-weight: 400; color: var(--text); }

.adv-badges { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 8px; }
.adv-badge { display: flex; gap: 10px; align-items: center; border: 2px solid var(--rule); border-radius: 3px; background: var(--ink-raised); padding: 10px 12px; }
.adv-badge small { color: var(--text-dim); }
.adv-badge-emoji { font-size: 1.6rem; }
.adv-badge.is-locked { opacity: 0.5; border-style: dashed; }
.adv-rules-card { margin-top: 8px; }

@media (max-width: 560px) {
  .adv-grid { grid-template-columns: repeat(4, 1fr); }
  .adv-stat-n { font-size: 1.15rem; }
  .adv-stat-l { font-size: 0.6rem; }
}
</style>
