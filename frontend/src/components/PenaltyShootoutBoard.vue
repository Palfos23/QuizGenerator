<template>
  <div class="pen-shootout-board">
    <div class="pitch-scoreline" v-if="teamName || opponentName">
      <div class="pitch-scoreline-team">
        <img v-if="teamCrestUrl" :src="teamCrestUrl" alt="" class="pitch-scoreline-crest" />
        <span>{{ teamName }}</span>
      </div>
      <div v-if="teamPensScored != null && opponentPensScored != null" class="pitch-scoreline-score">
        <span>{{ teamPensScored }}</span><span class="dash">-</span><span>{{ opponentPensScored }}</span>
      </div>
      <div v-else class="pitch-scoreline-vs">vs</div>
      <div class="pitch-scoreline-team away">
        <img v-if="opponentCrestUrl" :src="opponentCrestUrl" alt="" class="pitch-scoreline-crest" />
        <span>{{ opponentName }}</span>
      </div>
    </div>

    <div class="pen-kick-list">
      <div
        v-for="k in sortedKicks"
        :key="k.id ?? k.kickOrder"
        class="pen-kick-row"
        :class="k.forTeam ? 'pen-kick-row--team' : 'pen-kick-row--opponent'"
      >
        <div
          class="pen-kick-card"
          :class="{ correct: k.solved, 'revealed-only': k.solved && !k.guessedByUser, 'just-solved': k.id === justSolvedId }"
        >
          <span class="pen-kick-order">{{ k.kickOrder }}</span>
          <span class="pen-kick-outcome" :class="k.scored ? 'scored' : 'missed'" :title="k.scored ? 'Scored' : 'Missed'">
            {{ k.scored ? '✓' : '✕' }}
          </span>
          <img v-if="k.solved && k.athletePhotoUrl" :src="k.athletePhotoUrl" alt="" class="pen-kick-photo" />
          <span class="pen-kick-name">{{ k.solved ? k.athleteName : '?' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// Shared by the admin preview, solo play, and pass-and-play battle - a
// vertical timeline of kicks in real shootout order, each side's kicks
// aligned to that side (team left, opponent right) like a broadcast
// shootout tracker. scored/missed is always shown regardless of `solved` -
// see PenaltyKickDto's class comment for why (it's public match history,
// not part of what's being guessed).
const props = defineProps({
  teamName: { type: String, default: '' },
  teamCrestUrl: { type: String, default: '' },
  opponentName: { type: String, default: '' },
  opponentCrestUrl: { type: String, default: '' },
  teamPensScored: { type: Number, default: null },
  opponentPensScored: { type: Number, default: null },
  // [{ id, kickOrder, forTeam, scored, solved, guessedByUser, athleteName, athletePhotoUrl }]
  kicks: { type: Array, default: () => [] },
  justSolvedId: { type: [Number, String], default: null }
})

const sortedKicks = computed(() => [...props.kicks].sort((a, b) => a.kickOrder - b.kickOrder))
</script>
