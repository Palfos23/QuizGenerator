<template>
  <!-- The advent calendar, top of the dashboard from 1 November through January: a countdown before 1 December, today's
       door while it runs (or "taken" once you've played it), and the season's wrapped afterwards. -->
  <router-link v-if="view" :to="view.to" class="advent-banner">
    <ChristmasScene variant="snow" />
    <span class="advent-banner-lights" aria-hidden="true"><i v-for="n in 18" :key="n" :style="{ animationDelay: (n % 4) * 0.45 + 's' }"></i></span>
    <span class="advent-banner-door" aria-hidden="true">
      <svg v-if="view.icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9h18v4H3zM5 13v8h14v-8M12 9v12M12 9c-1.5-3-5-4-5-1.5S10 9 12 9zM12 9c1.5-3 5-4 5-1.5S14 9 12 9z" /></svg>
      <template v-else>{{ view.number }}</template>
    </span>
    <span class="advent-banner-text">
      <span class="advent-banner-eyebrow">
        {{ view.eyebrow }}
        <span v-if="view.prize" class="advent-banner-prize">{{ $t('Premie til vinneren') }}</span>
      </span>
      <span class="advent-banner-title">{{ view.title }}</span>
      <span class="advent-banner-sub">{{ view.sub }}</span>
    </span>
    <span class="advent-banner-trees"><ChristmasScene variant="trees" /></span>
    <span class="advent-banner-cta">{{ view.cta }}</span>
  </router-link>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import api from '../services/api'
import auth from '../services/auth'
import ChristmasScene from './ChristmasScene.vue'

const now = ref(new Date())
const advent = ref(null) // the player's own calendar state, once loaded - only used to tell whether today's door is done
let timer = null

onMounted(() => {
  timer = setInterval(() => { now.value = new Date() }, 30000)
  if (auth.isAdmin.value || auth.isGuest.value) return
  api.getAdvent().then(s => { advent.value = s }).catch(() => { /* the banner works on the date alone */ })
})
onBeforeUnmount(() => clearInterval(timer))

function untilMidnight(d) {
  const midnight = new Date(d)
  midnight.setHours(24, 0, 0, 0)
  const mins = Math.max(1, Math.round((midnight - d) / 60000))
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return h > 0 ? _t('{0} t {1} min', [h, m]) : _t('{0} min', [m])
}

// Worked out from the date straight away (no waiting, no jump), so the server's answer only adds "you've taken it".
const view = computed(() => {
  const d = now.value
  const month = d.getMonth() // 0 = January
  const day = d.getDate()

  if (month === 10) {
    const start = new Date(d.getFullYear(), 11, 1)
    const days = Math.max(1, Math.ceil((start - new Date(d.getFullYear(), month, day)) / 86400000))
    return {
      to: '/julekalender', icon: true, prize: true,
      eyebrow: _t('Julekalender'),
      title: _t('Åpner 1. desember'),
      sub: _t('Om {0} dag - 24 luker der du tenker som spillmesteren og samler poeng.¦Om {0} dager - 24 luker der du tenker som spillmesteren og samler poeng.', [days]),
      cta: _t('Se kalenderen')
    }
  }
  if (month === 11 && day <= 24) {
    const door = advent.value?.doors?.find(x => x.day === day)
    const eyebrow = _t('Julekalender · {0}. desember', [day])
    if (door?.entered) {
      return {
        to: '/julekalender', number: day, eyebrow, prize: true,
        title: _t('Du har tatt luke {0}', [day]),
        sub: day < 24 ? _t('Neste luke åpner om {0}.', [untilMidnight(d)]) : _t('Det var siste luke - resultatene kommer etter midnatt.'),
        cta: _t('Se kalenderen')
      }
    }
    return {
      to: `/julekalender/luke/${day}`, number: day, eyebrow, prize: true,
      title: _t('Dagens luke er åpen'),
      sub: _t('Tenk som spillmesteren. Luken stenger om {0}.', [untilMidnight(d)]),
      cta: _t('Åpne luke {0}', [day])
    }
  }
  if (month === 11 || month === 0) {
    return {
      to: advent.value?.wrappedAvailable === false ? '/julekalender' : '/julekalender/wrapped', icon: true,
      eyebrow: _t('Julekalender'),
      title: _t('Julekalenderen er over'),
      sub: _t('Se hvordan det gikk - poengene dine, plassen din og de beste dagene.'),
      cta: _t('Se oppsummeringen')
    }
  }
  return null
})
</script>

<style scoped>
/* Deep Christmas red with a string of lights along the top, snow falling over it, a little wrapped-present door and a
   cluster of pine trees (one decorated) beside the button. */
.advent-banner {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 0 0 20px;
  padding: 18px 14px 12px;
  border-radius: var(--radius-sm);
  background:
    radial-gradient(120% 140% at 0% 0%, #a3302f 0%, transparent 60%),
    linear-gradient(135deg, #8b2323 0%, #6a1a1d 55%, #4d1216 100%);
  color: #fdf1ea;
  text-decoration: none;
  box-shadow: inset 0 0 0 1px rgb(255 214 140 / 0.25);
  transition: filter 0.15s ease;
}
.advent-banner:hover { filter: brightness(1.08); }
.advent-banner > :not(.xmas-scene):not(.advent-banner-lights) { position: relative; z-index: 1; }

.advent-banner-lights {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 14px;
  display: flex;
  justify-content: space-between;
  padding: 0 10px;
}
/* the wire the bulbs hang from */
.advent-banner-lights::before { content: ''; position: absolute; left: 0; right: 0; top: 4px; height: 1px; background: rgb(20 10 10 / 0.6); }
.advent-banner-lights i {
  width: 7px;
  height: 10px;
  margin-top: 4px;
  position: relative;
  border-radius: 50% 50% 50% 50% / 40% 40% 60% 60%;
  background: #ffd45c;
  box-shadow: 0 0 6px 1px currentColor;
  color: #ffd45c;
  animation: advent-light 1.8s ease-in-out infinite;
}
.advent-banner-lights i:nth-child(4n + 1) { background: #ff5a5f; color: #ff5a5f; }
.advent-banner-lights i:nth-child(4n + 2) { background: #6ee7a0; color: #6ee7a0; }
.advent-banner-lights i:nth-child(4n + 3) { background: #7cc7ff; color: #7cc7ff; }
@keyframes advent-light { 0%, 100% { opacity: 1; } 50% { opacity: 0.35; box-shadow: none; } }

/* the door is a little wrapped present: green paper, gold ribbon both ways */
.advent-banner-door {
  flex-shrink: 0;
  width: 46px;
  height: 50px;
  display: grid;
  place-items: center;
  border-radius: 6px;
  background:
    linear-gradient(90deg, transparent 42%, #e9b949 42% 58%, transparent 58%),
    linear-gradient(0deg, transparent 42%, #e9b949 42% 58%, transparent 58%),
    #1f5a3a;
  box-shadow: 0 2px 0 rgb(0 0 0 / 0.25), inset 0 0 0 1px rgb(255 255 255 / 0.15);
  color: #fff;
  font-family: var(--font-display, Georgia, serif);
  font-size: 1.45rem;
  font-weight: 700;
  line-height: 1;
  text-shadow: 0 1px 2px rgb(0 0 0 / 0.6), 0 0 8px rgb(0 0 0 / 0.4);
}
.advent-banner-door svg { width: 24px; height: 24px; filter: drop-shadow(0 1px 2px rgb(0 0 0 / 0.5)); }
.advent-banner-text { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.advent-banner-eyebrow { display: flex; align-items: center; flex-wrap: wrap; gap: 4px 8px; font-size: 0.7rem; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; color: #ffd98a; }
/* "the winner gets a prize" - a small gold pill beside the eyebrow */
.advent-banner-prize { padding: 2px 8px; border-radius: 999px; background: #ffd45c; color: #4d2a05; letter-spacing: 0.04em; }
.advent-banner-title { font-family: var(--font-display, Georgia, serif); font-size: 1.2rem; line-height: 1.25; }
.advent-banner-sub { font-size: 0.86rem; opacity: 0.9; }
.advent-banner-trees { position: relative; flex-shrink: 0; align-self: stretch; width: 96px; margin: -18px 0 -12px; }
.advent-banner-trees :deep(.xmas-scene) { position: absolute; inset: 0; }
.advent-banner-cta {
  flex-shrink: 0;
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  background: #fdf1ea;
  color: #6a1a1d;
  font-weight: 700;
  font-size: 0.88rem;
  white-space: nowrap;
  box-shadow: 0 2px 0 rgb(0 0 0 / 0.25);
}
@media (max-width: 560px) {
  .advent-banner { flex-wrap: wrap; gap: 10px 12px; padding: 18px 12px 12px; }
  .advent-banner-door { width: 38px; height: 42px; font-size: 1.2rem; }
  .advent-banner-door svg { width: 20px; height: 20px; }
  .advent-banner-title { font-size: 1rem; }
  .advent-banner-text { flex-basis: calc(100% - 140px); }
  .advent-banner-trees { width: 70px; margin: -18px -4px 0 0; height: 64px; align-self: flex-start; }
  .advent-banner-cta { flex-basis: 100%; text-align: center; }
}
@media (prefers-reduced-motion: reduce) {
  .advent-banner-lights i { animation: none; }
}
</style>
