<template>
  <div v-if="cookieConsent.isUnknown.value" class="cookie-banner" role="dialog" :aria-label="$t('Cookie-innstillinger§CookieConsentBannerA')">
    <p class="cookie-banner-text">
      {{ $t('Dette nettstedet setter ingen egne informasjonskapsler. Innlogging med Google laster Googles eget innloggingsskript, som kan sette informasjonskapsler på Googles domene - det lastes først etter at du godtar nedenfor. Hvis du avslår, bruker du i stedet «Fortsett med e-post». Se våre') }}
      <router-link to="/cookies">{{ $t('retningslinjer for informasjonskapsler') }}</router-link>.
    </p>
    <div class="cookie-banner-actions">
      <button type="button" class="btn btn-secondary btn-sm" @click="cookieConsent.decline()">{{ $t('Avslå') }}</button>
      <button type="button" class="btn btn-primary btn-sm" @click="cookieConsent.accept()">{{ $t('Godta') }}</button>
    </div>
  </div>
</template>

<script setup>
import cookieConsent from '../services/cookieConsent'
</script>

<style scoped>
.cookie-banner {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 90;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
  padding: 16px 24px;
  background: var(--ink-raised);
  border-top: 2px solid var(--rule);
  box-shadow: none;
}
.cookie-banner-text {
  margin: 0;
  max-width: 640px;
  color: var(--text-dim);
  font-size: 0.85rem;
  line-height: 1.5;
}
.cookie-banner-text a {
  color: var(--gold);
}
.cookie-banner-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}
</style>
