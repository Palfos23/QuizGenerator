<template>
  <button type="button" class="btn btn-secondary" @click="share">
    {{ copied ? 'Copied!' : 'Invite friends' }}
  </button>
</template>

<script setup>
import { ref } from 'vue'

// /join/:code already exists (see JoinGuestView.vue / router/index.js) as a
// game-agnostic "type a name, get dropped straight into the room" landing
// page - this button just hands out a link to it instead of the room code
// alone, so a friend can tap through without retyping anything.
const props = defineProps({
  roomCode: { type: String, required: true }
})

const copied = ref(false)
let resetTimer = null

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
