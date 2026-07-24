<template>
  <div class="dashboard">
    <section class="dashboard-hero">
      <h1>Create a quiz</h1>
      <p class="page-subtitle landing-lede">
        Pick your categories, how many questions from each, a difficulty and a language -
        then reorder, swap out, search in specific questions, or remove anything before you save or print it.
      </p>
      <router-link to="/generate" class="btn btn-primary btn-lg">Create a quiz →</router-link>
    </section>

    <h2 style="margin-top:48px;">Quiz templates</h2>
    <p class="page-subtitle" style="margin-top:-6px;">
      Published by an admin - download a ready-made PDF right away, or copy one into your own My Quizzes to edit first.
    </p>

    <div v-if="templatesLoading" style="color:var(--text-dim);">Loading…</div>
    <div v-else-if="!templates.length" class="empty-state">No templates published yet.</div>
    <div v-else class="saved-quiz-list">
      <div v-for="t in templates" :key="t.id" class="saved-quiz-row">
        <div class="saved-quiz-info">
          <div class="saved-quiz-title">{{ t.title }}</div>
          <div class="saved-quiz-meta">{{ languageLabel(t.language) }} · {{ t.questionCount }} questions</div>
        </div>
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <button class="btn btn-secondary btn-sm" :disabled="downloadingId === t.id" @click="downloadTemplate(t)">
            {{ downloadingId === t.id ? 'Preparing…' : 'Download PDF' }}
          </button>
          <button class="btn btn-secondary btn-sm" :disabled="copyingId === t.id" @click="copyTemplate(t)">
            {{ copyingId === t.id ? 'Copying…' : 'Copy to My Quizzes' }}
          </button>
        </div>
      </div>
    </div>

    <h2 style="margin-top:48px;">More ways to play</h2>
    <div class="dashboard-features">
      <router-link to="/weekly-grid" class="dashboard-feature-card">
        <h3>Weekly grid</h3>
        <p>Guess every athlete that fits this week's theme before you run out of strikes.</p>
      </router-link>
      <router-link to="/grid-battle" class="dashboard-feature-card">
        <h3>Grid Battle</h3>
        <p>A pass-the-device multiplayer version of Weekly Grid - take turns, or lose a life trying.</p>
      </router-link>
      <router-link to="/tension" class="dashboard-feature-card">
        <h3>Tension</h3>
        <p>A pass-the-device party quiz - push for a high-value guess, or play it safe.</p>
      </router-link>
      <router-link to="/my-quizzes" class="dashboard-feature-card">
        <h3>My quizzes</h3>
        <p>Revisit, edit or re-download anything you've saved - or copy an admin-published template.</p>
      </router-link>
      <router-link to="/suggest-question" class="dashboard-feature-card">
        <h3>Suggest a question</h3>
        <p>Add to the shared question bank - admin-reviewed, and usable in your own quizzes either way.</p>
      </router-link>
      <router-link to="/report-problem" class="dashboard-feature-card">
        <h3>Report a problem</h3>
        <p>Found a bug or something confusing? Let us know.</p>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../services/api'
import toast from '../services/toast'
import { languageLabel } from '../constants'

const templates = ref([])
const templatesLoading = ref(true)
const downloadingId = ref(null)
const copyingId = ref(null)

onMounted(loadTemplates)

async function loadTemplates() {
  templatesLoading.value = true
  try {
    templates.value = await api.listQuizTemplates()
  } catch (e) {
    // non-critical - the section just shows its empty state
  } finally {
    templatesLoading.value = false
  }
}

async function downloadTemplate(t) {
  downloadingId.value = t.id
  try {
    const full = await api.getQuizTemplate(t.id)
    const blob = await api.exportPdf(full, true)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${full.title.replace(/[^a-zA-Z0-9-_]/g, '_')}.pdf`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    toast.show('Could not download that template.')
  } finally {
    downloadingId.value = null
  }
}

async function copyTemplate(t) {
  copyingId.value = t.id
  try {
    await api.copyQuizTemplate(t.id)
    toast.show(`"${t.title}" added to My Quizzes.`)
  } catch (e) {
    toast.show('Could not copy that template.')
  } finally {
    copyingId.value = null
  }
}
</script>
