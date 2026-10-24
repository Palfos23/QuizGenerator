<template>
  <div class="bl">
    <div class="bl-top">
      <h1>🎉 Toppliste</h1>
      <router-link to="/bday" class="bl-link">Oversikt</router-link>
    </div>
    <p class="page-subtitle">
      <template v-if="masked">Topp {{ standings.reveal.tiles }} er skjult og avsløres på slutten. Poengene per konkurranse vises etterpå.</template>
      <template v-else>Quiz + Mario Kart + musikkquiz + bonus fra hjulet. Oppdateres automatisk.</template>
    </p>

    <BdayStandingsTable :players="standings.players" :show-parts="!masked" :flipped="flipped" large />

    <div class="bl-grid">
      <section v-if="standings.marioKart.length" class="bl-box">
        <h2>Mario Kart - beste runder</h2>
        <div v-for="r in standings.marioKart" :key="r.guestId" class="bl-line">
          <span class="bl-n">{{ r.rank }}</span>
          <span class="bl-name">{{ r.name }}</span>
          <span class="bl-time">{{ formatLap(r.timeMs) }}</span>
          <span class="bl-pts">{{ r.points }} p</span>
        </div>
      </section>

      <section v-if="standings.teams.length" class="bl-box">
        <h2>Musikkquiz - lag</h2>
        <div v-for="t in standings.teams" :key="t.id" class="bl-line">
          <span class="bl-name">{{ t.name }}<small> {{ t.members }} {{ t.members === 1 ? 'spiller' : 'spillere' }}</small></span>
          <span class="bl-pts bl-pts--big">{{ t.musicScore }}</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import bdayApi from '../services/bdayApi'
import BdayStandingsTable from '../components/BdayStandingsTable.vue'
import { formatLap } from '../lib/lap'
import { usePolling } from '../../composables/usePolling'

// The big-screen board for the whole night: every competition added together, with the two team/lap lists below.
const standings = ref({ players: [], teams: [], marioKart: [], marioKartOpen: false, reveal: { masked: false, tiles: 0 } })
const flipped = ref([]) // guests whose tile was just turned, for the flip animation

const masked = computed(() => !!standings.value.reveal?.masked)

async function fetchStandings() {
  try {
    const next = await bdayApi.standings()
    const before = standings.value.players
    // a place that was a face-down tile a moment ago and now has a name has just been turned
    flipped.value = next.players.filter((p, i) => !p.hidden && before[i]?.hidden).map(p => p.guestId)
    standings.value = next
  } catch (e) {
    // display-only board - fail quietly and just try again next poll
  }
}

// this board only shows - tiles are turned by the host, from the PIN-gated admin page - so it polls often enough that a
// turn shows up within a couple of seconds
usePolling(fetchStandings, 2000)
</script>

<style scoped>
.bl { max-width: 960px; margin: 0 auto; padding: 24px 16px 60px; width: 100%; }
.bl-top { display: flex; justify-content: space-between; align-items: baseline; gap: 10px; }
.bl-top h1 { margin: 0; font-size: 2.4rem; }
.bl-link { color: var(--text); font-weight: 600; font-size: 0.9rem; text-decoration: underline; text-underline-offset: 3px; }
.bl-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 16px; margin-top: 32px; }
.bl-box { background: var(--ink-raised); border: 2px solid var(--rule); border-radius: 3px; padding: 14px 16px; }
.bl-box h2 { font-size: 1.2rem; margin: 0 0 6px; }
.bl-line { display: flex; align-items: baseline; gap: 10px; padding: 7px 0; border-top: 1px solid var(--line); }
.bl-line:first-of-type { border-top: none; }
.bl-n { width: 22px; font-family: var(--font-mono); font-size: 0.82rem; color: var(--text-dim); }
.bl-name { flex: 1; min-width: 0; font-family: var(--font-display); font-weight: 600; overflow-wrap: anywhere; }
.bl-name small { font-family: var(--font-body); font-weight: 500; color: var(--text-dim); }
.bl-time { font-family: var(--font-mono); font-weight: 600; }
.bl-pts { font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-dim); }
.bl-pts--big { font-family: var(--font-display); font-weight: 700; font-size: 1.4rem; color: var(--text); }
</style>
