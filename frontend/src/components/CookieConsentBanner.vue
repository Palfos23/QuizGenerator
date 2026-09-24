<template>
  <div v-if="cookieConsent.isUnknown.value" class="cookie-banner" role="dialog" aria-label="Cookie preferences">
    <p class="cookie-banner-text">
      This site doesn't set cookies of its own. Signing in with Google loads Google's own sign-in
      script, which can set cookies on Google's domain - it only loads after you accept below.
      Declining just means using "Continue with email" instead. See our
      <router-link to="/cookies">Cookie Policy</router-link>.
    </p>
    <div class="cookie-banner-actions">
      <button type="button" class="btn btn-secondary btn-sm" @click="cookieConsent.decline()">Decline</button>
      <button type="button" class="btn btn-primary btn-sm" @click="cookieConsent.accept()">Accept</button>
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
  border-top: 1px solid var(--border);
  box-shadow: 0 -10px 30px rgba(0, 0, 0, 0.35);
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
