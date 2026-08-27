<template>
  <div class="game-preview" role="img" aria-label="Preview of 501: a running score counting down from 501 toward a checkout window.">
    <span class="game-preview-tag">Preview</span>
    <div class="fp-cat">Category · “Premier League appearances”</div>

    <div class="fp-body">
      <div class="fp-board">
        <div class="fp-ring">
          <div class="fp-score">
            <span class="fp-num">8</span>
            <span class="fp-lbl">to check out</span>
          </div>
        </div>
      </div>

      <div class="fp-throws">
        <div class="fp-throw">
          <span class="fp-p">Start</span>
          <span class="fp-d"></span>
          <span class="fp-t">501</span>
        </div>
        <div class="fp-throw">
          <span class="fp-p">M. Salah <i>234</i></span>
          <span class="fp-d">−234</span>
          <span class="fp-t">267</span>
        </div>
        <div class="fp-throw">
          <span class="fp-p">H. Kane <i>89</i></span>
          <span class="fp-d">−89</span>
          <span class="fp-t">178</span>
        </div>
        <div class="fp-throw bust">
          <span class="fp-p">P. Foden <i>201</i></span>
          <span class="fp-d">over 180</span>
          <span class="fp-t">bust</span>
        </div>
        <div class="fp-throw now">
          <span class="fp-p">your throw…</span>
          <span class="fp-d"></span>
          <span class="fp-t">8</span>
        </div>
      </div>
    </div>

    <p class="game-preview-caption">
      Start at 501, subtract each pick’s number, and land between <strong>0 and −10</strong> to win.
      Bust the window or break darts scoring and the throw <span class="bad">doesn’t count</span>.
    </p>
  </div>
</template>

<script setup></script>

<style scoped>
.fp-cat {
  font-size: 0.8rem;
  color: var(--text-dim);
  margin-bottom: 16px;
}
.fp-body {
  display: flex;
  gap: 22px;
  align-items: center;
  justify-content: center;
}
.fp-board {
  flex-shrink: 0;
}
.fp-ring {
  width: 104px;
  height: 104px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle, transparent 34%, rgba(242, 183, 5, 0.14) 35% 44%, transparent 45%),
    conic-gradient(from 0deg, rgba(255,255,255,0.05) 0 25%, rgba(255,255,255,0.02) 25% 50%, rgba(255,255,255,0.05) 50% 75%, rgba(255,255,255,0.02) 75% 100%);
  border: 1px solid var(--border);
}
.fp-score {
  width: 62px;
  height: 62px;
  border-radius: 50%;
  background: var(--gold-soft);
  border: 1px solid rgba(242, 183, 5, 0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.fp-num {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--gold);
  line-height: 1;
}
.fp-lbl {
  font-size: 0.5rem;
  color: var(--text-dim);
  margin-top: 2px;
}
.fp-throws {
  flex: 0 1 380px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.fp-throw {
  display: grid;
  grid-template-columns: 1fr auto 46px;
  align-items: center;
  gap: 10px;
  padding: 5px 10px;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.02);
  font-size: 0.74rem;
}
.fp-p { color: var(--text-dim); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fp-p i { font-style: normal; color: var(--text); font-family: var(--font-mono); font-size: 0.68rem; }
.fp-d { font-family: var(--font-mono); font-size: 0.66rem; color: var(--text-dim); }
.fp-t { font-family: var(--font-mono); font-weight: 700; color: var(--text); text-align: right; }
.fp-throw.bust { background: rgba(255, 77, 109, 0.1); }
.fp-throw.bust .fp-d, .fp-throw.bust .fp-t { color: var(--coral); }
.fp-throw.now { background: var(--gold-soft); }
.fp-throw.now .fp-t { color: var(--gold); }

@media (max-width: 560px) {
  .fp-body { flex-direction: column; gap: 16px; }
  .fp-throws { width: 100%; }
}
</style>
