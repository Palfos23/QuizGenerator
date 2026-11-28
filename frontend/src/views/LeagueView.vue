<template>
  <div class="dq-page dq-page--wide">
    <router-link :to="adminView ? '/admin/leagues' : '/ligaer'" class="dq-back">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6" /></svg>
      {{ $t('Ligaer') }}
    </router-link>

    <div v-if="error" class="banner error" style="margin-top:16px;">{{ error }}</div>
    <LoadingSkeleton v-if="loading && !league" />

    <template v-if="league">
      <header class="dq-hero lg-head">
        <span class="dq-eyebrow"><template v-if="adminView">{{ $t('Admin - bare lesing') }} · </template>{{ formatName(season.format) }} · {{ $t('Sesong {0}', [season.number]) }} · {{ stateName(season.state) }}</span>
        <h1 class="dq-title">{{ league.name }}</h1>
        <p class="dq-sub">
          {{ seasonSpan(season) }}
          <template v-if="season.format === 'HEAD_TO_HEAD' && season.playoffs"> · {{ $t('med sluttspill') }}</template>
        </p>
        <label v-if="league.seasons.length > 1" class="lg-season-pick">
          <span>{{ $t('Sesong') }}</span>
          <select :value="season.number" @change="load(Number($event.target.value))">
            <option v-for="s in league.seasons" :key="s.number" :value="s.number">{{ $t('Sesong {0}', [s.number]) }} – {{ seasonSpan(s) }}</option>
          </select>
        </label>
      </header>

      <!-- Inviting: the code and a link to share -->
      <section class="lg-invite" :class="{ 'is-new': justCreated }">
        <div>
          <div class="lg-invite-label">{{ adminView ? $t('Ligakode') : justCreated ? $t('Ligaen er klar! Inviter vennene dine') : $t('Inviter venner') }}</div>
          <div class="lg-invite-code">{{ league.code }}</div>
        </div>
        <div v-if="!adminView" class="lg-invite-actions">
          <button type="button" class="btn btn-secondary btn-sm" @click="copyLink">{{ $t('Kopier lenke') }}</button>
          <button v-if="canShare" type="button" class="btn btn-secondary btn-sm" @click="share">{{ $t('Del') }}</button>
        </div>
      </section>

      <p v-if="notice" class="lg-notice">{{ notice }}</p>

      <LeagueRecap v-if="season.recap" :recap="season.recap" :season-number="season.number" :format="season.format" />

      <!-- Today: my matchup (head-to-head) or how many have played (points) -->
      <section v-if="season.today" class="dq-card lg-today">
        <div class="lg-today-head">
          <span class="dq-eyebrow">{{ $t('I dag') }}</span>
          <span class="lg-today-count">{{ $t('{0} av {1} har spilt', [season.today.playedCount, season.today.participantCount]) }}</span>
        </div>
        <LeagueMatchRow v-if="season.today.myMatch" :match="season.today.myMatch" />
        <p v-else-if="!adminView && season.format === 'HEAD_TO_HEAD' && inSeason" class="lg-today-text">{{ $t('Fri runde i dag - dagens quiz teller likevel på snittet ditt.') }}</p>
        <router-link v-if="!adminView && !season.today.iPlayed && inSeason" to="/daily-quiz" class="btn btn-primary">{{ $t('Spill dagens quiz') }} →</router-link>
        <p v-else-if="!adminView && season.today.iPlayed" class="lg-today-text">{{ $t('Du har spilt i dag.') }}</p>
      </section>

      <nav class="lg-tabs" role="tablist">
        <button v-for="t in tabs" :key="t.key" type="button" role="tab" :aria-selected="tab === t.key" :class="{ 'is-on': tab === t.key }" @click="tab = t.key">{{ t.label }}</button>
      </nav>

      <!-- Table -->
      <section v-if="tab === 'table'" class="dq-card lg-panel">
        <p v-if="!season.table.length" class="lg-empty">{{ $t('Ingen spillere ennå.') }}</p>
        <!-- The top three on a podium (2nd, 1st, 3rd from left to right), everyone else in the table below.
             A step's colour and height come from the place, not the position: two sharing 1st both stand on gold
             (and nobody on silver), and the line under the podium says it's shared. -->
        <div v-if="podium.length" class="lg-podium">
          <div v-for="p in podium" :key="p.row.player.id" class="lg-podium-spot" :class="['place-' + p.row.rank, { 'is-me': p.row.player.me }]">
            <span class="lg-podium-name">{{ playerName(p.row.player) }}<span v-if="p.row.player.me" class="lg-you">{{ $t('DU') }}</span></span>
            <span class="lg-podium-value">{{ tableValue(p.row) }}</span>
            <span class="lg-podium-meta">{{ podiumMeta(p.row) }}</span>
            <span class="lg-podium-step"><b>{{ p.row.rank }}</b></span>
          </div>
        </div>
        <p v-if="podiumNote" class="lg-podium-note">{{ podiumNote }}</p>

        <div v-if="!podium.length || season.table.length > 3" class="lg-scroll">
          <table class="lg-table">
            <thead v-if="season.format === 'HEAD_TO_HEAD'">
              <tr><th>#</th><th class="lg-left">{{ $t('Spiller') }}</th><th :title="$t('Kamper')">{{ $t('K§LeagueTable') }}</th><th :title="$t('Seier')">{{ $t('S§LeagueTable') }}</th><th :title="$t('Uavgjort')">{{ $t('U§LeagueTable') }}</th><th :title="$t('Tap')">{{ $t('T§LeagueTable') }}</th><th class="lg-hide-sm" :title="$t('Quizpoeng')">{{ $t('Quiz') }}</th><th>{{ $t('Poeng') }}</th><th class="lg-hide-sm">{{ $t('Form') }}</th></tr>
            </thead>
            <thead v-else>
              <tr><th>#</th><th class="lg-left">{{ $t('Spiller') }}</th><th>{{ $t('Dager') }}</th><th class="lg-hide-sm">{{ $t('Snitt') }}</th><th class="lg-hide-sm">{{ $t('Beste') }}</th><th>{{ $t('Totalt') }}</th><th class="lg-hide-sm">{{ $t('Siste dager') }}</th></tr>
            </thead>
            <tbody>
              <tr v-for="r in tableRows" :key="r.player.id" :class="{ 'is-me': r.player.me, 'is-playoff': inPlayoffSpot(r) }">
                <td class="lg-rank">{{ r.rank }}</td>
                <td class="lg-left lg-name">{{ playerName(r.player) }}<span v-if="r.player.me" class="lg-you">{{ $t('DU') }}</span><small v-if="r.player.left"> {{ $t('(har forlatt ligaen)') }}</small></td>
                <template v-if="season.format === 'HEAD_TO_HEAD'">
                  <td>{{ r.wins + r.draws + r.losses }}</td><td>{{ r.wins }}</td><td>{{ r.draws }}</td><td>{{ r.losses }}</td>
                  <td class="lg-hide-sm">{{ r.totalScore }}</td><td class="lg-strong">{{ r.points }}</td>
                  <td class="lg-hide-sm"><span class="lg-form"><i v-for="(f, i) in r.form" :key="i" :class="`is-${f}`">{{ formLetter(f) }}</i></span></td>
                </template>
                <template v-else>
                  <td>{{ r.played }}</td><td class="lg-hide-sm">{{ num(r.average) }}</td><td class="lg-hide-sm">{{ r.best ?? '–' }}</td>
                  <td class="lg-strong">{{ r.totalScore }}</td>
                  <td class="lg-hide-sm"><span class="lg-form lg-form--scores"><i v-for="(f, i) in r.form" :key="i">{{ f }}</i></span></td>
                </template>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="season.format === 'HEAD_TO_HEAD' && season.playoffs" class="lg-foot">{{ $t('De fire øverste etter serien går til sluttspill.') }} {{ $t('Ved likt i et sluttspillmøte går den som endte høyest i serien videre.') }}</p>
        <p class="lg-foot">{{ $t('Du må svare samme dag for at quizen skal telle. Svar som venter på gjennomgang telles når de er sjekket.') }}</p>
      </section>

      <!-- Matchups (head-to-head) or day by day (points) -->
      <section v-if="tab === 'games'" class="lg-panel">
        <p v-if="!season.days.length" class="dq-card lg-empty">{{ season.state === 'UPCOMING' ? $t('Oppgjørene trekkes når sesongen starter.') : $t('Ingen dager ennå.') }}</p>
        <template v-if="season.format === 'HEAD_TO_HEAD'">
          <div v-for="d in visibleDays" :key="d.date + d.stage" class="dq-card lg-day" :class="{ 'is-today': d.date === season.today?.date, 'is-playoff': d.stage !== 'REGULAR' }">
            <div class="lg-day-head">
              <span>{{ stageName(d.stage) || dayName(d.date) }}</span>
              <small v-if="d.stage !== 'REGULAR'">{{ dayName(d.date) }}</small>
              <small v-else-if="d.date === season.today?.date">{{ $t('I dag') }}</small>
            </div>
            <LeagueMatchRow v-for="(m, i) in d.matches" :key="i" :match="m" />
            <p v-if="d.stage !== 'REGULAR'" class="lg-byes">{{ $t('Ved likt går den som endte høyest i serien videre.') }}</p>
            <p v-if="d.byes.length" class="lg-byes">{{ $t('Fri runde') }}: {{ d.byes.map(playerName).join(', ') }}</p>
          </div>
          <button v-if="hiddenDays > 0" type="button" class="btn btn-secondary lg-more" @click="showAllDays = true">{{ $t('Vis hele terminlisten ({0} dag til)¦Vis hele terminlisten ({0} dager til)', [hiddenDays]) }}</button>
        </template>
        <template v-else>
          <div v-for="d in pointsDays" :key="d.date" class="dq-card lg-day" :class="{ 'is-today': d.date === season.today?.date }">
            <div class="lg-day-head"><span>{{ dayName(d.date) }}</span></div>
            <ol class="lg-dayscores">
              <li v-for="s in d.scores" :key="s.player.id" :class="{ 'is-me': s.player.me }">
                <span>{{ playerName(s.player) }}</span>
                <b>{{ dayScoreText(s) }}</b>
              </li>
            </ol>
          </div>
        </template>
      </section>

      <!-- Stats -->
      <section v-if="tab === 'stats'" class="lg-panel">
        <div class="dq-card">
          <div class="lg-scroll">
            <table class="lg-table">
              <thead>
                <tr><th class="lg-left">{{ $t('Spiller') }}</th><th>{{ $t('Spilt') }}</th><th>{{ $t('Snitt') }}</th><th class="lg-hide-sm">{{ $t('Beste') }}</th><th :title="$t('Dager på rad nå / lengste')">{{ $t('Rekke') }}</th><th v-if="showExpert" class="lg-hide-sm">{{ $t('Slo eksperten') }}</th></tr>
              </thead>
              <tbody>
                <tr v-for="r in season.stats" :key="r.player.id" :class="{ 'is-me': r.player.me }">
                  <td class="lg-left lg-name">{{ playerName(r.player) }}</td>
                  <td>{{ r.played }}</td><td>{{ num(r.average) }}</td><td class="lg-hide-sm">{{ r.best ?? '–' }}</td>
                  <td>{{ r.currentStreak }} / {{ r.longestStreak }}</td>
                  <td v-if="showExpert" class="lg-hide-sm">{{ r.beatExpert }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p class="lg-foot">{{ $t('Rekke: dager på rad du har spilt nå, og den lengste i sesongen.') }}</p>
        </div>
        <div v-if="league.rivalries.length" class="dq-card" style="margin-top:14px;">
          <h3 class="lg-sub">{{ $t('Dine oppgjør, alle sesonger') }}</h3>
          <ul class="lg-rivals">
            <li v-for="r in league.rivalries" :key="r.opponent.id">
              <span>{{ playerName(r.opponent) }}</span>
              <span class="lg-record"><b class="is-W">{{ r.wins }}</b>–<b>{{ r.draws }}</b>–<b class="is-L">{{ r.losses }}</b></span>
            </li>
          </ul>
          <p class="lg-foot">{{ $t('Seier – uavgjort – tap.') }}</p>
        </div>
      </section>

      <!-- Members, and the owner's settings -->
      <section v-if="tab === 'members'" class="lg-panel">
        <div class="dq-card">
          <ul class="lg-members">
            <li v-for="m in league.members" :key="m.player.id">
              <span class="lg-name">{{ playerName(m.player) }}<small v-if="m.owner"> · {{ $t('styrer ligaen') }}</small><small v-if="!m.inCurrentSeason && season.format === 'HEAD_TO_HEAD' && season.scheduled"> · {{ $t('fra neste sesong') }}</small></span>
              <button v-if="league.owner && !m.player.me" type="button" class="lg-link-btn" @click="removeMember(m)">{{ $t('Fjern') }}</button>
            </li>
          </ul>
        </div>

        <div v-if="league.canStartNewSeason" class="dq-card" style="margin-top:14px;">
          <h3 class="lg-sub">{{ season.state === 'UPCOMING' ? $t('Endre sesongen før den starter') : $t('Start ny sesong') }}</h3>
          <p v-if="openEndedRunning" class="lg-foot" style="margin-top:0;">{{ $t('Den nåværende sesongen avsluttes i går, og den nye starter når du velger.') }}</p>
          <LeagueSeasonForm :submit-label="busy ? $t('Lagrer…') : (season.state === 'UPCOMING' ? $t('Lagre endringene') : $t('Start sesongen'))" :busy="busy" @submit="startSeason" />
        </div>

        <div v-if="adminView" class="lg-danger">
          <button type="button" class="btn btn-secondary lg-delete" @click="removeLeague">{{ $t('Slett ligaen') }}</button>
        </div>
        <div v-else class="lg-danger">
          <button type="button" class="btn btn-secondary" @click="leave">{{ $t('Forlat ligaen') }}</button>
          <button v-if="league.owner" type="button" class="btn btn-secondary lg-delete" @click="removeLeague">{{ $t('Slett ligaen') }}</button>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { t as _t } from '../i18n'
import api from '../services/api'
import toast from '../services/toast'
import LoadingSkeleton from '../components/LoadingSkeleton.vue'
import LeagueMatchRow from '../components/league/LeagueMatchRow.vue'
import LeagueRecap from '../components/league/LeagueRecap.vue'
import LeagueSeasonForm from '../components/league/LeagueSeasonForm.vue'
import { dayName, formatName, num, playerName, seasonSpan, stageName, stateName } from '../composables/leagueFormat'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const league = ref(null)
const loading = ref(true)
const error = ref('')
const busy = ref(false)
const tab = ref('table')
const showAllDays = ref(false)
const justCreated = route.query.ny === '1'
// The admin opens any league read-only: no "me", no owner controls - just the option to delete it
const adminView = !!route.meta.adminView
const canShare = typeof navigator !== 'undefined' && !!navigator.share

const season = computed(() => league.value?.season)
const inSeason = computed(() => season.value?.participants.some(p => p.me))

// --- The podium: the top three once anyone has a score, laid out 2nd - 1st - 3rd. With fewer than three players,
// or before anything is played, it's just the table.
const tableValue = r => (season.value?.format === 'HEAD_TO_HEAD' ? r.points : r.totalScore)
const podium = computed(() => {
  const table = season.value?.table || []
  if (table.length < 3 || !table.slice(0, 3).some(r => tableValue(r) > 0)) return []
  const [first, second, third] = table
  return [{ row: second, slot: 2 }, { row: first, slot: 1 }, { row: third, slot: 3 }]
})
const tableRows = computed(() => (podium.value.length ? season.value.table.slice(3) : season.value?.table || []))
function podiumMeta(r) {
  if (season.value?.format === 'HEAD_TO_HEAD') return `${r.wins}-${r.draws}-${r.losses}`
  return _t('snitt {0}', [num(r.average)])
}
// "Delt 1.-plass · 38 poeng" when the top is shared
const podiumNote = computed(() => {
  if (!podium.value.length) return ''
  const firsts = season.value.table.filter(r => r.rank === 1)
  if (firsts.length < 2) return ''
  return _t('Delt 1.-plass · {0}', [_t('{0} poeng¦{0} poeng', [tableValue(firsts[0])])])
})
const showExpert = computed(() => season.value?.stats.some(r => r.beatExpert > 0))
const openEndedRunning = computed(() => season.value?.format === 'POINTS' && !season.value?.endDate && season.value?.state === 'ACTIVE')

const tabs = computed(() => [
  { key: 'table', label: _t('Tabell') },
  { key: 'games', label: season.value?.format === 'HEAD_TO_HEAD' ? _t('Oppgjør') : _t('Dag for dag') },
  { key: 'stats', label: _t('Statistikk') },
  { key: 'members', label: _t('Medlemmer ({0})', [league.value?.members.length || 0]) }
])

const notice = computed(() => {
  const s = season.value
  if (!s) return ''
  if (s.notice === 'NEEDS_PLAYERS') return _t('Head-to-head trenger minst to spillere. Sesongen starter så snart noen blir med - del koden.')
  if (s.notice === 'NOT_IN_SEASON') return _t('Du ble med etter at oppgjørene ble trukket, så du er med fra neste sesong.')
  if (s.state === 'UPCOMING') {
    return s.format === 'HEAD_TO_HEAD'
      ? _t('Sesongen starter {0}. Oppgjørene trekkes da, blant alle som er med.', [dayName(s.startDate)])
      : _t('Sesongen starter {0}.', [dayName(s.startDate)])
  }
  return ''
})

// Head-to-head: the finished days, today and tomorrow, plus the playoffs - the rest of the fixture list on request
const regularCutoff = computed(() => {
  const t = season.value?.today?.date
  if (!t) return null
  const d = new Date(`${t}T12:00:00`)
  d.setDate(d.getDate() + 1)
  return d.toISOString().slice(0, 10)
})
const visibleDays = computed(() => {
  const days = season.value?.days || []
  if (showAllDays.value || !regularCutoff.value) return days
  return days.filter(d => d.stage !== 'REGULAR' || d.date <= regularCutoff.value)
})
const hiddenDays = computed(() => (season.value?.days.length || 0) - visibleDays.value.length)
const pointsDays = computed(() => [...(season.value?.days || [])].reverse())

function inPlayoffSpot(r) {
  return season.value?.format === 'HEAD_TO_HEAD' && season.value?.playoffs && r.rank <= 4
}

function formLetter(f) {
  return { W: _t('S§LeagueForm'), D: _t('U§LeagueForm'), L: _t('T§LeagueForm') }[f] || f
}

function dayScoreText(s) {
  if (s.state === 'SCORED') return s.score
  if (s.state === 'PENDING') return _t('venter')
  if (s.state === 'MISSED') return '–'
  return ''
}

async function load(seasonNumber) {
  loading.value = true
  error.value = ''
  try {
    league.value = adminView ? await api.adminGetLeague(id, seasonNumber) : await api.getLeague(id, seasonNumber)
    showAllDays.value = false
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke laste ligaen.')
  } finally {
    loading.value = false
  }
}

function inviteLink() {
  return `${window.location.origin}/liga/${league.value.code}`
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(inviteLink())
    toast.show(_t('Lenken er kopiert.'))
  } catch (e) {
    toast.show(_t('Kunne ikke kopiere. Koden er {0}.', [league.value.code]))
  }
}

async function share() {
  try {
    await navigator.share({ title: league.value.name, text: _t('Bli med i ligaen min på quizzes.no!'), url: inviteLink() })
  } catch (e) { /* closed without sharing */ }
}

async function removeMember(m) {
  if (!window.confirm(_t('Fjerne {0} fra ligaen?', [playerName(m.player)]))) return
  try {
    league.value = await api.removeLeagueMember(id, m.player.id)
  } catch (e) {
    toast.show(e.response?.data?.message || _t('Noe gikk galt - prøv igjen.'))
  }
}

async function startSeason(settings) {
  busy.value = true
  try {
    league.value = await api.newLeagueSeason(id, settings)
    tab.value = 'table'
    toast.show(_t('Sesongen er lagret.'))
  } catch (e) {
    toast.show(e.response?.data?.message || _t('Noe gikk galt - prøv igjen.'))
  } finally {
    busy.value = false
  }
}

async function leave() {
  if (!window.confirm(_t('Forlate ligaen? Resultatene dine blir stående i sesongene du har spilt.'))) return
  try {
    await api.leaveLeague(id)
    router.push('/ligaer')
  } catch (e) {
    toast.show(e.response?.data?.message || _t('Noe gikk galt - prøv igjen.'))
  }
}

async function removeLeague() {
  if (!window.confirm(_t('Slette ligaen for alle? Alle sesonger og resultater forsvinner, og det kan ikke angres.'))) return
  try {
    if (adminView) await api.adminDeleteLeague(id)
    else await api.deleteLeague(id)
    router.push(adminView ? '/admin/leagues' : '/ligaer')
  } catch (e) {
    toast.show(e.response?.data?.message || _t('Noe gikk galt - prøv igjen.'))
  }
}

onMounted(async () => {
  await load()
  if (justCreated) router.replace({ query: {} })
})
</script>

<style scoped>
.lg-head { margin-top: 12px; }
.lg-season-pick { display: inline-flex; align-items: center; gap: 8px; margin-top: 12px; font-size: 0.88rem; color: var(--text-dim); }
.lg-season-pick select { width: auto; }
.lg-invite { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; margin-bottom: 18px; padding: 14px 18px; border: 1px dashed var(--rule); border-radius: var(--radius-lg); }
.lg-invite.is-new { border-style: solid; border-color: var(--gold); }
.lg-invite-label { font-size: 0.8rem; font-weight: 700; letter-spacing: 0.05em; text-transform: uppercase; color: var(--text-dim); }
.lg-invite-code { font-family: var(--font-mono, monospace); font-size: 1.5rem; font-weight: 700; letter-spacing: 0.18em; }
.lg-invite-actions { display: flex; gap: 8px; }
.lg-notice { margin: 0 0 18px; padding: 12px 14px; border: 1px solid rgb(var(--primary-rgb) / 0.3); border-radius: var(--radius-md); background: rgb(var(--primary-rgb) / 0.06); }
.lg-today { display: flex; flex-direction: column; gap: 10px; align-items: stretch; margin-bottom: 20px; }
.lg-today .btn { align-self: flex-start; }
.lg-today-head { display: flex; justify-content: space-between; align-items: baseline; gap: 10px; }
.lg-today-count { color: var(--text-dim); font-size: 0.88rem; }
.lg-today-text { margin: 0; color: var(--text-dim); }
.lg-tabs { display: flex; gap: 4px; margin-bottom: 14px; border-bottom: 1px solid var(--rule); overflow-x: auto; }
.lg-tabs button { flex-shrink: 0; min-height: 42px; padding: 0 14px; border: none; border-bottom: 2px solid transparent; background: none; color: var(--text-dim); font: inherit; font-weight: 600; cursor: pointer; }
.lg-tabs button.is-on { color: var(--text); border-bottom-color: var(--gold); }
.lg-panel { margin-bottom: 24px; }
.lg-scroll { overflow-x: auto; }
.lg-table { width: 100%; border-collapse: collapse; font-variant-numeric: tabular-nums; }
.lg-table th { padding: 8px 6px; font-size: 0.74rem; font-weight: 700; letter-spacing: 0.04em; text-transform: uppercase; color: var(--text-dim); text-align: center; border-bottom: 1px solid var(--rule); }
.lg-table td { padding: 10px 6px; text-align: center; border-bottom: 1px solid var(--rule); }
.lg-table tr:last-child td { border-bottom: none; }
.lg-table .lg-left { text-align: left; }
.lg-table tr.is-me td { background: rgb(var(--teal-rgb) / 0.1); }
.lg-you { margin-left: 6px; padding: 1px 6px; border-radius: 999px; font-size: 0.62rem; font-weight: 700; letter-spacing: 0.04em; vertical-align: 2px; background: var(--teal); color: var(--ink-raised); }

/* The podium: three steps, the middle (1st) the tallest, in gold / silver / bronze */
.lg-podium { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); align-items: end; gap: 10px; max-width: 460px; margin: 6px auto 4px; }
.lg-podium-spot { display: flex; flex-direction: column; align-items: center; gap: 2px; min-width: 0; text-align: center; }
.lg-podium-name { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 700; font-size: 0.95rem; }
.lg-podium-value { font-family: var(--font-display); font-size: 1.5rem; font-weight: 700; line-height: 1.1; color: var(--gold); }
.lg-podium-meta { font-size: 0.74rem; color: var(--text-dim); margin-bottom: 6px; }
.lg-podium-step { width: 100%; display: grid; place-items: center; border-radius: 10px 10px 0 0; box-shadow: inset 0 -4px 0 rgb(0 0 0 / 0.15); }
.lg-podium-step b { font-family: var(--font-display); font-size: 1.6rem; }
.place-1 .lg-podium-step { height: 96px; background: linear-gradient(180deg, #f2cf6b, #d9a93a); color: #4a3205; }
.place-2 .lg-podium-step { height: 70px; background: linear-gradient(180deg, #e3e7ee, #b9c2cc); color: #2d3138; }
.place-3 .lg-podium-step { height: 52px; background: linear-gradient(180deg, #e2a978, #c0834f); color: #3d2410; }
.lg-podium-spot.is-me .lg-podium-name { color: var(--teal); }
.lg-podium-note { text-align: center; margin: 4px 0 12px; font-size: 0.82rem; color: var(--text-dim); }
.lg-podium + .lg-scroll, .lg-podium-note + .lg-scroll { margin-top: 10px; border-top: 1px solid var(--rule); }
@media (max-width: 560px) {
  .lg-podium { gap: 6px; }
  .lg-podium-value { font-size: 1.25rem; }
  .place-1 .lg-podium-step { height: 76px; }
  .place-2 .lg-podium-step { height: 56px; }
  .place-3 .lg-podium-step { height: 42px; }
}
.lg-table tr.is-playoff .lg-rank { box-shadow: inset 3px 0 0 var(--gold); }
.lg-rank { color: var(--text-dim); font-weight: 700; width: 2.5em; }
.lg-name { font-weight: 600; white-space: nowrap; }
.lg-name small { font-weight: 400; color: var(--text-dim); }
.lg-strong { font-weight: 800; }
.lg-form { display: inline-flex; gap: 3px; }
.lg-form i { display: inline-grid; place-items: center; min-width: 20px; height: 20px; padding: 0 3px; border-radius: var(--radius-sm); font-style: normal; font-size: 0.72rem; font-weight: 700; background: rgb(var(--fg-rgb) / 0.08); }
.lg-form i.is-W { background: var(--teal); color: var(--ink-raised); }
.lg-form i.is-L { background: rgb(var(--coral-rgb) / 0.25); color: var(--coral-ink); }
.lg-form--scores i { min-width: 24px; }
.lg-foot { margin: 12px 0 0; color: var(--text-dim); font-size: 0.84rem; }
.lg-empty { margin: 0; color: var(--text-dim); }
.lg-day { margin-bottom: 10px; padding: 14px 16px; }
.lg-day.is-today { border-color: var(--gold); }
.lg-day.is-playoff { border-color: rgb(var(--gold-rgb) / 0.6); }
.lg-day-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 6px; font-weight: 700; }
.lg-day-head > span { display: inline-block; }
.lg-day-head > span::first-letter { text-transform: uppercase; }
.lg-day-head small { font-weight: 500; color: var(--text-dim); }
.lg-byes { margin: 6px 0 0; color: var(--text-dim); font-size: 0.86rem; text-align: center; }
.lg-more { width: 100%; }
.lg-dayscores { margin: 0; padding: 0; list-style: none; }
.lg-dayscores li { display: flex; justify-content: space-between; padding: 6px 4px; border-bottom: 1px solid var(--rule); }
.lg-dayscores li:last-child { border-bottom: none; }
.lg-dayscores li.is-me { font-weight: 700; }
.lg-sub { margin: 0 0 12px; font-size: 1.05rem; }
.lg-rivals, .lg-members { margin: 0; padding: 0; list-style: none; }
.lg-rivals li, .lg-members li { display: flex; justify-content: space-between; align-items: center; gap: 10px; min-height: 44px; border-bottom: 1px solid var(--rule); }
.lg-rivals li:last-child, .lg-members li:last-child { border-bottom: none; }
.lg-record { font-variant-numeric: tabular-nums; color: var(--text-dim); }
.lg-record b { color: var(--text); }
.lg-record b.is-W { color: var(--teal-ink); }
.lg-record b.is-L { color: var(--coral-ink); }
.lg-link-btn { border: none; background: none; color: var(--coral-ink); font: inherit; font-size: 0.9rem; cursor: pointer; padding: 8px; }
.lg-danger { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 18px; }
.lg-delete { color: var(--coral-ink); }
@media (max-width: 560px) {
  .lg-hide-sm { display: none; }
  .lg-invite-code { font-size: 1.3rem; }
}
</style>
