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
      <router-link to="/my-quizzes?tab=templates" class="dashboard-feature-card">
        <h3>Quiz templates</h3>
        <p>Pre-made quizzes published by an admin - download a PDF right away, or copy one to edit.</p>
      </router-link>
      <router-link to="/weekly-grid" class="dashboard-feature-card">
        <h3>Weekly grid</h3>
        <p>Guess every answer that fits this week's theme before you run out of strikes.</p>
      </router-link>
      <router-link to="/starting-xi" class="dashboard-feature-card">
        <h3>Starting XI</h3>
        <p>Guess a full lineup, position by position, before the week's board runs out of lives.</p>
      </router-link>
      <router-link to="/grid-battle" class="dashboard-feature-card">
        <h3>Grid Battle</h3>
        <p>A pass-the-device multiplayer version of Weekly Grid - take turns, or lose a life trying.</p>
      </router-link>
      <router-link to="/starting-xi-battle" class="dashboard-feature-card">
        <h3>XI Battle</h3>
        <p>Same idea as Grid Battle, for a Starting XI board - take turns naming the lineup.</p>
      </router-link>
      <router-link to="/tension" class="dashboard-feature-card">
        <h3>Tension</h3>
        <p>A pass-the-device party quiz - push for a high-value guess, or play it safe.</p>
      </router-link>
      <router-link to="/501" class="dashboard-feature-card">
        <h3>501</h3>
        <p>A darts-style countdown from 501 - 1v1, checkout between 0 and -10 to win.</p>
      </router-link>
      <router-link to="/imposter" class="dashboard-feature-card">
        <h3>Imposter</h3>
        <p>One player doesn't get the answer - find out who by asking around the table.</p>
      </router-link>
      <router-link to="/bullseye" class="dashboard-feature-card">
        <h3>Bullseye</h3>
        <p>Everyone answers, lowest score is eliminated each round, until one player is left.</p>
      </router-link>
      <router-link to="/penalty-shootout" class="dashboard-feature-card">
        <h3>Penalty Shootout</h3>
        <p>Guess who took each penalty, in real order - solo, or pass the device around and take turns.</p>
      </router-link>
      <router-link to="/my-quizzes" class="dashboard-feature-card">
        <h3>My quizzes</h3>
        <p>Revisit, edit or re-download anything you've saved.</p>
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
