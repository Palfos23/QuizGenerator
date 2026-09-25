<template>
  <div style="display:inline-flex; gap:8px; flex-wrap:wrap;">
    <button type="button" class="btn btn-secondary" @click="share">
      {{ copied ? 'Copied!' : 'Invite friends' }}
    </button>
    <button type="button" class="btn btn-secondary" @click="toggleQr">
      Show QR code
    </button>

    <div v-if="showQr" class="modal-backdrop" @click.self="toggleQr">
      <div class="modal" role="dialog" aria-modal="true" aria-label="QR code to join the room" style="text-align:center; max-width:280px;">
        <h2 style="margin-bottom:4px;">Scan to join</h2>
        <p class="page-subtitle" style="margin-bottom:16px;">Room code {{ roomCode }}</p>
        <img v-if="qrDataUrl" :src="qrDataUrl" alt="QR code to join the room" width="220" height="220" style="border-radius:8px; background:#fff; padding:8px;" />
        <p v-else style="color:var(--text-dim); font-size:0.85rem;">Generating QR code…</p>
        <button type="button" class="btn btn-secondary" style="margin-top:16px; width:100%;" @click="toggleQr">Close</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import QRCode from 'qrcode'
import { useEscapeKey } from '../composables/useEscapeKey'

// /join/:code already exists (see JoinGuestView.vue / router/index.js) as a
// game-agnostic "type a name, get dropped straight into the room" landing
// page - this button just hands out a link to it instead of the room code
// alone, so a friend can tap through without retyping anything. The QR code
// below encodes that same link, so it works identically for a logged-in
// scanner (dropped straight in) and a guest (asked for a name first) -
// generated client-side so the room link is never sent to a third-party
// QR-rendering API.
const props = defineProps({
  roomCode: { type: String, required: true }
})

const copied = ref(false)
const showQr = ref(false)
const qrDataUrl = ref('')
let resetTimer = null

useEscapeKey(() => { showQr.value = false })

function toggleQr() {
  showQr.value = !showQr.value
}

watch(showQr, async (visible) => {
  if (visible && !qrDataUrl.value) {
    try {
      qrDataUrl.value = await QRCode.toDataURL(inviteLink(), { width: 200, margin: 1 })
    } catch (e) {
      // Generation failed (shouldn't normally happen client-side) - the
      // "Generating..." placeholder just stays put; the room code and the
      // Invite friends button above are still there as a fallback.
    }
  }
})

function inviteLink() {
  return `${window.location.origin}/join/${props.roomCode}`
}

async function share() {
  const link = inviteLink()
  // Native share sheet where available (mainly mobile) - lets the host drop
  // the link straight into Messages/WhatsApp/AirDrop instead of copy-paste.
  if (navigator.share) {
    try {
      await navigator.share({ title: 'Join my game', text: `Join my game - room code ${props.roomCode}`, url: link })
      return
    } catch (e) {
      // Share sheet dismissed/cancelled, or unsupported for this content -
      // fall through to a plain clipboard copy instead of failing silently.
    }
  }
  try {
    await navigator.clipboard.writeText(link)
    copied.value = true
    clearTimeout(resetTimer)
    resetTimer = setTimeout(() => { copied.value = false }, 2000)
  } catch (e) {
    // Clipboard access blocked (insecure context, a permissions policy, or
    // the user denying the prompt) - failing silently here would leave the
    // click looking like it did nothing. window.prompt is an ugly but
    // normally-universal fallback: the browser pre-selects its text, so
    // it's still a one-step copy for the host. Some embedding contexts
    // (a sandboxed iframe without allow-modals, certain in-app browsers)
    // disable window.prompt outright and have it throw rather than just
    // return null, so this still needs its own guard rather than assuming
    // the fallback itself can't fail.
    try {
      window.prompt('Copy this link to invite friends:', link)
    } catch (e2) {
      // Truly nothing left to fall back to - the room code shown above
      // this button is still there to read off and share manually.
    }
  }
}
</script>
