import { reactive } from 'vue'

const state = reactive({
  tension: 0,
  gridBattle: 0,
  myQuizzes: 0,
  generate: 0
})

export default {
  state,
  fire(key) {
    state[key]++
  }
}
