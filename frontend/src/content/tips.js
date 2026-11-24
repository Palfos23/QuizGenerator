import { t } from '../i18n'

// The text of every page's "Tips og triks" popup. Built when asked for (not at import time) because the English
// catalog is only loaded just before the app starts. Keep each page to a handful of tips - the point is a quick
// nudge towards things people miss, not a manual.

// shared by the party games that can be played with a room code
const room = () => ({
  title: t('Spill sammen'),
  text: t('Send enheten rundt, eller la alle bruke sin egen enhet med en romkode. Del koden, invitasjonslenken eller QR-koden - vennene dine trenger ikke konto, de kan bli med som gjest.')
})
const host = () => ({
  title: t('Er noen koblet fra?'),
  text: t('Som vert kan du under «Administrer spillere» fjerne en spiller som ikke kommer tilbake, så spillet kan fortsette. Har verten mistet forbindelsen, kan en annen overta.')
})
const resume = () => ({
  title: t('Mistet tråden?'),
  text: t('Har du et spill i gang, vises en boks øverst på denne siden der du kan fortsette der du slapp.')
})
const example = () => ({
  title: t('Usikker på reglene?'),
  text: t('Åpne «Se et eksempel» lenger ned på siden - det viser en runde steg for steg.')
})

const ALL = {
  dashboard: () => [
    { title: t('Tilpass kontoen din'), text: t('Under Konto kan du endre visningsnavnet ditt - det er navnet som vises på topplistene og i spillrom - og velge fargetema.'), to: '/account', toLabel: t('Gå til Konto') },
    { title: t('Bytt språk'), text: t('Med NO / EN øverst til høyre bytter du språk. Titler og hint i spillene følger språket, når de er oversatt.') },
    { title: t('Uenig i en retting?'), text: t('Når dagens quiz er ferdig rettet, kan du be om ny vurdering av et svar du mener var riktig.') },
    { title: t('Spill med venner'), text: t('Alle selskapsspillene kan spilles på én enhet, eller på hver sin enhet med en romkode. Har du fått en kode, bruker du «Bli med med kode».') },
    { title: t('Lag din egen quiz'), text: t('Velg kategorier, lag en quiz på sekunder, og last den ned som PDF - med eller uten svar.'), to: '/generate', toLabel: t('Lag en quiz') }
  ],

  account: () => [
    { title: t('Visningsnavn'), text: t('Bruk gjerne for- og etternavn. Når to spillere har samme fornavn, legges første bokstav i etternavnet til, så «Erik H.» og «Erik S.» kan skilles.') },
    { title: t('Fargetema'), text: t('Velg blant flere fargetemaer - det byttes med en gang og huskes på denne enheten.') },
    { title: t('Passord'), text: t('Har du registrert deg med e-post, kan du bytte passord her. Du må skrive inn det nåværende først.') },
    { title: t('Språk'), text: t('Bytt mellom norsk og engelsk med NO / EN i menyen øverst.') },
    { title: t('Dataene dine'), text: t('Du kan når som helst laste ned en kopi av alt som er knyttet til kontoen din, eller slette den.') }
  ],

  tension: () => [
    { title: t('Hold deg nær 10'), text: t('Jo nærmere posisjon 10 du gjetter, jo mer får du - men rett bak ligger en felle, et «tension»-svar som koster deg poeng.') },
    { title: t('Ingen gjentakelser'), text: t('Et svar som en annen allerede har brukt denne runden kan ikke brukes igjen. Når alle har svart, avsløres hele listen.') },
    example(),
    room()
  ],

  imposter: () => [
    { title: t('Færrest imposters vinner'), text: t('De fleste rutene passer til temaet - noen få er bevisste imposters. Snu ruter sammen, og prøv å unngå dem.') },
    resume(),
    room(),
    host()
  ],

  bullseye: () => [
    { title: t('Nærmest vinner'), text: t('Hver runde viser et måltall. Bytt på å nevne en spiller du tror er nær det - den som svarer lengst unna slås ut.') },
    { title: t('Velg hva dere vil ha'), text: t('Under oppsettet kan du utelate kategorier du helst ikke vil ha spørsmål fra.') },
    room(),
    host()
  ],

  fiveoone: () => [
    { title: t('Som dart'), text: t('Start på 501 og trekk fra verdien av kastet. Første som sjekker ut mellom 0 og -10 vinner. Alt over 180 gir null.') },
    { title: t('Pass på bust'), text: t('Kommer du under -10, er kastet bust - totalen din går tilbake til det den var før kastet.') },
    example(),
    room()
  ],

  flashback: () => [
    { title: t('Treff året, ikke bare nær'), text: t('Du må gjette det nøyaktige året. Hintene kommer ett om gangen, vanskeligste først.') },
    { title: t('Tidlig treff gir mest'), text: t('Med 4 hint er riktig svar på hint 1 verdt 4 poeng, hint 2 verdt 3, og så videre ned til 1.') },
    { title: t('Bruk de feile årene'), text: t('År som allerede er prøvd forblir synlige for alle - bruk dem til å snevre inn hvor året ligger.') },
    room()
  ],

  penalty: () => [
    { title: t('Riktig rekkefølge'), text: t('Gjett hvem som tok hvert straffespark. Riktig gjetning avslører sparket og gir ett poeng - feil koster deg et liv.') },
    { title: t('Alene eller sammen'), text: t('Sett «Spillere» til 1 for å spille alene. Er dere flere, har hver spiller sine egne liv, og turen går videre etter hver gjetning.') },
    resume(),
    example()
  ],

  gridBattle: () => [
    { title: t('Bytt på å gjette'), text: t('Riktig gjetning snur ruten og gir ett poeng. Feil koster deg et liv. Alle har sine egne liv, så én som er ute stopper ikke de andre.') },
    { title: t('Flere runder'), text: t('Et helt spill er 2-4 grids etter hverandre. Poengene legges sammen, og høyest sum vinner.') },
    example(),
    room()
  ],

  xiBattle: () => [
    { title: t('Bytt på å gjette'), text: t('Søk etter en spiller du tror startet kampen. Riktig snur drakten og gir ett poeng - feil koster deg et liv.') },
    { title: t('Flere brett'), text: t('Et helt spill er 2-4 brett etter hverandre. Poengene legges sammen, og høyest sum vinner.') },
    example(),
    room()
  ],

  weeklyGrid: () => [
    { title: t('Liv og overtid'), text: t('Feil gjetninger koster liv. Er livene brukt opp, kan du fortsette i overtid for moro skyld - det teller ikke mot en ren løsning.') },
    { title: t('Ukens stilling'), text: t('Det du finner på ukens brett legges sammen fra mandag til søndag. Høyest sum vinner.') },
    { title: t('Se hvordan det gikk'), text: t('Når du er ferdig, kan du se hvordan det gikk med alle på brettet.') }
  ],

  startingXi: () => [
    { title: t('Liv'), text: t('Feil gjetninger koster liv. Finn alle spillerne som startet kampen før livene er brukt opp.') },
    { title: t('Ukens stilling'), text: t('Det du finner på ukens brett legges sammen fra mandag til søndag. Høyest sum vinner.') },
    { title: t('Flere spillere?'), text: t('Vil dere spille sammen, finnes XI Battle - der bytter dere på å gjette.') }
  ],

  quizGenerator: () => [
    { title: t('Velg hvor mange fra hver'), text: t('Legg til kategorier og bruk Flere / Færre for å bestemme hvor mange spørsmål du vil ha fra hver. Under «Avanserte valg» kan du begrense vanskelighetsgraden.') },
    { title: t('Eller velg selv'), text: t('Vil du ha bestemte spørsmål? Start tom, og søk deg frem til dem.') },
    { title: t('Finpuss i steg 2'), text: t('Bytt rekkefølge med ↑ / ↓, og forkast eller bytt ut spørsmål du ikke vil ha.') },
    { title: t('PDF og lagring'), text: t('Last ned som PDF - med eller uten svar, så kan du skrive ut ett ark til deltakerne og ett til deg. Å lage en quiz lagrer den ikke automatisk: trykk «Lagre i Mine quizer».') }
  ]
}

export function getTips(id) {
  return (ALL[id] || (() => []))()
}
