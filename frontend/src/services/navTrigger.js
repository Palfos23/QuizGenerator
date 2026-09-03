import { reactive } from 'vue'

const state = reactive({
  tension: 0,
  gridBattle: 0,
  imposter: 0,
  fiveOhOne: 0,
  myQuizzes: 0,
  generate: 0,
  startingXiBattle: 0,
  bullseye: 0,
  penaltyShootout: 0
})

export default {
  state,
  fire(key) {
    state[key]++
  }
}
