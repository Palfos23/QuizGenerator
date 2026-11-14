<template>
  <div class="home">
    <section class="home-hero">
      <!-- the number counts up, the words rise out of a mask one after another -->
      <h1 :aria-label="title">
        <span class="home-num" aria-hidden="true">{{ shownNumber }}</span>
        <template v-for="(w, i) in restWords" :key="i"> <span class="home-w" aria-hidden="true"><span :style="{ animationDelay: (260 + i * 80) + 'ms' }">{{ w }}</span></span></template>
      </h1>
      <div class="home-cta">
        <button type="button" class="btn btn-primary btn-lg" @click="loginDialog.open()">{{ $t('Spill dagens quiz§HomeViewT') }}</button>
      </div>
    </section>

    <div class="home-stage" @pointermove="tilt" @pointerleave="untilt">
      <DailyQuizPreview />
    </div>

    <section class="home-more">
      <h2 v-reveal>{{ $t('Lag egne quizer og spill sammen') }}</h2>
      <ul class="home-more-list">
        <li v-reveal="0">
          <span class="home-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M4 6h16M4 12h10M4 18h7" /><path d="M17 15l2 2 3-4" /></svg>
          </span>
          <div>
            <h3>{{ $t('Lag en quiz') }}</h3>
            <p>{{ $t('Velg kategorier og vanskelighetsgrad, bytt ut spørsmål og skriv ut quizen.') }}</p>
          </div>
        </li>
        <li v-reveal="90">
          <span class="home-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /></svg>
          </span>
          <div>
            <h3>{{ $t('Ukens grid§HomeViewT') }}</h3>
            <p>{{ $t('Gjett alle svarene som passer ukens tema før du går tom for forsøk.') }}</p>
          </div>
        </li>
        <li v-reveal="180">
          <span class="home-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="8" r="3.2" /><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6" /><circle cx="17.5" cy="9.5" r="2.5" /><path d="M16.5 14.2c2.7.2 4.5 2.3 4.5 5" /></svg>
          </span>
          <div>
            <h3>{{ $t('Selskapsspill') }}</h3>
            <p>{{ $t('Tension, Imposter, Bullseye og mer - på én skjerm, eller hver på sin egen telefon med en romkode.') }}</p>
          </div>
        </li>
        <li v-reveal="270">
          <span class="home-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14" /></svg>
          </span>
          <div>
            <h3>{{ $t('Foreslå et spørsmål') }}</h3>
            <p>{{ $t('Legg til i den felles banken - en administrator gjennomgår det, og du kan bruke det i dine egne quizer.') }}</p>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { t as _t } from '../i18n'
import { useRoute, useRouter } from 'vue-router'
import DailyQuizPreview from '../components/previews/DailyQuizPreview.vue'
import auth from '../services/auth'
import loginDialog from '../services/loginDialog'
import reveal from '../directives/reveal'
import { safeRedirectTarget } from '../router'

const route = useRoute()
const router = useRouter()

const prefersReducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches

// ---- headline: "15" counts up from 0 while the rest of the words rise into place ----
const title = _t('15 nye spørsmål hver dag')
const words = title.split(' ')
const targetNumber = parseInt(words[0], 10) || 0
const restWords = words.slice(1)
const shownNumber = ref(prefersReducedMotion() ? targetNumber : 0)
let rafId = 0
function countUp() {
  const start = performance.now() + 120
  const duration = 900
  const tick = (now) => {
    const k = Math.min(1, Math.max(0, (now - start) / duration))
    shownNumber.value = Math.round(targetNumber * (1 - Math.pow(1 - k, 3))) // ease out
    if (k < 1) rafId = requestAnimationFrame(tick)
  }
  rafId = requestAnimationFrame(tick)
}

// ---- the preview card tilts a little toward the pointer (mouse only) ----
function tilt(e) {
  if (e.pointerType !== 'mouse' || prefersReducedMotion()) return
  const box = e.currentTarget.getBoundingClientRect()
  const x = (e.clientX - box.left) / box.width - 0.5
  const y = (e.clientY - box.top) / box.height - 0.5
  e.currentTarget.style.setProperty('--tilt-y', `${(x * 7).toFixed(2)}deg`)
  e.currentTarget.style.setProperty('--tilt-x', `${(-y * 7).toFixed(2)}deg`)
}
function untilt(e) {
  e.currentTarget.style.setProperty('--tilt-x', '0deg')
  e.currentTarget.style.setProperty('--tilt-y', '0deg')
}

const vReveal = reveal // fades up on scroll; icons inside draw their lines (see directives/reveal.js)

onMounted(() => {
  if (!prefersReducedMotion()) countUp()
  if (auth.isAuthenticated.value) {
    router.push(auth.isAdmin.value ? '/admin/questions' : safeRedirectTarget(route, '/dashboard'))
  } else if (route.query.redirect) {
    // sent here from a page that needs an account: go straight to the sign-in popup
    loginDialog.open()
  }
})

onBeforeUnmount(() => cancelAnimationFrame(rafId))
</script>

<style scoped>
/* A two-column page: the pitch and its one call to action on the left, the real product on the right.
   Signing in is a popup (components/LoginDialog.vue), opened from here or from the header. */
.home {
  max-width: 1040px;
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 460px);
  grid-template-areas:
    "hero preview"
    "more more";
  column-gap: var(--space-7);
  row-gap: var(--space-8);
  align-items: start;
  padding: var(--space-6) 0 var(--space-7);
}
.home-hero { grid-area: hero; min-width: 0; align-self: center; }
.home-more { grid-area: more; }

.home-num { color: var(--gold-ink); }
.home-hero h1::before { content: ''; display: block; width: 56px; height: 5px; margin-bottom: var(--space-4); border-radius: 3px; background: var(--gold); }
.home-hero h1 { font-size: clamp(2.3rem, 5.4vw, 3.4rem); line-height: 1.04; letter-spacing: -0.025em; margin: 0 0 var(--space-5); max-width: 14ch; }
.home-cta { margin: 0; }
.home-cta .btn-lg { padding-inline: var(--space-6); min-height: 52px; font-size: 1.05rem; }
.home-stage { grid-area: preview; width: 100%; }

/* headline words rise out of a mask */
.home-num { display: inline-block; min-width: 1.05em; margin-right: 0.26em; font-variant-numeric: tabular-nums; }
.home-w { display: inline-block; overflow: hidden; vertical-align: bottom; padding-bottom: 0.1em; margin-bottom: -0.1em; margin-right: 0.26em; }
.home-w > span { display: inline-block; animation: word-up 620ms var(--ease-out) backwards; }
@keyframes word-up { from { translate: 0 110%; } }

/* the main button gets a light sweep every few seconds */
.home-cta .btn { position: relative; overflow: hidden; }
.home-cta .btn::after {
  content: ''; position: absolute; top: 0; bottom: 0; left: -60%; width: 40%;
  background: linear-gradient(100deg, transparent, rgb(255 255 255 / 0.28), transparent);
  transform: skewX(-18deg); pointer-events: none;
  animation: shine 5.5s var(--ease-in-out) 2.4s infinite;
}
@keyframes shine { 0%, 40% { left: -60%; } 70%, 100% { left: 130%; } }

@media (prefers-reduced-motion: reduce) { .home-w > span, .home-cta .btn::after { animation: none; } }

.home-more h2 { font-size: 1.5rem; margin: 0 0 var(--space-5); max-width: 24ch; }
.home-more-list { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-6) var(--space-7); }
.home-more-list li { display: flex; gap: var(--space-4); align-items: flex-start; }
.home-icon { flex-shrink: 0; width: 40px; height: 40px; display: grid; place-items: center; border-radius: var(--radius-md); background: rgb(var(--primary-rgb) / 0.09); color: var(--primary); }
.home-icon svg { width: 22px; height: 22px; }
.home-more-list h3 { margin: 0 0 var(--space-1); font-family: var(--font-body); font-size: 1.05rem; font-weight: 650; letter-spacing: 0; }
.home-more-list p { margin: 0; color: var(--text-dim); font-size: 0.95rem; line-height: 1.5; max-width: 38ch; }

@media (max-width: 900px) {
  .home { grid-template-columns: minmax(0, 1fr); grid-template-areas: "hero" "preview" "more"; row-gap: var(--space-7); padding-top: var(--space-4); }
}
@media (max-width: 560px) {
  .home-more-list { grid-template-columns: 1fr; gap: var(--space-5); }
  .home-cta .btn-lg { width: 100%; }
}
</style>
