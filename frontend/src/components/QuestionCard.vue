<template>
  <div class="question-card" :class="{ 'discard-anim': discarding }">
    <div class="question-card-header">
      <span class="question-number">Q{{ index + 1 }}</span>
      <div class="question-tags">
        <span class="tag category">{{ question.category }}</span>
        <span class="tag difficulty-tag" :style="{ background: difficultyColor(question.difficultyLevel), color: difficultyTextColor(question.difficultyLevel) }">
          {{ question.difficultyLevel }}/10
        </span>
        <span v-for="name in (question.labelNames || [])" :key="name" class="tag" style="background:rgba(61, 79, 159, 0.15); color:#3d4f9f;">{{ name }}</span>
      </div>
    </div>

    <div class="question-text">{{ question.questionText }}</div>

    <img
      v-if="question.photoUrl"
      :src="question.photoUrl"
      alt=""
      style="max-width:220px; max-height:220px; border-radius: 2px; margin:8px 0; display:block;"
      @error="$event.target.style.display = 'none'"
    />

    <div class="answer-box" v-if="showAnswer">
      <span class="answer-label">Svar</span>
      {{ question.answer }}
    </div>

    <button class="btn btn-secondary btn-sm no-print reveal-btn" @click="showAnswer = !showAnswer">
      {{ showAnswer ? 'Skjul svaret' : 'Vis svaret' }}
    </button>

    <div class="question-card-actions no-print" v-if="editable">
      <div class="reorder-buttons">
        <button class="btn btn-secondary btn-sm icon-btn" :disabled="isFirst" @click="$emit('move-up')" aria-label="Flytt opp">↑</button>
        <button class="btn btn-secondary btn-sm icon-btn" :disabled="isLast" @click="$emit('move-down')" aria-label="Flytt ned">↓</button>
      </div>
      <div style="flex:1;"></div>
      <button class="btn btn-secondary btn-sm" :disabled="busy" @click="$emit('discard')">
        Forkast og bytt ut
      </button>
      <button class="btn btn-danger btn-sm" :disabled="busy" @click="$emit('remove')">
        Fjern
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { difficultyColor, difficultyTextColor } from '../constants'

defineProps({
  question: { type: Object, required: true },
  index: { type: Number, required: true },
  editable: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
  discarding: { type: Boolean, default: false },
  isFirst: { type: Boolean, default: false },
  isLast: { type: Boolean, default: false }
})

defineEmits(['discard', 'remove', 'move-up', 'move-down'])

const showAnswer = ref(false)
</script>
