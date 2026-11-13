<template>
  <div class="tri">
    <div class="tri-top">
      <div class="tri-progress" role="progressbar" :aria-valuenow="done" :aria-valuemin="0" :aria-valuemax="total">
        <span :style="{ width: pct + '%' }"></span>
      </div>
      <div class="tri-count">
        <strong>{{ done }}</strong> {{ $t('av') }} {{ total }} {{ $t('rettet') }}
        <span class="tri-left" v-if="remaining">· {{ $t('{0} igjen', [remaining]) }}</span>
      </div>
      <button type="button" class="tri-exit" @click="emit('exit')">{{ $t('Til listen') }}</button>
    </div>

    <div class="tri-stage">
      <article v-if="current" :key="current.id" class="tri-card" :class="'is-' + lastAction">
        <div class="tri-context">
          {{ $t('Spørsmål {0}', [current.q.number]) }} · {{ dayLabel }}
        </div>
        <h2 class="tri-question">{{ current.q.questionText }}</h2>

        <div class="tri-versus">
          <div class="tri-side tri-side--right">
            <span class="tri-label">{{ $t('Riktig svar:') }}</span>
            <span class="tri-value">{{ current.q.correctAnswer }}</span>
          </div>
          <div class="tri-side tri-side--given">
            <span class="tri-label">{{ $t('Spilleren skrev') }}</span>
            <span class="tri-value tri-value--given">{{ current.g.text }}</span>
          </div>
        </div>

        <div class="tri-meta">
          <span class="tri-chip">{{ $t('{0} spillere skrev dette¦{0} spillere skrev dette', [current.g.count]) }}</span>
          <span v-if="current.g.likelyTypo" class="tri-chip tri-chip--hint">{{ $t('ser ut som skrivefeil') }}</span>
        </div>

        <div v-if="siblings.length" class="tri-siblings">
          <span class="tri-label">{{ $t('Andre svar på spørsmålet') }}</span>
          <ul>
            <li v-for="s in siblings" :key="s.id" :class="'is-' + s.state">
              {{ s.g.text }} <small>{{ s.g.count }}×</small>
            </li>
          </ul>
        </div>
      </article>

      <div v-else class="tri-done">
        <div class="tri-done-mark" aria-hidden="true">✓</div>
        <h2>{{ $t('Alt er rettet!') }}</h2>
        <p>{{ $t('Ingen svar venter på gjennomgang.') }}</p>
        <button type="button" class="btn btn-primary" @click="emit('exit')">{{ $t('Til listen') }}</button>
      </div>
    </div>

    <div class="tri-actions">
      <button type="button" class="tri-btn tri-btn--no" :disabled="!current" @click="act('no')">
        <span class="tri-btn-main">✕ {{ $t('Feil§AdminAnswers') }}</span>
        <kbd>F</kbd>
      </button>
      <button type="button" class="tri-btn tri-btn--skip" :disabled="!current || remaining < 2" @click="act('skip')">
        <span class="tri-btn-main">{{ $t('Hopp over') }}</span>
        <kbd>S</kbd>
      </button>
      <button type="button" class="tri-btn tri-btn--yes" :disabled="!current" @click="act('yes')">
        <span class="tri-btn-main">✓ {{ $t('Riktig§AdminAnswers') }}</span>
        <kbd>R</kbd>
      </button>
    </div>

    <div class="tri-foot">
      <label class="tri-remember">
        <input type="checkbox" :checked="remember" @change="emit('update:remember', $event.target.checked)" />
        <span>{{ $t('Husk valget') }} <kbd>H</kbd></span>
      </label>
      <button type="button" class="tri-undo" :disabled="!canUndo" @click="act('undo')">↶ {{ $t('Angre') }} <kbd>Z</kbd></button>
    </div>
    <p class="tri-keys">{{ $t('Tastatur: R eller → = riktig · F eller ← = feil · S = hopp over · Z = angre') }}</p>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { locale } from '../i18n'

// One answer at a time, full focus, keyboard first: the fastest way through a pile of answers. The page decides what a
// decision does (it is shown at once and saved in the background) - this only shows the next answer and reports the click.
const props = defineProps({
  rows: { type: Array, required: true }, // the answers still waiting, in the order to take them
  allRows: { type: Array, required: true }, // every answer (for "other answers to this question")
  remember: { type: Boolean, default: true },
  canUndo: { type: Boolean, default: false },
  total: { type: Number, required: true }, // how many answers this round started with
  focusId: { type: String, default: null } // an answer brought back by "angre" goes first
})
const emit = defineEmits(['decide', 'undo', 'exit', 'update:remember'])

const skipped = ref([]) // pushed to the back of the line
const lastAction = ref('yes')

const ordered = computed(() => {
  const rest = props.rows.filter(r => !skipped.value.includes(r.id) && r.id !== props.focusId)
  const later = props.rows.filter(r => skipped.value.includes(r.id) && r.id !== props.focusId)
  const first = props.rows.filter(r => r.id === props.focusId)
  return [...first, ...rest, ...later]
})
const current = computed(() => ordered.value[0] || null)
const remaining = computed(() => props.rows.length)
const done = computed(() => Math.max(0, props.total - remaining.value))
const pct = computed(() => (props.total ? Math.min(100, (done.value / props.total) * 100) : 100))

// everybody who is skipped and nothing else left: start the skipped ones over
watch(remaining, () => {
  if (props.rows.length && props.rows.every(r => skipped.value.includes(r.id))) skipped.value = []
})

const siblings = computed(() => {
  const c = current.value
  if (!c) return []
  return props.allRows.filter(r => r.q.questionId === c.q.questionId && r.id !== c.id).slice(0, 8)
})

const dayLabel = computed(() => {
  if (!current.value) return ''
  const [y, m, d] = current.value.day.quizDate.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString(locale, { weekday: 'long', day: 'numeric', month: 'long' })
})

function act(action) {
  const c = current.value
  if (action === 'undo') {
    if (!props.canUndo) return
    lastAction.value = 'undo'
    emit('undo')
    return
  }
  if (!c) return
  if (action === 'skip') {
    if (remaining.value < 2) return
    lastAction.value = 'skip'
    skipped.value = [...skipped.value, c.id]
    return
  }
  lastAction.value = action
  emit('decide', c, action === 'yes')
}

function onKey(e) {
  if (e.metaKey || e.ctrlKey || e.altKey) return
  const tag = (e.target && e.target.tagName) || ''
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(tag) || (e.target && e.target.isContentEditable)) return
  const k = e.key.toLowerCase()
  if (k === 'r' || k === 'arrowright') { e.preventDefault(); act('yes') }
  else if (k === 'f' || k === 'arrowleft') { e.preventDefault(); act('no') }
  else if (k === 's' || k === 'arrowdown' || k === ' ') { e.preventDefault(); act('skip') }
  else if (k === 'z' || k === 'backspace') { e.preventDefault(); act('undo') }
  else if (k === 'h') { emit('update:remember', !props.remember) }
  else if (k === 'escape') { emit('exit') }
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<style scoped>
.tri { max-width: 720px; margin: 0 auto; }

.tri-top { display: grid; grid-template-columns: 1fr auto; gap: 8px 16px; align-items: center; margin-bottom: 14px; }
.tri-progress { grid-column: 1 / -1; height: 8px; background: var(--line); border-radius: 4px; overflow: hidden; order: 3; }
.tri-progress span { display: block; height: 100%; background: var(--teal); border-radius: 4px; transition: width 0.3s ease; }
.tri-count { font-size: 0.95rem; color: var(--text-dim); }
.tri-count strong { color: var(--text); font-family: var(--font-display); font-size: 1.2rem; }
.tri-left { margin-left: 4px; }
.tri-exit { background: none; border: none; color: var(--text-dim); text-decoration: underline; text-underline-offset: 3px; cursor: pointer; font: inherit; font-size: 0.9rem; }
.tri-exit:hover { color: var(--text); }

.tri-card {
  background: var(--ink-raised);
  border: 2px solid var(--rule);
  border-radius: 3px;
  padding: 24px 26px 22px;
  box-shadow: 6px 6px 0 var(--gold);
}
.tri-context { font-family: var(--font-mono); font-size: 0.74rem; letter-spacing: 0.08em; text-transform: uppercase; color: var(--text-dim); }
.tri-question { margin: 8px 0 18px; font-size: 1.35rem; line-height: 1.25; overflow-wrap: anywhere; }

.tri-versus { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.tri-side { border: 1.5px solid var(--line); border-radius: 3px; padding: 12px 14px; min-width: 0; }
.tri-side--right { background: rgb(var(--teal-rgb) / 0.08); }
.tri-side--given { background: rgb(var(--gold-rgb) / 0.08); border-color: var(--gold); border-width: 2px; }
.tri-label { display: block; font-size: 0.7rem; letter-spacing: 0.07em; text-transform: uppercase; color: var(--text-dim); font-weight: 700; margin-bottom: 4px; }
.tri-value { display: block; font-family: var(--font-display); font-size: 1.35rem; font-weight: 600; overflow-wrap: anywhere; }
.tri-value--given { font-size: 1.7rem; }

.tri-meta { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 14px; }
.tri-chip { font-size: 0.82rem; padding: 3px 10px; border: 1.5px solid var(--line); border-radius: 12px; color: var(--text-dim); }
.tri-chip--hint { border-color: var(--gold); color: var(--text); background: rgb(var(--gold-rgb) / 0.1); font-weight: 600; }

.tri-siblings { margin-top: 16px; padding-top: 12px; border-top: 1px solid var(--line); }
.tri-siblings ul { list-style: none; display: flex; flex-wrap: wrap; gap: 6px; margin: 6px 0 0; padding: 0; }
.tri-siblings li { font-size: 0.85rem; padding: 2px 9px; border-radius: 3px; border: 1.5px solid var(--line); color: var(--text-dim); }
.tri-siblings li small { font-family: var(--font-mono); }
.tri-siblings li.is-correct { border-color: var(--teal); color: var(--teal-ink, var(--teal)); }
.tri-siblings li.is-wrong { border-color: var(--coral); color: var(--coral-ink, var(--coral)); text-decoration: line-through; text-decoration-thickness: 1px; }
.tri-siblings li.is-pending { border-style: dashed; }

.tri-done { text-align: center; padding: 38px 16px; border: 2px solid var(--rule); border-radius: 3px; background: var(--ink-raised); }
.tri-done-mark { width: 64px; height: 64px; margin: 0 auto 10px; display: grid; place-items: center; border-radius: 50%; background: var(--teal); color: #fff; font-size: 2rem; }
.tri-done h2 { margin: 0 0 4px; }
.tri-done p { margin: 0 0 16px; color: var(--text-dim); }

.tri-actions { display: grid; grid-template-columns: 1fr auto 1fr; gap: 12px; margin-top: 18px; }
.tri-btn {
  display: flex; align-items: center; justify-content: center; gap: 10px;
  padding: 16px 18px; border: 2px solid var(--rule); border-radius: 3px; background: var(--ink-raised); color: var(--text);
  font-family: var(--font-body); font-size: 1.1rem; font-weight: 700; cursor: pointer;
  transition: transform 0.08s ease, background 0.12s ease, color 0.12s ease, border-color 0.12s ease;
}
.tri-btn:active:not(:disabled) { transform: scale(0.97); }
.tri-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.tri-btn--yes:hover:not(:disabled), .tri-btn--yes:focus-visible { background: var(--teal); border-color: var(--teal); color: #fff; }
.tri-btn--no:hover:not(:disabled), .tri-btn--no:focus-visible { background: var(--coral); border-color: var(--coral); color: #fff; }
.tri-btn--skip { font-weight: 600; font-size: 0.95rem; color: var(--text-dim); border-style: dashed; padding-inline: 22px; }
kbd { font-family: var(--font-mono); font-size: 0.72rem; padding: 1px 6px; border: 1.5px solid currentColor; border-radius: 3px; opacity: 0.65; font-weight: 600; }

.tri-foot { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; margin-top: 14px; }
.tri-remember { display: flex; gap: 8px; align-items: center; margin: 0; text-transform: none; letter-spacing: normal; font-weight: 400; color: var(--text); font-size: 0.92rem; cursor: pointer; }
.tri-remember input { width: 18px; height: 18px; }
.tri-undo { background: none; border: none; font: inherit; font-size: 0.92rem; color: var(--text); cursor: pointer; display: inline-flex; gap: 6px; align-items: center; }
.tri-undo:disabled { opacity: 0.35; cursor: not-allowed; }
.tri-keys { margin: 12px 0 0; text-align: center; font-size: 0.78rem; color: var(--text-dim); }

/* each new card slides up from the side of the choice that brought it (plain CSS, so it can never get stuck mid-way) */
@keyframes tri-in-yes { from { transform: translate(-28px, 10px) rotate(-1deg); opacity: 0; } to { transform: none; opacity: 1; } }
@keyframes tri-in-no { from { transform: translate(28px, 10px) rotate(1deg); opacity: 0; } to { transform: none; opacity: 1; } }
@keyframes tri-in-up { from { transform: translateY(18px); opacity: 0; } to { transform: none; opacity: 1; } }
.tri-card.is-yes { animation: tri-in-yes 0.2s ease-out; }
.tri-card.is-no { animation: tri-in-no 0.2s ease-out; }
.tri-card.is-skip, .tri-card.is-undo { animation: tri-in-up 0.2s ease-out; }
@media (prefers-reduced-motion: reduce) { .tri-card { animation: none !important; } .tri-progress span { transition: none; } }

@media (max-width: 600px) {
  .tri-card { padding: 18px 16px; box-shadow: 4px 4px 0 var(--gold); }
  .tri-versus { grid-template-columns: 1fr; }
  .tri-question { font-size: 1.15rem; }
  .tri-actions { grid-template-columns: 1fr 1fr; }
  .tri-btn--skip { grid-column: 1 / -1; order: 3; padding: 11px; }
  .tri-keys { display: none; }
}
</style>
