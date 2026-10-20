<template>
  <div style="max-width:760px; margin:0 auto;">
    <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:12px; margin-bottom:12px;">
      <h1 style="margin:0;">{{ $t('Personvernerklæring') }}</h1>
      <router-link to="/" class="btn btn-secondary btn-sm">{{ $t('← Tilbake') }}</router-link>
    </div>

    <p class="page-subtitle">{{ $t('Sist oppdatert: 24.09.2026.') }}</p>

    <h2>{{ $t('Hvem vi er') }}</h2>
    <p>
      {{ $t('Quizzes (quizzes.no) drives av en privatperson, ikke et selskap. For spørsmål om personvern, eller for å utøve rettighetene nedenfor, kontakt') }} <strong>pal.fossnes@gmail.com</strong>.
    </p>

    <h2>{{ $t('Hva vi samler inn') }}</h2>
    <ul>
      <li><strong>{{ $t('Kontoinformasjon:') }}</strong> {{ $t('e-postadressen og navnet ditt, enten fra Google-innlogging eller fra registrering med e-post og passord (da lagres en sikkert hashet - aldri i klartekst - kopi av passordet ditt).') }}</li>
      <li><strong>{{ $t('Innhold du lager:') }}</strong> {{ $t('quizer du lager eller lagrer, spørsmål du sender inn til den felles spørsmålsbanken, og eventuelle problemrapporter eller tilbakemeldinger du sender inn, inkludert fritekst du skriver.') }}</li>
      <li><strong>{{ $t('Spillhistorikk:') }}</strong> {{ $t('hvilke ukentlige grids/brett du har fullført og poengene dine, slik at du kan fortsette på dem og se dine egne tidligere resultater.') }}</li>
      <li><strong>{{ $t('Flerspillerrom:') }}</strong> {{ $t('hvis du er vert for eller blir med i et live-rom, visningsnavnet ditt og et tilkoblingssignal mens spillet pågår.') }}</li>
      <li><strong>{{ $t('Gjester:') }}</strong> {{ $t('å bli med i et rom som gjest (uten konto) lagrer bare et valgt visningsnavn for den økten - ingenting annet, og det opprettes ingen konto.') }}</li>
    </ul>
    <p>{{ $t('Vi samler ikke inn betalingsinformasjon, nasjonale ID-numre eller noen særlige kategorier av personopplysninger (helse, religion osv.).') }}</p>

    <h2>{{ $t('Hvorfor vi behandler dem') }}</h2>
    <ul>
      <li>{{ $t('For å drifte kontoen din og la deg bruke funksjonene du registrerer deg for (avtale).') }}</li>
      <li>{{ $t('For å holde nettstedet sikkert og hindre misbruk, f.eks. ved å begrense gjentatte mislykkede innloggingsforsøk (berettiget interesse).') }}</li>
      <li>{{ $t('Der du aktivt har sagt ja, for eksempel Googles egen cookie for innlogging (samtykke - se våre') }} <router-link to="/cookies">{{ $t('retningslinjer for informasjonskapsler') }}</router-link>).</li>
    </ul>

    <h2>{{ $t('Hvem andre som ser dem') }}</h2>
    <p>{{ $t('Vi bruker noen få eksterne tjenester for å drifte nettstedet, som alle er databehandlere på våre vegne:') }}</p>
    <ul>
      <li><strong>Google</strong> {{ $t('- for å verifisere innlogginger med «Fortsett med Google», og for å sende transaksjons-e-post (nullstilling av passord, e-postbekreftelse) via Gmail. Googles egen personvernerklæring gjelder for deres håndtering av disse dataene.') }}</li>
      <li><strong>Render</strong> {{ $t('- huser selve applikasjonen, i et datasenter i EU (Frankfurt).') }}</li>
      <li><strong>{{ $t('Databaseleverandøren vår') }}</strong> {{ $t('- lagrer dataene beskrevet ovenfor, i en EU-region.') }}</li>
    </ul>
    <p>{{ $t('Vi selger aldri dataene dine, bruker dem ikke til reklame og deler dem ikke med datameglere.') }}</p>

    <h2>{{ $t('Hvor lenge vi beholder dem') }}</h2>
    <ul>
      <li>{{ $t('Kontoen din og innholdet som er knyttet til den beholdes til du sletter kontoen (se nedenfor) - vi har ingen automatisk utløpstid for aktive kontoer.') }}</li>
      <li>{{ $t('Data fra flerspillerrom (hvem som ble med, visningsnavn) slettes automatisk innen 24 timer etter at et spill er ferdig eller et rom er forlatt.') }}</li>
      <li>{{ $t('Lenker for nullstilling av passord og e-postbekreftelse utløper etter 30 minutter og kan ikke brukes på nytt.') }}</li>
    </ul>

    <h2>{{ $t('Dine rettigheter') }}</h2>
    <p>{{ $t('Etter personvernforordningen (GDPR) og personopplysningsloven kan du:') }}</p>
    <ul>
      <li><strong>{{ $t('Få innsyn og eksportere') }}</strong> {{ $t('dataene dine - last ned alt vi har om deg fra') }} <router-link to="/account">{{ $t('kontosiden') }}</router-link> {{ $t('din.') }}</li>
      <li><strong>{{ $t('Slette') }}</strong> {{ $t('kontoen og personopplysningene dine - også fra') }} <router-link to="/account">{{ $t('kontosiden') }}</router-link> {{ $t('din. Innhold du har bidratt med til den felles spørsmålsbanken eller meldt inn beholdes, men avidentifiseres (ikke lenger knyttet til deg) i stedet for å slettes helt, siden det allerede er delt med eller gjennomgått av andre.') }}</li>
      <li><strong>{{ $t('Rette') }}</strong> {{ $t('uriktige opplysninger - oppdater navnet ditt når som helst, eller kontakt oss for det du ikke kan endre selv.') }}</li>
      <li><strong>{{ $t('Protestere mot eller begrense') }}</strong> {{ $t('behandlingen, eller stille spørsmål om noe av det ovenstående - kontakt oss på adressen øverst på denne siden.') }}</li>
    </ul>
    <p>
      {{ $t('Hvis du ikke er fornøyd med svaret vårt, kan du klage til Datatilsynet,') }}
      <a href="https://www.datatilsynet.no" target="_blank" rel="noopener">Datatilsynet</a>.
    </p>

    <h2>{{ $t('Barn') }}</h2>
    <p>{{ $t('Denne tjenesten er ikke rettet mot barn under 13 år, og vi samler ikke bevisst inn data fra noen under den alderen.') }}</p>

    <h2>{{ $t('Sikkerhet') }}</h2>
    <p>
      {{ $t('Passord hashes (lagres aldri i klartekst), trafikken er kryptert (HTTPS), og tilgang til administratorfunksjoner er begrenset separat. Ingen systemer er helt sikre, men vi tar rimelige, vanlige forholdsregler.') }}
    </p>

    <h2>{{ $t('Endringer') }}</h2>
    <p>{{ $t('Hvis denne erklæringen endres på en vesentlig måte, oppdaterer vi datoen over.') }}</p>
  </div>
</template>
