<template>
  <div style="max-width:760px; margin:0 auto;">
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:12px;">
      <h1 style="margin:0;">Privacy Policy</h1>
      <router-link to="/" class="btn btn-secondary btn-sm">← Back</router-link>
    </div>

    <div class="banner" style="background:rgba(242,183,5,0.1); border:1px solid rgba(242,183,5,0.3); color:var(--text);">
      <strong>Draft, not legal advice.</strong> This page describes what the app actually does today, in plain
      language, for the site operator to review (and have checked by a lawyer if needed) before treating it as final.
    </div>

    <p class="page-subtitle">Last updated: 2026-09-24.</p>

    <h2>Who this is</h2>
    <p>
      Quizzes (quizzes.no) is operated by an individual, not a company. For any privacy question or to exercise the
      rights below, contact <strong>pal.fossnes@gmail.com</strong>.
    </p>

    <h2>What we collect</h2>
    <ul>
      <li><strong>Account info:</strong> your email address and name, either from Google sign-in or from registering with an email and password (in which case a securely hashed - never plain-text - copy of your password is stored).</li>
      <li><strong>Content you create:</strong> quizzes you build or save, questions you submit for the shared question bank, and any problem reports or feedback you send in, including free-text you write.</li>
      <li><strong>Gameplay history:</strong> which weekly grids/boards you've completed and your scores, so you can resume them and see your own past results.</li>
      <li><strong>Multiplayer rooms:</strong> if you host or join a live room, your display name and a connection heartbeat while the game is active.</li>
      <li><strong>Guests:</strong> joining a room as a guest (no account) only ever stores a chosen display name for that session - nothing else, and no account is created.</li>
    </ul>
    <p>We do not collect payment information, government ID numbers, or any special category of data (health, religion, etc.).</p>

    <h2>Why we process it</h2>
    <ul>
      <li>To run your account and let you use the features you sign up for (contract).</li>
      <li>To keep the site secure and prevent abuse, e.g. rate-limiting repeated failed sign-ins (legitimate interest).</li>
      <li>Where you've actively opted in, such as Google Sign-In's own cookie (consent - see our <router-link to="/cookies">Cookie Policy</router-link>).</li>
    </ul>

    <h2>Who else sees it</h2>
    <p>We use a small number of external services to run the site, all acting as data processors on our behalf:</p>
    <ul>
      <li><strong>Google</strong> - to verify "Continue with Google" sign-ins, and to send transactional email (password reset, email verification) via Gmail. Google's own privacy policy applies to their handling of this data.</li>
      <li><strong>Render</strong> - hosts the application itself, in an EU (Frankfurt) data center.</li>
      <li><strong>Our database provider</strong> - stores the data described above, in an EU region.</li>
    </ul>
    <p>We never sell your data, and don't use it for advertising or share it with data brokers.</p>

    <h2>How long we keep it</h2>
    <ul>
      <li>Your account and the content tied to it is kept until you delete your account (see below) - we don't have an automatic expiry for active accounts.</li>
      <li>Multiplayer room data (who joined, display names) is automatically deleted within 24 hours of a game finishing or a room being abandoned.</li>
      <li>Password-reset and email-verification links expire after 30 minutes and aren't reusable.</li>
    </ul>

    <h2>Your rights</h2>
    <p>Under GDPR and the Norwegian Personal Data Act, you can:</p>
    <ul>
      <li><strong>Access &amp; export</strong> your data - download everything we hold on you from your <router-link to="/account">Account</router-link> page.</li>
      <li><strong>Erase</strong> your account and personal data - also from your <router-link to="/account">Account</router-link> page. Content you contributed to the shared question bank or reported is kept but de-identified (no longer linked to you) rather than deleted outright, since it's already been shared with or reviewed by others.</li>
      <li><strong>Correct</strong> inaccurate info - update your name any time, or contact us for anything you can't change yourself.</li>
      <li><strong>Object or restrict</strong> processing, or ask questions about any of the above - contact us using the address at the top of this page.</li>
    </ul>
    <p>
      If you're not satisfied with our response, you can complain to Norway's data protection authority,
      <a href="https://www.datatilsynet.no" target="_blank" rel="noopener">Datatilsynet</a>.
    </p>

    <h2>Children</h2>
    <p>This service isn't directed at children under 13, and we don't knowingly collect data from anyone under that age.</p>

    <h2>Security</h2>
    <p>
      Passwords are hashed (never stored in plain text), traffic is encrypted (HTTPS), and access to admin
      functionality is separately restricted. No system is perfectly secure, but we take reasonable, standard
      precautions.
    </p>

    <h2>Changes</h2>
    <p>If this policy changes in a meaningful way, we'll update the date above.</p>
  </div>
</template>
