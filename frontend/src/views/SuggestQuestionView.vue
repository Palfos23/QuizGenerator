<template>
  <div>
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:12px;">
      <h1 style="margin:0;">{{ $t('Foreslå et spørsmål') }}</h1>
      <router-link to="/dashboard" class="btn btn-secondary btn-sm">{{ $t('← Tilbake til oversikten') }}</router-link>
    </div>
    <p class="page-subtitle">
      {{ $t('Send inn et spørsmål til gjennomgang av en administrator. Hvis det godkjennes, blir det en del av den felles spørsmålsbanken for alle. Hvis det avvises, ser du hvorfor her - og du kan uansett bruke det i dine egne quizer.') }}
    </p>

    <div v-if="error" class="banner error">{{ error }}</div>

    <div class="field">
      <label>{{ $t('Spørsmål§SuggestQuestionViewT') }}<input type="text" v-model="form.questionText" :placeholder="$t('f.eks. Hva er hovedstaden i Norge?')" /></label>
    </div>

    <div class="field" style="display:flex; gap:16px; flex-wrap:wrap;">
      <div style="flex:1; min-width:160px;">
        <label>{{ $t('Kategori') }}<input type="text" v-model="form.category" :placeholder="$t('f.eks. Geografi')" /></label>
      </div>
      <div style="flex:1; min-width:160px;">
        <label>{{ $t('Vanskelighetsgrad§SuggestQuestionViewT') }} <span class="picker-hint">{{ $t('1 (lett) - 10 (vanskelig)') }}</span><input type="number" min="1" max="10" v-model.number="form.difficultyLevel" /></label>
      </div>
    </div>

    <div class="field">
      <label>{{ $t('Språk§SuggestQuestionViewT') }} <span class="picker-hint">{{ $t('velg en') }}</span></label>
      <div class="language-row">
        <button
          v-for="lang in LANGUAGES"
          :key="lang.code"
          class="language-btn"
          :class="{ active: form.language === lang.code }"
          @click="form.language = lang.code"
        >
          <span>{{ lang.flag }}</span> {{ lang.label }}
        </button>
      </div>
    </div>

    <div class="field">
      <label>{{ $t('Svar§SuggestQuestionViewT') }}<input type="text" v-model="form.answer" /></label>
    </div>

    <div class="field">
      <label style="display:flex; align-items:center; gap:8px; text-transform:none; font-weight:400;">
        <input type="checkbox" v-model="form.couldChange" style="width:auto;" />
        {{ $t('Dette svaret kan endre seg over tid (f.eks. en gjeldende rekord eller tittelinnehaver)') }}
      </label>
    </div>

    <button class="btn btn-primary" :disabled="submitting" @click="submit">
      {{ submitting ? $t('Sender inn…') : $t('Send inn til gjennomgang') }}
    </button>

    <h2 style="margin-top:40px;">{{ $t('Dine innsendinger') }}</h2>

    <div v-if="loading" style="color:var(--text-dim);">{{ $t('Laster…') }}</div>
    <div v-else-if="!submissions.length" class="empty-state">{{ $t('Du har ikke sendt inn noen spørsmål ennå.') }}</div>

    <div v-else class="saved-quiz-list">
      <div v-for="s in submissions" :key="s.id" class="saved-quiz-row" style="align-items:flex-start;">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ s.questionText }}</div>
          <div class="saved-quiz-meta">
            {{ s.category }} · {{ languageLabel(s.language) }} {{ $t('· vanskelighetsgrad') }} {{ s.difficultyLevel }}
          </div>
          <div v-if="s.status === 'REJECTED'" style="color:var(--coral); font-size:0.85rem; margin-top:6px;">
            {{ $t('Avvist:') }} {{ s.rejectionReason }}
          </div>
        </div>
        <span
          class="tag"
          :style="statusStyle(s.status)"
        >{{ statusLabel(s.status) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { t as _t } from '../i18n'
import { onMounted, reactive, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import { LANGUAGES, languageLabel } from '../constants'

const form = reactive({
  questionText: '', category: '', difficultyLevel: 5, language: 'EN', answer: '', couldChange: false
})
const submitting = ref(false)
const error = ref('')
const submissions = ref([])
const loading = ref(true)

onMounted(loadMine)

async function loadMine() {
  loading.value = true
  try {
    submissions.value = await api.myQuestionSubmissions()
  } catch (e) {
    error.value = _t('Kunne ikke laste innsendingene dine.')
  } finally {
    loading.value = false
  }
}

async function submit() {
  error.value = ''
  if (!form.questionText.trim() || !form.category.trim() || !form.answer.trim()) {
    error.value = _t('Spørsmål, kategori og svar må fylles ut.')
    return
  }
  submitting.value = true
  try {
    await api.submitQuestion(form)
    toast.show(_t('Sendt inn til gjennomgang.'))
    form.questionText = ''
    form.category = ''
    form.answer = ''
    form.couldChange = false
    loadMine()
  } catch (e) {
    error.value = e.response?.data?.message || _t('Kunne ikke sende inn spørsmålet.')
  } finally {
    submitting.value = false
  }
}

function statusLabel(status) {
  return { PENDING: _t('Venter på gjennomgang§SuggestQuestionViewJ'), APPROVED: _t('Godkjent'), REJECTED: _t('Avvist') }[status] || status
}
function statusStyle(status) {
  if (status === 'APPROVED') return { background: 'rgba(31, 122, 80, 0.15)', color: 'var(--teal)' }
  if (status === 'REJECTED') return { background: 'rgba(179, 38, 30, 0.15)', color: 'var(--coral)' }
  return { background: 'rgba(216, 67, 43, 0.15)', color: 'var(--gold)' }
}
</script>
