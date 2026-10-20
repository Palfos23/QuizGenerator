<template>
  <div style="max-width:760px; margin:0 auto;">
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:12px;">
      <h1 style="margin:0;">{{ $t('Retningslinjer for informasjonskapsler') }}</h1>
      <router-link to="/" class="btn btn-secondary btn-sm">{{ $t('← Tilbake') }}</router-link>
    </div>

    <p class="page-subtitle">{{ $t('Sist oppdatert: 24.09.2026.') }}</p>

    <h2>{{ $t('Kortversjonen') }}</h2>
    <p>
      {{ $t('Dette nettstedet setter ingen egne informasjonskapsler. Det bruker i stedet nettleserens lokale lagring (funksjonelt lignende, og behandlet på samme måte etter norsk og europeisk lovgivning om informasjonskapsler) for å holde deg innlogget og huske noen innstillinger. Det eneste her som kan sette en faktisk informasjonskapsel er Googles innloggingsknapp - og den lastes først etter at du godtar nedenfor.') }}
    </p>

    <h2>{{ $t('Hva vi lagrer, og hvorfor') }}</h2>
    <table class="table" style="width:100%;">
      <thead>
        <tr><th>{{ $t('Hva') }}</th><th>{{ $t('Formål') }}</th><th>{{ $t('Beholdes til') }}</th></tr>
      </thead>
      <tbody>
        <tr><td>{{ $t('Innloggingsøkt') }}</td><td>{{ $t('Holder deg innlogget mellom besøk') }}</td><td>{{ $t('Du logger ut, eller den utløper') }}</td></tr>
        <tr><td>{{ $t('Visningsnavn og rolle') }}</td><td>{{ $t('Viser navnet ditt i appen, husker gjest-/administratorstatus') }}</td><td>{{ $t('Som over') }}</td></tr>
        <tr><td>{{ $t('Pågående spilltilstand') }}</td><td>{{ $t('Lar deg fortsette et spill der enheten sendes rundt, eller gå tilbake til et aktivt rom') }}</td><td>{{ $t('Til spillet er ferdig') }}</td></tr>
        <tr><td>{{ $t('Quizutkast') }}</td><td>{{ $t('Lagrer automatisk et pågående «Lag en quiz»-skjema') }}</td><td>{{ $t('Til du fullfører eller tømmer det') }}</td></tr>
        <tr><td>{{ $t('Cookie-valg') }}</td><td>{{ $t('Husker valget ditt nedenfor, slik at vi ikke spør ved hvert besøk') }}</td><td>{{ $t('Til du endrer det') }}</td></tr>
      </tbody>
    </table>
    <p style="color:var(--text-dim); font-size:0.85rem;">
      {{ $t('Alt det ovenstående er «strengt nødvendig» for en funksjon du aktivt bruker - den typen lagring som både norsk lov (ekomloven §2-7b) og personvernforordningens regler om informasjonskapsler unntar fra krav om eget samtykke, på samme måte som en handlekurv ikke trenger det.') }}
    </p>

    <h2>{{ $t('Google-innlogging') }}</h2>
    <p>
      {{ $t('Hvis du velger «Fortsett med Google» og godtar forespørselen, laster vi Googles eget innloggingsskript. Fra da av kan Google sette informasjonskapsler på sitt eget domene (accounts.google.com) som en del av innloggingen - dette er Googles informasjonskapsel, ikke vår, og styres av') }}
      <a href="https://policies.google.com/privacy" target="_blank" rel="noopener">{{ $t('Googles egen personvernerklæring') }}</a>{{ $t('. Hvis du avslår, lastes aldri skriptet, og du kan fortsatt logge inn med «Fortsett med e-post» i stedet.') }}
    </p>

    <h2>{{ $t('Skrifttyper') }}</h2>
    <p>
      {{ $t('Skrifttypene som brukes på dette nettstedet hostes direkte av oss og lastes ikke live fra Google Fonts - så å bare se på en side her sender aldri IP-adressen din til Google bare for å vise tekst.') }}
    </p>

    <h2>{{ $t('Endre valget ditt') }}</h2>
    <p>
      {{ $t('Klikk på') }} <strong>{{ $t('«Cookie-innstillinger»') }}</strong> {{ $t('nederst på en hvilken som helst side for å se valget igjen og endre det.') }}
    </p>

    <p>{{ $t('Se også vår') }} <router-link to="/privacy">{{ $t('personvernerklæring') }}</router-link>.</p>
  </div>
</template>
