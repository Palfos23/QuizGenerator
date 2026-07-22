import { reactive } from 'vue'

const state = reactive({
  message: '',
  type: 'success', // 'success' | 'error'
  visible: false
})

let hideTimer = null

function show(message, type = 'success', duration = 3000) {
  clearTimeout(hideTimer)
  state.message = message
  state.type = type
  state.visible = true
  hideTimer = setTimeout(() => { state.visible = false }, duration)
}

export default { state, show }
