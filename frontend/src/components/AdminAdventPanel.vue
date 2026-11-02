<template>
  <div class="dq-page dq-page--wide adm-adv">
    <header class="dq-hero">
      <span class="dq-eyebrow">Admin</span>
      <h1 class="dq-title">{{ $t('Julekalender') }} {{ season?.year }}</h1>
      <p class="dq-sub">{{ $t('Fyll ut de 24 lukene i forkant, og gå gjennom hva spillerne skrev når en luke er over.') }}</p>
    </header>

    <div v-if="error" class="banner error">{{ error }}</div>
    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>

    <template v-else-if="season">
      <!-- try it out before December -->
      <section class="dq-card adm-test">
        <h2 class="adm-h">🧪 {{ $t('Testmodus') }}</h2>
        <p class="picker-hint" style="margin:0 0 10px;">
          {{ $t('Late som om det er en annen dato, så du kan prøve julekalenderen før 1. desember. Gjelder alle som åpner siden, og slår seg av av seg selv etter 3 timer. Du må være logget inn som en vanlig spiller (ikke admin) for å spille lukene.') }}
        </p>
        <div class="adm-test-row">
          <input type="date" class="adv-input" v-model="testDate" :min="testMin" :max="testMax" />
          <button class="btn btn-secondary btn-sm" :disabled="!testDate || testBusy" @click="setTest(testDate)">{{ $t('Sett dato') }}</button>
          <button v-if="test?.active" class="btn btn-secondary btn-sm" :disabled="testBusy" @click="setTest(null)">{{ $t('Slå av') }}</button>
          <button v-if="test?.canReset" class="btn btn-danger btn-sm" :disabled="testBusy" @click="confirmReset = true">{{ $t('Nullstill testdata') }}</button>
        </div>
        <p v-if="test?.active" class="adm-test-status">
          ✓ {{ $t('Testmodus er på: kalenderen tror det er {0}. Slås av automatisk {1}.', [dateLabel(test.date), untilLabel]) }}
        </p>
        <p v-else class="adm-test-status adm-test-status--off">{{ $t('Testmodus er av - kalenderen følger den ekte datoen.') }}</p>
      </section>
      <ConfirmModal
        v-if="confirmReset"
        :title="$t('Nullstille testdata?')"
        :message="$t('Alle svar fra spillere, dine rettelser og «gjennomgått»-markeringene slettes. Lukene du har fylt ut blir stående. Dette kan bare gjøres før julekalenderen har startet.')"
        :confirm-text="$t('Nullstill')"
        @confirm="doReset"
        @cancel="confirmReset = false"
      />

      <!-- the 24 doors -->
      <div class="adm-grid">
        <button
          v-for="d in season.days"
          :key="d.day"
          type="button"
          class="adm-door"
          :class="{ 'is-selected': selected === d.day, 'is-ready': d.ready, 'is-review': d.closed && d.entries > 0 && !d.approved }"
          @click="select(d.day)"
        >
          <span class="adm-door-n">{{ d.day }}</span>
          <span class="adm-door-sub">{{ doorSub(d) }}</span>
        </button>
      </div>
      <p class="adm-legend">
        <span class="adm-dot is-ready"></span> {{ $t('ferdig utfylt') }}
        <span class="adm-dot is-review"></span> {{ $t('over - svarene må gjennomgås') }}
      </p>

      <!-- the selected door -->
      <section v-if="day" class="dq-card adm-editor">
        <h2 class="adm-h">{{ $t('Luke {0}', [day.day]) }} <small>{{ dateLabel(day.date) }} · {{ dayState }}</small></h2>

        <div class="field">
          <label>{{ $t('Kategori') }}</label>
          <input type="text" v-model="form.category" maxlength="200" :placeholder="$t('F.eks. Land med bokstaven T i navnet')" />
        </div>

        <div class="field">
          <label>{{ $t('Type luke') }}</label>
          <select v-model="form.type" :disabled="day.locked" @change="typeChanged">
            <option v-for="(info, key) in TYPES" :key="key" :value="key">{{ info.emoji }} {{ info.name }}</option>
          </select>
          <p class="picker-hint" style="margin:6px 0 0;">{{ typeInfo(form.type).rules }}</p>
        </div>

        <div class="field">
          <label>{{ form.type === 'ONE_OF_FOUR' ? $t('Ditt ene svar (gullsvaret)') : $t('Dine 4 svar (skjult for spillerne)') }}</label>
          <div v-for="(_, i) in form.answers" :key="i" class="adm-answer">
            <input type="text" v-model="form.answers[i]" maxlength="100" :disabled="day.locked" :placeholder="$t('Svar {0}', [i + 1])" />
            <label v-if="form.type === 'WILDCARD'" class="adm-wild" :class="{ 'is-on': form.wildcardIndex === i }">
              <input type="radio" :value="i" v-model="form.wildcardIndex" :disabled="day.locked" /> 🌟 {{ $t('Wildcard') }}
            </label>
          </div>
          <p v-if="day.locked" class="picker-hint" style="margin:6px 0 0;">{{ $t('{0} spillere har allerede svart - type og svar kan ikke endres lenger. Bruk gjennomgangen under for å rette hvordan svar leses.', [day.entries]) }}</p>
        </div>

        <button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? $t('Lagrer…') : $t('Lagre luka') }}</button>
        <span v-if="saved" class="adm-saved">✓ {{ $t('Lagret') }}</span>
        <p v-if="!day.locked && form.type === 'ONE_OF_FOUR'" class="picker-hint">{{ $t('Spillerne skriver fortsatt 4 svar - bare ett av dem trenger å treffe.') }}</p>
      </section>

      <!-- going through what the players wrote -->
      <section v-if="day && day.entries > 0" class="dq-card adm-review">
        <h2 class="adm-h">{{ $t('Gjennomgang') }} <small>{{ $t('{0} spillere har svart', [day.entries]) }}</small></h2>
        <div v-if="reviewLoading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>
        <template v-else-if="review">
          <p class="picker-hint" style="margin-top:0;">
            {{ $t('Svar som er skrevet likt som ditt, teller automatisk (stor/liten bokstav, aksenter og tegnsetting spiller ingen rolle). Rett opp resten her - valget gjelder alle som skrev det samme.') }}
          </p>
          <p class="adm-gm">🎩 {{ $t('Spillmesteren har {0} poeng på denne luka akkurat nå.', [review.gamemasterPoints]) }}</p>

          <div class="adm-table">
            <div v-for="a in sortedAnswers" :key="a.normalized" class="adm-row" :class="{ 'is-miss': a.matchedIndex < 0 }">
              <div class="adm-row-text">
                <strong>{{ a.text }}</strong>
                <span class="adm-count">{{ a.count }}×</span>
              </div>
              <select class="adv-input" :value="selectValue(a)" @change="decide(a, $event.target.value)">
                <option value="auto">{{ $t('Automatisk') }} ({{ a.corrected ? '…' : readLabel(a) }})</option>
                <option value="-1">{{ $t('Teller IKKE som treff') }}</option>
                <option v-for="(h, i) in review.hidden" :key="i" :value="String(i)">= {{ h }}</option>
              </select>
            </div>
          </div>

          <div class="adm-approve">
            <button class="btn btn-secondary btn-sm" @click="approve(!review.approved)">
              {{ review.approved ? $t('Merk som ikke gjennomgått') : $t('Marker som gjennomgått') }}
            </button>
            <span class="picker-hint">{{ review.approved ? $t('Spillerne ser resultatet som endelig.') : $t('Spillerne ser resultatet som «foreløpig» til du markerer det som gjennomgått.') }}</span>
          </div>
        </template>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { t as _t, locale } from '../i18n'
import api from '../services/api'
import { TYPES, typeInfo } from '../services/advent'
import ConfirmModal from './ConfirmModal.vue'

const season = ref(null)
const selected = ref(null)
const day = ref(null)
const review = ref(null)
const loading = ref(true)
const reviewLoading = ref(false)
const saving = ref(false)
const saved = ref(false)
const error = ref('')
const test = ref(null)
const testDate = ref('')
const testBusy = ref(false)
const confirmReset = ref(false)
const form = reactive({ category: '', type: 'NORMAL', answers: ['', '', '', ''], wildcardIndex: null })

const sortedAnswers = computed(() => {
  if (!review.value) return []
  // what is not counted as a hit comes first - that's the part that may need a decision
  return [...review.value.answers].sort((a, b) => (a.matchedIndex < 0) - (b.matchedIndex < 0) || b.count - a.count)
})

const dayState = computed(() => {
  if (!day.value) return ''
  if (day.value.closed) return _t('over')
  return day.value.date === season.value.today ? _t('åpen i dag') : _t('åpner senere')
})

// the pretend date can be anywhere from 1 November to 31 January of the season
const testMin = computed(() => (season.value ? `${season.value.year}-11-01` : ''))
const testMax = computed(() => (season.value ? `${season.value.year + 1}-01-31` : ''))
const untilLabel = computed(() => (test.value?.until
  ? new Date(test.value.until).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
  : ''))

async function loadTest() {
  test.value = await api.adminGetAdventTest()
  if (!testDate.value) testDate.value = test.value.date || `${season.value.year}-12-05`
}

async function setTest(date) {
  testBusy.value = true
  error.value = ''
  try {
    test.value = await api.adminSetAdventTestDate(date)
    season.value = await api.adminGetAdventSeason()
    await select(selected.value)
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke endre testmodus.')
  } finally {
    testBusy.value = false
  }
}

async function doReset() {
  confirmReset.value = false
  testBusy.value = true
  error.value = ''
  try {
    test.value = await api.adminResetAdventTest()
    season.value = await api.adminGetAdventSeason()
    await select(selected.value)
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke nullstille testdata.')
  } finally {
    testBusy.value = false
  }
}

onMounted(async () => {
  try {
    season.value = await api.adminGetAdventSeason()
    await loadTest()
    const today = season.value.days.find(d => d.date === season.value.today)
    await select(today ? today.day : 1)
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke laste julekalenderen.')
  } finally {
    loading.value = false
  }
})

function dateLabel(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString(locale, { day: 'numeric', month: 'long' })
}

function doorSub(d) {
  if (d.entries > 0) return `${d.entries}`
  return d.ready ? '✓' : '·'
}

function fillForm(d) {
  form.category = d.category || ''
  form.type = d.type
  form.wildcardIndex = d.wildcardIndex
  form.answers = resize(d.answers, TYPES[d.type] && d.type === 'ONE_OF_FOUR' ? 1 : 4)
}

function resize(list, n) {
  const out = list.slice(0, n)
  while (out.length < n) out.push('')
  return out
}

function typeChanged() {
  form.answers = resize(form.answers, form.type === 'ONE_OF_FOUR' ? 1 : 4)
  if (form.type !== 'WILDCARD') form.wildcardIndex = null
}

async function select(n) {
  selected.value = n
  saved.value = false
  error.value = ''
  review.value = null
  try {
    day.value = await api.adminGetAdventDay(n)
    fillForm(day.value)
    if (day.value.entries > 0) loadReview()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke laste luka.')
  }
}

async function loadReview() {
  reviewLoading.value = true
  try {
    review.value = await api.adminGetAdventReview(selected.value)
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke laste gjennomgangen.')
  } finally {
    reviewLoading.value = false
  }
}

async function save() {
  saving.value = true
  saved.value = false
  error.value = ''
  try {
    day.value = await api.adminSaveAdventDay(selected.value, {
      category: form.category,
      type: form.type,
      answers: form.answers,
      wildcardIndex: form.type === 'WILDCARD' ? form.wildcardIndex : null
    })
    fillForm(day.value)
    season.value = await api.adminGetAdventSeason()
    saved.value = true
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke lagre luka.')
  } finally {
    saving.value = false
  }
}

function selectValue(a) {
  if (!a.corrected) return 'auto'
  return String(a.matchedIndex)
}

function readLabel(a) {
  return a.matchedIndex >= 0 ? `= ${review.value.hidden[a.matchedIndex]}` : _t('ikke treff')
}

async function decide(a, value) {
  error.value = ''
  try {
    review.value = await api.adminDecideAdventAnswer(selected.value, a.text, value === 'auto' ? null : Number(value))
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke lagre valget.')
  }
}

async function approve(approved) {
  error.value = ''
  try {
    review.value = await api.adminApproveAdventDay(selected.value, approved)
    season.value = await api.adminGetAdventSeason()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke lagre.')
  }
}
</script>

<style scoped>
.adm-test { margin-bottom: 16px; }
.adm-test-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.adm-test-status { margin: 10px 0 0; font-size: 0.9rem; color: var(--teal-ink, var(--teal)); }
.adm-test-status--off { color: var(--text-dim); }
.adm-grid { display: grid; grid-template-columns: repeat(8, 1fr); gap: 6px; }
.adm-door { display: flex; flex-direction: column; align-items: center; padding: 8px 2px; background: var(--ink-raised); border: 2px solid var(--rule); border-radius: 3px; color: var(--text); font-family: var(--font-body); cursor: pointer; }
.adm-door-n { font-family: var(--font-display); font-weight: 700; font-size: 1.15rem; }
.adm-door-sub { font-size: 0.72rem; font-family: var(--font-mono); color: var(--text-dim); }
.adm-door.is-ready { background: rgb(var(--teal-rgb) / 0.1); }
.adm-door.is-review { border-color: var(--gold); background: rgb(var(--gold-rgb) / 0.12); }
.adm-door.is-selected { outline: 3px solid var(--text); outline-offset: 1px; }
.adm-legend { font-size: 0.8rem; color: var(--text-dim); margin: 8px 0 18px; display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.adm-dot { display: inline-block; width: 12px; height: 12px; border-radius: 2px; border: 2px solid var(--rule); margin-left: 8px; }
.adm-dot.is-ready { background: rgb(var(--teal-rgb) / 0.3); }
.adm-dot.is-review { border-color: var(--gold); background: rgb(var(--gold-rgb) / 0.3); }
.adm-editor, .adm-review { margin-top: 14px; }
.adm-editor input:disabled, .adm-editor select:disabled { opacity: 0.6; cursor: not-allowed; }
.adm-wild input { width: 18px; height: 18px; }
.adm-h { margin: 0 0 14px; font-size: 1.3rem; }
.adm-h small { font-size: 0.8rem; color: var(--text-dim); font-family: var(--font-body); font-weight: 400; }
.adm-answer { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.adm-answer > input[type="text"] { flex: 1; }
.adm-wild { display: flex; gap: 6px; align-items: center; margin: 0; white-space: nowrap; text-transform: none; letter-spacing: normal; font-weight: 400; color: var(--text-dim); }
.adm-wild.is-on { color: var(--text); font-weight: 600; }
.adm-saved { margin-left: 12px; color: var(--teal-ink, var(--teal)); font-weight: 600; }
.adm-gm { margin: 0 0 12px; }
.adm-table { display: grid; gap: 0; }
.adm-row { display: flex; gap: 10px; align-items: center; justify-content: space-between; padding: 8px 0; border-top: 1px solid var(--line); flex-wrap: wrap; }
.adm-row:first-child { border-top: none; }
.adm-row.is-miss .adm-row-text strong { color: var(--coral-ink, var(--coral)); }
.adm-row-text { flex: 1; min-width: 160px; overflow-wrap: anywhere; }
.adm-count { margin-left: 8px; font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-dim); }
.adm-row select { max-width: 300px; flex: 1 1 220px; }
.adm-approve { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-top: 14px; }
@media (max-width: 560px) { .adm-grid { grid-template-columns: repeat(6, 1fr); } }
</style>
