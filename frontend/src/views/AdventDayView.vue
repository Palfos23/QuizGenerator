<template>
  <div class="dq-page adv-page">
    <router-link to="/julekalender" class="adv-back">← {{ $t('Julekalenderen') }}</router-link>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

    <template v-else-if="detail">
      <header class="dq-hero adv-hero">
        <span class="dq-eyebrow">{{ $t('Luke {0}', [detail.day]) }} · {{ dateLabel }}</span>
        <h1 class="dq-title adv-category">{{ detail.category }}</h1>
        <div class="adv-pills">
          <span class="dq-pill">{{ info.emoji }} {{ info.name }}</span>
          <span v-if="detail.finale" class="dq-pill dq-pill--wait">🎄 {{ $t('Finale - tredoble poeng') }}</span>
        </div>
        <p class="adv-rule">{{ info.rules }}</p>
      </header>

      <!-- ============================================================ open: the form -->
      <section v-if="detail.status === 'OPEN'" class="dq-card adv-form">
        <p v-if="detail.type === 'ONE_OF_FOUR'" class="adv-hint">{{ $t('Spillmesteren har bare ett svar - skriv 4 forskjellige svar, så øker du sjansen.') }}</p>
        <p v-else class="adv-hint">{{ $t('Skriv 4 forskjellige svar du tror spillmesteren også har valgt.') }}</p>

        <div v-for="(_, i) in answers" :key="i" class="adv-answer">
          <label :for="'ans' + i">{{ i + 1 }}</label>
          <input :id="'ans' + i" type="text" v-model="answers[i]" maxlength="100" autocomplete="off" :placeholder="$t('Svar {0}', [i + 1])" />
        </div>

        <!-- Risk or reward: pick before sending -->
        <fieldset v-if="detail.type === 'RISK_REWARD'" class="adv-choice">
          <legend>{{ $t('Hva gjør du?') }}</legend>
          <label class="adv-option" :class="{ 'is-on': !risky }">
            <input type="radio" :value="false" v-model="risky" />
            <span><strong>{{ $t('Spill trygt') }}</strong><br />{{ $t('Vanlige poeng: 1 per treff.') }}</span>
          </label>
          <label class="adv-option" :class="{ 'is-on': risky }">
            <input type="radio" :value="true" v-model="risky" />
            <span><strong>{{ $t('Sats!') }}</strong><br />{{ $t('2 poeng per treff - men -1 poeng hvis du ikke treffer noen.') }}</span>
          </label>
        </fieldset>

        <!-- The joker, explained where it is used -->
        <div v-if="detail.jokerAllowed" class="adv-joker" :class="{ 'is-on': useJoker }">
          <label class="adv-joker-label">
            <input type="checkbox" v-model="useJoker" :disabled="!useJoker && detail.jokersLeft === 0" />
            <span>
              <strong>🃏 {{ $t('Bruk en joker på denne luka') }}</strong><br />
              <template v-if="detail.jokersLeft > 0 || useJoker">{{ $t('Poengene dine på luka dobles. Du har {0} av 3 jokere igjen. Ombestemmer du deg før midnatt, får du den tilbake.', [detail.jokersLeft]) }}</template>
              <template v-else>{{ $t('Du har brukt alle 3 jokerne dine.') }}</template>
            </span>
          </label>
        </div>
        <p v-else-if="detail.finale" class="adv-hint">🃏 {{ $t('Jokeren kan ikke brukes på finalen.') }}</p>

        <div class="adv-submit">
          <button class="btn btn-primary" :disabled="saving" @click="save">
            {{ saving ? $t('Lagrer…') : detail.entry ? $t('Oppdater svarene') : $t('Send inn svarene') }}
          </button>
          <span v-if="saved" class="adv-saved">✓ {{ $t('Lagret') }}</span>
        </div>
        <p class="adv-hint" style="margin-bottom:0;">{{ $t('Du kan endre svarene frem til midnatt. Dagen etter får du se spillmesterens svar og poengene dine.') }}</p>
      </section>

      <!-- ============================================================ closed: the result -->
      <template v-else>
        <section v-if="detail.provisional" class="adv-prelim">{{ $t('Foreløpig resultat - spillmesteren gjennomgår fortsatt svarene, så poengene kan endre seg.') }}</section>

        <section v-if="detail.result" class="dq-card adv-result">
          <div class="adv-points" :class="{ 'is-neg': detail.result.points < 0 }">{{ detail.result.points > 0 ? '+' : detail.result.points < 0 ? '−' : '' }}{{ Math.abs(detail.result.points) }}</div>
          <div class="adv-points-l">{{ $t('poeng¦poeng', [Math.abs(detail.result.points)]) }}</div>
          <ul class="adv-breakdown">
            <li>{{ $t('{0} av 4 treff', [detail.result.matches]) }}<template v-if="detail.type === 'ONE_OF_FOUR'"> ({{ detail.result.matches ? $t('du traff gullsvaret!') : $t('ingen treff på gullsvaret') }})</template></li>
            <li v-if="detail.result.wildcardHit">🌟 {{ $t('Du traff wildcard: +3 poeng') }}</li>
            <li v-if="detail.entry?.risky">🎲 {{ $t('Du satset') }}</li>
            <li v-if="detail.result.jokerBonus > 0">🃏 {{ $t('Jokeren ga deg +{0} poeng', [detail.result.jokerBonus]) }}</li>
            <li v-if="detail.finale">🎄 {{ $t('Finalen: alle poeng tredoblet') }}</li>
          </ul>
          <button class="btn btn-secondary btn-sm" @click="share">{{ $t('Del resultatet') }}</button>
          <span v-if="shareNote" class="adv-saved" style="margin-left:10px;">{{ shareNote }}</span>
        </section>
        <section v-else class="dq-card dq-empty">
          <strong>{{ $t('Du var ikke med denne dagen') }}</strong>
          {{ $t('Men du kan se hva spillmesteren svarte og hva de andre skrev.') }}
        </section>

        <!-- what you wrote -->
        <template v-if="detail.entry">
          <h2 class="dq-section-title">{{ $t('Svarene dine') }}</h2>
          <div class="dq-card adv-list">
            <div v-for="(a, i) in detail.entry.answers" :key="i" class="adv-line">
              <span class="adv-mark" :class="matchOf(i) >= 0 ? 'is-ok' : 'is-bad'">{{ matchOf(i) >= 0 ? '✓' : '✕' }}</span>
              <span class="adv-line-main">{{ a }}<small v-if="matchOf(i) >= 0 && differs(a, detail.hidden[matchOf(i)].text)">= {{ detail.hidden[matchOf(i)].text }}</small></span>
            </div>
          </div>
        </template>

        <!-- the game master's answers -->
        <h2 class="dq-section-title">{{ $t('Spillmesterens svar') }}</h2>
        <div class="dq-card adv-list">
          <div v-for="(h, i) in detail.hidden" :key="i" class="adv-line">
            <span class="adv-mark">{{ h.wildcard ? '🌟' : detail.type === 'ONE_OF_FOUR' ? '🥇' : '•' }}</span>
            <span class="adv-line-main">{{ h.text }}<small v-if="h.wildcard"> {{ $t('(wildcard)') }}</small></span>
            <span class="adv-line-side">{{ $t('{0} % traff', [h.matchedPercent]) }}</span>
          </div>
          <p class="adv-hint" style="margin:8px 0 0;">
            <template v-if="detail.gamemasterPoints > 0">🎩 {{ $t('Spillmesteren fikk {0} poeng på denne luka.', [detail.gamemasterPoints]) }}</template>
            <template v-else>🎩 {{ $t('Spillmesteren fikk ingen poeng på denne luka.') }}</template>
          </p>
        </div>

        <!-- what everybody wrote -->
        <template v-if="detail.crowd && detail.crowd.length">
          <h2 class="dq-section-title">{{ $t('Hva skrev folk?') }} <small>{{ $t('{0} spillere', [detail.entrants]) }}</small></h2>
          <div class="dq-card adv-list">
            <div v-for="c in detail.crowd" :key="c.text" class="adv-crowd">
              <div class="adv-crowd-top">
                <span class="adv-line-main">{{ c.text }}<small v-if="c.matchedIndex !== null"> ✓</small></span>
                <span class="adv-line-side">{{ c.percent }} %</span>
              </div>
              <div class="adv-bar"><span :class="{ 'is-hit': c.matchedIndex !== null }" :style="{ width: c.percent + '%' }"></span></div>
            </div>
          </div>
        </template>
      </template>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { t as _t, locale } from '../i18n'
import api from '../services/api'
import { typeInfo, shareText, shareOrCopy } from '../services/advent'

const route = useRoute()
const day = computed(() => Number(route.params.n))
const detail = ref(null)
const year = ref(new Date().getFullYear())
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const saved = ref(false)
const shareNote = ref('')
const answers = ref(['', '', '', ''])
const risky = ref(false)
const useJoker = ref(false)

const info = computed(() => typeInfo(detail.value?.type))
const dateLabel = computed(() => {
  if (!detail.value) return ''
  const [y, m, d] = detail.value.date.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString(locale, { day: 'numeric', month: 'long' })
})

function differs(a, b) {
  return a.trim().toLowerCase() !== b.trim().toLowerCase()
}

function matchOf(i) {
  return detail.value?.result?.answerMatches?.[i] ?? -1
}

function apply(d) {
  detail.value = d
  year.value = Number(d.date.split('-')[0])
  if (d.entry) {
    answers.value = [...d.entry.answers]
    risky.value = d.entry.risky
    useJoker.value = d.entry.jokerUsed
  }
}

// Reloads when the number in the address changes (the browser's back/forward between two doors reuses this page)
async function load() {
  loading.value = true
  error.value = ''
  detail.value = null
  answers.value = ['', '', '', '']
  risky.value = false
  useJoker.value = false
  saved.value = false
  try {
    apply(await api.getAdventDay(day.value))
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke laste luka.')
  } finally {
    loading.value = false
  }
}
watch(day, load, { immediate: true })

async function save() {
  saving.value = true
  saved.value = false
  error.value = ''
  try {
    apply(await api.submitAdventEntry(day.value, { answers: answers.value, risky: risky.value, useJoker: useJoker.value }))
    saved.value = true
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke lagre svarene.')
  } finally {
    saving.value = false
  }
}

async function share() {
  const result = await shareOrCopy(shareText(detail.value, year.value))
  shareNote.value = result === 'copied' ? _t('Kopiert!') : ''
  if (result === 'copied') setTimeout(() => { shareNote.value = '' }, 2500)
}
</script>

<style scoped>
.adv-page { max-width: 640px; }
.adv-back { display: inline-block; margin-bottom: 10px; color: var(--text-dim); font-size: 0.9rem; text-decoration: none; }
.adv-category { overflow-wrap: anywhere; }
.adv-pills { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px; }
.adv-rule { margin: 12px 0 0; color: var(--text-dim); }
.adv-hint { color: var(--text-dim); font-size: 0.92rem; margin: 0 0 14px; }

.adv-answer { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.adv-answer label { width: 20px; font-family: var(--font-mono); color: var(--text-dim); margin: 0; }
.adv-answer input { flex: 1; font-size: 1.05rem; }

.adv-choice { border: none; padding: 0; margin: 16px 0 0; display: grid; gap: 8px; }
.adv-choice legend { font-weight: 600; margin-bottom: 6px; padding: 0; }
.adv-option, .adv-joker-label { display: flex; gap: 10px; align-items: flex-start; border: 2px solid var(--rule); border-radius: 3px; padding: 10px 12px; cursor: pointer; text-transform: none; letter-spacing: normal; font-weight: 400; color: var(--text); font-size: 0.95rem; margin: 0; }
.adv-option.is-on { background: rgb(var(--gold-rgb) / 0.1); border-color: var(--gold); }
.adv-joker { margin-top: 16px; }
.adv-joker-label { border-style: dashed; }
.adv-joker.is-on .adv-joker-label { border-style: solid; border-color: var(--gold); background: rgb(var(--gold-rgb) / 0.1); }
.adv-option input, .adv-joker-label input { margin-top: 4px; }

.adv-submit { display: flex; align-items: center; gap: 14px; margin: 18px 0 10px; }
.adv-saved { color: var(--teal-ink, var(--teal)); font-weight: 600; font-size: 0.92rem; }

.adv-result { text-align: center; }
.adv-points { font-family: var(--font-display); font-weight: 700; font-size: 4rem; line-height: 1; color: var(--teal-ink, var(--teal)); }
.adv-points.is-neg { color: var(--coral-ink, var(--coral)); }
.adv-points-l { color: var(--text-dim); text-transform: uppercase; letter-spacing: 0.08em; font-size: 0.78rem; margin-bottom: 10px; }
.adv-breakdown { list-style: none; margin: 0 0 14px; padding: 0; display: grid; gap: 4px; font-size: 0.95rem; }

.adv-list { padding: 8px 14px; }
.adv-line { display: flex; align-items: baseline; gap: 10px; padding: 8px 0; border-top: 1px solid var(--line); }
.adv-line:first-child { border-top: none; }
.adv-line-main { flex: 1; min-width: 0; overflow-wrap: anywhere; }
.adv-line-main small { color: var(--text-dim); margin-left: 6px; }
.adv-prelim { margin-bottom: 14px; padding: 10px 14px; border: 2px solid var(--gold); border-radius: 3px; background: rgb(var(--gold-rgb) / 0.08); font-size: 0.92rem; }
.adv-line-side { font-family: var(--font-mono); font-size: 0.82rem; color: var(--text-dim); white-space: nowrap; }
.adv-mark { width: 22px; text-align: center; font-weight: 700; }
.adv-mark.is-ok { color: var(--teal-ink, var(--teal)); }
.adv-mark.is-bad { color: var(--coral-ink, var(--coral)); }
.adv-crowd { padding: 7px 0; border-top: 1px solid var(--line); }
.adv-crowd:first-child { border-top: none; }
.adv-crowd-top { display: flex; gap: 10px; align-items: baseline; }
.adv-bar { height: 6px; background: var(--line); border-radius: 3px; margin-top: 5px; overflow: hidden; }
.adv-bar span { display: block; height: 100%; background: var(--text); opacity: 0.45; border-radius: 3px; }
.adv-bar span.is-hit { background: var(--teal); opacity: 1; }
</style>
