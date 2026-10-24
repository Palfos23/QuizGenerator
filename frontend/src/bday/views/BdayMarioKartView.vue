<template>
  <div class="bm">
    <div class="bm-top">
      <h1>🏁 Mario Kart</h1>
      <router-link to="/bday" class="bm-link">Oversikt</router-link>
    </div>

    <div v-if="error" class="banner error">{{ error }}</div>

    <!-- A time was just registered -->
    <div v-if="done" class="bm-done">
      <div class="bm-done-label">{{ done.name }} fikk</div>
      <div class="bm-done-time">{{ formatLap(done.result.timeMs) }}</div>
      <div class="bm-done-line">Foreløpig plass <strong>{{ done.result.rank }}</strong> · <strong>{{ done.result.points }}</strong> poeng</div>
      <router-link to="/bday/leaderboard" class="btn btn-primary" style="margin-top:16px;">Se topplisten</router-link>
    </div>

    <template v-else-if="data">
      <div v-if="!data.open" class="banner" style="background:rgb(var(--gold-rgb) / 0.08); border-color:var(--gold);">
        Registreringen er ikke åpen akkurat nå. Vent til arrangøren sier fra.
      </div>

      <!-- 1. Who are you? -->
      <template v-if="data.open && !picked">
        <p class="page-subtitle">Når du har kjørt, velg navnet ditt og skriv inn tiden på beste runde.</p>
        <input v-if="data.guests.length > 8" v-model="search" type="search" class="bm-search" placeholder="Søk etter navnet ditt" autocomplete="off" />
        <div class="bm-names">
          <button
            v-for="g in visibleGuests"
            :key="g.id"
            type="button"
            class="bm-name"
            :disabled="g.hasTime"
            @click="picked = g"
          >{{ g.name }}<span v-if="g.hasTime" class="bm-name-note">har registrert tid</span></button>
        </div>
      </template>

      <!-- 2. Enter the time -->
      <template v-else-if="data.open && picked">
        <div class="bm-who">
          <span>Tid for <strong>{{ picked.name }}</strong></span>
          <button type="button" class="bm-link bm-link--btn" @click="picked = null; time = ''">Ikke deg? Bytt navn</button>
        </div>
        <p class="page-subtitle">Beste runde, slik den står i spillet: minutter, sekunder og tusendeler.</p>
        <LapTimeInput v-model="time" @enter="submit" />
        <p v-if="time" class="bm-preview">Du registrerer <strong>{{ formatLap(previewMs) }}</strong></p>
        <button class="btn btn-primary" style="margin-top:16px; min-height:50px; width:100%;" :disabled="submitting || !time" @click="submit">
          {{ submitting ? 'Lagrer…' : 'Registrer tid' }}
        </button>
        <p class="picker-hint" style="margin-top:8px;">Du kan bare registrere én tid. Tastet feil? Si fra til arrangøren.</p>
      </template>

      <!-- The ranking so far -->
      <section v-if="data.ranking.length" class="bm-rank">
        <h2>Beste runder så langt</h2>
        <div v-for="r in data.ranking" :key="r.guestId" class="bm-rank-row">
          <span class="bm-rank-n">{{ r.rank }}</span>
          <span class="bm-rank-name">{{ r.name }}</span>
          <span class="bm-rank-time">{{ formatLap(r.timeMs) }}</span>
          <span v-if="r.points !== null && r.points !== undefined" class="bm-rank-pts">{{ r.points }} p</span>
        </div>
      </section>
    </template>
    <div v-else-if="!error" style="color:var(--text-dim);">Laster…</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import bdayApi from '../services/bdayApi'
import LapTimeInput from '../components/LapTimeInput.vue'
import { formatLap } from '../lib/lap'

// The guests' Mario Kart page: pick your name, type your best lap once, see where it puts you. The host opens and closes
// registration, and can fix a time that was typed wrong.
const data = ref(null)
const error = ref('')
const search = ref('')
const picked = ref(null)
const time = ref('')
const submitting = ref(false)
const done = ref(null)

const visibleGuests = computed(() => {
  const term = search.value.trim().toLowerCase()
  return term ? data.value.guests.filter(g => g.name.toLowerCase().includes(term)) : data.value.guests
})

// "1:23.456" -> ms, only to show the guest what they are about to register (the server does the real parsing)
const previewMs = computed(() => {
  const m = /^(\d+):(\d{2})(?:\.(\d+))?$/.exec(time.value)
  if (!m) return 0
  return (Number(m[1]) * 60 + Number(m[2])) * 1000 + Number(((m[3] || '') + '00').slice(0, 3))
})

onMounted(load)

async function load() {
  error.value = ''
  try {
    data.value = await bdayApi.marioKart()
  } catch (e) {
    error.value = 'Kunne ikke laste siden. Prøv å laste den på nytt.'
  }
}

async function submit() {
  if (!time.value || submitting.value) return
  submitting.value = true
  error.value = ''
  try {
    const result = await bdayApi.enterLapTime(picked.value.id, time.value)
    done.value = { name: picked.value.name, result }
  } catch (e) {
    error.value = e.response?.data?.message || 'Kunne ikke registrere tiden. Prøv igjen.'
    if (e.response?.status === 409 || e.response?.status === 400) await load()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.bm { max-width: 520px; margin: 0 auto; padding: 18px 16px 60px; width: 100%; }
.bm-top { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.bm-top h1 { margin: 0; font-size: 1.8rem; }
.bm-link { color: var(--text); font-weight: 600; font-size: 0.9rem; text-decoration: underline; text-underline-offset: 3px; }
.bm-link--btn { background: none; border: none; padding: 0; cursor: pointer; font-family: inherit; }
.bm-search { width: 100%; margin-bottom: 10px; min-height: 46px; font-size: 1rem; }
.bm-names { display: flex; flex-direction: column; gap: 8px; }
.bm-name {
  min-height: 52px;
  padding: 12px 16px;
  text-align: left;
  background: var(--ink-raised);
  color: var(--text);
  border: 2px solid var(--rule);
  border-radius: 3px;
  font-family: var(--font-display);
  font-size: 1.15rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.bm-name:disabled { opacity: 0.5; cursor: default; border-style: dashed; }
.bm-name-note { font-family: var(--font-body); font-size: 0.75rem; font-weight: 500; color: var(--text-dim); }
.bm-who { display: flex; justify-content: space-between; align-items: baseline; gap: 10px; flex-wrap: wrap; margin: 8px 0; }
.bm-preview { margin: 10px 0 0; color: var(--text-dim); }
.bm-done { text-align: center; padding: 28px 0 10px; }
.bm-done-label { font-family: var(--font-mono); font-size: 0.8rem; letter-spacing: 0.08em; text-transform: uppercase; color: var(--text-dim); }
.bm-done-time { font-family: var(--font-mono); font-weight: 700; font-size: 3rem; line-height: 1.1; margin: 4px 0; }
.bm-done-line { color: var(--text-dim); }
.bm-rank { margin-top: 28px; }
.bm-rank h2 { font-size: 1.2rem; margin: 0 0 8px; }
.bm-rank-row { display: flex; align-items: baseline; gap: 10px; padding: 7px 0; border-top: 1px solid var(--line); }
.bm-rank-n { width: 24px; font-family: var(--font-mono); font-size: 0.82rem; color: var(--text-dim); }
.bm-rank-name { flex: 1; font-family: var(--font-display); font-weight: 600; min-width: 0; overflow-wrap: anywhere; }
.bm-rank-time { font-family: var(--font-mono); font-weight: 600; }
.bm-rank-pts { font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-dim); width: 44px; text-align: right; }
</style>
