<template>
  <figure class="dqp" aria-hidden="true">
    <header class="dqp-top">
      <div>
        <strong>{{ $t('Dagens quiz§HomeViewT') }}</strong>
        <span class="dqp-date">{{ $t('15 spørsmål · skriv svarene dine') }}</span>
      </div>
      <div class="dqp-progress" role="presentation">
        <span class="dqp-progress-label">{{ $t('3 av 15') }}</span>
        <span class="dqp-bar"><i style="width:20%"></i></span>
      </div>
    </header>

    <div class="dqp-list">
      <article class="dqp-q is-ok">
        <span class="dqp-n">1</span>
        <div class="dqp-main">
          <p>{{ $t('Hva heter Norges lengste elv?') }}</p>
          <div class="dqp-a">Glomma</div>
        </div>
        <span class="dq-pill dq-pill--ok">{{ $t('Riktig§DailyQuizPlayViewM') }}</span>
      </article>

      <article class="dqp-q is-bad">
        <span class="dqp-n">2</span>
        <div class="dqp-main">
          <p>{{ $t('Hvilket land vant fotball-VM i 2022?') }}</p>
          <div class="dqp-a">{{ $t('Frankrike§DailyQuizPreviewT') }}</div>
          <div class="dqp-fix">{{ $t('Riktig svar§DailyQuizPlayViewT') }}: <strong>Argentina</strong></div>
        </div>
        <span class="dq-pill dq-pill--bad">{{ $t('Feil§DailyQuizPlayViewM') }}</span>
      </article>

      <article class="dqp-q is-typing">
        <span class="dqp-n">3</span>
        <div class="dqp-main">
          <p>{{ $t('Hvilket grunnstoff har symbolet Fe?') }}</p>
          <div class="dqp-input"><span>{{ $t('Jern§DailyQuizPreviewT') }}</span><i class="dqp-caret"></i></div>
        </div>
      </article>
    </div>
  </figure>
</template>

<style scoped>
/* A faithful, non-interactive picture of the real daily quiz: the same question rows, answers and verdict pills. */
.dqp { margin: 0; padding: var(--space-4); background: var(--ink-raised); border: 1px solid var(--rule); border-radius: var(--radius-xl); box-shadow: var(--shadow-float); user-select: none; pointer-events: none; }
.dqp-top { display: flex; justify-content: space-between; align-items: center; gap: var(--space-4); padding: var(--space-1) var(--space-2) var(--space-4); }
.dqp-top strong { display: block; font-family: var(--font-display); font-size: 1.2rem; }
.dqp-date { display: block; font-size: 0.85rem; color: var(--text-dim); margin-top: 2px; }
.dqp-progress { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; min-width: 96px; }
.dqp-progress-label { font-size: 0.82rem; font-weight: 600; color: var(--text-dim); }
.dqp-bar { display: block; width: 96px; height: 6px; border-radius: 999px; background: rgb(var(--fg-rgb) / 0.09); overflow: hidden; }
.dqp-bar i { display: block; height: 100%; background: var(--gold); border-radius: inherit; }

/* rows sit 8px inside the card's 16px padding, so the radius steps down: 20 - 16 = 4 around the list, 10 on each row */
.dqp-list { display: grid; gap: var(--space-2); }
.dqp-q { display: grid; grid-template-columns: auto 1fr auto; gap: var(--space-3); align-items: start; padding: var(--space-3) var(--space-4); border-radius: var(--radius-md); background: rgb(var(--fg-rgb) / 0.035); }
.dqp-q.is-ok { background: rgb(var(--teal-rgb) / 0.08); }
.dqp-q.is-bad { background: rgb(var(--coral-rgb) / 0.07); }
.dqp-q.is-typing .dqp-n { background: var(--gold); color: var(--on-accent); }
.dqp-n { width: 24px; height: 24px; display: grid; place-items: center; border-radius: 50%; background: rgb(var(--fg-rgb) / 0.08); color: var(--text-dim); font-size: 0.78rem; font-weight: 700; }
.dqp-main p { margin: 0 0 6px; font-weight: 600; font-size: 0.98rem; line-height: 1.3; }
.dqp-a { font-size: 1.05rem; font-weight: 650; }
.dqp-fix { margin-top: 4px; font-size: 0.85rem; color: var(--text-dim); }
.dqp-fix strong { color: var(--teal-ink); }
.dqp-input { display: flex; align-items: center; gap: 2px; padding: 9px 12px; background: var(--field-bg); border: 1px solid var(--primary); border-radius: var(--radius-sm); box-shadow: 0 0 0 3px rgb(var(--primary-rgb) / 0.15); font-size: 1.02rem; }
.dqp-caret { width: 2px; height: 1.1em; background: var(--gold); animation: dqp-blink 1.1s steps(1) infinite; }
@keyframes dqp-blink { 50% { opacity: 0; } }
@media (prefers-reduced-motion: reduce) { .dqp-caret { animation: none; } }
</style>
