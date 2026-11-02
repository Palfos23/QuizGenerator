<template>
  <div class="adv-rules">
    <ol class="adv-steps">
      <li>{{ $t('Hver dag fra 1. til 24. desember åpnes en ny luke med en kategori - for eksempel «land med T i navnet».') }}</li>
      <li>{{ $t('Du velger 4 svar du tror passer. Spillmesteren har i skjul valgt sine egne svar.') }}</li>
      <li>{{ $t('Hvert svar du har som matcher et av spillmesterens, gir poeng. Målet er å tenke likt som spillmesteren - men helst ikke like åpenbart som alle andre.') }}</li>
      <li>{{ $t('Luka er åpen hele dagen og stenger ved midnatt. Du får se over svarene før du sender dem inn - men når de er sendt, kan de ikke endres. Dagen etter får du se spillmesterens svar og poengene dine.') }}</li>
      <li>{{ $t('Treffer ingen av spillerne et eneste av svarene, får spillmesteren poeng.') }}</li>
    </ol>

    <h3 class="adv-rules-h">{{ $t('Spesielle luker') }}</h3>
    <p class="adv-rules-note">{{ $t('Underveis dukker det opp luker med egne regler. Du ser hvilken type det er når luka åpnes.') }}</p>
    <ul class="adv-types">
      <li v-for="(info, key) in TYPES" :key="key" v-show="key !== 'NORMAL'">
        <span class="adv-type-emoji" aria-hidden="true">{{ info.emoji }}</span>
        <div><strong>{{ info.name }}</strong><br />{{ info.rules }}</div>
      </li>
    </ul>

    <div class="adv-callout adv-callout--joker">
      <span class="adv-callout-emoji" aria-hidden="true">🃏</span>
      <div>
        <strong>{{ $t('Jokeren') }}</strong>
        <p>{{ $t('Du har 3 jokere i hele julekalenderen. Bruker du en på en luke, får du dobbelt så mange poeng på den luka. Huk av for jokeren før du sender inn svarene.') }}</p>
        <p>{{ $t('Får du minuspoeng, dobles de ikke. Jokeren velger du når du sender inn svarene - etterpå kan den ikke tas tilbake. Jokeren kan ikke brukes på finalen.') }}</p>
      </div>
    </div>

    <div class="adv-callout adv-callout--finale">
      <span class="adv-callout-emoji" aria-hidden="true">🎄</span>
      <div>
        <strong>{{ $t('Finalen 24. desember') }}</strong>
        <p>{{ $t('På julaften teller alle poeng tredoblet - så det er fortsatt mulig å ta igjen mye på siste luke.') }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { TYPES } from '../services/advent'
</script>

<style scoped>
.adv-steps { margin: 0 0 18px; padding-left: 22px; display: grid; gap: 8px; }
.adv-rules-h { margin: 18px 0 4px; font-size: 1.05rem; }
.adv-rules-note { margin: 0 0 10px; color: var(--text-dim); font-size: 0.92rem; }
.adv-types { list-style: none; margin: 0 0 18px; padding: 0; display: grid; gap: 10px; }
.adv-types li { display: flex; gap: 12px; align-items: flex-start; }
.adv-type-emoji { font-size: 1.5rem; line-height: 1.2; flex-shrink: 0; }
.adv-callout { display: flex; gap: 12px; align-items: flex-start; border: 2px solid var(--rule); border-radius: 3px; padding: 12px 14px; margin-top: 10px; }
.adv-callout p { margin: 4px 0 0; }
.adv-callout-emoji { font-size: 1.6rem; line-height: 1.2; flex-shrink: 0; }
.adv-callout--joker { background: rgb(var(--gold-rgb) / 0.08); }
.adv-callout--finale { background: rgb(var(--teal-rgb) / 0.09); }
</style>
