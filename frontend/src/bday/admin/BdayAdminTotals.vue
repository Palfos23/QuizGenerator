<template>
  <div>
    <p class="page-subtitle">Totalen for hver gjest: quiz + Mario Kart + musikkquiz (lagets poeng) + bonus fra hjulet. Oppdateres automatisk.</p>
    <div v-if="error" class="banner error">{{ error }}</div>
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
const error = ref('')

async function load() {
  try {
    standings.value = await bdayApi.standings()
    error.value = ''
  } catch (e) {
    error.value = 'Kunne ikke laste totalen.'
  }
}

usePolling(load, 6000)
</script>
