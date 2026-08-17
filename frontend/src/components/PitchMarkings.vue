<!--
  Purely decorative full-pitch line markings (boundary, halfway line, center
  circle, both penalty areas, six-yard boxes, penalty arcs, corner arcs) -
  dropped as the first child inside any ".pitch" container, behind the
  ".pitch-row" shirts (which sit at z-index:1). One shared component so the
  three places a pitch renders (solo play, admin preview, online battle)
  never drift out of sync with each other.
-->
<template>
  <div class="pitch-markings" aria-hidden="true">
    <div class="pitch-boundary"></div>
    <div class="pitch-halfway-line"></div>
    <div class="pitch-center-circle"></div>
    <div class="pitch-center-spot"></div>

    <div class="pitch-penalty-box bottom"></div>
    <div class="pitch-six-yard-box bottom"></div>
    <div class="pitch-penalty-spot bottom"></div>
    <div class="pitch-penalty-arc bottom"></div>

    <div class="pitch-penalty-box top"></div>
    <div class="pitch-six-yard-box top"></div>
    <div class="pitch-penalty-spot top"></div>
    <div class="pitch-penalty-arc top"></div>

    <div class="pitch-corner-arc top-left"><div class="pitch-corner-arc-circle top-left"></div></div>
    <div class="pitch-corner-arc top-right"><div class="pitch-corner-arc-circle top-right"></div></div>
    <div class="pitch-corner-arc bottom-left"><div class="pitch-corner-arc-circle bottom-left"></div></div>
    <div class="pitch-corner-arc bottom-right"><div class="pitch-corner-arc-circle bottom-right"></div></div>
  </div>
</template>
