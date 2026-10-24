<template>
  <div class="bh">
    <h1>🎂 Bursdagsfest</h1>
    <p class="page-subtitle">Flere konkurranser i kveld - poengene legges sammen på topplisten.</p>

    <router-link to="/bday/quiz" class="bh-card">
      <span class="bh-title">Quiz</span>
      <span class="bh-text">Velg navnet ditt og svar på spørsmålene på din egen telefon.</span>
    </router-link>

    <router-link to="/bday/mariokart" class="bh-card">
      <span class="bh-title">Mario Kart <span v-if="open !== null" class="bh-pill" :class="open ? 'is-open' : 'is-closed'">{{ open ? 'åpen' : 'stengt' }}</span></span>
      <span class="bh-text">Registrer din beste runde når du har kjørt ferdig.</span>
    </router-link>

    <router-link to="/bday/leaderboard" class="bh-card">
      <span class="bh-title">Topplisten</span>
      <span class="bh-text">Totalen for alle konkurransene, oppdatert underveis.</span>
    </router-link>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import bdayApi from '../services/bdayApi'

// The front door for the guests: one place to find every competition of the night. (The host's page is /bday/admin -
// deliberately not linked from here.)
const open = ref(null) // whether Mario Kart times can be registered right now - null until known

onMounted(async () => {
  try {
    open.value = (await bdayApi.marioKart()).open
  } catch (e) {
    // just a hint on the card - the page works without it
  }
})
</script>

<style scoped>
.bh { max-width: 520px; margin: 0 auto; padding: 24px 16px 60px; width: 100%; }
.bh h1 { margin-bottom: 4px; }
.bh-card {
  display: block;
  margin-top: 14px;
  padding: 18px 18px 16px;
  background: var(--ink-raised);
  border: 2px solid var(--rule);
  border-radius: 3px;
  color: var(--text);
  text-decoration: none;
}
.bh-card:active { background: var(--text); color: var(--ink-raised); }
.bh-title { display: block; font-family: var(--font-display); font-weight: 600; font-size: 1.5rem; }
.bh-text { display: block; margin-top: 4px; color: var(--text-dim); }
.bh-card:active .bh-text { color: inherit; }
.bh-pill { font-family: var(--font-body); font-size: 0.7rem; font-weight: 700; letter-spacing: 0.04em; border: 1.5px solid currentColor; border-radius: 3px; padding: 1px 7px; vertical-align: 4px; margin-left: 6px; }
.bh-pill.is-open { color: var(--teal); }
.bh-pill.is-closed { color: var(--text-dim); }
</style>
