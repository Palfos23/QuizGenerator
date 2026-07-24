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

    <h2 style="margin-top:48px;">More ways to play</h2>
    <div class="dashboard-features">
      <router-link to="/weekly-grid" class="dashboard-feature-card">
        <h3>Weekly grid</h3>
        <p>Guess every athlete that fits this week's theme before you run out of strikes.</p>
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
    </div>
  </div>
</template>
