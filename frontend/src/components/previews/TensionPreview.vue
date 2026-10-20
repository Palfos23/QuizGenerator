<template>
  <div class="game-preview" role="img" aria-label="Forhåndsvisning av Tension: en skjult rangert liste med ti svar, med et fellesvar rett etter slutten.">
    <span class="game-preview-tag">Forhåndsvisning</span>
    <div class="tp-q">«Nevn en av de 10 mest søkte ingrediensene i oppskrifter»</div>

    <div class="tp-track">
      <div class="tp-fill"></div>
      <div class="tp-danger">
        <span>Tension-sone</span>
      </div>

      <div class="tp-pin safe" style="left: 8%;">
        <span class="tp-dot"></span>
        <span class="tp-label">Salt<b>plass 1 · +1</b></span>
      </div>
      <div class="tp-pin safe hi" style="left: 60%;">
        <span class="tp-dot"></span>
        <span class="tp-label">Basilikum<b>plass 10 · +10</b></span>
      </div>
      <div class="tp-pin trap" style="left: 82%;">
        <span class="tp-dot"></span>
        <span class="tp-label">Oregano<b class="bad">felle · −5</b></span>
      </div>
    </div>

    <div class="tp-scale"><span>trygt · lav belønning</span><span>risikabelt · høy belønning</span><span>for langt</span></div>

    <p class="game-preview-caption">
      Gjett så nær <strong>plass 10</strong> som du tør — jo nærmere du går, jo flere poeng. Går du for langt inn i <span class="bad">tension</span> -sonen, koster det deg.
    </p>
  </div>
</template>

<script setup></script>

<style scoped>
.tp-q {
  font-size: 0.82rem;
  color: var(--text);
  font-style: italic;
  margin-bottom: 22px;
}
.tp-track {
  position: relative;
  height: 12px;
  border-radius: 999px;
  background: rgba(27, 26, 23, 0.06);
  /* Bottom margin has to clear the "hi"/"trap" pin labels below, which are
     ~2 lines tall and absolutely positioned starting 20px under the track -
     too little room here and .tp-scale (the safe/risky/overshoot captions,
     right below in normal flow) collides with and garbles into them,
     especially on narrow screens where there's less width for the text to
     dodge each other horizontally. */
  margin: 40px 0 46px;
}
.tp-fill {
  position: absolute;
  inset: 0 32% 0 0;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(31, 122, 80, 0.25), var(--teal));
}
.tp-danger {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 68%;
  right: 0;
  border-radius: 0 999px 999px 0;
  background: repeating-linear-gradient(-45deg, rgba(179, 38, 30, 0.3) 0 6px, rgba(179, 38, 30, 0.12) 6px 12px);
}
.tp-danger span {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 0.58rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--coral);
  white-space: nowrap;
}
.tp-pin {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
}
.tp-dot {
  display: block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 3px solid var(--teal);
  background: var(--ink-raised);
}
.tp-pin.trap .tp-dot { border-color: var(--coral); }
.tp-label {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 18px;
  font-size: 0.66rem;
  font-weight: 700;
  color: var(--text);
  text-align: center;
  white-space: nowrap;
  line-height: 1.35;
}
.tp-label b {
  display: block;
  font-weight: 500;
  font-size: 0.6rem;
  color: var(--text-dim);
}
.tp-label b.bad { color: var(--coral); }
.tp-pin.hi .tp-label,
.tp-pin.trap .tp-label {
  bottom: auto;
  top: 20px;
}
.tp-scale {
  display: flex;
  justify-content: space-between;
  font-size: 0.58rem;
  color: var(--text-dim);
  margin-top: 4px;
}
</style>
