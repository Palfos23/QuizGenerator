<template>
  <div style="max-width:760px; margin:0 auto;">
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:12px;">
      <h1 style="margin:0;">Cookie Policy</h1>
      <router-link to="/" class="btn btn-secondary btn-sm">← Back</router-link>
    </div>

    <p class="page-subtitle">Last updated: 2026-09-24.</p>

    <h2>The short version</h2>
    <p>
      This site doesn't set any cookies of its own. It uses your browser's local storage instead (functionally
      similar, and treated the same way under Norwegian/EU cookie law) to keep you signed in and remember a few
      preferences. The only thing here that can set an actual cookie is Google's sign-in button - and it only loads
      after you accept below.
    </p>

    <h2>What we store, and why</h2>
    <table class="table" style="width:100%;">
      <thead>
        <tr><th>What</th><th>Purpose</th><th>Kept until</th></tr>
      </thead>
      <tbody>
        <tr><td>Sign-in session</td><td>Keeps you logged in between visits</td><td>You log out, or it expires</td></tr>
        <tr><td>Display name &amp; role</td><td>Shows your name in the app, remembers guest/admin status</td><td>Same as above</td></tr>
        <tr><td>In-progress game state</td><td>Lets you resume a pass-and-play game or rejoin an active room</td><td>Until the game finishes</td></tr>
        <tr><td>Quiz draft</td><td>Autosaves an in-progress "Create a quiz" form</td><td>Until you finish or clear it</td></tr>
        <tr><td>Cookie preference</td><td>Remembers your choice below, so we don't ask every visit</td><td>Until you change it</td></tr>
      </tbody>
    </table>
    <p style="color:var(--text-dim); font-size:0.85rem;">
      All of the above is "strictly necessary" for a feature you're actively using - the kind of storage that both
      Norwegian law (ekomloven §2-7b) and GDPR's cookie rules exempt from needing separate consent, the same way a
      shopping cart doesn't need one.
    </p>

    <h2>Google Sign-In</h2>
    <p>
      If you choose "Continue with Google" and accept the prompt, we load Google's own sign-in script. From that
      point on, Google can set cookies on its own domain (accounts.google.com) as part of running that sign-in flow -
      this is Google's cookie, not ours, and governed by
      <a href="https://policies.google.com/privacy" target="_blank" rel="noopener">Google's own privacy policy</a>.
      If you decline, the script never loads, and you can still sign in with "Continue with email" instead.
    </p>

    <h2>Fonts</h2>
    <p>
      The typefaces used on this site are hosted directly by us, not loaded live from Google Fonts - so simply
      viewing a page here never sends your IP address to Google just to render text.
    </p>

    <h2>Changing your choice</h2>
    <p>
      Click <strong>"Cookie settings"</strong> in the footer of any page to see the choice again and change it.
    </p>

    <p>See also our <router-link to="/privacy">Privacy Policy</router-link>.</p>
  </div>
</template>
