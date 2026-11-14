<template>
  <figure class="dqp" aria-hidden="true">
    <header class="dqp-top">
      <div>
        <strong>{{ $t('Dagens quiz§HomeViewT') }}</strong>
        <span class="dqp-date">{{ $t('15 spørsmål · skriv svarene dine') }}</span>
      </div>
      <div class="dqp-progress" role="presentation">
        <span class="dqp-progress-label">{{ $t('{0} av 15', [current]) }}</span>
        <span class="dqp-bar"><i :style="{ width: (current / 15 * 100) + '%' }"></i></span>
      </div>
    </header>

    <div class="dqp-list" :class="{ 'is-leaving': leaving }">
      <article v-for="(r, i) in rows" :key="i" class="dqp-q" :class="'is-' + r.status">
        <span class="dqp-n">{{ i + 1 }}</span>
        <div class="dqp-main">
          <p>{{ $t(r.q) }}</p>
          <div v-if="r.status === 'ok' || r.status === 'bad'" class="dqp-a">{{ r.text }}</div>
          <div v-else class="dqp-input" :class="{ 'is-idle': r.status === 'idle' }">
            <span>{{ r.text }}</span><i v-if="r.status === 'typing'" class="dqp-caret"></i>
          </div>
          <!-- the correction takes its space from the start, so the card never changes height -->
          <div v-if="r.fix" class="dqp-fix" :class="{ 'is-shown': r.status === 'bad' }">{{ $t('Riktig svar§DailyQuizPlayViewT') }}: <strong>{{ r.fix }}</strong></div>
        </div>
        <span v-if="r.status === 'ok'" class="dq-pill dq-pill--ok dqp-pill">{{ $t('Riktig§DailyQuizPlayViewM') }}</span>
        <span v-else-if="r.status === 'bad'" class="dq-pill dq-pill--bad dqp-pill">{{ $t('Feil§DailyQuizPlayViewM') }}</span>
      </article>
    </div>

    <!-- a point flies up out of the card each time an answer is right -->
    <span v-if="plusKey" :key="plusKey" class="dqp-plus">{{ $t('+1 poeng§DailyQuizPreviewT') }}</span>
  </figure>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { t as _t } from '../../i18n'

// A small film of the real daily quiz, looping: an answer is typed, graded, the next one is typed... It stands in for
// a screenshot, so it uses the same rows, pills and wording as the real page. With reduced motion it holds the last frame.
const rows = reactive([
  { q: 'Hva heter Norges lengste elv?', a: 'Glomma', fix: null, status: 'idle', text: '' },
  { q: 'Hvilket land vant fotball-VM i 2022?', a: _t('Frankrike§DailyQuizPreviewT'), fix: 'Argentina', status: 'idle', text: '' },
  { q: 'Hvilket grunnstoff har symbolet Fe?', a: _t('Jern§DailyQuizPreviewT'), fix: null, status: 'idle', text: '' }
])
const current = ref(1)
const leaving = ref(false)
const plusKey = ref(0)

let alive = true
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

async function typeInto(row) {
  row.status = 'typing'
  row.text = ''
  for (const ch of row.a) {
    if (!alive) return
    row.text += ch
    await sleep(70 + Math.random() * 55)
  }
  await sleep(380)
}

function showFinalFrame() {
  rows[0].status = 'ok'; rows[0].text = rows[0].a
  rows[1].status = 'bad'; rows[1].text = rows[1].a
  rows[2].status = 'typing'; rows[2].text = rows[2].a
  current.value = 3
}

async function play() {
  while (alive) {
    leaving.value = false
    rows.forEach(r => { r.status = 'idle'; r.text = '' })
    current.value = 1
    await sleep(1000)
    if (!alive) return

    await typeInto(rows[0])
    if (!alive) return
    rows[0].status = 'ok'; rows[0].text = rows[0].a
    plusKey.value++
    await sleep(1000)

    current.value = 2
    await typeInto(rows[1])
    if (!alive) return
    rows[1].status = 'bad'; rows[1].text = rows[1].a
    await sleep(1200)

    current.value = 3
    await typeInto(rows[2])
    await sleep(2800)
    if (!alive) return

    leaving.value = true
    await sleep(380)
  }
}

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) showFinalFrame()
  else play()
})
onBeforeUnmount(() => { alive = false })
</script>

<style scoped>
/* A faithful, non-interactive picture of the real daily quiz: the same question rows, answers and verdict pills. */
.dqp { position: relative; margin: 0; padding: var(--space-4); background: var(--ink-raised); border: 1px solid var(--rule); border-radius: var(--radius-xl); box-shadow: var(--shadow-float); user-select: none; pointer-events: none;
  transform: perspective(1100px) rotateX(var(--tilt-x, 0deg)) rotateY(var(--tilt-y, 0deg)); transition: transform 180ms var(--ease-out); }
.dqp-top { display: flex; justify-content: space-between; align-items: center; gap: var(--space-4); padding: var(--space-1) var(--space-2) var(--space-4); }
.dqp-top strong { display: block; font-family: var(--font-display); font-size: 1.2rem; }
.dqp-date { display: block; font-size: 0.85rem; color: var(--text-dim); margin-top: 2px; }
.dqp-progress { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; min-width: 96px; }
.dqp-progress-label { font-size: 0.82rem; font-weight: 600; color: var(--text-dim); }
.dqp-bar { display: block; width: 96px; height: 6px; border-radius: 999px; background: rgb(var(--fg-rgb) / 0.09); overflow: hidden; }
.dqp-bar i { display: block; height: 100%; background: var(--gold); border-radius: inherit; transition: width var(--dur-move) var(--ease-in-out); }

/* rows sit 8px inside the card's 16px padding, so the radius steps down: 20 - 16 = 4 around the list, 10 on each row */
.dqp-list { display: grid; gap: var(--space-2); transition: opacity 300ms var(--ease-in); }
.dqp-list.is-leaving { opacity: 0; }
.dqp-q { display: grid; grid-template-columns: auto 1fr auto; gap: var(--space-3); align-items: start; padding: var(--space-3) var(--space-4); border-radius: var(--radius-md); background: rgb(var(--fg-rgb) / 0.035); transition: background 300ms var(--ease-out); }
.dqp-q.is-ok { background: rgb(var(--teal-rgb) / 0.08); }
.dqp-q.is-bad { background: rgb(var(--coral-rgb) / 0.07); }
.dqp-q.is-typing .dqp-n { background: var(--gold); color: var(--on-accent); }
.dqp-n { width: 24px; height: 24px; display: grid; place-items: center; border-radius: 50%; background: rgb(var(--fg-rgb) / 0.08); color: var(--text-dim); font-size: 0.78rem; font-weight: 700; transition: background 250ms var(--ease-out), color 250ms var(--ease-out); }
.dqp-main p { margin: 0 0 6px; font-weight: 600; font-size: 0.98rem; line-height: 1.3; }
.dqp-a { font-size: 1.05rem; font-weight: 650; min-height: 41px; display: flex; align-items: center; padding-inline: 13px; }
.dqp-fix { margin-top: 4px; font-size: 0.85rem; color: var(--text-dim); opacity: 0; translate: 0 -4px; transition: opacity 260ms var(--ease-out), translate 260ms var(--ease-out); }
.dqp-fix.is-shown { opacity: 1; translate: 0 0; }
.dqp-fix strong { color: var(--teal-ink); }
.dqp-input { display: flex; align-items: center; gap: 2px; min-height: 41px; padding: 9px 12px; background: var(--field-bg); border: 1px solid rgb(var(--fg-rgb) / 0.2); border-radius: var(--radius-sm); font-size: 1.02rem; transition: border-color 200ms var(--ease-out), box-shadow 200ms var(--ease-out); }
.is-typing .dqp-input { border-color: var(--primary); box-shadow: 0 0 0 3px rgb(var(--primary-rgb) / 0.15); }
.dqp-caret { width: 2px; height: 1.1em; background: var(--gold); animation: dqp-blink 1.1s steps(1) infinite; }
@keyframes dqp-blink { 50% { opacity: 0; } }

/* the verdict pill pops in and, when right, pulses once */
.dqp-pill { animation: pop-in 0.28s var(--ease-out) both; }
.dqp-pill.dq-pill--ok { animation: pop-in 0.28s var(--ease-out) both, pulse-ok 600ms var(--ease-out) 140ms; }

/* +1 poeng: x eases out while y eases in-out, so it travels along an arc; then it fades */
.dqp-plus {
  position: absolute; top: 96px; right: 28px; padding: 4px 12px; border-radius: 999px;
  background: var(--teal); color: #fff; font-size: 0.85rem; font-weight: 700; pointer-events: none;
  animation: plus-x 1.3s var(--ease-out) both, plus-y 1.3s var(--ease-in-out) both, plus-fade 1.3s linear both;
}
@keyframes plus-x { from { translate: 0 0; } to { translate: 26px 0; } }
@keyframes plus-y { from { transform: translateY(0); } to { transform: translateY(-58px); } }
@keyframes plus-fade { 0% { opacity: 0; } 12% { opacity: 1; } 70% { opacity: 1; } 100% { opacity: 0; } }

@media (prefers-reduced-motion: reduce) { .dqp-caret, .dqp-plus { animation: none; } }
</style>
