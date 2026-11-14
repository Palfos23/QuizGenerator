<template>
  <div ref="rootEl" class="game-preview" role="img" :aria-label="$t('Forhåndsvisning av XI Battle: en fotballbane med en startellever, noen drakter avslørt og noen fortsatt skjult.')">
    <span class="game-preview-tag">{{ $t('Forhåndsvisning') }}</span>
    <div class="xp-head">
      <span class="xp-match">{{ $t('Arsenal XI · Community Shield 2014') }}</span>
      <span class="xp-lives">
        <span v-for="n in 5" :key="n" class="xp-heart" :class="{ lost: n > lives }">♥</span>
      </span>
    </div>

    <div class="xp-pitch">
      <div v-for="(row, ri) in rows" :key="ri" class="xp-row">
        <div
          v-for="(slot, si) in row"
          :key="si"
          class="xp-shirt"
          :class="{ found: isFound(ri, si, slot), gk: slot.gk, shaking: shaking === ri + '-' + si }"
        >
          <span class="xp-num">{{ isFound(ri, si, slot) ? '' : slot.num }}</span>
          <span v-if="isFound(ri, si, slot)" class="xp-tick">✓</span>
          <span v-if="isFound(ri, si, slot)" class="xp-pname">{{ slot.name }}</span>
        </div>
      </div>
    </div>

    <p class="game-preview-caption">
      {{ $t('Gjett hvem som startet en ekte kamp.') }} <span class="good">{{ $t('Riktig§XiBattlePreviewT') }}</span> {{ $t('avslører drakten og gir poeng;') }}
      <span class="bad">{{ $t('feil') }}</span> {{ $t('koster et liv. Høyest totalsum på 2–4 brett vinner.') }}
    </p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { usePreviewLoop } from '../../composables/usePreviewLoop'

// The film: names are guessed one by one and their shirts flip to show the player; one wrong guess shakes a shirt and costs a life.
const rootEl = ref(null)
const found = ref([])
const lives = ref(5)
const shaking = ref(null)
const isFound = (ri, si, slot) => !!slot.name && found.value.includes(ri + '-' + si)

async function guess(key, sleep) { found.value = [...found.value, key]; await sleep(800) }
async function miss(key, sleep) {
  shaking.value = key; lives.value--
  await sleep(560); shaking.value = null; await sleep(300)
}

usePreviewLoop(rootEl, {
  async play(sleep) {
    found.value = []; lives.value = 5; shaking.value = null; await sleep(1000)
    await guess('0-0', sleep)
    await guess('0-2', sleep)
    await miss('0-1', sleep)
    await guess('1-1', sleep)
    await guess('2-1', sleep)
    await guess('3-0', sleep)
    await sleep(2800)
  },
  finalFrame() { found.value = ['0-0', '0-2', '1-1', '2-1', '3-0']; lives.value = 4 }
})

// Top-to-bottom as displayed: attack at the top, keeper at the back.
const rows = [
  [{ num: 7, name: 'Sánchez' }, { num: 11 }, { num: 14, name: 'Walcott' }],
  [{ num: 16 }, { num: 8, name: 'Arteta' }, { num: 35 }],
  [{ num: 3 }, { num: 6, name: 'Koscielny' }, { num: 5 }, { num: 24 }],
  [{ num: 1, name: 'Szczęsny', gk: true }]
]
</script>

<style scoped>
.xp-head {
  padding-top: 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  max-width: 440px;
  margin: 0 auto 12px;
}
.xp-match { font-size: 0.8rem; color: var(--text-dim); }
.xp-lives { display: inline-flex; gap: 3px; }
.xp-heart { color: var(--coral); font-size: 0.8rem; }
.xp-heart.lost { color: rgb(var(--coral-rgb) / 0.3); }

.xp-pitch {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 440px;
  margin: 0 auto;
  padding: 16px 12px;
  border-radius: var(--radius-sm);
  background:
    repeating-linear-gradient(0deg, rgb(var(--fg-rgb) / 0.07) 0 22px, transparent 22px 44px),
    linear-gradient(180deg, var(--pitch-a), var(--pitch-b));
  border: 1px solid rgb(var(--fg-rgb) / 0.08);
}
.xp-row {
  display: flex;
  justify-content: space-evenly;
}
.xp-shirt {
  position: relative;
  width: 30px;
  height: 32px;
  border-radius: 7px 7px 5px 5px;
  background: rgb(var(--fg-rgb) / 0.5);
  border: 2px solid rgb(var(--fg-rgb) / 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.68rem;
  font-weight: 800;
  color: var(--text);
}
.xp-shirt.gk { border-color: var(--gold); color: var(--gold); }
.xp-shirt { transition: background 250ms var(--ease-out), border-color 250ms var(--ease-out); }
.xp-shirt.found {
  background: var(--teal);
  border-color: var(--teal);
  color: var(--ink-raised);
  animation: pv-flip 520ms var(--ease-out) both;
}
.xp-shirt.shaking { animation: pv-shake 520ms both; border-color: var(--coral); background: rgb(var(--coral-rgb) / 0.5); }
.xp-pname { animation: pv-rise 360ms 260ms var(--ease-out) backwards; }
.xp-heart { transition: color 300ms var(--ease-out); }
.xp-tick { font-size: 0.7rem; }
.xp-pname {
  position: absolute;
  left: 50%;
  bottom: -13px;
  transform: translateX(-50%);
  font-size: 0.56rem;
  font-weight: 700;
  color: var(--pitch-text);
  white-space: nowrap;
}
.xp-row:first-child .xp-pname { bottom: auto; top: -13px; }
</style>
