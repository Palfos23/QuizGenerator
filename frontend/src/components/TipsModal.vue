<template>
  <div class="tips-wrap">
    <button type="button" class="tips-chip" @click="show">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18h6M10 21h4M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2V16h5v-.1c0-.8.4-1.5 1-2A6 6 0 0 0 12 3z" /></svg>
      {{ $t('Tips') }}
    </button>

    <Teleport to="body">
      <div v-if="open" class="modal-backdrop" @click.self="close">
        <div class="modal dq-modal tips-modal" role="dialog" aria-modal="true" :aria-label="title || $t('Tips og triks')">
          <div class="dq-modal-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18h6M10 21h4M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2V16h5v-.1c0-.8.4-1.5 1-2A6 6 0 0 0 12 3z" /></svg>
          </div>
          <h2 class="dq-modal-title">{{ title || $t('Tips og triks') }}</h2>
          <p v-if="subtitle" class="dq-modal-sub">{{ subtitle }}</p>

          <ol class="tips-list">
            <li v-for="(tip, i) in tips" :key="i">
              <span class="tips-num">{{ i + 1 }}</span>
              <div>
                <strong>{{ tip.title }}</strong>
                <p>{{ tip.text }}</p>
                <router-link v-if="tip.to" :to="tip.to" class="tips-link" @click="close">{{ tip.toLabel }} →</router-link>
              </div>
            </li>
          </ol>

          <div class="dq-modal-actions tips-actions">
            <button type="button" class="btn btn-secondary" @click="closeAndSilence">{{ $t('Ikke vis tips automatisk') }}</button>
            <button ref="okButton" type="button" class="btn btn-primary" @click="close">{{ $t('Skjønner') }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import tipsService from '../services/tips'
import cookieConsent from '../services/cookieConsent'
import loginDialog from '../services/loginDialog'
import { useEscapeKey } from '../composables/useEscapeKey'

// A page's tips: one small "Tips" button on the page, and a popup that opens by itself the first time the visitor
// reaches the page (never again after that) - but not while the cookie question or the sign-in popup is up, so two
// popups never stack. Closing it in any way counts as "seen".
const props = defineProps({
  id: { type: String, required: true },
  tips: { type: Array, required: true }, // [{ title, text, to?, toLabel? }]
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' }
})

const open = ref(false)
const okButton = ref(null)

function show() {
  open.value = true
}
function close() {
  open.value = false
  tipsService.markSeen(props.id)
}
function closeAndSilence() {
  tipsService.turnOff()
  close()
}
useEscapeKey(() => { if (open.value) close() })

watch(open, async isOpen => {
  if (isOpen) {
    await nextTick()
    okButton.value?.focus({ preventScroll: true }) // on a phone the list scrolls - keep it at the top
  }
})

let timer = null
let stop = null
onMounted(() => {
  if (!props.tips.length || !tipsService.shouldAutoShow(props.id)) return
  // wait until nothing else is asking for the visitor's attention, then a short beat so the page has settled
  stop = watch(
    () => cookieConsent.isUnknown.value || loginDialog.state.open,
    blocked => {
      clearTimeout(timer)
      if (blocked) return
      timer = setTimeout(() => {
        if (tipsService.shouldAutoShow(props.id)) open.value = true
        stop && stop()
      }, 900)
    },
    { immediate: true }
  )
})
onBeforeUnmount(() => {
  clearTimeout(timer)
  stop && stop()
})
</script>

<style scoped>
.tips-wrap { margin: 0 0 14px; }
.tips-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 13px 5px 10px;
  border: 1px solid rgb(var(--fg-rgb) / 0.22);
  border-radius: 999px;
  background: transparent;
  color: var(--text-dim);
  font: inherit;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease, background-color 0.15s ease;
}
.tips-chip svg { width: 16px; height: 16px; }
.tips-chip:hover { border-color: var(--primary); color: var(--text); background: rgb(var(--primary-rgb) / 0.08); }

.tips-modal { max-width: 520px; max-height: 88vh; overflow-y: auto; }
.tips-list { list-style: none; margin: 4px 0 0; padding: 0; display: grid; gap: 14px; }
.tips-list li { display: flex; gap: 12px; align-items: flex-start; }
.tips-num {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  margin-top: 1px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgb(var(--primary-rgb) / 0.16);
  color: var(--primary);
  font-size: 0.78rem;
  font-weight: 800;
}
.tips-list strong { display: block; margin-bottom: 2px; }
.tips-list p { margin: 0; color: var(--text-dim); font-size: 0.92rem; line-height: 1.45; }
.tips-link { display: inline-block; margin-top: 6px; font-size: 0.88rem; font-weight: 600; }
.tips-actions { flex-wrap: wrap; }
@media (max-width: 520px) {
  .tips-actions .btn { flex: 1 1 100%; }
}
</style>
