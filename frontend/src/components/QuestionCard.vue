<template>
  <div class="question-card" :class="{ 'discard-anim': discarding }" :style="tiltStyle">
    <div class="question-card-header">
      <span class="question-number">Q{{ index + 1 }}</span>
      <div class="question-tags">
        <span class="tag category">{{ question.category }}</span>
        <span class="tag difficulty-tag" :style="{ background: difficultyColor(question.difficultyLevel) }">
          {{ question.difficultyLevel }}/10
        </span>
      </div>
    </div>

    <div class="question-text">{{ question.questionText }}</div>

    <div class="answer-box">
      <span class="answer-label">Answer</span>
      {{ question.answer }}
    </div>

    <div class="question-card-actions no-print" v-if="editable">
      <div class="reorder-buttons">
        <button class="btn btn-secondary btn-sm icon-btn" :disabled="isFirst" @click="$emit('move-up')" aria-label="Move up">↑</button>
        <button class="btn btn-secondary btn-sm icon-btn" :disabled="isLast" @click="$emit('move-down')" aria-label="Move down">↓</button>
      </div>
      <div style="flex:1;"></div>
      <button class="btn btn-secondary btn-sm" :disabled="busy" @click="$emit('discard')">
        Discard &amp; replace
      </button>
      <button class="btn btn-danger btn-sm" :disabled="busy" @click="$emit('remove')">
        Remove
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { difficultyColor } from '../constants'

const props = defineProps({
  question: { type: Object, required: true },
  index: { type: Number, required: true },
  editable: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
  discarding: { type: Boolean, default: false },
  isFirst: { type: Boolean, default: false },
  isLast: { type: Boolean, default: false }
})

defineEmits(['discard', 'remove', 'move-up', 'move-down'])

// small alternating tilt so the stack reads like real index cards, not a rigid list
const tiltStyle = computed(() => {
  const tilt = props.index % 2 === 0 ? -0.4 : 0.4
  return { '--tilt': `${tilt}deg` }
})
</script>
