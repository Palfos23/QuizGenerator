<template>
  <!-- The advent calendar, top of the dashboard from 1 November through January: a countdown before 1 December, today's
       door while it runs (or "taken" once you've played it), and the season's wrapped afterwards. -->
  <router-link v-if="view" :to="view.to" class="advent-banner">
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
    <span class="advent-banner-cta">{{ view.cta }}</span>
  </router-link>
</template>

<script setup>
import { t as _t } from '../i18n'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import api from '../services/api'
import auth from '../services/auth'

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
.advent-banner {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 0 0 20px;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  background: #7d2424;
  color: #fbeaea;
  text-decoration: none;
  transition: background-color 0.15s ease;
}
.advent-banner:hover { background: #8a2a2a; }
.advent-banner-door {
  flex-shrink: 0;
  width: 44px;
  height: 50px;
  display: grid;
  place-items: center;
  border: 1px solid rgb(251 234 234 / 0.45);
  border-radius: var(--radius-sm);
  font-family: var(--font-display, Georgia, serif);
  font-size: 1.5rem;
  line-height: 1;
}
.advent-banner-door svg { width: 24px; height: 24px; }
.advent-banner-text { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.advent-banner-eyebrow { display: flex; align-items: center; flex-wrap: wrap; gap: 4px 8px; font-size: 0.7rem; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; color: rgb(251 234 234 / 0.8); }
/* "the winner gets a prize" - a small pill beside the eyebrow, in the banner's own light colour */
.advent-banner-prize { padding: 2px 8px; border-radius: 999px; background: #fbeaea; color: #5c1717; letter-spacing: 0.04em; }
.advent-banner-title { font-family: var(--font-display, Georgia, serif); font-size: 1.15rem; line-height: 1.25; }
.advent-banner-sub { font-size: 0.86rem; opacity: 0.88; }
.advent-banner-cta {
  flex-shrink: 0;
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  background: #fbeaea;
  color: #5c1717;
  font-weight: 700;
  font-size: 0.88rem;
  white-space: nowrap;
}
@media (max-width: 560px) {
  .advent-banner { flex-wrap: wrap; gap: 10px 12px; padding: 12px; }
  .advent-banner-door { width: 36px; height: 42px; font-size: 1.2rem; }
  .advent-banner-door svg { width: 20px; height: 20px; }
  .advent-banner-title { font-size: 1rem; }
  .advent-banner-cta { flex-basis: 100%; text-align: center; }
}
</style>
