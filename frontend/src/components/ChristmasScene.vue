<template>
  <!-- Decoration only: a snowy night with pine trees (one of them decorated) and falling snow.
       "hero": the full scene behind the calendar's header. "trees": just the three trees, sized to their box (the
       advent banner sets them beside its button). "snow": just the falling snow, over a whole element. -->
  <div class="xmas-scene" :class="'xmas-scene--' + variant" aria-hidden="true">
    <svg
      v-if="variant !== 'snow'"
      class="xmas-scene-art"
      :viewBox="variant === 'hero' ? '0 0 600 160' : '424 38 192 124'"
      :preserveAspectRatio="variant === 'hero' ? 'xMaxYMax slice' : 'xMidYMax meet'"
    >
      <g v-if="variant === 'hero'" class="xmas-stars">
        <circle v-for="s in STARS" :key="s.x + '-' + s.y" :cx="s.x" :cy="s.y" :r="s.r" :style="{ animationDelay: s.d + 's' }" />
      </g>
      <circle v-if="variant === 'hero'" class="xmas-moon" cx="520" cy="34" r="16" />

      <path v-if="variant === 'hero'" class="xmas-hill xmas-hill--back" d="M0 128 Q120 104 250 122 T480 112 T600 118 V160 H0Z" />

      <g v-for="t in TREES" :key="t.x" :transform="`translate(${t.x} ${t.y}) scale(${t.s})`" class="xmas-tree" :class="{ far: t.far }">
        <rect x="-3" y="0" width="6" height="10" class="xmas-trunk" />
        <path d="M0 -62 L18 -34 H10 L26 -12 H14 L32 4 H-32 L-14 -12 H-26 L-10 -34 H-18 Z" class="xmas-needles" />
        <path d="M0 -62 L8 -50 L2 -48 L-3 -51 L-8 -50 Z M-10 -34 L-18 -34 L-14 -29 Z M10 -34 L18 -34 L14 -29 Z M-26 -12 L-14 -12 L-20 -7 Z M14 -12 L26 -12 L20 -7 Z" class="xmas-snowcap" />
        <template v-if="t.lit">
          <circle v-for="(o, i) in ORNAMENTS" :key="i" :cx="o.x" :cy="o.y" r="2.6" :class="'xmas-bauble xmas-bauble--' + o.c" :style="{ animationDelay: (i * 0.35) + 's' }" />
          <path d="M0 -71 L2.4 -65.5 L8 -65 L3.8 -61.4 L5 -56 L0 -59 L-5 -56 L-3.8 -61.4 L-8 -65 L-2.4 -65.5 Z" class="xmas-topstar" />
        </template>
      </g>

      <path v-if="variant === 'hero'" class="xmas-hill xmas-hill--front" d="M0 140 Q150 126 300 138 T600 132 V160 H0Z" />
    </svg>

    <span
      v-for="(f, i) in (variant === 'trees' ? [] : FLAKES)"
      :key="i"
      class="xmas-flake"
      :style="{ left: f.x + '%', width: f.size + 'px', height: f.size + 'px', animationDuration: f.dur + 's', animationDelay: f.delay + 's', opacity: f.o }"
    ></span>
  </div>
</template>

<script setup>
const props = defineProps({
  variant: { type: String, default: 'hero' } // 'hero' | 'trees' | 'snow'
})

// Fixed positions (no randomness), so the scene is the same on every render and in tests.
const STARS = [
  { x: 40, y: 22, r: 1.2, d: 0 }, { x: 110, y: 46, r: 0.9, d: 1.1 }, { x: 170, y: 18, r: 1.4, d: 0.5 },
  { x: 240, y: 40, r: 1, d: 1.6 }, { x: 300, y: 14, r: 1.2, d: 0.9 }, { x: 360, y: 52, r: 0.9, d: 2.1 },
  { x: 420, y: 24, r: 1.3, d: 0.3 }, { x: 470, y: 60, r: 1, d: 1.3 }, { x: 560, y: 70, r: 1.1, d: 0.7 },
  { x: 80, y: 74, r: 0.8, d: 1.9 }, { x: 200, y: 76, r: 0.8, d: 2.4 }, { x: 590, y: 20, r: 1, d: 1.5 }
]
const HERO_TREES = [
  { x: 40, y: 136, s: 0.9, far: true }, { x: 92, y: 132, s: 1.25 }, { x: 150, y: 138, s: 0.8, far: true },
  { x: 430, y: 134, s: 1.05, far: true }, { x: 488, y: 136, s: 1.55, lit: true }, { x: 552, y: 132, s: 1.15 },
  { x: 596, y: 138, s: 0.85, far: true }
]
const BANNER_TREES = [
  { x: 462, y: 150, s: 1.05, far: true }, { x: 518, y: 152, s: 1.5, lit: true }, { x: 575, y: 150, s: 1.15 }
]
const TREES = props.variant === 'hero' ? HERO_TREES : BANNER_TREES
const ORNAMENTS = [
  { x: -6, y: -42, c: 'red' }, { x: 7, y: -38, c: 'gold' }, { x: -12, y: -22, c: 'gold' },
  { x: 3, y: -24, c: 'red' }, { x: 15, y: -18, c: 'blue' }, { x: -20, y: -3, c: 'red' },
  { x: -5, y: -6, c: 'blue' }, { x: 10, y: -2, c: 'gold' }, { x: 23, y: -1, c: 'red' }
]
const FLAKES = Array.from({ length: props.variant === 'hero' ? 34 : 18 }, (_, i) => ({
  x: (i * 37 + 11) % 100,
  size: 2 + (i % 3),
  dur: 7 + (i % 5) * 1.6,
  delay: -((i * 1.7) % 9),
  o: 0.45 + (i % 4) * 0.13
}))
</script>

<style scoped>
.xmas-scene { position: absolute; inset: 0; overflow: hidden; pointer-events: none; border-radius: inherit; }
.xmas-scene-art { position: absolute; inset: 0; width: 100%; height: 100%; }
.xmas-stars circle { fill: #fff6d6; animation: xmas-twinkle 3.2s ease-in-out infinite; }
.xmas-moon { fill: #fff4d0; opacity: 0.9; }
.xmas-hill--back { fill: rgb(255 255 255 / 0.12); }
.xmas-hill--front { fill: rgb(255 255 255 / 0.2); }
.xmas-needles { fill: #1f5a3a; }
.xmas-tree.far .xmas-needles { fill: #17442c; }
.xmas-tree.far { opacity: 0.85; }
.xmas-snowcap { fill: #f4fbff; opacity: 0.9; }
.xmas-trunk { fill: #5b3a24; }
.xmas-topstar { fill: #ffd45c; filter: drop-shadow(0 0 3px rgb(255 212 92 / 0.9)); }
.xmas-bauble { animation: xmas-twinkle 2.4s ease-in-out infinite; }
.xmas-bauble--red { fill: #ff5a5f; }
.xmas-bauble--gold { fill: #ffd45c; }
.xmas-bauble--blue { fill: #7cc7ff; }

.xmas-flake {
  position: absolute;
  top: -8px;
  border-radius: 50%;
  background: #fff;
  animation-name: xmas-fall;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
}
@keyframes xmas-fall {
  from { transform: translate3d(0, -10px, 0); }
  50% { transform: translate3d(8px, 50cqh, 0); }
  to { transform: translate3d(-4px, 100cqh, 0); }
}
@keyframes xmas-twinkle { 0%, 100% { opacity: 1; } 50% { opacity: 0.35; } }
.xmas-scene { container-type: size; }

@media (prefers-reduced-motion: reduce) {
  .xmas-flake, .xmas-stars circle, .xmas-bauble { animation: none; }
  .xmas-flake { top: auto; bottom: 30%; }
}
</style>
