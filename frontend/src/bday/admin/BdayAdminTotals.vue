<template>
  <div>
    <p class="page-subtitle">Totalen for hver gjest: quiz + Mario Kart + musikkquiz (lagets poeng) + bonus fra hjulet. Oppdateres automatisk.</p>
    <div v-if="error" class="banner error">{{ error }}</div>

    <!-- Final reveal of the public board -->
    <div class="rv-box" v-if="reveal">
      <div class="rv-head">
        <strong>Toppliste for gjestene</strong>
        <span class="rv-state">
          <template v-if="!reveal.hideTop">Alt er synlig</template>
          <template v-else-if="reveal.masked">Topp {{ reveal.tiles }} er skjult - {{ reveal.revealed }} av {{ reveal.tiles }} avslørt</template>
          <template v-else>Alt avslørt</template>
        </span>
      </div>
      <p class="picker-hint" style="margin:6px 0 10px;">
        Skjul de beste plasseringene og snu dem én og én mot slutten. Så lenge noe er skjult, ser gjestene hverken navn på de skjulte plassene
        eller poeng for noen konkurranse. Bare du kan snu flisene, her (PIN-koden kreves) - Topplisten for gjestene bare viser det.
      </p>
      <div class="rv-row">
        <span class="rv-label">Skjul topp</span>
        <button v-for="n in [0, 5, 10, 15]" :key="n" type="button" class="rv-chip" :class="{ 'is-on': reveal.hideTop === n }" :disabled="busy" @click="setHide(n)">{{ n === 0 ? 'Av' : n }}</button>
      </div>
      <div v-if="reveal.hideTop" class="rv-row">
        <button class="btn btn-primary btn-sm" :disabled="busy || !reveal.masked" @click="next">
          {{ reveal.masked ? 'Avslør plass ' + (reveal.tiles - reveal.revealed) : 'Alt er avslørt' }}
        </button>
        <button class="btn btn-secondary btn-sm" :disabled="busy || reveal.revealed === 0" @click="setRevealed(0)">Skjul alle igjen</button>
        <button class="btn btn-secondary btn-sm" :disabled="busy || !reveal.masked" @click="setRevealed(reveal.tiles)">Avslør alle</button>
        <router-link to="/bday/leaderboard" class="btn btn-secondary btn-sm">Åpne Topplisten</router-link>
      </div>
    </div>

    <p class="picker-hint" style="margin:14px 0 4px;">Dette er hele tabellen - du ser alt, uansett hva som er skjult for gjestene.</p>
    <BdayStandingsTable :players="standings.players" />
    <p class="picker-hint" style="margin-top:12px;">
      Quiz-poeng teller først når gjesten er ferdig. Rett poengene i fanene for Quiz, Mario Kart, Lag og Hjulet - totalen følger med.
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import bdayApi from '../services/bdayApi'
import BdayStandingsTable from '../components/BdayStandingsTable.vue'
import { usePolling } from '../../composables/usePolling'

const standings = ref({ players: [] })
const reveal = ref(null)
const error = ref('')
const busy = ref(false)

async function load() {
  try {
    standings.value = await bdayApi.adminStandings()
    if (!busy.value) reveal.value = standings.value.reveal
    error.value = ''
  } catch (e) {
    error.value = 'Kunne ikke laste totalen.'
  }
}

async function run(call) {
  busy.value = true
  error.value = ''
  try {
    reveal.value = await call()
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke lagre.'
  } finally {
    busy.value = false
  }
}

const setHide = n => run(() => bdayApi.adminSetReveal({ hideTop: n }))
const setRevealed = n => run(() => bdayApi.adminSetReveal({ revealed: n }))
const next = () => run(() => bdayApi.adminRevealNext())

usePolling(load, 6000)
</script>

<style scoped>
.rv-box { border: 2px solid var(--rule); border-radius: 3px; padding: 14px; background: var(--ink-raised); margin-bottom: 8px; }
.rv-head { display: flex; justify-content: space-between; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.rv-state { font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-dim); }
.rv-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 10px; }
.rv-label { color: var(--text-dim); font-size: 0.9rem; margin-right: 4px; }
.rv-chip { padding: 5px 16px; background: transparent; color: var(--text); border: 2px solid var(--rule); border-radius: 3px; font-family: var(--font-mono); font-weight: 600; cursor: pointer; }
.rv-chip.is-on { background: var(--text); color: var(--ink-raised); }
</style>
